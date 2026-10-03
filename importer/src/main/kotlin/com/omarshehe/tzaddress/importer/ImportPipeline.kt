package com.omarshehe.tzaddress.importer

object ImportPipeline {

    fun parse(pdf: ExtractedPdf): BuildResult {
        val builder = HierarchyBuilder()
        pdf.pages.forEach { builder.addPage(it.page, LayoutDetector.detect(it.lines)) }
        return builder.result()
    }

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
