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
import org.h2gis.utilities.JDBCUtilities
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Railway_Emission_from_Traffic
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Terts_to_octave
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.junit.jupiter.api.Assertions.*

class TestTertsToOctave extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestTertsToOctave.class)

    @Test
    void testTertsToOctaveTable(@TempDir File temp) {

        def sql = new Sql(connection)

        sql.execute("DROP TABLE IF EXISTS LW_RAILWAY")
        sql.execute("DROP TABLE IF EXISTS LW_RAILWAY_OCTAVE")


        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("Train/RAIL_SECTIONS.shp").getPath(),
                 "inputSRID": "2154"])

        new Import_File().exec(connection,
                ["pathFile" : TestNoiseModelling.getResource("Train/RAIL_TRAFFIC.dbf").getPath(),
                 "inputSRID": "2154"])

        new Railway_Emission_from_Traffic().exec(connection,
                ["tableRailwayTraffic": "RAIL_TRAFFIC",
                 "tableRailwayTrack": "RAIL_SECTIONS"
                ])

        new Terts_to_octave().exec(connection,["tableName": "LW_RAILWAY"])

        // Test columns
        def fieldNamesOctave = JDBCUtilities.getColumnNames(connection, "LW_RAILWAY_OCTAVE")
        def expectedOctave = ["PK_SECTION","PK","THE_GEOM","DIR_ID","GS","HRAIL","CREF","HZD63","HZD125","HZD250",
                              "HZD500","HZD1000","HZD2000","HZD4000","HZD8000","HZE63","HZE125","HZE250","HZE500",
                              "HZE1000","HZE2000","HZE4000","HZE8000","HZN63","HZN125","HZN250","HZN500","HZN1000",
                              "HZN2000","HZN4000","HZN8000"]
        assertArrayEquals(expectedOctave.toArray(new String[expectedOctave.size()]),
                fieldNamesOctave.toArray(new String[fieldNamesOctave.size()]))

        // Test rows
        def tertsSize = sql.rows("SELECT COUNT(*) CPT FROM LW_RAILWAY")
        def octaveSize = sql.rows("SELECT COUNT(*) CPT FROM LW_RAILWAY_OCTAVE")
        assertEquals(tertsSize[0]["CPT"] as Integer, octaveSize[0]["CPT"] as Integer)

        // Test content
        def tertsbands = sql.rows("SELECT * FROM LW_RAILWAY WHERE PK=1")
        def octaveLevel = sql.rows("SELECT * FROM LW_RAILWAY_OCTAVE WHERE PK=1 ")
        def expectedOctaveLevel = 10*Math.log10(10**(tertsbands[0]["HZD50"]/10) +
                                                10**(tertsbands[0]["HZD63"]/10)  +
                                                10**(tertsbands[0]["HZD80"]/10))
        assertEquals(expectedOctaveLevel, octaveLevel[0]["HZD63"] as double, 0.001)
    }
}
