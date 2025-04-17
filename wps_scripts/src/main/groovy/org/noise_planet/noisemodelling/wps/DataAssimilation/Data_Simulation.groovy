package org.noise_planet.noisemodelling.wps.DataAssimilation


import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
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
                description: 'The optional parameter limits the number of maps to be generated, to avoid JAVA errors like "Out-Of-Memory" ',
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


def exec(Connection connection,input) {
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Data simulation ')
    Sql sql = new Sql(connection)
    Integer limit = input['noiseMapLimit'] as Integer

    // Create the ROADS_CONFIG table.
    sql.execute("CREATE TABLE ROADS_CONFIG (" +
            "IT INTEGER," +
            "PK serial PRIMARY KEY," +
            "ID_WAY INTEGER," +
            "THE_GEOM GEOMETRY," +
            "TYPE CHARACTER VARYING," +
            "LV_D INTEGER," +
            "LV_E INTEGER," +
            "LV_N INTEGER," +
            "HGV_D INTEGER," +
            "HGV_E INTEGER," +
            "HGV_N INTEGER," +
            "LV_SPD_D INTEGER," +
            "LV_SPD_E INTEGER," +
            "LV_SPD_N INTEGER," +
            "HGV_SPD_D INTEGER," +
            "HGV_SPD_E INTEGER," +
            "HGV_SPD_N INTEGER," +
            "PVMT CHARACTER VARYING(10)," +
            "TEMP DOUBLE" +
            ")")

    // Read all combinations from the table .
    List<String[]> allCombinations = new ArrayList<>()

    sql.eachRow("SELECT * FROM ALL_CONFIGURATIONS") { row ->
        allCombinations.add(row.toRowResult().values() as String[])
    }

    // get the similated noise map.
    assimilationProcess(allCombinations, connection,limit)


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

    int i
    double primary, secondary, tertiary, others
    int valTemps
    Statement stmt = connection.createStatement()
    String nameTableA = "NOISE_MAPS"
    stmt.execute("ALTER TABLE ROADS ADD TEMP DOUBLE")


    stmt.execute("CREATE TABLE  "+ nameTableA+" ( " +
            "IT INTEGER, "+
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
    int size
    if (limit != null){
        size = limit
    }
    else{
        size = allCombinations.size()
    }

    try {
        for (int j = 0; j < size; j++) {
            println(LocalDateTime.now())
            int it = j + 1
            String[] combination = allCombinations.get(j)
            i = Integer.parseInt(combination[0])
            primary = Double.parseDouble(combination[1])
            secondary = Double.parseDouble(combination[2])
            tertiary = Double.parseDouble(combination[3])
            others = Double.parseDouble(combination[4])
            valTemps = Integer.parseInt(combination[5])

            stmt.execute("TRUNCATE TABLE ROADS_CONFIG")
            stmt.execute("ALTER TABLE ROADS_CONFIG ALTER COLUMN PK RESTART WITH 1")

            stmt.execute("INSERT INTO ROADS_CONFIG (" +
                    " IT, ID_WAY, THE_GEOM, TYPE, LV_D, LV_E, LV_N, " +
                    " HGV_D, HGV_E, HGV_N, LV_SPD_D, LV_SPD_E, LV_SPD_N, " +
                    " HGV_SPD_D, HGV_SPD_E, HGV_SPD_N, PVMT, TEMP) " +
                    " SELECT " + i +
                    " , ID_WAY, THE_GEOM, TYPE, " +
                    "    CASE " +
                    "        WHEN TYPE = 'primary' OR TYPE ='primary_link' THEN LV_D * " + primary +
                    "        WHEN TYPE = 'secondary' OR TYPE = 'secondary_link' THEN LV_D * " + secondary +
                    "        WHEN TYPE = 'tertiary' OR TYPE = 'tertiary_link' THEN LV_D * " + tertiary +
                    "        ELSE LV_D * " + others +
                    "    END AS LV_D, " +
                    " LV_E, LV_N, " +
                    "    CASE " +
                    "        WHEN TYPE = 'primary' OR TYPE ='primary_link' THEN HGV_D * " + primary +
                    "        WHEN TYPE = 'secondary' OR TYPE = 'secondary_link' THEN HGV_D * " + secondary +
                    "        WHEN TYPE = 'tertiary' THEN HGV_D * " + tertiary +
                    "        ELSE HGV_D * " + others +
                    "    END AS HGV_D, " +
                    " HGV_E, HGV_N, LV_SPD_D, LV_SPD_E, LV_SPD_N, HGV_SPD_D, HGV_SPD_E, HGV_SPD_N, PVMT, " + valTemps +
                    " FROM ROADS")

            new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS_CONFIG"])

            stmt.execute("INSERT INTO "+nameTableA+" (IT,TEMP,IDRECEIVER , HZ63 , HZ125 , HZ250 , HZ500 , HZ1000 , HZ2000 , HZ4000 , HZ8000)" +
                    "SELECT "+it+","+ valTemps +", lg.IDRECEIVER, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD63 + lg.HZ63) / 10))) AS HZ63, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD125 + lg.HZ125) / 10))) AS HZ125, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD250 + lg.HZ250) / 10))) AS HZ250, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD500 + lg.HZ500) / 10))) AS HZ500, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD1000 + lg.HZ1000) / 10))) AS HZ1000, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD2000 + lg.HZ2000) / 10))) AS HZ2000, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD4000 + lg.HZ4000) / 10))) AS HZ4000, " +
                    "10 * LOG10( SUM(POWER(10,(mr.LWD8000 + lg.HZ8000) / 10))) AS HZ8000 " +
                    "FROM LDAY_GEOM lg " +
                    "INNER JOIN LW_ROADS mr ON lg.IDSOURCE = mr.PK " +
                    "GROUP BY lg.IDRECEIVER")

            println(LocalDateTime.now())
        }
    } catch (SQLException e) {
        e.printStackTrace()
    }
}
