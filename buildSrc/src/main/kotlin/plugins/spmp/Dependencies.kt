package plugin.spmp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

fun Project.getDeps(): SpMpDeps =
    SpMpDeps(extra.properties)

class SpMpDeps(extra: Map<String, Any>) {
    companion object {
        const val OUTPUT_DIR: String = "generated/sources/dependencyList/main/com/toasterofbread/spmp"
    }

    fun get(artifact: String, dependency_id: String = artifact): String {
        return (getRedirect(dependency_id) ?: artifact) + ":" + getVersion(dependency_id)
    }

    fun getRedirect(artifact: String): String? {
        val dependency: DependencyInfo = findDependency(artifact)
        return dependency.redirect
    }

    fun getVersion(artifact: String): String {
        val dependency: DependencyInfo = findDependency(artifact)
        if (dependency.version == null) {
            throw RuntimeException("Dependency for artifact '$artifact' has no specified version")
        }
        return dependency.version
    }

    fun findDependency(artifact: String): DependencyInfo {
        dependencies[artifact]?.also { return it }
        dependencies[artifact.split(":", limit = 2).first()]?.also { return it }
        throw RuntimeException("No dependency found matching artifact '$artifact")
    }

    fun getAllComposeKit(): List<String> =
        listOf(
            "dev.toastbits:composekit-application",
            "dev.toastbits:composekit-commonsettings",
            "dev.toastbits:composekit-components",
            "dev.toastbits:composekit-context",
            "dev.toastbits:composekit-navigation",
            "dev.toastbits:composekit-settings",
            "dev.toastbits:composekit-settingsitem-presentation",
            "dev.toastbits:composekit-theme-core",
            "dev.toastbits:composekit-theme-config",
            "dev.toastbits:composekit-util",
            "dev.toastbits:composekit-utilKt"
        ).map { get(it, "dev.toastbits:composekit") }

