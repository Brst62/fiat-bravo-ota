plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.nwd.externalapp.smartcamera"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.nwd.smartcamera"
        minSdk = 33
        targetSdk = 35
        versionCode = providers.gradleProperty("NWD_VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("NWD_VERSION_NAME").get()
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].java.srcDirs("src/main/kotlin")
}
dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
}
