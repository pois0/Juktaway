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

# http://qiita.com/petitviolet/items/1b709f3f0db2659a271a
-keepnames class net.slashOmega.juktaway.model.** { *; }
-keepnames class net.slashOmega.juktaway.settings.** { *; }

# *** Debug ***
# -renamesourcefileattribute SourceFile
# -keepattributes SourceFile,LineNumberTable
# *** Debug ***
