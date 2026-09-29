package com.omarshehe.tzaddress.importer

object Validator {
    private val WARD_POSTCODE = Regex("^\\d{5}$")

    fun validate(dataset: DatasetDto, anomalyCount: Int, dataLineCount: Int, policy: Policy): ValidationResult {
        val violations = ArrayList<Violation>()
        val warnings = ArrayList<Warning>()

        if (dataset.regions.size != policy.expectedRegions) {
            violations += Violation(ViolationKind.REGION_COUNT, "Expected ${policy.expectedRegions} regions, found ${dataset.regions.size}")
        }

        val districtCodes = HashMap<String, String>()
        val wardCodes = HashSet<String>()
        val reportedDuplicates = HashSet<String>()
        for (region in dataset.regions) {
            for (district in region.districts) {
                val previous = districtCodes.put(district.code, region.code)
                if (previous != null) {
                    violations += Violation(ViolationKind.DUPLICATE_DISTRICT_CODE, "District code ${district.code} appears in regions $previous and ${region.code}")
                }
                if (district.wards.isEmpty()) {
                    warnings += Warning(WarningKind.DISTRICT_WITHOUT_WARDS, "District ${district.code} ${district.name} (region ${region.code}) has no wards")
                }
                for (ward in district.wards) {
                    val wellFormed = WARD_POSTCODE.matches(ward.postcode)
                    if (!wellFormed) {
                        violations += Violation(ViolationKind.BAD_WARD_POSTCODE, "Ward '${ward.name}' has postcode '${ward.postcode}', expected 5 digits")
                    } else if (!ward.postcode.startsWith(district.code)) {
                        violations += Violation(ViolationKind.WARD_PREFIX_MISMATCH, "Ward ${ward.postcode} '${ward.name}' does not start with its district code ${district.code}")
                    }
                    if (!wardCodes.add(ward.postcode) && reportedDuplicates.add(ward.postcode)) {
                        violations += Violation(ViolationKind.DUPLICATE_WARD_POSTCODE, "Ward postcode ${ward.postcode} appears more than once")
                    }
                    if (ward.mtaas.isEmpty()) {
                        warnings += Warning(WarningKind.WARD_WITHOUT_MTAAS, "Ward ${ward.postcode} '${ward.name}' has no mtaa/village")
                    }
                }
            }
        }

        val ratio = if (dataLineCount == 0) 0.0 else anomalyCount.toDouble() / dataLineCount
        if (ratio > policy.maxAnomalyRatio) {
            violations += Violation(
                ViolationKind.ANOMALY_RATIO,
                "Anomaly ratio %.4f (%d of %d lines) exceeds the maximum %.4f".format(ratio, anomalyCount, dataLineCount, policy.maxAnomalyRatio),
            )
        }

        val notes = ArrayList<String>()
        if (dataset.regions.none { it.name.equals("Songwe", ignoreCase = true) }) {
            notes += "Songwe region (created in 2016) is not in this edition; later administrative changes are not reflected."
        }
        return ValidationResult(violations, warnings, notes)
    }
}
