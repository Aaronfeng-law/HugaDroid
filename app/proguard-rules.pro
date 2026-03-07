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
