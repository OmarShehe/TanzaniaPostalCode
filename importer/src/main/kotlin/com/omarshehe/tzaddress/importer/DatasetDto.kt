package com.omarshehe.tzaddress.importer

import kotlinx.serialization.Serializable

/** Canonical on-disk dataset. mtaa/kitongoji ids are derived at load time, never stored. */
@Serializable
data class DatasetDto(val info: InfoDto, val regions: List<RegionDto>)
