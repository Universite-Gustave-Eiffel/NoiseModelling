package org.noise_planet.noisemodelling.wps.Dynamic

import com.opencsv.CSVReader
import groovy.sql.Sql
import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Add_Laeq_Leq_columns
import org.noise_planet.noisemodelling.wps.Database_Manager.Table_Visualization_Data
import org.noise_planet.noisemodelling.wps.Experimental.DiffAB
import org.noise_planet.noisemodelling.wps.Experimental.Noise_Map_Sum
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

class DataAssimilation {

    /**
     * Main method to execute a series of operations for processing observation data,
     * importing OSM data, calculating road emissions and generating noise maps with the aim of extracting the best configruation.
     *
     * @param args Command line arguments (not used in this method).
     * */
     static void main(String[] args){
         // Retrieve all configurations.
         getAllConfig()
         // Extract observation data from the input CSV file 24_hour.csv and save it to a new CSV file.
         extractObservationData("./wps_script/24_hour.csv", "./wps_script/observationSensor.csv")


         // Establish a connection to the spatial database.
         Connection connection = JDBCUtilities.wrapConnection(
                 H2GISDBFactory.createSpatialDataBase("mem:assimilationDatabase", true)
         )
         // Initialize SQL execution object.
         Sql sql = new Sql(connection)

         Object res

         // Import OSM data into the database.
         res = new Import_OSM().exec(connection, [
                 "pathFile": "./wps_scripts/geneve.osm.pbf",
                 "targetSRID": 2056,
                 "ignoreGround": true,
                 "ignoreBuilding": false,
                 "ignoreRoads": false,
                 "removeTunnels": true
         ])

         // Add a temperature: TEMP column to the ROADS table.
         sql.execute("ALTER TABLE ROADS ADD TEMP DOUBLE")


         // Calculate road emissions from traffic data.
         res = new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS"])

         // Import the observation sensor data into the database.
         new Import_File().exec(connection, [
                 "pathFile": "./wps_scripts/observationSensor.csv",
                 "tableName": "OBSERVATION"
         ])


         // Change the type "text from the csv file" to INTEGER or FLOAT
         sql.execute("ALTER TABLE OBSERVATION " +
                 " ALTER COLUMN T SET DATA TYPE  INTEGER")

         sql.execute("ALTER TABLE OBSERVATION " +
                 " ALTER COLUMN IDRECEIVER SET DATA TYPE  INTEGER")

         sql.execute("ALTER TABLE OBSERVATION " +
                 " ALTER COLUMN LEQA SET DATA TYPE  FLOAT")

         // because thes sensors always measure 40 db: ID 17, 12 AND 29
         sql.execute("DELETE FROM OBSERVATION "+
                 "WHERE IDRECEIVER = 17 or IDRECEIVER = 12 or IDRECEIVER = 29;")

         // Create the RECEIVERS table with unique sensor data from OBSERVATION table.
         sql.execute("DROP TABLE IF EXISTS RECEIVERS")
         sql.execute("CREATE TABLE RECEIVERS(IDRECEIVER INTEGER PRIMARY KEY, THE_GEOM GEOMETRY)")
         sql.execute("INSERT INTO RECEIVERS (IDRECEIVER, THE_GEOM) SELECT DISTINCT IDRECEIVER, ST_GeomFromText(ST_AsText(THE_GEOM), 2056) FROM OBSERVATION")


         // Create and initialize the LW_ROADS_0DB table.
         sql.execute("DROP TABLE IF EXISTS LW_ROADS_0DB")
         sql.execute("CREATE TABLE LW_ROADS_0DB(PK integer NOT NULL PRIMARY KEY, THE_GEOM geometry, LWD63 real, LWD125 real, LWD250 real, LWD500 real, LWD1000 real, LWD2000 real, LWD4000 real, LWD8000 real) AS SELECT r.PK, r.THE_GEOM, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 FROM LW_ROADS AS r")

         // Calculate noise levels from road sources with a maximum distance of 500 metres from the sources .
         res = new Noise_level_from_source().exec(connection, [
                 "tableSources": "LW_ROADS_0DB",
                 "tableBuilding": "BUILDINGS",
                 "tableReceivers": "RECEIVERS",
                 "confExportSourceId": true,
                 "confMaxSrcDist": 500,
                 "confDiffVertical": true,
                 "confDiffHorizontal": true,
                 "confSkipLevening": true,
                 "confSkipLnight": true,
                 "confSkipLden": true
         ])

         // Visualize the LDAY_GEOM table.
         new Table_Visualization_Data().exec(connection, ["tableName": "LDAY_GEOM"])


         // Summarize noise map data.
         res = new Noise_Map_Sum().exec(connection, [
                 "attenuation": "LDAY_GEOM",
                 "source": "LW_ROADS",
                 "outTable": "TABLE_A_0"
         ])

         // Visualize the TABLE_A_0 table.
         new Table_Visualization_Data().exec(connection, ["tableName": "TABLE_A_0"])

         // Add LEQA columns to the TABLE_A_0 table.
         new Add_Laeq_Leq_columns().exec(connection, [
                 "prefix": "HZ",
                 "tableName": "TABLE_A_0"
         ]);

         // Calculate the difference between observation and simulated data.
         res = new DiffAB().exec(connection, [
                 "mainMapTable": "OBSERVATION",
                 "secondMapTable": "TABLE_A_0",
                 "outTable": "SCENARIO_0"
         ]);

         // Visualize the SCENARIO_0 table.
         new Table_Visualization_Data().exec(connection, ["tableName": "SCENARIO_0"])

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

         // Read all combinations from the CSV file.
         List<String[]> allCombinations = new ArrayList<>();
         CSVReader reader = new CSVReader(new FileReader("./wps_scripts/ALL_COMBI.csv"))
         reader.readNext()
         String[] row
         while ((row = reader.readNext()) != null) {
             allCombinations.add(row)
         }
         // get the similated noise table TABLE_A.
         assimilationProcess(allCombinations, connection)

         // Add LEQA columns to the TABLE_A table.
         new Add_Laeq_Leq_columns().exec(connection, [
                 "prefix": "HZ",
                 "tableName": "TABLE_A"
         ])

         // Export the TABLE_A table to a CSV file.
         new Export_Table().exec(connection, [
                 "exportPath": "./target/TABLE_A.csv",
                 "tableToExport": "TABLE_A"
         ])


         /*new Import_File().exec(connection, [
                 pathFile : "./TABLE_A.csv",
                 tableName : "TABLE_A"])
         sql.execute("ALTER TABLE TABLE_A " +
                 " ALTER COLUMN LEQA SET DATA TYPE  FLOAT")*/


         sql.execute("CREATE TABLE file1_cleaned AS " +
                 "SELECT " +
                 "    IDRECEIVER AS ID_sensor, " +
                 "    T, " +
                 "    LEQA " +
                 "FROM OBSERVATION; \n" )
         sql.execute("CREATE TABLE file2_cleaned AS \n" +
                 "SELECT \n" +
                 "    IDRECEIVER AS ID_sensor, \n" +
                 "    IT, \n" +
                 "    LEQA\n" +
                 "FROM TABLE_A; \n" )
         sql.execute("CREATE TABLE joined_data AS \n" +
                 "SELECT \n" +
                 "    f1.ID_sensor, \n" +
                 "    f1.T, \n" +
                 "    f2.IT, \n" +
                 "    f1.LEQA AS LEQA_file1, \n" +
                 "    f2.LEQA AS LEQA_file2\n" +
                 "FROM file1_cleaned f1\n" +
                 "INNER JOIN file2_cleaned f2 \n" +
                 "    ON f1.ID_sensor = f2.ID_sensor;\n")

         sql.execute("CREATE TABLE agg_data AS \n" +
                 "SELECT \n" +
                 "    T, \n" +
                 "    IT, \n" +
                 "    MEDIAN(ABS(LEQA_file1 - LEQA_file2)) AS median_abs_diff, \n" +
                 "    MEDIAN(LEQA_file1) AS value_file1,\n" +
                 "    MEDIAN(LEQA_file2) AS value_file2,\n" +
                 "    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY LEQA_file1) AS file1_lower,\n" +
                 "    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY LEQA_file1) AS file1_upper,\n" +
                 "    PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY LEQA_file2) AS file2_lower,\n" +
                 "    PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY LEQA_file2) AS file2_upper\n" +
                 "FROM joined_data\n" +
                 "GROUP BY T, IT;\n" )

         sql.execute("CREATE TABLE best_IT AS \n" +
                 "SELECT \n" +
                 "    T,\n" +
                 "    IT,\n" +
                 "    median_abs_diff,\n" +
                 "    value_file1,\n" +
                 "    value_file2,\n" +
                 "    file1_lower,\n" +
                 "    file1_upper,\n" +
                 "    file2_lower,\n" +
                 "    file2_upper\n" +
                 "FROM agg_data\n" +
                 "WHERE (T, median_abs_diff) IN (\n" +
                 "    SELECT \n" +
                 "        T, \n" +
                 "        MIN(median_abs_diff)\n" +
                 "    FROM agg_data\n" +
                 "    GROUP BY T\n" +
                 ");")

          sql.execute("CREATE TABLE BEST_CONFIGURATION AS SELECT DISTINCT T,IT, ROUND(median_abs_diff,2) AS LEQA_DIFF FROM BEST_IT;\n")

         // Import the ALL_CONFIG table from the CSV file.
         new Import_File().exec(connection, [
                 "pathFile": "./wps_scripts/ALL_COMBI.csv",
                 "tableName": "ALL_CONFIG"
         ])

         // Change the type "text from the csv file" to integer
         sql.execute("ALTER TABLE ALl_CONFIG " +
                 " ALTER COLUMN IT SET DATA TYPE  INTEGER")

         // Create the BEST_CONFIG table to store the best configurations with adding the corresponding combination.
         sql.execute("CREATE TABLE BEST_CONFIG AS " +
                 "SELECT b.T, b.IT, b.LEQA_DIFF, a.PRIMARY_VAL, a.SECONDARY_VAL, a.TERTIARY_VAL, a.OTHERS_VAL, a.TEMP_VAL " +
                 "FROM BEST_CONFIGURATION b " +
                 "JOIN ALL_CONFIG a ON b.IT = a.IT")

         // Export the BEST_CONFIG table to a CSV file.
         new Export_Table().exec(connection, [exportPath: "target/BEST_CONFIGURATION.csv", tableToExport: "BEST_CONFIG"])

         connection.close()

    }

