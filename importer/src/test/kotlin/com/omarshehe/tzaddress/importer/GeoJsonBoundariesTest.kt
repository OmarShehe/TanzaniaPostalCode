package com.omarshehe.tzaddress.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoJsonBoundariesTest {
    private fun feature(name: String, geometry: String) = """{"type":"Feature","properties":{"shapeName":"$name"},"geometry":$geometry}"""
    private fun collection(vararg features: String) = """{"type":"FeatureCollection","features":[${features.joinToString(",")}]}"""

    @Test
    fun parsesPolygonAndMultiPolygonWithTheirNames() {
        val text = collection(
            feature("Square", """{"type":"Polygon","coordinates":[[[0,0],[4,0],[4,4],[0,4],[0,0]]]}"""),
            feature("Two", """{"type":"MultiPolygon","coordinates":[[[[0,0],[1,0],[1,1],[0,1],[0,0]]],[[[5,5],[7,5],[7,7],[5,7],[5,5]]]]}"""),
        )
        val features = GeoJsonBoundaries.parse(text)
        assertEquals(listOf("Square", "Two"), features.map { it.name })
        assertEquals(2, features[1].geometry.numGeometries)
    }

    @Test
    fun interiorPointOfAHoledPolygonIsNotInTheHole() {
        val text = collection(
            feature("Ring", """{"type":"Polygon","coordinates":[[[0,0],[10,0],[10,10],[0,10],[0,0]],[[2,2],[8,2],[8,8],[2,8],[2,2]]]}"""),
        )
        val feature = GeoJsonBoundaries.parse(text).single()
        val point = feature.interiorPoint()
        assertTrue(feature.geometry.contains(GeoJsonBoundaries.factory.createPoint(org.locationtech.jts.geom.Coordinate(point.longitude, point.latitude))))
    }

    @Test
    fun interiorPointOfAConcaveShapeLiesInsideEvenWhenTheCentroidDoesNot() {
        // A "C" whose centroid sits in the opening.
        val c = """{"type":"Polygon","coordinates":[[[0,0],[10,0],[10,2],[2,2],[2,8],[10,8],[10,10],[0,10],[0,0]]]}"""
        val feature = GeoJsonBoundaries.parse(collection(feature("C", c))).single()
        val centroid = feature.geometry.centroid
        assertTrue(!feature.geometry.contains(centroid), "fixture must have an outside centroid")
        val p = feature.interiorPoint()
        assertTrue(feature.geometry.contains(GeoJsonBoundaries.factory.createPoint(org.locationtech.jts.geom.Coordinate(p.longitude, p.latitude))))
    }

    @Test
    fun interiorPointIsRoundedToFiveDecimalsAndLatitudeComesFromY() {
        val text = collection(feature("Tiny", """{"type":"Polygon","coordinates":[[[39.123456789,-6.1],[39.2,-6.1],[39.2,-6.2],[39.123456789,-6.2],[39.123456789,-6.1]]]}"""))
        val p = GeoJsonBoundaries.parse(text).single().interiorPoint()
        assertEquals(-6.15, p.latitude)
        assertEquals(5, p.longitude.toString().substringAfter('.').length.coerceAtMost(5))
        assertTrue(p.longitude > 39.1 && p.longitude < 39.2)
    }
}
