/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.Tuple
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.LineString
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException

title = 'Map Difference'
description = '&#10145;&#65039; Computes the difference between two noise maps'

inputs = [
        railsGeometries : [
                name: 'Table that contains geometries of rails',
                title: 'Table that contains geometries of rails',
                description: '',
                type: String.class
        ],
        trainTraffic: [
                name: 'Table that contains train traffics',
                title: 'Table that contains train traffics',
                description: '',
                type: String.class
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

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


def run(Map input) {

    // Get name of the database
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// main function of the script
@CompileStatic
def exec(Connection connection, Map input) {

    Logger logger = LoggerFactory.getLogger("DynamicTrainFromAADTTraffic")

    connection = new ConnectionWrapper(connection)

    final def railsGeometries = input["railsGeometries"] as String
    final def railsTraffic = input["railsTraffic"] as String

    Sql sql = new Sql(connection)

    final int srid = GeometryTableUtilities.getSRID(connection, railsGeometries) as Integer

    // check that the srid is a metric unit coordinate reference system

    def isMetric = sql.firstRow("SELECT COUNT(*) FROM SPATIAL_REF_SYS WHERE SRID=$srid" +
            " AND PROJ4TEXT LIKE '%units=m%'")[0] as Boolean

    if(!isMetric) {
        throw new SQLException("Geometry projection system of the table $railsGeometries must be metric ! (not EPSG=$srid)")
    }

    def snapDistance = 0.1

    // Generate graph of rails segments
    sql.execute($/
    DROP TABLE IF EXISTS SEGMENTS;
    CREATE TABLE SEGMENTS(the_geom GEOMETRY(LINESTRINGZ, $srid), PK_RAILS VARCHAR)
    AS SELECT the_geom, pk_rails FROM ST_EXPLODE('$railsGeometries');
    ALTER TABLE SEGMENTS ADD COLUMN EDGE_ID SERIAL PRIMARY KEY;
    SELECT ST_GRAPH('SEGMENTS', 'THE_GEOM', $snapDistance, 0, 1);
    /$.toString())

    // Load connectivity into memory for fast access

    Map<String, SegmentAttributes> segments = new HashMap<>()
    Map<Tuple<Integer, Integer>, String> edgeToIndex = new HashMap<>()
    Map<Integer, HashSet<Integer>> nodeConnectivity = new HashMap<>()

    sql.eachRow("SELECT pk_rails, SEGMENTS.the_geom, start_node, end_node FROM SEGMENTS_EDGES INNER JOIN SEGMENTS USING (EDGE_ID)") {
        final def segment = new SegmentAttributes(it.getString("pk_rails"),
                ((LineString) it.getObject("the_geom")),
                it.getInt("start_node"),
                it.getInt("end_node"))
        if(segment.startNode == segment.endNode) {
            logger.warn("Segment $segment.startNode (length: ${segment.lineString.length} m) looping with itself $segment.lineString")
        } else {
            segments.put(segment.pk, segment)
            nodeConnectivity.merge(segment.startNode, new HashSet<Integer>(Arrays.asList(segment.endNode))) { HashSet<Integer> left, HashSet<Integer> right ->
                left.addAll(right)
                left
            }
            nodeConnectivity.merge(segment.endNode, new HashSet<Integer>(Arrays.asList(segment.startNode))) { HashSet<Integer> left, HashSet<Integer> right ->
                left.addAll(right)
                left
            }
            edgeToIndex.putIfAbsent(new Tuple(segment.startNode, segment.endNode), segment.pk)
            edgeToIndex.putIfAbsent(new Tuple(segment.endNode, segment.startNode), segment.pk)
        }
    }

    // Loop over unique characteristic of train (currently limited to train set attribute)
    sql.eachRow("SELECT DISTINCT train_set FROM $railsTraffic".toString()) {
        def trainSet = it.getString("train_set")

        // init expected traffic to 0 for all segments
        segments.each {entry ->
            def segment = entry.getValue()
            segment.remainingDay = 0
            segment.remainingEvening = 0
            segment.remainingNight = 0
        }

        // Fetch expected traffic for this train set
        sql.eachRow("SELECT PK_RAILS, TDAY, TEVENING, TNIGHT, MAXSPEED FROM $railsTraffic WHERE TRAIN_SET = :train_set".toString(), [train_set:trainSet]) {
            def pkRails = it.getString("PK_RAILS")
            if(segments.containsKey(pkRails)) {
                def segment = segments.get(pkRails)
                segment.remainingDay = Math.ceil(it.getDouble("TDAY"))
                segment.remainingEvening = Math.ceil(it.getDouble("TEVENING"))
                segment.remainingNight = Math.ceil(it.getDouble("TNIGHT"))
            }
        }

        // Look for nodes with only one connectivity (end/begin of networks)
        nodeConnectivity.each {entry ->
            if(entry.value.size() == 1) {
                int currentNode = entry.key
                int nextNode = entry.value.first()
            }
        }
    }



    return ""
}

@CompileStatic
class SegmentAttributes {
    String pk
    LineString lineString
    int startNode
    int endNode
    /** remaining traffic count for day */
    double remainingDay = 0
    /** remaining traffic count for evening */
    double remainingEvening = 0
    /** remaining traffic count for night */
    double remainingNight = 0

    SegmentAttributes(String pk, LineString lineString, int startNode, int endNode) {
        this.pk = pk
        this.lineString = lineString
        this.startNode = startNode
        this.endNode = endNode
    }
}
