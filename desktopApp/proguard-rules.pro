-dontobfuscate
-keep class com.toasterofbread.spmp.** { *; }

-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# KJna
-dontwarn gen.*.jextract.**
-dontwarn dev.toastbits.kjna.**

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider { *; }

# KDiscordIPC
-keep class org.newsclub.** { *; }
#-keep class com.kohlschutter.junixsocket.** { *; }
-dontwarn org.newsclub.**

# Database
-keep class * implements java.sql.Driver
-keep class org.sqlite.** { *; }
-keep class org.sqlite.database.** { *; }

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-dontwarn com.sshtools.**
-dontwarn com.badlogic.**
-dontwarn com.jogamp.**
-dontwarn jogamp.**
-dontwarn com.sun.org.apache.**
-dontwarn org.apache.tika.**
-dontwarn org.apache.batik.script.jpython.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.bytedeco.javacpp.**

-dontwarn com.sun.beans.**
-dontwarn com.sun.rowset.**
-dontwarn com.sun.xml.internal.**
-dontwarn java.util.prefs.**
-dontwarn javax.imageio.metadata.**
-dontwarn javax.swing.plaf.synth.**
-dontwarn javax.xml.catalog.**
-dontwarn jdk.xml.internal.**
-dontwarn org.w3c.dom.html.**
-dontwarn com.multiplatform.webview.web.**
-dontwarn dev.toastbits.composekit.platform.**
-dontwarn dev.toastbits.kjna.runtime.**
-dontwarn games.spooky.gdx.nativefilechooser.desktop.**
-dontwarn org.jsoup.**
-dontwarn java.sql.**
