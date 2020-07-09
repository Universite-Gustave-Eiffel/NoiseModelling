import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Database_Manager.Clean_Database
import org.noise_planet.noisemodelling.wps.Experimental.Create_Roads_Matsim_From_TimeString
import org.noise_planet.noisemodelling.wps.Experimental.Osm_to_Buildings
import org.noise_planet.noisemodelling.wps.Experimental.Traffic_from_matsim_events
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid

import java.sql.Connection

class Main {

    public static void main(String[] args) {
        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        boolean cleanDB = true;
        boolean importBuildings = true;
        boolean importMatsimTraffic = true;
        boolean createReceivers = true;
        boolean calculateNoiseMap = true;

        if (cleanDB) {
            println "-------------------------------"
            println "Cleaning Database"
            println "-------------------------------"
            new Clean_Database().exec(connection, [
                "areYouSure": true
            ])
        }
        if (importBuildings) {
            println "-------------------------------"
            println "Importing Buildings from Osm"
            println "-------------------------------"
            new Osm_to_Buildings().exec(connection, [
                    "pathFile"        : "C:\\Users\\valen\\Documents\\IFSTTAR\\OsmMaps\\proce-plus.pbf",
                    "convert2Building": false,
                    "targetSRID"      : 2154
            ])
        }
        if (importMatsimTraffic) {
            println "-------------------------------"
            println "Importing Matsim traffic results"
            println "-------------------------------"
            new Traffic_from_matsim_events().exec(connection, [
                    "folder" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01",
                    "outTableName" : "MATSIM_ROADS",
                    "link2GeometryTable" : "LINK2GEOM",
                    "link2GeometryFile" : "network.csv" // relative path
            ])
        }
        if (createReceivers) {
            println "-------------------------------"
            println "Creating Receivers grid"
            println "-------------------------------"
            new Regular_Grid().exec(connection, [
                    "delta" : 20,
                    "buildingTableName" : "BUILDINGS",
            ])
        }

        def timeStrings = ["0_1", "1_2", "2_3", "3_4", "4_5", "5_6", "6_7", "7_8", "8_9", "9_10", "10_11", "11_12", "12_13", "13_14", "14_15", "15_16", "16_17", "17_18", "18_19", "19_20", "20_21", "21_22", "22_23", "23_24"]

        if (calculateNoiseMap) {
            for (timeString in timeStrings) {
                println "-------------------------------"
                println "Getting roads for " + timeString
                println "-------------------------------"
                new Create_Roads_Matsim_From_TimeString().exec(connection, [
                        "roadsTableName" : "MATSIM_ROADS",
                        "statsTableName" : "MATSIM_ROADS_STATS",
                        "timeString" : timeString,
                        "outTableName" : "ROADS_"+timeString
                ])
                println "-------------------------------"
                println "Getting NoiseMap for " + timeString
                println "-------------------------------"
                new Noise_level_from_traffic().exec(connection, [
                        "tableBuilding": "BUILDINGS",
                        "tableReceivers" : "RECEIVERS",
                        "tableRoads" : "ROADS_"+timeString,
                        "confMaxSrcDist": 150,
                        "confReflOrder": 1,
                        "confSkipLevening": true,
                        "confSkipLnight": true,
                        "confSkipLden": true,
                ])
                println "-------------------------------"
                println "Exporting Results for " + timeString
                println "-------------------------------"
                new Export_Table().exec(connection, [
                        "tableToExport" : "LDAY_GEOM",
                        "exportPath": "C:\\Users\\valen\\Documents\\IFSTTAR\\Results\\RES_"+timeString+".shp"
                ])
            }
        }
        connection.close()
    }
}

