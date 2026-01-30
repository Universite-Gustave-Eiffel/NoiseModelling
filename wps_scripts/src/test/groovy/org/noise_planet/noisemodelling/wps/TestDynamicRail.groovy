package org.noise_planet.noisemodelling.wps

import groovy.sql.Sql
import org.h2gis.utilities.JDBCUtilities
import org.noise_planet.noisemodelling.emission.railway.Railway
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
    /**
    * Train tests
     */
    void testDynamicTrainGeneration() {
        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainTrafficSource/RAILS_GEOM.geojson").getPath()])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainTrafficSource/RAILS_TRAFFIC.csv").getPath()])

        def columns  = JDBCUtilities.getColumnNames(connection, "RAILS_TRAFFIC")

        new DynamicTrainFromAADTTraffic().exec(connection,
                [railsGeometries: "RAILS_GEOM",
                  railsTraffic: "RAILS_TRAFFIC"])
    }
    void testDynamicTrainSourcesInterpolation() {
        new TrainRailwayPosition().exec(connection, [
                railwayGeom: [[0.0, 0.0, 0.0],[1000.0, 0.0, 0.0]],
                fieldTrainset: "TGVSE-10U2",
                speedSet: 350,
                idSection: 1,
                integrationTimeSet: 0.125,
                timeStartSet: 1734297900,
                nameFile: "vehicle/vehicleCasR4E",
        ])
    }
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
    void testFusionGeojson() {
        new RailWayNetworkFusion().exec(connection, [
                file1Path: "Dynamic/TrainExport/test_Fret_200.geojson",
                file2Path: "Dynamic/TrainExport/test_TGVSE10U2_300.geojson",
                nameFile: "Dynamic/TrainExport/test_TrainTraffic_Fusion"
        ])
    }




    /**
     * Test the generation of multiple wagons sources from engine train position
     */
    void testDynamicTrainSourcesPlacement() {
        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/R4E/vehicleR4E.geojson").getPath()])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/R4E/railTrackR4E.geojson").getPath()])


        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition: "pointTrainDynamic",
                railwayGeometries: "train_network_32635",
                fieldTrainset: "train_set",
                fieldTrainId: "train_id",
                fieldTimeStep: "timestep",
                trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossos.json").toString(),
                trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2021.json").toString()
        ])


        // Check output table content
        def sql = new Sql(connection)

        def cols = sql.rows("SELECT MIN(PERIOD::long) min_period, MAX(PERIOD::long) max_period FROM SOURCES_EMISSION")[0]
        assertEquals(1734297901, cols["min_period"])
        assertEquals(1734297955, cols["max_period"])

    }

    void testDynamicIndividualTrainCas1() {
        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/TrainDynamicTest/testBati.geojson").getPath() ,
                     "inputSRID": "32635",
                 "tableName": "buildings"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainDynamicTest/receiverTest.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "RECEIVERS"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/vehicle/vehicleCasTest1.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "vehicle"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/rail_track/railTrackCasTest1.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "rail_track"])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition: "vehicle",
                railwayGeometries: "rail_track",
                fieldTrainset: "train_set",
                fieldTrainId: "train_id",
                fieldTimeStep: "timestep",
                trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossos.json").toString(),
                trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2021.json").toString()
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

    }
    void testDynamicIndividualTrainCas2() {
        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/TrainDynamicTest/testBati.geojson").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainDynamicTest/receiverTest.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "RECEIVERS"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/vehicle/vehicleCasTest2.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "vehicle"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/rail_track/railTrackCasTest2.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "rail_track"])


        // TODO prevoir le ENRICH_DEM_with_rail

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition: "vehicle",
                railwayGeometries: "rail_track",
                fieldTrainset: "train_set",
                fieldTrainId: "train_id",
                fieldTimeStep: "timestep",
                trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossos.json").toString(),
                trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2021.json").toString()
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
    }

    /**
     * analyse R4E
     */
    void testDynamicTrainSourcesInterpolationR4E() {
        def vehUse = ["TGV-A-12U1", "TGV-A-12U2", "TGV-D-10U1", "TGV-D-10U2"]
        def speedUse = (200..350).step(5).toList()
        def railwayGeomEO = [
                [546521.693513195612468, 6777500.572052604518831, 0.0],
                [546477.599999999976717, 6777514.200000000186265, 0.0],
                [546391.099999999976717, 6777539.799999999813735, 0.0],
                [546282.0, 6777573.400000000372529, 0.0],
                [546253.599999999976717, 6777582.099999999627471, 0.0],
                [546103.099999999976717, 6777630.900000000372529, 0.0],
                [545999.0, 6777666.599999999627471, 0.0],
                [545876.800000000046566, 6777708.299999999813735, 0.0],
                [545740.5, 6777756.700000000186265, 0.0],
                [545597.900000000023283, 6777806.900000000372529, 0.0],
                [545481.900000000023283, 6777847.599999999627471, 0.0],
                [545446.900000000023283, 6777859.299999999813735, 0.0],
                [545380.199999999953434, 6777881.700000000186265, 0.0],
                [545340.0, 6777895.400000000372529, 0.0],
                [545219.0, 6777935.0, 0.0],
                [545116.0, 6777965.400000000372529, 0.0],
                [545034.400000000023283, 6777989.099999999627471, 0.0],
                [544938.699999999953434, 6778013.900000000372529, 0.0],
                [544836.199999999953434, 6778040.099999999627471, 0.0],
                [544835.56068417429924, 6778040.249127665534616, 0.0]
        ]

        def railwayGeomOE = [
                [544835.56068417429924, 6778040.249127665534616, 0.0],
                [544836.199999999953434, 6778040.099999999627471, 0.0],
                [544938.699999999953434, 6778013.900000000372529, 0.0],
                [545034.400000000023283, 6777989.099999999627471, 0.0],
                [545116.0, 6777965.400000000372529, 0.0],
                [545219.0, 6777935.0, 0.0],
                [545340.0, 6777895.400000000372529, 0.0],
                [545380.199999999953434, 6777881.700000000186265, 0.0],
                [545446.900000000023283, 6777859.299999999813735, 0.0],
                [545481.900000000023283, 6777847.599999999627471, 0.0],
                [545597.900000000023283, 6777806.900000000372529, 0.0],
                [545740.5, 6777756.700000000186265, 0.0],
                [545876.800000000046566, 6777708.299999999813735, 0.0],
                [545999.0, 6777666.599999999627471, 0.0],
                [546103.099999999976717, 6777630.900000000372529, 0.0],
                [546253.599999999976717, 6777582.099999999627471, 0.0],
                [546282.0, 6777573.400000000372529, 0.0],
                [546391.099999999976717, 6777539.799999999813735, 0.0],
                [546477.599999999976717, 6777514.200000000186265, 0.0],
                [546521.693513195612468, 6777500.572052604518831, 0.0]
        ]

        vehUse.each { veh ->
            speedUse.each { speed ->
                // Aller (A->B)
                new TrainRailwayPosition().exec(connection, [
                        railwayGeom: railwayGeomEO,
                        fieldTrainset: veh,
                        speedSet: speed,
                        idSection: 1,
                        integrationTimeSet: 0.125,
                        timeStartSet: 1734297900,
                        nameFile: "vehicle/R4E/${veh}_EO_${speed}.geojson",
                ])

                // Retour (B->A)
                new TrainRailwayPosition().exec(connection, [
                        railwayGeom: railwayGeomOE,
                        fieldTrainset: veh,
                        speedSet: speed,
                        idSection: 1,
                        integrationTimeSet: 0.125,
                        timeStartSet: 1734297900,
                        nameFile: "vehicle/R4E/${veh}_OE_${speed}.geojson",
                ])
            }
        }
    }
    void testTrainRailWayNetworkR4E() {
        new TrainNetworkParameters().exec(connection, [
                railwayGeom: [ [ 546521.693513195612468, 6777500.572052604518831, 0.0],
                               [ 546477.599999999976717, 6777514.200000000186265, 0.0],
                               [ 546391.099999999976717, 6777539.799999999813735, 0.0],
                               [ 546282.0, 6777573.400000000372529, 0.0],
                               [ 546253.599999999976717, 6777582.099999999627471, 0.0],
                               [ 546103.099999999976717, 6777630.900000000372529, 0.0],
                               [ 545999.0, 6777666.599999999627471, 0.0],
                               [ 545876.800000000046566, 6777708.299999999813735, 0.0],
                               [ 545740.5, 6777756.700000000186265, 0.0],
                               [ 545597.900000000023283, 6777806.900000000372529, 0.0],
                               [ 545481.900000000023283, 6777847.599999999627471, 0.0],
                               [ 545446.900000000023283, 6777859.299999999813735, 0.0],
                               [ 545380.199999999953434, 6777881.700000000186265, 0.0],
                               [ 545340.0, 6777895.400000000372529, 0.0],
                               [ 545219.0, 6777935.0, 0.0],
                               [ 545116.0, 6777965.400000000372529, 0.0],
                               [ 545034.400000000023283, 6777989.099999999627471, 0.0],
                               [ 544938.699999999953434, 6778013.900000000372529, 0.0],
                               [ 544836.199999999953434, 6778040.099999999627471, 0.0],
                               [ 544835.56068417429924, 6778040.249127665534616, 0.0] ] ,
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
                nameFile: "rail_track/R4E/R4E_rail_EO",
        ])
        new TrainNetworkParameters().exec(connection, [
                railwayGeom:[ [ 544835.56068417429924, 6778040.249127665534616, 0.0 ],
                              [ 544836.199999999953434, 6778040.099999999627471, 0.0 ],
                              [ 544938.699999999953434, 6778013.900000000372529, 0.0 ],
                              [ 545034.400000000023283, 6777989.099999999627471, 0.0 ],
                              [ 545116.0, 6777965.400000000372529, 0.0 ],
                              [ 545219.0, 6777935.0, 0.0 ],
                              [ 545340.0, 6777895.400000000372529, 0.0 ],
                              [ 545380.199999999953434, 6777881.700000000186265, 0.0 ],
                              [ 545446.900000000023283, 6777859.299999999813735, 0.0 ],
                              [ 545481.900000000023283, 6777847.599999999627471, 0.0 ],
                              [ 545597.900000000023283, 6777806.900000000372529, 0.0 ],
                              [ 545740.5, 6777756.700000000186265, 0.0 ],
                              [ 545876.800000000046566, 6777708.299999999813735, 0.0 ],
                              [ 545999.0, 6777666.599999999627471, 0.0 ],
                              [ 546103.099999999976717, 6777630.900000000372529, 0.0 ],
                              [ 546253.599999999976717, 6777582.099999999627471, 0.0 ],
                              [ 546282.0, 6777573.400000000372529, 0.0 ],
                              [ 546391.099999999976717, 6777539.799999999813735, 0.0 ],
                              [ 546477.599999999976717, 6777514.200000000186265, 0.0 ],
                              [ 546521.693513195612468, 6777500.572052604518831, 0.0 ] ],
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
                nameFile: "rail_track/R4E/R4E_rail_OE",
        ])
    }
    void testDynamicR4E(){
        def vehUse = ["TGV-A-12U1", "TGV-A-12U2", "TGV-D-10U1", "TGV-D-10U2"]
        def speedUse = (200..350).step(5).toList()
        def orientation = ["EO", "OE"]

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/R4E/R4E_receivers.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "RECEIVERS"])

        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/R4E/R4E_buildings.geojson").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        orientation.each { direction ->
            def railfile = "Dynamic/R4E/R4E_rail_${direction}.geojson"
            vehUse.each { veh ->
                speedUse.each { speed ->
                    def vehfile = "Dynamic/R4E/veh/${veh}_${direction}_${speed}.geojson"
                    def exportSourceGeom = "src/test/resources/org/noise_planet/noisemodelling/wps/Dynamic/R4E/Resultats/Sources/SOURCES_GEOM_R4E_${veh}_${direction}_${speed}.csv"
                    def exportSourceEmission = "src/test/resources/org/noise_planet/noisemodelling/wps/Dynamic/R4E/Resultats/Sources/SOURCES_EMISSION_R4E_${veh}_${direction}_${speed}.csv"
                    def exportReceiversLevel = "src/test/resources/org/noise_planet/noisemodelling/wps/Dynamic/R4E/Resultats/Receivers/receiversResultsR4E_${veh}_${direction}_${speed}.csv"
                    new Import_File().exec(connection, [
                            pathFile: TestDynamic.getResource(vehfile).getPath(),
                            "inputSRID": "32635",
                            "tableName": "vehicle"])

                    new Import_File().exec(connection, [
                            pathFile: TestDynamic.getResource(railfile).getPath(),
                            "inputSRID": "32635",
                            "tableName": "rail_track"])

                    new TrainSourcesFromPosition().exec(connection, [
                            trainsPosition: "vehicle",
                            railwayGeometries: "rail_track",
                            fieldTrainset: "train_set",
                            fieldTrainId: "train_id",
                            fieldTimeStep: "timestep",
                            trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                            trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossos.json").toString(),
                            trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2021.json").toString()
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
                    new Export_Table().exec(connection, [exportPath:exportSourceGeom,
                                                         tableToExport:"SOURCES_GEOM"])
                    new Export_Table().exec(connection, [exportPath:exportSourceEmission,
                                                         tableToExport:"SOURCES_EMISSION"])
                    new Export_Table().exec(connection,["exportPath"   :exportReceiversLevel,
                                                        "tableToExport"   : "RECEIVERS_LEVEL",])
                }
            }
        }

    }

    void testDynamicIndividualTrainSimple() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/TrainDynamicTest/testBati.geojson").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainDynamicTest/receiverTest.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "RECEIVERS"])

        new Import_File().exec(connection, [
//                pathFile: TestDynamic.getResource("Dynamic/TrainDynamicTest/PointFastTrain.geojson").getPath(),
//                pathFile: TestDynamic.getResource("Dynamic/TrainDynamicTest/SimplePointFastTrain.geojson").getPath(),
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/test_TrainTraffic_Fusion.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "vehicle"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/test_RailwayNetwork_Fusion.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "rail_track"])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition: "vehicle",
                railwayGeometries: "rail_track",
                fieldTrainset: "train_set",
                fieldTrainId: "train_id",
                fieldTimeStep: "timestep",
                trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossos.json").toString(),
                trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2021.json").toString()
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
                 //                 "confRaysName":"RaysExport",
                 "confMaxSrcDist" : 1000,
                 "confReflOrder" : 0,
                 "paramWallAlpha" : 1,
                 "confDiffHorizontal" : false,
                 "confDiffVertical" : false,
                 "confExportSourceId": false
                ])
    }

    void testDynamicDoubleTrain() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/TrainDynamicTest/testBati.geojson").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainDynamicTest/receiverTest.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "RECEIVERS"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/test_TrainTraffic_Fusion.geojson").getPath(),
