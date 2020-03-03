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

package org.noise_planet.noisemodelling.wps.Import_and_Export

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.io.FileType
import groovy.time.TimeCategory
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
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation

import java.sql.Connection
import java.sql.Statement

title = 'Import all files from a folder'
description = 'Import all files with a specified extension from a folder to the database. </br> Valid file extensions : (csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv). </br>'

inputs = [
        pathFolder : [name: 'Path of the folder', title: 'Path of the folder', description: 'Path of the folder </br> For example : c:/home/inputdata/ ', type: String.class],
        inputSRID: [name: 'Projection identifier', title: 'Projection identifier', description: 'Original projection identifier (also called SRID) of all the table that contain a geometry attribute. It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). </br>  All coordinates will be projected from the specified EPSG to WGS84 coordinates. </br> This entry is optional because many formats already include the projection and you can also import files without geometry attributes.</br>  <b> Default value : 4326 </b> ', type: Integer.class, min: 0, max: 1],
        importExt: [name: 'Extension to import', title: 'Extension to import', description: 'Extension to import. </br> For example : shp ', type: String.class]
]

outputs = [
        result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]
]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Import all files of a folder')
    def start = new Date()

    // Default SRID (WGS84)
    Integer srid = 4326
    // Get user SRID
    if (input['inputSRID']) {
        srid = input['inputSRID'] as Integer
    }

    // Get file extension
    String importExt = input["importExt"] as String

    // Get path of the folder
    String folder = input["pathFolder"] as String
    def dir = new File(folder)

    // name of the imported tables
    String outputTableName_full = null

    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()

    dir.eachFileRecurse(FileType.FILES) { file ->

        String pathFile = file as String
        String ext = pathFile.substring(pathFile.lastIndexOf('.') + 1, pathFile.length())

        if (ext == importExt) {

            // get the name of the fileName
            String fileName = FilenameUtils.removeExtension(new File(pathFile).getName())
            // replace whitespaces by _ in the file name
            fileName.replaceAll("\\s", "_")
            // remove special characters in the file name
            fileName.replaceAll("[^a-zA-Z0-9 ]+", "_")
            // the tableName will be called as the fileName
            String outputTableName = fileName.toUpperCase()

            // Drop the table if already exists
            String dropOutputTable = "drop table if exists " + outputTableName
            stmt.execute(dropOutputTable)

            switch (ext) {
                case "csv":
                    CSVDriverFunction csvDriver = new CSVDriverFunction()
                    csvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "dbf":
                    DBFDriverFunction dbfDriver = new DBFDriverFunction()
                    dbfDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "geojson":
                    GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
                    geoJsonDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "gpx":
                    GPXDriverFunction gpxDriver = new GPXDriverFunction()
                    gpxDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "bz2":
                    OSMDriverFunction osmDriver = new OSMDriverFunction()
                    osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "gz":
                    OSMDriverFunction osmDriver = new OSMDriverFunction()
                    osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "osm":
                    OSMDriverFunction osmDriver = new OSMDriverFunction()
                    osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "shp":
                    SHPDriverFunction shpDriver = new SHPDriverFunction()
                    shpDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "tsv":
                    TSVDriverFunction tsvDriver = new TSVDriverFunction()
                    tsvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
            }


            // Read Geometry Index and type of the table
            List<String> spatialFieldNames = SFSUtilities.getGeometryFields(connection, TableLocation.parse(outputTableName, JDBCUtilities.isH2DataBase(connection.getMetaData())))

            // If the table does not contain a geometry field
            if (spatialFieldNames.isEmpty()) {
                System.out.println("The table "+ outputTableName+" does not contain a geometry field.")
            }

            // Get the SRID of the table
            Integer tableSrid = SFSUtilities.getSRID(connection, TableLocation.parse(outputTableName))

            if (tableSrid != 0 && tableSrid != srid && input['inputSRID']) {
                resultString = "The table "+ outputTableName+" already has a different SRID than the one you gave."
                // print to command window
                System.out.println('ERROR : ' + resultString)
                System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))
                // print to WPS Builder
                return resultString
            }

            // Replace default SRID by the srid of the table
            if (tableSrid != 0) srid = tableSrid

            // Display the actual SRID in the command window
            System.out.println("The SRID of the table "+ outputTableName+" is " + srid)

            // If the table does not have an associated SRID, add a SRID
            if (tableSrid == 0 && !spatialFieldNames.isEmpty()) {
                connection.createStatement().execute(String.format("UPDATE %s SET " + spatialFieldNames.get(0) + " = ST_SetSRID(" + spatialFieldNames.get(0) + ",%d)",
                        TableLocation.parse(outputTableName).toString(), srid))
            }

        }
    }


    resultString = "The table(s) " + outputTableName_full + " has/have been uploaded to database !"

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Import all files of a folder')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

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