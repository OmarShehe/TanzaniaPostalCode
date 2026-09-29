plugins {
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

    androidLibrary {
        namespace = "com.omarshehe.tzaddress.data"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdkCore.get().toInt()

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
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io.core)
        }
        jvmTest.dependencies {
            // The bundled driver has no Intel-macOS binary; tests run on this JDBC-backed driver instead.
            implementation(libs.sqlite.jdbc)
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
