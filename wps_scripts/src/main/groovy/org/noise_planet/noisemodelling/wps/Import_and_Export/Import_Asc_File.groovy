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
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.utility.PRJUtil
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter
import org.noise_planet.noisemodelling.ext.asc.AscDriverFunction
import org.noise_planet.noisemodelling.ext.asc.AscReaderDriver
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor

import java.sql.Connection
import java.sql.Statement

title = 'Import Asc File.'
description = 'Import ESRI Ascii Raster file and convert into a Digital Elevation Model (DEM) compatible with NoiseModelling (X,Y,Z). </br> Valid file extensions : (asc). </br>' +
        '</br> </br> <b> The output table is called : DEM </b> ' +
        'and contain : </br>' +
        '- <b> THE_GEOM </b> : the 3D point cloud of the DEM (POINT).</br> '

inputs = [pathFile : [name: 'Path of the input File', title: 'Path of the ESRI Ascii Raster file', description: 'Path of the ESRI Ascii Raster file you want to import, including its extension. </br> For example : c:/home/receivers.asc', type: String.class],
          inputSRID: [name: 'Projection identifier', title: 'Projection identifier', description: 'Original projection identifier (also called SRID) of your table. It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). (INTEGER) </br>  All coordinates will be projected from the specified EPSG to WGS84 coordinates. </br> This entry is optional because many formats already include the projection and you can also import files without geometry attributes.</br> </br> <b> Default value : 4326 </b> ', type: Integer.class, min: 0, max: 1],
          fence    : [name: 'Fence geometry', title: 'Fence geometry', description: 'Create DEM table only in the provided polygon', min: 0, max: 1, type: Geometry.class],
          downscale: [name: 'SkipPixels', title: 'Skip pixels on each axis', description: 'Divide the number of rows and columns read by the following coefficient (FLOAT) </br> </br> <b> Default value : 1.0 </b>', min: 0, max: 1, type: Integer.class]]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]


static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
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

def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Import Asc File')
    def start = new Date()

    // Default SRID (WGS84)
    Integer defaultSRID = 4326
    // Get user SRID
    if (input['inputSRID']) {
        defaultSRID = input['inputSRID'] as Integer
    }

    Integer downscale = 1
    if (input['downscale']) {
        downscale = Math.max(1, input['downscale'] as Integer)
    }

    String fence = null
    if (input['fence']) {
        fence = (String) input['fence']
    }

    String pathFile = input["pathFile"] as String

    def file = new File(pathFile)
    if (!file.exists()) {
        resultString = pathFile + " is not found."
        // print to command window
        System.out.println('ERROR : ' + resultString)
        System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))
        // print to WPS Builder
        return resultString
    }

    String outputTableName = 'ASC'

    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()



    // Get the extension of the file
    String ext = pathFile.substring(pathFile.lastIndexOf('.') + 1, pathFile.length())
    if (ext != "asc") {
        resultString = "The extension is not valid"
        // print to command window
        System.out.println('ERROR : ' + resultString)
        System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))
        // print to WPS Builder
        return resultString
    }
    switch (ext) {
        case "asc":
            AscDriverFunction ascDriver = new AscDriverFunction()
            ascDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
            break

    }

    AscReaderDriver ascDriver = new AscReaderDriver()
    ascDriver.setAs3DPoint(true)
    ascDriver.setExtractEnvelope()

    int srid = defaultSRID

    String filePath = new File(pathFile).getAbsolutePath()
    final int dotIndex = filePath.lastIndexOf('.')
    final String fileNamePrefix = filePath.substring(0, dotIndex)
    File prjFile = new File(fileNamePrefix + ".prj")
    if (prjFile.exists()) {
        System.out.println("Found prj file :" + prjFile.getAbsolutePath())
        try {
            srid = PRJUtil.getSRID(prjFile)
            if (srid == 0) {
                srid = defaultSRID;
            }
        } catch (IllegalArgumentException ex) {
            System.err.println("PRJ file invalid, use default SRID " + prjFile.getAbsolutePath())
        }
    } else {
        System.err.println("PRJ file not found " + prjFile.getAbsolutePath())
    }
    if (fence != null) {
        // Reproject fence
        if (srid != 0) {
            // Transform fence to the same coordinate system than the DEM table
            Geometry fenceGeom = null
            WKTReader wktReader = new WKTReader()
            WKTWriter wktWriter = new WKTWriter()
            if (input['fence']) {
                fenceGeom = wktReader.read(input['fence'] as String)
            }
            System.out.println("Got fence :" + wktWriter.write(fenceGeom))
            Geometry fenceTransform = ST_Transform.ST_Transform(connection, ST_SetSRID.setSRID(fenceGeom, 4326), srid)
            ascDriver.setExtractEnvelope(fenceTransform.getEnvelopeInternal())
            System.out.println("Fence coordinate transformed :" + wktWriter.write(fenceTransform))
        } else {
            throw new IllegalArgumentException("Unable to find DEM SRID but fence was provided")
        }
    }
    if (downscale > 1) {
        ascDriver.setDownScale(downscale)
    }

    // Import ASC file
    RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1)
    new FileInputStream(new File(pathFile)).withStream { inputStream ->
        ascDriver.read(connection, inputStream, progressLogger, TableLocation.parse('DEM').toString(), srid)
    }

    // Drop the table if already exists
    String dropOutputTable = "drop table if exists " + outputTableName
    stmt.execute(dropOutputTable)


    // Display the actual SRID in the command window
    System.out.println("The SRID of your table is " + srid)


    resultString = "The table DEM has been uploaded to database ! </br>  Its SRID is : " + srid + ". </br> Remember that to calculate a noise map, your SRID must be in metric coordinates. Please use the Wps block 'Change SRID' if needed."

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Import File')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

}