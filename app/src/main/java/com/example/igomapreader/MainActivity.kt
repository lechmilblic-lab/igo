package com.example.igomapreader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {
 private val picker = 101
 private lateinit var result: TextView
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20) }
  root.addView(TextView(this).apply { text="iGO Map Reader"; textSize=26f })
  root.addView(TextView(this).apply { text="Sélectionne une archive ZIP contenant tes cartes iGO. L'application inventorie les fichiers sans les extraire."; textSize=15f })
  root.addView(Button(this).apply { text="CHOISIR UN ZIP"; setOnClickListener {
   val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="application/zip" }
   startActivityForResult(i,picker)
  }})
  result=TextView(this).apply { text="Aucune archive sélectionnée."; textSize=14f; setPadding(0,16,0,0) }
  root.addView(ScrollView(this).apply { addView(result) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f))
  setContentView(root)
 }
 @Deprecated("Document picker")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==picker && resultCode==Activity.RESULT_OK) data?.data?.let { uri ->
   try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_:Exception) {}
   inspect(uri)
  }
 }
 private fun inspect(uri:Uri) {
  result.text="Analyse en cours…"
  Thread {
   try {
    val found=mutableListOf<String>(); var total=0
    contentResolver.openInputStream(uri).use { stream ->
     requireNotNull(stream)
     ZipInputStream(stream).use { zip ->
      var e=zip.nextEntry
      while(e!=null) {
       if(!e.isDirectory) {
        total++; val n=e.name; val ext=n.substringAfterLast('.', "").lowercase()
        if(ext in listOf("fbl","fda","fpa","ftr","fsp","fhn","poi","3dl","3dc","gpx","kml","geojson"))
         found.add("• $n  [$ext]")
       }
       zip.closeEntry(); e=zip.nextEntry
      }
     }
    }
    val text="Archive analysée : $total fichier(s).\n\nFichiers reconnus :\n"+(if(found.isEmpty()) "Aucun fichier reconnu." else found.joinToString("\n"))+
     "\n\nÀ savoir : les cartes iGO .FBL et fichiers associés sont propriétaires. Cette version les détecte mais ne décode pas leur géométrie et ne peut donc pas les dessiner sur une carte. Pour une visualisation, il faut un export ouvert GPX, KML ou GeoJSON, ou un SDK autorisé."
    runOnUiThread { result.text=text }
   } catch(ex:Exception) { runOnUiThread { result.text="Erreur : ${ex.message}" } }
  }.start()
 }
}
