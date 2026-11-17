import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dokka)
    `maven-publish`
}

@OptIn(ExperimentalAbiValidation::class)
kotlin {
    explicitApi()
    abiValidation {
        keepLocallyUnsupportedTargets.set(false)
    }
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
            implementation(libs.compose.animation)
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

dokka {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}

val apiDocumentationJar =
    tasks.register<Jar>("apiDocumentationJar") {
        dependsOn("dokkaGeneratePublicationHtml")
        from(layout.buildDirectory.dir("dokka/html"))
        archiveClassifier.set("javadoc")
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

val legalNoticeEntries = listOf("LICENSE", "LICENSE-DOCS", "NOTICE", "app-ui/source-provenance.json").sorted()
val legalNoticesArchive = layout.buildDirectory.file("legal-notices/legal-notices.zip")
val legalNoticesZip =
    tasks.register("legalNoticesZip") {
        inputs.files(legalNoticeEntries.map { rootProject.file(it) })
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.file(legalNoticesArchive)
        doLast {
            val output = legalNoticesArchive.get().asFile
            output.parentFile.mkdirs()
            ZipOutputStream(output.outputStream()).use { archive ->
                legalNoticeEntries.forEach { relative ->
                    val entry = ZipEntry(relative)
                    entry.setTimeLocal(LocalDateTime.of(1980, 1, 1, 0, 0))
                    archive.putNextEntry(entry)
                    rootProject.file(relative).inputStream().use { it.copyTo(archive) }
                    archive.closeEntry()
                }
            }
        }
    }

publishing {
    repositories {
        maven {
            name = "LocalValidation"
            url = rootProject.layout.buildDirectory.dir("maven-repository").get().asFile.toURI()
        }
    }
    publications.withType<MavenPublication>().configureEach {
        artifact(apiDocumentationJar)
        artifact(legalNoticesArchive) {
            builtBy(legalNoticesZip)
            classifier = "legal-notices"
            extension = "zip"
        }
        pom {
            name = "SIMT UI"
            description = "Shared Android and iOS Compose UI primitives"
            url = "https://github.com/QuazarMedia/simt-ui"
            licenses {
                license {
                    name = "Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    distribution = "repo"
                }
            }
            scm {
                connection = "scm:git:https://github.com/QuazarMedia/simt-ui.git"
                url = "https://github.com/QuazarMedia/simt-ui"
            }
        }
    }
}
