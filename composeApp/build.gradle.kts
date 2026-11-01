import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation("com.google.mlkit:text-recognition:16.0.1")
            implementation("androidx.camera:camera-camera2:1.4.2")
            implementation("androidx.camera:camera-lifecycle:1.4.2")
            implementation("androidx.camera:camera-view:1.4.2")
            implementation("com.google.zxing:core:3.5.3")
        }
        commonMain.dependencies {
            implementation(project(":skyDesignSystem"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.coil.compose)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

room { schemaDirectory("$projectDir/schemas") }

// The standalone Kotlin test runner has no Swift app delegate to install the
// production crypto provider. Build the same Swift primitives into a test host.
val simulatorCryptoLibrary = layout.buildDirectory.file("ios-test-crypto/libMotoVaultTestCrypto.dylib")
val buildSimulatorCryptoBridge by tasks.registering(Exec::class) {
    val primitives = rootProject.file("iosApp/MotoVault/BackupCryptoPrimitives.swift")
    val bridge = rootProject.file("tools/ios-test-crypto/Bridge.swift")
    inputs.files(primitives, bridge)
    outputs.file(simulatorCryptoLibrary)
    doFirst { simulatorCryptoLibrary.get().asFile.parentFile.mkdirs() }
    commandLine("xcrun", "--sdk", "iphonesimulator", "swiftc", "-target", "arm64-apple-ios15.0-simulator",
        "-emit-library", primitives, bridge, "-o", simulatorCryptoLibrary.get().asFile)
}
tasks.named<KotlinNativeSimulatorTest>("iosSimulatorArm64Test") {
    dependsOn(buildSimulatorCryptoBridge)
    inputs.file(simulatorCryptoLibrary)
    environment("MOTOVAULT_TEST_CRYPTO_LIBRARY", simulatorCryptoLibrary.get().asFile.absolutePath)
    environment("SIMCTL_CHILD_MOTOVAULT_TEST_CRYPTO_LIBRARY", simulatorCryptoLibrary.get().asFile.absolutePath)
}

android {
    namespace = "com.motovault.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.motovault.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "com.motovault.app.storage.security.NativeAssetInstrumentation"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        create("privacyProbe") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".privacyprobe"
            versionNameSuffix = "-privacyprobe"
            matchingFallbacks += listOf("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    testBuildType = "privacyProbe"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
