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
 * @Author Valetin Le Bescond, Université Gustave Eiffel, Ghent University
 */

package org.noise_planet.noisemodelling.wps.Dynamic

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.Tuple
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.*
import org.noise_planet.noisemodelling.emission.road.cnossosvar.RoadVehicleCnossosvar
import org.noise_planet.noisemodelling.emission.road.cnossosvar.RoadVehicleCnossosvarParameters

import java.sql.Connection
import java.sql.SQLException

title = 'Convert Individual Vehicles traffic to emission noise level and Snap them to the network point sources.'
description = 'Calculating dynamic road emissions based on vehicles trajectories and snap them to the network (table SOURCES_0DB should exists).' +
        '</br> </br> <b> The output table is called : LW_DYNAMIC_GEOM </b> ' +
        'and contain : </br>' +
        '-  <b> T  </b> : The TIMESTAMP iteration (STRING).</br>' +
        '-  <b> IDRECEIVER  </b> : an identifier (INTEGER, PRIMARY KEY). </br>' +
        '- <b> THE_GEOM </b> : the 3D geometry of the receivers (POINT). </br> ' +
        '-  <b> HZ63, HZ125, HZ250, HZ500, HZ1000,HZ2000, HZ4000, HZ8000 </b> : 8 columns giving the day emission sound level for each octave band (FLOAT).'

inputs = [
        tableVehicles : [name : 'table of the individual Vehicles',
                  title : "table of the individual Vehicles",
                  description : "it should contain timestep, geometry (POINT), speed, acceleration, veh_type...",
                  type: String.class],
        tableSourceGeom : [name : 'table of network points source geometry, the output emission will be reattached to the' +
                ' index of this table according to the snap distance',
                           title : "table of the individual Vehicles",
                           description : "table of points source geometry, the output emission will be reattached to" +
                                   " the index of this table according to the snap distance." +
                                   " See Point_Source_From_Network to convert lines to points",
                           type: String.class],
        distance2snap : [name : 'Maximum distance to snap on the network point sources',
                  title : "Maximum distance to snap on the network point sources",
                  description : "Maximum distance to snap on the network point sources",
                         min        : 0,
                         max        : 1,
                  type: Double.class],
        tableFormat : [name : 'Format of the individual Vehicles table',
                  title : 'Format of the individual Vehicles table',
                  description :'Format of the individual Vehicles table. Can be for the moment SUMO or Matsim. See in the code to understand the different format.',
                  type: String.class],
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]

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
def exec(Connection connection, Map input) {

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class))

    // Open sql connection to communicate with the database
    Sql sql = new Sql(connection)

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start ')
    def start = new Date()

    // -------------------
    // Get every inputs
    // -------------------

    String vehicles_table_name = input['tableVehicles']
    
    String tableSourceGeom = input["tableSourceGeom"] as String

    String tableFormat = input['tableFormat']

    double distance2snap = input['distance2snap']

    // do it case-insensitive
    vehicles_table_name = vehicles_table_name.toUpperCase()
    // Check if srid are in metric projection.
    sridSources = GeometryTableUtilities.getSRID(connection, TableLocation.parse(vehicles_table_name))
    if (sridSources == 3785 || sridSources == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+vehicles_table_name+".")
    if (sridSources == 0) throw new IllegalArgumentException("Error : The table "+vehicles_table_name+" does not have an associated SRID.")

    System.out.println('Start  time : ' + TimeCategory.minus(new Date(), start))

    def nameAndIndex = JDBCUtilities.getIntegerPrimaryKeyNameAndIndex(connection, TableLocation.parse(tableSourceGeom, dbType))
    if(nameAndIndex == null) {
        throw new SQLException("Table table $nameAndIndex does not contain a primary key")
    }
    String primaryKeyColumnName = nameAndIndex.first()

    VehicleEmissionProcessData vehicleEmissionProcessData = new VehicleEmissionProcessData();
    vehicleEmissionProcessData.setDynamicEmissionTable(vehicles_table_name, sql, tableFormat)

    sql.execute("drop table if exists SOURCES_EMISSION;")

    // Associate vehicles position to the closest source point
    sql.execute("create table SOURCES_EMISSION as SELECT b.PERIOD ," +
            "b.Lw63, b.Lw125, b.Lw250, b.Lw500, b.Lw1000, b.Lw2000, b.Lw4000, b.Lw8000," +
            " (SELECT a.$primaryKeyColumnName FROM $tableSourceGeom a " +
            "WHERE ST_EXPAND(b.the_geom,$distance2snap, $distance2snap) && a.the_geom" +
            "  ORDER BY ST_Distance(a.the_geom, b.the_geom) ASC LIMIT 1) IDSOURCE FROM LW_VEHICLE b ;")
        
    sql.execute("DROP TABLE IF EXISTS LW_VEHICLE")

    System.out.println('Intermediate  time : ' + TimeCategory.minus(new Date(), start))
    System.out.println("Export data to table")

    resultString = "Calculation Done ! The table SOURCES_EMISSION has been created."

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End ')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString
}