    val dependencies: Map<String, DependencyInfo> =
        mapOf(
            "dev.toastbits:spms" to DependencyInfo(
                version = "0.4.6",
                name = "spmp-server",
                author = "toasterofbread",
                url = "https://github.com/toasterofbread/spmp-server",
                license = "GPL-3.0",
                license_url = "https://github.com/toasterofbread/spmp-server/blob/6dde651ffc102d604ac7ecd5ac7471b1572fd2e6/LICENSE"
            ),
            "dev.toastbits:composekit" to DependencyInfo(
                version = "0.1.0-rc4",
                name = "ComposeKit",
                author = "toasterofbread",
                url = "https://github.com/toasterofbread/composekit",
                license = "GPL-3.0",
                license_url = "https://github.com/toasterofbread/ComposeKit/blob/136f216e65395660255d3270af9b79c90ae2254c/LICENSE"
            ),
            "dev.toastbits:ytm-kt" to DependencyInfo(
                version = "0.4.3",
                name = "ytm-kt",
                author = "toasterofbread",
                url = "https://gitlab.com/toasterofbread/ytm-kt",
                license = "Apache-2.0",
                license_url = "https://gitlab.com/toastbits/ytm-kt/-/blob/665916f150af57532e80ce3397d260af4cbee789/LICENSE"
            ),
            "dev.toastbits.mediasession" to DependencyInfo(
                version = "0.1.0",
                name = "mediasession-kt",
                author = "toasterofbread",
                url = "https://github.com/toasterofbread/mediasession-kt",
                license = "Apache-2.0",
                license_url = "https://github.com/toasterofbread/mediasession-kt/blob/fd4c5e876e2782dbe856b886f5b8dc083c26293c/LICENSE"
            ),
            "dev.toastbits.kana-kt" to DependencyInfo(
                version = "c891811875",
                name = "kana-kt",
                author = "toasterofbread",
                url = "https://github.com/toasterofbread/kana-kt",
                license = "Apache-2.0",
                license_url = "https://github.com/toasterofbread/kana-kt/blob/2f8dc077096f3d344649fe91caa6b079194e6718/LICENSE"
            ),

            "org.ketbrains.kotlin" to DependencyInfo(
                version = extra["kotlin.version"] as String,
                name = "Kotlin",
                author = "JetBrains",
                url = "https://github.com/JetBrains/kotlin",
                license = "Apache-2.0",
                license_url = "https://github.com/Kotlin/kotlinx.serialization/blob/51cb8e8e556983fc83a565d5f04bb089363453e0/LICENSE.txt"
            ),
            "org.jetbrains.compose" to DependencyInfo(
                version = "1.8.0-alpha01",
                name = "Compose Multiplatform",
                author = "JetBrains",
                url = "https://github.com/JetBrains/compose-multiplatform",
                license = "Apache-2.0",
                license_url = "https://github.com/JetBrains/compose-multiplatform/blob/a731ebc6494816764ae1d8850239fac9d606025d/LICENSE.txt"
            ),
            "androidx" to DependencyInfo(
                version = null,
                name = "AndroidX",
                author = "AOSP",
                url = "https://github.com/androidx",
                license = "Apache-2.0",
                license_url = "https://github.com/androidx/androidx/blob/8d6777b558dc315e96ee908502e847e2cd29e216/LICENSE.txt"
            ),
            "androidx.media3" to DependencyInfo(
                version = "1.4.1",
                name = "AndroidX Media",
                author = "AOSP",
                url = "https://github.com/androidx/media",
                license = "Apache-2.0",
                license_url = "https://github.com/androidx/media/blob/d833d59124d795afc146322fe488b2c0d4b9af6a/LICENSE"
            ),
            "org.apache.commons:commons-text" to DependencyInfo(
                version = "1.10.0",
                name = "Commons Text",
                author = "Apache",
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
            "com.andree-surya:moji4j" to DependencyInfo(
                version = "1.2.0",
                name = "Moji4J",
                author = "Andree Surya",
                url = "https://github.com/andree-surya/moji4jh",
                license = "Apache-2.0",
                license_url = "https://github.com/andree-surya/moji4j/blob/ea0168f125da8791e951eab7cdf18b06a7db705b/README.md"
            ),
            "com.mohamedrejeb.ksoup" to DependencyInfo(
                version = "0.4.0",
                name = "Ksoup",
                author = "MohamedRejeb",
                url = "https://github.com/MohamedRejeb/Ksoup",
                license = "Apache-2.0",
                license_url = "https://github.com/MohamedRejeb/Ksoup/blob/5f07e799c95e518d80caf70fc586ddcc649e1315/LICENSE"
            ),
            "com.github.toasterofbread.ComposeReorderable" to DependencyInfo(
                version = "77c7652169",
                name = "ComposeReorderable",
                author = "aclassen",
                url = "https://github.com/aclassen/ComposeReorderable",
                license = "Apache-2.0",
                license_url = "https://github.com/aclassen/ComposeReorderable/blob/b0729bddaeb11c88eca97b6bb01b011246df8f9e/LICENSE",
                fork_url = "https://github.com/toasterofbread/ComposeReorderable/",
//                redirect = "org.burnoutcrew.composereorderable:reorderable"
            ),

            "com.github.SvenWoltmann:color-thief-java" to DependencyInfo(
                version = "v1.1.2",
                name = "Color Thief Java",
                author = "SvenWoltmann",
                url = "https://github.com/SvenWoltmann/color-thief-java",
                license = "CC BY 2.5",
                license_url = "https://creativecommons.org/licenses/by/2.5/"
            ),
            "com.github.paramsen:noise" to DependencyInfo(
                version = "2.0.0",
                name = "Noise",
                author = "paramsen",
                url = "com.github.paramsen:noise",
                license = "Apache-2.0",
                license_url = "https://github.com/paramsen/noise/blob/0cccb4caaa0c7d31b5c76ec6e61805f937c4399e/LICENSE"
            ),
            "io.github.pdvrieze.xmlutil" to DependencyInfo(
                version = "0.90.1",
                name = "XmlUtil",
                author = "pdvrieze",
                url = "https://github.com/pdvrieze/xmlutil",
                license = "Apache-2.0",
                license_url = "https://github.com/pdvrieze/xmlutil/blob/0c9e14016b9cfc1682642a8d6ad40ae64cf6ef6f/COPYING"
            ),
            "org.bitbucket.ijabz:jaudiotagger" to DependencyInfo(
                version = "v3.0.1",
                name = "Jaudiotagger",
                author = "IJabz",
                url = "https://bitbucket.org/ijabz/jaudiotagger/src/master/",
                license = "LGPL-2.1 or later",
                license_url = "https://bitbucket.org/ijabz/jaudiotagger/src/master/license.txt",
                fork_url = "https://github.com/marcoc1712/jaudiotagger"
            ),
            "org.zeromq:jeromq" to DependencyInfo(
                version = "0.6.0",
                name = "JeroMQ",
                author = "zeromq",
                url = "https://github.com/zeromq/jeromq",
                license = "MPL-2.0",
                license_url = "https://github.com/zeromq/jeromq/blob/30b2bf4e1c7332108497db7e4125cd8b15113ea4/LICENSE"
            ),
            "io.coil-kt.coil3" to DependencyInfo(
                version = "3.0.0-rc01",
                name = "Coil",
                author = "Coil",
                url = "https://github.com/coil-kt/coil",
                license = "Apache-2.0",
                license_url = "https://github.com/coil-kt/coil/blob/f1e16bdf534dcc6413a0d3763bc741c23aafa441/LICENSE.txt"
            ),
            "io.ktor" to DependencyInfo(
                version = "3.0.0",
                name = "Ktor",
                author = "JetBrains",
                url = "https://github.com/ktorio/ktor",
                license = "Apache-2.0",
                license_url = "https://github.com/ktorio/ktor/blob/d5ae8e5641dea582fbe5ebb52577e7bdad2f5ad8/LICENSE"
            ),

            "com.google.accompanist" to DependencyInfo(
                version = "0.21.2-beta",
                name = "Accompanist",
                author = "Google",
                url = "https://github.com/google/accompanist",
                license = "Apache-2.0",
                license_url = "https://github.com/google/accompanist/blob/4bafb060eb30a3d19d808aab2b6c30df16cad70b/LICENSE"
            ),
            "com.github.andob:android-awt" to DependencyInfo(
                version = "1.0.0",
                name = "android-awt",
                author = "windwardadmin",
                url = "https://github.com/windwardadmin/android-awt",
                license = "Apache-2.0",
                license_url = "https://github.com/windwardadmin/android-awt/blob/a6ac3b06c71eee94195c46aef75b1c4440d82ea8/LICENSE.txt",
                fork_url = "https://github.com/andob/android-awt"
            ),
            "com.github.toasterofbread:KizzyRPC" to DependencyInfo(
                version = "e2459918be",
                name = "KizzyRPC",
                author = "dead8309",
                url = "https://github.com/dead8309/KizzyRPC",
                license = "Apache-2.0",
                license_url = "https://github.com/toasterofbread/KizzyRPC/blob/84e79614b4aaec6bb16c70bd60c31c30ff03bb6d/LICENSE",
                fork_url = "https://github.com/toasterofbread/KizzyRPC/"
            ),
            "app.cash.sqldelight" to DependencyInfo(
                version = extra["sqldelight.version"] as String,
                name = "SQLDelight",
                author = "Cash App",
                url = "https://github.com/cashapp/sqldelight",
                license = "Apache-2.0",
                license_url = "https://github.com/dead8309/KizzyRPC/blob/8c5c05bbda8095accd34d66795b6d191ceb927ae/LICENSE"
            ),
            "com.anggrayudi:storage" to DependencyInfo(
                version = "1.5.5",
                name = "SimpleStorage",
                author = "anggrayudi",
                url = "https://github.com/anggrayudi/SimpleStorage",
                license = "Apache-2.0",
                license_url = "https://github.com/anggrayudi/SimpleStorage/blob/cdab9945ccaeb6deae3906db3af98a87bc450e5f/LICENSE"
            ),
            "io.github.jan-tennert.supabase:functions-kt" to DependencyInfo(
                version = "2.5.3-wasm0",
                name = "supabase-kt",
                author = " jan-tennert",
                url = "https://github.com/supabase-community/supabase-kt",
                license = "MIT",
                license_url = "https://github.com/supabase-community/supabase-kt/blob/d198a112ba7e1b11d83cd28eba74fdd863d259c3/LICENSE"
            ),
            "com.github.toasterofbread.compose-webview-multiplatform" to DependencyInfo(
                version = "83732f91e4",
                name = "WebView for JetBrains Compose Multiplatform",
                author = "KevinnZou",
                url = "https://github.com/KevinnZou/compose-webview-multiplatform",
                license = "Apache-2.0",
                license_url = "https://github.com/KevinnZou/compose-webview-multiplatform/blob/f36c90aa356e0adbf95724fc3023be50f3467c96/LICENSE.txt",
                fork_url = "https://github.com/toasterofbread/compose-webview-multiplatform"
            ),

            "com.github.caoimhebyrne:KDiscordIPC" to DependencyInfo(
                version = "0.2.2",
                name = "KDiscordIPC",
                author = "caoimhebyrne",
                url = "https://github.com/caoimhebyrne/KDiscordIPC",
                license = "MIT",
                license_url = "https://github.com/caoimhebyrne/KDiscordIPC/blob/b136b267d146544c49d80b4c9a046d66324a601b/LICENSE"
            ),
            "org.bytedeco:ffmpeg-platform" to DependencyInfo(
                version = "6.1.1-1.5.10",
                name = "FFmpeg for Java",
                author = "FFmpeg team / bytedeco",
                url = "https://github.com/bytedeco/javacpp-presets",
                license = "Apache-2.0",
                license_url = "https://github.com/bytedeco/javacpp-presets/blob/34289d4d0c421fa345c9f537bb3afdde4bb4a0c6/LICENSE.txt"
            ),
            "io.github.selemba1000:jmtc" to DependencyInfo(
                version = null,
                name = "JavaMediaTransportControls",
                author = "Selemba1000",
                url = "https://github.com/Selemba1000/JavaMediaTransportControls",
                license = "MIT",
                license_url = "https://github.com/toasterofbread/mediasession-kt/blob/ee08de4f82375e90fad0d285dca5add66227b5e5/library/src/nativeInterop/mingw-x86_64/thirdparty/libsmtc/LICENSE"
            ),
            "com.egeniq.exovisualizer" to DependencyInfo(
                version = null,
                name = "ExoVisualizer",
                author = "dzolnai",
                url = "https://github.com/dzolnai/ExoVisualizer",
                license = "MIT",
                license_url = "https://github.com/dzolnai/ExoVisualizer/blob/720e1b127b900aece546b8e88aed65db50379f0a/LICENSE"
            )
        )
}
