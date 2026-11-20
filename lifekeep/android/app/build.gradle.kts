import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Release signing (upload key). Never commit keystores or passwords.
// Source 1: android/key.properties (git-ignored).
// Source 2: environment variables (e.g. GitHub Actions secrets):
//   LIFEKEEP_UPLOAD_STORE_FILE, LIFEKEEP_UPLOAD_STORE_PASSWORD,
//   LIFEKEEP_UPLOAD_KEY_ALIAS, LIFEKEEP_UPLOAD_KEY_PASSWORD
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
fun signingValue(prop: String, env: String): String? =
    (keystoreProperties[prop] as String?) ?: System.getenv(env)?.takeIf { it.isNotBlank() }
val uploadStoreFile = signingValue("storeFile", "LIFEKEEP_UPLOAD_STORE_FILE")
val hasUploadKey = uploadStoreFile != null && file(uploadStoreFile).exists()
// Pass -PrequireUploadKey=true for Play builds: fail instead of falling back to the debug key.
val requireUploadKey = (project.findProperty("requireUploadKey") as String?) == "true"
if (requireUploadKey && !hasUploadKey) {
    throw GradleException("Upload keystore not configured (see README: Release signing).")
}

android {
    namespace = "com.lifekeep.app"
    // androidx.core 1.19+ (pulled in by plugins) requires compiling against API 37.
    compileSdk = maxOf(flutter.compileSdkVersion, 37)
    ndkVersion = flutter.ndkVersion

    compileOptions {
        // Required by flutter_local_notifications (scheduled notifications on older Android).
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Permanent Play Store identity: cannot change after the first upload.
        applicationId = "com.sek789102.lifekeep"
        minSdk = maxOf(flutter.minSdkVersion, 24)
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        multiDexEnabled = true
    }

    signingConfigs {
        if (hasUploadKey) {
            create("release") {
                storeFile = file(uploadStoreFile!!)
                storePassword = signingValue("storePassword", "LIFEKEEP_UPLOAD_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "LIFEKEEP_UPLOAD_KEY_ALIAS") ?: "upload"
                keyPassword = signingValue("keyPassword", "LIFEKEEP_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 code + resource shrinking is enabled by the Flutter Gradle plugin.
            // Uses the upload key when configured, otherwise the debug key so the
            // APK is still installable for testing (Play rejects debug-signed bundles).
            signingConfig = if (hasUploadKey) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}

flutter {
    source = "../.."
}
