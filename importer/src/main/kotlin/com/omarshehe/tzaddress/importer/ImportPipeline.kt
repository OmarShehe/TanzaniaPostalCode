package com.omarshehe.tzaddress.importer

object ImportPipeline {

    fun parse(pdf: ExtractedPdf): BuildResult {
        val builder = HierarchyBuilder()
        pdf.pages.forEach { builder.addPage(it.page, LayoutDetector.detect(it.lines)) }
        return builder.result()
    }

    /** Mainland regions from one file each (the region is named by the file), Zanzibar from the older single list. */
    fun parseEdition(regionFiles: List<Pair<Banner, ExtractedPdf>>, zanzibarList: ExtractedPdf, isZanzibar: (Banner) -> Boolean = { it.regionCode.startsWith("7") }): BuildResult {
        val mainland = HierarchyBuilder(headerOncePerRegion = true)
        for ((banner, pdf) in regionFiles) {
            mainland.startRegion(banner)
            pdf.pages.forEach { mainland.addPage(it.page, LayoutDetector.detect(it.lines)) }
        }
        val zanzibar = HierarchyBuilder(keepRegion = isZanzibar)
        zanzibarList.pages.forEach { zanzibar.addPage(it.page, LayoutDetector.detect(it.lines)) }
        val a = mainland.result()
        val b = zanzibar.result()
        return BuildResult(a.regions + b.regions, a.anomalies + b.anomalies, a.dataLineCount + b.dataLineCount)
    }

    /** New postcode to previous postcode, for wards whose file gives one. */
    fun oldPostcodes(result: BuildResult): Map<String, String> = result.regions
        .flatMap { r -> r.districts.flatMap { d -> d.wards } }
        .mapNotNull { w -> w.oldPostcode?.let { w.postcode to it } }
        .toMap()

    fun toDataset(result: BuildResult, info: InfoDto): DatasetDto = DatasetDto(
        info,
        result.regions.map { region ->
            RegionDto(
                region.code, region.name,
                region.districts.map { district ->
                    DistrictDto(
                        district.code, district.name,
                        district.wards.map { ward -> WardDto(ward.postcode, ward.name, ward.mtaas.map { MtaaDto(it.name, it.kitongojis.toList()) }) },
                    )
                },
            )
        },
    )
}
