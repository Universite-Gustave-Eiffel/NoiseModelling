package org.noise_planet.noisemodelling.wps.Others_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import org.apache.commons.io.FilenameUtils
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.utility.PRJUtil
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter
//import org.noise_planet.noisemodelling.ext.asc.AscReaderDriver
import org.noise_planet.noisemodelling.propagation.RootProgressVisitor

import java.sql.Connection
import java.sql.Statement

title = 'Import File'
description = 'Import ESRI Ascii Raster file and convert into a DEM compatible with NoiseModelling (X,Y,Z) point cloud'

inputs = [pathFile       : [name: 'Path of the asc File', description: 'Path of the ESRI Ascii Raster file', title: 'Path of the input File', type: String.class],
          fence             : [name: 'Fence', title: 'Optional filtering of DEM extraction', min: 0, max: 1, type: Geometry.class],
          downscale      : [name: 'Downscale', title: 'Skip pixels on each axis', description: 'Divide the number of rows and columns read by this coefficient (default 1)', min: 0, max: 1, type: Integer.class],
          databaseName   : [name: 'Name of the database', title: 'Name of the database', description: 'Name of the database (default : first found db)', min: 0, max: 1, type: String.class],
          defaultSRID    : [name: 'Default SRID', title: 'Default SRID', description: 'If the layer does not include SRID properties, it will take this value (default : 4326)', min: 0, max: 1, type: Integer.class],
          outputTableName: [name: 'outputTableName', description: 'Do not write the name of a table that contains a space. (default : file name without extension)', title: 'Name of output table', min: 0, max: 1, type: String.class]]

outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

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
    String dbName = ""
    if (input['databaseName']){dbName = input['databaseName'] as String}

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return exec(connection, input)
    }

}
def exec(Connection connection, input) {

    Integer defaultSRID = 4326
    if (input['defaultSRID']) {
        defaultSRID = input['defaultSRID'] as Integer
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
    String fileName = FilenameUtils.removeExtension(new File(pathFile).getName())

    String outputTableName = input["outputTableName"] as String
    if (!outputTableName) {
        outputTableName = fileName
    }

    Statement stmt = connection.createStatement()
    stmt.execute("drop table if exists " + TableLocation.parse(outputTableName).toString())

    if (!new File(pathFile).exists()) {
        return [tableNameCreated: String.format("File %s does not exists",
                new File(pathFile).getAbsolutePath())]
    }

   /* AscReaderDriver ascDriver = new AscReaderDriver();
    ascDriver.setAs3DPoint(true)
    ascDriver.setExtractEnvelope()

    int srid = defaultSRID;
    String filePath = new File(pathFile).getAbsolutePath();
    final int dotIndex = filePath.lastIndexOf('.');
    final String fileNamePrefix = filePath.substring(0, dotIndex);
    File prjFile = new File(fileNamePrefix+".prj");
    if(prjFile.exists()) {
        System.out.println("Found prj file :" + prjFile.getAbsolutePath())
        try {
            srid = PRJUtil.getSRID(prjFile);
            if(srid == 0) {
                srid = defaultSRID;
            }
        } catch(IllegalArgumentException ex) {
            System.err.println("PRJ file invalid, use default SRID " + prjFile.getAbsolutePath())
        }
    } else {
        System.err.println("PRJ file not found " + prjFile.getAbsolutePath())
    }

    if (fence != null) {
        // Reproject fence
        if(srid != 0) {
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

    if(downscale > 1) {
        ascDriver.setDownScale(downscale);
    }

    // Import ASC file
    RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1);
    new FileInputStream(new File(pathFile)).withStream { inputStream ->
        ascDriver.read(connection, inputStream, progressLogger, TableLocation.parse(outputTableName).toString(), srid);
    }*/

    return [tableNameCreated: outputTableName]

}
