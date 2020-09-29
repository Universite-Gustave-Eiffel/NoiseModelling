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

import org.h2gis.functions.io.shp.SHPRead
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Database_Manager.Add_Primary_Key
import org.noise_planet.noisemodelling.wps.Database_Manager.Clean_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Database_Manager.Drop_a_Table
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Map
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Statement

/**
 * Test parsing of zip file using H2GIS database
 */
class TestDatabaseManager extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestDatabaseManager.class)

    @Test
    void testAddPrimaryKey1() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())
        Statement stmt = connection.createStatement()
        stmt.execute("ALTER TABLE receivers DROP PRIMARY KEY;")

        String res = new Add_Primary_Key().exec(connection,
                ["pkName": "ID",
                 "tableName" : "receivers"])

        assertEquals("RECEIVERS has a new primary key column which is called ID.", res)
    }

    @Test
    void testAddPrimaryKey2() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())
        Statement stmt = connection.createStatement()
        stmt.execute("ALTER TABLE receivers DROP PRIMARY KEY;")

        String res = new Add_Primary_Key().exec(connection,
                ["pkName": "PK",
                 "tableName" : "receivers"])

        assertEquals("RECEIVERS has a new primary key constraint on PK.", res)
    }

    @Test
    void testAddPrimaryKey3() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())

        String res = new Add_Primary_Key().exec(connection,
                ["pkName": "PK",
                 "tableName" : "receivers"])

        assertEquals("Warning : Source table RECEIVERS did already contain a primary key. The constraint has been removed. </br>RECEIVERS has a new primary key constraint on PK.", res)
    }

    @Test
    void testCleanDatabase() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())

        String res = new Clean_Database().exec(connection,
                ["areYouSure": true ])

        assertEquals("The table(s) RECEIVERS was/were dropped.", res)
    }

    @Test
    void testDropTable() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())

        String res = new Drop_a_Table().exec(connection,
                ["tableToDrop": "receivers" ])

        assertEquals("The table RECEIVERS was dropped !", res)
    }

    @Test
    void testDisplayTables1() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("buildings.shp").getPath())
        String res = new Display_Database().exec(connection, [])
        assertEquals("BUILDINGS</br></br>", res)
    }

    @Test
    void testTableVisualizationMap() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())
        def res = new Table_Visualization_Map().exec(connection,
                ["tableName": "receivers" ])
        assertTrue(res instanceof org.locationtech.jts.geom.MultiPoint)
    }

    @Test
    void testTableVisualizationData() {
        SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())
        String res = new Table_Visualization_Data().exec(connection,
                ["tableName": "receivers" ])
        assertTrue(res.contains("The total number of rows is 830"))
        assertTrue(res.contains("The srid of the table is 2154"))
        assertTrue(res.contains("POINT (223495.9880411485 6757167.98900822 0)"))
    }
}
