plugins {
    base
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "app.simt"
    version = providers.gradleProperty("VERSION_NAME").get()
}

spotless {
    kotlin {
        target("app-ui/src/**/*.kt", "examples/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "app-ui/*.gradle.kts", "cosmos-spec/*.gradle.kts", "examples/**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("config") {
        target(".gitignore", ".editorconfig", "gradle.properties", "gradle/*.toml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

val auditTests =
    tasks.register<Exec>("auditTests") {
        group = "verification"
        workingDir(rootDir)
        commandLine("python3", "-B", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_check_publication.py")
    }

val sourceAudit =
    tasks.register<Exec>("sourceAudit") {
        group = "verification"
        workingDir(rootDir)
        commandLine("python3", "-B", "scripts/check_publication.py", "source", "--root", rootDir.absolutePath)
    }

val publicationAudit =
    tasks.register<Exec>("publicationAudit") {
        group = "verification"
        dependsOn(":app-ui:publishAllPublicationsToLocalValidationRepository")
        workingDir(rootDir)
        commandLine(
            "python3",
            "-B",
            "scripts/check_publication.py",
            "publication",
            "--repo",
            layout.buildDirectory.dir("maven-repository").get().asFile.absolutePath,
            "--version",
            project.version.toString(),
        )
    }

tasks.named("check") {
    dependsOn(":app-ui:check", ":cosmos-spec:check", auditTests, sourceAudit)
}

tasks.register("localQuality") {
    group = "verification"
    dependsOn("check", "spotlessCheck", publicationAudit)
}

val publicationRepository = layout.buildDirectory.dir("maven-repository").get().asFile.absolutePath
val publicationVersion = project.version.toString()

tasks.register<Exec>("stageIntegration") {
    group = "verification"
    dependsOn("localQuality")
    workingDir(rootDir)
    commandLine(
        "python3",
        "-B",
        "scripts/check_publication.py",
        "stage-integration",
        "--root",
        rootDir.absolutePath,
        "--repo",
        publicationRepository,
        "--version",
        publicationVersion,
        "--output",
        layout.buildDirectory.file("simt-ui-$publicationVersion-private-integration.zip").get().asFile.absolutePath,
    )
}

listOf("releaseCheck" to "release", "stageRelease" to "stage").forEach { (taskName, mode) ->
    tasks.register<Exec>(taskName) {
        group = "verification"
        dependsOn(if (mode == "stage") "releaseCheck" else "localQuality")
        workingDir(rootDir)
        doFirst {
            val arguments =
                mutableListOf(
                    "python3",
                    "-B",
                    "scripts/check_publication.py",
                    mode,
                    "--root",
                    rootDir.absolutePath,
                    "--repo",
                    publicationRepository,
                    "--version",
                    publicationVersion,
                )
            if (mode == "stage") {
                arguments.addAll(listOf("--output", layout.buildDirectory.file("simt-ui-$publicationVersion-unsigned.zip").get().asFile.absolutePath))
            }
            commandLine(arguments)
        }
    }
}
