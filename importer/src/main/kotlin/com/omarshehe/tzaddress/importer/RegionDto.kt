package com.omarshehe.tzaddress.importer

import kotlinx.serialization.Serializable

@Serializable
data class RegionDto(val code: String, val name: String, val districts: List<DistrictDto>)
