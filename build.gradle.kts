import com.aliucord.gradle.AliucordExtension
import com.android.build.gradle.BaseExtension

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliucord.com/snapshots")
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.1.3")
        classpath("com.aliucord:gradle:main-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.aliucord.com/snapshots")
    }
}

fun Project.aliucord(configuration: AliucordExtension.() -> Unit) = extensions.getByName<AliucordExtension>("aliucord").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "com.aliucord.gradle")
    apply(plugin = "kotlin-android")

    aliucord {
        author("Lava", 368398754077868032L)
        updateUrl.set("https://raw.githubusercontent.com/LavaDesu/Awoocord/builds/updater.json")
        buildUrl.set("https://raw.githubusercontent.com/LavaDesu/Awoocord/builds/%s.zip")
    }

    android {
        compileSdkVersion(31)

        defaultConfig {
            minSdk = 24
            targetSdk = 31
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "11"
                // Disables some unnecessary features
                freeCompilerArgs = freeCompilerArgs +
                        "-Xno-call-assertions" +
                        "-Xno-param-assertions" +
                        "-Xno-receiver-assertions"
            }
        }
    }

    dependencies {
        val discord by configurations
        val implementation by configurations

        // Stubs for all Discord classes
        discord("com.discord:discord:aliucord-SNAPSHOT")
        implementation("com.aliucord:Aliucord:main-SNAPSHOT")

        implementation("androidx.appcompat:appcompat:1.4.0")
        implementation("com.google.android.material:material:1.4.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.2")
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
