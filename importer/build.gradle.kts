plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.pdfbox)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
    // Opt-in real-PDF checks: ./gradlew :importer:test -Ppdf=/path/to/tzPostcodeList.pdf
    providers.gradleProperty("pdf").orNull?.let { environment("TZ_PDF", it) }
}

// ./gradlew :importer:importPostcodes -Ppdf=/path/to/tzPostcodeList.pdf [-PsourceEdition=...] [-PgeneratedAt=...] [-Pforce] [-PoutDir=dataset]
tasks.register<JavaExec>("importPostcodes") {
    group = "import"
    description = "Parses the postcode PDF into dataset/tz-address.json plus a validation report."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.omarshehe.tzaddress.importer.ImportMainKt")
    val root = rootProject.layout.projectDirectory.asFile
    val props = listOf("pdf", "sourceEdition", "generatedAt", "outDir", "maxAnomalyRatio", "expectedRegions")
    val values = props.mapNotNull { name -> providers.gradleProperty(name).orNull?.let { "--$name=$it" } }
    val forced = providers.gradleProperty("force").isPresent
    val defaults = listOfNotNull(
        providers.gradleProperty("importer.maxAnomalyRatio").orNull?.let { "--maxAnomalyRatio=$it" },
        providers.gradleProperty("importer.expectedRegions").orNull?.let { "--expectedRegions=$it" },
    )
    args = defaults + values + listOfNotNull("--force".takeIf { forced }) + listOf("--root=${root.absolutePath}")
}
