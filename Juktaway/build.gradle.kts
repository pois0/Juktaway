import java.util.Properties
import kotlin.collections.listOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val xCoroutinesVersion = "1.3.0"
val xSerializationVersion = "0.10.0"

val androidxVersion = "1.0.0"

val ankoVersion = "0.10.8"
val ktorVersion = "1.2.3-rc"
val eventbusVersion = "2.2.0"
val universalImageLoaderVersion = "1.9.5"
val pageIndicatorViewVersion = "1.0.3"
val penicillinVersion = "4.2.3-eap-36"
val jsonKtVersion = "5.0.0-eap-4"

val properties = Properties().apply {
    load(project.rootProject.file("local.properties").inputStream())
}

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

    implementation(kotlinx("coroutines-android", xCoroutinesVersion))
    implementation(kotlinx("serialization-runtime", xSerializationVersion))

    implementation(androidx("core", "core-ktx"))
    implementation(androidx("fragment"))
    implementation(androidx("appcompat"))

    implementation("org.jetbrains.anko", "anko-commons", ankoVersion)
    implementation("org.jetbrains.anko", "anko-sqlite", ankoVersion)

    implementation("io.ktor", "ktor-client-android", ktorVersion)
    implementation("de.greenrobot", "eventbus", eventbusVersion)
    implementation("com.nostra13.universalimageloader", "universal-image-loader", universalImageLoaderVersion)
    implementation("com.romandanylyk", "pageindicatorview", pageIndicatorViewVersion)

    implementation("jp.nephy", "penicillin", penicillinVersion)
    implementation("jp.nephy", "jsonkt", jsonKtVersion)
}

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(28)

        multiDexEnabled = true
        versionCode = 32
        versionName = "3.1.0"
        applicationId = "net.slash_omega.juktaway"

        proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }

    signingConfigs {
        create("release") {
            storeFile = file(properties["RELEASE_STORE_LOCATION"]!!)
            storePassword = properties["RELEASE_STORE_PASSWORD"]!! as String
            keyAlias = properties["RELEASE_KEY_ALIAS"]!! as String
            keyPassword = properties["RELEASE_KEY_PASSWORD"]!! as String
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lintOptions {
        isAbortOnError = false
    }

    packagingOptions {
        excludes = setOf(
                "META-INF/LICENSE.txt",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/MANIFEST.MF",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/kotlinx-io.kotlin_module",
                "META-INF/atomicfu.kotlin_module",
                "META-INF/kotlinx-coroutines-io.kotlin_module",
                "META-INF/kotlinx-coroutines-core.kotlin_module",
                "META-INF/kotlin-logging.kotlin_module",
                "META-INF/proguard/coroutines.pro"
        )
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false

            applicationIdSuffix = ".debug"
        }
    }

    dataBinding {
        isEnabled = true
    }
}

gradle.projectsEvaluated {
    tasks.withType(JavaCompile::class.java) {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }

    tasks.withType(KotlinCompile::class.java) {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
            jvmTarget = "1.8"
        }
    }
}

fun DependencyHandler.kotlinx(module: String, version: String? = null): String {
    val versionString = version?.let { ":$it" }.orEmpty()

    return "org.jetbrains.kotlinx:kotlinx-$module$versionString"
}

fun DependencyHandler.androidx(repository: String, module: String? = repository, version: String? = androidxVersion): String {
    val versionString = version?.let { ":$it" }.orEmpty()

    return "androidx.$repository:$module$versionString"
}
