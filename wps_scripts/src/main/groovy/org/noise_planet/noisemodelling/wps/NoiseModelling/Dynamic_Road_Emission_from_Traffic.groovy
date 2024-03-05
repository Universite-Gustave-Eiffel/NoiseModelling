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
 */

package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceDynamic
import org.noise_planet.noisemodelling.emission.RoadSourceParametersDynamic

import java.sql.Connection
import java.sql.SQLException

title = 'Dynamic Road Emission from traffic'
description = 'Compute dynamic road emission from average traffic data as describe in <b>Aumond, P., Jacquesson, L., & Can, A. (2018). Probabilistic modeling framework for multisource sound mapping. Applied Acoustics, 139, 34-43. </b>.' +
        '</br>The user can indicate the number of iterations he wants the model to calculate.' +
        '</br> </br> <b> The output table is called : LW_DYNAMIC </b> ' +
        'and contain : </br>' +
        '-  <b> TIMESTAMP  </b> : The TIMESTAMP iteration (STRING).</br>' +
        '-  <b> IDRECEIVER  </b> : an identifier (INTEGER, PRIMARY KEY). </br>' +
        '- <b> THE_GEOM </b> : the 3D geometry of the receivers (POINT). </br> ' +
        '-  <b> LWD63, LWD125, LWD250, LWD500, LWD1000,LWD2000, LWD4000, LWD8000 </b> : 8 columns giving the day emission sound level for each octave band (FLOAT).'

