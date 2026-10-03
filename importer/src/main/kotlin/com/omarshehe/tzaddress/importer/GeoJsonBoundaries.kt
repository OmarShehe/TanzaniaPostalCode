package com.omarshehe.tzaddress.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

/** Reads a boundary GeoJSON file (geoBoundaries layout: `properties.shapeName`, Polygon or MultiPolygon). */
object GeoJsonBoundaries {
    val factory = GeometryFactory()

    fun parse(text: String): List<BoundaryFeature> =
        Json.parseToJsonElement(text).jsonObject.getValue("features").jsonArray.mapNotNull { feature ->
            val obj = feature.jsonObject
            val name = obj.getValue("properties").jsonObject["shapeName"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val geometry = geometry(obj.getValue("geometry").jsonObject) ?: return@mapNotNull null
            BoundaryFeature(name, geometry)
        }

    private fun geometry(obj: JsonObject): Geometry? {
        val coordinates = obj.getValue("coordinates").jsonArray
        return when (obj.getValue("type").jsonPrimitive.content) {
            "Polygon" -> polygon(coordinates)
            "MultiPolygon" -> factory.createMultiPolygon(coordinates.map { polygon(it.jsonArray) }.toTypedArray())
            else -> null
        }
    }

    private fun polygon(rings: JsonArray): Polygon {
        val linearRings = rings.map { ring -> factory.createLinearRing(ring.jsonArray.map(::coordinate).toTypedArray()) }
        return factory.createPolygon(linearRings.first(), linearRings.drop(1).toTypedArray<LinearRing>())
    }

    private fun coordinate(position: kotlinx.serialization.json.JsonElement): Coordinate {
        val parts = position.jsonArray
        return Coordinate(parts[0].jsonPrimitive.doubleOrNull ?: 0.0, parts[1].jsonPrimitive.doubleOrNull ?: 0.0)
    }
}
