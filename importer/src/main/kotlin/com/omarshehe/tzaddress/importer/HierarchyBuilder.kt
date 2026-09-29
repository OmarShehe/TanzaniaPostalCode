package com.omarshehe.tzaddress.importer

/**
 * Turns detected pages into the region tree, keeping state across pages (rows continue over page breaks).
 *
 * Table cells are not always on the same line, so a name or code that is incomplete is held for exactly one
 * following row, which either completes it or leaves it flagged:
 * - a ward/district code can sit on the line after its name, or the line before it;
 * - a long ward/district name can wrap over two lines, with the code on either line.
 */
class HierarchyBuilder {
    private val regions = ArrayList<RegionNode>()
    private val anomalies = ArrayList<Anomaly>()
    private var dataLines = 0

    private var lastLayout: PageLayout? = null
    private var region: RegionNode? = null
    private var district: DistrictNode? = null
    private var ward: WardNode? = null
    private var mtaa: MtaaNode? = null

    private class PendingWard(val node: WardNode, val owner: DistrictNode, val anomaly: Anomaly)
    private class PendingDistrict(val node: DistrictNode, val owner: RegionNode, val anomaly: Anomaly)
    private class StashedCode(val code: String, val anomaly: Anomaly)

    private var pendingWard: PendingWard? = null
    private var pendingDistrict: PendingDistrict? = null
    private var stashedCode: StashedCode? = null

    fun addPage(page: Int, structure: PageStructure) {
        if (structure.kind == PageKind.BLANK) return
        val segments = listOf(PageSegment(structure.banner, structure.layout, structure.body)) + structure.later
        var lineIndex = 0
        for (segment in segments) {
            segment.banner?.let { startRegion(it) }
            if (segment.layout != null) {
                lastLayout = segment.layout
            } else if (segment.body.isNotEmpty()) {
                anomalies += Anomaly(page, lineIndex, AnomalyKind.MISSING_HEADER, "no usable header row; reusing the previous columns")
            }
            val active = segment.layout ?: lastLayout ?: continue
            for (line in segment.body) {
                lineIndex++
                dataLines++
                process(page, lineIndex, RowParser.parse(line, active), line.text)
            }
        }
    }

    fun result(): BuildResult {
        settleAll()
        return BuildResult(regions.toList(), anomalies.toList(), dataLines)
    }

    private fun startRegion(banner: Banner) {
        settleAll()
        region = RegionNode(banner.regionCode, NameNormalizer.normalize(banner.regionName)).also { regions += it }
        district = null
        ward = null
        mtaa = null
    }

    private fun settleAll() {
        pendingDistrict?.let { settleDistrict(it) }
        pendingWard?.let { settleWard(it) }
        stashedCode?.let { anomalies += it.anomaly }
        pendingDistrict = null
        pendingWard = null
        stashedCode = null
    }

    /** A district that never got a code is the second line of the previous district's name, or is flagged. */
    private fun settleDistrict(pending: PendingDistrict) {
        val list = pending.owner.districts
        val index = list.indexOf(pending.node)
        val previous = list.getOrNull(index - 1)
        if (index > 0 && previous != null && previous.code.isNotEmpty()) {
            previous.rawName = previous.rawName + " " + pending.node.rawName
            previous.wards += pending.node.wards
            list.removeAt(index)
            if (district === pending.node) district = previous
        } else {
            anomalies += pending.anomaly
        }
    }

    /** A ward that never got a code is the second line of the previous ward's name, or is flagged. */
    private fun settleWard(pending: PendingWard) {
        val list = pending.owner.wards
        val index = list.indexOf(pending.node)
        val previous = list.getOrNull(index - 1)
        if (index > 0 && previous != null && previous.postcode.isNotEmpty()) {
            previous.rawName = previous.rawName + " " + pending.node.rawName
            previous.mtaas += pending.node.mtaas
            list.removeAt(index)
            if (ward === pending.node) ward = previous
        } else {
            anomalies += pending.anomaly
        }
    }

