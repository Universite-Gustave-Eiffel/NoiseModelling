package org.noise_planet.noisemodelling.wps.Dynamic

import com.opencsv.CSVReader
import groovy.sql.Sql
import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Add_Laeq_Leq_columns
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
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
         // Extract observation data from the input CSV file 24_hour.csv and save it to a new CSV file.
         //extractObservationData("./wps_script/24_hour.csv", "observationSensor.csv")


         // Establish a connection to the spatial database.
         Connection connection = JDBCUtilities.wrapConnection(
                 H2GISDBFactory.createSpatialDataBase("mydatabase", true)
         )
         // Initialize SQL execution object.
         Sql sql = new Sql(connection)


         Object res
         // Display the initial state of the database.
          //res = new Display_Database().exec(connection, [])

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

         // Display the database after importing OSM data.
         //res = new Display_Database().exec(connection, [])

         // Calculate road emissions from traffic data.
         res = new Road_Emission_from_Traffic().exec(connection, ["tableRoads": "ROADS"])

         // Display the database after calculating road emissions.
         //res = new Display_Database().exec(connection, [])

         // Import the observation sensor data into the database.
         new Import_File().exec(connection, [
                 "pathFile": "./wps_scripts/observationSensor.csv",
                 "tableName": "OBSERVATION"
         ])

         // Display the database after importing observation data.
         //res = new Display_Database().exec(connection, [])

         // Create the RECEIVERS table with unique sensor data from OBSERVATION table.
         sql.execute("DROP TABLE IF EXISTS RECEIVERS")
         sql.execute("CREATE TABLE RECEIVERS(IDRECEIVER INTEGER PRIMARY KEY, THE_GEOM GEOMETRY)")
         sql.execute("INSERT INTO RECEIVERS (IDRECEIVER, THE_GEOM) SELECT DISTINCT IDRECEIVER, ST_GeomFromText(ST_AsText(THE_GEOM), 2056) FROM OBSERVATION")

         // Display the database after creating the RECEIVERS table.
         //res = new Display_Database().exec(connection, [])


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

         // Display the database after calculating noise levels.
         //res = new Display_Database().exec(connection, [])

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

         // Display the database after adding LEQA columns.
         //res = new Display_Database().exec(connection, [])

         // Calculate the difference between observation and simulated data.
         res = new DiffAB().exec(connection, [
                 "mainMapTable": "OBSERVATION",
                 "secondMapTable": "TABLE_A_0",
                 "outTable": "SCENARIO_0"
         ]);

         // Visualize the SCENARIO_0 table.
         new Table_Visualization_Data().exec(connection, ["tableName": "SCENARIO_0"])

         // Display the database after calculating the difference.
         //res = new Display_Database().exec(connection, [])

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

         // Retrieve all configurations.
         //getAllConfig()

         // Read all combinations from the CSV file.
         List<String[]> allCombinations = new ArrayList<>();
         CSVReader reader = new CSVReader(new FileReader("./wps_scripts/ALL_COMBINATION.csv"))
         reader.readNext()
         String[] row
         while ((row = reader.readNext()) != null) {
             allCombinations.add(row)
         }
         // get the similated noise table TABLE_A.
         assimilationProcess(allCombinations, connection)

         // Display the database after processing time steps.
         //res = new Display_Database().exec(connection, [])

         // Add LEQA columns to the TABLE_A table.
         new Add_Laeq_Leq_columns().exec(connection, [
                 "prefix": "HZ",
                 "tableName": "TABLE_A"
         ])

         // Display the database after adding LEQA columns.
         //res = new Display_Database().exec(connection, [])

         // Export the TABLE_A table to a CSV file.
         new Export_Table().exec(connection, [
                 "exportPath": "target/TABLE_A.csv",
                 "tableToExport": "TABLE_A"
         ])

         sql.execute("ALTER TABLE OBSERVATION " +
                 " ALTER COLUMN T SET DATA TYPE  INTEGER")

         sql.execute("ALTER TABLE OBSERVATION " +
                 " ALTER COLUMN IDRECEIVER SET DATA TYPE  FLOAT")

         sql.execute("ALTER TABLE OBSERVATION " +
                 " ALTER COLUMN LEQA SET DATA TYPE  FLOAT")

         /*new Import_File().exec(connection, [
                 pathFile : "./TABLE_A.csv",
                 tableName : "TABLE_A"])
         sql.execute("ALTER TABLE TABLE_A " +
                 " ALTER COLUMN LEQA SET DATA TYPE  FLOAT")*/

         // Create the LOOP_TEMPS table to store average LEQA differences between observation and simulated data for each time step T
         sql.execute("CREATE TABLE LOOP_TEMPS AS " +
                 "SELECT mmt.T, smt.IT, AVG(ABS(mmt.LEQA - smt.LEQA)) AS LEQA " +
                 "FROM OBSERVATION mmt " +
                 "JOIN TABLE_A smt ON mmt.IDRECEIVER = smt.IDRECEIVER " +
                 "GROUP BY mmt.T, smt.IT")

         // Create the B_CONFIG table to store the best configurations.
         sql.execute("CREATE TABLE B_CONFIG AS  SELECT T, IT, LEQA " +
                 "FROM (" +
                 "    SELECT T, IT, LEQA," +
                 "           ROW_NUMBER() OVER (PARTITION BY T ORDER BY LEQA ASC) AS rn " +
                 "    FROM LOOP_TEMPS " +
                 ") " +
                 "WHERE rn = 1 ")

         // Import the ALL_CONFIG table from the CSV file.
         new Import_File().exec(connection, [
                 "pathFile": "./ALL_COMBINATION.csv",
                 "tableName": "ALL_CONFIG"
         ])

         // Change the type "text from the csv file" to integer
         sql.execute("ALTER TABLE ALl_CONFIG " +
                 " ALTER COLUMN IT SET DATA TYPE  INTEGER")

         // Create the BEST_CONFIG table to store the best configurations with adding the corresponding combination.
         sql.execute("CREATE TABLE BEST_CONFIG AS " +
                 "SELECT b.T, b.IT, b.LEQA, a.PRIMARY_VAL, a.SECONDARY_VAL, a.TERTIARY_VAL, a.OTHERS_VAL, a.TEMP_VAL " +
                 "FROM B_CONFIG b " +
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
    void getAllConfig() {

        FileWriter csvWriterCombi = new FileWriter("ALL_COMBINATION.csv")
        csvWriterCombi.append("IT,PRIMARY,SECONDARY,TERTIARY,OTHERS,TEMP\n")

        //double[] vals = [0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.8, 2.0, 2.2, 2.4, 2.8, 3]
        //int[] temps = [-5, 0, 5, 10, 15, 20, 25, 30]

        // this values can be modified
        double[] vals = [0.2, 0.4, 0.6, 0.8, 1.0, 1.15, 1.30, 1.45, 2.0]
        int[] temps = [0, 10, 20, 30]
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

        int size = 20000
        try {
            for (int j=0; j< size; j++) {
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
