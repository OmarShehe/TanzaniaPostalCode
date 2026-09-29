plugins {
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "com.omarshehe.tzaddress.ui"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdkUi.get().toInt() // Compose needs API 23

        // Off by default in the AGP-KMP library plugin; without it the Compose resources (strings) never reach the AAR assets.
        androidResources { enable = true }

        withHostTest {}
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.runtime.saveable)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.forminput.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.get().kotlin.srcDir("src/uiTest/kotlin") // Compose UI tests run on desktop JVM only, not in the Android host test
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.resources {
    packageOfResClass = "com.omarshehe.tzaddress.ui.resources"
}

mavenPublishing {
    // Group, version and the shared POM fields come from gradle.properties.
    coordinates(artifactId = "tz-address-ui")
    pom {
        name.set("TZ Address UI")
        description.set("Compose Multiplatform address search field and cascading address picker.")
    }
    publishToMavenCentral()
    // Signing is only required for a real release; a local publishToMavenLocal works without keys.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
}
