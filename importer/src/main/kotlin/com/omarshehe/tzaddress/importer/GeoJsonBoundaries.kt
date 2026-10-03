package com.omarshehe.tzaddress.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon

/**
 * Reads a boundary GeoJSON file (geoBoundaries layout: `properties.shapeName`, Polygon or MultiPolygon).
 * A feature with no name or no polygon geometry is counted in [BoundaryFile.skipped]; a malformed polygon fails the read and
 * names its feature, because a wrong shape would silently give a wrong point.
 */
object GeoJsonBoundaries {
    val factory = GeometryFactory()

    fun parse(text: String): List<BoundaryFeature> = read(text).features

    fun read(text: String): BoundaryFile {
        val features = ArrayList<BoundaryFeature>()
        var skipped = 0
        for (element in Json.parseToJsonElement(text).jsonObject.getValue("features").jsonArray) {
            val obj = element.jsonObject
            val name = (obj["properties"] as? JsonObject)?.get("shapeName")?.jsonPrimitive?.content
            val geometry = obj["geometry"] as? JsonObject
            if (name == null || geometry == null) {
                skipped++
                continue
            }
            val shape = try {
                geometry(geometry)
            } catch (e: RuntimeException) {
                throw IllegalArgumentException("Boundary '$name' is malformed: ${e.message}", e)
            }
            if (shape == null) skipped++ else features += BoundaryFeature(name, shape)
        }
        return BoundaryFile(features, skipped)
    }

    private fun geometry(obj: JsonObject): Geometry? = when (obj["type"]?.jsonPrimitive?.content) {
        "Polygon" -> polygon(obj.getValue("coordinates").jsonArray)
        "MultiPolygon" -> factory.createMultiPolygon(obj.getValue("coordinates").jsonArray.map { polygon(it.jsonArray) }.toTypedArray())
        else -> null
    }

    private fun polygon(rings: JsonArray): Polygon {
        val linearRings = rings.map { ring -> factory.createLinearRing(ring.jsonArray.map(::coordinate).toTypedArray()) }
        return factory.createPolygon(linearRings.first(), linearRings.drop(1).toTypedArray<LinearRing>())
    }

    private fun coordinate(position: JsonElement): Coordinate {
        val parts = position.jsonArray
        val x = parts[0].jsonPrimitive.doubleOrNull ?: throw IllegalArgumentException("coordinate '${parts[0]}' is not a number")
        val y = parts[1].jsonPrimitive.doubleOrNull ?: throw IllegalArgumentException("coordinate '${parts[1]}' is not a number")
        return Coordinate(x, y)
    }
}
