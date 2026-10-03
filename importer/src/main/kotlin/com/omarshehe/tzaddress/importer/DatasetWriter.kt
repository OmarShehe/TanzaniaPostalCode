package com.omarshehe.tzaddress.importer

import java.io.File
import kotlinx.serialization.json.Json

object DatasetWriter {
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
    }

    /** Regions, districts and wards sorted by code; mtaas and kitongojis keep source order. */
    fun sorted(dataset: DatasetDto): DatasetDto = dataset.copy(
        regions = dataset.regions.sortedBy { it.code }.map { region ->
            region.copy(
                districts = region.districts.sortedBy { it.code }.map { district ->
                    district.copy(wards = district.wards.sortedBy { it.postcode })
                },
            )
        },
    )

    fun toJson(dataset: DatasetDto): String = json.encodeToString(DatasetDto.serializer(), sorted(dataset)) + "\n"

    fun fromJson(text: String): DatasetDto = json.decodeFromString(DatasetDto.serializer(), text)

    fun write(file: File, dataset: DatasetDto) {
        file.parentFile?.mkdirs()
        file.writeText(toJson(dataset))
    }
}
