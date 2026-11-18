package org.noise_planet.noisemodelling.scripts

import groovy.sql.Sql
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.utilities.JDBCUtilities
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters
import org.noise_planet.noisemodelling.scripts.Acoustic_Tools.Create_Isosurface;
import org.noise_planet.noisemodelling.scripts.Acoustic_Tools.DynamicIndicators;
import org.noise_planet.noisemodelling.scripts.Database_Manager.Add_Primary_Key;
import org.noise_planet.noisemodelling.scripts.Dynamic.Flow_2_Noisy_Vehicles;
import org.noise_planet.noisemodelling.scripts.Dynamic.IndividualVehicleEmissionProcessData;
import org.noise_planet.noisemodelling.scripts.Dynamic.Ind_Vehicles_2_Noisy_Vehicles;
import org.noise_planet.noisemodelling.scripts.Dynamic.Noise_From_Attenuation_Matrix;
import org.noise_planet.noisemodelling.scripts.Dynamic.Point_Source_From_Network
import org.noise_planet.noisemodelling.scripts.Dynamic.Split_Sources_Period;
import org.noise_planet.noisemodelling.scripts.Geometric_Tools.Set_Height
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Export_Table;
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_File;
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_OSM;
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.scripts.Receivers.Regular_Grid


class TestDynamic extends JdbcTestCase {

