package com.omarshehe.tzaddress.importer

/** [rawName] is the name as printed; [name] is the cleaned form. Fragments of wrapped names are joined on the raw text. */
class WardNode(var postcode: String, var rawName: String, val mtaas: MutableList<MtaaNode> = ArrayList()) {
    val name: String get() = NameNormalizer.normalize(rawName)
}
