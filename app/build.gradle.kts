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
        versionCode = 1
        versionName = "1.0"
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
    // Aucune dépendance pour l'appli elle-même : uniquement le SDK Android.
    testImplementation("junit:junit:4.13.2")
}
