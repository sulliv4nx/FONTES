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
        versionCode = 10
        versionName = "4.5"
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

// Table Ciqual 2025 (ANSES, licence ouverte) : téléchargée sur Zenodo et convertie à chaque compilation
// (tools/CiqualImport.java), puis rangée dans les assets de l'appli. Si le téléchargement échoue, la compilation échoue.
val ciqualDir = layout.buildDirectory.dir("generated/ciqual/assets").get().asFile
val ciqualImport = tasks.register<Exec>("ciqualImport") {
    description = "Télécharge la table Ciqual et la convertit pour la recherche d'aliments."
    val script = rootProject.file("tools/CiqualImport.java")
    val out = File(ciqualDir, "ciqual.tsv")
    inputs.file(script)
    outputs.file(out)
    executable = File(System.getProperty("java.home"), "bin/java").absolutePath
    args("-Dfile.encoding=UTF-8", script.absolutePath, out.absolutePath)
}
android.sourceSets.getByName("main").assets.srcDir(ciqualDir)
tasks.named("preBuild").configure { dependsOn(ciqualImport) }
tasks.configureEach {
    if (name != "ciqualImport" && Regex("^(generate|merge|compress|package)\\w*Assets$").matches(name)) dependsOn(ciqualImport)
}

dependencies {
    // Lecture du texte des photos (programmation du coach), modèle intégré : fonctionne hors ligne.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    // Lecture des codes-barres des produits (scan nutrition), modèle intégré.
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    testImplementation("junit:junit:4.13.2")
}
