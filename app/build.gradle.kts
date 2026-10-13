plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ca.panierfute"
    compileSdk = 35

    defaultConfig {
        applicationId = "ca.panierfute"
        minSdk = 28          // Android 9+ (tous les Galaxy récents)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Clé de signature personnelle, optionnelle et jamais versionnée sur GitHub.
    // Sur GitHub, elle vient des « secrets » du dépôt (voir README). Avec toujours
    // la même clé, chaque nouvelle version s'installe par-dessus l'ancienne.
    // Sans clé, l'APK est signé avec la clé de débogage standard.
    val fichierCle = file("panierfute.keystore")
    val motDePasseCle = System.getenv("PANIER_KEYSTORE_PASSWORD")
    val cleDisponible = fichierCle.exists() && !motDePasseCle.isNullOrBlank()

    signingConfigs {
        if (cleDisponible) {
            create("perso") {
                storeFile = fichierCle
                storePassword = motDePasseCle
                keyAlias = "panierfute"
                keyPassword = motDePasseCle
            }
        }
    }

    buildTypes {
        debug {
            if (cleDisponible) signingConfig = signingConfigs.getByName("perso")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (cleDisponible) "perso" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
