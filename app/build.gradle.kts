plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreFilePath = System.getenv("KEYSTORE_FILE")
val keystorePasswordEnv = System.getenv("KEYSTORE_PASSWORD")
val keyAliasEnv = System.getenv("KEY_ALIAS")
val keyPasswordEnv = System.getenv("KEY_PASSWORD")

val hasReleaseSigning = !keystoreFilePath.isNullOrBlank() &&
    !keystorePasswordEnv.isNullOrBlank() &&
    !keyAliasEnv.isNullOrBlank() &&
    !keyPasswordEnv.isNullOrBlank() &&
    file(keystoreFilePath).exists()

android {
    namespace = "io.github.linerfwz.linerxperianot"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.linerfwz.linerxperianot"
        minSdk = 31
        targetSdk = 35
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() } ?: "1.0.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreFilePath!!)
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
