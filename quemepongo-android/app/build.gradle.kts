plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
}

// En GitHub Actions un secreto que no existe llega como texto vacío.
fun entorno(nombre: String): String? = System.getenv(nombre)?.takeIf { it.isNotBlank() }

// Firma de subida a Google Play: llega por variables de entorno (secretos de
// GitHub Actions). Sin ellas, la release se firma con la llave de pruebas para
// poder instalar la APK en el teléfono, pero Play no la aceptaría.
val llaveSubida: String? = entorno("QMP_KEYSTORE_PATH")

// IDs de AdMob. Por defecto, los de prueba de Google: muestran anuncios de
// ejemplo y no pagan. Los reales se pasan como secretos al compilar.
val admobAppId: String = entorno("QMP_ADMOB_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
val admobBanner: String = entorno("QMP_ADMOB_BANNER") ?: "ca-app-pub-3940256099942544/9214589741"

android {
  namespace = "com.ancylostoma.quemepongo"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.ancylostoma.quemepongo"
    minSdk = 26
    targetSdk = 36
    // Cada compilación en GitHub Actions sube la versión: Play exige que
    // cada subida tenga un versionCode mayor que la anterior.
    versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
    versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"

    manifestPlaceholders["admobAppId"] = admobAppId
    buildConfigField("String", "ADMOB_BANNER", "\"$admobBanner\"")
  }

  signingConfigs {
    create("pruebas") {
      storeFile = file("${rootDir}/pruebas.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    llaveSubida?.let { ruta ->
      create("subida") {
        storeFile = file(ruta)
        storePassword = entorno("QMP_STORE_PASSWORD")
        keyAlias = entorno("QMP_KEY_ALIAS") ?: "subida"
        keyPassword = entorno("QMP_KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      // R8 apagado mientras se estabiliza la app: los errores salen legibles.
      // Al encenderlo la app pesa menos, pero hay que probar la release otra vez.
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName(if (llaveSubida != null) "subida" else "pruebas")
    }
    debug { signingConfig = signingConfigs.getByName("pruebas") }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.core:core-ktx:1.18.0")
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("io.coil-kt:coil-compose:2.7.0")
  implementation("com.google.android.gms:play-services-ads:23.6.0")
  implementation("com.google.android.ump:user-messaging-platform:3.1.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.16.1")
  testImplementation("androidx.test:core:1.6.1")
  testImplementation("androidx.test.ext:junit:1.3.0")
  testImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
  testImplementation("androidx.compose.ui:ui-test-junit4")
  // En las pruebas JVM, org.json de Android es un esqueleto vacío; esta es la real.
  testImplementation("org.json:json:20240303")
}
