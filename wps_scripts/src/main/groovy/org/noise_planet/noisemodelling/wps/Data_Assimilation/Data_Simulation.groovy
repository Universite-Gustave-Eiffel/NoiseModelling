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

package org.noise_planet.noisemodelling.wps.Data_Assimilation

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossos
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossosParameters
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

title = 'Data Simulation'
description = 'Method to execute a series of operations for generate noise maps'

inputs = [
        noiseMapLimit: [
                name: 'Number of map ',
                title: 'Number of map',
                description: 'The optional parameter limits the number of maps to be generated',
                type: Integer.class
        ]
]

outputs = [
        result: [
                name: 'Noise map table ',
                title: 'Noise map table',
                description: 'NOISE_MAPS table input',
                type: String.class
        ]
]

// Executes the assimilation process for a list of combinations with road configurations and calculating noise levels.
def exec(Connection connection,input) {
    connection = new ConnectionWrapper(connection)
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Data simulation ')
    Sql sql = new Sql(connection)
    Integer limit = null
    if (input!=[]) limit = input['noiseMapLimit'] as Integer // limit the number of maps to be generated

    //  A list of possible parameter combinations, where each entry contains:
    // [iteration ID, primary factor, secondary factor, tertiary factor, others factor, temperature].
    List<String[]> allCombinations = new ArrayList<>()

    // Read all combinations from the table .
    sql.eachRow("SELECT * FROM ALL_CONFIGURATIONS") { row ->
        allCombinations.add(row.toRowResult().values() as String[])
    }

    // get the simulated noise map.
    double primary, secondary, tertiary, others
    double valTemps

    sql.execute("drop table if exists ROADS_GEOM;")
    sql.execute("create table ROADS_GEOM (IDSOURCE integer PRIMARY KEY, THE_GEOM geometry) as select PK, THE_GEOM FROM ROADS;" )

    sql.execute("drop table if exists LW_ROADS;")
    sql.execute("create table LW_ROADS (pk integer PRIMARY KEY, IDSOURCE INTEGER, PERIOD CHARACTER VARYING, " +
            "HZ63 double precision, HZ125 double precision, HZ250 double precision, HZ500 double precision, HZ1000 double precision, HZ2000 double precision, HZ4000 double precision, HZ8000 double precision);")
    int size
    if (limit != null){
        size = limit
    }
    else{
        size = allCombinations.size()
    }


    // Total combinations
    int totalCombinations = allCombinations.size()

    // How many samples you want
    int sampleCount = size

    // Sanity check
    if (sampleCount > totalCombinations) {
        throw new IllegalArgumentException("Sample count exceeds total combinations")
    }
    // Generate LHS indices
    List<Integer> lhsIndices = (0..<sampleCount).collect { i ->
        int binSize = (int) (totalCombinations / sampleCount)
        int start = i * binSize
        int end = Math.min((i + 1) * binSize - 1, totalCombinations - 1)
        start + (Math.random() * (end - start + 1)) as int
    }

    // Convert list to comma-separated string without brackets
    String idList = lhsIndices.join(", ")

// Create filtered table
    sql.execute("DROP TABLE IF EXISTS FILTERED_CONFIGURATIONS")
    sql.execute("CREATE TABLE FILTERED_CONFIGURATIONS AS SELECT * FROM ALL_CONFIGURATIONS WHERE IT IN ("+ idList +" )")


    try {

        int pk = 1
        for (int j = 0; j < size; j++) {
            String[] combination = allCombinations.get( lhsIndices[j])
            primary = Double.parseDouble(combination[1])
            secondary = Double.parseDouble(combination[2])
            tertiary = Double.parseDouble(combination[3])
            others = Double.parseDouble(combination[4])
            valTemps = Double.parseDouble(combination[5])

            String qry = "INSERT INTO LW_ROADS(pk,IDSOURCE, PERIOD, HZ63, HZ125, HZ250, HZ500, HZ1000,HZ2000, HZ4000, HZ8000) VALUES (?,?,?,?,?,?,?,?,?,?,?);"

            PreparedStatement st = connection.prepareStatement("SELECT * FROM ROADS")
            int coefficientVersion = 2
            sql.withBatch( 100, qry) { ps ->

                SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)

                double lvPerHour
                double hgvPerHour
                while (rs.next()) {
                    String type = rs.getString("TYPE")
                    if (type == "primary" || type =="primary_link" ){
                        lvPerHour = rs.getDouble("LV_D") * primary
                        hgvPerHour = rs.getDouble("HGV_D") * primary
                    }
                    else if (type == "secondary" || type =="secondary_link" ){
                        lvPerHour = rs.getDouble("LV_D") * secondary
                        hgvPerHour = rs.getDouble("HGV_D") * secondary
                    }
                    else if (type == "tertiary"){
                        lvPerHour = rs.getDouble("LV_D") * tertiary
                        hgvPerHour = rs.getDouble("HGV_D") * tertiary
                    }
                    else {
                        lvPerHour = rs.getDouble("LV_D") * others
                        hgvPerHour = rs.getDouble("HGV_D") * others
                    }

                    double lv_speed = rs.getDouble("LV_SPD_D")
                    double hgv_speed = rs.getDouble("HGV_SPD_D")
                    double junctionDistance = 100; // Distance to junction
                    int junctionType =2 ; // Junction type (k=1 traffic lights, k=2 roundabout)
                    double mvPerHour = 1
                    double wavPerHour = 1
                    double wbvPerHour = 1
                    String roadSurface = rs.getString("PVMT")
                    double tsStud = 1
                    double pmStud = 2
                    double mv_speed = 20
                    double wav_speed = 20
                    double wbv_speed = 20

                    // Compute emission sound level for each road segment

                    List<Integer> roadOctaveFrequencyBands = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
                    double[] lday = new double[roadOctaveFrequencyBands.size()]
                    for (int idFreq = 0; idFreq < roadOctaveFrequencyBands.size(); idFreq++) {
                        int freq = roadOctaveFrequencyBands.get(idFreq)
                        RoadCnossosParameters rsParametersCnossos = new RoadCnossosParameters(lv_speed, mv_speed, hgv_speed, wav_speed,
                                wbv_speed, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, valTemps,
                                roadSurface, tsStud, pmStud, junctionDistance, junctionType)
                        rsParametersCnossos.setSlopePercentage(1)
                        rsParametersCnossos.setWay(3)
                        rsParametersCnossos.setFileVersion(coefficientVersion)
                        try {
                            lday[idFreq] = RoadCnossos.evaluate(rsParametersCnossos)

                        } catch (IOException ex) {
                            throw new SQLException(ex)
                        }
                    }

                    // fill the LW_ROADS table
                    ps.addBatch(pk, rs.getInt("PK")  as Integer, lhsIndices[j] as String,
                            lday[0] as Double, lday[1] as Double, lday[2] as Double,
                            lday[3] as Double, lday[4] as Double, lday[5] as Double,
                            lday[6] as Double, lday[7] as Double)
                    pk++
                }
            }
            if (j%10 ==0){
                println('Generate LW maps for '+ size +' configurations : (%): '+ 100 * (j/size))
            }
        }
        sql.execute("CREATE INDEX ON LW_ROADS(IDSOURCE, PERIOD);")
    } catch (SQLException e) {
        e.printStackTrace()
    }

    logger.info('End data simulation ')
    return "Calculation Done ! The table LW_ROADS has been created."

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
            return [result: exec(connection,input)]
    }
}

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}
