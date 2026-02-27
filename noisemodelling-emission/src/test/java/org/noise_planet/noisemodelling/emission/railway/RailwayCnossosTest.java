/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.railway;

import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayCnossos;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayVehicleCnossosParameters;
import org.noise_planet.noisemodelling.emission.utils.Utils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test the Railway model CNOSSOS as implemented in RailwayCnossos.java
 * @author Adrien Le Bellec - univ Gustave eiffel
 * @author Olivier Chiello, Univ Gustave Eiffel
 */

public class RailwayCnossosTest {
    private static final double EPSILON_TEST1 = 0.0001;
    RailwayCnossos railwayCnossos = new RailwayCnossos();

    @Test
    public void testUnknownVehicle() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesNMPB.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");


        String vehCat = "notsupported";

        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;

        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle

        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("EU");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        var t = assertThrows(IllegalArgumentException.class, () -> railwayCnossos.evaluate(vehicleParameters, trackParameters));

        assertTrue(t.getMessage().contains("not found must be one of"));
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_1() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossos_2015.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosEU_2020.json");

        double[] expectedValuesLWRolling = new double[]{46.62923937074308, 47.5930887825746, 49.48855395436553, 50.84525150627218, 48.290416823553656, 47.55991120199147, 48.36598621415334, 53.685023895294904, 55.17947382327178, 56.44358069598965, 57.381090582936295, 58.06233085323542, 59.82942366487711, 59.45435581560096, 56.541564191934896, 52.173989709214666, 54.51129722944201, 53.2570463370446, 51.118153521942574, 48.73174211849708, 49.13517759611298, 48.60046539576308, 48.58746317902302, 50.14566621392022};
        double[] expectedValuesLWTractionA = new double[]{46.8200805302231, 42.7200805302231, 40.5200805302231, 42.5200805302231, 40.7200805302231, 40.7200805302231, 40.9200805302231, 42.7200805302231, 42.5200805302231, 43.6200805302231, 43.5200805302231, 46.5200805302231, 43.1200805302231, 43.0200805302231, 43.0200805302231, 42.0200805302231, 42.0200805302231, 47.3200805302231, 40.4200805302231, 37.4200805302231, 34.9200805302231, 32.0200805302231, 29.4200805302231, 27.1200805302231};
        double[] expectedValuesLWTractionB = new double[]{51.1200805302231, 47.9200805302231, 43.4200805302231, 41.9200805302231, 41.2200805302231, 41.5200805302231, 40.8200805302231, 40.6200805302231, 40.3200805302231, 40.7200805302231, 40.7200805302231, 44.7200805302231, 40.6200805302231, 40.9200805302231, 40.8200805302231, 41.0200805302231, 41.1200805302231, 46.2200805302231, 39.4200805302231, 36.6200805302231, 33.9200805302231, 31.3200805302231, 28.8200805302231, 26.6200805302231};
        String[] typeNoise = new String[] {"ROLLING", "TRACTIONA", "TRACTIONB"};
        double[][]expectedValues ={expectedValuesLWRolling,expectedValuesLWTractionA,expectedValuesLWTractionB};

        // Initiate Train
        String vehCat = "SNCF-BB66400";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 1;

