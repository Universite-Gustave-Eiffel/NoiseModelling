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

package org.noise_planet.noisemodelling.wps.DataAssimilation


import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.SpatialResultSet
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossos
import org.noise_planet.noisemodelling.emission.road.cnossos.RoadCnossosParameters
import org.noise_planet.noisemodelling.jdbc.EmissionTableGenerator
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime


title = 'Data Simulation'
description = 'Method to execute a series of operations for generate noise maps'
input = [
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
                type: Sql.class
        ]
]

// todo do only on D, delete E and N
def exec(Connection connection,input) {
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Data simulation ')
    Sql sql = new Sql(connection)
    Integer limit = input['noiseMapLimit'] as Integer

    // Create the ROADS_CONFIG table.
    sql.execute("DROP TABLE IF EXISTS ROADS_CONFIG")
    sql.execute("CREATE TABLE ROADS_CONFIG (" +
            "PK INTEGER," +
            "PERIOD CHARACTER VARYING," +
            "ID_WAY INTEGER," +
            "THE_GEOM GEOMETRY," +
            "TYPE CHARACTER VARYING," +
            "LV_D INTEGER," +
            "HGV_D INTEGER," +
            "LV_SPD_D INTEGER," +
            "HGV_SPD_D INTEGER," +
            "PVMT CHARACTER VARYING(10)," +
            "TEMP_D DOUBLE" +
            ")")

    // Read all combinations from the table .
    List<String[]> allCombinations = new ArrayList<>()

    sql.eachRow("SELECT * FROM ALL_CONFIGURATIONS") { row ->
        allCombinations.add(row.toRowResult().values() as String[])
    }

    // get the similated noise map.
    def iConf
    double primary, secondary, tertiary, others
    int valTemps
    Statement stmt = connection.createStatement()
    stmt.execute("ALTER TABLE ROADS ADD TEMP DOUBLE")

    stmt.execute("DROP TABLE IF EXISTS NOISE_MAPS")
    stmt.execute("CREATE TABLE NOISE_MAPS ( " +
            "PERIOD CHARACTER VARYING, "+
            "TEMP double precision, "+
            "IDRECEIVER integer, " +
            "HZ63 double precision, " +
            "HZ125 double precision, " +
            "HZ250 double precision, " +
            "HZ500 double precision, " +
            "HZ1000 double precision, " +
            "HZ2000 double precision, " +
            "HZ4000 double precision, " +
            "HZ8000 double precision)" )

    stmt.execute("drop table if exists LW_ROADS;")
    stmt.execute("create table LW_ROADS (pk integer, IDSOURCE LONG, PERIOD CHARACTER VARYING, " +
            "HZ63 double precision, HZ125 double precision, HZ250 double precision, HZ500 double precision, HZ1000 double precision, HZ2000 double precision, HZ4000 double precision, HZ8000 double precision);")
    int size
    if (limit != null){
        size = limit
    }
    else{
        size = allCombinations.size()
    }

    try {
        EmissionTableGenerator emissionTableGenerator = new EmissionTableGenerator();


        int pk = 1
        for (int j = 0; j < size; j++) {
            println(LocalDateTime.now())
            int it = j + 1
            String[] combination = allCombinations.get(j)
            iConf = Integer.parseInt(combination[0]).toString()
            primary = Double.parseDouble(combination[1])
            secondary = Double.parseDouble(combination[2])
            tertiary = Double.parseDouble(combination[3])
            others = Double.parseDouble(combination[4])
            valTemps = Integer.parseInt(combination[5])

            stmt.execute("TRUNCATE TABLE ROADS_CONFIG")

            stmt.execute("INSERT INTO ROADS_CONFIG (" +
                    " PK, PERIOD, ID_WAY, THE_GEOM, TYPE, LV_D,  " +
                    " HGV_D, LV_SPD_D, " +
                    " HGV_SPD_D, PVMT, TEMP_D) " +
                    " SELECT  PK , " + iConf +
                    " AS PERIOD, ID_WAY, THE_GEOM, TYPE, " +
                    "    CASE " +
                    "        WHEN TYPE = 'primary' OR TYPE ='primary_link' THEN LV_D * " + primary +
                    "        WHEN TYPE = 'secondary' OR TYPE = 'secondary_link' THEN LV_D * " + secondary +
                    "        WHEN TYPE = 'tertiary' OR TYPE = 'tertiary_link' THEN LV_D * " + tertiary +
                    "        ELSE LV_D * " + others +
                    "    END AS LV_D, " +
                    "    CASE " +
                    "        WHEN TYPE = 'primary' OR TYPE ='primary_link' THEN HGV_D * " + primary +
                    "        WHEN TYPE = 'secondary' OR TYPE = 'secondary_link' THEN HGV_D * " + secondary +
                    "        WHEN TYPE = 'tertiary' THEN HGV_D * " + tertiary +
                    "        ELSE HGV_D * " + others +
                    "    END AS HGV_D, " +
                    " LV_SPD_D AS LV_SPD_D , HGV_SPD_D AS HGV_SPD_D, PVMT, " + valTemps +
                    " AS TEMP_D FROM ROADS")

            // remove THE_GEOM but ad ID_WAY SOURCE_ID

            stmt.execute("drop table if exists ROADS_GEOM;")
            stmt.execute("create table ROADS_GEOM (IDSOURCE LONG PRIMARY KEY, THE_GEOM geometry) as select PK, THE_GEOM FROM ROADS;" )

            def qry = 'INSERT INTO LW_ROADS(pk,IDSOURCE, PERIOD,' +
                    'HZ63, HZ125, HZ250, HZ500, HZ1000,HZ2000, HZ4000, HZ8000) ' +
                    'VALUES (?,?,?,?,?,?,?,?,?,?,?);'

            int k = 0
            st = connection.prepareStatement("SELECT * FROM ROADS_CONFIG" )
            int coefficientVersion = 2
            sql.withBatch( 100, qry) { ps ->

                SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)

                Map<String, Integer> sourceFieldsCache = new HashMap<>()

                while (rs.next()) {
                    k++
                    //logger.info(rs)
                    //Geometry geo = rs.getGeometry()

                    // Compute emission sound level for each road segment


                    double[][] results = emissionTableGenerator.computeLw(rs, coefficientVersion, sourceFieldsCache)
                    def lday = AcousticIndicatorsFunctions.wToDb(results[0])

                    // todo to win some time use this to compute lday (only read road, and not write road_config)
                   /* List<Integer> roadOctaveFrequencyBands = Arrays.asList(AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
                    double[] lvl = new double[roadOctaveFrequencyBands.size()];
                    for (int idFreq = 0; idFreq < roadOctaveFrequencyBands.size(); idFreq++) {
                        int freq = roadOctaveFrequencyBands.get(idFreq);
                        RoadCnossosParameters rsParametersCnossos = new RoadCnossosParameters(lv_speed, mv_speed, hgv_speed, wav_speed,
                                wbv_speed, lvPerHour, mvPerHour, hgvPerHour, wavPerHour, wbvPerHour, freq, temperature,
                                roadSurface, tsStud, pmStud, junctionDistance, junctionType);
                        rsParametersCnossos.setSlopePercentage(slope);
                        rsParametersCnossos.setWay(way);
                        rsParametersCnossos.setFileVersion(coefficientVersion);
                        try {
                            lvl[idFreq] = RoadCnossos.evaluate(rsParametersCnossos);
                        } catch (IOException ex) {
                            throw new SQLException(ex);
                        }
                    }*/
                    // fill the LW_ROADS table

                    ps.addBatch(pk as Integer, rs.getInt("PK")  as Integer, rs.getString("PERIOD") as String,
                            lday[0] as Double, lday[1] as Double, lday[2] as Double,
                            lday[3] as Double, lday[4] as Double, lday[5] as Double,
                            lday[6] as Double, lday[7] as Double)
                    pk++
                }
            }

            // Add primary key to the road table

         /*   sql.execute("INSERT INTO NOISE_MAPS (PERIOD,TEMP,IDRECEIVER , HZ63 , HZ125 , HZ250 , HZ500 , HZ1000 , HZ2000 , HZ4000 , HZ8000)" +
                    "SELECT "+it+","+ valTemps +", lg.IDRECEIVER, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD63 + lg.HZ63) / 10))) AS HZ63, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD125 + lg.HZ125) / 10))) AS HZ125, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD250 + lg.HZ250) / 10))) AS HZ250, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD500 + lg.HZ500) / 10))) AS HZ500, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD1000 + lg.HZ1000) / 10))) AS HZ1000, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD2000 + lg.HZ2000) / 10))) AS HZ2000, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD4000 + lg.HZ4000) / 10))) AS HZ4000, " +
                    "10 * LOG10( SUM(POWER(10,(mr.HZD8000 + lg.HZ8000) / 10))) AS HZ8000 " +
                    "FROM RECEIVERS_LEVEL lg " +
                    "INNER JOIN LW_ROADS mr ON lg.IDSOURCE = mr.PK " +
                    "GROUP BY lg.IDRECEIVER")
*/
            println(LocalDateTime.now())
        }
        sql.execute("ALTER TABLE LW_ROADS ALTER COLUMN PK INT NOT NULL;")
        sql.execute("ALTER TABLE LW_ROADS ADD PRIMARY KEY (PK);  ")
        sql.execute("CREATE INDEX ON LW_ROADS(IDSOURCE, PERIOD);")
    } catch (SQLException e) {
        e.printStackTrace()
    }

    logger.info('End data simulation ')

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
/**
 * Executes the assimilation process for a list of combinations, updating the database
 * with road configurations and calculating noise levels.
 *
 * @param allCombinations A list of possible parameter combinations, where each entry contains:
 *  [iteration ID, primary factor, secondary factor, tertiary factor, others factor, temperature].
 * @param connection The database connection used for executing queries.
 * @param limit the number of maps to be generated
 * @throws SQLException If a database access error occurs.
 */
def assimilationProcess(List<String[]> allCombinations, Connection connection,Integer limit) {


}
