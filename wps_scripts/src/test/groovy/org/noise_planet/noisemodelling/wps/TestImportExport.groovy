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
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_Asc_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_Folder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestImportExport extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestImportExport.class)

    void testImportFile1() {

        String res = new Import_File().exec(connection,
                ["pathFile" : TestImportExport.getResource("receivers.shp").getPath(),
                 "inputSRID": "2154",
                 "tableName": "receivers"])

        assertEquals("The table RECEIVERS has been uploaded to database!", res)
    }

    void testImportFile2() {

        String res = new Import_File().exec(connection,
                ["pathFile" : TestImportExport.getResource("receivers.shp").getPath(),
                 "inputSRID": "4362",
                 "tableName": "receivers"])

        assertEquals("The table already has a different SRID than the one you gave.", res)
    }

    void testImportAsc() {

        String res = new Import_Asc_File().exec(connection,
                ["pathFile" : TestImportExport.getResource("precip30min.asc").getPath(),
                 "inputSRID": "2154"])

        assertEquals("The table DEM has been uploaded to database ! </br>  Its SRID is : 4326. </br> Remember that to calculate a noise map, your SRID must be in metric coordinates. Please use the Wps block 'Change SRID' if needed.", res)
    }


    void testImportFolder() {

        File file = new File(TestImportExport.getResource("receivers.shp").getPath()).getParentFile()
        String res = new Import_Folder().exec(connection,
                ["pathFolder": file.getPath(),
                 "inputSRID" : "2154",
                 "importExt" : "shp"])

        assertTrue(res.contains("ROADS2"))
        assertTrue(res.contains("ROADS"))
        assertTrue(res.contains("RECEIVERS"))
        assertTrue(res.contains("GROUND_TYPE"))
        assertTrue(res.contains("BUILDINGS"))
    }

    void testExportFile() {

        SHPRead.readShape(connection, TestImportExport.getResource("receivers.shp").getPath())

        String res = new Export_Table().exec(connection,
                ["exportPath"   : TestImportExport.getResource("receivers.shp").getPath(),
                 "tableToExport": "RECEIVERS"])


        assertTrue(res.contains("RECEIVERS"))
        assertTrue(res.contains("2154"))
    }

}
