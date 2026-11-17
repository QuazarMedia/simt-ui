pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "simtUiPublication"
                    url = file(providers.gradleProperty("uiRepository").getOrElse("../../build/maven-repository")).toURI()
                }
            }
            filter { includeGroup("app.simt") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "multiplatform-consumer"
