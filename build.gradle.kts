buildscript {
    val kotlinVersion = "1.3.50"
    val buildToolVersion = "3.5.0"
    val androidLicenseToolsVersion = "1.7.0"

    repositories {
        mavenCentral()
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build", "gradle", buildToolVersion)
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
        classpath("com.cookpad.android.licensetools", "license-tools-plugin", androidLicenseToolsVersion)
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        jcenter()
        maven("http://dl.bintray.com/populov/maven")
        maven("https://maven.google.com")
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://dl.bintray.com/kotlin/ktor")
        maven("https://dl.bintray.com/nephyproject/stable")
        maven("https://dl.bintray.com/nephyproject/dev")
    }
}
