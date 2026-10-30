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

import groovy.json.JsonOutput
import groovy.sql.Sql
import org.junit.jupiter.api.Test
import org.noise_planet.noisemodelling.scripts.Explain.Explain_Rays

import static org.junit.jupiter.api.Assertions.*

class TestExplainRays extends JdbcTestCase {

    private static final String METEO = 'homogeneous'
    private static final String PERIOD = 'D'

    private void createRaysTable() {
        Sql sql = new Sql(connection)
        sql.execute('DROP TABLE IF EXISTS RAYS')
        sql.execute('CREATE TABLE RAYS (IDRECEIVER INTEGER, IDSOURCE INTEGER, LEQ DOUBLE, METEO VARCHAR, PERIOD VARCHAR, PATH VARCHAR)')
    }

    private void insertRay(int receiver, int source, Double leq, String meteo, String period, String path) {
        Sql sql = new Sql(connection)
        sql.execute('INSERT INTO RAYS (IDRECEIVER, IDSOURCE, LEQ, METEO, PERIOD, PATH) VALUES (?, ?, ?, ?, ?, ?)',
                [receiver, source, leq, meteo, period, path])
    }

    private static String pathJson(int source, int receiver, String kind = 'direct', boolean includeAttenuation = true, int bandCount = 1) {
        boolean reflected = kind == 'reflected' || kind == 'mixed'
        boolean diffracted = kind == 'diffracted' || kind == 'mixed'
        boolean rayleigh = kind == 'rayleigh'
        List<Map<String, Object>> cutPoints = new ArrayList<>()
        cutPoints.add([type: 'Source', coordinate: [x: 0d, y: 0d, z: 2d], zGround: 0d, groundCoefficient: 0.5d])
        if (reflected) {
            cutPoints.add([type: 'Reflection', coordinate: [x: 10d, y: 0d, z: 5d], zGround: 0d, groundCoefficient: 0.3d])
        }
        if (diffracted) {
            cutPoints.add([type: 'VEdgeDiffraction', coordinate: [x: 15d, y: 0d, z: 6d], zGround: 0d, groundCoefficient: 0.7d])
        }
        if (rayleigh) {
            cutPoints.add([type: 'Topography', coordinate: [x: 10d, y: 0d, z: 5d], zGround: 5d, groundCoefficient: 0.4d])
        }
        cutPoints.add([type: 'Receiver', coordinate: [x: 20d, y: 0d, z: 2d], zGround: 0d, groundCoefficient: 0.5d])

        List<Map<String, Object>> pointList = new ArrayList<>()
        pointList.add([type: 'SRCE', coordinate: [x: 0d, y: 0d, z: 2d]])
        if (reflected) {
            pointList.add([type: 'REFL', coordinate: [x: 10d, y: 0d, z: 5d]])
        }
        if (diffracted) {
            pointList.add([type: 'DIFH', coordinate: [x: 15d, y: 0d, z: 6d]])
        }
        if (rayleigh) {
            pointList.add([type: 'DIFH_RCRIT', coordinate: [x: 10d, y: 0d, z: 5d]])
        }
        pointList.add([type: 'RECV', coordinate: [x: 20d, y: 0d, z: 2d]])

        List<List<Double>> rayCoordinates = [[0d, 0d, 2d]]
        if (reflected) {
            rayCoordinates.add([10d, 0d, 5d])
        }
        if (diffracted) {
            rayCoordinates.add([15d, 0d, 6d])
        }
        if (rayleigh) {
            rayCoordinates.add([10d, 0d, 5d])
        }
        rayCoordinates.add([20d, 0d, 2d])

        Map<String, Object> path = [
                cutProfile     : [cutPoints: cutPoints, curvedPath: false, profileType: 'DIRECT'],
                lineString     : [type: 'LineString', coordinates: rayCoordinates],
                propagationPath: [pointList: pointList]
        ]
        if (includeAttenuation) {
            path.putAll([
                    aDiv            : (0..<bandCount).collect { 60d + it },
                    aAtm            : (0..<bandCount).collect { 0.5d + it },
                    aDif            : (0..<bandCount).collect { (diffracted || rayleigh) ? 3d + it : 0d + it },
                    aRef            : (0..<bandCount).collect { reflected ? 2d + it : 0d + it },
                    aGlobal         : (0..<bandCount).collect { 63d + it },
                    groundAttenuation: [aGround: [0.2d]]
            ])
        }
        return JsonOutput.toJson(path)
    }

