-dontwarn sun.misc.Unsafe

# for EventBus
-keepclassmembers class ** {
    public void onEvent*(**);
}

# for kotlinx-serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.yourcompany.yourpackage.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}

# for Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# http://qiita.com/petitviolet/items/1b709f3f0db2659a271a
-keepnames class net.slash_omega.juktaway.model.** { *; }
-keepnames class net.slash_omega.juktaway.settings.** { *; }
-keepnames class net.slash_omega.juktaway.fragment.** { *; }
-dontwarn net.slash_omega.juktaway.fragment.**

# *** Debug ***
# -renamesourcefileattribute SourceFile
# -keepattributes SourceFile,LineNumberTable
# *** Debug ***
