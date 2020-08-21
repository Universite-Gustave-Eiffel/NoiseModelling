import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities

import java.sql.Connection

class Main {

    public static void main(String[] args) {

        def hourTimeStrings = ["0_1", "1_2", "2_3", "3_4", "4_5", "5_6", "6_7", "7_8", "8_9", "9_10", "10_11", "11_12", "12_13", "13_14", "14_15", "15_16", "16_17", "17_18", "18_19", "19_20", "20_21", "21_22", "22_23", "23_24"]
        def quarterHourTimeStrings = ["0h00_0h15", "0h15_0h30", "0h30_0h45", "0h45_1h00", "1h00_1h15", "1h15_1h30", "1h30_1h45", "1h45_2h00", "2h00_2h15", "2h15_2h30", "2h30_2h45", "2h45_3h00", "3h00_3h15", "3h15_3h30", "3h30_3h45", "3h45_4h00", "4h00_4h15", "4h15_4h30", "4h30_4h45", "4h45_5h00", "5h00_5h15", "5h15_5h30", "5h30_5h45", "5h45_6h00", "6h00_6h15", "6h15_6h30", "6h30_6h45", "6h45_7h00", "7h00_7h15", "7h15_7h30", "7h30_7h45", "7h45_8h00", "8h00_8h15", "8h15_8h30", "8h30_8h45", "8h45_9h00", "9h00_9h15", "9h15_9h30", "9h30_9h45", "9h45_10h00", "10h00_10h15", "10h15_10h30", "10h30_10h45", "10h45_11h00", "11h00_11h15", "11h15_11h30", "11h30_11h45", "11h45_12h00", "12h00_12h15", "12h15_12h30", "12h30_12h45", "12h45_13h00", "13h00_13h15", "13h15_13h30", "13h30_13h45", "13h45_14h00", "14h00_14h15", "14h15_14h30", "14h30_14h45", "14h45_15h00", "15h00_15h15", "15h15_15h30", "15h30_15h45", "15h45_16h00", "16h00_16h15", "16h15_16h30", "16h30_16h45", "16h45_17h00", "17h00_17h15", "17h15_17h30", "17h30_17h45", "17h45_18h00", "18h00_18h15", "18h15_18h30", "18h30_18h45", "18h45_19h00", "19h00_19h15", "19h15_19h30", "19h30_19h45", "19h45_20h00", "20h00_20h15", "20h15_20h30", "20h30_20h45", "20h45_21h00", "21h00_21h15", "21h15_21h30", "21h30_21h45", "21h45_22h00", "22h00_22h15", "22h15_22h30", "22h30_22h45", "22h45_23h00", "23h00_23h15", "23h15_23h30", "23h30_23h45", "23h45_24h00"];

        Connection connection;
        String dbName = "h2gisdb"
        boolean createDB = false;
        if (createDB) {
            connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(dbName, true));
        }
        else {
            connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
        }

        boolean doCleanDB = false;
        boolean doImportBuildings = false;
        boolean doImportMatsimTraffic = false;
        boolean doCreateReceiversFromMatsim = false;
        boolean doCalculateNoisePropagation = true;
        boolean doCalculateRoadEmission = false;
        boolean doCalculateNoiseMap = true;
        boolean doExportResults = true;
        boolean doCalcuateExposure = false;

        String timeSlice = "quarter";
        String osmFile = "/home/valoo/Projects/IFSTTAR/OsmMaps/nantes.pbf";
        String matsimFolder = "/home/valoo/Projects/IFSTTAR/eqasim-nantes/output_0.25/simulation_output"
        String resultsFolder = "/home/valoo/Projects/IFSTTAR/Results/GeoData/AgentsImpact_0.01"
        String ignoreAgents = ""

