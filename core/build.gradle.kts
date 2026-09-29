plugins {
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    explicitApi()

    // Published libraries compile against an older language/API level and stdlib so consumers on Kotlin 2.2+ can use them.
    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
    coreLibrariesVersion = "2.2.0"

    androidLibrary {
        namespace = "com.omarshehe.tzaddress"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdkCore.get().toInt()

        withHostTest {}
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

mavenPublishing {
    // Group, version and the shared POM fields come from gradle.properties.
    coordinates(artifactId = "tz-address-core")
    pom {
        name.set("TZ Address Core")
        description.set("Immutable address model, repository interface and deterministic ids for Tanzanian addresses.")
    }
    publishToMavenCentral()
    // Signing is only required for a real release; a local publishToMavenLocal works without keys.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
}
