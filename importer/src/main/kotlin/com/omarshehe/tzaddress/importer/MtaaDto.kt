package com.omarshehe.tzaddress.importer

import kotlinx.serialization.Serializable

@Serializable
data class MtaaDto(val name: String, val kitongojis: List<String>)