        if (doCleanDB) {
            CleanDB.cleanDB(connection);
        }
        if (doImportBuildings) {
            ImportBuildings.importBuildings(connection, [
                    "pathFile"        : osmFile,
                    "targetSRID"      : 2154
            ]);
        }
        if (doImportMatsimTraffic) {
            ImportMatsimTraffic.importMatsimTraffic(connection, [
                    "folder" : matsimFolder,
                    "outTableName" : "MATSIM_ROADS",
                    "link2GeometryFile" : "network.csv", // relative path
                    "timeSlice": timeSlice, // DEN, hour, quarter
                    "skipUnused": "true",
                    "ignoreAgents": ignoreAgents
            ]);
        }
        if (doCreateReceiversFromMatsim) {
            CreateReceiversOnBuildings.createReceiversOnBuildings(connection);
            ImportActivitesFromMatsim.importActivitesFromMatsim(connection, [
                    "facilitiesPath" : matsimFolder + "/output_facilities.xml.gz",
                    "filter" : "*",
                    "outTableName" : "ACTIVITIES"
            ]);
            ChoseReceiversFromActivities.choseReceiversFromActivities(connection);
        }

        if (doCalculateNoisePropagation) {
            Create0dBSourceFromRoads.create0dBSourceFromRoads(connection);
            CalculateNoiseMapFromSource.calculateNoiseMap(connection);
        }

        def timeStrings = (timeSlice == "hour") ? hourTimeStrings : quarterHourTimeStrings;
        // def timeStrings = ["0_1"]

        if (doCalculateRoadEmission) {
            for (timeString in timeStrings) {
                CreateRoadsFromTimeString.createRoadsFromTimeString(connection, [
                        "roadsTableName" : "MATSIM_ROADS",
                        "statsTableName" : "MATSIM_ROADS_STATS",
                        "timeString" : timeString,
                        "outTableName" : "ROADS_"+timeString
                ])
                CreateRoadEmissionFromTraffic.createRoadEmissionFromTraffic(connection, [
                        "tableRoads": "ROADS_" + timeString,
                        "outTable": "ROADS_" + timeString + "_LW"
                ])
            }
        }

        if (doCalculateNoiseMap) {
            for (timeString in timeStrings) {
                CalculateNoiseMapFromAttenuation.calculateNoiseMap(connection, [
                        "matsimRoads": "MATSIM_ROADS",
                        "timeString": timeString,
                        "attenuationTable": "LDAY_GEOM",
                        "outTableName"    : "RESULT_GEOM_" + timeString,
                ])
                CalculateMapDifference.calculateDifference(connection, [
                        "mainMapTable" : "ALT_RESULT_GEOM_" + timeString,
                        "secondMapTable" : "RESULT_GEOM_" + timeString,
                        "invert" : true,
                        "outTable" : "DIFF_RESULT_GEOM_" + timeString,
                ])
            }
        }

        if (doExportResults) {
            for (timeString in timeStrings) {
                ExportTable.exportTable(connection, [
                        "tableToExport": "RESULT_GEOM_" + timeString,
                        "exportPath"   : resultsFolder + "/RES_" + timeString + ".geojson"
                ])
                ExportTable.exportTable(connection, [
                        "tableToExport": "ALT_RESULT_GEOM_" + timeString,
                        "exportPath"   : resultsFolder + "/ALT_RES_" + timeString + ".geojson"
                ])
                ExportTable.exportTable(connection, [
                        "tableToExport": "DIFF_RESULT_GEOM_" + timeString,
                        "exportPath"   : resultsFolder + "/DIFF_RES_" + timeString + ".geojson"
                ])
            }
        }
        if (doCalcuateExposure) {
            CalculateMatsimAgentExposure.calculateMatsimAgentExposure(connection, [
                    "folder" : matsimFolder,
                    "outTableName" : "AGENTS",
                    "dataTablePrefix": "RESULT_GEOM_",
                    "timeSlice": timeSlice // DEN, hour, quarter
            ])
        }

        connection.close()
    }
}

