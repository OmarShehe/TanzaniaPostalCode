package com.omarshehe.tzaddress.importer

/** Small valid dataset used by validator/writer tests. */
object DatasetFixtures {
    fun ward(postcode: String, name: String = "W$postcode", mtaas: List<MtaaDto> = listOf(MtaaDto("M", listOf("K")))) =
        WardDto(postcode, name, mtaas)

    fun dataset(regions: List<RegionDto>, generatedAt: String = "2026-01-01T00:00:00Z") =
        DatasetDto(InfoDto("1", "test", generatedAt), regions)

    fun valid() = dataset(
        listOf(
            RegionDto("53000", "Mbeya", listOf(DistrictDto("531", "Mbeya Cbd", listOf(ward("53101"), ward("53102"))))),
            RegionDto("11000", "Dar es Salaam", listOf(DistrictDto("11", "Ilala Cbd", listOf(ward("11101"))))),
        ),
    )
}
