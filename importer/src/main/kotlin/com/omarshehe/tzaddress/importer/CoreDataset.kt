package com.omarshehe.tzaddress.importer

import com.omarshehe.tzaddress.model.DatasetInfo
import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

/** The dataset flattened into the `:core` model types. */
class CoreDataset(
    val info: DatasetInfo,
    val regions: List<Region>,
    val districts: List<District>,
    val wards: List<Ward>,
    val mtaas: List<Mtaa>,
    val kitongojis: List<Kitongoji>,
)
