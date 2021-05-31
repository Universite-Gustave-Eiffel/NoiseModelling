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


package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.spatial.edit.ST_AddZ
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.noise_planet.noisemodelling.emission.RailWayLW
import org.noise_planet.noisemodelling.jdbc.LDENConfig
import org.noise_planet.noisemodelling.jdbc.LDENPropagationProcessData
import org.noise_planet.noisemodelling.jdbc.RailWayLWIterator
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * @Author Pierre Aumond,  Univ Gustave Eiffel
 * @Author Adrien Le Bellec,  Univ Gustave Eiffel
 * @Author Olivier Chiello, Univ Gustave Eiffel
 */

title = 'Compute railway emission noise map from vehicule, traffic table AND section table.'
description = 'Compute Rail Emission Noise Map from Day Evening Night traffic flow rate and speed estimates (specific format, see input details). ' +
        '</br> </br> <b> The output table is called : LW_RAILWAY </b> '

inputs = [
        tableRailwayTraffic: [
                name                                                                                                                            : 'Railway traffic table name',
                title                                                                                                                           : 'Railway traffic table name',
                description                                                                                                                     : "<b>Name of the Rail traffic table.</b>  </br>  " +
                        "<br>  This function recognize the following columns (* mandatory) : </br><ul>" +
                        "<li><b> idTraffic </b>* : an identifier. It shall be a primary key (INT, PRIMARY KEY)</li>" +
                        "<li><b> idSection </b>* : an identifier. (INT)</li>" +
                        "<li><b> TYPETRAIN </b>* : Type vehicle (STRING)/li>" +
                        "<li><b> speedVehic </b>* : Maximum Train speed (DOUBLE) </li>" +
                        "<li><b> TDAY </b><b> TEVENING </b><b> TNIGHT </b> : Hourly average train count (6-18h)(18-22h)(22-6h) (INT)</li>", type: String.class],
        tableRailwayTrack  : [
                name                                           : 'RailWay Geom table name', title: 'RailWay Track table name', description: "<b>Name of the Railway Track table.</b>  </br>  " +
                "<br>  This function recognize the following columns (* mandatory) : </br><ul>" +
                "<li><b> idSection </b>* : an identifier. It shall be a primary key(INTEGER, PRIMARY KEY)</li>" +
                "<li><b> nTrack </b>* : Number of tracks (INTEGER) /li>" +
                "<li><b> speedTrack </b>* : Maximum speed on the section in km/h (DOUBLE) </li>" +
                "<li><b> trackTrans </b> : Track transfer function identifier (INTEGER) </li>" +
                "<li><b> railRoughn </b> : Rail roughness identifier (INTEGER)  </li>" +
                "<li><b> impactNois </b> : Impact noise identifier (INTEGER) </li>" +
                "<li><b> curvature </b> : Listed code describing the curvature of the section (INTEGER) </li>" +
                "<li><b> bridgeTran </b> : Bridge transfer function identifier (INTEGER) </li>" +
                "<li><b> speedComme </b> : Commercial speed on the section in km/h (DOUBLE) </li>" +
                "<li><b> isTunnel </b> : (BOOLEAN) </li>", type: String.class]
]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
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

