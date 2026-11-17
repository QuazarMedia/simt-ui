plugins {
    base
}

// Dependency bootstrap is manual so verification never implicitly uses the network.
val checkSnapshot =
    tasks.register<Exec>("checkSnapshot") {
        group = "verification"
        description = "Verify the frozen private Cosmos snapshot, allowing only the four recorded upstream schema defects."
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
    description = "Strict canonical schema gate: currently fails on all four upstream scoped sections.minItems defects."
    workingDir(projectDir)
    commandLine("npm", "run", "check:canonical-schema")
}

val privateSnapshotZip =
    tasks.register<Exec>("privateSnapshotZip") {
        group = "distribution"
        description = "Build a deterministic local private ZIP; public redistribution remains blocked."
        dependsOn(checkSnapshot)
        workingDir(projectDir)
        commandLine("npm", "run", "archive:private")
    }

tasks.named("check") {
    // Do not aggregate the expected-failing strict gate into private development checks.
    dependsOn(checkSnapshot, snapshotTest)
}

tasks.named("assemble") {
    dependsOn(privateSnapshotZip)
}
