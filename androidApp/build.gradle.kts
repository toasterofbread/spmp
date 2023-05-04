import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun createKeysHashTable(): String {
    val keys_file = rootProject.file("keys.properties")
    if (keys_file.exists()) {
        val keys = Properties()
        keys.load(FileInputStream(keys_file))

        var value = ""
        for (key in keys) {
            value += String.format("put(\"%s\",%s); ", key.key, key.value)
        }

        return String.format("%s%s%s", "new java.util.Hashtable<String, String>(){{ ", value, "}}")
    }
    else {
        return "null"
    }
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

//    splits {
//        abi {
//            enable = true
//            reset()
//            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
//            universalApk = true
//        }
//    }

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
//            this.configure {
//                java.srcDir(buildConfigDir)
//            }
        }
    }
}


configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

var compose_version = "1.2.1"

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.compose.material3:material3:1.0.0-alpha11")
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("androidx.fragment:fragment-ktx:1.3.0-alpha07")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
    implementation("androidx.compose.material:material-icons-extended:$compose_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.palette:palette:1.0.0")

    implementation("com.google.accompanist:accompanist-pager:0.21.2-beta")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.21.2-beta")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.21.2-beta")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.21.2-beta")

    // Shared
    implementation("com.beust:klaxon:5.5")
    implementation("com.godaddy.android.colorpicker:compose-color-picker:0.7.0")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.22.1")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.2")

    // Exoplayer
    implementation(project(":library-core"))
    implementation(project(":library-ui"))
    implementation(project(":extension-mediasession"))
}