//                pathFile: TestDynamic.getResource("Dynamic/TrainExport/test_FRET_200.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "vehicle"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainExport/test_RailwayNetwork_Fusion.geojson").getPath(),
                "inputSRID": "32635",
                "tableName": "rail_track"])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition: "vehicle",
                railwayGeometries: "rail_track",
                fieldTrainset: "train_set",
                fieldTrainId: "train_id",
                fieldTimeStep: "timestep",
                trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossos.json").toString(),
                trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2021.json").toString()
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

    }

    void testDynamicIndividualTrainTutorial() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/buildings_nm_ready_pop_heights.shp").getPath() ,
                 "inputSRID": "32635",
                 "tableName": "buildings"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainSourceDistribution/pointTrainDynamic.geojson").getPath(),
                "tableName": "vehicle"])

        new Import_File().exec(connection, [
                pathFile: TestDynamic.getResource("Dynamic/TrainSourceDistribution/train_network_32635.geojson").getPath(),
                "tableName": "rail_track"])


        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new TrainSourcesFromPosition().exec(connection, [
                trainsPosition: "vehicle",
                //sourceRelativePosition: Railway.class.getResource("RailwaySourcePosition.json").toString(),
                railwayGeometries: "rail_track",
                fieldTrainset: "train_set",
                fieldTrainId: "train_id",
                fieldTimeStep: "timestep",
                trainTrainsetData: Railway.class.getResource("RailwayTrainsets.json").toString(),
                trainVehicleData: Railway.class.getResource("RailwayVehiclesCnossos.json").toString(),
                trainCoefficientsData: Railway.class.getResource("RailwayCnossosSNCF_2021.json").toString()
        ])

        new Delaunay_Grid().exec(connection, ["buildingTableName"  : "buildings",
                                              "sourcesTableName"   : "rail_track",
                                              "maxArea" : 500
                                            ]);

        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                  "height": 1.5
                ])

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_train_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "SOURCES_GEOM",
                 "tableSourcesEmission" : "SOURCES_EMISSION",
                 "tableReceivers": "RECEIVERS",
                 "maxError" : 0.0,
                 "confMaxSrcDist" : 500,
                 "confReflOrder" : 0,
                 "confDiffHorizontal" : true,
                 "confDiffVertical" : true,
                 "confExportSourceId": false
                ])

        new Create_Isosurface().exec(connection,
                [resultTable: "RECEIVERS_LEVEL",
                 smoothCoefficient : 0])

    }

}
