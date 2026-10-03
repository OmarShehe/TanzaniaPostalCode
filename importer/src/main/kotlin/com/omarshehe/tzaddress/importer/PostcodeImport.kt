package com.omarshehe.tzaddress.importer

import java.io.File
import java.time.Instant

/** The `importPostcodes` run: parses the postcode PDF into `tz-address.json` plus a report. */
object PostcodeImport {

    /** Returns the process exit code: 0 passed, 1 failed validation (dataset not written unless forced), 2 usage error. */
    fun run(options: ImportOptions, log: (String) -> Unit = ::println): Int {
        val pdf = options.pdf
        if (pdf == null || !pdf.isFile) {
            log("Usage: ./gradlew :importer:importPostcodes -Ppdf=/path/to/tzPostcodeList.pdf [-PsourceEdition=..] [-PgeneratedAt=..] [-Pforce] [-PoutDir=dataset] [-PexpectedRegions=30] [-PmaxAnomalyRatio=0.005]")
            log(if (pdf == null) "Missing -Ppdf." else "PDF not found: ${pdf.path}")
            return 2
        }

        val extracted = PdfWordExtractor.extract(pdf)
        val result = ImportPipeline.parse(extracted)
        val info = InfoDto(
            version = DatasetVersion.CURRENT,
            sourceEdition = options.sourceEdition ?: extracted.creationDate ?: "unknown",
            generatedAt = options.generatedAt ?: Instant.now().toString(),
        )
        val datasetFile = File(options.outDir, "tz-address.json")
        // The postcode list has no positions; keep the ones an earlier ward-points import wrote.
        val existing = datasetFile.takeIf { it.isFile }?.let { DatasetWriter.fromJson(it.readText()) }
        val dataset = CarryOverPoints.merge(ImportPipeline.toDataset(result, info), existing)
        val validation = Validator.validate(dataset, result.suspectAnomalyCount, result.dataLineCount, options.policy)
        val writeDataset = validation.passed || options.force

        options.outDir.mkdirs()
        File(options.outDir, "import-report.md").writeText(ReportWriter.report(dataset, validation, result.anomalies, result.dataLineCount, writeDataset))
        File(options.outDir, "import-anomalies.csv").writeText(ReportWriter.anomaliesCsv(result.anomalies))
        if (writeDataset) DatasetWriter.write(datasetFile, dataset)

        log("Regions ${dataset.regions.size}, anomalies ${result.anomalies.size} of ${result.dataLineCount} lines: ${if (validation.passed) "PASSED" else "FAILED"}")
        validation.violations.forEach { log("  ${it.kind}: ${it.message}") }
        log(if (writeDataset) "Output: ${options.outDir.path}" else "tz-address.json was NOT written; any existing copy in ${options.outDir.path} is from an earlier run.")
        return if (!validation.passed && !options.force) 1 else 0
    }
}
