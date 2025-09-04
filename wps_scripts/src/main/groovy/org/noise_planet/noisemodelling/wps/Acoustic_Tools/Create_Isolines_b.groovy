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

/**
 * @Author Adapted from NM WPS scripts
 * @Author Ignacio Soto Molina, Ministry for Ecological Transition (MITECO), Spain
 */

package org.noise_planet.noisemodelling.wps.Acoustic_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException

// ----------------
// Create Isolines (Isophones) from TRIANGLES + RECEIVERS_LEVEL
// ----------------

title = 'Create Isolines (Isophones)'

description = 'Generate isolines (isophones) by linear interpolation on triangle edges (marching-triangles).' +
        '</br> One multilines map per <b>PERIOD</b> and per <b>LEVEL</b> is created.' +
        '</br> </br> <b> Output table : ISOLINES_NOISE_MAP </b> ' +
        'with : </br>' +
        '-  <b> PERIOD </b> : receivers period label (VARCHAR). </br>' +
        '-  <b> LEVEL </b> : isoline value (DOUBLE). </br>' +
        '-  <b> THE_GEOM </b> : MULTILINESTRING/LINESTRING geometry.'

inputs = [
        trianglesTable : [
                name       : 'Triangles table',
                title      : 'Triangles table',
                description: '<b>Name of the triangles table.</b></br>' +
                             'Shall contain : PK, THE_GEOM, PK_1, PK_2, PK_3, CELL_ID.',
                min        : 0, max: 1,
                type       : String.class
        ],
        receiversTable : [
                name       : 'Receivers level table',
                title      : 'Receivers level table',
                description: '<b>Name of the receivers level table.</b></br>' +
                             'Shall contain : IDRECEIVER, PERIOD, THE_GEOM, LAEQ (or any field to contour).',
                min        : 0, max: 1,
                type       : String.class
        ],
        fieldName      : [
                name       : 'Field to contour',
                title      : 'Field to contour',
                description: 'Receivers numeric field to contour (e.g. LAEQ). Default: LAEQ.',
                min        : 0, max: 1,
                type       : String.class
        ],
        isoClasses     : [
                name       : 'Iso levels (dB)',
                title      : 'Iso levels (dB)',
                description: 'Comma-separated levels. Default: 35.0,40.0,45.0,50.0,55.0,60.0,65.0,70.0,75.0,80.0,200.0',
                min        : 0, max: 1,
                type       : String.class
        ]
]

outputs = [
        result: [
                name: 'Result output string',
                title: 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type: String.class
        ]
]

// -------------------
// Open Connection to Geoserver (same pattern as Template)
// -------------------
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// -------------------
// run() wrapper (same pattern as Template)
// -------------------
def run(input) {
    String dbName = "h2gisdb"
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        return [result: exec(connection, input)]
    }
}

