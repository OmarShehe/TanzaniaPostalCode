package com.omarshehe.tzaddress.importer

class BuildResult(
    val regions: List<RegionNode>,
    val anomalies: List<Anomaly>,
    val dataLineCount: Int,
) {
    /** Anomalies that suggest a parse problem; informational ones are excluded. */
    val suspectAnomalyCount: Int get() = anomalies.count { !it.kind.informational }
}
