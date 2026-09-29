package com.omarshehe.tzaddress.importer

import kotlinx.serialization.Serializable

@Serializable
data class DistrictDto(val code: String, val name: String, val wards: List<WardDto>)
