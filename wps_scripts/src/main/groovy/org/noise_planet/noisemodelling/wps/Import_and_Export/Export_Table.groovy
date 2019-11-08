/**
* @Author Nicolas Fortin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.wps.Import_and_Export

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore

import java.sql.Connection
import java.sql.Statement

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

title = 'Export_Table'
description = 'Export_Table (csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv)'

inputs = [
        exportPath: [name: 'Export path', title: 'Path of the file to export', description: 'Path of the output File (including extension .csv, .shp, etc.)', type: String.class],
        databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database. (default : h2gisdb)', min : 0, max : 1, type: String.class],
        tableToExport: [name: 'Name of the table to export', title: 'Name of the table to export.',  type: String.class]
]

outputs = [
        result: [name: 'result', title: 'result', type: Boolean.class]
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

                connection = new WpsConnectionWrapper(connection)

                String exportPath = input["exportPath"] as String
                String tableToExport = input["tableToExport"] as String
                tableToExport = tableToExport.toUpperCase()

                String ext = exportPath.substring(exportPath.lastIndexOf('.') + 1, exportPath.length())
                String success = "Table " + tableToExport + " successfully exported !"

                switch (ext) {
                        case "csv":
                                CSVDriverFunction csvDriver = new CSVDriverFunction()
                                csvDriver.exportTable(connection, tableToExport, new File(exportPath), new EmptyProgressVisitor())
                                break
                        case "dbf":
                                DBFDriverFunction dbfDriver = new DBFDriverFunction()
                                dbfDriver.exportTable(connection, tableToExport, new File(exportPath), new EmptyProgressVisitor())
                                break
                        case "geojson":
                                GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
                                geoJsonDriver.exportTable(connection, tableToExport, new File(exportPath), new EmptyProgressVisitor())
                                break
                        case "json":
                                JsonDriverFunction jsonDriver = new JsonDriverFunction()
                                jsonDriver.exportTable(connection, tableToExport, new File(exportPath), new EmptyProgressVisitor())
                                break
                        case "kml":
                                KMLDriverFunction kmlDriver = new KMLDriverFunction()
                                kmlDriver.exportTable(connection, tableToExport, new File(exportPath), new EmptyProgressVisitor())
                                break
                        case "shp":
                                SHPDriverFunction shpDriver = new SHPDriverFunction()
                                shpDriver.exportTable(connection, tableToExport, new File(exportPath), new EmptyProgressVisitor())
                                break
                        case "tsv":
                                TSVDriverFunction tsvDriver = new TSVDriverFunction()
                                tsvDriver.exportTable(connection, tableToExport, new File(exportPath), new EmptyProgressVisitor())
                                break
                        default:
                                success = "Error ! table not exported"
                                break
                }

                return [result: success]
        }
}
