package com.omarshehe.tzaddress.importer

import java.io.File
import kotlin.system.exitProcess

/** `GenerateDbMain <dataset.json> <import-report.md> <out.db>`: builds the bundled DB and checks it against the report totals. */
fun main(args: Array<String>) {
    if (args.size != 3) {
        System.err.println("Usage: GenerateDbMain <dataset.json> <import-report.md> <out.db>")
        exitProcess(2)
    }
    val (datasetPath, reportPath, outPath) = args
    val core = DatasetWriter.fromJson(File(datasetPath).readText()).toCore()
    val counts = AddressDbBuilder.build(core, File(outPath))
    try {
        counts.verifyEquals(AddressDbCounts.fromReport(File(reportPath).readText()), reportPath)
    } catch (e: IllegalStateException) {
        File(outPath).delete()
        System.err.println(e.message)
        exitProcess(1)
    }
    // Lets the installer decide whether the copied DB is current without reading the whole bundled file.
    File(outPath + ".version").writeText(with(core.info) { "$version|$sourceEdition|$generatedAt" })
    println("Wrote $outPath: $counts")
}
