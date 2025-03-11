package org.noise_planet.noisemodelling.wps.Dynamic;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import groovy.sql.Sql
import groovy.transform.CompileStatic;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table;
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File;
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM;
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source;
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic;
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;


class RunAssimilation {

    /**
     * Main method to execute the traffic calibration process.
     *
     * @param args Command line arguments (not used in this method).
     */
    //@CompileStatic
    static void main(String[] args) {
        // Establish a connection to the spatial database.
        Connection connection = JDBCUtilities.wrapConnection(
                H2GISDBFactory.createSpatialDataBase("ltdata_base", true)
        )

        Sql sql = new Sql(connection)

        // Import the best configuration data into the database.
        new Import_File().exec(connection, [
                pathFile: "./wps_scripts/BEST_CONFIGURATION_.csv",
                tableName: "BEST_CONFIG"
        ])
        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN IT SET DATA TYPE  INTEGER")

        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN LEQA SET DATA TYPE  FLOAT")

        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN T SET DATA TYPE  INTEGER")

        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN PRIMARY_VAL SET DATA TYPE  FLOAT")

        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN SECONDARY_VAL SET DATA TYPE  FLOAT")

        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN TERTIARY_VAL SET DATA TYPE  FLOAT")

        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN OTHERS_VAL SET DATA TYPE  FLOAT")

        sql.execute("ALTER TABLE BEST_CONFIG " +
                " ALTER COLUMN TEMP_VAL SET DATA TYPE  INTEGER")


        String res
        //res = new Display_Database().exec(connection, [])

        // Alter the data types of columns in the BEST_CONFIG table.
        /*sql.execute("ALTER TABLE BEST_CONFIG ALTER COLUMN PRIMARY_VAL SET DATA TYPE FLOAT")
        sql.execute("ALTER TABLE BEST_CONFIG ALTER COLUMN SECONDARY_VAL SET DATA TYPE FLOAT")
        sql.execute("ALTER TABLE BEST_CONFIG ALTER COLUMN TERTIARY_VAL SET DATA TYPE FLOAT")
        sql.execute("ALTER TABLE BEST_CONFIG ALTER COLUMN OTHERS_VAL SET DATA TYPE FLOAT")
        sql.execute("ALTER TABLE BEST_CONFIG ALTER COLUMN TEMP_VAL SET DATA TYPE INTEGER")*/

        // Import observation sensor data into the database.
        new Import_File().exec(connection, [
                pathFile: "./wps_scripts/observationSensor.csv",
                tableName: "OBSERVATION"
        ])
        sql.execute("ALTER TABLE OBSERVATION " +
                " ALTER COLUMN T SET DATA TYPE  INTEGER")

        sql.execute("ALTER TABLE OBSERVATION " +
                " ALTER COLUMN IDRECEIVER SET DATA TYPE  FLOAT")

        sql.execute("ALTER TABLE OBSERVATION " +
                " ALTER COLUMN LEQA SET DATA TYPE  FLOAT")

        //res = new Display_Database().exec(connection, [])

        // Import OSM data into the database.
        res = new Import_OSM().exec(connection, [
                pathFile: "./wps_scripts/geneve_area.osm.pbf",
                targetSRID: 2056,
                ignoreGround: true,
                ignoreBuilding: false,
                ignoreRoads: false,
                removeTunnels: true
        ])

        //res = new Display_Database().exec(connection, [])

        // Add a TEMP column to the ROADS table.
        sql.execute("ALTER TABLE ROADS ADD TEMP DOUBLE")

        // Create a regular grid of receivers.
        res = new Regular_Grid().exec(connection, [
                buildingTableName: "BUILDINGS",
                sourcesTableName: "ROADS",
                delta: 20
        ])

        //res = new Display_Database().exec(connection, [])
        sql.execute("ALTER TABLE RECEIVERS DROP COLUMN ID_ROW, ID_COL")
       // res = new Display_Database().exec(connection, [])

        int last_pk
        try {
            java.sql.Statement smt = connection.createStatement()
            ResultSet rs = smt.executeQuery("SELECT MAX(PK) FROM RECEIVERS")
            if (rs.next()) {
                last_pk = rs.getInt(1)
            }
        } catch (SQLException e) {
            e.printStackTrace()
        }

        println("last pk :"+ last_pk)

        // Update the device mapping CSV file with new receiver IDs and training flags.
        new CSVReader(new FileReader("./wps_scripts/device_mapping_sf.csv")).withCloseable { reader ->
            List<String[]> rows = new ArrayList<>()
            String[] header = reader.readNext()
            if (header != null) {
                String[] newHeader = Arrays.copyOf(header, header.length + 2)
                newHeader[header.length] = "IDRECEIVER"
                newHeader[header.length + 1] = "TRAINING"
                rows.add(newHeader)
            }
            String[] row
            while ((row = reader.readNext()) != null) {
                String[] newRow = Arrays.copyOf(row, row.length + 2);
                newRow[row.length] = String.valueOf(++last_pk)
                newRow[row.length + 1] = "NO";
                rows.add(newRow)
                sql.execute("INSERT INTO RECEIVERS (THE_GEOM) VALUES (ST_GeomFromText('" + row[1] + "', 2056))")
            }

            FileWriter fileWriter =  new FileWriter("./target/device_mapping_sf.csv")
            CSVWriter writer = new CSVWriter(fileWriter)
            writer.writeAll(rows)
            /*new CSVWriter(new FileWriter("./target/device_mapping_sf.csv")).withCloseable { writer ->
                writer.writeAll(rows)
            }*/
        }

        //res = new Display_Database().exec(connection, [])

        // Create the DYNAMIC_ROADS table and populate it with dynamic road data by varying the traffic with the best configuration.
        sql.execute("CREATE TABLE DYNAMIC_ROADS (" +
                "PK serial PRIMARY KEY," +
                "TIME integer," +
                "LINK_ID INTEGER," +
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
                "TEMP DOUBLE)")

        sql.execute("INSERT INTO DYNAMIC_ROADS (TIME, LINK_ID, THE_GEOM, TYPE, LV_D, LV_E, LV_N, HGV_D, HGV_E, HGV_N, LV_SPD_D, LV_SPD_E, LV_SPD_N, HGV_SPD_D, HGV_SPD_E, HGV_SPD_N, PVMT, TEMP)" +
                "SELECT " +
                "c.T," +
                "r.ID_WAY, r.THE_GEOM, r.TYPE," +
                "CASE " +
                "WHEN r.TYPE = 'primary' OR r.TYPE = 'primary_link' THEN r.LV_D * c.PRIMARY_VAL " +
                "WHEN r.TYPE = 'secondary' OR r.TYPE = 'secondary_link' THEN r.LV_D * c.SECONDARY_VAL " +
                "WHEN r.TYPE = 'tertiary' OR r.TYPE = 'tertiary_link' THEN r.LV_D * c.TERTIARY_VAL " +
                "ELSE r.LV_D * c.OTHERS_VAL " +
                "END AS LV_D, " +
                "r.LV_E, r.LV_N," +
                "CASE " +
                "WHEN r.TYPE = 'primary' OR r.TYPE = 'primary_link' THEN r.HGV_D * c.PRIMARY_VAL " +
                "WHEN r.TYPE = 'secondary' OR r.TYPE = 'secondary_link' THEN r.HGV_D * c.SECONDARY_VAL " +
                "WHEN r.TYPE = 'tertiary' OR r.TYPE = 'tertiary_link' THEN r.HGV_D * c.TERTIARY_VAL " +
                "ELSE r.HGV_D * c.OTHERS_VAL " +
                "END AS HGV_D, " +
                "r.HGV_E, r.HGV_N, r.LV_SPD_D, r.LV_SPD_E, r.LV_SPD_N, r.HGV_SPD_D, r.HGV_SPD_E, r.HGV_SPD_N, r.PVMT, c.TEMP_VAL " +
                "FROM ROADS r " +
                "CROSS JOIN BEST_CONFIG c")

        // Execute road emission and noise level calculations with dybnamic mode.
        res = new Road_Emission_from_Traffic().exec(connection, [
                tableRoads: "DYNAMIC_ROADS",
                mode: "dynamic"
        ])
        new Noise_level_from_source().exec(connection, [
                tableBuilding: "BUILDINGS",
                tableSources: "SOURCES_0DB",
                tableReceivers: "RECEIVERS",
                maxError: 0.0,
                confMaxSrcDist: 500,
                confDiffHorizontal: false,
                confExportSourceId: true,
                confSkipLday: true,
                confSkipLevening: true,
                confSkipLnight: true,
                confSkipLden: true
        ])


        // Compute the noise level from the moving vehicles to the receivers
        // the output table is called here LT_GEOM and contains the noise level at each receiver for the whole timesteps
        res = new Noise_From_Attenuation_Matrix().exec(connection, [
                lwTable: "LW_ROADS",
                lwTable_sourceId: "LINK_ID",
                attenuationTable: "LDAY_GEOM",
                sources0DBTable: "SOURCES_0DB",
                outputTable: "LT_GEOM"
        ])
        //WHERE st.LINK_ID = mr.LINK_ID AND lg.IDSOURCE = st.PK
        //assertEquals("Process done. Table of receivers LT_GEOM created!", res)

        new Export_Table().exec(connection, [exportPath: "target/LT_GEOM.shp", tableToExport: "LT_GEOM"])

        new Import_File().exec(connection, [
                pathFile: "./wps_scripts/device_mapping_sf.csv",
                tableName: "SENSORS"
        ])
        new Import_File().exec(connection, [
                pathFile: RunAssimilation.getResource("./wps_scripts/24_hour.csv").getPath(),
                tableName: "TWENTY_FOUR_HOUR"
        ])

        // Create the RECAP_TABLE to store the comparison results of training and testing data.
        sql.execute("CREATE TABLE RECAP_TABLE (IDRECEIVER INTEGER, THE_GEOM GEOMETRY, TIMESTRING INTEGER, LEQA_PRED DOUBLE, LEQA_MES DOUBLE, DIFF DOUBLE, TRAINING VARCHAR(255))")

        // Insert the comparison results into the RECAP_TABLE.
        sql.execute("INSERT INTO RECAP_TABLE (IDRECEIVER, THE_GEOM, TIMESTRING, LEQA_PRED, LEQA_MES, DIFF, TRAINING) " +
                "SELECT lt.IDRECEIVER, s.THE_GEOM, lt.TIMESTRING, lt.LEQA, h.LEQ, ABS(h.LEQ - lt.LEQA) AS DIFF, s.TRAINING " +
                "FROM LT_GEOM lt " +
                "INNER JOIN TWENTY_FOUR_HOUR h ON h.EPOCH = lt.TIMESTRING " +
                "INNER JOIN SENSORS s ON h.DEVEUI = s.DEVEUI " +
                "WHERE lt.IDRECEIVER > ? AND s.IDRECEIVER = lt.IDRECEIVER", last_pk)

        // Update the training flag in the RECAP_TABLE.
        sql.execute("UPDATE RECAP_TABLE rt " +
                "SET TRAINING = 'yes' " +
                "WHERE rt.IDRECEIVER IN (" +
                "SELECT s.IDRECEIVER " +
                "FROM SENSORS s " +
                "JOIN OBSERVATION o " +
                "WHERE o.SENSORS = s.DEVEUI" +
                ")");

        //res = new Display_Database().exec(connection, [])

        // Create and populate the EACH_TEMP table to store average differences per timestamp.
        sql.execute("CREATE TABLE EACH_TEMP (TIMESTRING INTEGER, TRAINING VARCHAR(255), MEAN DOUBLE)")
        sql.execute("INSERT INTO EACH_TEMP (TIMESTRING, TRAINING, MEAN) " +
                "SELECT " +
                "t.TIMESTRING, " +
                "t.TRAINING, " +
                "avg(DIFF) " +
                "FROM " +
                "RECAP_TABLE t " +
                "GROUP BY " +
                "TIMESTRING, TRAINING")

        //res = new Display_Database().exec(connection, [])

        // Create and populate the EACH_TYPE_SENSORS table to store average differences per sensor type(training and test).
        sql.execute("CREATE TABLE EACH_TYPE_SENSORS (TRAINING VARCHAR(255), MEAN DOUBLE)")
        sql.execute("INSERT INTO EACH_TYPE_SENSORS (TRAINING, MEAN) " +
                "SELECT " +
                "TRAINING, " +
                "avg(DIFF) " +
                "FROM " +
                "RECAP_TABLE " +
                "GROUP BY TRAINING")

        // Export the final results to CSV and SHP files.
        new Export_Table().exec(connection, [exportPath: "target/RECAP.csv", tableToExport: "RECAP_TABLE"])
        new Export_Table().exec(connection, [exportPath: "target/RECAP.shp", tableToExport: "RECAP_TABLE"])
        new Export_Table().exec(connection, [exportPath: "target/EACH_TYPE_SENSORS.csv", tableToExport: "EACH_TYPE_SENSORS"])
        new Export_Table().exec(connection, [exportPath: "target/EACH_TEMP.csv", tableToExport: "EACH_TEMP"])

        connection.close()
    }
}