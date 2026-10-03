package com.omarshehe.tzaddress.importer

/** [rawName] is the name as printed; [name] is the cleaned form. Fragments of wrapped names are joined on the raw text. */
class DistrictNode(var code: String, var rawName: String, val wards: MutableList<WardNode> = ArrayList()) {
    val name: String get() = NameNormalizer.normalize(rawName)
}
