plugins {
    id("com.android.application")
}

android {
    namespace = "com.fonte.carnet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fonte.carnet"
        minSdk = 26          // Android 8.0 et plus
        targetSdk = 34
        versionCode = 5
        versionName = "4.0"
        // Téléphones Android (ARM) uniquement : APK plus léger avec la lecture de texte.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // Même clé de signature à chaque compilation (GitHub ou Android Studio) :
    // chaque nouvelle version s'installe par-dessus la précédente en gardant les données.
    signingConfigs {
        getByName("debug") {
            storeFile = file("fonte-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        // Appli installée directement (hors Play Store) : les exigences de version cible
        // du Play Store ne doivent pas bloquer une compilation « release ».
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Lecture du texte des photos (programmation du coach), modèle intégré : fonctionne hors ligne.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Lecture des codes-barres des produits (scan nutrition), modèle intégré.
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    testImplementation("junit:junit:4.13.2")
}
