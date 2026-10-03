package com.omarshehe.tzaddress

import com.omarshehe.tzaddress.model.District
import com.omarshehe.tzaddress.model.Kitongoji
import com.omarshehe.tzaddress.model.Mtaa
import com.omarshehe.tzaddress.model.Region
import com.omarshehe.tzaddress.model.Ward

/** A node with all its ancestors; levels below the node are null. */
public data class AddressPath(
    val region: Region,
    val district: District? = null,
    val ward: Ward? = null,
    val mtaa: Mtaa? = null,
    val kitongoji: Kitongoji? = null,
)
