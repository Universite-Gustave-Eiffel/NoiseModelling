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

package org.noise_planet.noisemodelling.wps

import org.h2gis.functions.io.geojson.GeoJsonRead
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Experimental.Get_Rayz
import org.noise_planet.noisemodelling.wps.Experimental.Multi_Runs

class TestExperimental extends JdbcTestCase  {

    @Test
    void testMultiRun() {

        GeoJsonRead.readGeoJson(connection, TestExperimental.class.getResource("multirun/buildings.geojson").getPath())
        GeoJsonRead.readGeoJson(connection, TestExperimental.class.getResource("multirun/receivers.geojson").getPath())
        GeoJsonRead.readGeoJson(connection, TestExperimental.class.getResource("multirun/sources.geojson").getPath())

        new Add_Primary_Key().exec(connection,
                ["pkName":"PK",
                 "tableName" : "RECEIVERS"])

        new Add_Primary_Key().exec(connection,
                 ["pkName":"PK",
                  "tableName" : "SOURCES"])


        new Get_Rayz().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "roadsTableName"   : "SOURCES",
                 "tableReceivers": "RECEIVERS",
                 "exportPath"   : TestExperimental.class.getResource("multirun/").getPath()])


        new Multi_Runs().exec(connection,
                ["workingDir":TestExperimental.class.getResource("multirun/").getPath()])



      String res =   new Table_Visualization_Data().exec(connection,
                ["tableName": "MultiRunsResults_geom"])

        System.out.println(res)
        assertTrue(res.startsWith("The total number of rows is 4</br>The srid of the table is 2154"));
    }


}
