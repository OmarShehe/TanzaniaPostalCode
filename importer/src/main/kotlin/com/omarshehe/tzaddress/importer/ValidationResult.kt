package com.omarshehe.tzaddress.importer

/** [violations] fail the import; [warnings] and [notes] are informational. */
data class ValidationResult(
    val violations: List<Violation>,
    val warnings: List<Warning>,
    val notes: List<String>,
) {
    val passed: Boolean get() = violations.isEmpty()
}
