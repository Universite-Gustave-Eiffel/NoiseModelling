/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.scripts

import groovy.sql.Sql
import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.functions.io.shp.SHPWrite
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir;
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Change_SRID
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Clean_Buildings_Table
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Enrich_DEM_with_road
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Enrich_DEM_with_rail
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Enrich_Landcover_with_rail
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Screen_to_building
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Set_Height
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_Asc_File
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Road_Emission_from_Traffic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Test parsing of zip file using H2GIS database
 */
class TestGeometricTools extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestGeometricTools.class)

    @Test
    void testChangeSRID1() {

        SHPRead.importTable(connection, TestGeometricTools.getResource("roads.shp").getPath())

        String res = new Change_SRID().exec(connection,
                ["newSRID": "2154",
                 "tableName": "roads"])

        assertEquals("The table already counts 2154 as SRID.", res)
    }

    @Test
    void testChangeSRID2() {

        SHPRead.importTable(connection, TestGeometricTools.getResource("roads.shp").getPath())

        String res = new Change_SRID().exec(connection,
                ["newSRID": "4326",
                 "tableName": "roads"])

        assertEquals("SRID changed from 2154 to 4326.", res)
    }



    @Test
    void testTruncateScreens() {

        String screen1 = "LINESTRING (224146.48 6758063.29, 224164.4 6757986.29, 224164.81 6757970.4) "
        String screen2 = "LINESTRING (224206.98 6757997.9, 224213.9 6757964.7, 224210.24 6757964.29, 224206.98 6757997.9)"
        def sql = new Sql(connection)
        sql.execute("CREATE TABLE SCREENS(pk serial, the_geom geometry, height double)")
        sql.executeInsert("INSERT INTO SCREENS(pk, THE_GEOM, HEIGHT) VALUES (2001,?, 66), (2002,?, 99)", [screen1, screen2])
        SHPRead.importTable(connection, TestGeometricTools.getResource("buildings.shp").getPath())

        String res = new Change_SRID().exec(connection,
                ["newSRID": "2154",
                 "tableName": "SCREENS"])

        new Screen_to_building().exec(connection, ["tableBuilding": "BUILDINGS", "tableScreens" : "SCREENS"])

        // Check new walls not intersecting with buildings
        assertEquals(0, sql.firstRow("SELECT COUNT(*) CPT FROM BUILDINGS B, BUILDINGS_SCREENS S WHERE B.the_geom && S.the_geom and (S.height = 66 OR S.height = 99) and ST_INTERSECTS(B.THE_GEOM, S.THE_GEOM)")[0] as Integer)
    }

    @Test
    void testSetHeight1() {
        SHPRead.importTable(connection, TestGeometricTools.getResource("receivers.shp").getPath())
        def sql = new Sql(connection)

        new Set_Height().exec(connection,
                ["height": 0.05,
                 "tableName": "receivers"])

        assertEquals(0.05, sql.firstRow("SELECT ST_Z(THE_GEOM) FROM RECEIVERS")[0])
    }



    @Test
    void testSetHeight2() {
        SHPRead.importTable(connection, TestGeometricTools.getResource("roads.shp").getPath())
        def sql = new Sql(connection)
        def srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse("ROADS"))
        new Set_Height().exec(connection,
                ["height": 0.05,
                 "tableName": "roads"])
        assertEquals(0.05, sql.firstRow("SELECT ST_Z(THE_GEOM) FROM ROADS")[0])
        assertEquals(srid, GeometryTableUtilities.getSRID(connection, TableLocation.parse("ROADS")))
    }

    @Test
    void testCleanBuildings() {
        SHPRead.importTable(connection, TestGeometricTools.getResource("buildings.shp").getPath())
        def sql = new Sql(connection)

        new Clean_Buildings_Table().exec(connection,
                ["tableName": "buildings"])

        // Check if there is remaining intersecting buildings
        assertEquals(0, sql.firstRow("select COUNT(*) COUNTINTERS FROM buildings S1, buildings S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.05;")[0] as Integer)
    }

    void testSetHeightByColumnName1() {
        new Import_File().exec(connection,[
                "pathFile": TestGeometricTools.getResource("dem_test_height.geojson").getPath(),
                "tableName": "DEM"
        ])
        def sql = new Sql(connection)
        new Set_Height().exec(connection,
                ["tableName": "DEM",
                 "heightColumn": "ELEVATION"])

        assertEquals(
                71.0,
                sql.firstRow("SELECT ST_Z(THE_GEOM) AS z FROM DEM WHERE ID=41.0")[0]
        )
        assertEquals(2154, GeometryTableUtilities.getSRID(connection, TableLocation.parse("DEM")))
    }

    @Test
    void testSetHeightByColumnName2() {
        def sql = new Sql(connection)
        sql.execute("""
            CREATE TABLE RECEIVER (
                ID INT PRIMARY KEY,
                THE_GEOM GEOMETRY,
                ELEVATION DOUBLE
            )
            """)
        sql.execute("""
                INSERT INTO RECEIVER (ID, THE_GEOM, ELEVATION) VALUES
                (1, ST_GeomFromText('POINT(654305.1 6853353.699999999)'), 12),
                (2, ST_GeomFromText('POINT(654330.1 6853353.699999999)'), 57),
                (3, ST_GeomFromText('POINT(654355.1 6853353.699999999)'), 89),
                (4, ST_GeomFromText('POINT(654380.1 6853353.699999999)'), 10),
                (5, ST_GeomFromText('POINT(654405.1 6853353.699999999)'), 25),
                (6, ST_GeomFromText('POINT(654430.1 6853353.699999999)'), 0)
        """)

        new Change_SRID().exec(connection,[
                "newSRID":2154,
                "tableName":"RECEIVER"
        ])

        new Set_Height().exec(connection, [
                'tableName': 'RECEIVER',
                'heightColumn': 'ELEVATION'
        ])

        def row = sql.firstRow("SELECT ST_Z(THE_GEOM) AS z FROM RECEIVER WHERE ID=2")
        double actual = row.z

        assertEquals(57.0, actual)


    }

    @Test
    void testCleanBuildingsPop() {
        SHPRead.importTable(connection, TestGeometricTools.getResource("buildings.shp").getPath())

        def srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse("BUILDINGS"))

        def sql = new Sql(connection)

        sql.execute("ALTER TABLE BUILDINGS ADD COLUMN POP REAL")
        sql.execute("UPDATE BUILDINGS SET POP = 30")

        new Clean_Buildings_Table().exec(connection,
                ["tableName": "buildings"])


        def srid2 = GeometryTableUtilities.getSRID(connection, TableLocation.parse("BUILDINGS"))

        // Check if there is remaining intersecting buildings
        assertEquals(0, sql.firstRow("select COUNT(*) COUNTINTERS FROM buildings S1, buildings S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.05;")[0] as Integer)
        assertEquals(30, sql.firstRow("select AVG(POP) FROM buildings")[0] as Integer, 1e-3)
        assertEquals(2154, srid)
        assertEquals(2154, srid2)
    }

    @Test
    void testEnrichRoad() {

        new Import_Asc_File().exec(connection,
                ["pathFile" : TestGeometricTools.getResource("testDem/dem.asc.gz").getPath(),
                 "inputSRID": 2154])

        new Import_File().exec(connection,
                ["pathFile" : TestGeometricTools.getResource("testDem/test_roads.geojson").getPath(),
                 "inputSRID": 2154,
                 "tableName": "ROADS"])

        new Enrich_DEM_with_road().exec(connection,
                ["inputDEM" : "DEM",
                "inputRoad" : "ROADS",
                "roadWidth" : "WIDTH"])

        Sql sql = new Sql(connection)

        def countBefore = sql.firstRow("SELECT COUNT(*) FROM DEM")[0] as Integer

        assertTrue(JDBCUtilities.tableExists(connection, "DEM_ENRICHED"))

        def countAfter = sql.firstRow("SELECT COUNT(*) FROM DEM_ENRICHED")[0] as Integer

        assertTrue(countBefore < countAfter)
    }


    @Test
    void testExportKML(@TempDir File tempDir) {

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("ground_type.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("ROADS2.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("dem.geojson").getPath(),
                 "inputSRID": "2154"])


        new Road_Emission_from_Traffic().exec(connection, [tableRoads: "ROADS2"])

        File kmlLocation = new File(tempDir, "dem")
        assertTrue(kmlLocation.mkdirs())

        new Noise_level_from_source().exec(connection,
                ["tableBuilding"        : "BUILDINGS",
                 "tableSources"         : "LW_ROADS",
                 "tableReceivers"       : "RECEIVERS",
                 "tableGroundAbs"       : "GROUND_TYPE",
                 "tableDEM"             : "DEM",
                 "confRaysName"         : kmlLocation.toURI().toString(),
                 "confMaxSrcDist"       : 500.0,
                 "confReflOrder"        : 0,
                 "confMaxError"         : 3.0])

        // Check if KML files have been created in the output directory kmlLocation
        // List all files ending with .kml
        // Exception if no KML file is found
        int kmlFileCount = 0
        kmlLocation.eachFileMatch(~/.*\.kml/) { file ->
            kmlFileCount++
            // Check if it is not empty
            assertTrue(file.length() > 0, "KML file ${file.name} is empty")
        }
        assertTrue(kmlFileCount > 0, "No KML files found in directory ${kmlLocation}")
    }

    @Test
    void testEnrichLandcoverWithRail() {
        def sql = new Sql(connection)
        sql.execute("CREATE TABLE LANDCOVER(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), GROUND_G DOUBLE)")
        sql.execute("""INSERT INTO LANDCOVER VALUES
            (1, ST_GeomFromText('POLYGON((0 -50, 100 -50, 100 50, 0 50, 0 -50))', 2154), 0.7),
            (2, ST_GeomFromText('POLYGON((200 0, 300 0, 300 100, 200 100, 200 0))', 2154), 1),
            (3, ST_GeomFromText('POLYGON((0 100, 100 100, 100 200, 0 200, 0 100))', 2154), 0.1),
            (4, ST_GeomFromText('POLYGON((0 -200, 100 -200, 100 -100, 0 -100, 0 -200))', 2154), 0)""")
        sql.execute("CREATE TABLE RAIL(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154), WIDTH DOUBLE, PLATFORM VARCHAR)")
        sql.execute("INSERT INTO RAIL VALUES (1, ST_GeomFromText('LINESTRING Z(-10 0 1, 110 0 1)', 2154), 10, 'SNCF')")

        new Enrich_Landcover_with_rail().exec(connection,
                ["inputLandcover": "LANDCOVER", "gColumn": "GROUND_G", "inputRail": "RAIL", "railWidth": "WIDTH"])

        // Outside the rail strip, every input class is kept with its own G
        def areas = [:]
        sql.eachRow("SELECT G, SUM(ST_AREA(ST_DIFFERENCE(THE_GEOM, ST_MakeEnvelope(-20, -5, 120, 5, 2154)))) A FROM LANDCOVER_ENRICHED GROUP BY G") {
            areas[it.G as Double] = it.A as Double
        }
        assertEquals([0.0d: 10000d, 0.1d: 10000d, 0.7d: 9000d, 1.0d: 10000d], areas)
        // Inside the rail strip, the G=0.7 landcover is cut away
        assertEquals(0d, sql.firstRow("SELECT COALESCE(SUM(ST_AREA(ST_INTERSECTION(THE_GEOM, ST_MakeEnvelope(-20, -5, 120, 5, 2154)))), 0) A FROM LANDCOVER_ENRICHED WHERE G = 0.7").A as double, 1e-6)
    }

    @Test
    void testEnrichDEMWithRail() {
        // Import DEM near the train tracks
        new Import_Asc_File().exec(connection,
                ["pathFile" : TestGeometricTools.getResource("testDem/dem.asc.gz").getPath(),
                 "inputSRID": 2154])

        // Import the train tracks
        new Import_File().exec(connection, [
                "pathFile" : TestGeometricTools.getResource("testDem/rail_track.geojson").getPath(),
                tableName: "RAIL"
        ])

        def sql = new Sql(connection)


        // Update RAIL table with a WIDTH column and a PLATFORM column (platform SNCF is resolved against the data file)
        sql.execute("ALTER TABLE RAIL ADD COLUMN PLATFORM VARCHAR")
        sql.execute("UPDATE RAIL SET PLATFORM = 'SNCF'")
        sql.execute("ALTER TABLE RAIL RENAME COLUMN largempris TO WIDTH")

        assertEquals("DEM_ENRICHED", new Enrich_DEM_with_rail().exec(connection,
                ["inputDEM": "DEM", "inputRail": "RAIL", "railWidth": "WIDTH"]).result)

        assertTrue(JDBCUtilities.tableExists(connection, "DEM_ENRICHED"))

        // Get the average height near the train tracks
        def averageHeightBefore = sql.firstRow("SELECT AVG(ST_Z(d.the_geom)) avgz FROM DEM d, RAIL R WHERE ST_EXPAND(r.the_geom, 15, 15) && d.the_geom AND ST_Intersects(ST_Buffer(r.the_geom, 15), d.the_geom)").avgz

        def averageHeightAfter = sql.firstRow("SELECT AVG(ST_Z(d.the_geom)) avgz FROM DEM_ENRICHED d, RAIL R WHERE ST_EXPAND(r.the_geom, 15, 15) && d.the_geom AND ST_Intersects(ST_Buffer(r.the_geom, 15), d.the_geom)").avgz

        assertNotEquals(averageHeightBefore, averageHeightAfter)
    }

    @Test
    void testEnrichDEMWithRailElevations() {
        def sql = new Sql(connection)
        // Flat DEM at Z=0 and a rail at Z=5 with the DEFAULT platform (h1=0.5, h2=0.18,
        // d2_0=3.3, d3_0=4.8, d4_0=8.3): the d2 ring stays at the rail level, the d3 ring is
        // lowered by h2 (ballast shoulder level) and the d4 ring by h2+h1 (surrounding terrain).
        List<String> demValues = []
        (-20..130).step(5) { x ->
            (-20..20).step(5) { y ->
                demValues << "ST_SetSRID(ST_MakePoint($x, $y, 0), 2154)"
            }
        }
        sql.execute("CREATE TABLE DEM (THE_GEOM GEOMETRY(POINTZ, 2154))")
        sql.execute("INSERT INTO DEM VALUES " + demValues.collect { "($it)" }.join(", "))
        sql.execute("CREATE TABLE RAIL (THE_GEOM GEOMETRY(LINESTRINGZ, 2154))")
        sql.execute("INSERT INTO RAIL VALUES (ST_GeomFromText('LINESTRING Z(-10 0 5, 110 0 5)', 2154))")

        double densifydistance = 10

        new Enrich_DEM_with_rail().exec(connection,
                ["inputDEM": "DEM", "inputRail": "RAIL", "inputSRID": 2154, "densifyDistance" : densifydistance])

        String railGeom = "ST_GeomFromText('LINESTRING Z(-10 0 5, 110 0 5)', 2154)"
        // d2 ring (d2_0/2 = 1.65 m): at the rail elevation
        assertEquals(5.0d, sql.firstRow("SELECT AVG(ST_Z(THE_GEOM)) z FROM DEM_ENRICHED WHERE ST_DISTANCE(THE_GEOM, " + railGeom + ") BETWEEN 1.6 AND 1.7").z as double, 1e-6)
        // d3 ring (d3_0/2 = 2.4 m): lowered by h2 (ballast shoulder level)
        assertEquals(4.82d, sql.firstRow("SELECT AVG(ST_Z(THE_GEOM)) z FROM DEM_ENRICHED WHERE ST_DISTANCE(THE_GEOM, " + railGeom + ") BETWEEN 2.35 AND 2.45").z as double, 1e-6)
        // d4 ring (d4_0/2 = 4.15 m): lowered by h2 + h1 (surrounding terrain)
        assertEquals(4.32d, sql.firstRow("SELECT AVG(ST_Z(THE_GEOM)) z FROM DEM_ENRICHED WHERE ST_DISTANCE(THE_GEOM, " + railGeom + ") BETWEEN 4.1 AND 4.2").z as double, 1e-6)
        // Original DEM points beyond the platform keep their elevation
        assertEquals(0.0d, sql.firstRow("SELECT AVG(ST_Z(THE_GEOM)) z FROM DEM_ENRICHED WHERE ST_DISTANCE(THE_GEOM, " + railGeom + ") BETWEEN 8 AND 15").z as double, 1e-6)
        // The densifyDistance parameter controls the spacing of the points created on the d4 ring.
        int countDense = sql.firstRow("SELECT COUNT(*) C FROM DEM_ENRICHED WHERE ST_DISTANCE(THE_GEOM, " + railGeom + ") BETWEEN 4.1 AND 4.2").C as Integer
        assertEquals(27, countDense)

        // Check with large densify
        new Enrich_DEM_with_rail().exec(connection,
                ["inputDEM": "DEM", "inputRail": "RAIL", "inputSRID": 2154, "densifyDistance": 40])
        int countSparse = sql.firstRow("SELECT COUNT(*) C FROM DEM_ENRICHED WHERE ST_DISTANCE(THE_GEOM, " + railGeom + ") BETWEEN 4.1 AND 4.2").C as Integer
        assertEquals(9, countSparse)
    }

    @Test
    void testEnrichWithRailwayPlatformDataFileURI(@TempDir File tempDir) {
        def sql = new Sql(connection)
        // Custom platform data file loaded through a file:// URI (UriUtils.openSafeStream file branch).
        // Distinctive values: d2_0=4, d3_0=6, d4_0=12, h1=1, h2=0.5 (DEFAULT is 3.3/4.8/8.3/0.5/0.18)
        File customPlatformFile = new File(tempDir, "CustomPlatforms.json")
        customPlatformFile.text = """{
            "WIDE": {
                "d1": 1.8, "d2_0": 4.0, "d3_0": 6.0, "d4_0": 12.0,
                "g1": 0, "g2": 1, "g3": 1, "h1": 1.0, "h2": 0.5, "trackspacing": 3.67
            }
        }"""
        String platformFileURI = customPlatformFile.toURI().toString()
        assertTrue(platformFileURI.startsWith("file:"))

        // Flat DEM at Z=0 and a rail at Z=5 with the WIDE platform (h1=1, h2=0.5,
        // d2_0/2=2, d3_0/2=3, d4_0/2=6): d2 stays at the rail level, d3 is lowered by h2
        // and d4 by h2+h1.
        List<String> demValues = []
        (-20..130).step(5) { x ->
            (-20..20).step(5) { y ->
                demValues << "ST_SetSRID(ST_MakePoint($x, $y, 0), 2154)"
            }
        }
        sql.execute("CREATE TABLE DEM (THE_GEOM GEOMETRY(POINTZ, 2154))")
        sql.execute("INSERT INTO DEM VALUES " + demValues.collect { "($it)" }.join(", "))
        sql.execute("CREATE TABLE RAIL(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154), PLATFORM VARCHAR)")
        sql.execute("INSERT INTO RAIL VALUES (1, ST_GeomFromText('LINESTRING Z(-10 0 5, 110 0 5)', 2154), 'WIDE')")

        new Enrich_DEM_with_rail().exec(connection,
                ["inputDEM": "DEM", "inputRail": "RAIL", "inputSRID": 2154,
                 "railwayPlatformDataFile": platformFileURI])

        String railGeom = "ST_GeomFromText('LINESTRING Z(-10 0 5, 110 0 5)', 2154)"
        // Points on the long sides of the rings (far from the flat endcaps) only: the endcap points
        // of the outer rings can otherwise fall within the distance band of the inner rings.
        String sideFilter = "ST_X(THE_GEOM) BETWEEN 10 AND 90 AND ST_DISTANCE(THE_GEOM, " + railGeom + ") "
        // d2 ring (2.0 m): at the rail elevation
        assertEquals(5.0d, sql.firstRow("SELECT AVG(ST_Z(THE_GEOM)) z FROM DEM_ENRICHED WHERE " + sideFilter + "BETWEEN 1.9 AND 2.1").z as double, 1e-6)
        // d3 ring (3.0 m): lowered by the custom h2 (ballast shoulder level)
        assertEquals(4.5d, sql.firstRow("SELECT AVG(ST_Z(THE_GEOM)) z FROM DEM_ENRICHED WHERE " + sideFilter + "BETWEEN 2.9 AND 3.1").z as double, 1e-6)
        // d4 ring (6.0 m): lowered by the custom h2 + h1 (surrounding terrain)
        assertEquals(3.5d, sql.firstRow("SELECT AVG(ST_Z(THE_GEOM)) z FROM DEM_ENRICHED WHERE " + sideFilter + "BETWEEN 5.9 AND 6.1").z as double, 1e-6)

        // Landcover enrichment with the same custom platform: the WIDE d4=12 platform cuts a 12 m wide strip
        sql.execute("CREATE TABLE LANDCOVER(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), GROUND_G DOUBLE)")
        sql.execute("INSERT INTO LANDCOVER VALUES (1, ST_GeomFromText('POLYGON((0 -20, 200 -20, 200 20, 0 20, 0 -20))', 2154), 0.7)")
        sql.execute("INSERT INTO RAIL VALUES (2, ST_GeomFromText('LINESTRING Z(-10 0 1, 210 0 1)', 2154), 'WIDE')")

        new Enrich_Landcover_with_rail().exec(connection,
                ["inputLandcover": "LANDCOVER", "gColumn": "GROUND_G", "inputRail": "RAIL",
                 "railwayPlatformDataFile": platformFileURI])

        // No original G=0.7 landcover may remain inside the 12 m wide strip (d4/2 = 6.0 m)
        String strip = "ST_BUFFER(ST_GeomFromText('LINESTRING Z(-10 0 1, 210 0 1)', 2154), 6.0)"
        assertEquals(0d, sql.firstRow("SELECT COALESCE(SUM(ST_AREA(ST_INTERSECTION(THE_GEOM, " + strip + "))), 0) A FROM LANDCOVER_ENRICHED WHERE G = 0.7").A as double, 1e-6)
        // The strip (200 m x 12 m) is cut away from the 200 m x 40 m polygon
        assertEquals(8000d - 2400d, sql.firstRow("SELECT COALESCE(SUM(ST_AREA(THE_GEOM)), 0) A FROM LANDCOVER_ENRICHED WHERE G = 0.7").A as double, 1e-6)
        // The custom g1=0 zone of the WIDE platform produces G=0 rows
        assertTrue(sql.firstRow("SELECT COUNT(*) C FROM LANDCOVER_ENRICHED WHERE G = 0").C as Integer > 0)
    }

    @Test
    void testEnrichLandcoverWithRailDefaultPlatform() {
        def sql = new Sql(connection)
        // LANDCOVER without a platform column: the default platform must be used
        sql.execute("CREATE TABLE LANDCOVER(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), GROUND_G DOUBLE)")
        sql.execute("""INSERT INTO LANDCOVER VALUES
            (1, ST_GeomFromText('POLYGON((0 -50, 100 -50, 100 50, 0 50, 0 -50))', 2154), 0.7),
            (2, ST_GeomFromText('POLYGON((200 0, 300 0, 300 100, 200 100, 200 0))', 2154), 1)""")
        // RAIL table without a PLATFORM column
        sql.execute("CREATE TABLE RAIL(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154))")
        sql.execute("INSERT INTO RAIL VALUES (1, ST_GeomFromText('LINESTRING Z(-10 0 1, 110 0 1)', 2154))")

        new Enrich_Landcover_with_rail().exec(connection,
                ["inputLandcover": "LANDCOVER", "gColumn": "GROUND_G", "inputRail": "RAIL"])

        assertTrue(JDBCUtilities.tableExists(connection, "LANDCOVER_ENRICHED"))
        // The default platform (DEFAULT) must have been applied: its g1=0 zone produces G=0 rows in the enriched
        // landcover, whereas the input landcover only contained G=0.7 and G=1.0.
        assertTrue(sql.firstRow("SELECT COUNT(*) C FROM LANDCOVER_ENRICHED WHERE G = 0").C as Integer > 0)
    }

    @Test
    void testEnrichLandcoverWithRailLowercasePlatform() {
        def sql = new Sql(connection)
        sql.execute("CREATE TABLE LANDCOVER(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), GROUND_G DOUBLE)")
        sql.execute("""INSERT INTO LANDCOVER VALUES
            (1, ST_GeomFromText('POLYGON((0 -50, 100 -50, 100 50, 0 50, 0 -50))', 2154), 0.7),
            (2, ST_GeomFromText('POLYGON((200 0, 300 0, 300 100, 200 100, 200 0))', 2154), 1)""")
        // PLATFORM value is lowercase: it must be normalized to uppercase (SNCF)
        sql.execute("CREATE TABLE RAIL(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154), PLATFORM VARCHAR)")
        sql.execute("INSERT INTO RAIL VALUES (1, ST_GeomFromText('LINESTRING Z(-10 0 1, 110 0 1)', 2154), 'sncf')")

        new Enrich_Landcover_with_rail().exec(connection,
                ["inputLandcover": "LANDCOVER", "gColumn": "GROUND_G", "inputRail": "RAIL"])

        assertTrue(JDBCUtilities.tableExists(connection, "LANDCOVER_ENRICHED"))
        // The SNCF platform (g1=0, g3=0) must be applied, producing G=0 rows in the enriched landcover.
        assertTrue(sql.firstRow("SELECT COUNT(*) C FROM LANDCOVER_ENRICHED WHERE G = 0").C as Integer > 0)
    }

    @Test
    void testEnrichLandcoverWithRailTwoPlatforms() {
        def sql = new Sql(connection)
        // A single G=0.7 landcover polygon crossed by two parallel rails with different platforms.
        // The combined d4 buffer of both platforms must be subtracted from each polygon once,
        // otherwise the original landcover would remain under the rails with its old G.
        sql.execute("CREATE TABLE LANDCOVER(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), GROUND_G DOUBLE)")
        sql.execute("INSERT INTO LANDCOVER VALUES (1, ST_GeomFromText('POLYGON((0 -20, 200 -20, 200 20, 0 20, 0 -20))', 2154), 0.7)")
        sql.execute("CREATE TABLE RAIL(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154), PLATFORM VARCHAR)")
        sql.execute("INSERT INTO RAIL VALUES (1, ST_GeomFromText('LINESTRING Z(-10 0 1, 210 0 1)', 2154), 'SNCF'), (2, ST_GeomFromText('LINESTRING Z(-10 10 1, 210 10 1)', 2154), 'DEFAULT')")

        new Enrich_Landcover_with_rail().exec(connection,
                ["inputLandcover": "LANDCOVER", "gColumn": "GROUND_G", "inputRail": "RAIL"])

        // No original G=0.7 landcover may remain inside the combined strip of the two platforms
        // (SNCF d4/2 = 4.0 m around the y=0 rail, DEFAULT d4_0/2 = 4.15 m around the y=10 rail)
        String strips = "ST_UNION(ST_BUFFER(ST_GeomFromText('LINESTRING Z(-10 0 1, 210 0 1)', 2154), 4.0), ST_BUFFER(ST_GeomFromText('LINESTRING Z(-10 10 1, 210 10 1)', 2154), 4.15))"
        assertEquals(0d, sql.firstRow("SELECT COALESCE(SUM(ST_AREA(ST_INTERSECTION(THE_GEOM, " + strips + "))), 0) A FROM LANDCOVER_ENRICHED WHERE G = 0.7").A as double, 1e-6)
        // The two strips (200 m x 8 m for SNCF d4=8 and 200 m x 8.3 m for DEFAULT d4=8.3) are cut away
        assertEquals(8000d - 1600d - 1660d, sql.firstRow("SELECT COALESCE(SUM(ST_AREA(THE_GEOM)), 0) A FROM LANDCOVER_ENRICHED WHERE G = 0.7").A as double, 1e-6)
        // Within the landcover extent the area is fully covered (no hole is left behind)
        assertEquals(8000d, sql.firstRow("SELECT COALESCE(SUM(ST_AREA(ST_INTERSECTION(THE_GEOM, ST_GeomFromText('POLYGON((0 -20, 200 -20, 200 20, 0 20, 0 -20))', 2154)))), 0) A FROM LANDCOVER_ENRICHED").A as double, 1e-6)
    }

    @Test
    void testEnrichLandcoverWithRailUnknownPlatform() {
        def sql = new Sql(connection)
        sql.execute("CREATE TABLE LANDCOVER(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), GROUND_G DOUBLE)")
        sql.execute("""INSERT INTO LANDCOVER VALUES
            (1, ST_GeomFromText('POLYGON((0 -50, 100 -50, 100 50, 0 50, 0 -50))', 2154), 0.7)""")
        // PLATFORM value not present in the data file: must be rejected instead of silently dropping rails
        sql.execute("CREATE TABLE RAIL(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154), PLATFORM VARCHAR)")
        sql.execute("INSERT INTO RAIL VALUES (1, ST_GeomFromText('LINESTRING Z(-10 0 1, 110 0 1)', 2154), 'UNKNOWN_PLATFORM')")

        boolean thrown = false
        try {
            new Enrich_Landcover_with_rail().exec(connection,
                    ["inputLandcover": "LANDCOVER", "gColumn": "GROUND_G", "inputRail": "RAIL"])
        } catch (IllegalArgumentException e) {
            thrown = true
        }
        assertTrue(thrown, "Expected IllegalArgumentException for unknown platform")
    }
}
