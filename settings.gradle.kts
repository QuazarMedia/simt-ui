pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

nmcpSettings {
    centralPortal {
        username = providers.gradleProperty("centralPortalUsername").orNull
            ?: System.getenv("CENTRAL_PORTAL_USERNAME")
            ?: ""
        password = providers.gradleProperty("centralPortalPassword").orNull
            ?: System.getenv("CENTRAL_PORTAL_PASSWORD")
            ?: ""
        publishingType = "AUTOMATIC"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "simt-ui"
include(":app-ui", ":cosmos-spec", ":cosmos-site")
