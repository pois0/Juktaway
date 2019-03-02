-dontwarn sun.misc.Unsafe

# for EventBus
-keepclassmembers class ** {
    public void onEvent*(**);
}

# for Gson
-keepattributes Signature
-keepattributes *Annotation*

-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }

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
