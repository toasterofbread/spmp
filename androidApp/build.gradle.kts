import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val strings_file = rootProject.file("shared/src/commonMain/resources/assets/values/strings.xml")
var keystore_props_file = rootProject.file("androidApp/keystore.properties")
if (!keystore_props_file.isFile()) {
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

android {
    namespace = "com.toasterofbread.spmp"

    signingConfigs {
        create("main") {
            storeFile = file(keystore_props["storeFile"] as String)
            storePassword = keystore_props["storePassword"] as String
            keyAlias = keystore_props["keyAlias"] as String
            keyPassword = keystore_props["keyPassword"] as String
        }
    }

    compileSdk = (findProperty("android.compileSdk") as String).toInt()

    defaultConfig {
        applicationId = "com.toasterofbread.spmp"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 2
        versionName = getString("version_string")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appAuthRedirectScheme"] = "com.toasterofbread.spmp.debug"
            manifestPlaceholders["appName"] = getString("app_name_debug")
            signingConfig = signingConfigs.getByName("main")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["appAuthRedirectScheme"] = "com.toasterofbread.spmp"
            manifestPlaceholders["appName"] = getString("app_name")
            signingConfig = signingConfigs.getByName("main")
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
