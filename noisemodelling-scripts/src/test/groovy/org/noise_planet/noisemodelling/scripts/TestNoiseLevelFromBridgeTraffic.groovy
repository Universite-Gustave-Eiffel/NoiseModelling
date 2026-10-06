/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.scripts

import groovy.sql.Sql
import org.junit.jupiter.api.Test
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Road_Emission_from_Traffic

import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Real-dataset counterpart to {@code fix}'s (the fork's) {@code TestNoiseLevelFromBridgeTraffic}:
 * same TutoBridgeTraffic fixtures (Seishin Bypass, Shizuoka - raw traffic-flow {@code roads.geojson}
 * plus matching real {@code bridgepoints}/{@code buildings}/{@code receivers}, all copied from the
 * fork and stripped of their embedded EPSG:6676 CRS declaration so they import like
 * {@code TestNoiseLevelFromBridge}'s existing synthetic TutoBridge fixtures under a forced SRID),
 * same BRIDGE_PK-after-import pattern, but driven through the split Road_Emission_from_Traffic -&gt;
 * Noise_level_from_source pipeline (merge_project.md §4.x "BRIDGE_PK による音源→デッキ高さの引き継ぎ")
 * instead of the fork's single monolithic {@code Noise_level_from_traffic} block (deprecated
 * upstream, not reimplemented here - see merge_project.md for why).
 *
 * <p>{@code roads.geojson} has no BRIDGE_PK column, so - exactly like the fork's test - it is
 * added and linked to bridge 1 after import.
 *
 * <p><b>{@code bridgepoints.geojson} fixture note:</b> the fork's original fixture set both
 * {@code ABSOLUTE_DECK_HEIGHT} (8.3 m) and {@code RELATIVE_DECK_HEIGHT} (7.8 m) on every point;
 * {@code DefaultTableLoader} prefers the absolute value when both are present. 8.3 m absolute is
 * ~21 m *below* {@code dem.geojson}'s real ~30 m ground elevation for this scene - a pre-existing
 * inconsistency (present in {@code fix} too; its own test never checks that the levels are
 * physically sensible, only that the output table exists) that puts the deck underground and
 * silences every ray once a DEM is in the scene. Fixed here by dropping ABSOLUTE_DECK_HEIGHT from
 * the fixture so the already-present RELATIVE_DECK_HEIGHT (deck height above the ground it
 * actually sits on) resolves instead.
 */
class TestNoiseLevelFromBridgeTraffic extends JdbcTestCase {

    @Test
    void testBridgeTraffic() {
        Sql sql = new Sql(connection)
        String dir = TestNoiseLevelFromBridgeTraffic.getResource("TutoBridgeTraffic").getPath()

        Map<String, String> tables = ["DEM": "dem", "BUILDINGS": "buildings", "ROADS": "roads",
                                      "BRIDGE_POINTS": "bridgepoints", "RECEIVERS": "receivers"]
        tables.each { table, file ->
            new Import_File().exec(connection, ["pathFile" : dir + "/" + file + ".geojson",
                                               "inputSRID": "2154", "tableName": table])
            sql.execute("UPDATE " + table + " SET THE_GEOM = ST_SetSRID(THE_GEOM, 2154)")
        }
        for (String t : ["ROADS", "RECEIVERS"]) {
            try { sql.execute("ALTER TABLE " + t + " ADD PRIMARY KEY(PK)") } catch (ignored) { /* import already added one */ }
        }

        // ROADS.geojson has no BRIDGE_PK column - add and link all segments to bridge 1,
        // exactly like the fork's TestNoiseLevelFromBridgeTraffic#testBridgeTraffic.
        sql.execute("ALTER TABLE ROADS ADD COLUMN BRIDGE_PK INT")
        sql.execute("UPDATE ROADS SET BRIDGE_PK = 1")

        String lwTable = new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS"]).result

        // Emission carried the BRIDGE_PK through: on-deck sources resolve against the deck.
        def bridgePkValues = sql.rows("SELECT DISTINCT BRIDGE_PK FROM " + lwTable).collect { it["BRIDGE_PK"] }
        assertTrue(bridgePkValues == [1], "expected LW_ROADS.BRIDGE_PK to all be 1, got ${bridgePkValues}")

        sql.execute("DROP TABLE IF EXISTS " + NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME)
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"     : "BUILDINGS",
                 "tableSources"      : lwTable,
                 "tableReceivers"    : "RECEIVERS",
                 "tableBridgePoints" : "BRIDGE_POINTS",
                 "tableDEM"          : "DEM",
                 "confMaxSrcDist"    : 500.0,
                 "confDiffHorizontal": true,
                 "confReflOrder"     : 0])

        def rows = sql.rows("SELECT PERIOD, LEQ FROM " + NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME)
        assertTrue(rows.size() > 0, "expected at least one receiver level row")

        // roads.geojson only carries LV/MV traffic in the _e (evening) columns (_d/_n are all
        // zero) and has no PERIOD column, so Road_Emission_from_Traffic emits the wide
        // HZD*/HZE*/HZN* form and Noise_level_from_source folds it into a single combined DEN
        // period - unlike the fork's separate D/E/N/DEN rows, but the point here is that the
        // BRIDGE_PK-carrying pipeline runs end to end on the real dataset and yields physical
        // levels. D and N are correctly silent (zero traffic in those periods in this fixture).
        def byPeriod = rows.groupBy { it["PERIOD"] as String }
        assertTrue(byPeriod.containsKey("E"), "expected an E period, got ${byPeriod.keySet()}")
        assertTrue(byPeriod.containsKey("DEN"), "expected a DEN period, got ${byPeriod.keySet()}")
        for (String period : ["E", "DEN"]) {
            byPeriod[period].each { row ->
                double leq = row["LEQ"] as Double
                assertTrue(leq > 20.0 && leq < 150.0,
                        "period ${period}: LEQ ${leq} dB outside a physically plausible range " +
                        "for the real evening traffic in this fixture")
            }
        }
    }
}