    /**
     * as SUMO or SYMUVIA or Drone input
     */
    @Test
    void testDynamicIndividualVehiclesTutorial() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/buildings_nm_ready_pop_heights.shp").getPath() ,
                "inputSRID": "32635",
                "tableName": "buildings"], new EmptyProgressVisitor())

        // Import the receivers (or generate your set of receivers using Regular_Grid script for example)
        new Import_File().exec(connection,
                ["pathFile" : TestDatabaseManager.getResource("Dynamic/receivers_python_method0_50m_pop.shp").getPath() ,
                "inputSRID": "32635",
                "tableName": "receivers"])

        // Set the height of the receivers
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                "height": 1.5
                ])

        // Import the road network
        new Import_File().exec(connection,
                ["pathFile" :TestDatabaseManager.getResource("Dynamic/network_tartu_32635_.geojson").getPath() ,
                "inputSRID": "32635",
                "tableName": "network_tartu"])

        // (optional) Add a primary key to the road network
        new Add_Primary_Key().exec(connection,
                ["pkName" :"PK",
                "tableName": "network_tartu"])

        // Import the vehicles trajectories
        new Import_File().exec(connection,
                ["pathFile" : TestDatabaseManager.getResource("Dynamic/SUMO.geojson").getPath() ,
                "inputSRID": "32635",
                "tableName": "vehicle"])

        // Create point sources from the network every 10 meters. This point source will be used to compute the noise attenuation level from them to each receiver.
        // The created table will be named SOURCES_GEOM
        new Point_Source_From_Network().exec(connection,
                ["tableNetwork": "network_tartu",
                 "gridStep" : 10
                ])

        // Create a table with the noise level from the vehicles and snap the vehicles to the discretized network
        new Ind_Vehicles_2_Noisy_Vehicles().exec(connection,
                ["tableSourceGeom" : "SOURCES_GEOM",
                 "tableVehicles": "vehicle",
                 "distance2snap" : 30,
                 "tableFormat" : "SUMO"])

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                "tableSources"   : "SOURCES_GEOM",
                "tableReceivers": "RECEIVERS",
                "maxError" : 0.0,
                "confMaxSrcDist" : 300,
                "confReflOrder" : 0,
                "confDiffHorizontal" : false,
                "confExportSourceId": true,
                ])


        // Compute the noise level from the moving vehicles to the receivers
        // the output table is called here LT_GEOM and contains the time series of the noise level at each receiver
        new Noise_From_Attenuation_Matrix().exec(connection,
                ["lwTable"   : "SOURCES_EMISSION",
                "attenuationTable"   : "RECEIVERS_LEVEL",
                "outputTable"   : "LT_GEOM"
                ])

        def columnNames = JDBCUtilities.getColumnNames(connection, "LT_GEOM")
        assertTrue(columnNames.containsAll(Arrays.asList("PERIOD", "THE_GEOM")))

        // This step is optional, it compute the LEQA, LEQ, L10, L50 and L90 at each receiver from the table LT_GEOM
        String res = new DynamicIndicators().exec(connection,
                ["tableName"   : "LT_GEOM",
                "columnName"   : "LAEQ",
                "outputTableName" : "INDICATORS"
                ])

        columnNames = JDBCUtilities.getColumnNames(connection, "INDICATORS")
        assertTrue(columnNames.containsAll(Arrays.asList("L90", "L50", "L10")))
    }


    /**
     * as OSM input
     */
    @Test
    void testDynamicFlowTutorialProbabilisticWithAttenuationMatrix() {

        // Import the road network (with predicted traffic flows) and buildings from an OSM file
        new Import_OSM().exec(connection, [
                "pathFile"      : TestImportExport.getResource("map.osm.gz").getPath(),
                "targetSRID"    : 2154,
                "ignoreGround"  : true,
                "ignoreBuilding": false,
                "ignoreRoads"   : false,
                "removeTunnels" : true
        ]);

        // Create a receiver grid
        new Regular_Grid().exec(connection,  [
                "fenceTableName": "ROADS",
                "delta"            : 25])

        // Set a height to the receivers at 1.5 m
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                  "height": 1.5
                ])

        // From the network with traffic flow to individual trajectories with associated Lw using the Probabilistic method
        // This method place randomly the vehicles on the network according to the traffic flow
        new Flow_2_Noisy_Vehicles().exec(connection,
                ["tableRoads": "ROADS",
                 "method": "PROBA",
                 "timestep": 1,
                 "gridStep" : 10,
                 "duration" : 60])

        // Compute the attenuation noise level from the network sources (SOURCES_GEOM) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "SOURCES_GEOM",
                 "tableReceivers": "RECEIVERS",
                 "confExportSourceId": true,
                 "maxError" : 0.0,
                 "confMaxSrcDist" : 800,
                 "confDiffHorizontal" : false
                ])

        // Compute the noise level from the moving vehicles to the receivers
        // the output table is called here LT_GEOM and contains the time series of the noise level at each receiver
        new Noise_From_Attenuation_Matrix().exec(connection,
                ["lwTable"   : "SOURCES_EMISSION",
                 "lwTable_sourceId": "IDSOURCE",
                 "attenuationTable": NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME,
                 "outputTable"   : "LT_GEOM"
                ])

        // This step is optional, it compute the L10, L50 and L90 at each receiver from the table LT_GEOM
        String res =new DynamicIndicators().exec(connection,
                ["tableName"   : "LT_GEOM",
                 "columnName"   : "LAEQ",
                 "outputTableName" : "INDICATORS"
                ])

        def columnNames = JDBCUtilities.getColumnNames(connection, "INDICATORS")
        assertTrue(columnNames.containsAll(Arrays.asList("L90", "L50", "L10")))
    }


    /**
     * as OSM input
     */
    @Test
    void testDynamicFlowTutorialProba() {

        // Import the road network (with predicted traffic flows) and buildings from an OSM file
        new Import_OSM().exec(connection, [
                "pathFile"      : TestImportExport.getResource("map.osm.gz").getPath(),
                "targetSRID"    : 2154,
                "ignoreGround"  : true,
                "ignoreBuilding": false,
                "ignoreRoads"   : false,
                "removeTunnels" : true
        ]);

        // Create a receiver grid
        new Regular_Grid().exec(connection,  [
                "fenceTableName": "ROADS",
                "delta"            : 25])

        // Set a height to the receivers at 1.5 m
        new Set_Height().exec(connection,
                [ "tableName":"RECEIVERS",
                "height": 1.5
                ])

        // From the network with traffic flow to individual trajectories with associated Lw using the Probabilistic method
        // This method place randomly the vehicles on the network according to the traffic flow
        new Flow_2_Noisy_Vehicles().exec(connection,
                ["tableRoads": "ROADS",
                "method": "PROBA",
                "timestep": 1,
                "gridStep" : 10,
                "duration" : 60])


        def expected = JDBCUtilities.getUniqueFieldValues(connection,
                "SOURCES_EMISSION", "PERIOD")

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                "tableSources"   : "SOURCES_GEOM",
                "tableSourcesEmission" : "SOURCES_EMISSION",
                "tableReceivers": "RECEIVERS",
                "maxError" : 2.0,
                "confMaxSrcDist" : 800,
                "confDiffHorizontal" : false
                ])

        def periods = JDBCUtilities.getUniqueFieldValues(connection,
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME, "PERIOD")


        assertEquals(expected.size(), periods.size())
        assertTrue(periods.containsAll(expected))

        // This step is optional, it compute the L10, L50 and L90 at each receiver from the table LT_GEOM
        String res =new DynamicIndicators().exec(connection,
                ["tableName"   : NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME,
                "columnName"   : "LAEQ",
                "outputTableName" : "INDICATORS"
                ])

        def columnNames = JDBCUtilities.getColumnNames(connection, "INDICATORS")
        assertTrue(columnNames.containsAll(Arrays.asList("L90", "L50", "L10")))
    }

    /**
     * as OSM input
     */
    @Test
    void testDynamicFlowTutorialPoisson(@TempDir File tutorialOutputFolder) {

        // Import the road network (with predicted traffic flows) and buildings from an OSM file
        new Import_OSM().exec(connection, [
                "pathFile"      : TestImportExport.getResource("map.osm.gz").getPath(),
                "targetSRID"    : 2154,
                "ignoreGround"  : true,
                "ignoreBuilding": false,
                "ignoreRoads"   : false,
                "removeTunnels" : true
        ]);

        // Export result table
        new Export_Table().exec(connection,
                [exportPath: new File(tutorialOutputFolder, "BUILDINGS.shp").absolutePath,
                 tableToExport: "BUILDINGS"])

        // Export result table
        new Export_Table().exec(connection,
                [exportPath: new File(tutorialOutputFolder, "ROADS.shp").absolutePath,
                 tableToExport: "ROADS"])

        // Create a receiver grid
        new Regular_Grid().exec(connection,  [
                "fenceTableName": "ROADS",
                "delta" : 15,
                "height": 1.5,
                "outputTriangleTable" : true])

        // From the network with traffic flow to individual trajectories with associated Lw using the Poisson method
        // This method place the vehicles on the network according to the traffic flow following a poisson law
        // It keeps a coherence in the time series of the noise level
        new Flow_2_Noisy_Vehicles().exec(connection,
                ["tableRoads": "ROADS",
                 "method"    : "TNP",
                 "timestep"  : 1,
                 "duration"  : 60,
                 "gridStep"  : 10])

        assertTrue(JDBCUtilities.tableExists(connection, "SOURCES_EMISSION"))
        assertTrue(JDBCUtilities.tableExists(connection, "SOURCES_GEOM"))

        def expected = JDBCUtilities.getUniqueFieldValues(connection,
                "SOURCES_EMISSION", "PERIOD")

        // Compute the attenuation noise level from the network sources (SOURCES_0DB) to the receivers
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"       : "BUILDINGS",
                 "tableSources"        : "SOURCES_GEOM",
                 "tableSourcesEmission": "SOURCES_EMISSION",
                 "tableReceivers"      : "RECEIVERS",
                 "maxError"            : 3.0,
                 "confMaxSrcDist"      : 200,
                 "confDiffHorizontal"  : true,
                 "confReflOrder"       : 0
                ])

        def periods = JDBCUtilities.getUniqueFieldValues(connection,
                NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME, "PERIOD")

        // Export result table
        new Export_Table().exec(connection,
                [exportPath: new File(tutorialOutputFolder, NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME+".shp").absolutePath,
                 tableToExport: NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME])

        // This step is optional, it compute the L10, L50 and L90 at each receiver from the table RECEIVERS_LEVEL
        String res = new DynamicIndicators().exec(connection,
                ["tableName"      : NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME,
                 "columnName"     : "LAEQ",
                 "outputTableName": "INDICATORS"
                ])

        def columnNames = JDBCUtilities.getColumnNames(connection, "INDICATORS")
        assertTrue(columnNames.containsAll(Arrays.asList("L90", "L50", "L10")))

        // Compute contouring noise map
        new Create_Isosurface().exec(connection,
                ["resultTable"      : NoiseMapDatabaseParameters.DEFAULT_RECEIVERS_LEVEL_TABLE_NAME,
                 "smoothCoefficient": 0])

        assertTrue(JDBCUtilities.tableExists(connection, "CONTOURING_NOISE_MAP"))

        // Export result table
        new Export_Table().exec(connection,
                [exportPath: new File(tutorialOutputFolder, "CONTOURING_NOISE_MAP.shp").absolutePath,
                 tableToExport: "CONTOURING_NOISE_MAP"])

        assertEquals(expected.size(), periods.size())
        assertTrue(periods.containsAll(expected))

    }



    /**
     * as MATSIM input
     */
    @Test
    void testDynamicFluctuatingFlowTutorial() {

        // Import Buildings for your study area
        new Import_File().exec(connection,
                ["pathFile" :  TestDatabaseManager.getResource("Dynamic/Z_EXPORT_TEST_BUILDINGS.geojson").getPath() ,
                "inputSRID": "2154",
                "tableName": "buildings"])

        // Import the road network
        new Import_File().exec(connection,
                ["pathFile" :TestDatabaseManager.getResource("Dynamic/Z_EXPORT_TEST_TRAFFIC.geojson").getPath() ,
                "inputSRID": "2154",
                "tableName": "ROADS"])

        // Create a receiver grid
        new Regular_Grid().exec(connection,  [
                "fenceTableName": "ROADS",
                "delta"            : 25,
                "height": 1.5])

        // From the network with traffic flow to individual trajectories with associated Lw using the Probabilistic method
        // This method place randomly the vehicles on the network according to the traffic flow
        new Split_Sources_Period().exec(connection,
                ["tableSourceDynamic": "ROADS",
                "sourceIndexFieldName" : "LINK_ID",
                "sourcePeriodFieldName" : "TIME"])

        new Road_Emission_from_Traffic().exec(connection,
                ["tableRoads": "SOURCES_EMISSION"])

        // Compute the noise level from the network sources for each time period
        new Noise_level_from_source().exec(connection,
                ["tableBuilding"   : "BUILDINGS",
                 "tableSources"   : "SOURCES_GEOM",
                 "tableSourcesEmission"   : "LW_ROADS",
                 "tableReceivers": "RECEIVERS",
                 "confDiffHorizontal" : true,
                 "confReflOrder"       : 0
                ])

        def columnNames = JDBCUtilities.getColumnNames(connection, "RECEIVERS_LEVEL")

        assertTrue(columnNames.containsAll(Arrays.asList("PERIOD", "LAEQ")))

    }

    /**
     * The PROBA method must emit, on average over time, the energy of the Q/v vehicles that the
     * traffic flow puts on the road
     */
    @Test
    void testDynamicFlowProbaMeanEmission() {
        // 1 km road, 1000 light and 200 heavy vehicles per hour at 50 km/h: 20 light and 4 heavy vehicles
        assertProbaMeanEmission(1000, 200, 50, 20, 4)
    }

    /**
     * Same check when the traffic puts more than one vehicle on each source point
     */
    @Test
    void testDynamicFlowProbaMeanEmissionDenseTraffic() {
        // 1 km road, 4000 light vehicles per hour at 20 km/h: 200 vehicles on about 100 source points
        assertProbaMeanEmission(4000, 0, 20, 200, 0)
    }

    void assertProbaMeanEmission(int lv, int hv, double speed, double nLight, double nHeavy) {
        Sql sql = new Sql(connection)
        sql.execute("CREATE TABLE ROADS(PK INT PRIMARY KEY, THE_GEOM GEOMETRY(LINESTRING, 2154), LV_D INT, LV_SPD_D DOUBLE, HGV_D INT, HGV_SPD_D DOUBLE)")
        sql.execute("INSERT INTO ROADS VALUES (1, ST_GeomFromText('LINESTRING(0 0, 1000 0)', 2154), " + lv + ", " + speed + ", " + hv + ", " + speed + ")")

        new Flow_2_Noisy_Vehicles().exec(connection,
                ["tableRoads": "ROADS",
                 "method"    : "PROBA",
                 "timestep"  : 1,
                 "gridStep"  : 10,
                 "duration"  : 3600])

        // time steps written by the script, the steps without any vehicle have no row
        int steps = 3599
        double energy = sql.firstRow("SELECT SUM(POWER(10, HZ1000 / 10)) FROM SOURCES_EMISSION")[0] as double
        double meanLevel = 10 * Math.log10(energy / steps)

        double lwLight = IndividualVehicleEmissionProcessData.vehicleSpectrum(speed, "1")[4]
        double lwHeavy = IndividualVehicleEmissionProcessData.vehicleSpectrum(speed, "3")[4]
        double expectedLevel = 10 * Math.log10(nLight * Math.pow(10, lwLight / 10) + nHeavy * Math.pow(10, lwHeavy / 10))

        assertEquals(expectedLevel, meanLevel, 0.5)
    }

}