    /**
     * Extracts observation sensor data from an input CSV 24_hour file  and writes filtered training dataset to an output CSV file.
     *
     * @param inputCsv  The path to the input CSV file.
     * @param outputCsv The path to the output CSV file.
     * @throws Exception If an error occurs during file reading or writing.
     */
    void extractObservationData(String inputCsv, String outputCsv) throws Exception {

        List<Map<String, String>> pointsData = new ArrayList<>()
        Map<String, Integer> idReceiverMap = new HashMap<>()
        AtomicInteger idCounter = new AtomicInteger(1)

        CSVReader reader = new CSVReader(new FileReader(inputCsv))
        reader.readNext() // Skip the header

        Set<String> uniqueSensors = new HashSet<>()
        String[] row
        while ((row = reader.readNext()) != null) {
            uniqueSensors.add(row[0])
        }

        // Assign a unique identifier to each sensor
        for (String sensor : uniqueSensors) {
            idReceiverMap.put(sensor, idCounter.getAndIncrement())
        }

        // Limit the list to 80% of elements which will represent the training data
        int sizeToKeep = (int) (idReceiverMap.size() * 0.8)
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(idReceiverMap.entrySet())

        List<Map.Entry<String, Integer>> subsetList = entryList.subList(0, sizeToKeep)

        // Create a new map from the selected elements
        Map<String, Integer> resultMap = new HashMap<>()
        for (Map.Entry<String, Integer> entry : subsetList) {
            resultMap.put(entry.getKey(), entry.getValue())
        }
        reader.close()

        try {
            CSVReader secondReader = new CSVReader(new FileReader(inputCsv))
            secondReader.readNext() // Skip the header

            while ((row = secondReader.readNext()) != null) {
                if (resultMap.containsKey(row[0]) && !row[2].equals("0")) {
                    Map<String, String> point = new HashMap<>()
                    point.put("SENSORS", row[0])
                    point.put("THE_GEOM", row[4])
                    point.put("IDRECEIVER", String.valueOf(idReceiverMap.get(row[0])))
                    point.put("T", row[1])
                    point.put("LEQA", row[2])
                    pointsData.add(point)
                }
            }
            secondReader.close()
        } catch (SQLException e) {
            e.printStackTrace()
        }

        FileWriter writer = new FileWriter(outputCsv)
        writer.write("SENSORS,THE_GEOM,IDRECEIVER,T,LEQA\n")
        for (Map<String, String> point : pointsData) {
            writer.write(String.format("%s,%s,%s,%s,%s\n",
                    point.get("SENSORS"),
                    point.get("THE_GEOM"),
                    point.get("IDRECEIVER"),
                    point.get("T"),
                    point.get("LEQA")))
        }
        writer.close()
    }

