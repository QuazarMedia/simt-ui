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
    signing
}

@OptIn(ExperimentalAbiValidation::class)
kotlin {
    explicitApi()
    abiValidation {
        keepLocallyUnsupportedTargets.set(false)
    }
    jvmToolchain(21)
    android {
        namespace = "app.simt.ui"
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
                implementation(libs.kotlinx.coroutines.test)
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
        named("androidMain") {
            dependencies {
                // NotificationCompat, ContextCompat and the annotation types used by the
                // notifications kit; part of the public API (NotificationCompat.Action in
                // NotificationSpec / NotificationActionBuilder), hence api scope.
                api(libs.androidx.core.ktx)
                // Play in-app updates backing PlayCoreUpdateChecker; kept out of
                // the public API surface so consumers never see play-core types.
                implementation(libs.play.app.update.ktx)
                implementation(libs.kotlinx.coroutines.play.services)
                // collectAsStateWithLifecycle for the global toast host; internal to the
                // host composable, never part of the public API.
                implementation(libs.androidx.lifecycle.runtime.compose)
                // Credential Manager + Google ID backing the sign-in feature; kept out of
                // the public ABI surface - the launcher exposes only String callbacks.
                implementation(libs.androidx.credentials)
                implementation(libs.androidx.credentials.play.services.auth)
                implementation(libs.googleid)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
}

compose.resources {
    packageOfResClass = "app.simt.ui.resources"
    publicResClass = false
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}

val legalNoticeEntries = listOf("LICENSE", "LICENSE-DOCS", "NOTICE", "app-ui/source-provenance.json").sorted()

publishing {
    repositories {
        maven {
            name = "LocalValidation"
            url = rootProject.layout.buildDirectory.dir("maven-repository").get().asFile.toURI()
        }
    }
    publications.withType<MavenPublication>().configureEach {
        // The signing plugin writes each .asc beside the file it signs, so a
        // shared javadoc jar or legal-notices archive makes every publication's
        // Sign task claim the same output paths.
        val artifactsDirectory = layout.buildDirectory.dir("publication-artifacts/$name")
        val apiDocumentationJar =
            tasks.register<Jar>("${name}ApiDocumentationJar") {
                dependsOn("dokkaGeneratePublicationHtml")
                from(layout.buildDirectory.dir("dokka/html"))
                archiveClassifier.set("javadoc")
                destinationDirectory.set(artifactsDirectory)
                isPreserveFileTimestamps = false
                isReproducibleFileOrder = true
            }
        val legalNoticesArchive = artifactsDirectory.map { it.file("legal-notices.zip") }
        val legalNoticesZip =
            tasks.register("${name}LegalNoticesZip") {
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
            developers {
                developer {
                    id = "saidelimam"
                    name = "Said Elimam"
                    email = "contact@simt.app"
                }
            }
            scm {
                connection = "scm:git:git://github.com/QuazarMedia/simt-ui.git"
                developerConnection = "scm:git:ssh://github.com:QuazarMedia/simt-ui.git"
                url = "https://github.com/QuazarMedia/simt-ui/tree/main"
            }
        }
    }
}

signing {
    val signingKey = providers.gradleProperty("signingKey").orNull
    val signingPassword = providers.gradleProperty("signingPassword").orNull
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
