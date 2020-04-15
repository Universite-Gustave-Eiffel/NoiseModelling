/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 */

package org.noise_planet.noisemodelling.wps

import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.NoiseModelling.Lden_from_Road_Emission
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Others_Tools.OsmToInputData
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestTutorialOpenStreetMap extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestTutorialOpenStreetMap.class)

    void testTutorial() {
        // Check empty database
        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)
        // Import OSM file
        res = new OsmToInputData().exec(connection,
                ["pathFile": TestTutorialOpenStreetMap.getResource("map.osm.gz").getPath(),
                 "targetSRID" : 2154,
                 "convert2Building" : true,
                 "convert2Ground" : true,
                 "convert2Roads" : true])

        // Check SRID
        assertEquals(2154, SFSUtilities.getSRID(connection, TableLocation.parse("BUILDINGS")))

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("GROUND"))
        assertTrue(res.contains("BUILDINGS"))
        assertTrue(res.contains("ROADS"))

        // Check export geojson
        File testPath = new File("target/test.geojson")

        if(testPath.exists()) {
            testPath.delete()
        }

        res = new Export_Table().exec(connection, ["exportPath" : "target/test.geojson",
                                                   "tableToExport": "BUILDINGS"])
        assertTrue(testPath.exists())

        // Check regular grid

        res = new Regular_Grid().exec(connection, ["delta": 50,
                                                   "sourcesTableName": "ROADS",
                                                   "buildingTableName": "BUILDINGS"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("RECEIVERS"))

        new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("LW_ROADS"))

        res = new Lden_from_Road_Emission().exec(connection, ["tableSources"  : "LW_ROADS",
                                                              "tableBuilding" : "BUILDINGS",
                                                              "tableGroundAbs": "GROUND",
                                                              "tableReceivers": "RECEIVERS"])

        res =  new Display_Database().exec(connection, [])

        // Check database
        new Table_Visualization_Data().exec(connection, ["tableName": "LDEN_GEOM"])

        assertTrue(res.contains("LDEN_GEOM"))
    }

}
