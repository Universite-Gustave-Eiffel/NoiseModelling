/**
* @Author Nicolas Fortin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTWriter

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

title = 'Visualize a Table'
description = 'Groups all the geometries of a table and returns them in WKT OGC format. Be careful, this treatment can be blocking if the table is large.'

inputs = [
   databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : first found db)', min : 0, max : 1, type: String.class],
   tableName: [name: 'Table Name', title: 'Table Name', description: 'Table Name', type: String.class]
]

outputs = [
    result: [name: 'Result', title: 'Result', type: Geometry.class]
]


static Connection openGeoserverDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    // Get name of the database
    String dbName = ""
    if (input['databaseName']){dbName = input['databaseName'] as String}

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->
        Statement sql = connection.createStatement();


        // Execute
        String tableName = input["tableName"] as String
        tableName = tableName.toUpperCase()

        // Read Geometry Index and type
        List<String> spatialFieldNames = SFSUtilities.getGeometryFields(connection, TableLocation.parse(tableName, JDBCUtilities.isH2DataBase(connection.getMetaData())))
        if (spatialFieldNames.isEmpty()) {
            System.err.println("The table %s does not contain a geometry field")
            return new GeometryFactory().createPolygon()
        }


        ResultSet rs = sql.executeQuery(String.format("select ST_ACCUM(ST_TRANSFORM(ST_SetSRID("+spatialFieldNames.get(0)+",2154),4326)) the_geom from %s", tableName))

        Geometry geom = null
        while (rs.next()) {
            geom = (Geometry) rs.getObject(0)
        }


        // print to Console windows
        return [result: geom]
    }
}

/**
 * Convert a Geometry value into a Well Known Text value.
 * @param geometry Geometry instance
 * @return The String representation
 */
static String asWKT(Geometry geometry) {
    if(geometry==null) {
        return null
    }
    WKTWriter wktWriter = new WKTWriter()
    return wktWriter.write(geometry)
}