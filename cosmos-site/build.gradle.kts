plugins {
    base
}

// Dependency bootstrap stays explicit: Gradle runs only the locked local tools.
val buildSite =
    tasks.register<Exec>("buildSite") {
        group = "build"
        description = "Generate the standalone Cosmos static site."
        workingDir(projectDir)
        commandLine("npm", "run", "build")
    }

val checkSite =
    tasks.register<Exec>("checkSite") {
        group = "verification"
        description = "Verify generated Cosmos routes, schemas, licenses and assets."
        dependsOn(buildSite)
        workingDir(projectDir)
        commandLine("npm", "run", "check")
    }

tasks.named("assemble") {
    dependsOn(buildSite)
}

tasks.named("check") {
    dependsOn(checkSite)
}

tasks.named<Delete>("clean") {
    delete(layout.projectDirectory.dir("dist"), layout.projectDirectory.dir(".wrangler"))
}
