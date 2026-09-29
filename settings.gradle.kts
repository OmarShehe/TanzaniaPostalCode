pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // forminput-compose is published locally for now (./gradlew publishToMavenLocal in the FormInputs repo).
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "TanzaniaPostalCode"
include(":core")
include(":importer")
include(":data")
include(":ui")
include(":app")
