package com.omarshehe.tzaddress.importer

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val options = try {
        ImportOptions.parse(args)
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message)
        exitProcess(2)
    }
    val code = PostcodeImport.run(options)
    if (code != 0) exitProcess(code)
}
