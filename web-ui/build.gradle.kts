plugins {
    base
}

val checkWebUi =
    tasks.register<Exec>("checkWebUi") {
        group = "verification"
        description = "Run the framework-agnostic SIMT web UI tests."
        workingDir(projectDir)
        commandLine("npm", "run", "check")
    }

tasks.named("check") {
    dependsOn(checkWebUi)
}
