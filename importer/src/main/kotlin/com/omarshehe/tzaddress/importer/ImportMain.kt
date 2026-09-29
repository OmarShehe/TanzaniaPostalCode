package com.omarshehe.tzaddress.importer

import java.io.File
import java.time.Instant
import kotlin.system.exitProcess

private const val DATASET_VERSION = "1"

fun main(args: Array<String>) {
    val options = args.filter { it.startsWith("--") }.associate {
        val (key, value) = (it.removePrefix("--") + "=").split("=", limit = 2)
        key to value.removeSuffix("=")
    }
    val pdf = options["pdf"]?.let(::File)
    if (pdf == null || !pdf.isFile) {
        System.err.println("Usage: ./gradlew :importer:importPostcodes -Ppdf=/path/to/tzPostcodeList.pdf [-PsourceEdition=..] [-PgeneratedAt=..] [-Pforce] [-PoutDir=dataset]")
        System.err.println(if (pdf == null) "Missing -Ppdf." else "PDF not found: ${pdf.path}")
        exitProcess(2)
    }
    val root = File(options["root"] ?: ".")
    val outDir = File(options["outDir"] ?: "dataset").let { if (it.isAbsolute) it else File(root, it.path) }
    val policy = Policy(
        expectedRegions = options["expectedRegions"]?.toInt() ?: 30,
        maxAnomalyRatio = options["maxAnomalyRatio"]?.toDouble() ?: 0.005,
    )
    val force = "force" in options

    val extracted = PdfWordExtractor.extract(pdf)
    val result = ImportPipeline.parse(extracted)
    val info = InfoDto(
        version = DATASET_VERSION,
        sourceEdition = options["sourceEdition"]?.takeIf { it.isNotBlank() } ?: extracted.creationDate ?: "unknown",
        generatedAt = options["generatedAt"]?.takeIf { it.isNotBlank() } ?: Instant.now().toString(),
    )
    val dataset = ImportPipeline.toDataset(result, info)
    val validation = Validator.validate(dataset, result.suspectAnomalyCount, result.dataLineCount, policy)

    outDir.mkdirs()
    File(outDir, "import-report.md").writeText(ReportWriter.report(dataset, validation, result.anomalies, result.dataLineCount))
    File(outDir, "import-anomalies.csv").writeText(ReportWriter.anomaliesCsv(result.anomalies))
    if (validation.passed || force) DatasetWriter.write(File(outDir, "tz-address.json"), dataset)

    println("Regions ${dataset.regions.size}, anomalies ${result.anomalies.size} of ${result.dataLineCount} lines: ${if (validation.passed) "PASSED" else "FAILED"}")
    validation.violations.forEach { println("  ${it.kind}: ${it.message}") }
    println("Output: ${outDir.path}")
    if (!validation.passed && !force) exitProcess(1)
}
