@file:Suppress("UnstableApiUsage")

import com.aliucord.gradle.AliucordExtension
import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.aliucord.plugin) apply true
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.shadow) apply false
}

subprojects {
    val libs = rootProject.libs

    apply {
        plugin(libs.plugins.android.library.get().pluginId)
        plugin(libs.plugins.aliucord.plugin.get().pluginId)
        plugin(libs.plugins.kotlin.android.get().pluginId)
        plugin(libs.plugins.ktlint.get().pluginId)
    }

    configure<LibraryExtension> {
        compileSdk = 36
        namespace = "moe.lava.awoocord"

        defaultConfig {
            minSdk = 21
        }

        buildFeatures {
            aidl = false
            buildConfig = true
            renderScript = false
            shaders = false
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    configure<AliucordExtension> {
        author("cilly", 368398754077868032L, hyperlink = false)
        github("https://github.com/cillynder/Awoocord")
    }

    configure<KtlintExtension> {
        version.set(libs.versions.ktlint.asProvider())

        coloredOutput.set(true)
        outputColorName.set("RED")
        ignoreFailures.set(true)
    }

    configure<KotlinAndroidExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            optIn.add("kotlin.RequiresOptIn")
        }
    }

    @Suppress("unused")
    dependencies {
        val compileOnly by configurations
        val implementation by configurations

        compileOnly(libs.discord)
        compileOnly(libs.aliucord)
        compileOnly(libs.aliuhook)
        compileOnly(libs.kotlin.stdlib)
    }
}