    private fun process(page: Int, line: Int, row: Row, text: String) {
        fun flag(kind: AnomalyKind) { anomalies += Anomaly(page, line, kind, text) }

        val prevWard = pendingWard
        val prevDistrict = pendingDistrict
        val stashed = stashedCode
        pendingWard = null
        pendingDistrict = null
        stashedCode = null
        var stashUsed = false

        fun abandonPending() {
            prevDistrict?.let { settleDistrict(it) }
            prevWard?.let { settleWard(it) }
            stashed?.let { anomalies += it.anomaly }
        }

        if (row.unassigned.isNotEmpty()) flag(AnomalyKind.UNASSIGNED_TEXT)
        val cells = row.cells

        // District
        val districtName = cells[Role.DISTRICT_NAME]
        val districtCode = cells[Role.DISTRICT_CODE]
        if (districtName != null) {
            val currentRegion = region
            if (currentRegion == null) {
                abandonPending()
                flag(AnomalyKind.ORPHAN_ROW)
                return
            }
            if (prevDistrict != null && districtCode != null) {
                val merged = DistrictNode(districtCode, prevDistrict.node.rawName + " " + districtName)
                merged.wards += prevDistrict.node.wards
                currentRegion.districts[currentRegion.districts.indexOf(prevDistrict.node)] = merged
                district = merged
            } else {
                prevDistrict?.let { settleDistrict(it) }
                val node = DistrictNode(districtCode.orEmpty(), districtName).also { currentRegion.districts += it }
                if (districtCode == null) {
                    pendingDistrict = PendingDistrict(node, currentRegion, Anomaly(page, line, AnomalyKind.DISTRICT_WITHOUT_CODE, text))
                }
                district = node
            }
            ward = null
            mtaa = null
        } else if (districtCode != null) {
            if (prevDistrict != null) prevDistrict.node.code = districtCode else flag(AnomalyKind.UNEXPECTED_CELL)
        } else {
            prevDistrict?.let { settleDistrict(it) }
        }

        // Ward
        val wardName = cells[Role.WARD_NAME]
        val wardCode = cells[Role.WARD_CODE]
        if (wardName != null) {
            val currentDistrict = district
            if (currentDistrict == null) {
                prevWard?.let { settleWard(it) }
                stashed?.let { anomalies += it.anomaly }
                flag(AnomalyKind.ORPHAN_ROW)
                return
            }
            if (prevWard != null && wardCode != null && currentDistrict.wards.lastOrNull() === prevWard.node) {
                val merged = WardNode(wardCode, prevWard.node.rawName + " " + wardName)
                merged.mtaas += prevWard.node.mtaas
                currentDistrict.wards[currentDistrict.wards.size - 1] = merged
                if (!PostcodeRules.ward.matches(wardCode)) flag(AnomalyKind.BAD_WARD_POSTCODE)
                ward = merged
            } else {
                prevWard?.let { settleWard(it) }
                val code = wardCode ?: stashed?.code
                if (wardCode == null && stashed != null) stashUsed = true
                val node = WardNode(code.orEmpty(), wardName).also { currentDistrict.wards += it }
                if (code == null) {
                    pendingWard = PendingWard(node, currentDistrict, Anomaly(page, line, AnomalyKind.BAD_WARD_POSTCODE, text))
                } else if (!PostcodeRules.ward.matches(code)) {
                    flag(AnomalyKind.BAD_WARD_POSTCODE)
                }
                ward = node
            }
            mtaa = null
        } else if (wardCode != null) {
            if (prevWard != null) {
                prevWard.node.postcode = wardCode
                if (!PostcodeRules.ward.matches(wardCode)) flag(AnomalyKind.BAD_WARD_POSTCODE)
            } else {
                stashedCode = StashedCode(wardCode, Anomaly(page, line, AnomalyKind.UNEXPECTED_CELL, text))
            }
        } else {
            prevWard?.let { settleWard(it) }
        }
        if (stashed != null && !stashUsed) anomalies += stashed.anomaly

        // Mtaa / shehia
        cells[Role.MTAA]?.let { name ->
            val currentWard = ward
            if (currentWard == null) {
                flag(AnomalyKind.ORPHAN_ROW)
                return
            }
            mtaa = MtaaNode(NameNormalizer.normalize(name, shortAllCapsAreAcronyms = true)).also { currentWard.mtaas += it }
        }

        // Kitongoji
        cells[Role.KITONGOJI]?.let { name ->
            val currentWard = ward
            if (currentWard == null) {
                flag(AnomalyKind.ORPHAN_ROW)
                return
            }
            val target = mtaa ?: run {
                flag(AnomalyKind.KITONGOJI_WITHOUT_MTAA)
                MtaaNode(currentWard.name).also { currentWard.mtaas += it; mtaa = it }
            }
            target.kitongojis += NameNormalizer.normalize(name, shortAllCapsAreAcronyms = true)
        }
    }
}