// -------------------
// Main function (same structure as Template.exec)
// -------------------
def exec(Connection connection, input) {

    // Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Logger
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Create Isolines')
    logger.info("inputs {}", input)

    // -------------------
    // Get inputs
    // -------------------
    String trianglesTable = (input['trianglesTable'] ?: 'TRIANGLES') as String
    String receiversTable = (input['receiversTable'] ?: 'RECEIVERS_LEVEL') as String
    String fieldName      = (input['fieldName']      ?: 'LAEQ') as String
    String isoClassesStr  = (input['isoClasses']     ?: '35.0,40.0,45.0,50.0,55.0,60.0,65.0,70.0,75.0,80.0,200.0') as String

    // do it case-insensitive for table names
    trianglesTable = trianglesTable.toUpperCase()
    receiversTable = receiversTable.toUpperCase()

    // Parse levels (sorted)
    List<Double> levels = isoClassesStr.split(',')
            .collect { it.trim() }
            .findAll { it }
            .collect { it as Double }
            .sort()

    // -------------------
    // Checks
    // -------------------
    def triExists = org.h2gis.utilities.JDBCUtilities.tableExists(connection, trianglesTable)
    def recExists = org.h2gis.utilities.JDBCUtilities.tableExists(connection, receiversTable)
    if (!triExists) throw new SQLException(String.format("The table %s does not exist.", trianglesTable))
    if (!recExists) throw new SQLException(String.format("The table %s does not exist.", receiversTable))

    // SRID (receivers first, then triangles)
    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(receiversTable))
    if (srid == 0) srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(trianglesTable))
    if (srid == 0) throw new SQLException("Unable to determine SRID of geometries (receivers/triangles).")

    // Validate field existence in receivers (case-insensitive)
    def cntRow = sql.firstRow(
            "SELECT COUNT(*) AS CNT FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?",
            [receiversTable.toUpperCase(), fieldName.toUpperCase()]
    )
    if ((cntRow?.CNT ?: 0) == 0) {
        throw new SQLException(String.format("Field '%s' does not exist in %s", fieldName, receiversTable))
    }

    // -------------------
    // Create output & temporaries (NO placeholders in DDL)
    // -------------------
    String outTable  = "ISOLINES_NOISE_MAP"
    String segTable  = "TMP_ISO_SEGMENTS"
    String resultString

    sql.execute("DROP TABLE IF EXISTS \"" + outTable + "\"")
    sql.execute("""CREATE TABLE "${outTable}"(
                    "PERIOD" VARCHAR,
                    "LEVEL" DOUBLE,
                    "THE_GEOM" GEOMETRY
                  )""")
    sql.execute("CREATE SPATIAL INDEX ON \"" + outTable + "\"(\"THE_GEOM\")")

    sql.execute("DROP TABLE IF EXISTS \"" + segTable + "\"")
    sql.execute("""CREATE TABLE "${segTable}"(
                    "PERIOD" VARCHAR,
                    "LEVEL" DOUBLE,
                    "THE_GEOM" GEOMETRY
                  )""")
    sql.execute("CREATE SPATIAL INDEX ON \"" + segTable + "\"(\"THE_GEOM\")")

    // -------------------
    // Periods list
    // -------------------
    List<String> periods = []
    sql.eachRow("SELECT DISTINCT \"PERIOD\" FROM \"" + receiversTable + "\" WHERE \"PERIOD\" IS NOT NULL") { r ->
        periods.add(r.PERIOD?.toString())
    }
    if (periods.isEmpty()) periods = [null]

    // -------------------
    // Geometry machinery
    // -------------------
    GeometryFactory gf = new GeometryFactory(new PrecisionModel(), srid)
    double eps = 1e-9

    // Interpolate point on edge
    def interpPoint = { Coordinate a, Coordinate b, double va, double vb, double cLevel ->
        double dv = (vb - va)
        if (Math.abs(dv) < eps) return null
        double t = (cLevel - va) / dv
        if (t < -eps || t > 1 + eps) return null
        if (t < 0) t = 0
        if (t > 1) t = 1
        new Coordinate(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
    }

    // Load triangles to memory
    List<Map> triRows = []
    sql.eachRow("SELECT \"PK\", \"PK_1\", \"PK_2\", \"PK_3\" FROM \"" + trianglesTable + "\"") { r ->
        triRows.add([PK: r.PK as Integer, A: r.PK_1 as Integer, B: r.PK_2 as Integer, C: r.PK_3 as Integer])
    }

    // Insert batch for segments
    String insSegSQL = "INSERT INTO " + segTable + "(PERIOD, LEVEL, THE_GEOM) VALUES (?, ?, ?)"

    // -------------------
    // Build segments by PERIOD & LEVEL
    // -------------------
    periods.each { periodVal ->
        // Map: receiver id -> (coord, val)
        Map<Integer, Map> recMap = [:]
        String sqlRec = (periodVal == null) ?
                "SELECT IDRECEIVER, THE_GEOM, " + fieldName + " AS VAL FROM " + receiversTable :
                "SELECT IDRECEIVER, THE_GEOM, " + fieldName + " AS VAL FROM " + receiversTable + " WHERE PERIOD = ?"

        if (periodVal == null) {
            sql.eachRow(sqlRec) { r ->
                Point pt = r.THE_GEOM as Point
                if (pt != null) recMap[r.IDRECEIVER as Integer] = [coord: pt.coordinate, val: (r.VAL as Double)]
            }
        } else {
            sql.eachRow(sqlRec, [periodVal]) { r ->
                Point pt = r.THE_GEOM as Point
                if (pt != null) recMap[r.IDRECEIVER as Integer] = [coord: pt.coordinate, val: (r.VAL as Double)]
            }
        }

        if (recMap.isEmpty()) return

        sql.withBatch(5000, insSegSQL) { ps ->
            triRows.each { trow ->
                def rA = recMap[trow.A]; def rB = recMap[trow.B]; def rC = recMap[trow.C]
                if (rA == null || rB == null || rC == null) return

                Coordinate A = rA.coord as Coordinate
                Coordinate B = rB.coord as Coordinate
                Coordinate C = rC.coord as Coordinate
                double vA = (rA.val as Double)
                double vB = (rB.val as Double)
                double vC = (rC.val as Double)

                levels.each { L ->
                    List<Coordinate> hits = []

                    // AB
                    if ((L > Math.min(vA, vB) - eps) && (L < Math.max(vA, vB) + eps) && Math.abs(vA - vB) > eps) {
                        def p = interpPoint(A, B, vA, vB, L); if (p != null) hits << p
                    } else if (Math.abs(vA - L) <= eps && Math.abs(vA - vB) > eps) {
                        hits << A
                    } else if (Math.abs(vB - L) <= eps && Math.abs(vA - vB) > eps) {
                        hits << B
                    }
                    // BC
                    if ((L > Math.min(vB, vC) - eps) && (L < Math.max(vB, vC) + eps) && Math.abs(vB - vC) > eps) {
                        def p = interpPoint(B, C, vB, vC, L); if (p != null) hits << p
                    } else if (Math.abs(vB - L) <= eps && Math.abs(vB - vC) > eps) {
                        hits << B
                    } else if (Math.abs(vC - L) <= eps && Math.abs(vB - vC) > eps) {
                        hits << C
                    }
                    // CA
                    if ((L > Math.min(vC, vA) - eps) && (L < Math.max(vC, vA) + eps) && Math.abs(vC - vA) > eps) {
                        def p = interpPoint(C, A, vC, vA, L); if (p != null) hits << p
                    } else if (Math.abs(vC - L) <= eps && Math.abs(vC - vA) > eps) {
                        hits << C
                    } else if (Math.abs(vA - L) <= eps && Math.abs(vC - vA) > eps) {
                        hits << A
                    }

                    // de-dup small numeric jitter
                    def uniq = []
                    hits.each { h ->
                        if (h == null) return
                        def kx = Math.rint(h.x * 1e6) / 1e6
                        def ky = Math.rint(h.y * 1e6) / 1e6
                        if (!uniq.any { Math.abs(it.x - kx) < 1e-9 && Math.abs(it.y - ky) < 1e-9 }) {
                            uniq << new Coordinate(kx, ky)
                        }
                    }

                    if (uniq.size() == 2) {
                        LineString ls = gf.createLineString([uniq[0], uniq[1]] as Coordinate[])
                        ps.addBatch(periodVal, (L as Double), ls)
                    }
                } // levels
            } // triangles
        } // batch
    } // periods

    // -------------------
    // Stitch segments per (PERIOD, LEVEL) and insert to output
    // (without using GROUP BY directly to avoid dialect quirks)
    // -------------------
    List<Map> groups = []
    sql.eachRow("SELECT DISTINCT \"PERIOD\", \"LEVEL\" FROM \"" + segTable + "\"") { g ->
        groups.add([p: g.PERIOD, l: (g.LEVEL as Double)])
    }

    String insMerged =
            "INSERT INTO \"" + outTable + "\"(\"PERIOD\", \"LEVEL\", \"THE_GEOM\") " +
            "SELECT ?, ?, ST_LineMerge(ST_Union(\"THE_GEOM\")) " +
            "FROM \"" + segTable + "\" " +
            "WHERE ((\"PERIOD\" IS NULL AND ? IS NULL) OR \"PERIOD\" = ?) " +
            "AND \"LEVEL\" = ?"

    groups.each { g ->
        sql.execute(insMerged, [g.p, g.l, g.p, g.p, g.l])
    }

    // -------------------
    // Cleanup
    // -------------------
    sql.execute("DROP TABLE IF EXISTS \"" + segTable + "\"")

    // -------------------
    // Print results
    // -------------------
    resultString = "Isolines created in " + outTable + " for " + levels.size() + " levels."
    logger.info('Result : ' + resultString)
    logger.info('End : Create Isolines')

    // send resultString to WPS Builder
    return resultString
}
