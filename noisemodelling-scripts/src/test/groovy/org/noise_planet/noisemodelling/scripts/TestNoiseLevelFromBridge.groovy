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

import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * End-to-end for the WPS block: passing a BRIDGE_POINTS table to Noise_level_from_source builds an
 * elevated deck that shields a ground receiver, lowering its level compared with the same scene
 * without the table.
 */
class TestNoiseLevelFromBridge extends JdbcTestCase {

    private void buildScene(Sql sql) {
        sql.execute("DROP TABLE IF EXISTS BUILDINGS")
        sql.execute("CREATE TABLE BUILDINGS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POLYGON, 2154), HEIGHT DOUBLE)")

        sql.execute("DROP TABLE IF EXISTS DEM")
        sql.execute("CREATE TABLE DEM(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POINTZ, 2154))")
        for (int x = -40; x <= 140; x += 30) {
            for (int y = -40; y <= 140; y += 30) {
                sql.execute("INSERT INTO DEM(THE_GEOM) VALUES (ST_SetSRID(ST_MakePoint(${x}, ${y}, 0), 2154))")
            }
        }

        // one short line source on the deck (absolute Z ~ 10.6), inside the deck footprint (strip at y=20)
        StringBuilder cols = new StringBuilder("PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRINGZ, 2154)")
        StringBuilder vals = new StringBuilder("1, ST_SetSRID('LINESTRING Z(40 20 10.6, 60 20 10.6)'::geometry, 2154)")
        for (String f : ["63", "125", "250", "500", "1000", "2000", "4000", "8000"]) {
            cols.append(", HZD").append(f).append(" DOUBLE")
            vals.append(", 95.0")
        }
        sql.execute("DROP TABLE IF EXISTS LW_ROADS")
        sql.execute("CREATE TABLE LW_ROADS(" + cols + ")")
        sql.execute("INSERT INTO LW_ROADS VALUES(" + vals + ")")

        sql.execute("DROP TABLE IF EXISTS RECEIVERS")
        sql.execute("CREATE TABLE RECEIVERS(PK SERIAL PRIMARY KEY, THE_GEOM GEOMETRY(POINTZ, 2154))")
        sql.execute("INSERT INTO RECEIVERS(THE_GEOM) VALUES (ST_SetSRID(ST_MakePoint(50, 75, 4), 2154))")

        sql.execute("DROP TABLE IF EXISTS BRIDGE_POINTS")
        sql.execute("CREATE TABLE BRIDGE_POINTS(PK INT, BRIDGE_PK INT, THE_GEOM GEOMETRY(POINT, 2154), " +
                "ABSOLUTE_DECK_HEIGHT DOUBLE, RELATIVE_DECK_HEIGHT DOUBLE, DECK_THICKNESS DOUBLE, " +
                "RIGHT_WIDTH DOUBLE, LEFT_WIDTH DOUBLE, RIGHT_BARRIER_HEIGHT DOUBLE, LEFT_BARRIER_HEIGHT DOUBLE, " +
                "POSITION VARCHAR, GIRDER_TYPE VARCHAR, SLAB_TYPE VARCHAR)")
        for (int i = 0; i < 3; i++) {
            sql.execute("INSERT INTO BRIDGE_POINTS VALUES(${i}, 100, ST_SetSRID(ST_MakePoint(${i * 50}, 20), 2154), " +
                    "10.0, NULL, 0.5, 5.0, 5.0, 1.0, 1.0, 'CENTER', 'STEEL_BOX', 'STEEL')")
        }
    }

    private static final List<Integer> BANDS = [63, 125, 250, 500, 1000, 2000, 4000, 8000]