// main function of the script
def exec(Connection connection, input) {
    Logger LOGGER = LoggerFactory.getLogger("Railway_Emission_from_Traffic")

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // print to command window
    LOGGER.info('Start : Railway Emission from DEN')

    // -------------------
    // Get every inputs
    // -------------------

    String sources_geom_table_name = input['tableRailwayTrack'] as String
    // do it case-insensitive
    sources_geom_table_name = sources_geom_table_name.toUpperCase()

    int sridSources = SFSUtilities.getSRID(connection, TableLocation.parse(sources_geom_table_name))
    if (sridSources == 3785 || sridSources == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+sources_geom_table_name+".")
    if (sridSources == 0) throw new IllegalArgumentException("Error : The table "+sources_geom_table_name+" does not have an associated spatial reference system. (missing prj file on import ?)")

    String sources_table_traffic_name = input['tableRailwayTraffic'] as String
    // do it case-insensitive
    sources_table_traffic_name = sources_table_traffic_name.toUpperCase()

    //Get the geometry field of the source table
    TableLocation sourceTableIdentifier = TableLocation.parse(sources_geom_table_name)
    List<String> geomFields = SFSUtilities.getGeometryFields(connection, sourceTableIdentifier)
    if (geomFields.isEmpty()) {
        throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier))
    }


    // -------------------
    // Init table LW_RAIL
    // -------------------

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // drop table LW_RAILWAY if exists and the create and prepare the table
    sql.execute("drop table if exists LW_RAILWAY;")

    // Build and execute queries
    StringBuilder createTableQuery = new StringBuilder("create table LW_RAILWAY (ID_SECTION int," +
            " the_geom geometry, DIR_ID int")
    StringBuilder insertIntoQuery = new StringBuilder("INSERT INTO LW_RAILWAY(ID_SECTION, the_geom," +
            " DIR_ID")
    StringBuilder insertIntoValuesQuery = new StringBuilder("?,?,?")
    for(int thirdOctave : PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
        createTableQuery.append(", LWD")
        createTableQuery.append(thirdOctave)
        createTableQuery.append(" double precision")
        insertIntoQuery.append(", LWD")
        insertIntoQuery.append(thirdOctave)
        insertIntoValuesQuery.append(", ?")
    }
    for(int thirdOctave : PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
        createTableQuery.append(", LWE")
        createTableQuery.append(thirdOctave)
        createTableQuery.append(" double precision")
        insertIntoQuery.append(", LWE")
        insertIntoQuery.append(thirdOctave)
        insertIntoValuesQuery.append(", ?")
    }
    for(int thirdOctave : PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE) {
        createTableQuery.append(", LWN")
        createTableQuery.append(thirdOctave)
        createTableQuery.append(" double precision")
        insertIntoQuery.append(", LWN")
        insertIntoQuery.append(thirdOctave)
        insertIntoValuesQuery.append(", ?")
    }
    createTableQuery.append(")")
    insertIntoQuery.append(") VALUES (")
    insertIntoQuery.append(insertIntoValuesQuery)
    insertIntoQuery.append(")")
    sql.execute(createTableQuery.toString())

    // --------------------------------------
    // Start calculation and fill the table
    // --------------------------------------

    // Get Class to compute LW

    LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_RAILWAY_FLOW)
    ldenConfig.setPropagationProcessPathData(new PropagationProcessPathData())
    ldenConfig.setCoefficientVersion(2)

    // Get size of the table (number of rail segments
    PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) AS total FROM " + sources_geom_table_name)
    SpatialResultSet rs1 = st.executeQuery().unwrap(SpatialResultSet.class)

    while (rs1.next()) {
        nSection = rs1.getInt("total")
        System.println('The table Rail Geom has ' + nSection + ' rail segments.')
    }

    RailWayLWIterator railWayLWIterator = new RailWayLWIterator(connection, sources_geom_table_name, sources_table_traffic_name, ldenConfig)
    RailWayLWIterator.RailWayLWGeom railWayLWGeom;

    while ((railWayLWGeom = railWayLWIterator.next()) != null) {
        RailWayLW railWayLWDay = railWayLWGeom.getRailWayLWDay()
        RailWayLW railWayLWEvening = railWayLWGeom.getRailWayLWEvening()
        RailWayLW railWayLWNight = railWayLWGeom.getRailWayLWNight()
        List<LineString> geometries = railWayLWGeom.getRailWayLWGeometry(2) //set distance between Rail
        int pk = railWayLWGeom.getPK()
        double[] LWDay
        double[] LWEvening
        double[] LWNight
        double heightSource
        int directivityId
        for (int iSource = 0; iSource < 6; iSource++) {
            switch (iSource) {
                case 0:
                    LWDay = railWayLWDay.getLWRolling()
                    LWEvening = railWayLWEvening.getLWRolling()
                    LWNight = railWayLWNight.getLWRolling()
                    heightSource = 0.5
                    directivityId = 1
                    break
                case 1:
                    LWDay = railWayLWDay.getLWTractionA()
                    LWEvening = railWayLWEvening.getLWTractionA()
                    LWNight = railWayLWNight.getLWTractionA()
                    heightSource = 0.5
                    directivityId = 2
                    break
                case 2:
                    LWDay = railWayLWDay.getLWTractionB()
                    LWEvening = railWayLWEvening.getLWTractionB()
                    LWNight = railWayLWNight.getLWTractionB()
                    heightSource = 4
                    directivityId = 3
                    break
                case 3:
                    LWDay = railWayLWDay.getLWAerodynamicA()
                    LWEvening = railWayLWEvening.getLWAerodynamicA()
                    LWNight = railWayLWNight.getLWAerodynamicA()
                    heightSource = 0.5
                    directivityId = 4
                    break
                case 4:
                    LWDay = railWayLWDay.getLWAerodynamicB()
                    LWEvening = railWayLWEvening.getLWAerodynamicB()
                    LWNight = railWayLWNight.getLWAerodynamicB()
                    heightSource = 4
                    directivityId = 5
                    break
                case 5:
                    LWDay = railWayLWDay.getLWBridge()
                    LWEvening = railWayLWEvening.getLWBridge()
                    LWNight = railWayLWNight.getLWBridge()
                    heightSource = 0.5
                    directivityId = 6
                    break
            }
            for (int nTrack = 0; nTrack < geometries.size(); nTrack++) {

                sql.withBatch(100, insertIntoQuery.toString()) { ps ->
                    Geometry trackGeometry = (Geometry) geometries.get(nTrack)
                    Geometry sourceGeometry = trackGeometry.copy()
                    // offset geometry z
                    sourceGeometry.apply(new ST_AddZ.AddZCoordinateSequenceFilter(heightSource))
                    def batchData = [pk as int, sourceGeometry as Geometry, directivityId as int]
                    batchData.addAll(LWDay)
                    batchData.addAll(LWEvening)
                    batchData.addAll(LWNight)
                    ps.addBatch(batchData)
                }
            }
        }

    }


    // Fusion geometry and traffic table

    // Add Z dimension to the rail segments
    sql.execute("UPDATE LW_RAILWAY SET THE_GEOM = ST_SETSRID(ST_UPDATEZ(The_geom,0.01), :srid);", ["srid" : sridSources])

    // Add primary key to the LW table
    sql.execute("ALTER TABLE  LW_RAILWAY  ADD PK INT AUTO_INCREMENT PRIMARY KEY;")


    resultString = "Calculation Done ! The table LW_RAILWAY has been created."

    // print to command window
    System.out.println('\nResult : ' + resultString)
    System.out.println('End : LW_RAILWAY from Emission')
    // System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

}


