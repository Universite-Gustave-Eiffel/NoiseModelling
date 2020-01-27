/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */

package org.noise_planet.noisemodelling.wps

import org.h2gis.functions.io.shp.SHPRead
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.NoiseModelling.Lden_from_Emission
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_From_AADF
import org.noise_planet.noisemodelling.wps.OSM_Tools.Get_Table_from_OSM
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
        res = new Get_Table_from_OSM().exec(connection,
                ["pathFile": TestTutorialOpenStreetMap.getResource("map.osm.gz").getPath(),
                "targetSRID" : 2154,
                "convert2Building" : true,
                "convert2Vegetation" : true,
                "convert2Roads" : true])
        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("SURFACE_OSM"))
        assertTrue(res.contains("BUILDINGS_OSM"))
        assertTrue(res.contains("ROADS"))

        // Check export geojson
        File testPath = new File("target/test.geojson")

        if(testPath.exists()) {
            testPath.delete()
        }

        res = new Export_Table().exec(connection, ["exportPath" : "target/test.geojson",
                                                   "tableToExport": "BUILDINGS_OSM"])
        assertTrue(testPath.exists())

        // Check regular grid

        res = new Regular_Grid().exec(connection, ["delta": 50,
        "sourcesTableName": "ROADS",
        "buildingTableName": "BUILDINGS_OSM"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("RECEIVERS"))

        new Road_Emission_From_AADF().exec(connection, ["sourcesTableName": "ROADS"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("LW_ROADS"))

        res = new Lden_from_Emission().exec(connection, ["sourcesTableName": "LW_ROADS",
        "buildingTableName": "BUILDINGS_OSM",
        "groundTableName": "SURFACE_OSM"])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("LDEN_GEOM"))
    }

}
