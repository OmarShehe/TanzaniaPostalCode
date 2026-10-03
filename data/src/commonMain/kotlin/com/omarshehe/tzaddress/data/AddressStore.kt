package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.AddressRepository
import com.omarshehe.tzaddress.model.ExtraPlaceEntry

/**
 * The bundled-database repository. It holds an open database connection: create it once, share it,
 * and [close] it when the process or scope that owns it ends.
 *
 * [close] does not block. A query that is still running finishes normally and the connection closes right after it;
 * queries started after [close] fail with an [IllegalStateException]. Calling [close] twice is safe, and
 * [isValidPostcode] keeps working because it only reads an in-memory set.
 */
public interface AddressStore : AddressRepository, AutoCloseable {
    /** The extra places this store was created with, each with its id and whether it is [com.omarshehe.tzaddress.model.ExtraPlaceStatus.ACTIVE] or shadowed by a built-in place; empty when none. */
    public fun extraPlaceStatuses(): List<ExtraPlaceEntry> = emptyList()
}
