package com.omarshehe.tzaddress.importer

import java.io.File
import java.time.Instant
import kotlin.system.exitProcess

private const val DATASET_VERSION = "1"

fun main(args: Array<String>) {
    val options = ImportOptions.parse(args)
    val pdf = options.pdf
    if (pdf == null || !pdf.isFile) {
        System.err.println("Usage: ./gradlew :importer:importPostcodes -Ppdf=/path/to/tzPostcodeList.pdf [-PsourceEdition=..] [-PgeneratedAt=..] [-Pforce] [-PoutDir=dataset] [-PexpectedRegions=30] [-PmaxAnomalyRatio=0.005]")
        System.err.println(if (pdf == null) "Missing -Ppdf." else "PDF not found: ${pdf.path}")
        exitProcess(2)
    }

    val extracted = PdfWordExtractor.extract(pdf)
    val result = ImportPipeline.parse(extracted)
    val info = InfoDto(
        version = DATASET_VERSION,
        sourceEdition = options.sourceEdition ?: extracted.creationDate ?: "unknown",
        generatedAt = options.generatedAt ?: Instant.now().toString(),
    )
    val dataset = ImportPipeline.toDataset(result, info)
    val validation = Validator.validate(dataset, result.suspectAnomalyCount, result.dataLineCount, options.policy)
    val writeDataset = validation.passed || options.force

    options.outDir.mkdirs()
    File(options.outDir, "import-report.md").writeText(ReportWriter.report(dataset, validation, result.anomalies, result.dataLineCount, writeDataset))
    File(options.outDir, "import-anomalies.csv").writeText(ReportWriter.anomaliesCsv(result.anomalies))
    if (writeDataset) DatasetWriter.write(File(options.outDir, "tz-address.json"), dataset)

    println("Regions ${dataset.regions.size}, anomalies ${result.anomalies.size} of ${result.dataLineCount} lines: ${if (validation.passed) "PASSED" else "FAILED"}")
    validation.violations.forEach { println("  ${it.kind}: ${it.message}") }
    println(if (writeDataset) "Output: ${options.outDir.path}" else "tz-address.json was NOT written; any existing copy in ${options.outDir.path} is from an earlier run.")
    if (!validation.passed && !options.force) exitProcess(1)
}
