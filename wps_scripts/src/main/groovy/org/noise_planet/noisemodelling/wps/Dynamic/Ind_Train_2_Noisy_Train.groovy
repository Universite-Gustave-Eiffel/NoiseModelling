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
 * @Author Le Bellec Adrien, Université Gustave Eiffel
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
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.*
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters
import org.noise_planet.noisemodelling.emission.railway.cnossosvar.RailwayCnossosvar;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossosvar.RailwayVehicleCnossosParametersvar

import java.sql.Connection
import java.sql.SQLException

title = 'Convert Individual Train traffic to emission noise level and Snap them to the network point sources.'
description = 'Calculating dynamic rail emissions based on train trajectories and snap them to the network' +
        '</br> </br> <b> The output table is called : SOURCES_EMISSION </b> ' +
        'and contain : </br>' +
        '-  <b> IDSOURCE  </b> : an identifier (INTEGER). </br>' +
        '-  <b> PERIOD </b> : The TIMESTAMP iteration (STRING).</br>' +
        '-  <b> HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000 </b> : 8 columns giving the emission sound level for each octave band (FLOAT).'

inputs = [
        tableRailwayTrack  : [name : 'RailWay Geom table name',
                title : 'RailWay Track table name',
                description : '<b>Name of the Railway Track table.</b> </br><br>' +
                        'This function recognize the following columns (* mandatory): </br><ul>' +
                        '<li><b>IDSECTION</b>* : A section identifier (PRIMARY KEY) (INTEGER)</li>' +
                        '<li><b>NTRACK</b>* : Number of tracks (INTEGER)</li>' +
                        '<li><b>TRACKSPD</b>* : Maximum speed on the section (in km/h) (DOUBLE)</li>' +
                        '<li><b>TRANSFER</b> : Track transfer function identifier (INTEGER)</li>' +
                        '<li><b>ROUGHNESS</b> : Rail roughness identifier (INTEGER)</li>' +
                        '<li><b>IMPACT</b> : Impact noise coefficient identifier (INTEGER)</li>' +
                        '<li><b>CURVATURE</b> : Listed code describing the curvature of the section (INTEGER)</li>' +
                        '<li><b>BRIDGE</b> : Bridge transfer function identifier (INTEGER)</li>' +
                        '<li><b>TRACKSPD</b> : Commercial speed on the section (in km/h) (DOUBLE)</li>' +
                        '<li><b>ISTUNNEL</b> : Indicates whether the section is a tunnel or not (0 = no / 1 = yes) (BOOLEAN) </li></ul>',
                type: String.class],
        tableVehicles : [name : 'Individual Vehicles table',
                  title : "table of the individual Vehicles",
                  description : "it should contain timestep, geometry (POINT), speed, acceleration, veh_type...",
                  type: String.class],
        tableSourceGeom : [name : 'Source geometry table',
                           title : "table of the individual Vehicles",
                           description : "table of points source geometry, the output emission will be reattached to" +
                                   " the index of this table according to the snap distance. Should be SOURCES_GEOM" +
                                   " See Point_Source_From_Network to convert lines to points",
                           type: String.class],
        distance2snap : [name : 'Snap distance',
                  title : "Maximum distance to snap on the network point sources",
                  description : "Maximum distance to snap on the network point sources",
                         min        : 0,
                         max        : 1,
                  type: Double.class],
        tableFormat : [name : 'Vehicles table format',
                  title : 'Format of the individual Vehicles table',
                  description :'Format of the individual Vehicles table. Can be for the moment SUMO or Matsim. See in the code to understand the different format.',
                  type: String.class],
        keepNoEmissionGeoms : [name : 'Keep source geometries without emission value',
                       title : 'Keep source geometries without emission value',
                       description :'Do not delete source geometries that does not contain any emission value. Default to true, it reduce the computation time when evaluating the attenuation',
                       min : 0, max: 1, type: Boolean.class]
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

    String rail_track_table_name = input['tableRailwayTrack']

    String vehicles_table_name = input['tableVehicles']

    String tableSourceGeom = input["tableSourceGeom"] as String

    String tableFormat = input['tableFormat']

    double distance2snap = input['distance2snap'] // bug ?

    boolean removeGeomsNoEmission = true
    if (input['keepNoEmissionGeoms']) {
        removeGeomsNoEmission = !input['keepNoEmissionGeoms']
    }


    rail_track_table_name = rail_track_table_name.toUpperCase()
    // Check if srid are in metric projection.
    sridSources = GeometryTableUtilities.getSRID(connection, TableLocation.parse(rail_track_table_name))
    if (sridSources == 3785 || sridSources == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+rail_track_table_name+".")
    if (sridSources == 0) throw new IllegalArgumentException("Error : The table "+rail_track_table_name+" does not have an associated SRID.")

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

    TrainEmissionProcessData trainEmissionProcessData = new TrainEmissionProcessData();
    trainEmissionProcessData.setDynamicEmissionTable(vehicles_table_name, rail_track_table_name, sql, tableFormat)

    sql.execute("drop table if exists SOURCES_EMISSION_DUPLICATES;")

    // Associate vehicles position to the closest source point
    sql.execute("create table SOURCES_EMISSION_DUPLICATES as SELECT b.PERIOD ," +
            "b.HZ50, b.HZ63, b.HZ80, b.HZ100, b.HZ125, b.HZ160, b.HZ200, b.HZ250," +
            "b.HZ315, b.HZ400, b.HZ500, b.HZ630, b.HZ800, b.HZ1000, b.HZ1250, b.HZ1600," +
            "b.HZ2000, b.HZ2500, b.HZ3150, b.HZ4000, b.HZ5000, b.HZ6300, b.HZ8000, b.HZ10000," +
            " (SELECT a.$primaryKeyColumnName FROM $tableSourceGeom a " +
            "WHERE ST_EXPAND(b.the_geom,$distance2snap, $distance2snap) && a.the_geom" +
            "  ORDER BY ST_Distance(a.the_geom, b.the_geom) ASC LIMIT 1) IDSOURCE FROM LW_VEHICLE b;")
    sql.execute("DROP TABLE IF EXISTS LW_VEHICLE")
    sql.execute("DELETE FROM SOURCES_EMISSION_DUPLICATES WHERE IDSOURCE IS NULL")

    sql.execute("drop table if exists SOURCES_EMISSION;")

    // Two vehicles may be associated with the same source point location
    // do an energetic sum of the vehicles emission at each location
    def mergeIdSource = $/
            CREATE TABLE SOURCES_EMISSION(  IDSOURCE INTEGER NOT NULL,
                                            PERIOD VARCHAR NOT NULL,
                                            HZ50 REAL,HZ63 REAL,HZ80 REAL,
                                            HZ100 REAL,HZ125 REAL,HZ160 REAL,
                                            HZ200 REAL,HZ250 REAL,HZ315 REAL,
                                            HZ400 REAL,HZ500 REAL,HZ630 REAL,
                                            HZ800 REAL,HZ1000 REAL,HZ1250 REAL,
                                            HZ1600 REAL,HZ2000 REAL,HZ2500 REAL,
                                            HZ3150 REAL,HZ4000 REAL,HZ5000 REAL,
                                            HZ6300 REAL,HZ8000 REAL,HZ10000 REAL);
            INSERT INTO SOURCES_EMISSION SELECT IDSOURCE, PERIOD,
            10 * LOG10( SUM(POWER(10, HZ50 / 10))) AS HZ50,
            10 * LOG10( SUM(POWER(10, HZ63 / 10))) AS HZ63,
            10 * LOG10( SUM(POWER(10, HZ80 / 10))) AS HZ80,
            10 * LOG10( SUM(POWER(10, HZ100 / 10))) AS HZ100,
            10 * LOG10( SUM(POWER(10, HZ125 / 10))) AS HZ125,
            10 * LOG10( SUM(POWER(10, HZ160 / 10))) AS HZ160,
            10 * LOG10( SUM(POWER(10, HZ200 / 10))) AS HZ200,
            10 * LOG10( SUM(POWER(10, HZ250 / 10))) AS HZ250,
            10 * LOG10( SUM(POWER(10, HZ315 / 10))) AS HZ315,
            10 * LOG10( SUM(POWER(10, HZ400 / 10))) AS HZ400,
            10 * LOG10( SUM(POWER(10, HZ500 / 10))) AS HZ500,
            10 * LOG10( SUM(POWER(10, HZ630 / 10))) AS HZ630,
            10 * LOG10( SUM(POWER(10, HZ800 / 10))) AS HZ800,
            10 * LOG10( SUM(POWER(10, HZ1000 / 10))) AS HZ1000,
            10 * LOG10( SUM(POWER(10, HZ1250 / 10))) AS HZ1250,
            10 * LOG10( SUM(POWER(10, HZ1600 / 10))) AS HZ1600,
            10 * LOG10( SUM(POWER(10, HZ2000 / 10))) AS HZ2000,
            10 * LOG10( SUM(POWER(10, HZ2500 / 10))) AS HZ2500,
            10 * LOG10( SUM(POWER(10, HZ3150 / 10))) AS HZ3150,
            10 * LOG10( SUM(POWER(10, HZ4000 / 10))) AS HZ4000,
            10 * LOG10( SUM(POWER(10, HZ5000 / 10))) AS HZ5000,
            10 * LOG10( SUM(POWER(10, HZ6300 / 10))) AS HZ6300,
            10 * LOG10( SUM(POWER(10, HZ8000 / 10))) AS HZ8000,
            10 * LOG10( SUM(POWER(10, HZ10000 / 10))) AS HZ10000
            FROM SOURCES_EMISSION_DUPLICATES
            GROUP BY IDSOURCE, PERIOD;
    /$

    sql.execute(mergeIdSource)

    sql.execute("drop table if exists SOURCES_EMISSION_DUPLICATES;")

    sql.execute("ALTER TABLE SOURCES_EMISSION ADD PRIMARY KEY (IDSOURCE, PERIOD)")

    if(removeGeomsNoEmission) {
        // remove source point without associated emission values
        def cpt = sql.executeUpdate("DELETE FROM $tableSourceGeom WHERE NOT EXISTS (SELECT FROM SOURCES_EMISSION WHERE IDSOURCE = $primaryKeyColumnName) ".toString())
        System.out.println("$cpt geometry sources deleted")
    }


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

class TrainEmissionProcessData {
    void setDynamicEmissionTable(String tableVehicleName, String tableRailTrack, Sql sql, String tableFormat) throws SQLException {
        //////////////////////
        // Import file text
        //////////////////////

        sql.execute("drop table if exists TRAIN_POSITION;")
        sql.execute("create table TRAIN_POSITION(PERIOD varchar,THE_GEOM geometry);")

        sql.execute("drop table if exists LW_ROLLING;")
        sql.execute("create table LW_ROLLING(PERIOD varchar, IDSOURCE INTEGER NOT NULL," +
                "HZ50 double precision,HZ63 double precision,HZ80 double precision," +
                "HZ100 double precision,HZ125 double precision,HZ160 double precision," +
                "HZ200 double precision,HZ250 double precision,HZ315 double precision," +
                "HZ400 double precision,HZ500 double precision,HZ630 double precision," +
                "HZ800 double precision, HZ1000 double precision, HZ1250 double precision," +
                "HZ1600 double precision,HZ2000 double precision,HZ2500 double precision," +
                "HZ3150 double precision,HZ4000 double precision,HZ5000 double precision," +
                "HZ6300 double precision,HZ8000 double precision,HZ10000 double precision);")


        sql.execute("drop table if exists LW_TRACTIONA;")
        sql.execute("create table LW_TRACTIONA(PERIOD varchar, IDSOURCE INTEGER NOT NULL," +
                "HZ50 double precision,HZ63 double precision,HZ80 double precision," +
                "HZ100 double precision,HZ125 double precision,HZ160 double precision," +
                "HZ200 double precision,HZ250 double precision,HZ315 double precision," +
                "HZ400 double precision,HZ500 double precision,HZ630 double precision," +
                "HZ800 double precision, HZ1000 double precision, HZ1250 double precision," +
                "HZ1600 double precision,HZ2000 double precision,HZ2500 double precision," +
                "HZ3150 double precision,HZ4000 double precision,HZ5000 double precision," +
                "HZ6300 double precision,HZ8000 double precision,HZ10000 double precision);")

        sql.execute("drop table if exists LW_TRACTIONB;")
        sql.execute("create table LW_TRACTIONB(PERIOD varchar, IDSOURCE INTEGER NOT NULL," +
                "HZ50 double precision,HZ63 double precision,HZ80 double precision," +
                "HZ100 double precision,HZ125 double precision,HZ160 double precision," +
                "HZ200 double precision,HZ250 double precision,HZ315 double precision," +
                "HZ400 double precision,HZ500 double precision,HZ630 double precision," +
                "HZ800 double precision, HZ1000 double precision, HZ1250 double precision," +
                "HZ1600 double precision,HZ2000 double precision,HZ2500 double precision," +
                "HZ3150 double precision,HZ4000 double precision,HZ5000 double precision," +
                "HZ6300 double precision,HZ8000 double precision,HZ10000 double precision);")

        sql.execute("drop table if exists LW_AERODYNAMICA;")
        sql.execute("create table LW_AERODYNAMICA(PERIOD varchar, IDSOURCE INTEGER NOT NULL," +
                "HZ50 double precision,HZ63 double precision,HZ80 double precision," +
                "HZ100 double precision,HZ125 double precision,HZ160 double precision," +
                "HZ200 double precision,HZ250 double precision,HZ315 double precision," +
                "HZ400 double precision,HZ500 double precision,HZ630 double precision," +
                "HZ800 double precision, HZ1000 double precision, HZ1250 double precision," +
                "HZ1600 double precision,HZ2000 double precision,HZ2500 double precision," +
                "HZ3150 double precision,HZ4000 double precision,HZ5000 double precision," +
                "HZ6300 double precision,HZ8000 double precision,HZ10000 double precision);")

        sql.execute("drop table if exists LW_AERODYNAMICB;")
        sql.execute("create table LW_AERODYNAMICB(PERIOD varchar, IDSOURCE INTEGER NOT NULL," +
                "HZ50 double precision,HZ63 double precision,HZ80 double precision," +
                "HZ100 double precision,HZ125 double precision,HZ160 double precision," +
                "HZ200 double precision,HZ250 double precision,HZ315 double precision," +
                "HZ400 double precision,HZ500 double precision,HZ630 double precision," +
                "HZ800 double precision, HZ1000 double precision, HZ1250 double precision," +
                "HZ1600 double precision,HZ2000 double precision,HZ2500 double precision," +
                "HZ3150 double precision,HZ4000 double precision,HZ5000 double precision," +
                "HZ6300 double precision,HZ8000 double precision,HZ10000 double precision);")

        sql.execute("drop table if exists LW_BRIDGE;")
        sql.execute("create table LW_BRIDGE(PERIOD varchar, IDSOURCE INTEGER NOT NULL," +
                "HZ50 double precision,HZ63 double precision,HZ80 double precision," +
                "HZ100 double precision,HZ125 double precision,HZ160 double precision," +
                "HZ200 double precision,HZ250 double precision,HZ315 double precision," +
                "HZ400 double precision,HZ500 double precision,HZ630 double precision," +
                "HZ800 double precision, HZ1000 double precision, HZ1250 double precision," +
                "HZ1600 double precision,HZ2000 double precision,HZ2500 double precision," +
                "HZ3150 double precision,HZ4000 double precision,HZ5000 double precision," +
                "HZ6300 double precision,HZ8000 double precision,HZ10000 double precision);")

        if (tableFormat.equals("SUMO")){
                // Remplissage des variables avec le contenu du fichier SUMO
            def firstRowProcessed = false
            sql.eachRow('SELECT v.THE_GEOM, v.ID, v.TIMESTEP, v.TYPETRAIN, v.SPEED, r.NTRACK, r.TRACKTRANS, r.RAILROUGHN, r.IMPACTNOIS, r.CURVATURE, r.BRIDGETRAN FROM ' + tableVehicleName + ' v INNER JOIN ' + tableRailTrack + ' r ON v.IDSECTION = r.IDSECTION;') { row ->
                Geometry the_geom = (Geometry) row[0]
                def id_veh = row[1]
                int timestep = (int) row[2]

                String typeTrain = (String) row[3]
                double speed = (double) row[4]
                int[] trackParam = [(int) row[5], (int) row[6],(int) row[7],(int) row[8], (int) row[9], (int) row[10]]

                // Try to convert id_veh to an Integer if it's a String
                if (id_veh instanceof String) {
                    try {
                        id_veh = Integer.parseInt(id_veh) // Convert to Integer
                    } catch (NumberFormatException e) {
                        // If conversion fails, id_veh remains unchanged (still a String)
                    }
                }

                def qryPosition = 'INSERT INTO TRAIN_POSITION (PERIOD , THE_GEOM) VALUES (?,?);'
                sql.withBatch(100, qryPosition) { ps ->
                    ps.addBatch(timestep as String, the_geom as Geometry)}

                if (!firstRowProcessed) {
                    def trainSourceLevel = getTrainsLevel(speed, id_veh, typeTrain, trackParam)
                    String[] typeSources = ['LW_ROLLING','LW_TRACTIONA','LW_TRACTIONB','LW_AERODYNAMICA','LW_AERODYNAMICB','LW_BRIDGE'];
                    if (trainSourceLevel[6].size() > 1) {
                        for (int m = 0; m <= 5; ++m) {
                            int idSource = 0
                            def qry = 'INSERT INTO ' + typeSources[m] + '(PERIOD, IDSOURCE,' +
                                    'HZ50, HZ63, HZ80, HZ100, HZ125, HZ160, HZ200, HZ250,' +
                                    'HZ315, HZ400, HZ500, HZ630, HZ800, HZ1000, HZ1250, HZ1600,' +
                                    'HZ2000, HZ2500, HZ3150, HZ4000, HZ5000, HZ6300, HZ8000, HZ10000)' +
                                    'VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'

                            for (int n = 0; n < trainSourceLevel[6].size(); ++n) {
                                int repetitions = trainSourceLevel[7][n] > 1 ? trainSourceLevel[7][n] : 1
                                for (int k = 0; k < repetitions; ++k) {
                                    idSource += 1
                                    addBatchToSQL(sql, qry, timestep, idSource, trainSourceLevel[m][n])
                                }
                            }
                        }
                    }else{
                        // TODO EDIT FOR SIMPLE TRAIN
                        sql.withBatch(100, qry) { ps ->
                            ps.addBatch(timestep as String, the_geom as Geometry,
                                    trainSourceLevel[0] as Double[], trainSourceLevel[1] as Double[], trainSourceLevel[2] as Double[],
                                    trainSourceLevel[3] as Double[], trainSourceLevel[4] as Double[], trainSourceLevel[5] as Double[])
                        }
                    }
                    firstRowProcessed = true
                }

            }
        }
        else    {
            System.out.println("Unknown File Format")

        }
    }
    void addBatchToSQL(sql, qry, timestep, idSource, trainSourceLevelRow) {
        sql.withBatch(100, qry) { ps ->
            ps.addBatch(timestep as String, idSource as Integer,
                    trainSourceLevelRow[0] as Double, trainSourceLevelRow[1] as Double, trainSourceLevelRow[2] as Double,
                    trainSourceLevelRow[3] as Double, trainSourceLevelRow[4] as Double, trainSourceLevelRow[5] as Double,
                    trainSourceLevelRow[6] as Double, trainSourceLevelRow[7] as Double, trainSourceLevelRow[8] as Double,
                    trainSourceLevelRow[9] as Double, trainSourceLevelRow[10] as Double, trainSourceLevelRow[11] as Double,
                    trainSourceLevelRow[12] as Double, trainSourceLevelRow[13] as Double, trainSourceLevelRow[14] as Double,
                    trainSourceLevelRow[15] as Double, trainSourceLevelRow[16] as Double, trainSourceLevelRow[17] as Double,
                    trainSourceLevelRow[18] as Double, trainSourceLevelRow[19] as Double, trainSourceLevelRow[20] as Double,
                    trainSourceLevelRow[21] as Double, trainSourceLevelRow[22] as Double, trainSourceLevelRow[23] as Double)
        }
    }

    def getTrainsLevel(double speed, def id_veh, String train, int[] trackParam) throws SQLException {

        RailwayCnossosvar railwayCnossos = new RailwayCnossosvar();
        RailWayCnossosParameters  lWRailWay = new RailWayCnossosParameters();

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossos.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2021.json");

        double[] res_LV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        def list = [50, 63,80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000]
        int trackTransfer= trackParam[1]
        int railRoughness= trackParam[2]
        int impactNoise= trackParam[3]
        int bridgeTransfert= trackParam[5]
        int curvature= trackParam[4]
        int nTracks= trackParam[0]

        Map<String, Integer> vehicles = railwayCnossos.getVehicleFromTrainset(train);

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(speed, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, speed, false, nTracks);

        if (vehicles != null) {
            int i = 0;
            String[] typeVehicle = new String[vehicles.size()]
            int[] nVehicle = new int[vehicles.size()]
            double[][] ROLLING = new double[vehicles.size()][24]
            double[][] TRACTIONA = new double[vehicles.size()][24]
            double[][] TRACTIONB = new double[vehicles.size()][24]
            double[][] AERODYNAMICA = new double[vehicles.size()][24]
            double[][] AERODYNAMICB = new double[vehicles.size()][24]
            double[][] BRIDGE = new double[vehicles.size()][24]

            for(Iterator var27 = vehicles.entrySet().iterator(); var27.hasNext(); ++i) {
                Map.Entry<String, Integer> entry = (Map.Entry)var27.next();
                String typeTrain = (String)entry.getKey();
                typeVehicle[i] = typeTrain;
                nVehicle[i] = (int)entry.getValue();
                RailwayVehicleCnossosParametersvar vehicleParameters = new RailwayVehicleCnossosParametersvar(typeTrain, speed, 0, 0);
                lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);
                ROLLING[i] = getSourceLevel("ROLLING", lWRailWay);
                TRACTIONA[i] = getSourceLevel("TRACTIONA", lWRailWay);
                TRACTIONB[i] = getSourceLevel("TRACTIONB", lWRailWay);
                AERODYNAMICA[i] = getSourceLevel("AERODYNAMICA", lWRailWay);
                AERODYNAMICB[i] = getSourceLevel("AERODYNAMICB", lWRailWay);
                BRIDGE[i] = getSourceLevel("BRIDGE", lWRailWay);

            }
            def sourcesTrain = [ROLLING,TRACTIONA,TRACTIONB,AERODYNAMICA,AERODYNAMICB,BRIDGE,typeVehicle,nVehicle];
            return sourcesTrain
        } else if (this.railway.isInVehicleList(train)) {
            RailwayVehicleCnossosParametersvar vehicleParameters = new RailwayVehicleCnossosParametersvar(train, vehicleSpeed, 0, 0);
            lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);
            double[] ROLLING = getSourceLevel("ROLLING", lWRailWay);
            double[] TRACTIONA = getSourceLevel("TRACTIONA", lWRailWay);
            double[] TRACTIONB = getSourceLevel("TRACTIONB", lWRailWay);
            double[] AERODYNAMICA = getSourceLevel("AERODYNAMICA", lWRailWay);
            double[] AERODYNAMICB = getSourceLevel("AERODYNAMICB", lWRailWay);
            double[] BRIDGE = getSourceLevel("BRIDGE", lWRailWay);
            def sourcesTrain = [ROLLING,TRACTIONA,TRACTIONB,AERODYNAMICA,AERODYNAMICB,BRIDGE,train];
            return sourcesTrain
        }
    }
    double[] getSourceLevel(String sourceType,def resultatslWRailWay){
        double[] lWextract =resultatslWRailWay.getRailwaySourceList().get(sourceType).getlW();
        return lWextract
    }


}

