package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.noise_planet.noisemodelling.emission.railway.Railway
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters
import org.noise_planet.noisemodelling.wps.Acoustic_Tools.Create_Isosurface;
import org.noise_planet.noisemodelling.wps.Dynamic.RailWayNetworkFusion
import org.noise_planet.noisemodelling.wps.Dynamic.TrainNetworkParameters
import org.noise_planet.noisemodelling.wps.Dynamic.TrainRailwayPosition
import org.noise_planet.noisemodelling.wps.Dynamic.TrainSourcesFromPosition
import org.noise_planet.noisemodelling.wps.Experimental.DynamicTrainFromAADTTraffic;
import org.noise_planet.noisemodelling.wps.Geometric_Tools.Set_Height
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table;
import org.noise_planet.noisemodelling.wps.Import_and_Export.Import_File;
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_train_source
import org.noise_planet.noisemodelling.wps.Receivers.Delaunay_Grid


class TestDynamicRail extends JdbcTestCase {

    /**
    * Train tests
     */
    void testDynamicTrainGeneration() {
        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/Rail/TrainTrafficSource/RAILS_GEOM.geojson").getPath()])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/Rail/TrainTrafficSource/RAILS_TRAFFIC.csv").getPath()])

        def columns  = JDBCUtilities.getColumnNames(connection, "RAILS_TRAFFIC")

        new DynamicTrainFromAADTTraffic().exec(connection,
                [railsGeometries: "RAILS_GEOM",
                  railsTraffic: "RAILS_TRAFFIC"])
    }

    /**
     * Test Linear position of specific train along the rail. This position depends on speed.
     */
    void testDynamicTrainSourcesInterpolation() {
        new TrainRailwayPosition().exec(connection, [
                railwayGeom: [[0.0, 0.0, 0.0],[1000.0, 0.0, 0.0]],
                fieldTrainset: "TGVSE-10U2",
                speedSet: 350,
                idSection: 1,
                integrationTimeSet: 0.125,
                timeStartSet: 1734297900,
                nameFile: "vehicle/vehicleTestDynamicTrainSourcesInterpolation",
        ])
    }

    /**
     * Test Creation of specific Railway Network
     */
    void testTrainRailWayNetwork() {
        new TrainNetworkParameters().exec(connection, [
                railwayGeom: [[0.0, 0.0, 0.0],[1000.0, 0.0, 0.0]],
                idSection: 1,
                nTrack: 1,
                speedTrack: 300,
                trackTrans: 5,
                railRoughn: 2,
                impactNois: 0,
                curvature: 0,
                bridgeTran: 0,
                speedComme: 300,
                isTunnel: false,
        ])
    }

    /**
     * Test Fusion of two Railway Network
     */
    void testFusionGeojson() {
        new RailWayNetworkFusion().exec(connection, [
                file1Path: "Dynamic/Rail/TrainDynamicTest/Cas1/vehicleCasTest1.geojson",
                file2Path: "Dynamic/Rail/TrainDynamicTest/Cas2/vehicleCasTest2.geojson",
                nameFile: "Dynamic/Rail/TrainExport/Fusion/test_testFusionGeojson"
        ])
    }

    /**
     * Test the generation of multiple wagons sources from engine train position
     */
    void testDynamicTrainSourcesPlacement() {
        new Import_File().exec(connection, [
                pathFile : TestTutorials.class.getResource("Dynamic/Rail/TrainDynamicTest/pointTrainDynamic.geojson").getPath(),
                "inputSRID": "32635",
                tableName : "pointTrainDynamic"])
        new Import_File().exec(connection, [
                pathFile : TestTutorials.class.getResource("Dynamic/Rail/TrainDynamicTest/train_network_32635.geojson").getPath(),
                "inputSRID": "32635",
                tableName : "train_network_32635"])


        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition: "pointTrainDynamic",
                railwayGeometries: "train_network_32635",
                fieldTrainset: "train_set",
                fieldTrainId: "train_id",
                fieldTimeStep: "timestep",
                trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossosSNCF_2022.json").toString(),
                trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2022.json").toString()
        ])


        // Check output table content
        def sql = new Sql(connection)

        def cols = sql.rows("SELECT MIN(PERIOD::long) min_period, MAX(PERIOD::long) max_period FROM SOURCES_EMISSION")[0]
        assertEquals(1734297901, cols["min_period"])
        assertEquals(1734297955, cols["max_period"])

    }

    /**
    * TEST Cas1 : Network 1km size with 4 microphone position orthogonally in center of railway at 7.5m / 25m / 250m / 500m and 1.5m height
    * Comparison of signatures for each frequency between reference results from OC and NM
    * Train setting : Fret at 200km/h
    * Railway setting : "trackTrans": 7,"railRoughn": 1,"impactNois": 1,"curvature": 0,"bridgeTran": 0
    */
    void testDynamicIndividualTrainCas1() {

        def basePath = "Dynamic/Rail/TrainDynamicTest/Cas1/studyCase1Mic"
        // Exécution pour chaque microphone
        (1..4).each { i ->
            def filePath = TestDatabaseManager.getResource("${basePath}${i}.csv").getPath()
            new Import_File().exec(connection, [
                    "pathFile" : filePath,
                    "tableName": "Mic${i}"
            ])
        }

        new Import_File().exec(connection,
                ["pathFile" : TestDatabaseManager.getResource("Dynamic/Rail/TrainDynamicTest/testBati.geojson").getPath(),
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        new Import_File().exec(connection, [
                pathFile   : TestDynamic.getResource("Dynamic/Rail/TrainDynamicTest/receiverTest.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "RECEIVERS"])

        new Import_File().exec(connection, [
                pathFile   : TestDynamic.getResource("Dynamic/Rail/TrainDynamicTest/Cas1/vehicleCasTest1.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "vehicle"])

        new Import_File().exec(connection, [
                pathFile   : TestDynamic.getResource("Dynamic/Rail/TrainDynamicTest/Cas1/railTrackCasTest1.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "rail_track"])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition       : "vehicle",
                railwayGeometries    : "rail_track",
                fieldTrainset        : "train_set",
                fieldTrainId         : "train_id",
                fieldTimeStep        : "timestep",
                trainTrainsetData    : "RailwayTrainsets.json",
                trainVehicleData     : "RailwayVehiclesCnossosSNCF_2022.json",
                trainCoefficientsData: "RailwayCnossosSNCF_2022.json"
        ])

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_train_source().exec(connection,
                ["tableBuilding"                  : "BUILDINGS",
                 "tableSources"                   : "SOURCES_GEOM",
                 "tableSourcesEmission"           : "SOURCES_EMISSION",
                 "selectSource"                   : "ALL",
                 "tableReceivers"                 : "RECEIVERS",
                 "maxError"                       : 0.0,
                 "confFavorableOccurrencesDefault": "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0",
                 "confTemperature"                : 20,
                 "confMaxSrcDist"                 : 1000,
                 "confReflOrder"                  : 0,
                 "paramWallAlpha"                 : 1,
                 "confDiffHorizontal"             : false,
                 "confDiffVertical"               : false,
                 "confExportSourceId"             : false
                ])

        double[] dBA = [-30.2, -26.2, -22.5, -19.1, -16.1, -13.4, -10.9, -8.6, -6.6, -4.8,
                        -3.2, -1.9, -0.8, 0, 0.6, 1, 1.2, 1.3, 1.2, 1, 0.5, -0.1, -1.1, -2.5]

        def tiersOctaveFrequencies = [
                50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000,
                1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000
        ]
        def sql = new Sql(connection)
        (1..4).each { i ->
            (0..23).each { n ->
                try {
                    def freq = tiersOctaveFrequencies[n]
                    def queryRef = "SELECT (F${freq}HZ::DOUBLE) FROM MIC${i}"
                    def queryResult = "SELECT (HZ${freq}) FROM RECEIVERS_LEVEL WHERE IDRECEIVER=${i}"

                    def dataRef = sql.rows(queryRef.toString()).collect {
                        it.values()[0] }
                    def dataResult = sql.rows(queryResult.toString()).collect {
                        it.values()[0] + dBA[n]}
                    (0..<1439).each { id ->
                        assertEquals(
                                "Fail for MIC${i} at ${freq} Hz id ${id+1}",
                                dataRef[id+1] as double, dataResult[id] as double,0.1
                        )
                    }
                } catch (Exception e) {
                    println "Error for MIC${i} at ${tiersOctaveFrequencies[n - 1]} Hz : ${e.message}"
                }
            }
        }
    }

    /**
     * TEST Cas1 : Network 1km size with 4 microphone position orthogonally in center of railway at 7.5m / 25m / 250m / 500m and 1.5m height
     * Comparison of signatures for each frequency between reference results from OC and NM
     * Train setting : TGVSE-10U2 at 300km/h
     * Railway setting : "trackTrans": 4,"railRoughn": 2,"impactNois": 0,"curvature": 0,"bridgeTran": 0
     */
    void testDynamicIndividualTrainCas2() {
        def basePath = "Dynamic/Rail/TrainDynamicTest/Cas2/studyCase2Mic"
        // Exécution pour chaque microphone
        (1..4).each { i ->
            def filePath = TestDatabaseManager.getResource("${basePath}${i}.csv").getPath()
            new Import_File().exec(connection, [
                    "pathFile": filePath,
                    "tableName": "Mic${i}"
            ])
        }

        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/Rail/TrainDynamicTest/testBati.geojson").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/Rail/TrainDynamicTest/receiverTest.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "RECEIVERS"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/Rail/TrainDynamicTest/Cas2/vehicleCasTest2.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "vehicle"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/Rail/TrainDynamicTest/Cas2/railTrackCasTest2.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "rail_track"])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition       : "vehicle",
                railwayGeometries    : "rail_track",
                fieldTrainset        : "train_set",
                fieldTrainId         : "train_id",
                fieldTimeStep        : "timestep",
                trainTrainsetData    : "RailwayTrainsets.json",
                trainVehicleData     : "RailwayVehiclesCnossosSNCF_2022.json",
                trainCoefficientsData: "RailwayCnossosSNCF_2022.json"
        ])

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_train_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "SOURCES_GEOM",
                 "tableSourcesEmission" : "SOURCES_EMISSION",
                 "selectSource":"ALL",
                 "tableReceivers": "RECEIVERS",
                 "maxError" : 0.0,
                 "confFavorableOccurrencesDefault"  :"0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 0",
                 "confTemperature":20,
                 "confMaxSrcDist" : 1000,
                 "confReflOrder" : 0,
                 "paramWallAlpha" : 1,
                 "confDiffHorizontal" : false,
                 "confDiffVertical" : false,
                 "confExportSourceId": false
                ])

        double[] dBA = [-30.2, -26.2, -22.5, -19.1, -16.1, -13.4, -10.9, -8.6, -6.6, -4.8,
                        -3.2, -1.9, -0.8, 0, 0.6, 1, 1.2, 1.3, 1.2, 1, 0.5, -0.1, -1.1, -2.5]

        def tiersOctaveFrequencies = [
                50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000,
                1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000
        ]
        def sql = new Sql(connection)
        (1..4).each { i ->
            (0..23).each { n ->
                try {
                    def freq = tiersOctaveFrequencies[n]
                    def queryRef = "SELECT (F${freq}HZ::DOUBLE) FROM MIC${i}"
                    def queryResult = "SELECT (HZ${freq}) FROM RECEIVERS_LEVEL WHERE IDRECEIVER=${i}"

                    def dataRef = sql.rows(queryRef.toString()).collect {
                        it.values()[0] }
                    def dataResult = sql.rows(queryResult.toString()).collect {
                        it.values()[0] + dBA[n]}
                    (24..<959).each { id ->
                        assertEquals(
                                "Fail for MIC${i} at ${freq} Hz - id ${id+1}",
                                dataRef[id+1] as double, dataResult[id] as double,0.1
                        )
                    }
                } catch (Exception e) {
                    println "Error for MIC${i} at ${tiersOctaveFrequencies[n - 1]} Hz & : ${e.message}"
                }
            }
        }
    }
}
