plugins {
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

val addressDbGenerator by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    addressDbGenerator(project(":importer"))
}

// ./gradlew :data:generateAddressDb: builds the read-only DB from dataset/tz-address.json and checks it against dataset/import-report.md.
val generateAddressDb = tasks.register<JavaExec>("generateAddressDb") {
    group = "import"
    description = "Generates tz-address.db (bundled as a resource) from the committed dataset."
    val datasetDir = rootProject.layout.projectDirectory.dir("dataset")
    val datasetFile = datasetDir.file("tz-address.json")
    val reportFile = datasetDir.file("import-report.md")
    val outDir = layout.buildDirectory.dir("generated/addressDb")
    val outFile = outDir.map { it.file("tz-address.db") }
    inputs.files(datasetFile, reportFile)
    outputs.dir(outDir)
    classpath = addressDbGenerator
    mainClass.set("com.omarshehe.tzaddress.importer.GenerateDbMainKt")
    args(datasetFile.asFile.absolutePath, reportFile.asFile.absolutePath, outFile.get().asFile.absolutePath)
}

tasks.withType<Test>().configureEach {
    dependsOn(generateAddressDb)
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
        namespace = "com.omarshehe.tzaddress.data"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdkData.get().toInt() // sqlite-bundled needs API 23

        withHostTest {}
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        val dbResources = generateAddressDb.map { it.outputs.files.singleFile }
        jvmMain.get().resources.srcDir(dbResources)
        androidMain.get().resources.srcDir(dbResources)
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.androidx.sqlite)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io.core)
        }
        // The JVM uses sqlite-jdbc (native SQLite for macOS Intel and Apple silicon, Windows, Linux); the bundled driver has no Intel-macOS binary.
        jvmMain.dependencies {
            implementation(libs.sqlite.jdbc)
        }
        androidMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        listOf("iosArm64Main", "iosSimulatorArm64Main").forEach { name ->
            getByName(name).dependencies {
                implementation(libs.androidx.sqlite.bundled)
            }
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.kotlin.test)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

mavenPublishing {
    // Group, version and the shared POM fields come from gradle.properties.
    coordinates(artifactId = "tz-address-data")
    pom {
        name.set("TZ Address Data")
        description.set("Bundled read-only SQLite database of Tanzanian addresses with offline search, postcode lookup and browse.")
    }
    publishToMavenCentral()
    // Signing is only required for a real release; a local publishToMavenLocal works without keys.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
}
