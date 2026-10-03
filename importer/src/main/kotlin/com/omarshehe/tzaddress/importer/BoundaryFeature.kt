package com.omarshehe.tzaddress.importer

import org.locationtech.jts.geom.Geometry
import kotlin.math.round

/** One boundary polygon (or several) with the name the boundary dataset gives it. */
class BoundaryFeature(val name: String, val geometry: Geometry) {

    /** A point inside the boundary (not a centroid, which can fall outside a concave shape), rounded to 5 decimals (about 1 m). */
    fun interiorPoint(): WardPoint {
        val point = geometry.interiorPoint
        return WardPoint(round5(point.y), round5(point.x))
    }

    private fun round5(value: Double): Double = round(value * SCALE) / SCALE

    private companion object {
        const val SCALE = 100_000.0
    }
}
