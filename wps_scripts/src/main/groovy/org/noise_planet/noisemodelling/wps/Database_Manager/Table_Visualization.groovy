/**
* @Author Nicolas Fortin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore

import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities

import org.locationtech.jts.io.WKTWriter

import java.sql.Connection
import java.sql.Statement

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.h2gis.api.ProgressVisitor;
import org.h2gis.functions.io.utility.FileUtil;
import org.h2gis.utilities.JDBCUtilities;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.locationtech.jts.geom.Geometry;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import groovy.sql.Sql

import org.h2gis.functions.io.csv.*
import org.h2gis.functions.io.dbf.*
import org.h2gis.functions.io.geojson.*
import org.h2gis.functions.io.json.*
import org.h2gis.functions.io.kml.*
import org.h2gis.functions.io.shp.*
import org.h2gis.functions.io.tsv.*
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.utilities.wrapper.ConnectionWrapper

import org.noisemodellingwps.utilities.WpsConnectionWrapper

title = 'Visualize a Table'
description = 'Visualize a Table'

inputs = [
   databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : first found db)', min : 0, max : 1, type: String.class],
   tableName: [name: 'Table Name', title: 'Table Name', description: 'Table Name', type: String.class]
]

outputs = [
    result: [name: 'Result', title: 'Result', type: Geometry.class]
]

def static Connection openPostgreSQLDataStoreConnection(String dbName) {
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    // Get name of the database
    String dbName = "h2gisdb"
    if (input['databaseName']){dbName = input['databaseName'] as String}

    // Open connection
    openPostgreSQLDataStoreConnection(dbName).withCloseable { Connection connection ->
        Statement sql = connection.createStatement();


        // Execute
        String tableName = input["tableName"] as String
        tableName = tableName.toUpperCase()

        // Read Geometry Index and type
        List<String> spatialFieldNames = SFSUtilities.getGeometryFields(connection, TableLocation.parse(tableName, JDBCUtilities.isH2DataBase(connection.getMetaData())))
        if (spatialFieldNames.isEmpty()) {
            System.out.println("The table %s does not contain a geometry field")
        }


        ResultSet rs = sql.executeQuery(String.format("select ST_ACCUM(ST_TRANSFORM(ST_SetSRID(the_geom,2154),4326)) the_geom from %s", tableName))
        //ResultSet rs = sql.executeQuery(String.format("select ST_ACCUM(the_geom) the_geom from %s", tableName))

        Geometry geom = null
        while (rs.next()) {
            ResultSetMetaData resultSetMetaData = rs.getMetaData()
            def geoFieldIndex = JDBCUtilities.getFieldIndex(resultSetMetaData, spatialFieldNames.get(0))
            geom = (Geometry) rs.getObject(geoFieldIndex)
        }

        System.out.println("-------------------------------------")
        //System.out.println(geom.hasM())

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