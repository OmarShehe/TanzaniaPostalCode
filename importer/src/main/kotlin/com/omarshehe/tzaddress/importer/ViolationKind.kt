package com.omarshehe.tzaddress.importer

enum class ViolationKind {
    REGION_COUNT,
    BAD_WARD_POSTCODE,
    DUPLICATE_WARD_POSTCODE,
    WARD_PREFIX_MISMATCH,
    DUPLICATE_DISTRICT_CODE,
    ANOMALY_RATIO,
}
