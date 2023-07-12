-dontobfuscate
-keep class com.toasterofbread.spmp.** { *; }

# Klaxon
-keep class kotlin.reflect.jvm.internal.**
-keep class com.beust.klaxon.** { *; }
-keep interface com.beust.klaxon.** { *; }
-keep class kotlin.Metadata { *; }

# NewPipe
-keep class org.mozilla.javascript.**

# KizzyRPC
# See https://github.com/google/gson/blob/6d9c3566b71900c54644a9f71ce028696ee5d4bd/examples/android-proguard-example/proguard.cfg
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken