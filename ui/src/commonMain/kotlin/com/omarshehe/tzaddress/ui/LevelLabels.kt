package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.Level
import com.omarshehe.tzaddress.ui.resources.Res
import com.omarshehe.tzaddress.ui.resources.level_district
import com.omarshehe.tzaddress.ui.resources.level_kitongoji
import com.omarshehe.tzaddress.ui.resources.level_mtaa
import com.omarshehe.tzaddress.ui.resources.level_region
import com.omarshehe.tzaddress.ui.resources.level_ward
import org.jetbrains.compose.resources.StringResource

internal fun Level.labelResource(): StringResource = when (this) {
    Level.REGION -> Res.string.level_region
    Level.DISTRICT -> Res.string.level_district
    Level.WARD -> Res.string.level_ward
    Level.MTAA -> Res.string.level_mtaa
    Level.KITONGOJI -> Res.string.level_kitongoji
}
