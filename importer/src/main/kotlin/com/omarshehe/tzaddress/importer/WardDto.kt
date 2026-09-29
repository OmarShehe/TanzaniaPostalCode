package com.omarshehe.tzaddress.importer

import kotlinx.serialization.Serializable

@Serializable
data class WardDto(val postcode: String, val name: String, val mtaas: List<MtaaDto>)
