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
 *
 * @Author Nicolas Fortin, Université Gustave Eiffel
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Database_Manager

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
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

title = 'Diplay a table on a map.'
description = 'Display a table containing a geometric field on a map. </br> Technically, it groups all the geometries of a table and returns them in WKT OGC format. </br> Be careful, this treatment can be blocking if the table is large.'

inputs = [
        inputSRID: [name: 'Projection identifier', title: 'Projection identifier', description: 'Original projection identifier (also called SRID) of your table. It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). </br>  All coordinates will be projected from the specified EPSG to WGS84 coordinates. </br> This entry is optional because many formats already include the projection and you can also import files without geometry attributes.</br>  <b> Default value : 4326 </b> ', type: Integer.class, min: 0, max: 1],
        tableName: [name: 'Name of the table', title: 'Name of the table', description: 'Name of the table you want to display.', type: String.class]
]

outputs = [result: [name: 'Result output geometry', title: 'Result output geometry', description: 'This is the output geometry in WKT OGC format', type: Geometry.class]]


static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(Connection connection, input) {

    // output geometry, the information given back to the user
    Geometry geom = null

    // print to command window
    System.out.println('Start : Display a table on a map')
    def start = new Date()

    // Get name of the table
    String tableName = input["tableName"] as String
    // do it case-insensitive
    tableName = tableName.toUpperCase()

    // Default SRID (WGS84)
    Integer srid = 4326
    // Get user SRID
    if (input['inputSRID']) {
        srid = input['inputSRID'] as Integer
    }

    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()

    // Read Geometry Index and type of the table
    List<String> spatialFieldNames = SFSUtilities.getGeometryFields(connection, TableLocation.parse(tableName, JDBCUtilities.isH2DataBase(connection.getMetaData())))

    // If the table does not contain a geometry field
    if (spatialFieldNames.isEmpty()) {
        System.err.println("The table does not contain a geometry field")
        geom = new GeometryFactory().createGeometryCollection()
        return geom
    }

    // Get the SRID of the table
    Integer tableSrid = SFSUtilities.getSRID(connection, TableLocation.parse(tableName))

    if (tableSrid != 0 && tableSrid != srid && input['inputSRID']) throw new Exception("The table already has a different SRID than the one you gave.")

    // Replace default SRID by the srid of the table
    if (tableSrid != 0) srid = tableSrid

    // Display the actual SRID in the command window
    System.out.println("The actual SRID of the table is " + srid)

    if (tableSrid == 0) {
        connection.createStatement().execute(String.format("UPDATE %s SET " + spatialFieldNames.get(0) + " = ST_SetSRID(" + spatialFieldNames.get(0) + ",%d)",
                TableLocation.parse(tableName).toString(), srid))
    }

    // Project geometry in WGS84 (EPSG:4326) and groups all the geometries of the table
    String geomField = "ST_ACCUM(ST_TRANSFORM(" + spatialFieldNames.get(0) + " ,4326))"
    ResultSet rs = stmt.executeQuery(String.format("select %s " + spatialFieldNames.get(0) + " from %s", geomField, tableName))

    // Get the geometry field from the table
    while (rs.next()) {
        geom = (Geometry) rs.getObject(1)
    }

    // print to command window
    if (asWKT(geom).size() > 100) {
        System.out.println('Result (100 first characters) : ' + asWKT(geom).substring(0,100) + '...')
    } else {
        System.out.println('Result : ' + asWKT(geom))
    }
    System.out.println('End : Display a table on a map')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return geom
}


def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

/**
 * Convert a Geometry value into a Well Known Text value.
 * @param geometry Geometry instance
 * @return The String representation
 */
static String asWKT(Geometry geometry) {
    if (geometry == null) {
        return null
    }
    WKTWriter wktWriter = new WKTWriter()
    return wktWriter.write(geometry)
}