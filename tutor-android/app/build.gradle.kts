plugins {
  id("com.android.application")
}

android {
  namespace = "com.ancylostoma.tutor"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.ancylostoma.tutor"
    minSdk = 24
    targetSdk = 35
    // Cada compilación en GitHub Actions sube el número de versión para que
    // la APK nueva se instale encima de la anterior sin perder los datos.
    versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
    versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "0"}"
  }

  // Firma fija: si cambiara entre compilaciones, Android rechazaría la
  // actualización y habría que desinstalar (borrando los datos).
  signingConfigs {
    create("tutor") {
      storeFile = file("${rootDir}/tutor.keystore")
      storePassword = "tutor123"
      keyAlias = "tutor"
      keyPassword = "tutor123"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("tutor")
    }
    debug { signingConfig = signingConfigs.getByName("tutor") }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation("androidx.core:core:1.13.1")
  implementation("androidx.webkit:webkit:1.12.1")
}

