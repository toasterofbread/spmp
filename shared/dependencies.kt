data class DependencyInfo(
  val version: String,
  val name: String,
  val author: String,
  val url: String,
  val license: String,
  val license_url: String,
  val fork_url: String? = null
)

val dependencies: Map<String, DependencyInfo> = 
  mapOf(
    "org.jetbrains.compose" to DependencyInfo(
      version = "1.6.2",
      name = "Compose Multiplatform",
      author = "JetBrains",
      url = "https://github.com/JetBrains/compose-multiplatform",
      license = "Apache-2.0",
      license_url = "https://github.com/JetBrains/compose-multiplatform/blob/a731ebc6494816764ae1d8850239fac9d606025d/LICENSE.txt"
    ),
    "org.apache.commons:commons-text" to DependencyInfo(
      version = "1.10.0",
      name = "Commons Text",
      author "Apache",
      url = "https://github.com/apache/commons-text",
      license = "Apache-2.0",
      license_url = "https://github.com/apache/commons-text/blob/82ec3722b1161cfdc7ccb0c2a6c93f037e29cf9e/LICENSE.txt"
    ),
    "com.atilika.kuromoji:kuromoji-ipadic" to DependencyInfo(
      version = "0.9.0",
      name = "Kuromoji",
      author = "atilika",
      url = "https://github.com/atilika/kuromoji",
      license = "Apache-2.0",
      license_url = "https://github.com/atilika/kuromoji/blob/e18ff911fdea0a93c92ec600dc6e123df363fa52/LICENSE.md"
    ),
    "org.jsoup:jsoup" to DependencyInfo(
      version = "1.16.1",
      name = "jsoup",
      author = "jhy",
      url = "https://github.com/jhy/jsoup",
      license = "MIT",
      license_url = "https://github.com/jhy/jsoup/blob/1f1f72d1e89821c630dcfc35e1a0a7f653cc877b/LICENSE"
    ),
    "com.github.toasterofbread.ComposeReorderable" to DependencyInfo(
      version = "e9ef693f63",
      name = "ComposeReorderable",
      author = "aclassen",
      url = "https://github.com/aclassen/ComposeReorderable",
      license = "Apache-2.0",
      license_url = "https://github.com/aclassen/ComposeReorderable/blob/b0729bddaeb11c88eca97b6bb01b011246df8f9e/LICENSE",
      fork_url = "https://github.com/toasterofbread/ComposeReorderable/"
    ),
    "com.github.SvenWoltmann:color-thief-java" to DependencyInfo(
      version = "v1.1.2",
      name = "Color Thief Java",
      author = "SvenWoltmann",
      url = "https://github.com/SvenWoltmann/color-thief-java",
      license = "CC BY 2.5",
      license_url = "https://creativecommons.org/licenses/by/2.5/"
    ),
    "com.github.catppuccin:java" to DependencyInfo(
      version = "v1.0.0",
      name = "Catppuccin Java",
      author = "Catppuccin",
      url = "https://github.com/catppuccin/java",
      license = "MIT",
      license_url = "https://github.com/catppuccin/java/blob/0b034e33c90585812d5287196bdfe930ab306914/LICENSE"
    ),
    "com.github.paramsen:noise" to DependencyInfo(
      version = "2.0.0",
      name = "Noise",
      author = "paramsen",
      url = "com.github.paramsen:noise",
      license = "Apache-2.0",
      license_url = "https://github.com/paramsen/noise/blob/0cccb4caaa0c7d31b5c76ec6e61805f937c4399e/LICENSE
    ),
    "org.kobjects.ktxml:core" to DependencyInfo(
      version = "0.2.3",
      name = "KtXml",
      author = "kobjects",
      url = "https://github.com/kobjects/ktxml",
      license = "Apache-2.0",
      license_url = "https://github.com/kobjects/ktxml/blob/428b7c1023c752354472c62b6f03490651458beb/LICENSE"
    ),
    "org.bitbucket.ijabz:jaudiotagger" to DependencyInfo(
      version = "v3.0.1",
      name = "Jaudiotagger",
      author = "IJabz",
      url = "https://bitbucket.org/ijabz/jaudiotagger/src/master/",
      license = "LGPL v2.1 or later",
      license_url = "https://bitbucket.org/ijabz/jaudiotagger/src/master/license.txt",
      fork_url = "https://github.com/marcoc1712/jaudiotagger"
    )
  )
        //     dependencies {
        //         implementation(compose.runtime)
        //         implementation(compose.foundation)
        //         implementation(compose.materialIconsExtended)
        //         implementation(compose.ui)
        //         implementation(compose.material)
        //         implementation(compose.material3)
        //         implementation(compose.components.resources)

        //         implementation("org.apache.commons:commons-text:1.10.0")
        //         implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")
        //         implementation("org.jsoup:jsoup:1.16.1")
        //         implementation("com.github.toasterofbread.ComposeReorderable:reorderable:e9ef693f63")
        //         implementation("com.github.SvenWoltmann:color-thief-java:v1.1.2")
        //         implementation("com.github.catppuccin:java:v1.0.0")
        //         implementation("com.github.paramsen:noise:2.0.0")
        //         implementation("org.kobjects.ktxml:core:0.2.3")
        //         implementation("org.bitbucket.ijabz:jaudiotagger:v3.0.1")
        //         implementation("com.github.teamnewpipe:NewPipeExtractor:v0.22.7")
        //         implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        //         implementation("org.zeromq:jeromq:0.5.3")
        //         implementation("media.kamel:kamel-image:0.9.4")

        //         implementation("io.ktor:ktor-client-core:$ktor_version")
        //         implementation("io.ktor:ktor-client-cio:$ktor_version")
        //         implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
        //         implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
        //     }
        // }

        // val androidMain by getting {
        //     dependencies {
        //         api("androidx.activity:activity-compose:1.8.1")
        //         api("androidx.core:core-ktx:1.12.0")
        //         api("androidx.appcompat:appcompat:1.6.1")
        //         implementation("androidx.palette:palette:1.0.0")
        //         implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

        //         val media3_version = "1.2.0"
        //         implementation("androidx.media3:media3-exoplayer:$media3_version")
        //         implementation("androidx.media3:media3-ui:$media3_version")
        //         implementation("androidx.media3:media3-session:$media3_version")

        //         implementation("com.google.accompanist:accompanist-pager:0.21.2-beta")
        //         implementation("com.google.accompanist:accompanist-pager-indicators:0.21.2-beta")
        //         implementation("com.google.accompanist:accompanist-systemuicontroller:0.21.2-beta")
        //         //noinspection GradleDependency
        //         implementation("com.github.andob:android-awt:1.0.0")
        //         implementation("com.github.toasterofbread:KizzyRPC:84e79614b4")
        //         implementation("app.cash.sqldelight:android-driver:2.0.0")
        //         implementation("com.anggrayudi:storage:1.5.5")
        //         implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.0")
        //         implementation("io.github.jan-tennert.supabase:functions-kt:1.3.2")
        //         implementation("io.ktor:ktor-client-cio:2.3.6")
        //         implementation("dev.toastbits.compose-webview-multiplatform:compose-webview-multiplatform-android:2d39439922")

        //         implementation("dev.toastbits.composekit:library-android:$composekit_version")
        //         implementation("dev.toastbits.ytm-kt:ytmkt-android:$ytmkt_version")
        //     }
        // }

        // val desktopMain by getting {
        //     dependencies {
        //         implementation(compose.desktop.common)
        //         implementation("com.github.ltttttttttttt:load-the-image:1.0.5")
        //         implementation("app.cash.sqldelight:sqlite-driver:2.0.0")
        //         implementation("com.github.caoimhebyrne:KDiscordIPC:0.2.2")
        //         implementation("org.bytedeco:ffmpeg-platform:6.1.1-1.5.10")
        //         implementation("dev.toastbits.compose-webview-multiplatform:compose-webview-multiplatform-desktop:2d39439922")

        //         implementation("dev.toastbits.composekit:library-desktop:$composekit_version")
        //         implementation("dev.toastbits.ytm-kt:ytmkt-jvm:$ytmkt_version")
        //         implementation("dev.toastbits.mediasession:library-jvm:$mediasessionkt_version")
        //     }
        // }