inputs = [
        tableRoads    : [name                                                                                 : 'Roads table name', title: 'Roads table name', description: "<b>Name of the Roads table.</b>  </br>  " +
                "<br>  The table shall contain : </br>" +
                "- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)<br/>" +
                "- <b> TV_D </b> : Hourly average light and heavy vehicle count (DOUBLE)<br/>" +
                "- <b> HV_D </b> :  Hourly average heavy vehicle count (DOUBLE)<br/>" +
                "- <b> LV_SPD_D </b> :  Hourly average light vehicle speed (DOUBLE)<br/>" +
                "- <b> HV_SPD_D </b> :  Hourly average heavy vehicle speed  (DOUBLE)<br/>" +
                "- <b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05) (VARCHAR)" +
                "</br> </br> <b> This table can be generated from the WPS Block 'Import_OSM'. </b>.", type: String.class],

        method : [name : 'Method',
                  title : "method",
                  description : "method",
                  type: String.class],

        timestep : [name : 'timestep',
                    title : "timestep",
                    description : "timestep in sec.",
                    type: Integer.class],

        gridStep : [name : 'gridStep',
                    title : "gridStep",
                    description : "gridStep in meters.",
                    type: Integer.class],

        duration       : [name       : 'duration', title: 'duration in sec.',
                             description: 'Number of the iterations to compute (INTEGER). </br> </br> <b> Default value : 100 </b>',
                             type: Integer.class],
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
def exec(Connection connection, input) {

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Open sql connection to communicate with the database
    Sql sql = new Sql(connection)

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Traffic Probabilistic Modelling')
    def start = new Date()

    // -------------------
    // Get every inputs
    // -------------------

    int duration = 300
    if (input['duration']) {
        duration = Integer.valueOf(input['duration'] as String)
    }

    int timestep = 10
    if (input['timestep']) {
        timestep = Integer.valueOf(input['timestep'] as String)
    }

    int gridStep = 10
    if (input['gridStep']) {
        gridStep = Integer.valueOf(input['gridStep'] as String)
    }

    int nIterations = (int) Math.round(duration/timestep);

    String method = "PROBA"
    if (input['method']) {
        method = input['method'] as String
    }

    String sources_table_name = input['tableRoads']
    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()
    // Check if srid are in metric projection.
    int sridSources = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
    if (sridSources == 3785 || sridSources == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+sources_table_name+".")
    if (sridSources == 0) throw new IllegalArgumentException("Error : The table "+sources_table_name+" does not have an associated SRID.")

    System.out.println('Start  time : ' + TimeCategory.minus(new Date(), start))

    if (method == "PROBA"){
        System.println("Create the random road traffic table over the number of iterations... ")

        sql.execute("drop table TRAFIC_DENSITY IF EXISTS;" +
                "create table TRAFIC_DENSITY AS SELECT *,case when LV_SPD_D  < 20 then 0.001*LV_D/20 else 0.001*LV_D/LV_SPD_D end  LV_DENS_D, case when HGV_SPD_D  < 20 then 0.001*HGV_D/20 else 0.001*HGV_D/HGV_SPD_D end HGV_DENS_D  FROM "+sources_table_name+" ;" +
                "alter table TRAFIC_DENSITY add LENGTH double as select ST_LENGTH(the_geom) ;" +
                "ALTER TABLE TRAFIC_DENSITY ALTER COLUMN LV_DENS_D double;" +
                "ALTER TABLE TRAFIC_DENSITY ALTER COLUMN HGV_DENS_D double;" +

                "drop table ROAD_POINTS if exists;" +
                "create table ROAD_POINTS as SELECT ST_Tomultipoint(ST_Densify(the_geom, "+gridStep+")) points_geom , * from ST_Explode('TRAFIC_DENSITY');" +
                "ALTER TABLE ROAD_POINTS DROP COLUMN the_geom;" +
                "ALTER TABLE ROAD_POINTS RENAME COLUMN points_geom TO the_geom;" +
                "ALTER TABLE ROAD_POINTS DROP COLUMN EXPLOD_ID, PK;" +

                "drop table VEHICLES if exists;" +
                " create table VEHICLES as SELECT ST_AddZ(ST_FORCE3D(the_geom),0.05) geom_3D,* from ST_Explode('ROAD_POINTS');" +
                "ALTER TABLE VEHICLES DROP COLUMN the_geom;" +
                " ALTER TABLE VEHICLES RENAME COLUMN geom_3D TO the_geom;" +
                "alter table VEHICLES add column PK serial ;" +
                "ALTER TABLE VEHICLES DROP COLUMN EXPLOD_ID;")

        IndividualVehicleEmissionProcessData probabilisticProcessData = new IndividualVehicleEmissionProcessData();
        probabilisticProcessData.setDynamicEmissionTable("VEHICLES", sql)


        // random number > Vehicle or not / Light of Heavy
        //
        sql.execute("drop table if exists LW_DYNAMIC;")
        sql.execute("create table LW_DYNAMIC(IT integer, PK integer, Hz63 double precision, Hz125 double precision, Hz250 double precision, Hz500 double precision, Hz1000 double precision, Hz2000 double precision, Hz4000 double precision, Hz8000 double precision);")
        def qry = 'INSERT INTO LW_DYNAMIC(IT , PK,Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000) VALUES (?,?,?,?,?,?,?,?,?,?);'

        int nCarsPos = probabilisticProcessData.getCarsPositions()
        k = 0
        for (int it = 1; it < nIterations; it++) {
            Map<Integer, double[]> sourceLev = new HashMap<>()
            for (int iCar = 1; iCar < nCarsPos; iCar++) {

                // get source SoundLevel
                if (!sourceLev.containsKey(iCar)) {
                    double[] carsLevel = probabilisticProcessData.getCarsLevel(iCar)

                    if (carsLevel.sum()!= 0) sourceLev.put(iCar, carsLevel)
                }
            }
            sql.withBatch(100, qry) { ps ->
                for (Map.Entry<Integer, double[]> s : sourceLev.entrySet()) {
                    int time = it*timestep
                    ps.addBatch(time as Integer, s.key as Integer,
                            s.value[0] as Double, s.value[1] as Double, s.value[2] as Double,
                            s.value[3] as Double, s.value[4] as Double, s.value[5] as Double,
                            s.value[6] as Double, s.value[7] as Double)

                }
            }
        }






        // Drop table LDEN_GEOM if exists
        sql.execute("drop table if exists LW_DYNAMIC_GEOM;")
        // Associate Geometry column to the table LDEN
        sql.execute("CREATE INDEX ON LW_DYNAMIC(PK);")
        sql.execute("CREATE INDEX ON VEHICLES(PK);")
        sql.execute("create table LW_DYNAMIC_GEOM  as select a.IT T,a.PK, b.THE_GEOM, a.Hz63, a.Hz125, a.Hz250, a.Hz500, a.Hz1000, a.Hz2000, a.Hz4000, a.Hz8000  FROM LW_DYNAMIC a LEFT JOIN  VEHICLES b  ON a.PK = b.PK;")

    }


    sql.execute("drop table LW_DYNAMIC if exists;")

    System.out.println('Intermediate  time : ' + TimeCategory.minus(new Date(), start))
    System.out.println("Export data to table")


    resultString = "Calculation Done ! The table LW_DYNAMIC_GEOM has been created."

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Traffic Probabilistic Modelling')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString
}


/**
 *
 */
class IndividualVehicleEmissionProcessData {

    Map<Integer, Double> SPEED_LV = new HashMap<>()
    Map<Integer, Double> SPEED_HV = new HashMap<>()
    Map<Integer, Double> LV = new HashMap<>()
    Map<Integer, Double> HV = new HashMap<>()
    int nCars = 0

    double[] getCarsLevel(int idSource) throws SQLException {
        double[] res_d = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        double[] res_LV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        double[] res_HV = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
        def list = [63, 125, 250, 500, 1000, 2000, 4000, 8000]
        // memes valeurs d e et n


        def random = Math.random()
        if (random < LV.get(idSource)) {
            int kk = 0
            for (f in list) {

                double speed = SPEED_LV.get(idSource)
                int acc = 0
                int FreqParam = f
                double Temperature = 20
                String RoadSurface = "DEF"
                boolean Stud = true
                double Junc_dist = 200
                int Junc_type = 1
                String veh_type = "1"
                int acc_type = 1
                double LwStd = 1
                int VehId = 10

                RoadSourceParametersDynamic rsParameters = new RoadSourceParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId)
                rsParameters.setSlopePercentage(0)

                res_LV[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
                kk++
            }

        }
        random = Math.random()
        if (random < HV.get(idSource)) {
            int kk = 0
            for (f in list) {
                double speed = SPEED_HV.get(idSource)
                int acc = 0
                int FreqParam = f
                double Temperature = 20
                String RoadSurface = "DEF"
                boolean Stud = true
                double Junc_dist = 200
                int Junc_type = 1
                String veh_type = "3"
                int acc_type = 1
                double LwStd = 1
                int VehId = 10

                RoadSourceParametersDynamic rsParameters = new RoadSourceParametersDynamic(speed, acc, veh_type, acc_type, FreqParam, Temperature, RoadSurface, Stud, Junc_dist, Junc_type, LwStd, VehId)
                rsParameters.setSlopePercentage(0)

                res_HV[kk] = EvaluateRoadSourceDynamic.evaluate(rsParameters)
                kk++
            }
        }
        int kk = 0
        for (f in list) {
            res_d[kk] = 10 * Math.log10(
                    (1.0 / 2.0) *
                            (Math.pow(10, (10 * Math.log10(Math.pow(10, res_LV[kk] / 10))) / 10)
                                    + Math.pow(10, (10 * Math.log10(Math.pow(10, res_HV[kk] / 10))) / 10)
                            )
            )
            kk++
        }


        return res_d
    }

    int getCarsPositions(){
        nCars = SPEED_LV.size()
        return nCars
    }
    void setDynamicEmissionTable(String tablename, Sql sql) {
        //////////////////////
        // Import file text
        //////////////////////

        // Remplissage des variables avec le contenu du fichier plan d'exp
        sql.eachRow('SELECT PK,  LV_SPD_D, LV_DENS_D,  HGV_SPD_D, HGV_DENS_D FROM ' + tablename + ';') { row ->
            int pk = (int) row[0]
            SPEED_LV.put(pk, (double) row[1])
            LV.put(pk, (double) row[2])
            SPEED_HV.put(pk, (double) row[3])
            HV.put(pk, (double) row[4])

        }


    }

}