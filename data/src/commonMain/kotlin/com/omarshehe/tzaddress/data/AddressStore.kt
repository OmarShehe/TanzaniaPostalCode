package com.omarshehe.tzaddress.data

import com.omarshehe.tzaddress.AddressRepository

/**
 * The bundled-database repository. It holds an open database connection: create it once, share it,
 * and [close] it when the process or scope that owns it ends.
 */
public interface AddressStore : AddressRepository, AutoCloseable
