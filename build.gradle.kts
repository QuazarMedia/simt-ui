plugins {
    base
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless)
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

tasks.named("check") {
    dependsOn(":app-ui:check", ":cosmos-spec:check")
}