    private static String realWriterPathJson(String kind) {
        String pointType = kind == 'rayleigh' ? 'DIFH_RCRIT' : 'DIFH'
        Map<String, Object> path = [
                cutProfile     : [cutPoints: [
                        [type: 'Source', coordinate: [x: 0d, y: 0d, z: 1d], zGround: 0d, groundCoefficient: 0.5d],
                        [type: 'Topography', coordinate: [x: 10d, y: 0d, z: 2d], zGround: 2d, groundCoefficient: 0.5d],
                        [type: 'Receiver', coordinate: [x: 20d, y: 0d, z: 1d], zGround: 0d, groundCoefficient: 0.5d]
                ], curvedPath: false, profileType: 'DIRECT'],
                lineString     : [type: 'LineString', coordinates: [[0d, 0d, 1d], [10d, 0d, 2d], [20d, 0d, 1d]]],
                propagationPath: [pointList: [
                        [type: 'SRCE', coordinate: [x: 0d, y: 1d, z: 'NaN'], altitude: 1d],
                        [type: pointType, coordinate: [x: 10d, y: 2d, z: 'NaN'], altitude: 2d],
                        [type: 'RECV', coordinate: [x: 20d, y: 1d, z: 'NaN'], altitude: 1d]
                ]],
                aDiv           : [60d],
                aAtm           : [0.5d],
                aDif           : [4d],
                aRef           : [0d],
                aGlobal        : [64d],
                groundAttenuation: [aGround: [0.2d]]
        ]
        return JsonOutput.toJson(path)
    }

    private String run(Map<String, Object> parameters) {
        Map<String, Object> inputs = new LinkedHashMap<>(parameters)
        inputs.put('tableRays', 'RAYS')
        return new Explain_Rays().exec(connection, inputs).result as String
    }

    private static int countOccurrences(String text, String token) {
        int count = 0
        int position = 0
        while ((position = text.indexOf(token, position)) >= 0) {
            count++
            position += token.length()
        }
        return count
    }

    @Test
    void testTop50PlusDirect() {
        createRaysTable()
        for (int i = 1; i <= 50; i++) {
            insertRay(1, i, 86d, METEO, PERIOD, pathJson(i, 1, 'reflected'))
        }
        insertRay(1, 999, 84d, METEO, PERIOD, pathJson(999, 1, 'direct'))

        String html = run([:])

        assertTrue(html.contains('data-ray-count="51"'), html)
        assertTrue(html.contains('data-direct-found="true"'))
        assertTrue(html.contains('data-direct-added="true"'))
        assertTrue(html.contains('data-direct-source="999"'))
        assertEquals(51, countOccurrences(html, 'data-ray-row="'))
        assertTrue(html.contains('was outside the top 50'))
        assertTrue(html.contains('source 999'))
    }

    @Test
    void testDirectAlreadyInTopDoesNotDuplicate() {
        createRaysTable()
        for (int i = 1; i <= 59; i++) {
            insertRay(1, i, 80d + i * 0.1d, METEO, PERIOD, pathJson(i, 1, 'reflected'))
        }
        insertRay(1, 999, 200d, METEO, PERIOD, pathJson(999, 1, 'direct'))

        String html = run([:])

        assertTrue(html.contains('data-ray-count="50"'), html)
        assertTrue(html.contains('data-direct-found="true"'))
        assertTrue(html.contains('data-direct-added="false"'))
        assertEquals(50, countOccurrences(html, 'data-ray-row="'))
        assertEquals(1, countOccurrences(html, '&middot; source 999'))
    }

    @Test
    void testMaxRaysTen() {
        createRaysTable()
        for (int i = 1; i <= 10; i++) {
            insertRay(1, i, 86d, METEO, PERIOD, pathJson(i, 1, 'reflected'))
        }
        insertRay(1, 999, 84d, METEO, PERIOD, pathJson(999, 1, 'direct'))

        String html = run([maxRays: 10])

        assertTrue(html.contains('data-ray-count="11"'), html)
        assertEquals(11, countOccurrences(html, 'data-ray-row="'))
        assertTrue(html.contains('data-direct-added="true"'))
    }

