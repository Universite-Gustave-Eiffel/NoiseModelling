/**
* @Author Hesry Quentin
* @Author Pierre Aumond
*/

package org.noise_planet.noisemodelling.scriptwps

import geoserver.GeoServer
import geoserver.catalog.Store
import org.apache.commons.io.FilenameUtils
import org.geotools.jdbc.JDBCDataStore
import groovy.io.FileType

import java.sql.Connection
import java.sql.Statement

import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.*
import org.h2gis.functions.io.geojson.*
import org.h2gis.functions.io.gpx.*
import org.h2gis.functions.io.osm.*
import org.h2gis.functions.io.shp.*
import org.h2gis.functions.io.tsv.*
import org.h2gis.api.EmptyProgressVisitor

import org.h2gis.utilities.wrapper.*

title = 'Import Tables from folder'
description = 'Import Tables (.shp, .csv, etc.)'

inputs = [
        pathFile: [name: 'Path of the folder', description : 'Path of the folder', title: 'Path of the folder', type: String.class],
        user_ext: [name: 'Extension to import', description : 'Extension to import (shp, csv, etc.). Don\'t use the dot !', title: 'Extension to import', type: String.class]
]

outputs = [
        tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]
]

def static Connection openPostgreSQLDataStoreConnection() {
    Store store = new GeoServer().catalog.getStore("h2gisdb")
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {
        Connection connection = openPostgreSQLDataStoreConnection()
        String user_ext = input["user_ext"] as String  
        String folder = input["pathFile"] as String  
        String outputTableName_full = ""

        def dir = new File(folder)
        dir.eachFileRecurse (FileType.FILES) { file ->
                 
                String pathFile = file as String  
                String ext = pathFile.substring(pathFile.lastIndexOf('.') + 1, pathFile.length())

                if (ext ==  user_ext){
                        System.out.println("Reading : " + pathFile)
                        
                        String fileName = FilenameUtils.removeExtension(new File(pathFile).getName())  
                        String outputTableName = fileName
                        outputTableName = outputTableName.toUpperCase()
                        
                        Statement stmt = connection.createStatement()
                        String dropOutputTable = "drop table if exists " + outputTableName
                        stmt.execute(dropOutputTable)
                        
                        switch(ext) {
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
                                        outputTableName_full = outputTableName + " & " + outputTableName_full
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
                                        outputTableName_full = outputTableName + " & " + outputTableName_full
                                        break
                                case "tsv":
                                        TSVDriverFunction tsvDriver = new TSVDriverFunction()
                                        tsvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                                        break
                        }
                }
        }
        
        return [tableNameCreated : "The table " + outputTableName_full + " has/have been created !"]    
}
