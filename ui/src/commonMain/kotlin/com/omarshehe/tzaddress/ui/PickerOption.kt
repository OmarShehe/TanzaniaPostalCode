package com.omarshehe.tzaddress.ui

import com.omarshehe.tzaddress.AddressPath

/** One dropdown entry; [extend] adds this node to the chain of its ancestors. */
internal class PickerOption(val id: String, val label: String, val extend: (AddressPath?) -> AddressPath)