        // Initiate Section Train
        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;
        double vMaxInfra = 160;
        double vehicleCommercial = 140;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTime = {deltaTDay, deltaTEvening, deltaTNight};

        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);



        for(int i=0;i<typeNoise.length;i++) {
            double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
            double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime[0]);
            // Compare with expected Values
            for (int idFreq = 0; idFreq < 24; idFreq++) {
                assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_2() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossos_2015.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosEU_2020.json");
        // Expected Values
        double[] expectedValuesLWRolling = new double[]{48.855812075460975, 49.80224950461141, 52.61908165678001, 54.982731704412586, 52.61987147312681, 52.041501361148036, 53.19288499441544, 57.95824500346427, 58.13283629462971, 58.077111652037416, 58.250150682153944, 58.69354673204066, 60.388651969482424, 59.76608857751005, 56.71229509132594, 52.262588086680765, 54.5627556471335, 53.29375923552483, 51.141539208928265, 48.745028050980125, 49.141647244090876, 48.601924082363865, 48.58782710304959, 50.14560443243711};
        String[] typeNoise = new String[] {"ROLLING"};
        double[][]expectedValues ={expectedValuesLWRolling};
        // Initiate Train
        String vehCat = "SNCF-BB66400";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit=1;

        // Initiate Section Train
        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 3;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTime = {deltaTDay, deltaTEvening, deltaTNight};

        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);
        for(int i=0;i<typeNoise.length;i++) {
            double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
            double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime[0]);

            for (int idFreq = 0; idFreq < 24; idFreq++) {
                assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_3() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossos_2015.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosEU_2020.json");
        // Expected Values



        double[] expectedValuesLWRolling = new double[]{43.61893941410327, 44.582788825934784, 46.478253997725716, 47.834951549632365, 45.28011686691384, 44.549611245351656, 45.355686257513526, 50.67472393865509, 52.169173866631965, 53.43328073934984, 54.37079062629648, 55.05203089659561, 56.8191237082373, 56.44405585896115, 53.53126423529508, 49.163689752574854, 51.500997272802195, 50.24674638040479, 48.10785356530276, 45.72144216185727, 46.12487763947317, 45.59016543912327, 45.577163222383206, 47.13536625728041};
        double[] expectedValuesLWBridge =  new double[]{53.4064322512979, 54.34996854728857, 56.292454005425355, 57.44443070664133, 56.09001937038818, 55.850224380375366, 50.77211422502701, 54.73176879056412, 56.54047921437049, 60.021829591883076, 61.70329906171462, 61.814271693033106, 62.815222205922, 61.219380439980284, 58.2171385791744, 47.325930549012355, 41.87698711397513, 40.78750686616502, 30.548614051062998, 20.206802528787065, 15.647517245266428, 7.866155280296219, 2.887065725857539, -0.5220958068212198};
        String[] typeNoise = new String[] {"ROLLING","BRIDGE"};
        double[][]expectedValues ={expectedValuesLWRolling,expectedValuesLWBridge};

        //Initiate Train
        String vehCat = "SNCF-BB66400";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit=1;

        //Initiate Section Train
        int nTracks = 4;
        int trackTransfer = 7;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 3;
        int curvature = 0;
        boolean isTunnel = false;
        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;
        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTime = {deltaTDay, deltaTEvening, deltaTNight};

        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        trackParameters.setFileVersion("EU"); // for bridge

        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);

        for(int i=0;i<typeNoise.length;i++) {
            double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
            double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime[0]);
            // Compare with expected Values

            for (int idFreq = 0; idFreq < 24; idFreq++) {
                assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
            }
        }

    }

    @Test
    public void Test_Cnossos_Rail_emission_section_4() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossos_2015.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosEU_2020.json");
        //Expected Values
        double[] expectedValuesLWRollingD = new double[]{42.64047716756204, 43.59103735908428, 45.456174153991, 46.773857830747545, 44.20212586691305, 43.07694967273832, 42.54462519449325, 47.92450477396579, 49.74087356604085, 54.05475982745676, 57.841090649116026, 57.771236415461615, 57.85669468078028, 56.12813486229132, 52.72126638618079, 47.75972514124385, 50.33326183725299, 49.08523837496412, 46.97538128072445, 44.5793134982533, 45.004481967694915, 44.50787367671097, 44.5287841222723, 46.11962258959353};
        double[] expectedValuesLWRollingE = new double[]{41.39108980147904, 42.34164999300128, 44.206786787908, 45.524470464664546, 42.95273850083005, 41.827562306655324, 41.29523782841025, 46.675117407882794, 48.49148619995785, 52.805372461373764, 56.59170328303303, 56.521849049378616, 56.60730731469728, 54.87874749620832, 51.47187902009779, 46.51033777516085, 49.08387447116999, 47.83585100888112, 45.72599391464145, 43.3299261321703, 43.755094601611916, 43.25848631062797, 43.2793967561893, 44.870235223510534};
        double[] expectedValuesLWRollingN = new double[]{40.59927734100279, 41.54983753252503, 43.41497432743175, 44.732658004188295, 42.1609260403538, 41.03574984617907, 40.503425367934, 45.88330494740654, 47.6996737394816, 52.01356000089751, 55.799890822556776, 55.730036588902365, 55.81549485422103, 54.08693503573207, 50.68006655962154, 45.7185253146846, 48.29206201069374, 47.04403854840487, 44.9341814541652, 42.53811367169405, 42.963282141135664, 42.46667385015172, 42.48758429571305, 44.07842276303428};

        double[] expectedValuesLW_Traction_A_D = new double[]{42.840700086720375, 38.74070008672038, 36.54070008672038, 38.54070008672038, 36.74070008672038, 36.74070008672038, 36.94070008672037, 38.74070008672038, 38.54070008672038, 39.64070008672037, 39.54070008672038, 42.54070008672038, 39.14070008672037, 39.04070008672038, 39.04070008672038, 38.04070008672038, 38.04070008672038, 43.340700086720375, 36.44070008672037, 33.44070008672037, 30.94070008672037, 28.040700086720378, 25.44070008672037, 23.140700086720372};
        double[] expectedValuesLW_Traction_A_E = new double[]{41.5912930774198, 37.4912930774198, 35.2912930774198, 37.2912930774198, 35.4912930774198, 35.4912930774198, 35.6912930774198, 37.4912930774198, 37.2912930774198, 38.3912930774198, 38.2912930774198, 41.2912930774198, 37.8912930774198, 37.7912930774198, 37.7912930774198, 36.7912930774198, 36.7912930774198, 42.0912930774198, 35.1912930774198, 32.1912930774198, 29.6912930774198, 26.7912930774198, 24.1912930774198, 21.8912930774198};
        double[] expectedValuesLW_Traction_A_N = new double[]{40.7994806169435, 36.6994806169435, 34.4994806169435, 36.4994806169435, 34.6994806169435, 34.6994806169435, 34.8994806169435, 36.6994806169435, 36.4994806169435, 37.5994806169435, 37.4994806169435, 40.4994806169435, 37.0994806169435, 36.9994806169435, 36.9994806169435, 35.9994806169435, 35.9994806169435, 41.2994806169435, 34.3994806169435, 31.3994806169435, 28.8994806169435, 25.9994806169435, 23.3994806169435, 21.0994806169435};

        double[] expectedValuesLW_Traction_B_D = new double[]{47.1406804435028, 43.9406804435028, 39.4406804435028, 37.9406804435028, 37.2406804435028, 37.5406804435028, 36.8406804435028, 36.6406804435028, 36.3406804435028, 36.7406804435028, 36.7406804435028, 40.7406804435028, 36.6406804435028, 36.9406804435028, 36.8406804435028, 37.0406804435028, 37.1406804435028, 42.2406804435028, 35.4406804435028, 32.6406804435028, 29.9406804435028, 27.3406804435028, 24.8406804435028, 22.6406804435028};
        double[] expectedValuesLW_Traction_B_E = new double[]{45.8912930774198, 42.6912930774198, 38.1912930774198, 36.6912930774198, 35.9912930774198, 36.2912930774198, 35.5912930774198, 35.3912930774198, 35.0912930774198, 35.4912930774198, 35.4912930774198, 39.4912930774198, 35.3912930774198, 35.6912930774198, 35.5912930774198, 35.7912930774198, 35.8912930774198, 40.9912930774198, 34.1912930774198, 31.3912930774198, 28.6912930774198, 26.0912930774198, 23.5912930774198, 21.3912930774198};
        double[] expectedValuesLW_Traction_B_N = new double[]{45.0994806169435, 41.8994806169435, 37.3994806169435, 35.8994806169435, 35.1994806169435, 35.4994806169435, 34.7994806169435, 34.5994806169435, 34.2994806169435, 34.6994806169435, 34.6994806169435, 38.6994806169435, 34.5994806169435, 34.8994806169435, 34.7994806169435, 34.9994806169435, 35.0994806169435, 40.1994806169435, 33.3994806169435, 30.5994806169435, 27.8994806169435, 25.2994806169435, 22.7994806169435, 20.5994806169436};
        double[][] expectedValuesLWRollingEvent = {expectedValuesLWRollingD,expectedValuesLWRollingE,expectedValuesLWRollingN};
        double[][] expectedValuesLW_Traction_AEvent = {expectedValuesLW_Traction_A_D,expectedValuesLW_Traction_A_E,expectedValuesLW_Traction_A_N};
        double[][] expectedValuesLW_Traction_BEvent = {expectedValuesLW_Traction_B_D,expectedValuesLW_Traction_B_E,expectedValuesLW_Traction_B_N};

        String[] typeNoise = new String[] {"ROLLING","TRACTIONA","TRACTIONB"};
        // Initiate Train
        String vehCat = "SNCF-BB66400";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit=1;

        // Initiate Section Train
        int nTracks = 2;
        int trackTransfer = 9;
        int railRoughness = 3;
        int impactNoise = 1;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;


        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;
        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};

        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("EU");

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);

        for(int timeEvent=0;timeEvent<3;timeEvent++){
            double[] expectedValuesLWRolling = expectedValuesLWRollingEvent[timeEvent];
            double[] expectedValuesLW_Traction_A = expectedValuesLW_Traction_AEvent[timeEvent];
            double[] expectedValuesLW_Traction_B = expectedValuesLW_Traction_BEvent[timeEvent];
            double[][] expectedValues={expectedValuesLWRolling,expectedValuesLW_Traction_A,expectedValuesLW_Traction_B};
            // Loop through time periods and sources
            double deltaTime = deltaTimes[timeEvent];
            for(int i=0;i<typeNoise.length;i++) {
                double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
                double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime);
                // Compare with expected Values
                for (int idFreq = 0; idFreq < 24; idFreq++) {
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                }
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_5() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossos_2015.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosEU_2020.json");

        double[] expectedValuesLWRollingD = new double[]{47.71997843033826, 46.637350019740694, 47.57462749051441, 48.37035398021847, 46.26756512527347, 46.231021285491536, 47.86579192688177, 52.487052374043586, 53.488856919653934, 54.60582219451978, 56.07350114723665, 58.46378649956855, 62.755024418449956, 63.70786252484214, 61.80735449363346, 58.68560995030353, 56.934883841636676, 54.84658995444764, 51.830594829509096, 49.969642427112504, 48.54957338454732, 47.75658020373088, 47.89544088700445, 48.67590747414407};
        double[] expectedValuesLWRollingE = new double[]{47.8571829614869, 46.773205039062276, 47.65061271097261, 48.41688361760173, 46.32933646576278, 46.28022349802543, 47.91165590399426, 52.32870371415995, 53.193078095746394, 54.26199790602617, 55.84798703049516, 58.496779937215315, 62.94191660235764, 63.901269120719455, 62.01181168644282, 58.892194681278255, 56.92203496269723, 54.83406505619752, 51.87274923380053, 50.01731005663079, 48.38932689131972, 47.63016466691677, 47.751238073652125, 48.38987243332228};
        double[] expectedValuesLWRollingN = new double[]{47.65504941580788, 46.52263037133977, 47.410602883575564, 48.44075766451791, 46.403473096977066, 46.477035962374885, 48.16562342931819, 52.52404691923551, 53.48428196138525, 54.65273465783923, 56.363119689612375, 58.960398517566645, 62.97519061980192, 63.521599596900884, 61.41409817229095, 58.02824808516533, 55.85451052086385, 54.18421078705474, 51.97980699274502, 49.8322214851138, 48.306533194735444, 47.956080531777864, 47.89683391435612, 48.574675637769815};
                double[][] expectedValuesLWRollingEvent = {expectedValuesLWRollingD,expectedValuesLWRollingE,expectedValuesLWRollingN};


        String[] vehCats = {"SNCF-BB66400", "RMR-Cat-1", "RMR-Cat-9-a"};
        double[] vehicleSpeeds = {80, 120, 100};
        int[] rollingConditions = {0, 0, 0, 0};
        double[] idlingTimes = {0, 0, 0, 0};
        double[] nBUnits = {1, 1, 1};
        double[] tDays = {0.4, 0.5, 0.2};
        double[] tEvenings = {0.3, 0.5, 0.3};
        double[] tNights = {0.25, 0.2, 0.7};

        RailWayParameters[] lWRailWays = new RailWayParameters[vehCats.length];

        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 4;
        int impactNoise = 1;
        int bridgeTrasnfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTrasnfert, curvature, vehicleCommercial, isTunnel, nTracks);
        double[] rolling = new double[24];

        double[][] lWRolling = new double[vehCats.length][24];
        double[] LWRollingTotal = new double[24];

        for (int timeEvent = 0; timeEvent < 3; timeEvent++) {
            double[] expectedValuesLWRolling = expectedValuesLWRollingEvent[timeEvent];

            for (int i = 0; i < vehCats.length; i++) {
                double vehiclePerHour = 1000 * vehicleSpeeds[i];
                double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
                double deltaTDay = 10 * Math.log10(tDays[i]) - deltaL0;
                double deltaTEvening = 10 * Math.log10(tEvenings[i]) - deltaL0;
                double deltaTNight = 10 * Math.log10(tNights[i]) - deltaL0;

                // Precompute delta times in an array
                double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};

                RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(
                        vehCats[i], vehicleSpeeds[i], vehiclePerHour, rollingConditions[i], idlingTimes[i]);
                lWRailWays[i] = railwayCnossos.evaluate(vehicleParameters, trackParameters);

                rolling=lWRailWays[i].getRailwaySourceList().get("ROLLING").getlW();

                // Loop through time periods and sources
                double deltaTime = deltaTimes[timeEvent];

                // Batch process the sources
                lWRolling[i] = Utils.trainLWmperFreq(rolling, nBUnits[i], deltaTime);
            }

            // Summing results for each frequency and Compare with expected Values
            for (int idFreq = 0; idFreq < 24; idFreq++) {
                LWRollingTotal[idFreq] = Utils.sumDbValues(lWRolling[0][idFreq], Utils.sumDbValues(lWRolling[1][idFreq], lWRolling[2][idFreq]));
                assertEquals(expectedValuesLWRolling[idFreq], LWRollingTotal[idFreq], EPSILON_TEST1);
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_section_6() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");

        double[] expectedValuesLWRolling = new double[]{48.5606, 49.5910, 51.5763, 56.4920, 55.5831, 55.4623, 55.2207, 56.4663, 56.3912,
                56.7350, 56.4837, 59.9648, 62.8745, 62.6585, 59.6990, 57.3252, 59.7649, 61.1898, 60.5615, 58.7027, 55.6686, 49.3786, 48.0600, 45.9195};

        // Initiate Section Train
        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        // Set : Take into account the number of units
        double nBUnit = 2;

        // Initiate Section Train
        int nTracks = 2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 0;
        int bridgeTransfert = 0;
        int curvature = 0;
        boolean isTunnel = false;

        double vMaxInfra = 160;
        double vehicleCommercial = 120;

        double tDay = 1;
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;

        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("FR");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);

        double[] lWRolling =  lWRailWay.getRailwaySourceList().get("ROLLING").getlW();
        for (int idFreq = 0; idFreq < 24; idFreq++) {
            // Compute sound powers per track meter
            lWRolling[idFreq] = 10 * Math.log10(Math.pow(10, (lWRolling[idFreq] + 10 * Math.log10(nBUnit) + deltaTDay) / 10));
            assertEquals(expectedValuesLWRolling[idFreq], lWRolling[idFreq], EPSILON_TEST1);
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC1() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");
        // Expected values
        double[] expectedValuesLWRolling = new double[]{48.5605583070706, 49.591094876553, 51.5763357483898, 56.4920337262433, 55.5830577481227, 55.4623526520535, 55.2207102517505, 56.4663370258577, 56.3912193484322, 56.7349538148708, 56.4837006351883, 59.9647735129824, 62.8744986704598, 62.6584506699844, 59.6990085689524, 57.3251971450722, 59.7649235057108, 61.1897948776294, 60.5615433617675, 58.7027221858109, 55.6685855421802, 49.378644576175, 48.0600152459358, 45.9195251325526};
        double[] expectedValuesLW_Traction_A = new double[]{49.8691001300806, 49.7691001300806, 49.6691001300806, 50.1691001300806, 47.0691001300806, 54.1691001300806, 50.7691001300806, 44.2691001300806, 46.8691001300806, 47.0691001300806, 45.6691001300806, 44.9691001300806, 43.4691001300806, 44.5691001300806, 41.5691001300806, 41.1691001300806, 38.9691001300806, 37.3691001300806, 36.4691001300806, 35.3691001300806, 31.7691001300806, 27.6691001300806, 22.4691001300806, 20.4691001300806};
        double[] expectedValuesLW_Traction_B = new double[]{-39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572};
        double[][] expectedValues={expectedValuesLWRolling,expectedValuesLW_Traction_A,expectedValuesLW_Traction_B};

        String[] typeNoise = new String[] {"ROLLING", "TRACTIONA", "TRACTIONB"};

        // Initiate Train
        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        // Initiate Section Train
        int nTracks = 2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial = 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};

        // Compute
        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("FR");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);

        // Loop through time periods and sources
        for(int timeEvent=0;timeEvent<3;timeEvent++){
            double deltaTime = deltaTimes[timeEvent];
            for(int i=0;i<typeNoise.length;i++) {
                double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
                double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime);
                // Compare with expected Values
                for (int idFreq = 0; idFreq < 24; idFreq++) {
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                }
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC2() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");
        // Expected values
        double[] expectedValuesLWRolling = new double[]{43.8903627967019, 44.9655697489009, 48.9478577301374, 55.5479496503529, 55.435644216130996, 56.28918949883372, 57.76517536242085, 59.857189090598624, 60.9673183839427, 61.989589714413825, 61.667715524758066, 64.86050810840895, 67.79089584625551, 65.62626998386897, 62.18314392899565, 59.57968136675302, 61.08640983016812, 62.20761878325611, 61.49143701879498, 59.61042465277447, 56.53747558356267, 50.41901289738583, 49.0315804220768, 46.79550354867328};
        double[] expectedValuesLW_Traction_A = new double[]{49.8691001300806, 49.7691001300806, 49.6691001300806, 50.1691001300806, 47.0691001300806, 54.1691001300806, 50.7691001300806, 44.2691001300806, 46.8691001300806, 47.0691001300806, 45.6691001300806, 44.9691001300806, 43.4691001300806, 44.5691001300806, 41.5691001300806, 41.1691001300806, 38.9691001300806, 37.3691001300806, 36.4691001300806, 35.3691001300806, 31.7691001300806, 27.6691001300806, 22.4691001300806, 20.4691001300806};
        double[] expectedValuesLW_Traction_B = new double[]{-39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572, -39.030899869572};
        double[][] expectedValues={expectedValuesLWRolling,expectedValuesLW_Traction_A,expectedValuesLW_Traction_B};

        String[] typeNoise = new String[] {"ROLLING", "TRACTIONA", "TRACTIONB"};

        // Initiate Train
        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        // Initiate Section Train
        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 2;
        int impactNoise = 1;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial = 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;


        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};

        // Compute
        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("EU");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);

        // Loop through time periods and sources
        for(int timeEvent=0;timeEvent<3;timeEvent++){
            double deltaTime = deltaTimes[timeEvent];
            for(int i=0;i<typeNoise.length;i++) {
                double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
                double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime);
                // Compare with expected Values
                for (int idFreq = 0; idFreq < 24; idFreq++) {
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                }
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC3() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");

        // Expected values
        double[] expectedValuesLWRolling = new double[]{59.3823815386426, 59.7740042542256, 61.6645074704619, 66.5651015825184, 64.9236256151963, 63.9195646659437, 63.0633891427656, 63.1703959172873, 62.0842218312365, 61.5507094539991, 59.9458124597247, 62.0451549860138, 63.5063034095735, 61.62742572976, 57.591356100339, 54.8777141322954, 57.2745023554145, 58.7784762578773, 58.2320686591241, 56.3534564010822, 53.2240832593016, 46.6986552124813, 45.4591519061876, 43.3326357315674};
        double[] expectedValuesLW_Traction_A = new double[]{46.8588001734408, 46.7588001734407, 46.6588001734408, 47.1588001734408, 44.0588001734407, 51.1588001734408, 47.7588001734407, 41.2588001734407, 43.8588001734408, 44.0588001734407, 42.6588001734408, 41.9588001734407, 40.4588001734407, 41.5588001734407, 38.5588001734407, 38.1588001734408, 35.9588001734407, 34.3588001734408, 33.4588001734407, 32.3588001734408, 28.7588001734407, 24.6588001734408, 19.4588001734407, 17.4588001734407};
        double[] expectedValuesLW_Traction_B = new double[]{-42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644, -42.0411998258644};
        double[][] expectedValues={expectedValuesLWRolling,expectedValuesLW_Traction_A,expectedValuesLW_Traction_B};

        String[] typeNoise = new String[] {"ROLLING", "TRACTIONA", "TRACTIONB"};

        // Initiate Train
        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 1;
        double tEvening = 1;
        double tNight = 1;

        // Initiate Section
        int nTracks = 4;
        int trackTransfer = 8;
        int railRoughness = 2;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial = 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};

        // Compute
        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("FR");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);

        // Loop through time periods and sources
        for(int timeEvent=0;timeEvent<3;timeEvent++){
            double deltaTime = deltaTimes[timeEvent];
            for(int i=0;i<typeNoise.length;i++) {
                double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
                double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime);
                // Compare with expected Values
                for (int idFreq = 0; idFreq < 24; idFreq++) {
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                }
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC4() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");
        // Expected values
        double[] expectedValuesLWRollingD = new double[]{44.5811582203502, 45.6116947898327, 47.5969356616694, 52.5126336395229, 51.6036576614023, 51.4829525653331, 51.2413101650301, 52.4869369391373, 52.4118192617118, 52.7555537281504, 52.5043005484679, 55.985373426262, 58.8950985837394, 58.679050583264, 55.7196084822321, 53.3457970583518, 55.7855234189905, 57.210394790909, 56.5821432750471, 54.7233220990906, 51.6891854554598, 45.3992444894546, 44.0806151592154, 41.9401250458322};
        double[] expectedValuesLWRollingE = new double[]{43.3317708542672, 44.3623074237497, 46.3475482955864, 51.2632462734399, 50.3542702953193, 50.2335651992501, 49.9919227989471, 51.2375495730543, 51.1624318956288, 51.5061663620674, 51.2549131823849, 54.735986060179, 57.6457112176564, 57.429663217181, 54.4702211161491, 52.0964096922688, 54.5361360529075, 55.961007424826, 55.3327559089641, 53.4739347330076, 50.4397980893768, 44.1498571233716, 42.8312277931324, 40.6907376797492};
        double[] expectedValuesLWRollingN = new double[]{42.5399583937909, 43.5704949632734, 45.5557358351102, 50.4714338129637, 49.562457834843, 49.4417527387739, 49.2001103384709, 50.445737112578, 50.3706194351526, 50.7143539015912, 50.4631007219087, 53.9441735997028, 56.8538987571802, 56.6378507567048, 53.6784086556728, 51.3045972317925, 53.7443235924312, 55.1691949643497, 54.5409434484879, 52.6821222725313, 49.6479856289006, 43.3580446628953, 42.0394153326562, 39.898925219273};

        double[] expectedValuesLW_Traction_A_D = new double[]{45.8897000433602, 45.7897000433602, 45.6897000433602, 46.1897000433602, 43.0897000433602, 50.1897000433602, 46.7897000433602, 40.2897000433602, 42.8897000433602, 43.0897000433602, 41.6897000433602, 40.9897000433602, 39.4897000433602, 40.5897000433602, 37.5897000433602, 37.1897000433602, 34.9897000433602, 33.3897000433602, 32.4897000433602, 31.3897000433602, 27.7897000433602, 23.6897000433602, 18.4897000433602, 16.4897000433602};
        double[] expectedValuesLW_Traction_A_E = new double[]{44.6403126772772, 44.5403126772772, 44.4403126772772, 44.9403126772772, 41.8403126772772, 48.9403126772772, 45.5403126772772, 39.0403126772772, 41.6403126772772, 41.8403126772772, 40.4403126772772, 39.7403126772772, 38.2403126772772, 39.3403126772772, 36.3403126772772, 35.9403126772772, 33.7403126772772, 32.1403126772772, 31.2403126772772, 30.1403126772772, 26.5403126772772, 22.4403126772772, 17.2403126772772, 15.2403126772772};
        double[] expectedValuesLW_Traction_A_N = new double[]{43.8485002168009, 43.7485002168009, 43.6485002168009, 44.1485002168009, 41.0485002168009, 48.1485002168009, 44.7485002168009, 38.2485002168009, 40.8485002168009, 41.0485002168009, 39.6485002168009, 38.9485002168009, 37.4485002168009, 38.5485002168009, 35.5485002168009, 35.1485002168009, 32.9485002168009, 31.3485002168009, 30.4485002168009, 29.3485002168009, 25.7485002168009, 21.6485002168009, 16.4485002168009, 14.4485002168009};

        double[] expectedValuesLW_Traction_B_D = new double[]{-43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712, -43.0102999557712};
        double[] expectedValuesLW_Traction_B_E = new double[]{-44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647, -44.2596873215647};
        double[] expectedValuesLW_Traction_B_N = new double[]{-45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093, -45.0514997818093};
        double[][] expectedValuesLWRollingEvent = {expectedValuesLWRollingD,expectedValuesLWRollingE,expectedValuesLWRollingN};
        double[][] expectedValuesLW_Traction_AEvent = {expectedValuesLW_Traction_A_D,expectedValuesLW_Traction_A_E,expectedValuesLW_Traction_A_N};
        double[][] expectedValuesLW_Traction_BEvent = {expectedValuesLW_Traction_B_D,expectedValuesLW_Traction_B_E,expectedValuesLW_Traction_B_N};
        String[] typeNoise = new String[] {"ROLLING", "TRACTIONA", "TRACTIONB"};

        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;

        int nTracks = 2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial = 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};

        // Compute
        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("FR");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);

        for(int timeEvent=0;timeEvent<3;timeEvent++){
            double[] expectedValuesLWRolling = expectedValuesLWRollingEvent[timeEvent];
            double[] expectedValuesLW_Traction_A = expectedValuesLW_Traction_AEvent[timeEvent];
            double[] expectedValuesLW_Traction_B = expectedValuesLW_Traction_BEvent[timeEvent];
            double[][] expectedValues={expectedValuesLWRolling,expectedValuesLW_Traction_A,expectedValuesLW_Traction_B};
            // Loop through time periods and sources
            double deltaTime = deltaTimes[timeEvent];
            for(int i=0;i<typeNoise.length;i++) {
                double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
                double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime);
                // Compare with expected Values
                for (int idFreq = 0; idFreq < 24; idFreq++) {
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                }
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC5() throws IOException {
        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");
        // Expected values
        double[] expectedValuesLWRollingD = new double[]{47.1552955205404, 48.8498299106887, 52.6506665062211, 59.9189540729727, 57.9682560825282, 58.5872885355725, 59.0528160959685, 60.1904875304058, 60.6442541026117, 62.4955801250664, 64.9184365575419, 70.5236596616762, 75.9091586312107, 76.6780384718047, 74.8656580571925, 71.8258438346925, 69.0422555458139, 66.2590037381391, 64.4635730861252, 62.3174294364882, 58.0523015636309, 51.1818941714616, 49.4692864131497, 47.5870164960235};
        double[] expectedValuesLWRollingE = new double[]{46.7153189292102, 48.5400035435940, 52.5625340892052, 60.0787091329295, 57.9923893388288, 58.6495173688799, 59.0916185428417, 60.1979886666328, 60.6185205324881, 62.4960038970257, 64.9683137433641, 70.5625482161490, 75.9313127158166, 76.6869876714216, 74.8713872507045, 71.8271267251379, 69.0163596754784, 66.1966324248387, 64.3988216929349, 62.2298468109704, 57.9544897520346, 51.0604922061408, 49.3019210916908, 47.4515242894244};
        double[] expectedValuesLWRollingN = new double[]{46.1270508967803, 48.0388064828929, 51.9977848913093, 59.7778707051652, 57.4505710153709, 58.0747874408452, 58.2265981950651, 59.2354414276101, 59.2675554683469, 60.7056375004388, 62.8199452089267, 67.8798770364391, 72.7517670066939, 73.2280095787096, 71.3038487930857, 68.2530122348682, 65.6895763573932, 63.6625773432251, 62.3175814829550, 60.0794037396615, 56.4441459388305, 49.9352912430575, 48.0608827756958, 46.1859326090793};

        double[] expectedValuesLW_Traction_A_D = new double[]{46.6113350513868, 46.5361930551616, 46.4507560297314, 46.4615638226975, 43.7430615177986, 50.3108176641904, 47.0669699562295, 41.9733820154675, 44.9476552863602, 44.0601109307387, 43.5845906890585, 42.5291935606719, 44.0678692086462, 42.2820604493695, 39.3273037341639, 38.7228940509159, 37.7014563451550, 35.0584081257409, 34.9065665976502, 32.1860323878995, 28.9788221314718, 24.6576825124615, 19.3206918485612, 17.4371753071292};
        double[] expectedValuesLW_Traction_A_E = new double[]{45.8328681049925, 45.7862346546680, 45.7429051220906, 45.3887520516725, 42.8767932132583, 49.1270860066919, 45.9523201972128, 41.2845923821496, 44.2721133413950, 43.2015176915916, 42.9875064075318, 41.9345847780754, 43.7642442442831, 41.6353141126725, 38.7319900260274, 37.9848318047791, 37.2510674836558, 34.3066693732319, 34.2589449165509, 31.1934631044681, 28.0984026857766, 23.7802040911827, 18.4668791427740, 16.6411101334843};
        double[] expectedValuesLW_Traction_A_N = new double[]{46.0848410660751, 46.1332009230439, 46.2608016902272, 44.9756706211064, 42.8060094745255, 48.4199531690002, 45.2738703819419, 40.3948530934846, 42.8148965285004, 42.5567543659976, 42.3409101395776, 41.8331316056441, 41.8487195283546, 40.9996698539350, 38.3337368784949, 37.0354784138840, 36.3931579683877, 33.0052878990180, 32.3770485393350, 30.0457096633810, 26.8475690712121, 23.0216616855918, 18.1249028285137, 16.4062831110709};

        double[] expectedValuesLW_Traction_B_D = new double[]{28.1206003941747, 27.4206004782821, 24.7206009653633, 22.9206015056726, 21.5206021113937, 22.6206016195594, 24.0206011493702, 28.2206003832282, 28.7206003321217, 23.6206012686259, 27.6206004528527, 25.1206008727914, 23.6206012686259, 24.4206010406076, 23.4206013325014, 22.3206017415914, 21.0206023796041, 20.0206030181980, 18.9206039131889, 15.1206095083875, 12.9206158371834, 11.0206245764616, 5.12069586340405, 3.82072934593712};
        double[] expectedValuesLW_Traction_B_E = new double[]{29.8815127708764, 29.1815128175810, 26.4815130880562, 24.6815133880888, 23.2815137244444, 24.3815134513299, 25.7815131902348, 29.9815127647978, 30.4815127364185, 25.3815132564572, 29.3815128034602, 26.8815130366512, 25.3815132564572, 26.1815131298392, 25.1815132919272, 24.0815135190939, 22.7815138733811, 21.7815142279909, 20.6815147249775, 16.8815178319809, 14.6815213463517, 12.7815261992633, 6.88156578498707, 5.58158437797714};
        double[] expectedValuesLW_Traction_B_N = new double[]{33.5612804391897, 32.8612804536025, 30.1612805370699, 28.3612806296585, 26.9612807334563, 28.0612806491744, 29.4612805686017, 33.6612804373138, 34.1612804285561, 29.0612805890376, 33.0612804492449, 30.5612805212065, 29.0612805890376, 29.8612805499639, 28.8612805999835, 27.7612806700861, 26.4612807794175, 25.4612808888484, 24.3612810422162, 20.5612820010234, 18.3612830855430, 16.4612845831314, 10.5612967991650, 9.26130253693161};
        double[][] expectedValuesLWRollingEvent = {expectedValuesLWRollingD,expectedValuesLWRollingE,expectedValuesLWRollingN};
        double[][] expectedValuesLW_Traction_AEvent = {expectedValuesLW_Traction_A_D,expectedValuesLW_Traction_A_E,expectedValuesLW_Traction_A_N};
        double[][] expectedValuesLW_Traction_BEvent = {expectedValuesLW_Traction_B_D,expectedValuesLW_Traction_B_E,expectedValuesLW_Traction_B_N};

        // Train data: vehCat, vehicleSpeed, rollingCondition, idlingTime, nBUnit, tDay, tEvening, tNight
        String[] vehCats = {"SNCF2", "SNCF6", "SNCF73", "SNCF8", "SNCF69"};
        double[] vehicleSpeeds = {80, 120, 120, 100, 100};
        int[] rollingConditions = {0, 0, 0, 0, 0};
        double[] idlingTimes = {0, 0, 0, 0, 0};
        double[] nBUnits = {2, 1, 9, 1, 4};
        double[] tDays = {0.4, 0.5, 0.5, 0.2, 0.2};
        double[] tEvenings = {0.3, 0.5, 0.5, 0.3, 0.3};
        double[] tNights = {0.25, 0.2, 0.2, 0.7, 0.7};

        // Section data
        int nTracks = 2;
        int trackTransfer = 7;
        int railRoughness = 1;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial = 120;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);

        RailWayParameters[] lWRailWays = new RailWayParameters[vehCats.length];
        double[] rolling = new double[24];
        double[]traction_A = new double[24];
        double[] traction_B = new double[24];

        double[][] lWRolling = new double[vehCats.length][24];
        double[][] lWTraction_A= new double[vehCats.length][24];
        double[][] lWTraction_B = new double[vehCats.length][24];
        double[] LWRollingTotal = new double[24];
        double[] LWTraction_ATotal = new double[24];
        double[] LWTraction_BTotal = new double[24];

        for (int timeEvent = 0; timeEvent < 3; timeEvent++) {
            double[] expectedValuesLWRolling = expectedValuesLWRollingEvent[timeEvent];
            double[] expectedValuesLW_Traction_A = expectedValuesLW_Traction_AEvent[timeEvent];
            double[] expectedValuesLW_Traction_B = expectedValuesLW_Traction_BEvent[timeEvent];

            for (int i = 0; i < vehCats.length; i++) {
                double vehiclePerHour = 1000 * vehicleSpeeds[i];
                double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
                double deltaTDay = 10 * Math.log10(tDays[i]) - deltaL0;
                double deltaTEvening = 10 * Math.log10(tEvenings[i]) - deltaL0;
                double deltaTNight = 10 * Math.log10(tNights[i]) - deltaL0;
                double deltaTime=0;
                // Precompute delta times in an array
                double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};

                RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(
                        vehCats[i], vehicleSpeeds[i], vehiclePerHour, rollingConditions[i], idlingTimes[i]);
                lWRailWays[i] = railwayCnossos.evaluate(vehicleParameters, trackParameters);

                rolling=lWRailWays[i].getRailwaySourceList().get("ROLLING").getlW();
                traction_A=lWRailWays[i].getRailwaySourceList().get("TRACTIONA").getlW();
                traction_B=lWRailWays[i].getRailwaySourceList().get("TRACTIONB").getlW();

                // Loop through time periods and sources
                deltaTime = deltaTimes[timeEvent];

                // Batch process the sources
                lWRolling[i] = Utils.trainLWmperFreq(rolling, nBUnits[i], deltaTime);
                lWTraction_A[i] = Utils.trainLWmperFreq(traction_A, nBUnits[i], deltaTime);
                lWTraction_B[i] = Utils.trainLWmperFreq(traction_B, nBUnits[i], deltaTime);
            }

            // Summing results for each frequency and Compare with expected Values
            for (int idFreq = 0; idFreq < 24; idFreq++) {
                LWRollingTotal[idFreq] = Utils.sumDb5(lWRolling[0][idFreq], lWRolling[1][idFreq], lWRolling[2][idFreq], lWRolling[3][idFreq], lWRolling[4][idFreq]);
                assertEquals(expectedValuesLWRolling[idFreq], LWRollingTotal[idFreq], EPSILON_TEST1);

                LWTraction_ATotal[idFreq] = Utils.sumDb5(lWTraction_A[0][idFreq], lWTraction_A[1][idFreq], lWTraction_A[2][idFreq], lWTraction_A[3][idFreq], lWTraction_A[4][idFreq]);
                assertEquals(expectedValuesLW_Traction_A[idFreq], LWTraction_ATotal[idFreq], EPSILON_TEST1);

                LWTraction_BTotal[idFreq] = Utils.sumDb5(lWTraction_B[0][idFreq], lWTraction_B[1][idFreq], lWTraction_B[2][idFreq], lWTraction_B[3][idFreq], lWTraction_B[4][idFreq]);
                assertEquals(expectedValuesLW_Traction_B[idFreq], LWTraction_BTotal[idFreq], EPSILON_TEST1);
            }
        }
    }

    @Test
    public void Test_Cnossos_Rail_emission_OC6() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");

        // Expected values
        double[] expectedValues = new double[]{-140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140, -140};

        String vehCat = "SNCF2";
        double vehicleSpeed = 80;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;

        int nTracks = 2;
        int trackTransfer = 5;
        int railRoughness = 1;
        int impactNoise = 1;
        int curvature = 0;
        double vMaxInfra = 160;
        double vehicleCommercial = 120;
        boolean isTunnel = true;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;

        // Compute
        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("FR");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);
        assertTrue(lWRailWay.railwaySourceList.isEmpty());

    }

    @Test
    public void Test_Cnossos_Rail_emission_OC7() throws IOException {

        railwayCnossos.setVehicleDataFile("RailwayVehiclesCnossosSNCF_2022.json");
        railwayCnossos.setTrainSetDataFile("RailwayTrainsets.json");
        railwayCnossos.setRailwayDataFile("RailwayCnossosSNCF_2022.json");
        // Expected values
        double[] expectedValuesLWRollingD = new double[]{33.6733596010837, 34.95169576625, 38.2539731975929, 44.1801059537931, 42.888610835611, 44.0148743871629, 45.6800277990472, 47.6677743937925, 48.7556304666653, 51.3814838437463, 52.4049926613164, 57.631720998909, 63.3357285948357, 66.1337582772167, 66.4000139556384, 66.2606389650649, 67.9407445637443, 68.7526472395718, 67.7497360005367, 64.0167245325835, 58.7017301999714, 52.2571748232612, 52.5279874725351, 50.9834474448221};
        double[] expectedValuesLWRollingE = new double[]{32.4239722350007, 33.702308400167, 37.0045858315099, 42.9307185877101, 41.639223469528, 42.7654870210799, 44.4306404329642, 46.4183870277095, 47.5062431005823, 50.1320964776633, 51.1556052952334, 56.382333632826, 62.0863412287527, 64.8843709111337, 65.1506265895554, 65.0112515989819, 66.6913571976613, 67.5032598734888, 66.5003486344537, 62.7673371665005, 57.4523428338884, 51.0077874571782, 51.2786001064521, 49.7340600787391};
        double[] expectedValuesLWRollingN = new double[]{31.6321597745245, 32.9104959396908, 36.2127733710337, 42.1389061272339, 40.8474110090518, 41.9736745606036, 43.638827972488, 45.6265745672333, 46.714430640106, 49.340284017187, 50.3637928347571, 55.5905211723497, 61.2945287682765, 64.0925584506574, 64.3588141290791, 64.2194391385057, 65.899544737185, 66.7114474130125, 65.7085361739774, 61.9755247060243, 56.6605303734121, 50.2159749967019, 50.4867876459759, 48.9422476182628};

        double[] expectedValuesLW_Traction_A_D = new double[]{40.149387366083, 40.049387366083, 39.949387366083, 40.449387366083, 37.349387366083, 44.449387366083, 41.049387366083, 34.549387366083, 37.149387366083, 37.349387366083, 35.949387366083, 35.249387366083, 33.749387366083, 34.849387366083, 31.849387366083, 31.449387366083, 29.249387366083, 27.649387366083, 26.749387366083, 25.649387366083, 22.049387366083, 17.949387366083, 12.749387366083, 10.749387366083};
        double[] expectedValuesLW_Traction_A_E = new double[]{38.9, 38.8, 38.7, 39.2, 36.1, 43.2, 39.8, 33.3, 35.9, 36.1, 34.7, 34, 32.5, 33.6, 30.6, 30.2, 28, 26.4, 25.5, 24.4, 20.8, 16.7, 11.5, 9.50000000000001};
        double[] expectedValuesLW_Traction_A_N = new double[]{38.1081875395238, 38.0081875395237, 37.9081875395238, 38.4081875395238, 35.3081875395237, 42.4081875395238, 39.0081875395237, 32.5081875395237, 35.1081875395238, 35.3081875395237, 33.9081875395238, 33.2081875395237, 31.7081875395237, 32.8081875395237, 29.8081875395237, 29.4081875395238, 27.2081875395237, 25.6081875395238, 24.7081875395237, 23.6081875395238, 20.0081875395237, 15.9081875395238, 10.7081875395238, 8.70818753952375};

        double[] expectedValuesLW_Traction_B_D = new double[]{-48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598, -48.7506126306598};
        double[] expectedValuesLW_Traction_B_E = new double[]{-49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571, -49.9999999956571};
        double[] expectedValuesLW_Traction_B_N = new double[]{-50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647, -50.7918124552647};

        double[] expectedValuesLW_Aero_A_D = new double[]{52.449387366083, 53.649387366083, 52.149387366083, 52.449387366083, 51.649387366083, 52.049387366083, 54.649387366083, 57.349387366083, 58.049387366083, 58.349387366083, 59.149387366083, 57.549387366083, 41.049387366083, 38.449387366083, 35.549387366083, 32.449387366083, 28.849387366083, 26.249387366083, 20.049387366083, -19.5506126339131, -62.7506125520996, -62.650612553962, -64.4506125129002, -64.5506125100814};
        double[] expectedValuesLW_Aero_A_E = new double[]{51.2, 52.4, 50.9, 51.2, 50.4, 50.8, 53.4, 56.1, 56.8, 57.1, 57.9, 56.3, 39.8, 37.2, 34.3, 31.2, 27.6, 25, 18.8, -20.7999999999948, -63.9999998909102, -63.8999998933933, -65.6999998386443, -65.7999998348859};
        double[] expectedValuesLW_Aero_A_N = new double[]{50.4081875395238, 51.6081875395238, 50.1081875395238, 50.4081875395238, 49.6081875395238, 50.0081875395237, 52.6081875395238, 55.3081875395237, 56.0081875395237, 56.3081875395237, 57.1081875395238, 55.5081875395237, 39.0081875395237, 36.4081875395238, 33.5081875395237, 30.4081875395238, 26.8081875395237, 24.2081875395237, 18.0081875395237, -21.59181246047, -64.7918123295684, -64.6918123325483, -66.4918122668494, -66.5918122623393};

        double[] expectedValuesLW_Aero_B_D = new double[]{61.449387366083, 62.549387366083, 63.849387366083, 64.949387366083, 63.949387366083, 62.349387366083, 62.349387366083, 61.449387366083, 62.049387366083, 62.349387366083, 62.249387366083, 60.649387366083, 58.549387366083, 57.349387366083, 51.849387366083, 48.949387366083, 42.849387366083, 16.349387366083, -34.9506126337812, -56.5506126142904, -56.3506126151737, -55.7506126175923, -61.7506125689271, -62.4506125575606};
        double[] expectedValuesLW_Aero_B_E = new double[]{60.2, 61.3, 62.6, 63.7, 62.7, 61.1, 61.1, 60.2, 60.8, 61.1, 61, 59.4, 57.3, 56.1, 50.6, 47.7, 41.6, 15.1, -36.199999999819, -57.7999999738312, -57.599999975009, -56.9999999782337, -62.9999999133469, -63.6999998981914};
        double[] expectedValuesLW_Aero_B_N = new double[]{59.4081875395238, 60.5081875395237, 61.8081875395237, 62.9081875395238, 61.9081875395238, 60.3081875395237, 60.3081875395237, 59.4081875395238, 60.0081875395237, 60.3081875395237, 60.2081875395237, 58.6081875395238, 56.5081875395237, 55.3081875395237, 49.8081875395237, 46.9081875395238, 40.8081875395237, 14.3081875395237, -36.991812460259, -58.5918124290736, -58.391812430487, -57.7918124343567, -63.7918123564925, -64.491812338306};

        double[][] expectedValuesLWRollingEvent = {expectedValuesLWRollingD,expectedValuesLWRollingE,expectedValuesLWRollingN};
        double[][] expectedValuesLW_Traction_AEvent = {expectedValuesLW_Traction_A_D,expectedValuesLW_Traction_A_E,expectedValuesLW_Traction_A_N};
        double[][] expectedValuesLW_Traction_BEvent = {expectedValuesLW_Traction_B_D,expectedValuesLW_Traction_B_E,expectedValuesLW_Traction_B_N};
        double[][] expectedValuesLW_Aero_AEvent = {expectedValuesLW_Aero_A_D,expectedValuesLW_Aero_A_E,expectedValuesLW_Aero_A_N};
        double[][] expectedValuesLW_Aero_BEvent = {expectedValuesLW_Aero_B_D, expectedValuesLW_Aero_B_E,expectedValuesLW_Aero_B_N };

        String[] typeNoise = new String[] {"ROLLING", "TRACTIONA", "TRACTIONB", "AERODYNAMICA", "AERODYNAMICB"};

        // Initiate train parameters
        String vehCat = "SNCF2";
        double vehicleSpeed = 300;
        int rollingCondition = 0;
        double idlingTime = 0;
        double nBUnit = 2;
        double tDay = 0.4;
        double tEvening = 0.3;
        double tNight = 0.25;

        // Initiate ection parameters
        int nTracks = 2;
        int trackTransfer = 5;
        int railRoughness = 2;
        int impactNoise = 0;
        int curvature = 0;
        double vMaxInfra = 350;
        double vehicleCommercial = 300;
        boolean isTunnel = false;
        int bridgeTransfert = 0;

        // Compute deltaT
        double vehiclePerHour = (1000 * vehicleSpeed); //for one vehicle
        double deltaL0 = 10 * Math.log10(vehiclePerHour * nTracks);
        double deltaTDay = 10 * Math.log10(tDay) - deltaL0;
        double deltaTEvening = 10 * Math.log10(tEvening) - deltaL0;
        double deltaTNight = 10 * Math.log10(tNight) - deltaL0;
        double[] deltaTimes = {deltaTDay, deltaTEvening, deltaTNight};


        // Compute
        RailwayVehicleCnossosParameters vehicleParameters = new RailwayVehicleCnossosParameters(vehCat, vehicleSpeed, vehiclePerHour, rollingCondition, idlingTime);
        vehicleParameters.setFileVersion("FR");
        RailwayTrackCnossosParameters trackParameters = new RailwayTrackCnossosParameters(vMaxInfra, trackTransfer, railRoughness,
                impactNoise, bridgeTransfert, curvature, vehicleCommercial, isTunnel, nTracks);
        RailWayParameters lWRailWay = railwayCnossos.evaluate(vehicleParameters, trackParameters);
        // Loop through time event and sources
        for(int timeEvent=0;timeEvent<3;timeEvent++){
            double[] expectedValuesLWRolling = expectedValuesLWRollingEvent[timeEvent];
            double[] expectedValuesLW_Traction_A = expectedValuesLW_Traction_AEvent[timeEvent];
            double[] expectedValuesLW_Traction_B = expectedValuesLW_Traction_BEvent[timeEvent];
            double[] expectedValuesLW_Aero_A = expectedValuesLW_Aero_AEvent[timeEvent];
            double[] expectedValuesLW_Aero_B = expectedValuesLW_Aero_BEvent[timeEvent];
            double[][] expectedValues={expectedValuesLWRolling,expectedValuesLW_Traction_A,expectedValuesLW_Traction_B,expectedValuesLW_Aero_A,expectedValuesLW_Aero_B};

            double deltaTime = deltaTimes[timeEvent];
            for(int i=0;i<typeNoise.length;i++) {
                double[] lWextract =lWRailWay.getRailwaySourceList().get(typeNoise[i]).getlW();
                double[] lWCalculate = Utils.trainLWmperFreq(lWextract, nBUnit, deltaTime);
                // Compare with expected Values
                for (int idFreq = 0; idFreq < 24; idFreq++) {
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                    assertEquals(expectedValues[i][idFreq], lWCalculate[idFreq], EPSILON_TEST1);
                }
            }
        }

    }
}
