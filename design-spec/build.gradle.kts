plugins {
    base
}

// Dependency bootstrap is manual so verification never implicitly uses the network.
val checkSnapshot =
    tasks.register<Exec>("checkSnapshot") {
        group = "verification"
        description = "Verify the frozen licensed Cosmos snapshot and every declared canonical schema."
        workingDir(projectDir)
        commandLine("npm", "run", "check:integrity")
    }

val snapshotTest =
    tasks.register<Exec>("snapshotTest") {
        group = "verification"
        description = "Test integrity, semantic tampering, offline schemas and actual archive repeatability."
        workingDir(projectDir)
        commandLine("npm", "test")
    }

tasks.register<Exec>("checkCanonicalSchema") {
    group = "verification"
    description = "Validate the union and all scoped exports against their unchanged committed canonical schemas."
    workingDir(projectDir)
    commandLine("npm", "run", "check:canonical-schema")
}

val privateSnapshotZip =
    tasks.register<Exec>("privateSnapshotZip") {
        group = "distribution"
        description = "Build a deterministic local ZIP retaining licenses and attribution; no upload or release approval."
        dependsOn(checkSnapshot)
        workingDir(projectDir)
        commandLine("npm", "run", "archive:private")
    }

tasks.named("check") {
    dependsOn(checkSnapshot, snapshotTest, "checkCanonicalSchema")
}

tasks.named("assemble") {
    dependsOn(privateSnapshotZip)
}
