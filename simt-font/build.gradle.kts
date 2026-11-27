plugins {
    base
}

// Offline verification of the Simt Font binaries: SHA-256 manifest, name-table
// license declarations and license files. Uses only the Python 3 standard library.
val checkFontIntegrity = tasks.register<Exec>("checkFontIntegrity") {
    group = "verification"
    description = "Verify the Simt Font binaries against checksums.sha256, their name tables and the OFL license files."
    workingDir(projectDir)
    commandLine("python3", "scripts/check-integrity.py")
}

tasks.named("check") {
    dependsOn(checkFontIntegrity)
}
