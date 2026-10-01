plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    dependencies {
        implementation(project.dependencies.platform(libs.koin.bom))
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.koin.android)

        implementation(projects.shared)
    }
}

android {
    namespace = "com.chow.pomegranate"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.chow.pomegranate"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.environmentVariable("ANDROID_VERSION_CODE").orNull?.toInt() ?: 2
        versionName = libs.versions.versionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    val releaseStore = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
    signingConfigs {
        if (releaseStore != null) {
            create("ciRelease") {
                storeFile = file(releaseStore)
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseStore != null) signingConfig = signingConfigs.getByName("ciRelease")
        }
    }
}
