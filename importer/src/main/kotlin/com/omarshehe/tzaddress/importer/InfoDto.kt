package com.omarshehe.tzaddress.importer

import kotlinx.serialization.Serializable

@Serializable
data class InfoDto(val version: String, val sourceEdition: String, val generatedAt: String)
