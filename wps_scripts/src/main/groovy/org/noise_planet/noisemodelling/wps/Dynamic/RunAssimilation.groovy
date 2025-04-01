package org.noise_planet.noisemodelling.wps.Dynamic

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import groovy.sql.Sql
import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_OSM
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement


class RunAssimilation {

    static void main(String[] args) {
        Connection connection = null
        try {
            // Establish a connection to the spatial database.
            connection = JDBCUtilities.wrapConnection(
                    H2GISDBFactory.createSpatialDataBase("mem:assimilationDatabase", true)
            );
            Sql sql = new Sql(connection);

            // Import the best configuration data into the database.
            new Import_File().exec(connection,[
                    "pathFile": "./target/BEST_CONFIGURATION.csv",
                    "tableName": "BEST_CONFIG"
            ])
            alterTableColumns(sql, "BEST_CONFIG");

            /*sql.execute("CREATE TABLE B_CONFIG AS SELECT * FROM BEST_CONFIG " +
                    "LIMIT (SELECT COUNT(*) / 2 FROM BEST_CONFIG) "+
                    "OFFSET (SELECT COUNT(*) / 2 FROM BEST_CONFIG)")*/

            // Import observation sensor data into the database.
            new Import_File().exec(connection,[
                    "pathFile": "./wps_scripts/observationSensor.csv",
                    "tableName": "OBSERVATION"]
            );
            // Change the type "text from the csv file" to INTEGER or FLOAT
            sql.execute("ALTER TABLE OBSERVATION " +
                    " ALTER COLUMN T SET DATA TYPE  INTEGER")

            sql.execute("ALTER TABLE OBSERVATION " +
                    " ALTER COLUMN IDRECEIVER SET DATA TYPE  INTEGER")

            sql.execute("ALTER TABLE OBSERVATION " +
                    " ALTER COLUMN LEQA SET DATA TYPE  FLOAT")

            // because these sensors always measure 40 db: ID 17, 12 AND 29
            sql.execute("DELETE FROM OBSERVATION "+
                    "WHERE IDRECEIVER = 17 or IDRECEIVER = 12 or IDRECEIVER = 29;")

            // Import OSM data into the database.
            new Import_OSM().exec(connection, [
                    "pathFile": "./wps_scripts/geneve.osm.pbf",
                    "targetSRID": 2056,
                    "ignoreGround": true,
                    "ignoreBuilding": false,
                    "ignoreRoads": false,
                    "removeTunnels": true
            ]);

            // Add a TEMP column to the ROADS table.
            sql.execute("ALTER TABLE ROADS ADD TEMP DOUBLE");

            // Create a regular grid of receivers.
            new Regular_Grid().exec(connection,[
                    "buildingTableName": "BUILDINGS",
                    "sourcesTableName":"ROADS",
                    "delta": 20
            ]);

            // Drop unnecessary columns from RECEIVERS table.
            sql.execute("ALTER TABLE RECEIVERS DROP COLUMN ID_ROW, ID_COL");

            int last_pk = getLastPk(connection, "RECEIVERS");
            println("last pk :" + last_pk);

            // Update the device mapping CSV file with new receiver IDs and training flags.
           updateDeviceMapping(last_pk, sql);

            // Create the DYNAMIC_ROADS table and populate it with dynamic road data by varying the traffic with the best configuration.
            createDynamicRoadsTable(sql);

            // Execute road emission and noise level calculations with dynamic mode.
            new Road_Emission_from_Traffic().exec(connection, [
                    "tableRoads": "DYNAMIC_ROADS",
                    "mode": "dynamic"
            ]);
            new Noise_level_from_source().exec(connection, [
                    "tableBuilding": "BUILDINGS",
                    "tableSources": "SOURCES_0DB",
                    "tableReceivers": "RECEIVERS",
                    "maxError": 0.0,
                    "confMaxSrcDist": 250,
                    "confDiffHorizontal": false,
                    "confExportSourceId": true,
                    "confSkipLday": true,
                    "confSkipLevening": true,
                    "confSkipLnight": true,
                    "confSkipLden": true
            ]);

            // Compute the noise level from the moving vehicles to the receivers
            new Noise_From_Attenuation_Matrix().exec(connection, [
                    "lwTable": "LW_ROADS",
                    "lwTable_sourceId": "LINK_ID",
                    "attenuationTable": "LDAY_GEOM",
                    "sources0DBTable": "SOURCES_0DB",
                    "outputTable": "LT_GEOM"
            ]);

            // Export the LT_GEOM table to a shapefile.
            new Export_Table().exec(connection,
                    ["exportPath": "./target/LT_GEOM.shp",
                    "tableToExport": "LT_GEOM"])


            // Import the updated device mapping and 24-hour data into the database.
            new Import_File().exec(connection,
                   [ "pathFile": "./wps_scripts/device_m_sf.csv",
                    "tableName": "SENSORS"]
            );
            sql.execute("ALTER TABLE SENSORS " +
                    " ALTER COLUMN IDRECEIVER SET DATA TYPE  INTEGER")

            new Import_File().exec(connection,[
                    "pathFile": "./wps_scripts/24_hour.csv",
                    "tableName": "TWENTY_FOUR_HOUR"
            ]);
            sql.execute("ALTER TABLE TWENTY_FOUR_HOUR " +
                    " ALTER COLUMN EPOCH SET DATA TYPE  INTEGER")

            sql.execute("ALTER TABLE TWENTY_FOUR_HOUR " +
                    " ALTER COLUMN LEQ SET DATA TYPE  FLOAT")

            // Create the RECAP_TABLE to store the comparison results of training and testing data.
            createRecapTable(sql, last_pk);

            // Create and populate the EACH_TEMP and EACH_TYPE_SENSORS tables.
            createEachTempTable(sql);
            createEachTypeSensorsTable(sql);

            // Export the final results to CSV and SHP files.
            exportFinalResults(connection);
            //Import observation sensor data into the database.



        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void alterTableColumns(Sql sql, String tableName) {
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN IT SET DATA TYPE INTEGER");
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN LEQA_DIFF SET DATA TYPE FLOAT");
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN T SET DATA TYPE INTEGER");
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN PRIMARY_VAL SET DATA TYPE FLOAT");
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN SECONDARY_VAL SET DATA TYPE FLOAT");
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN TERTIARY_VAL SET DATA TYPE FLOAT");
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN OTHERS_VAL SET DATA TYPE FLOAT");
        sql.execute("ALTER TABLE " + tableName + " ALTER COLUMN TEMP_VAL SET DATA TYPE INTEGER");
    }

    private static int getLastPk(Connection connection, String tableName) throws SQLException {
        int last_pk = 0;
        try {
            Statement smt = connection.createStatement()
             ResultSet rs = smt.executeQuery("SELECT MAX(PK) FROM " + tableName)
            if (rs.next()) {
                last_pk = rs.getInt(1);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return last_pk;
    }

    private static void updateDeviceMapping(int last_pk, Sql sql) {
        try {
             CSVReader reader = new CSVReader(new FileReader("./wps_scripts/device_mapping_sf.csv"));

            List<String[]> rows = new ArrayList<>();
            String[] header = reader.readNext();
            if (header != null) {
                String[] newHeader = Arrays.copyOf(header, header.length + 2);
                newHeader[header.length] = "IDRECEIVER";
                newHeader[header.length + 1] = "TRAINING";
                rows.add(newHeader);
            } // 70b3d5078000049e, 70b3d50780000678,
            String[] row;
            while ((row = reader.readNext()) != null) {
                String[] newRow = Arrays.copyOf(row, row.length + 2);
                newRow[row.length] = String.valueOf(++last_pk);
                newRow[row.length + 1] = "NO";
                rows.add(newRow);
                sql.execute("INSERT INTO RECEIVERS (THE_GEOM) VALUES (ST_GeomFromText('" + row[1] + "', 2056))");
            }
            new CSVWriter(new FileWriter("./wps_scripts/device_m_sf.csv")).withCloseable { writer ->
                writer.writeAll(rows)
            }
            //writer.writeAll(rows);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createDynamicRoadsTable(Sql sql) {
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
                "CROSS JOIN B_CONFIG c")
    }

    private static void createRecapTable(Sql sql, int last_pk) {
        sql.execute("CREATE TABLE RECAP_TABLE (IDRECEIVER INTEGER, THE_GEOM GEOMETRY, TIMESTRING INTEGER, LEQA_PRED DOUBLE, LEQA_MES DOUBLE, DIFF DOUBLE, TRAINING VARCHAR(255))");

        sql.execute("INSERT INTO RECAP_TABLE (IDRECEIVER, THE_GEOM, TIMESTRING, LEQA_PRED, LEQA_MES, DIFF, TRAINING) " +
                "SELECT lt.IDRECEIVER, ST_SetSRID(s.THE_GEOM, 2056), lt.TIMESTRING, lt.LEQA, h.LEQ, ABS(h.LEQ - lt.LEQA) AS DIFF, s.TRAINING " +
                "FROM LT_GEOM lt " +
                "INNER JOIN TWENTY_FOUR_HOUR h ON h.EPOCH = lt.TIMESTRING " +
                "INNER JOIN SENSORS s ON h.DEVEUI = s.DEVEUI " +
                "WHERE lt.IDRECEIVER > ? AND s.IDRECEIVER = lt.IDRECEIVER and h.LEQ > 0.0", last_pk);

        sql.execute("UPDATE RECAP_TABLE rt " +
                "SET TRAINING = 'yes' " +
                "WHERE rt.IDRECEIVER IN (" +
                "SELECT s.IDRECEIVER " +
                "FROM SENSORS s " +
                "JOIN OBSERVATION o " +
                "WHERE o.SENSORS = s.DEVEUI" +
                ")");
    }

    private static void createEachTempTable(Sql sql) {
        sql.execute("CREATE TABLE EACH_TEMP (TIMESTRING INTEGER, TRAINING VARCHAR(255), MEAN DOUBLE)");
        sql.execute("INSERT INTO EACH_TEMP (TIMESTRING, TRAINING, MEAN) " +
                "SELECT " +
                "t.TIMESTRING, " +
                "t.TRAINING, " +
                "avg(DIFF) " +
                "FROM " +
                "RECAP_TABLE t " +
                "GROUP BY " +
                "TIMESTRING, TRAINING");
    }

    private static void createEachTypeSensorsTable(Sql sql) {
        sql.execute("CREATE TABLE EACH_TYPE_SENSORS (TRAINING VARCHAR(255), MEAN DOUBLE)");
        sql.execute("INSERT INTO EACH_TYPE_SENSORS (TRAINING, MEAN) " +
                "SELECT " +
                "TRAINING, " +
                "avg(DIFF) " +
                "FROM " +
                "RECAP_TABLE " +
                "GROUP BY TRAINING");
    }

    private static void exportFinalResults(Connection connection) {
        new Export_Table().exec(connection,
               [ "exportPath": "./wps_scripts/RECAP.csv",
                "tableToExport": "RECAP_TABLE"]
        );
        new Export_Table().exec(connection,
                ["exportPath": "./wps_scripts/RECAP.shp",
                "tableToExport": "RECAP_TABLE"]
        );
        new Export_Table().exec(connection,
                ["exportPath": "./wps_scripts/EACH_TYPE_SENSORS.csv",
                "tableToExport": "EACH_TYPE_SENSORS"]
        );
        new Export_Table().exec(connection,
               [ "exportPath": "./wps_scripts/EACH_TEMP.csv",
                "tableToExport": "EACH_TEMP"]
        );
    }
}