/**
 * This class is used to generate the road traffic noise level for each octave band.
 */
class VehicleEmissionProcessData {


    void setDynamicEmissionTable(String tablename, Sql sql, String tableFormat) throws SQLException {

        //////////////////////
        // Import file text
        //////////////////////

        sql.execute("drop table if exists LW_DYNAMIC;")
        sql.execute("create table LW_VEHICLE(PERIOD varchar, THE_GEOM geometry, Lw63 double precision, Lw125 double precision, Lw250 double precision, Lw500 double precision, Lw1000 double precision, Lw2000 double precision, Lw4000 double precision, Lw8000 double precision);")
        def qry = 'INSERT INTO LW_VEHICLE(PERIOD , THE_GEOM,Lw63, Lw125, Lw250, Lw500, Lw1000,Lw2000, Lw4000, Lw8000) VALUES (?,?,?,?,?,?,?,?,?,?);'

        if (tableFormat.equals("SUMO")){
                // Remplissage des variables avec le contenu du fichier SUMO
                sql.eachRow('SELECT THE_GEOM, SPEED, ID, TIMESTEP FROM ' + tablename + ';') { row ->

                    Geometry the_geom = (Geometry) row[0]
                    double speed = (double) row[1]
                    def id_veh = row[2]

                    // Try to convert id_veh to an Integer if it's a String
                    if (id_veh instanceof String) {
                        try {
                            id_veh = Integer.parseInt(id_veh) // Convert to Integer
                        } catch (NumberFormatException e) {
                            // If conversion fails, id_veh remains unchanged (still a String)
                        }
                    }

                    int timestep = (int) row[3]
                    // in SUMO, the speed is in m.s-1, we need to convert it in km.h-1
                    double[] carLevel = getCarsLevel(speed*3.6, id_veh)
                    sql.withBatch(100, qry) { ps ->
                        ps.addBatch(timestep as String, the_geom as Geometry,
                                carLevel[0] as Double, carLevel[1] as Double, carLevel[2] as Double,
                                carLevel[3] as Double, carLevel[4] as Double, carLevel[5] as Double,
                                carLevel[6] as Double, carLevel[7] as Double)
                    }

                }
        } else if (tableFormat.equals("SYMUVIA")){
            // Remplissage des variables avec le contenu du fichier SUMO
            sql.eachRow('SELECT THE_GEOM, SPEED, ID, TIMESTEP FROM ' + tablename + ';') { row ->

                Geometry the_geom = (Geometry) row[0]
                double speed = (double) row[1]

                def id_veh = row[2]

                // Try to convert id_veh to an Integer if it's a String
                if (id_veh instanceof String) {
                    try {
                        id_veh = Integer.parseInt(id_veh) // Convert to Integer
                    } catch (NumberFormatException e) {
                        // If conversion fails, id_veh remains unchanged (still a String)
                    }
                }


                int timestep = (int) row[3]

                // in SYMUVIA, the speed is in km.h-1
                double[] carLevel = getCarsLevel(speed, id_veh)
                sql.withBatch(100, qry) { ps ->
                    ps.addBatch(timestep as String, the_geom as Geometry,
                            carLevel[0] as Double, carLevel[1] as Double, carLevel[2] as Double,
                            carLevel[3] as Double, carLevel[4] as Double, carLevel[5] as Double,
                            carLevel[6] as Double, carLevel[7] as Double)
                }

            }

        }  else    {
            System.out.println("Unknown File Format")

        }


    }

    double[] getCarsLevel(double speed, def id_veh) throws SQLException {
        double[] res_LV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        def list = [63, 125, 250, 500, 1000, 2000, 4000, 8000]

        int kk = 0
        for (f in list) {
            int acc = 0
            String RoadSurface = "DEF"
            boolean Stud = false
            String veh_type = "1"
            int acc_type = 1
            double LwStd = 1

            int VehId = 10

            // Check if id_veh is an Integer
            if (id_veh instanceof Integer) {
                VehId = id_veh as Integer // Replace VehId with the value of id_veh
            }

            RoadVehicleCnossosvarParameters rsParameters = new RoadVehicleCnossosvarParameters(speed, acc, veh_type, acc_type, Stud, LwStd, VehId)
            rsParameters.setRoadSurface(RoadSurface)
            rsParameters.setSlopePercentage(0)
            res_LV[kk] = RoadVehicleCnossosvar.evaluate(rsParameters)
            kk++
        }

        return res_LV
    }



}
