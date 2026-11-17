import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.kotlin.multiplatform.library") version "9.5.1"
    id("org.jetbrains.kotlin.multiplatform") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
}

kotlin {
    jvmToolchain(21)
    android {
        namespace = "app.simt.fixture.multiplatform"
        compileSdk = 37
        minSdk = 29
        androidResources { enable = true }
        withHostTest { isIncludeAndroidResources = true }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "UiConsumer"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation("app.simt:app-ui:${providers.gradleProperty("uiVersion").getOrElse("0.1.0-SNAPSHOT")}")
        }
        named("androidHostTest") {
            kotlin.srcDir("src/androidSmokeTest/kotlin")
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.17")
                implementation("androidx.compose.ui:ui-test-junit4:1.12.1")
                implementation("androidx.compose.ui:ui-test-manifest:1.12.1")
                implementation("androidx.test.espresso:espresso-core:3.7.0")
                implementation("androidx.activity:activity-compose:1.13.0")
            }
        }
        named("androidDeviceTest") {
            kotlin.srcDir("src/androidSmokeTest/kotlin")
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("androidx.test:runner:1.7.0")
                implementation("androidx.compose.ui:ui-test-junit4:1.12.1")
                implementation("androidx.compose.ui:ui-test-manifest:1.12.1")
                implementation("androidx.test.espresso:espresso-core:3.7.0")
                implementation("androidx.activity:activity-compose:1.13.0")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
    maxParallelForks = 1
}
