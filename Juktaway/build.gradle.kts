val xCoroutinesVersion = "1.3"
val xSerializationVersion = "0.10.0"
val ankoVersion = "0.10.8"
val ktorVersion = "1.2.3-rc"
val eventbusVersion = "2.2.0"
val universalImageLoaderVersion = "1.9.5"
val pageIndicatorViewVersion = "1.0.2@aar"
val penicillinVersion = "4.2.3-eap-36"
val jsonKtVersion = "5.0.0-eap-4"

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    id("kotlinx-serialization")
    id("com.cookpad.android.licensetools")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-android", xCoroutinesVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", xSerializationVersion)
    implementation("org.jetbrains.anko", "anko", ankoVersion)
    implementation("io.ktor", "ktor-client-android", ktorVersion)
    implementation("de.greenrobot", "eventbus", eventbusVersion)
    implementation("com.nostra13.universalimageloader", "universal-image-loader", universalImageLoaderVersion)
    implementation("com.romandanylyk", "pageindicatorview", pageIndicatorViewVersion)
    implementation("jp.nephy", "penicillin", penicillinVersion)
    implementation("jp.nephy", "jsonKt", jsonKtVersion)
}

android {
    compileSdkVersion(28)
}