    /**
     * Generates all possible value combinations based on predefined arrays ( which can be changed )
     * and writes them to a CSV file named "ALL_COMBINATION.csv".
     *
     * The generated combinations include values for type of roads primary, secondary, tertiary, others, and temperature.
     *
     * The total number of combinations is calculated as:
     * (number of `vals` elements) ^ (number of paramèters)  * (number of `temps` elements).
     *
     * The CSV file follows the structure:
     * IT, PRIMARY, SECONDARY, TERTIARY, OTHERS, TEMP.
     */
    static void getAllConfig() {

        FileWriter csvWriterCombi = new FileWriter("./wps_scripts/ALL_COMBI.csv")
        csvWriterCombi.append("IT,PRIMARY_VAL,SECONDARY_VAL,TERTIARY_VAL,OTHERS_VAL,TEMP_VAL\n")

        //double[] vals = [0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.8, 2.0, 2.2, 2.4, 2.8, 3]
        //int[] temps = [-5, 0, 5, 10, 15, 20, 25, 30]

        // this values can be modified
        double[] vals = [0.01,1.0, 2.0,3]
        int[] temps = [20]
        int totalCombinations = vals.length * vals.length * vals.length * vals.length * temps.length

        for (int i = 0; i < totalCombinations; i++) {
            int indexPrimary = (int) (i / (vals.length * vals.length * vals.length * temps.length)) % vals.length
            int indexSecondary = (int) (i / (vals.length * vals.length * temps.length)) % vals.length
            int indexTertiary = (int) (i / (vals.length * temps.length)) % vals.length
            int indexOthers = (int) (i / temps.length) % vals.length
            int indexTemps = (int) i % temps.length

            double primary = vals[indexPrimary]
            double secondary = vals[indexSecondary]
            double tertiary = vals[indexTertiary]
            double others = vals[indexOthers]
            int valTemps = temps[indexTemps]

            int it = i + 1

            csvWriterCombi.append(it + "," + primary + "," + secondary + "," + tertiary + "," + others + "," + valTemps + "\n")
        }

        csvWriterCombi.flush()
        csvWriterCombi.close()
    }


