buildscript {
    val kotlinVersion by extra { "1.3.11" }
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven(url = "https://dl.bintray.com/kotlin/ktor")
        maven(url = "https://maven.google.com")
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }

    dependencies {
        classpath('com.android.tools.build:gradle:3.2.1')
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
}

subprojects {
    repositories {
        maven(url = "http://dl.bintray.com/kotlin/kotlin-eap-1.2")
        maven(url = "https://maven.google.com")
        maven(url = "https://kotlin.bintray.com/kotlinx")
        maven(url = "https://dl.bintray.com/kotlin/ktor")
        mavenCentral()
        jcenter()
    }


}

task("wrapper", Delete::class) {
    gradleVersion = "5.0"
}