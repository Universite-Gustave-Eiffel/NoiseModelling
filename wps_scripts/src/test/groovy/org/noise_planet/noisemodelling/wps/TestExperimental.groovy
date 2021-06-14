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
        assertEquals(res, "The total number of rows is 4</br>The srid of the table is 2154</br>This table does not have primary key.</br> </br> <table  border=' 1px solid black'><thead><tr><th>IDRUN</th><th>IDRECEIVER</th><th>THE_GEOM</th><th>PK</th><th>HZ63</th><th>HZ125</th><th>HZ250</th><th>HZ500</th><th>HZ1000</th><th>HZ2000</th><th>HZ4000</th><th>HZ8000</th></tr></thead><tbody><tr><td><div style='width: 150px;'>0</div></td><td><div style='width: 150px;'>4004</div></td><td><div style='width: 150px;'>POINT (255936.71727636637 6741039.3820958175 4)</div></td><td><div style='width: 150px;'>4004</div></td><td><div style='width: 150px;'>67.19989916385938</div></td><td><div style='width: 150px;'>58.01181689605546</div></td><td><div style='width: 150px;'>56.46584496186841</div></td><td><div style='width: 150px;'>56.37601752391478</div></td><td><div style='width: 150px;'>57.63244358995122</div></td><td><div style='width: 150px;'>53.979858952204054</div></td><td><div style='width: 150px;'>48.09726129844285</div></td><td><div style='width: 150px;'>40.280843130896436</div></td></tr><tr><td><div style='width: 150px;'>0</div></td><td><div style='width: 150px;'>4002</div></td><td><div style='width: 150px;'>POINT (255836.71727636637 6741039.3820958175 4)</div></td><td><div style='width: 150px;'>4002</div></td><td><div style='width: 150px;'>78.41711811741729</div></td><td><div style='width: 150px;'>70.68565204799968</div></td><td><div style='width: 150px;'>69.41063774146896</div></td><td><div style='width: 150px;'>70.38561496973871</div></td><td><div style='width: 150px;'>72.72821358954391</div></td><td><div style='width: 150px;'>68.64717718211922</div></td><td><div style='width: 150px;'>61.72546770226745</div></td><td><div style='width: 150px;'>53.690928843721444</div></td></tr><tr><td><div style='width: 150px;'>0</div></td><td><div style='width: 150px;'>4106</div></td><td><div style='width: 150px;'>POINT (255836.71727636637 6741089.3820958175 4)</div></td><td><div style='width: 150px;'>4106</div></td><td><div style='width: 150px;'>73.32523882222326</div></td><td><div style='width: 150px;'>64.1606662329585</div></td><td><div style='width: 150px;'>62.61711349680972</div></td><td><div style='width: 150px;'>62.53536002553237</div></td><td><div style='width: 150px;'>63.77698294817418</div></td><td><div style='width: 150px;'>60.129412764258994</div></td><td><div style='width: 150px;'>54.29815854737755</div></td><td><div style='width: 150px;'>46.67475472227882</div></td></tr><tr><td><div style='width: 150px;'>0</div></td><td><div style='width: 150px;'>4003</div></td><td><div style='width: 150px;'>POINT (255886.71727636637 6741039.3820958175 4)</div></td><td><div style='width: 150px;'>4003</div></td><td><div style='width: 150px;'>72.67656939890026</div></td><td><div style='width: 150px;'>63.857884479510865</div></td><td><div style='width: 150px;'>62.429563901715845</div></td><td><div style='width: 150px;'>62.83108926715342</div></td><td><div style='width: 150px;'>64.83388827529842</div></td><td><div style='width: 150px;'>60.94827165451443</div></td><td><div style='width: 150px;'>54.3564855484474</div></td><td><div style='width: 150px;'>45.97623611293134</div></td></tr></tbody></table>")
    }


}