    /**
     * Executes the assimilation process for a list of combinations, updating the database
     * with road configurations and calculating noise levels.
     * @param allCombinations A list of possible parameter combinations, where each entry contains:
     *  [iteration ID, primary factor, secondary factor, tertiary factor, others factor, temperature].
     * @param connection The database connection used for executing queries.
     * @throws SQLException If a database access error occurs.
     */
    static void assimilationProcess(List<String[]> allCombinations, Connection connection) {

        int i
        double primary
        double secondary
        double tertiary
        double others
        int valTemps

        Statement stmt = connection.createStatement()

        String nameTableA = "TABLE_A"

        stmt.execute("CREATE TABLE  "+ nameTableA+" ( " +
                "IT INTEGER, "+
                "IDRECEIVER integer, " +
                "HZ63 double precision, " +
                "HZ125 double precision, " +
                "HZ250 double precision, " +
                "HZ500 double precision, " +
                "HZ1000 double precision, " +
                "HZ2000 double precision, " +
                "HZ4000 double precision, " +
                "HZ8000 double precision)" )

        //int size = 162
        try {
            for (int j=0; j< allCombinations.size(); j++) {
                println(LocalDateTime.now())

                int it = j+1
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
                        " , ID_WAY, ST_SetSRID(THE_GEOM, 2056) AS THE_GEOM, TYPE, " +
                        "    CASE " +
                        "        WHEN TYPE = 'primary' or TYPE ='primary_link' THEN LV_D * " + primary +
                        "        WHEN TYPE = 'secondary' or TYPE = 'secondary_link' THEN LV_D * " + secondary +
                        "        WHEN TYPE = 'tertiary' or TYPE = 'tertiary_link' THEN LV_D * " + tertiary +
                        "        ELSE LV_D * " + others +
                        "    END AS LV_D, " +
                        " LV_E, LV_N, " +
                        "    CASE " +
                        "        WHEN TYPE = 'primary' or TYPE ='primary_link' THEN HGV_D * " + primary +
                        "        WHEN TYPE = 'secondary' or TYPE = 'secondary_link' THEN HGV_D * " + secondary +
                        "        WHEN TYPE = 'tertiary' THEN HGV_D * " + tertiary +
                        "        ELSE HGV_D * " + others +
                        "    END AS HGV_D " +
                        ", HGV_E, HGV_N, LV_SPD_D, LV_SPD_E, LV_SPD_N, " +
                        " HGV_SPD_D, HGV_SPD_E, HGV_SPD_N, PVMT, " + valTemps +
                        " FROM ROADS")
                String res

                println(LocalDateTime.now())
                res = new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS_CONFIG"])

                //res = new Display_Database().exec(connection, [])

                println(LocalDateTime.now())
                stmt.execute("INSERT INTO "+nameTableA+" (IT,IDRECEIVER , HZ63 , HZ125 , HZ250 , HZ500 , HZ1000 , HZ2000 , HZ4000 , HZ8000)" +
                        "SELECT "+it+", lg.IDRECEIVER, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD63 + lg.HZ63) / 10))) AS HZ63, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD125 + lg.HZ125) / 10))) AS HZ125, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD250 + lg.HZ250) / 10))) AS HZ250, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD500 + lg.HZ500) / 10))) AS HZ500, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD1000 + lg.HZ1000) / 10))) AS HZ1000, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD2000 + lg.HZ2000) / 10))) AS HZ2000, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD4000 + lg.HZ4000) / 10))) AS HZ4000, " +
                        "10 * LOG10( SUM(POWER(10,(mr.LWD8000 + lg.HZ8000) / 10))) AS HZ8000 " +
                        "FROM LDAY_GEOM  lg " +
                        "INNER JOIN LW_ROADS mr ON lg.IDSOURCE = mr.PK " +
                        "GROUP BY lg.IDRECEIVER")


                // It is possible to see the evolution of the iteration by uncommenting the following lines.
                /*if(j%2000 ==0 ){
                    println("Itération: "+j)
                }*/
                println(LocalDateTime.now())
            }

        } catch (SQLException e) {
            e.printStackTrace()
        }
    }


}
