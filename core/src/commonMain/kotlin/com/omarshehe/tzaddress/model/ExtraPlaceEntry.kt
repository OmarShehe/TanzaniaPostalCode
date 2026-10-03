package com.omarshehe.tzaddress.model

/** How one [ExtraPlace] was resolved: its [id] (the postcode for a ward, else the derived id) and its [status]. */
public data class ExtraPlaceEntry(val place: ExtraPlace, val id: String, val status: ExtraPlaceStatus)
