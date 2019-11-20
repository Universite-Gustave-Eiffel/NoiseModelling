/**
 * @Author Aumond Pierre
 */

package org.noise_planet.noisemodelling.wps.Import_and_Export

import geoserver.GeoServer
import geoserver.catalog.Store
import org.apache.commons.io.FilenameUtils
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.DBFDriverFunction
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.functions.io.gpx.GPXDriverFunction
import org.h2gis.functions.io.osm.OSMDriverFunction
import org.h2gis.functions.io.shp.SHPDriverFunction
import org.h2gis.functions.io.tsv.TSVDriverFunction

import java.sql.Connection
import java.sql.Statement

title = 'Import Table'
description = 'Import Table (csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv)'

inputs = [pathFile       : [name: 'Path of the input File', description: 'Path of the input File (including extension .csv, .shp, etc.)', title: 'Path of the input File', type: String.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database (default : first found db)', min: 0, max: 1, type: String.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : file name without extension)', title: 'Name of output table', min: 0, max: 1, type: String.class]]

outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

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
    if (input['databaseName']) {
        dbName = input['databaseName'] as String
    }

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable { Connection connection ->


        String pathFile = input["pathFile"] as String
        String fileName = FilenameUtils.removeExtension(new File(pathFile).getName())

        String outputTableName = input["outputTableName"] as String
        if (!outputTableName) {
            outputTableName = fileName
        }
        outputTableName = outputTableName.toUpperCase()

        Statement stmt = connection.createStatement()
        String dropOutputTable = "drop table if exists " + outputTableName
        stmt.execute(dropOutputTable)

        String ext = pathFile.substring(pathFile.lastIndexOf('.') + 1, pathFile.length())
        System.out.println(pathFile)
        switch (ext) {
            case "csv":
                CSVDriverFunction csvDriver = new CSVDriverFunction()
                csvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "dbf":
                DBFDriverFunction dbfDriver = new DBFDriverFunction()
                dbfDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "geojson":
                GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
                geoJsonDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "gpx":
                GPXDriverFunction gpxDriver = new GPXDriverFunction()
                gpxDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "bz2":
                OSMDriverFunction osmDriver = new OSMDriverFunction()
                osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "gz":
                OSMDriverFunction osmDriver = new OSMDriverFunction()
                osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "osm":
                OSMDriverFunction osmDriver = new OSMDriverFunction()
                osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "shp":
                SHPDriverFunction shpDriver = new SHPDriverFunction()
                shpDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
            case "tsv":
                TSVDriverFunction tsvDriver = new TSVDriverFunction()
                tsvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                break
        }

        return [tableNameCreated: "The table " + outputTableName + " has been upload to database !"]
    }
}
