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
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.NoiseModelling.Lday_from_Traffic
import org.noise_planet.noisemodelling.wps.NoiseModelling.Lden_from_Road_Emission
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestNoiseModelling extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestNoiseModelling.class)

    void testRoadEmissionFromDEN() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("roads2.shp").getPath())

        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])


        assertEquals("Calculation Done ! The table LW_ROADS has been created.", res)
    }

    void testLdayFromTraffic() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("roads2.shp").getPath())

        //SHPRead.readShape(connection, TestDatabaseManager.getResource("buildings.shp").getPath())
        new Import_File().exec(connection,
                ["pathFile" : TestImportExport.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        //SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())
        new Import_File().exec(connection,
                ["pathFile" : TestImportExport.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


       String res = new Lday_from_Traffic().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableRoads"   : "ROADS2",
                 "tableReceivers": "RECEIVERS"])


        assertEquals("Calculation Done ! The table LDAY_GEOM has been created.", res)
    }

    void testLdenFromEmission() {

        SHPRead.readShape(connection, TestDatabaseManager.getResource("roads2.shp").getPath())

        String res = new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "ROADS2"])

        //SHPRead.readShape(connection, TestDatabaseManager.getResource("buildings.shp").getPath())
        res = new Import_File().exec(connection,
                ["pathFile" : TestImportExport.getResource("buildings.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "buildings"])

        //SHPRead.readShape(connection, TestDatabaseManager.getResource("receivers.shp").getPath())
        res = new Import_File().exec(connection,
                ["pathFile" : TestImportExport.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])


        res = new Lden_from_Road_Emission().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "LW_ROADS",
                 "tableReceivers": "RECEIVERS"])


        assertEquals("Calculation Done ! The table LDEN_GEOM has been created.", res)
    }

}
