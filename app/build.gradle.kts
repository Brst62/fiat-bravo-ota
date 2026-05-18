import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("signing/keystore.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.nwd.fiatlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = providers.gradleProperty("NWD_APPLICATION_ID").get()
        minSdk = 33
        targetSdk = 35
        versionCode = providers.gradleProperty("NWD_VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("NWD_VERSION_NAME").get()

        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }

        // Telemetry + OTA destination — overridable from local.properties / CI env
        buildConfigField(
            "String", "OTA_REPO",
            "\"${project.findProperty("OTA_REPO") ?: "Brst62/fiat-bravo-ota"}\""
        )
        buildConfigField(
            "String", "TELEMETRY_REPO",
            "\"${project.findProperty("TELEMETRY_REPO") ?: "Brst62/fiat-bravo-telemetry"}\""
        )
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true; viewBinding = true }
    sourceSets["main"].java.srcDirs("src/main/kotlin")
    packaging {
        resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/*.kotlin_module")
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":diagnostic:collector"))
    implementation(project(":diagnostic:uploader"))
    implementation(project(":ota:github"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.startup)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    coreLibraryDesugaring(libs.desugar.jdk)
}
