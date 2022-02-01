/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */


package org.noise_planet.noisemodelling.wps.Acoustic_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.jdbc.BezierContouring
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Create an isosurface from a NoiseModelling result table and its associated TRIANGLES table.'

description = 'Create an isosurface from a NoiseModelling result table and its associated TRIANGLES table. The Triangle vertices table must have been created using the WPS block <b> Receivers/Delaunay_Grid </b> . ' +
        '</br> </br> <b> The output table is called :  CONTOURING_NOISE_MAP'

inputs = [
        resultTable      : [
                name       : 'Sound levels table',
                title      : 'Sound levels table',
                description: 'Name of the sound levels table, generated from Noise_level_from_source. (STRING) ' +
                        '</br> </br>  example : LDEN_GEOM.',
                type       : String.class
        ],
        isoClass         : [
                name       : 'Iso levels in dB',
                title      : 'Iso levels in dB',
                description: 'Separation of sound levels for isosurfaces. ' +
                        '</br> </br> <b> Default value : 35.0,40.0,45.0,50.0,55.0,60.0,65.0,70.0,75.0,80.0,200.0 </b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        smoothCoefficient: [
                name       : 'Polygon smoothing coefficient',
                title      : 'Polygon smoothing coefficient',
                description: 'This coefficient (Bezier curve coefficient) will smooth generated isosurfaces. If equal to 0, it disables the smoothing step.' +
                        '</br> </br> <b> Default value : 1.0 </b>',
                min        : 0, max: 1,
                type       : Double.class
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
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

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // output string, the information given back to the user
    String resultString = null

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Compute Isosurfaces')
    logger.info("inputs {}", input) // log inputs of the run


    List<Double> isoLevels = BezierContouring.NF31_133_ISO // default values

    if (input.containsKey("isoClass")) {
        isoLevels = new ArrayList<>()
        StringTokenizer st = new StringTokenizer(input['isoClass'] as String, ",")
        while (st.hasMoreTokens()) {
            isoLevels.add(Double.parseDouble(st.nextToken()))
        }
    }

    String levelTable = input['resultTable'] as String

    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(levelTable))

    BezierContouring bezierContouring = new BezierContouring(isoLevels, srid)

    bezierContouring.setPointTable(levelTable)

    if (input.containsKey("smoothCoefficient")) {
        double coefficient = input['smoothCoefficient'] as Double
        if (coefficient < 0.01) {
            bezierContouring.setSmooth(false)
        } else {
            bezierContouring.setSmooth(true)
            bezierContouring.setSmoothCoefficient(coefficient)
        }
    }

    bezierContouring.createTable(connection)

    resultString = "Table " + bezierContouring.getOutputTable() + " created"

    logger.info('End : Compute Isosurfaces')
    logger.info(resultString)

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