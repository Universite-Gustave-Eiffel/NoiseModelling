/**
* @Author Nicolas Fortin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.wps

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.DBFDriverFunction
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.functions.io.json.JsonDriverFunction
import org.h2gis.functions.io.kml.KMLDriverFunction
import org.h2gis.functions.io.shp.SHPDriverFunction
import org.h2gis.functions.io.tsv.TSVDriverFunction

import java.sql.Connection

title = 'Export_Table'
description = 'Export database table to a file (csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv)'

inputs = [
        exportPath: [name: 'Export path', title: 'Path of the file to export', description: 'Path of the input File (including extension .csv, .shp, etc.)', type: String.class],
        databaseName: [name: 'Name of the database', title: 'Name of the database', description : 'Name of the database (default : first found db)', min : 0, max : 1, type: String.class],
        tableToExport: [name: 'Name of the table to export', title: 'Name of the table to export.',  type: String.class]
]

outputs = [
        result: [name: 'result', title: 'result', type: Boolean.class]
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