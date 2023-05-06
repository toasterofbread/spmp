plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "passwd"
            keyAlias = "default"
            keyPassword = "passwd"
        }
    }

    compileSdkVersion(33)

    defaultConfig {
        applicationId = "com.spectre7.spmp"
        minSdkVersion(31)
        targetSdkVersion(33)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

//        ndk {
//            abiFilters("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
//        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appAuthRedirectScheme"] = "com.spectre7.spmp.debug"
            manifestPlaceholders["appName"] = "SpMp (debug)"
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["appAuthRedirectScheme"] = "com.spectre7.spmp"
            manifestPlaceholders["appName"] = "SpMp"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        exclude("META-INF/DEPENDENCIES")
        exclude("mozilla/public-suffix-list.txt")

        // For Kuromoji
        pickFirst("META-INF/CONTRIBUTORS.md")
        pickFirst("META-INF/LICENSE.md")
    }

    lintOptions {
        disable("ByteOrderMark")
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}
