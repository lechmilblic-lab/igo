package com.example.igomapreader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
    private val pickerRequestCode = 101
    private lateinit var result: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20)
        }

        root.addView(TextView(this).apply {
            text = "iGO Map Reader"
            textSize = 26f
        })

        root.addView(TextView(this).apply {
            text = "Sélectionne une archive ZIP contenant tes fichiers iGO. L'application en affiche l'inventaire sans extraire les fichiers."
            textSize = 15f
            setPadding(0, 12, 0, 12)
        })

        root.addView(Button(this).apply {
            text = "CHOISIR UN ZIP"
            setOnClickListener { openZipPicker() }
        })

        result = TextView(this).apply {
            text = "Aucune archive sélectionnée. Appuie sur CHOISIR UN ZIP."
            textSize = 14f
            setPadding(0, 16, 0, 0)
        }

        root.addView(
            ScrollView(this).apply { addView(result) },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        setContentView(root)
    }

    private fun openZipPicker() {
        // Certains gestionnaires de fichiers Samsung n'annoncent pas les ZIP
        // avec le seul type MIME application/zip. On accepte donc tous les
        // types au sélecteur, puis on vérifie l'extension du fichier choisi.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream"
                )
            )
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }

        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, pickerRequestCode)
        } catch (e: Exception) {
            result.text = "Impossible d'ouvrir le sélecteur de fichiers : ${e.message ?: "erreur inconnue"}"
        }
    }

    @Deprecated("Utilisation du sélecteur de documents Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickerRequestCode) return

        if (resultCode != Activity.RESULT_OK) {
            result.text = "Aucun fichier sélectionné. Appuie sur CHOISIR UN ZIP pour réessayer."
            return
        }

        val uri: Uri = data?.data ?: run {
            result.text = "Le sélecteur n'a pas transmis de fichier. Réessaie."
            return
        }

        val displayName = getDisplayName(uri) ?: "archive sélectionnée"
        if (!displayName.lowercase().endsWith(".zip")) {
            result.text = "Fichier refusé : \"$displayName\" n'est pas un fichier .zip. Choisis une archive ZIP."
            return
        }

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Certains fournisseurs ne proposent pas de permission persistante.
            // L'URI reste lisible pendant cette session.
        }

        inspectZip(uri, displayName)
    }

    private fun getDisplayName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
    }

    private fun inspectZip(uri: Uri, displayName: String) {
        result.text = "Analyse de $displayName en cours…"

        Thread {
            try {
                val found = mutableListOf<String>()
                var total = 0
                val recognizedExtensions = setOf(
                    "fbl", "fda", "fpa", "ftr", "fsp", "fhn", "poi",
                    "3dl", "3dc", "gpx", "kml", "geojson"
                )

                val input = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Android n'a pas pu ouvrir ce fichier.")

                input.use { stream ->
                    ZipInputStream(stream).use { zip ->
                        var entry = zip.nextEntry
                        if (entry == null) {
                            throw ZipException("L'archive est vide ou n'est pas un ZIP valide.")
                        }
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                total++
                                val extension = entry.name.substringAfterLast('.', "").lowercase()
                                if (extension in recognizedExtensions) {
                                    found.add("• ${entry.name} [$extension]")
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }

                val report = buildString {
                    append("Archive : $displayName\n")
                    append("Analyse terminée : $total fichier(s).\n\n")
                    append("Fichiers reconnus :\n")
                    append(if (found.isEmpty()) "Aucun fichier iGO/GPX/KML reconnu." else found.joinToString("\n"))
                    append("\n\nÀ savoir : cette version détecte les fichiers .FBL et associés, mais ne décode pas leur format cartographique propriétaire pour les dessiner sur une carte. Elle peut inventorier les fichiers GPX, KML et GeoJSON, mais ne les affiche pas encore sur une carte.")
                }

                runOnUiThread { result.text = report }
            } catch (e: Exception) {
                val message = when (e) {
                    is ZipException -> "Cette archive ne semble pas être un ZIP valide : ${e.message ?: "format incorrect"}"
                    else -> "Erreur pendant la lecture du ZIP : ${e.message ?: e.javaClass.simpleName}"
                }
                runOnUiThread { result.text = message }
            }
        }.start()
    }
}
