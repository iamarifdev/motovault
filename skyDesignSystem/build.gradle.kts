plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Compile the supplied source with the host toolchain, including its Intel iOS target.
val skyRoot = providers.gradleProperty("skyDesignSystemDir")
    .orElse("../apks/sky-design-system")
    .map { rootProject.file(it) }
val skyCommon = skyRoot.map { it.resolve("design-system/src/commonMain") }
require(skyCommon.get().resolve("kotlin/dev/sky/design/SkyTheme.kt").isFile) {
    "Sky Design System is missing. Place it at ../apks/sky-design-system or set -PskyDesignSystemDir=/absolute/path."
}

kotlin {
    compilerOptions.optIn.add("org.jetbrains.compose.resources.ExperimentalResourceApi")
    androidTarget { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain {
            kotlin.srcDir(skyCommon.map { it.resolve("kotlin") })
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                implementation(compose.components.resources)
                implementation(libs.coil.compose)
                implementation("io.coil-kt.coil3:coil-svg:3.0.0-rc01")
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "dev.sky.design"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig.minSdk = libs.versions.android.minSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "dev.sky.design.resources"
    generateResClass = always
    customDirectory("commonMain", layout.dir(skyCommon.map { it.resolve("composeResources") }))
}
