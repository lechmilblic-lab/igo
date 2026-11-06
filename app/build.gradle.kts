plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "com.example.igomapreader"
 compileSdk = 36
 defaultConfig { applicationId = "com.example.igomapreader"; minSdk = 23; targetSdk = 36; versionCode = 1; versionName = "1.0" }
 ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.appcompat:appcompat:1.7.0")
 implementation("com.google.android.material:material:1.12.0")
}
