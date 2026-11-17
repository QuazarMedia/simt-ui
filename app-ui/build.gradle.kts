import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
    android {
        namespace = "app.shared.ui"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        androidResources { enable = true }
        withHostTest { isIncludeAndroidResources = true }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    iosArm64 {
        compilerOptions {
            freeCompilerArgs.add("-Xklib-relative-path-base=${rootDir.absolutePath}")
        }
    }
    iosSimulatorArm64 {
        compilerOptions {
            freeCompilerArgs.add("-Xklib-relative-path-base=${rootDir.absolutePath}")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.resources)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.androidx.test.espresso.core)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.activity.compose)
            }
        }
        named("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.androidx.test.espresso.core)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

compose.resources {
    packageOfResClass = "app.shared.ui.resources"
    publicResClass = false
}

publishing {
    repositories {
        maven {
            name = "LocalValidation"
            url = rootProject.layout.buildDirectory.dir("maven-repository").get().asFile.toURI()
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "SIMT UI"
            description = "Shared Android and iOS Compose UI primitives"
            url = "https://github.com/QuazarMedia/simt-ui"
            scm {
                connection = "scm:git:https://github.com/QuazarMedia/simt-ui.git"
                url = "https://github.com/QuazarMedia/simt-ui"
            }
        }
    }
}