    @Test
    void testExcludedVocabulary() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))
        insertRay(1, 2, 60d, METEO, PERIOD, pathJson(2, 1, 'reflected'))

        String html = run([:]).toLowerCase()

        assertFalse(html.contains('measured'))
        assertFalse(html.contains('delta'))
        assertFalse(html.contains('calibrated'))
        assertFalse(html.contains('offset'))
    }

    @Test
    void testFilters() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))
        insertRay(1, 2, 60d, 'favorable', 'N', pathJson(2, 1, 'direct'))
        insertRay(2, 3, 90d, 'favorable', 'N', pathJson(3, 2, 'direct'))
        insertRay(2, 4, 50d, METEO, PERIOD, pathJson(4, 2, 'direct'))

        String html = run([receiver: 2, meteo: 'favorable', period: 'N'])

        assertTrue(html.contains('data-receiver="2"'))
        assertTrue(html.contains('data-meteo="favorable"'))
        assertTrue(html.contains('data-period="N"'))
        assertTrue(html.contains('data-total-rays="1"'))
        assertTrue(html.contains('source 3'))
        assertFalse(html.contains('source 4'))
    }

    @Test
    void testWorksWithoutOptionalTables() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))
        insertRay(1, 2, 60d, METEO, PERIOD, pathJson(2, 1, 'reflected'))

        String html = run([:])

        assertTrue(html.contains('id="explain-rays-app"'))
        assertTrue(html.contains('data-marker="source"'))
        assertTrue(html.contains('data-marker="receiver"'))
        assertFalse(html.contains('<div class="explain-rays-error">'))
    }

    @Test
    void testInvalidPathsAreIgnored() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))
        insertRay(1, 2, 60d, METEO, PERIOD, pathJson(2, 1, 'reflected'))
        insertRay(1, 3, 50d, METEO, PERIOD, null)
        insertRay(1, 4, 40d, METEO, PERIOD, '{not valid json')

        String html = run([:])

        assertTrue(html.contains('data-ray-count="2"'))
        assertTrue(html.contains('data-invalid-paths="2"'))
        assertTrue(html.contains('2 row(s) with an invalid PATH were ignored'))
        assertFalse(html.contains('<div class="explain-rays-error">'))
    }

    @Test
    void testAllInvalidPathsReturnReadableError() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, null)

        String html = run([:])

        assertTrue(html.contains('<div class="explain-rays-error">'))
        assertTrue(html.contains('No valid propagation ray'))
    }

    @Test
    void testNullLeqIsLastAndDoesNotFail() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))
        insertRay(1, 2, 60d, METEO, PERIOD, pathJson(2, 1, 'reflected'))
        insertRay(1, 3, null, METEO, PERIOD, pathJson(3, 1, 'reflected'))

        String html = run([:])

        assertTrue(html.contains('data-ray-count="3"'))
        assertTrue(html.contains('n/a'))
        assertFalse(html.contains('<div class="explain-rays-error">'))
    }

    @Test
    void testHtmlEscaping() {
        createRaysTable()
        insertRay(1, 1, 70d, 'x<y&z', PERIOD, pathJson(1, 1, 'direct'))

        String html = run([meteo: 'x<y&z'])

        assertTrue(html.contains('x&lt;y&amp;z'))
        assertFalse(html.contains('x<y&z'))
    }

    @Test
    void testExpectedMarkersAndIds() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))
        insertRay(1, 2, 60d, METEO, PERIOD, pathJson(2, 1, 'reflected'))

        String html = run([:])

        assertTrue(html.contains('id="explain-rays-app"'))
        assertTrue(html.contains('id="explain-rays-data"'))
        assertTrue(html.contains('class="explain-rays-table"'))
        assertTrue(html.contains('data-explain-rays-selected'))
        assertTrue(html.contains('class="explain-rays-profile"'))
        assertTrue(html.contains('data-ray-index="0"'))
    }

    @Test
    void testOptionalTablesEnrichTheScene() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))

        Sql sql = new Sql(connection)
        sql.execute('CREATE TABLE BUILDINGS (ID INTEGER, THE_GEOM GEOMETRY(MULTIPOLYGON, 0))')
        sql.execute("INSERT INTO BUILDINGS VALUES (1, ST_GeomFromText('MULTIPOLYGON(((5 -5, 25 -5, 25 5, 5 5, 5 -5)))'))")
        sql.execute('CREATE TABLE RECEIVERS (IDRECEIVER INTEGER, THE_GEOM GEOMETRY(POINT, 0))')
        sql.execute("INSERT INTO RECEIVERS VALUES (1, ST_GeomFromText('POINT(20 0)'))")
        sql.execute('CREATE TABLE SOURCES (IDSOURCE INTEGER, THE_GEOM GEOMETRY(POINT, 0))')
        sql.execute("INSERT INTO SOURCES VALUES (1, ST_GeomFromText('POINT(0 0)'))")
        sql.execute('CREATE TABLE GROUNDS (ID INTEGER, G DOUBLE, THE_GEOM GEOMETRY(POLYGON, 0))')
        sql.execute("INSERT INTO GROUNDS VALUES (1, 0.6, ST_GeomFromText('POLYGON((0 -5, 20 -5, 20 5, 0 5, 0 -5))'))")
        sql.execute('CREATE TABLE DEM (ID INTEGER, THE_GEOM GEOMETRY(POINT, 0))')
        sql.execute("INSERT INTO DEM VALUES (1, ST_GeomFromText('POINT(10 0)'))")

        String html = run([tableBuilding: 'BUILDINGS', tableReceivers: 'RECEIVERS',
                           tableSources : 'SOURCES', tableGrounds: 'GROUNDS', tableDEM: 'DEM'])

        assertTrue(html.contains('<polygon'), 'The building footprint should be drawn')
        assertTrue(html.contains('data-marker="receiver"'))
        assertTrue(html.contains('data-marker="source"'))
        int groundIndex = html.indexOf('data-layer="ground"')
        int buildingIndex = html.indexOf('data-layer="building"')
        assertTrue(groundIndex > 0, 'The ground polygon should be drawn')
        assertTrue(buildingIndex > groundIndex, 'Buildings must be drawn above ground')
        assertTrue(html.contains('fill-opacity="0.5"'), 'Ground must be 50% opaque')
        assertTrue(html.contains('fill-opacity="1"'), 'Buildings must be fully opaque')
        assertFalse(html.contains('<div class="explain-rays-error">'))
    }

    @Test
    void testAttenuationBandSelector() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct', true, 3))

        String html = run([:])

        assertTrue(html.contains('data-explain-rays-band'))
        assertTrue(html.contains('<option value="2">'))
        assertTrue(html.contains('data-band-2="65.0"'))
        assertTrue(html.contains('LEQ is broadband'))
    }

    @Test
    void testRayleighCriterionMarker() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'rayleigh'))

        String html = run([:])

        assertTrue(html.contains('>RC<'), 'Rayleigh criterion node should be labelled RC')
        assertTrue(html.contains('RC Rayleigh criterion'))
        assertFalse(html.contains('<div class="explain-rays-error">'))
    }

    @Test
    void testBasemapWithProjectedCoordinates() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, pathJson(1, 1, 'direct'))

        String html = run([inputSRID: 2154])

        assertTrue(html.contains('data-basemap="true"'))
        assertTrue(html.contains('data-explain-rays-basemap-image="true"'))
        assertTrue(html.contains('ows.terrestris.de/osm/service'))
        assertTrue(html.contains('SRS=EPSG:4326'))
        assertTrue(html.contains('data-explain-rays-basemap checked'))
    }

    @Test
    void testNegligibleDirectIsNotAdded() {
        createRaysTable()
        for (int i = 1; i <= 59; i++) {
            insertRay(1, i, 86d, METEO, PERIOD, pathJson(i, 1, 'reflected'))
        }
        insertRay(1, 999, 10d, METEO, PERIOD, pathJson(999, 1, 'direct'))

        String html = run([:])

        assertTrue(html.contains('data-ray-count="50"'), html)
        assertTrue(html.contains('data-direct-found="true"'))
        assertTrue(html.contains('data-direct-added="false"'))
        assertTrue(html.contains('data-direct-skipped="true"'))
        assertEquals(50, countOccurrences(html, 'data-ray-row="'))
        assertTrue(html.contains('does not contribute to the receiver level'))
    }

    @Test
    void testRealWriterPointListCoordinates() {
        createRaysTable()
        insertRay(1, 1, 70d, METEO, PERIOD, realWriterPathJson('difh'))
        insertRay(1, 2, 69d, METEO, PERIOD, realWriterPathJson('rayleigh'))

        String html = run([:])

        assertTrue(html.contains('>DH<'), 'DIFH marker should be mapped to world coordinates')
        assertTrue(html.contains('>RC<'), 'DIFH_RCRIT marker should be mapped to world coordinates')
        assertFalse(html.contains('<div class="explain-rays-error">'))
    }

    @Test
    void testScriptParses() {
        File script = new File('src/main/groovy/org/noise_planet/noisemodelling/scripts/Explain/Explain_Rays.groovy')
        assertTrue(script.isFile(), "Missing script: ${script.absolutePath}")
        new GroovyClassLoader().parseClass(script)
    }
}