    private Map runAndGetLevel(Map extraInputs) {
        Sql sql = new Sql(connection)
        buildScene(sql)
        sql.execute("DROP TABLE IF EXISTS " + NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME)

        Map inputs = ["tableBuilding"           : "BUILDINGS",
                      "tableSources"            : "LW_ROADS",
                      "tableReceivers"          : "RECEIVERS",
                      "tableDEM"                : "DEM",
                      "confSourcesZIsAltitude"  : true,
                      "confReceiversZIsAltitude": true,
                      "confMaxSrcDist"          : 250,
                      "confReflOrder"           : 0,
                      // NB: Noise_level_from_source currently swaps its two diffraction setters,
                      // so confDiffHorizontal is what actually enables vertical (over-the-edge) diffraction.
                      "confDiffHorizontal"      : true]
        inputs.putAll(extraInputs)
        new Noise_level_from_source().exec(connection, inputs)

        String cols = BANDS.collect { "HZ" + it }.join(", ")
        def rows = sql.rows("SELECT " + cols + ", LEQ, LAEQ FROM " +
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'D'")
        assertTrue(rows.size() >= 1)
        return rows[0]
    }

    /** CNOSSOS-EU homogeneous single-diffraction attenuation for a path difference, c = 340 m/s. */
    private static double cnossosDeltaDif(double pathDiff, int freqHz) {
        double lambda = 340.0 / freqHz
        return 10.0 * Math.log10(3.0 + 20.0 * pathDiff / lambda)   // C2 = 20, C3 = 1
    }

    @Test
    void bridgeDeckShieldsAGroundReceiver() {
        Map without = runAndGetLevel([:])
        Map with = runAndGetLevel(["tableBridgePoints": "BRIDGE_POINTS"])

        // The controlling obstacle is the receiver-side parapet top. In the x = 50 plane (the source
        // line covers x = 50 and the receiver is at x = 50) the problem is 2-D:
        //   source on the deck   S = (y 20, z 10.6)
        //   parapet top          E = (y 25, z 11.0)   deck centre y 20, half-width 5; deck top 10.0 + barrier 1.0
        //   ground receiver      R = (y 75, z  4.0)
        //   delta = |SE| + |ER| - |SR| = 5.016 + 50.488 - 55.395
        double pathDiff = Math.hypot(5, 0.4) + Math.hypot(50, 7) - Math.hypot(55, 6.6)
        assertTrue(Math.abs(pathDiff - 0.109) < 0.005, "geometry drift: delta = ${pathDiff}")

        double[] loss = new double[BANDS.size()]
        BANDS.eachWithIndex { f, i ->
            loss[i] = (without["HZ" + f] as Double) - (with["HZ" + f] as Double)
            double predicted = cnossosDeltaDif(pathDiff, f)
            // Lower bound: the shadow is at least the bare geometric diffraction term.
            // Upper bound: at mid/high frequency the deck also cuts the elevated source's ground
            //   reflection, adding up to ~3.5 dB on top of the diffraction term.
            assertTrue(loss[i] > predicted - 1.5 && loss[i] < predicted + 5.0,
                    "${f} Hz: loss ${String.format('%.2f', loss[i])} dB outside " +
                    "[${String.format('%.2f', predicted - 1.5)}, ${String.format('%.2f', predicted + 5.0)}] " +
                    "(CNOSSOS dDif = ${String.format('%.2f', predicted)})")
        }

        // Diffraction attenuation grows with frequency (N proportional to f) -> the shadow deepens.
        for (int i = 1; i < loss.length; i++) {
            assertTrue(loss[i] > loss[i - 1] + 0.3,
                    "shadow should deepen with frequency: ${BANDS[i - 1]} Hz ${String.format('%.2f', loss[i - 1])} dB " +
                    ">= ${BANDS[i]} Hz ${String.format('%.2f', loss[i])} dB")
        }

        double leqLoss = (without["LEQ"] as Double) - (with["LEQ"] as Double)
        double laeqLoss = (without["LAEQ"] as Double) - (with["LAEQ"] as Double)
        assertTrue(leqLoss > 6.0 && leqLoss < 12.0, "unweighted insertion loss ${String.format('%.2f', leqLoss)} dB outside [6, 12]")
        assertTrue(laeqLoss > 10.0 && laeqLoss < 17.0, "A-weighted insertion loss ${String.format('%.2f', laeqLoss)} dB outside [10, 17]")
        assertTrue((with["LEQ"] as Double) > 40.0, "a physical level is expected, not a rejected-path floor: ${with['LEQ']}")
    }

    /** Import the TutoBridge dataset and run the WPS block with and without the deck, per receiver. */
    private Map<Integer, List<Double>> runTutoBridge() {
        Sql sql = new Sql(connection)
        String dir = TestNoiseLevelFromBridge.getResource("TutoBridge").getPath()
        Map<String, String> tables = ["DEM": "dem", "BUILDINGS": "buildings", "LW_ROADS": "lw_roads",
                                      "BRIDGE_POINTS": "bridgepoints", "RECEIVERS": "receivers"]
        tables.each { table, file ->
            new Import_File().exec(connection, ["pathFile" : dir + "/" + file + ".geojson",
                                               "inputSRID": "2154", "tableName": table])
            // Sql GStrings would bind ${table} as a parameter; build table names by concatenation.
            sql.execute("UPDATE " + table + " SET THE_GEOM = ST_SetSRID(THE_GEOM, 2154)")
        }
        for (String t : ["LW_ROADS", "RECEIVERS"]) {
            try { sql.execute("ALTER TABLE " + t + " ADD PRIMARY KEY(PK)") } catch (ignored) { /* import already added one */ }
        }

        Map base = ["tableBuilding"           : "BUILDINGS",
                    "tableSources"            : "LW_ROADS",
                    "tableReceivers"          : "RECEIVERS",
                    "tableDEM"                : "DEM",
                    "confSourcesZIsAltitude"  : true,
                    "confReceiversZIsAltitude": true,
                    "confMaxSrcDist"          : 300,
                    "confReflOrder"           : 0, "confDiffHorizontal": true]

        Map<Integer, List<Double>> byReceiver = [:]
        [false, true].eachWithIndex { boolean withBridge, int idx ->
            sql.execute("DROP TABLE IF EXISTS " + NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME)
            Map inputs = new HashMap(base)
            if (withBridge) {
                inputs.put("tableBridgePoints", "BRIDGE_POINTS")
            }
            new Noise_level_from_source().exec(connection, inputs)
            sql.rows("SELECT IDRECEIVER, LEQ FROM " +
                    NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME + " WHERE PERIOD = 'D'").each { row ->
                byReceiver.computeIfAbsent(row["IDRECEIVER"] as Integer, { [null, null] as List<Double> }).set(idx, row["LEQ"] as Double)
            }
        }
        return byReceiver
    }

    @Test
    void tutoBridgeCastsANoiseShadow() {
        // Deck spans x in [0, 120], centre line y = 50; receivers on the ground at y = 75,
        // x = -30 (PK 1) .. 180 (PK 22). Behind the deck the elevated source + 2 m barrier
        // should knock ~10-15 dB off; well past the deck end the effect is smaller.
        Map<Integer, List<Double>> r = runTutoBridge()
        assertTrue(r.size() >= 20, "a level is expected for every receiver, got ${r.size()}")

        double behindWithout = r[10][0], behindWith = r[10][1]   // x = 60, behind the middle
        double pastWithout = r[22][0], pastWith = r[22][1]       // x = 180, well past the deck

        assertTrue(behindWith < behindWithout - 8.0,
                "a receiver behind the deck should lose a lot: without=${behindWithout}, with=${behindWith}")
        assertTrue(behindWith > behindWithout - 30.0,
                "...but a physical amount, not a rejected-path floor: without=${behindWithout}, with=${behindWith}")
        assertTrue((pastWithout - pastWith) < (behindWithout - behindWith),
                "the shadow should be deeper behind the deck than past its end")
    }
}
