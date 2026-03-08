# JGit
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# SnakeYAML Engine
-keep class org.snakeyaml.engine.** { *; }
-dontwarn org.snakeyaml.engine.**

# ktoml
-keep class com.akuleshov7.ktoml.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **.*$serializer { static **.*$serializer INSTANCE; }

# JSch (SSH transport for JGit — uses reflection to load algorithm and kex factories)
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# Google Tink / security-crypto (EncryptedSharedPreferences)
# errorprone annotations are compile-only and not present at runtime
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Strip verbose debug/verbose logs in release builds (sensitive path & URL data)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
