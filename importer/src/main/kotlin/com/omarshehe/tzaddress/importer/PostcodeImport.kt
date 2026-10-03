package com.omarshehe.tzaddress.importer

import java.io.File
import java.time.Instant

/** The `importPostcodes` run: parses the postcode PDF(s) into `tz-address.json` plus a report. */
object PostcodeImport {

    private const val MAINLAND_EDITION = "2016-04-22 (Gazette Notice 240)"

    /** Returns the process exit code: 0 passed, 1 failed validation (dataset not written unless forced), 2 usage error. */
    fun run(options: ImportOptions, log: (String) -> Unit = ::println): Int {
        val pdf = options.pdf
        val dir = options.pdfDir
        if (pdf == null || !pdf.isFile) {
            log("Usage: ./gradlew :importer:importPostcodes -Ppdf=/path/to/tzPostcodeList.pdf [-PpdfDir=/path/to/regional-pdfs] [-PsourceEdition=..] [-PgeneratedAt=..] [-Pforce] [-PoutDir=dataset] [-PexpectedRegions=30] [-PmaxAnomalyRatio=0.005]")
            log(if (pdf == null) "Missing -Ppdf." else "PDF not found: ${pdf.path}")
            return 2
        }
        val regionFiles = dir?.let { RegionFiles.list(it) }
        if (dir != null && regionFiles.isNullOrEmpty()) {
            log("No <Region>_<code>.pdf files found in ${dir.path}")
            return 2
        }

        val extracted = PdfWordExtractor.extract(pdf)
        val result: BuildResult
        val edition: String
        if (regionFiles == null) {
            result = ImportPipeline.parse(extracted)
            edition = options.sourceEdition ?: extracted.creationDate ?: "unknown"
        } else {
            result = ImportPipeline.parseEdition(regionFiles.map { RegionFiles.banner(it) to PdfWordExtractor.extract(it) }, extracted)
            edition = options.sourceEdition ?: "$MAINLAND_EDITION; Zanzibar ${extracted.creationDate ?: "2012"}"
        }
        val info = InfoDto(
            version = DatasetVersion.CURRENT,
            sourceEdition = edition,
            generatedAt = options.generatedAt ?: Instant.now().toString(),
        )
        val datasetFile = File(options.outDir, "tz-address.json")
        // The postcode list has no positions; keep the ones an earlier ward-points import wrote.
        val existing = datasetFile.takeIf { it.isFile }?.let { DatasetWriter.fromJson(it.readText()) }
        val defects = if (regionFiles != null) SourceCorrections.apply(result) + SourceDefects.remove(result) else emptyList()
        val fresh = ImportPipeline.toDataset(result, info)
        val dataset = CarryOverPoints.merge(fresh, existing)
        val oldPostcodes = ImportPipeline.oldPostcodes(result)
        val checked = Validator.validate(dataset, result.suspectAnomalyCount, result.dataLineCount, options.policy, oldPostcodes)
        val validation = checked.copy(notes = checked.notes + defects)
        val writeDataset = validation.passed || options.force

        options.outDir.mkdirs()
        File(options.outDir, "import-report.md").writeText(ReportWriter.report(dataset, validation, result.anomalies, result.dataLineCount, writeDataset))
        File(options.outDir, "import-anomalies.csv").writeText(ReportWriter.anomaliesCsv(result.anomalies))
        if (existing != null && regionFiles != null && existing.info.sourceEdition != edition) {
            val changes = EditionChanges.compare(existing, fresh, oldPostcodes)
            File(options.outDir, "edition-changes.md").writeText(EditionChangesReport.markdown(changes, existing.info.sourceEdition, edition))
        }
        if (writeDataset) DatasetWriter.write(datasetFile, dataset)

        log("Regions ${dataset.regions.size}, anomalies ${result.anomalies.size} of ${result.dataLineCount} lines: ${if (validation.passed) "PASSED" else "FAILED"}")
        validation.violations.forEach { log("  ${it.kind}: ${it.message}") }
        log(if (writeDataset) "Output: ${options.outDir.path}" else "tz-address.json was NOT written; any existing copy in ${options.outDir.path} is from an earlier run.")
        return if (!validation.passed && !options.force) 1 else 0
    }
}
