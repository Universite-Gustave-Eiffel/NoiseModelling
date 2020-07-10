import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities

import java.sql.Connection

class Main {

    public static void main(String[] args) {
        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        boolean doCleanDB = false;
        boolean doImportBuildings = false;
        boolean doImportMatsimTraffic = false;
        boolean doCreateReceiversGrid = false;
        boolean doCreateReceiversFromMatsim = false;
        boolean doCalculateNoiseMap = false;

        if (doCleanDB) {
            CleanDB.cleanDB(connection);
        }
        if (doImportBuildings) {
            ImportBuildings.importBuildings(connection);
        }
        if (doImportMatsimTraffic) {
            ImportMatsimTraffic.importMatsimTraffic(connection);
        }
        if (doCreateReceiversGrid && !doCreateReceiversFromMatsim) {
            CreateReceiversGrid.createReceiversGrid(connection);
        }
        if (doCreateReceiversFromMatsim && !doCreateReceiversGrid) {
            CreateReceiversFromMatsim.createReceiversFromMatsim(connection);
        }

        def timeStrings = ["0_1", "1_2", "2_3", "3_4", "4_5", "5_6", "6_7", "7_8", "8_9", "9_10", "10_11", "11_12", "12_13", "13_14", "14_15", "15_16", "16_17", "17_18", "18_19", "19_20", "20_21", "21_22", "22_23", "23_24"]
        // def timeStrings = ["0_1"]


        for (timeString in timeStrings) {
            CreateRoadEmissionFromTraffic.createRoadEmissionFromTraffic(connection, [
                    "tableRoads": "ROADS_" + timeString,
                    "outTable": "ROADS_" + timeString + "_LW"
            ])
            CalculateNoiseMapFromAttenuation.calculateNoiseMap(connection, [
                    "roadsTableWithLw": "ROADS_" + timeString + "_LW",
                    "attenuationTable" : "LDAY_GEOM",
                    "outTableName" : "RESULT_GEOM_" + timeString,
            ])
        }

        if (doCalculateNoiseMap) {
            for (timeString in timeStrings) {
                CreateRoadsFromTimeString.createRoadsFromTimeString(connection, [
                    "roadsTableName" : "MATSIM_ROADS",
                    "statsTableName" : "MATSIM_ROADS_STATS",
                    "timeString" : timeString,
                    "outTableName" : "ROADS_"+timeString
                ])
                CalculateNoiseMapFromTraffic.calculateNoiseMap(connection, [
                    "tableBuilding": "BUILDINGS",
                    "tableReceivers" : "RECEIVERS",
                    "tableRoads" : "ROADS_"+timeString,
                    "confMaxSrcDist": 150,
                    "confReflOrder": 1,
                    "confSkipLevening": true,
                    "confSkipLnight": true,
                    "confSkipLden": true,
                    "confExportSourceId": true
                ])
                ExportTable.exportTable(connection, [
                    "tableToExport" : "LDAY_GEOM",
                    "exportPath": "C:\\Users\\valen\\Documents\\IFSTTAR\\Results\\RES_"+timeString+".shp"
                ])
            }
        }


        connection.close()
    }
}

