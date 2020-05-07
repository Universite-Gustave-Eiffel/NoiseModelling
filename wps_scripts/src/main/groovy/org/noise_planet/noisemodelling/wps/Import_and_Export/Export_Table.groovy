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
 * @Author Nicolas Fortin, Univ Gustave Eiffel
 * @Author Pierre Aumond, Univ Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Import_and_Export

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.DBFDriverFunction
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.functions.io.json.JsonDriverFunction
import org.h2gis.functions.io.kml.KMLDriverFunction
import org.h2gis.functions.io.shp.SHPDriverFunction
import org.h2gis.functions.io.tsv.TSVDriverFunction
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation

import java.nio.file.Paths
import java.sql.Connection


title = 'Export table'
description = 'Export table from the database to a local file. </br> Valid file extensions : (csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv).'

inputs = [
        exportPath   : [name: 'Export path', title: 'Path of the file you want to export', description: 'Path of the file, including its extension. </br> For example : c:/home/receivers.geojson', type: String.class],
        tableToExport: [name: 'Name of the table to export', title: 'Name of the table', description: 'Name of the table you want to export.', type: String.class]
]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]


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
    System.out.println('Start : Export File')
    def start = new Date()

    // get Export Path
    String exportPath = input["exportPath"] as String

    // get the name of the table to export
    String tableToExport = input["tableToExport"] as String
    // do it case-insensitive
    tableToExport = tableToExport.toUpperCase()

    List<String> fields = JDBCUtilities.getFieldNames(connection.getMetaData(), tableToExport)
    if (fields.size()<1)
    {
        return resultString = "The table is empty and can not be exported."
    }


    // run export
    String ext = exportPath.substring(exportPath.lastIndexOf('.') + 1, exportPath.length())
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
            return resultString = "The file extension is not valid. No table has been exported"
            break
    }


    //get SRID of the table
    int srid = SFSUtilities.getSRID(connection, TableLocation.parse(tableToExport))
    // if a SRID exists
    if (srid < 0) {
        System.println("Warning ! No SRID found !")
        resultString = "The table " + tableToExport + " successfully exported to <b>" +
                new File(exportPath).absolutePath + "</b>  without SRID ! "
    } else {
        resultString = "The table " + tableToExport + " successfully exported to <b>" +
                new File(exportPath).absolutePath + "</b>  with the SRID : " + srid
    }


    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Export File')
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