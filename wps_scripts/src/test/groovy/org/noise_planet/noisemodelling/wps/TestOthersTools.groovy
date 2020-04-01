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

import org.h2gis.functions.io.shp.SHPRead
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Others_Tools.Add_LAEQ_LEQ_columns
import org.noise_planet.noisemodelling.wps.Others_Tools.Change_SRID
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestOthersTools extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestOthersTools.class)

    void testChangeSRID1() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("roads.shp").getPath())

        String res = new Change_SRID().exec(connection,
                ["newSRID": "2154",
                 "tableName": "roads"])

        assertEquals("The table already counts 2154 as SRID.", res)
    }
    void testChangeSRID2() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("roads.shp").getPath())

        String res = new Change_SRID().exec(connection,
                ["newSRID": "4326",
                 "tableName": "roads"])

        assertEquals("SRID changed from 2154 to 4326.", res)
    }

    void testAddLeqLaeqColumns1() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("roads2.shp").getPath())

        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        String res = new Add_LAEQ_LEQ_columns().exec(connection,
                ["prefix": "HZ",
                 "tableName": "LW_ROADS"])

        assertEquals("This table does not contain column with this suffix : HZ", res)
    }


    void testAddLeqLaeqColumns2() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("roads2.shp").getPath())

        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        String res = new Add_LAEQ_LEQ_columns().exec(connection,
                ["prefix": "LWD",
                 "tableName": "LW_ROADS"])

        List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), "LW_ROADS")

        assertEquals(true, fields.contains("LEQ"))
    }


}
