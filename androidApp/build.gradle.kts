@file:Suppress("UnstableApiUsage")

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileInputStream
import java.util.Properties
import com.android.build.api.dsl.ApplicationVariantDimension
import plugin.spmp.SpMpDeps
import plugin.spmp.getDeps

plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
}

val strings_file: File = rootProject.file("shared/src/commonMain/resources/assets/values/strings.xml")
var keystore_props_file: File = rootProject.file("androidApp/keystore.properties")
if (!keystore_props_file.isFile) {
    keystore_props_file = rootProject.file("androidApp/keystore.properties.debug")
}

val keystore_props = Properties()
keystore_props.load(FileInputStream(keystore_props_file))

fun getString(key: String): String {
    val reader = strings_file.reader()
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(reader)

    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            parser.next()
            continue
        }

        if (parser.getAttributeValue(null, "name") != key) {
            parser.next()
            continue
        }

        val ret = parser.nextText()
        reader.close()
        return ret
    }

    reader.close()
    throw NoSuchElementException(key)
}

kotlin {
    androidTarget()
    sourceSets {
        val deps: SpMpDeps = getDeps()

        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(deps.get("dev.toastbits.composekit:library-android"))
            }
        }
    }
}

android {
    namespace = "com.toasterofbread.spmp"
    compileSdk = (findProperty("android.compileSdk") as String).toInt()

    signingConfigs {
        create("main") {
            storeFile = file(keystore_props["storeFile"] as String)
            storePassword = keystore_props["storePassword"] as String
            keyAlias = keystore_props["keyAlias"] as String
            keyPassword = keystore_props["keyPassword"] as String
        }
    }

    defaultConfig {
        versionCode = getString("version_code").toInt()
        versionName = getString("version_string")

        applicationId = "com.toasterofbread.spmp"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }

    buildTypes {
        fun ApplicationVariantDimension.getApkName(): String =
            rootProject.name.lowercase() + "-" + getString("version_string") + applicationIdSuffix?.replace(".", "-").orEmpty() + ".apk"

        getByName("debug") {
            applicationIdSuffix = ".debug"
            setProperty("archivesBaseName", getApkName())

            manifestPlaceholders["appAuthRedirectScheme"] = "com.toasterofbread.spmp.debug"
            manifestPlaceholders["appName"] = getString("app_name_debug")
            signingConfig = signingConfigs.getByName("main")
        }
        getByName("release") {
            setProperty("archivesBaseName", getApkName())

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            manifestPlaceholders["appAuthRedirectScheme"] = "com.toasterofbread.spmp"
            manifestPlaceholders["appName"] = getString("app_name")
            signingConfig = signingConfigs.getByName("main")
        }
    }

    splits {
        abi {
            isEnable = project.hasProperty("enableApkSplit")
            reset()

            isUniversalApk = true
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        jvmToolchain {
            version = "17"
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("mozilla/public-suffix-list.txt")

            // For Kuromoji
            pickFirsts.add("META-INF/CONTRIBUTORS.md")
            pickFirsts.add("META-INF/LICENSE.md")
        }
    }

    lint {
        // What is this needed for?
        disable.add("ByteOrderMark")
    }

    sourceSets.getByName("main") {
        assets.srcDirs("src/main/assets")
        dependencies {
            implementation(project(":shared"))
        }
        manifest {
            srcFile("src/main/AndroidManifest.xml")
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("androidx.core:core-splashscreen:1.0.0")
}
