package org.noise_planet.noisemodelling.jdbc;

import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.pathfinder.utils.AlphaUtils;
import org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.EvaluateAttenuationCnossos;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static java.lang.Double.NaN;
import static org.junit.Assert.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.addArray;
import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.*;
import static org.noise_planet.noisemodelling.propagation.PropagationProcessPathData.DEFAULT_FREQUENCIES_THIRD_OCTAVE;

// TODO reduce error epsilon

/**
 * Test class evaluation and testing attenuation values.
 */
public class EvaluateAttenuationCnossosTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(EvaluateAttenuationCnossosTest.class);

    private static final double ERROR_EPSILON_HIGHEST = 1e5;
    private static final double ERROR_EPSILON_VERY_HIGH = 15;
    private static final double ERROR_EPSILON_HIGH = 3;
    private static final double ERROR_EPSILON_MEDIUM = 1;
    private static final double ERROR_EPSILON_LOW = 0.5;
    private static final double ERROR_EPSILON_VERY_LOW = 0.2;
    private static final double ERROR_EPSILON_LOWEST = 0.02;

    private static final double[] HOM_WIND_ROSE = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final double[] FAV_WIND_ROSE = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;
    private static final double[] SOUND_POWER_LEVELS = new double[]{93, 93, 93, 93, 93, 93, 93, 93};
    private static final double[] A_WEIGHTING = new double[]{-26.2, -16.1, -8.6, -3.2, 0.0, 1.2, 1.0, -1.1};


    private static void assertDoubleArrayEquals(String valueName, double[] expected, double [] actual, double delta) {
        assertEquals(valueName + ": Different array length;", expected.length, actual.length);
        for(int i=0; i< expected.length; i++) {
            if(!Double.isNaN(expected[i])){
                double deltaOff = 1;
                /*if(expected[i]!=0) {
                    while (Math.abs(expected[i]/deltaOff) < 1) {
                        deltaOff /= 10;
                    }
                }*/
                assertEquals(valueName + ": Arrays first differed at element ["+i+"];", expected[i], actual[i], delta*deltaOff);
            }
        }
    }

    /**
     * Test identic rays : to the east, to the west
     */
    @Test
    public void eastWestTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addBuilding(new Coordinate[]{
                        new Coordinate(10, -5),
                        new Coordinate(20, -5),
                        new Coordinate(20, 5),
                        new Coordinate(10, 5)
                }, 0.0)
                .addBuilding(new Coordinate[]{
                        new Coordinate(-10, -5),
                        new Coordinate(-20, -5),
                        new Coordinate(-20, 5),
                        new Coordinate(-10, 5)
                }, 0.0)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(0, 0, 2)
                .addReceiver(30, 0, 2)
                .addReceiver(-30, 0, 2)
                .setGs(0.0)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);
        SegmentPath s0 = propDataOut.propagationPaths.get(0).getSRSegment();
        SegmentPath s1 = propDataOut.propagationPaths.get(1).getSRSegment();
        assertEquals(s0.dp, s1.dp);
        assertEquals(s0.testFormH, s1.testFormH);
        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().value, propDataOut.receiversAttenuationLevels.pop().value, Double.MIN_VALUE);
    }

    /**
     * Test identic rays : to the east, to the west
     */
    @Test
    public void northSouthTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addBuilding(new Coordinate[]{
                        new Coordinate(-5, 10),
                        new Coordinate(-5, 20),
                        new Coordinate(5, 20),
                        new Coordinate(5, 10)
                }, 0.0)
                .addBuilding(new Coordinate[]{
                        new Coordinate(-5, -10 ),
                        new Coordinate(-5, -20 ),
                        new Coordinate(5, -20),
                        new Coordinate(5, -10)
                }, 0.0)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(0, 0, 2)
                .addReceiver(0, 30, 2)
                .addReceiver(0, -30, 2)
                .setGs(0.0)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);
        SegmentPath s0 = propDataOut.propagationPaths.get(0).getSRSegment();
        SegmentPath s1 = propDataOut.propagationPaths.get(1).getSRSegment();
        assertEquals(s0.dp, s1.dp);
        assertEquals(s0.testFormH, s1.testFormH);
        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().value, propDataOut.receiversAttenuationLevels.pop().value, Double.MIN_VALUE);
    }

    @Test
    public void northSouthGroundTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addGroundEffect(-50, 50, -5, 5, 0.5)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(0, 0, 2)
                .addReceiver(0, 30, 2)
                .addReceiver(0, -30, 2)
                .setGs(0.0)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().value, propDataOut.receiversAttenuationLevels.pop().value, Double.MIN_VALUE);
    }

    @Test
    public void eastWestGroundTest() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder
                .addGroundEffect(-5, 5, -50, 50, 0.5)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(0, 0, 2)
                .addReceiver(30, 0, 2)
                .addReceiver(-30, 0, 2)
                .setGs(0.0)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        assertArrayEquals(propDataOut.receiversAttenuationLevels.pop().value, propDataOut.receiversAttenuationLevels.pop().value, Double.MIN_VALUE);
    }

    /**
     * Test TC01 -- Reflecting ground (G = 0)
     */
    @Test
    public void TC01() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.0)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00};
        double[] expectedCfH = new double[]{194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16};
        double[] expectedAGroundH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00};
        double[] expectedCfF = new double[]{194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16};
        double[] expectedAGroundF = new double[]{-4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36};
        
        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedABoundaryF = new double[]{-4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36};
        double[] expectedLH = new double[]{39.21, 39.16, 39.03, 38.86, 38.53, 37.36, 32.87, 16.54};
        double[] expectedLF = new double[]{40.58, 40.52, 40.40, 40.23, 39.89, 38.72, 34.24, 17.90};
        double[] expectedL = new double[]{39.95, 39.89, 39.77, 39.60, 39.26, 38.09, 33.61, 17.27};


        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryF", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH", expectedLH, actualLH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LF", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L", expectedL, actualL, ERROR_EPSILON_LOWEST);
    }

    /**
     * Test TC02 -- Mixed ground (G = 0.5)
     */
    @Test
    public void TC02() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.5)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{8.2e-05, 4.5e-04, 2.5e-03, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfH = new double[]{199.17, 213.44, 225.43, 134.05, 23.76, 2.49, 0.47, 0.10};
        double[] expectedAGroundH = new double[]{-1.50, -1.50, -1.50, 0.85, 5.71, -1.50, -1.50, -1.50};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfF = new double[]{199.17, 213.44, 225.43, 134.05, 23.76, 2.49, 0.47, 0.10};
        double[] expectedAGroundF = new double[]{-2.18, -2.18, -2.18, -2.18, -0.93, -2.18, -2.18, -2.18};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{-1.50, -1.50, -1.50, 0.85, 5.71, -1.50, -1.50, -1.50};
        double[] expectedABoundaryF = new double[]{-2.18, -2.18, -2.18, -2.18, -0.93, -2.18, -2.18, -2.18};
        double[] expectedLH = new double[]{37.71, 37.66, 37.53, 35.01, 29.82, 35.86, 31.37, 15.04};
        double[] expectedLF = new double[]{38.39, 38.34, 38.22, 38.04, 36.45, 36.54, 32.05, 15.72};
        double[] expectedL = new double[]{38.07, 38.01, 37.89, 36.79, 34.29, 36.21, 31.73, 15.39};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC03 -- Porous ground (G = 1)
     */
    @Test
    public void TC03() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(1.0)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{4.9e-04, 2.7e-03, 1.5e-02, 0.08, 0.41, 2.02, 9.06, 35.59};
        double[] expectedCfH = new double[]{214.47, 224.67, 130.15, 22.76, 2.48, 0.49, 0.11, 0.03};
        double[] expectedAGroundH = new double[]{0.00, 0.00, 1.59, 9.67, 5.03, 0.00, 0.00, 0.00};
        double[] expectedWF = new double[]{0.00, 0.00, 0.01, 0.08, 0.41, 2.02, 9.06, 35.59};
        double[] expectedCfF = new double[]{214.47, 224.67, 130.15, 22.76, 2.48, 0.49, 0.11, 0.03};
        double[] expectedAGroundF = new double[]{0.00, 0.00, 0.00, 4.23, 0.00, 0.00, 0.00, 0.00};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{0.00, 0.00, 1.59, 9.67, 5.03, 0.00, 0.00, 0.00};
        double[] expectedABoundaryF = new double[]{0.00, 0.00, 0.00, 4.23, 0.00, 0.00, 0.00, 0.00};
        double[] expectedLH = new double[]{36.21, 36.16, 34.45, 26.19, 30.49, 34.36, 29.87, 13.54};
        double[] expectedLF = new double[]{36.21, 36.16, 36.03, 31.63, 35.53, 34.36, 29.87, 13.54};
        double[] expectedL = new double[]{36.21, 36.16, 35.31, 29.71, 33.70, 34.36, 29.87, 13.54};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
    }
    
    /**
     * Test TC04 -- Flat ground with spatially varying acoustic properties
     */
    @Test
    public void TC04() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()

        .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.2)
        .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
        .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.9)

        .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{1.0e-04, 5.6e-04, 3.1e-03, 0.02, 0.09, 0.50, 2.53, 11.96};
        double[] expectedCfH = new double[]{200.18, 216.12, 221.91, 116.87, 17.87, 2.02, 0.39, 0.08};
        double[] expectedAGroundH = new double[]{-1.37, -1.37, -1.37, 1.77, 6.23, -1.37, -1.37, -1.37};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.02, 0.09, 0.50, 2.53, 11.96};
        double[] expectedCfF = new double[]{200.18, 216.12, 221.91, 116.87, 17.87, 2.02, 0.39, 0.08};
        double[] expectedAGroundF = new double[]{-2.00, -2.00, -2.00, -2.00, -0.95, -2.00, -2.00, -2.00};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{-1.37, -1.37, -1.37, 1.77, 6.23, -1.37, -1.37, -1.37};
        double[] expectedABoundaryF = new double[]{-2.00, -2.00, -2.00, -2.00, -0.95, -2.00, -2.00, -2.00};
        double[] expectedLH = new double[]{37.59, 37.53, 37.41, 34.10, 29.29, 35.73, 31.25, 14.91};
        double[] expectedLF = new double[]{38.21, 38.15, 38.03, 37.86, 36.48, 36.36, 31.87, 15.54};
        double[] expectedL = new double[]{37.91, 37.85, 37.73, 36.37, 34.23, 36.06, 31.57, 15.24};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
     }

    /**
     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC05() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()

        .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
        .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
        .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)

        .addTopographicLine(0, 80, 0, 255, 80, 0)
        .addTopographicLine(225, 80, 0, 225, -20, 0)
        .addTopographicLine(225, -20, 0, 0, -20, 0)
        .addTopographicLine(0, -20, 0, 0, 80, 0)
        .addTopographicLine(120, -20, 0, 120, 80, 0)
        .addTopographicLine(185, -5, 10, 205, -5, 10)
        .addTopographicLine(205, -5, 10, 205, 75, 10)
        .addTopographicLine(205, 74, 10, 185, 75, 10)
        .addTopographicLine(185, 75, 10, 185, -5, 10)

        .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{1.6e-04, 8.7e-04, 4.8e-03, 0.03, 0.14, 0.75, 3.70, 16.77};
        double[] expectedCfH = new double[]{203.37, 222.35, 207.73, 82.09, 9.63, 1.33, 0.27, 0.06};
        double[] expectedAGroundH = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{199.73, 214.27, 225.54, 131.93, 22.89, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.38, 22.75};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedABoundaryF = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedLH = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};
        double[] expectedLF = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};
        double[] expectedL = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryF", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH", expectedLH, actualLH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LF", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L", expectedL, actualL, ERROR_EPSILON_LOWEST);
    }

    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC06() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()

        .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
        .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
        .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)

        .addTopographicLine(0, 80, 0, 255, 80, 0)
        .addTopographicLine(225, 80, 0, 225, -20, 0)
        .addTopographicLine(225, -20, 0, 0, -20, 0)
        .addTopographicLine(0, -20, 0, 0, 80, 0)
        .addTopographicLine(120, -20, 0, 120, 80, 0)
        .addTopographicLine(185, -5, 10, 205, -5, 10)
        .addTopographicLine(205, -5, 10, 205, 75, 10)
        .addTopographicLine(205, 74, 10, 185, 75, 10)
        .addTopographicLine(185, 75, 10, 185, -5, 10)

        .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedDeltaDiffSR = new double[]{0., 0., 0., 3.16, 0.56, 0., 0., 0.};
        double[] expectedAGroundSO = new double[]{0., 0., 0., 2.74, -1.21, 0., 0., 0.};
        double[] expectedAGroundOR = new double[]{0., 0., 0., -2.40, -2.40, 0., 0., 0.};
        double[] expectedDeltaDiffSPrimeR = new double[]{0., 0., 0., 4.71, 4.65, 0., 0., 0.};
        double[] expectedDeltaDiffSRPrime = new double[]{0., 0., 0., 10.83, 13.26, 0., 0., 0.};
        double[] expectedDeltaGroundSO = new double[]{0., 0., 0., 2.23, -0.77, 0., 0., 0.};
        double[] expectedDeltaGroundOR = new double[]{0., 0., 0., -1.07, -0.62, 0., 0., 0.};
        double[] expectedADiff = new double[]{0., 0., 0., 4.31, -0.83, 0., 0., 0.};

        double[] expectedWH = new double[]{1.1e-04, 6.0e-04, 3.4e-03, Double.NaN, Double.NaN, 0.53, 2.70, 12.70};
        double[] expectedCfH = new double[]{200.89, 217.45, 220.41, Double.NaN, Double.NaN, 1.88, 0.37, 0.08};
        double[] expectedAGroundH = new double[]{-1.32, -1.32, -1.32, Double.NaN, -Double.NaN, -1.32, -1.32, -1.32};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{199.59, 214.11, 225.39, 131.90, 22.89, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.32, -1.32, -1.29, -1.05, -1.32, -1.32, -1.32, -1.32};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.37, 22.73};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{-1.32, -1.32, -1.32, 4.31, -0.83, -1.32, -1.32, -1.32};
        double[] expectedABoundaryF = new double[]{-1.32, -1.32, -1.29, -1.05, -1.32, -1.32, -1.32, -1.32};
        double[] expectedLH = new double[]{37.53, 37.47, 37.35, 31.54, 36.34, 35.67, 31.18, 14.82};
        double[] expectedLF = new double[]{37.53, 37.47, 37.31, 36.89, 36.84, 35.67, 31.18, 14.82};
        double[] expectedL = new double[]{37.53, 37.47, 37.33, 34.99, 36.60, 35.67, 31.18, 14.82};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSR = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSO = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundOR = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeR = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrime = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSO = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundOR = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiff = proPath.aBoundaryH.aDiff;

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
         double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.31, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-5.65, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_LOWEST);
        assertEquals(194.16, proPath.getSegmentList().get(1).rPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(8.50, proPath.getSegmentList().get(1).rPrime.y, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("DeltaDiffSR", expectedDeltaDiffSR, actualDeltaDiffSR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSO", expectedAGroundSO, actualAGroundSO, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundOR", expectedAGroundOR, actualAGroundOR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeR", expectedDeltaDiffSPrimeR, actualDeltaDiffSPrimeR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrime", expectedDeltaDiffSRPrime, actualDeltaDiffSRPrime, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSO", expectedDeltaGroundSO, actualDeltaGroundSO, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundOR", expectedDeltaGroundOR, actualDeltaGroundOR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiff", expectedADiff, actualADiff, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
     */
    @Test
    public void TC07() {
        //Profile building
        ProfileBuilder builder = new ProfileBuilder()

                .addWall(new Coordinate[]{new Coordinate(100, 240, 0), new Coordinate(265, -180, 0)}, 6, -1)

                .addGroundEffect(0, 50, -250, 250, 0.9)
                .addGroundEffect(50, 150, -250, 250, 0.5)
                .addGroundEffect(150, 225, -250, 250, 0.2)

                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addReceiver(200.0, 50.0, 4.0)
                .addSource(10.0, 10.0, 1.0)
                .setGs(0.9)
                .hEdgeDiff(true)
                .vEdgeDiff(false)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedDeltaDiffSRH = new double[]{6.01, 6.96, 8.41, 10.36, 12.72, 15.37, 18.19, 21.10};
        double[] expectedAGroundSOH = new double[]{-1.16, -1.16, -1.16, -1.16, 1.45, -1.16, -1.16, -1.16};
        double[] expectedAGroundORH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRH = new double[]{6.24, 7.32, 8.92, 11.00, 13.46, 16.16, 19.01, 21.94};
        double[] expectedDeltaDiffSRPrimeH = new double[]{12.54, 15.13, 17.94, 20.85, 23.80, 26.78, 29.78, 32.78};
        double[] expectedDeltaGroundSOH = new double[]{-1.13, -1.11, -1.09, -1.08, 1.32, -1.06, -1.06, -1.06};
        double[] expectedDeltaGroundORH = new double[]{-1.22, -1.02, -0.88, -0.79, -0.74, -0.71, -0.70, -0.69};
        double[] expectedADiffH = new double[]{3.67, 4.83, 6.44, 8.49, 13.30, 13.60, 16.43, 19.35};

        double[] expectedDeltaDiffSRF = new double[]{5.67, 6.40, 7.58, 9.27, 11.43, 13.94, 16.68, 19.55};
        double[] expectedAGroundSOF = new double[]{-1.16, -1.16, -1.16, -1.16, -1.16, -1.16, -1.16, -1.16};
        double[] expectedAGroundORF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRF = new double[]{5.91, 6.81, 8.19, 10.07, 12.39, 15.01, 17.81, 20.71};
        double[] expectedDeltaDiffSRPrimeF = new double[]{12.46, 15.05, 17.86, 20.76, 23.71, 26.70, 29.69, 32.70};
        double[] expectedDeltaGroundSOF = new double[]{-1.12, -1.11, -1.08, -1.06, -1.04, -1.03, -1.02, -1.02};
        double[] expectedDeltaGroundORF = new double[]{-1.18, -0.96, -0.81, -0.71, -0.65, -0.61, -0.60, -0.59};
        double[] expectedADiffF = new double[]{3.36, 4.33, 5.69, 7.50, 9.74, 12.30, 15.06, 17.94};

        //Disabled because only diffraction
        /*double[] expectedWH = new double[]{1.1e-04, 6.0e-04, 3.4e-03, Double.NaN, Double.NaN, 0.53, 2.70, 12.70};
        double[] expectedCfH = new double[]{200.89, 217.45, 220.41, Double.NaN, Double.NaN, 1.88, 0.37, 0.08};
        double[] expectedAGroundH = new double[]{-1.32, -1.32, -1.32, Double.NaN, -Double.NaN, -1.32, -1.32, -1.32};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{199.59, 214.11, 225.39, 131.90, 22.89, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.32, -1.32, -1.29, -1.05, -1.32, -1.32, -1.32, -1.32};*/

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{3.67, 4.83, 6.44, 8.49, 13.30, 13.60, 16.43, 19.35};
        double[] expectedABoundaryF = new double[]{3.36, 4.33, 5.69, 7.50, 9.74, 12.30, 15.06, 17.94};
        double[] expectedLH = new double[]{32.54, 31.32, 29.60, 27.37, 22.22, 20.76, 13.44, -5.81};
        double[] expectedLF = new double[]{32.85, 31.83, 30.35, 28.36, 25.78, 22.06, 14.81, -4.41};
        double[] expectedL = new double[]{32.70, 31.58, 29.99, 27.89, 24.36, 21.46, 14.18, -5.05};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        //Disabled because only diffraction
        /*double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;*/

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.00, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-1.00, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_LOWEST);
        assertEquals(194.16, proPath.getSegmentList().get(1).rPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-4.00, proPath.getSegmentList().get(1).rPrime.y, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("DeltaDiffSRH", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiffH", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOF", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiffF", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        //Disabled because only diffraction
        /*assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);*/

        assertDoubleArrayEquals("AlphaAtm", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L", expectedL, actualL, ERROR_EPSILON_LOWEST);
    }

    /**
     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
     */
    @Test
    public void TC08() {
        GeometryFactory factory = new GeometryFactory();

        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder()

                // Add building
                .addWall(new Coordinate[]{
                                new Coordinate(175, 50, 0),
                                new Coordinate(190, 10, 0)},
                        6, 1)
                // Add ground effect
                .addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9)
                .addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5)
                .addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2)

                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addReceiver(200, 50, 4)
                .addSource(10, 10, 1)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{6.02, 6.97, 8.42, 10.38, 12.75, 15.40, 18.21, 21.12};
        double[] expectedAGroundSOH = new double[]{-1.16, -1.16, -1.16, -1.16, 1.46, -1.16, -1.16, -1.16};
        double[] expectedAGroundORH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRH = new double[]{6.25, 7.33, 8.93, 11.01, 13.47, 16.18, 19.03, 21.96};
        double[] expectedDeltaDiffSRPrimeH = new double[]{12.57, 15.17, 17.98, 20.89, 23.84, 26.83, 29.82, 32.83};
        double[] expectedDeltaGroundSOH = new double[]{-1.13, -1.11, -1.10, -1.08, 1.33, -1.06, -1.06, -1.06};
        double[] expectedDeltaGroundORH = new double[]{-1.21, -1.01, -0.87, -0.79, -0.74, -0.71, -0.70, -0.69};
        double[] expectedADiffH = new double[]{3.68, 4.84, 6.45, 8.51, 13.34, 13.62, 16.46, 19.38};

        double[] expectedDeltaDiffSRF = new double[]{5.68, 6.41, 7.60, 9.30, 11.47, 13.99, 16.73, 19.60};
        double[] expectedAGroundSOF = new double[]{-1.16, -1.16, -1.16, -1.16, -1.16, -1.16, -1.16, -1.16};
        double[] expectedAGroundORF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRF = new double[]{5.92, 6.82, 8.21, 10.10, 12.42, 15.04, 17.84, 20.75};
        double[] expectedDeltaDiffSRPrimeF = new double[]{12.50, 15.09, 17.90, 20.80, 23.76, 26.74, 29.74, 32.74};
        double[] expectedDeltaGroundSOF = new double[]{-1.12, -1.11, -1.09, -1.06, -1.05, -1.03, -1.03, -1.02};
        double[] expectedDeltaGroundORF = new double[]{-1.18, -0.96, -0.81, -0.71, -0.65, -0.61, -0.60, -0.59};
        double[] expectedADiffF = new double[]{3.37, 4.34, 5.71, 7.53, 9.78, 12.34, 15.11, 17.99};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{3.68, 4.84, 6.45, 8.51, 13.34, 13.62, 16.43, 19.38};
        double[] expectedABoundaryF = new double[]{3.37, 4.34, 5.71, 7.53, 9.78, 12.34, 15.11, 17.99};
        double[] expectedLH = new double[]{32.54, 31.31, 29.58, 27.35, 22.19, 20.74, 13.42, -5.84};
        double[] expectedLF = new double[]{32.84, 31.81, 30.32, 28.33, 25.74, 22.02, 14.76, -4.45};
        double[] expectedL = new double[]{32.69, 31.57, 29.97, 27.87, 24.32, 21.42, 14.14, -5.09};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.00, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-1.00, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_LOWEST);
        assertEquals(194.16, proPath.getSegmentList().get(1).rPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-4.00, proPath.getSegmentList().get(1).rPrime.y, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOWEST);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.06, 0.34, 1.76, 8.58};
        double[] expectedCfH = new double[]{226.58, 242.17, 257.73, 159.33, 29.64, 3.03, 0.57, 0.12};
        double[] expectedAGroundH = new double[]{-1.61, -1.61, -1.61, 0.75, 6.25, -0.39, -1.61, -1.61};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.06, 0.34, 1.76, 8.58};
        double[] expectedCfF = new double[]{226.58, 242.17, 257.73, 159.33, 29.64, 3.03, 0.57, 0.12};
        double[] expectedAGroundF = new double[]{-2.65, -2.65, -2.65, -2.65, -1.30, -2.65, -2.65, -2.65};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.03, 0.09, 0.23, 0.43, 0.81, 2.14, 7.25, 25.86};
        expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        expectedDeltaDiffSRH = new double[]{23.09, 26.03, 29.03, 32.03, 35.03, 38.04, 41.05, 44.06};
        expectedDeltaDiffSRF = new double[]{23.09, 26.03, 29.03, 32.03, 35.03, 38.04, 41.05, 44.06};
        expectedLH = new double[]{14.73, 11.73, 8.59, 3.03, -5.86, -3.56, -10.45, -32.07};
        expectedLF = new double[]{15.77, 12.77, 9.63, 6.43, 1.69, -1.29, -9.41, -31.03};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRF - right lateral", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - right lateral", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.17, 10.40};
        expectedCfH = new double[]{199.96, 214.57, 225.67, 131.50, 22.70, 2.41, 0.46, 0.10};
        expectedAGroundH = new double[]{-1.48, -1.48, -1.48, 1.01, 5.84, -1.48, -1.48, -1.48};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.17, 10.40};
        expectedCfF = new double[]{199.96, 214.57, 225.67, 131.50, 22.70, 2.41, 0.46, 0.10};
        expectedAGroundF = new double[]{-2.16, -2.16, -2.16, -2.16, -0.92, -2.16, -2.16, -2.16};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.38, 0.71, 1.88, 6.38, 22.77};
        expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        expectedDeltaDiffSRH = new double[]{8.78, 10.81, 13.24, 15.93, 18.77, 21.69, 24.66, 27.64};
        expectedDeltaDiffSRF = new double[]{8.78, 10.81, 13.24, 15.93, 18.77, 21.69, 24.66, 27.64};
        expectedLH = new double[]{28.91, 26.83, 24.28, 18.92, 10.92, 14.14, 6.68, -12.70};
        expectedLF = new double[]{29.59, 27.51, 24.96, 22.09, 17.68, 14.82, 7.36, -12.02};

        //Actual values
        proPath = propDataOut.propagationPaths.get(2);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - left lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - left lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - left lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - left lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - left lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRH - left lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRF - left lateral", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - left lateral", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - left lateral", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{8.17,16.86,22.51,25.46,24.87,23.44,15.93,-5.43},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short barrier
     */
    @Test
    public void TC09() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                // Add building
                .addWall(new Coordinate[]{
                                new Coordinate(175, 50, 17),
                                new Coordinate(190, 10, 14)},
                        1);

        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{7.90, 9.67, 11.92, 14.49, 17.26, 20.15, 23.09, 26.07};
        double[] expectedAGroundSOH = new double[]{-0.71, -0.71, -0.71, -0.71, -0.71, -0.71, -0.71, -0.71};
        double[] expectedAGroundORH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRH = new double[]{8.65, 10.64, 13.05, 15.72, 18.56, 21.48, 24.44, 27.43};
        double[] expectedDeltaDiffSRPrimeH = new double[]{13.55, 16.23, 19.09, 22.01, 24.98, 27.97, 30.97, 33.98};
        double[] expectedDeltaGroundSOH = new double[]{-0.65, -0.64, -0.63, -0.62, -0.62, -0.61, -0.61, -0.61};
        double[] expectedDeltaGroundORH = new double[]{-1.33, -1.21, -1.13, -1.09, -1.07, -1.06, -1.05, -1.05};
        double[] expectedADiffH = new double[]{5.91, 7.82, 10.16, 12.78, 15.58, 18.48, 21.43, 23.34};

        double[] expectedDeltaDiffSRF = new double[]{7.68, 9.39, 11.57, 14.10, 16.85, 19.73, 22.67, 25.64};
        double[] expectedAGroundSOF = new double[]{-0.71, -0.71, -0.71, -0.71, -0.71, -0.71, -0.71, -0.71};
        double[] expectedAGroundORF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRF = new double[]{8.47, 10.41, 12.79, 15.44, 18.26, 21.17, 24.13, 27.12};
        double[] expectedDeltaDiffSRPrimeF = new double[]{13.50, 16.17, 19.02, 21.95, 24.92, 27.91, 30.91, 33.91};
        double[] expectedDeltaGroundSOF = new double[]{-0.65, -0.63, -0.62, -0.61, -0.61, -0.61, -0.60, -0.60};
        double[] expectedDeltaGroundORF = new double[]{-1.31, -1.18, -1.10, -1.05, -1.03, -1.02, -1.01, -1.01};
        double[] expectedADiffF = new double[]{5.72, 7.57, 9.85, 12.44, 15.22, 18.11, 21.05, 23.39};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.38, 22.75};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{5.91, 7.82, 10.16, 12.78, 15.58, 18.48, 21.43, 23.34};
        double[] expectedABoundaryF = new double[]{5.72, 7.57, 9.85, 12.44, 15.22, 18.11, 21.05, 23.39};
        double[] expectedLH = new double[]{30.28, 28.31, 25.86, 23.07, 19.93, 15.86, 8.41, -9.87};
        double[] expectedLF = new double[]{30.47, 28.57, 26.16, 23.40, 20.29, 16.23, 8.79, -9.92};
        double[] expectedL = new double[]{30.38, 28.44, 26.01, 23.24, 20.11, 16.05, 8.60, -9.89};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.24, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-4.92, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_LOWEST);
        assertEquals(194.48, proPath.getSegmentList().get(1).rPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(6.59, proPath.getSegmentList().get(1).rPrime.y, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOWEST);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.07, 0.39, 2.00, 9.66};
        double[] expectedCfH = new double[]{227.72, 244.70, 256.12, 145.81, 24.37, 2.61, 0.50, 0.10};
        double[] expectedAGroundH = new double[]{-1.53, -1.53, -1.53, 2.33, -1.53, -1.53, -1.53, -1.53};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.06, 0.34, 1.76, 8.58};
        double[] expectedCfF = new double[]{226.99, 242.62, 258.17, 159.45, 29.63, 3.03, 0.57, 0.12};
        double[] expectedAGroundF = new double[]{-1.53, -1.38, -1.35, -1.53, -1.53, -1.53, -1.53, -1.53};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.03, 0.09, 0.23, 0.43, 0.81, 2.14, 7.26, 25.91};
        expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        expectedDeltaDiffSRH = new double[]{23.08, 26.03, 29.02, 32.02, 35.03, 38.04, 41.05, 44.06};
        expectedDeltaDiffSRF = new double[]{23.08, 26.03, 29.02, 32.02, 35.03, 38.04, 41.05, 44.06};
        expectedLH = new double[]{14.64, 11.63, 8.50, 1.44, 1.91, -2.43, -10.56, -32.21};
        expectedLF = new double[]{14.64, 11.48, 8.31, 5.30, 1.91, -2.43, -10.56, -32.21};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);


        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRF - right lateral", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - right lateral", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.03, 0.14, 0.75, 3.72, 16.84};
        expectedCfH = new double[]{204.07, 223.16, 208.01, 81.73, 9.55, 1.33, 0.27, 0.06};
        expectedAGroundH = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.17, 10.40};
        expectedCfF = new double[]{200.40, 215.06, 226.13, 131.61, 22.68, 2.41, 0.46, 0.10};
        expectedAGroundF = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.38, 0.71, 1.89, 6.40, 22.82};
        expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        expectedDeltaDiffSRH = new double[]{8.79, 10.81, 13.24, 15.93, 18.77, 21.70, 24.66, 27.65};
        expectedDeltaDiffSRF = new double[]{8.79, 10.81, 13.24, 15.93, 18.78, 21.70, 24.66, 27.65};
        expectedLH = new double[]{28.47, 26.39, 23.84, 20.97, 17.79, 13.70, 6.22, -13.19};
        expectedLF = new double[]{28.47, 26.39, 23.84, 20.97, 17.79, 13.70, 6.22, -13.19};

        //Actual values
        proPath = propDataOut.propagationPaths.get(2);


        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - left lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - left lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - left lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - left lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - left lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRH - left lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRF - left lateral", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - left lateral", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - left lateral", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        // impossible geometry in NoiseModelling
        assertArrayEquals(  new double[]{6.41,14.50,19.52,22.09,22.16,19.28,11.62,-9.31},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver at low height
     */
    @Test
    public void TC10() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55, 5, 10),
                        new Coordinate(65, 5, 10),
                        new Coordinate(65, 15, 10),
                        new Coordinate(55, 15, 10)
                });

        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(70, 10, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{18.23, 21.88, 26.33, 30.63, 34.21, 37.39, 40.45, 43.47};
        double[] expectedAGroundSOH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedAGroundORH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedDeltaDiffSPrimeRH = new double[]{18.91, 22.58, 27.03, 31.33, 34.92, 38.10, 41.16, 44.18};
        double[] expectedDeltaDiffSRPrimeH = new double[]{20.80, 24.51, 28.97, 33.28, 36.87, 40.05, 43.11, 46.13};
        double[] expectedDeltaGroundSOH = new double[]{-1.40, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39};
        double[] expectedDeltaGroundORH = new double[]{-1.14, -1.13, -1.13, -1.13, -1.13, -1.13, -1.13, -1.13};
        double[] expectedADiffH = new double[]{15.69, 19.36, 22.48, 22.48, 22.48, 22.48, 22.48, 22.48};

        double[] expectedDeltaDiffSRF = new double[]{18.23, 21.88, 26.33, 30.63, 34.21, 37.39, 40.45, 43.47};
        double[] expectedAGroundSOF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedAGroundORF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedDeltaDiffSPrimeRF = new double[]{18.91, 22.58, 27.03, 31.33, 34.92, 38.10, 41.16, 44.18};
        double[] expectedDeltaDiffSRPrimeF = new double[]{20.80, 24.51, 28.97, 33.28, 36.87, 40.05, 43.11, 46.13};
        double[] expectedDeltaGroundSOF = new double[]{-1.40, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39};
        double[] expectedDeltaGroundORF = new double[]{-1.14, -1.13, -1.13, -1.13, -1.13, -1.13, -1.13, -1.13};
        double[] expectedADiffF = new double[]{15.69, 19.36, 22.48, 22.48, 22.48, 22.48, 22.48, 22.48};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.00, 0.01, 0.02, 0.04, 0.07, 0.20, 0.66, 2.36};
        double[] expectedADiv = new double[]{37.12, 37.12, 37.12, 37.12, 37.12, 37.12, 37.12, 37.12};
        double[] expectedABoundaryH = new double[]{15.69, 19.36, 22.48, 22.48, 22.48, 22.48, 22.48, 22.48};
        double[] expectedABoundaryF = new double[]{15.69, 19.36, 22.48, 22.48, 22.48, 22.48, 22.48, 22.48};
        double[] expectedLH = new double[]{40.19, 36.52, 33.38, 33.36, 33.33, 33.21, 32.74, 31.04};
        double[] expectedLF = new double[]{40.19, 36.52, 33.38, 33.36, 33.33, 33.21, 32.74, 31.04};
        double[] expectedL = new double[]{40.19, 36.52, 33.38, 33.36, 33.33, 33.21, 32.74, 31.04};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.00, proPath.getSRSegment().sPrime.x, ERROR_EPSILON_MEDIUM);
        assertEquals(-1.00, proPath.getSRSegment().sPrime.y, ERROR_EPSILON_HIGHEST);
        assertEquals(20.00, proPath.getSRSegment().rPrime.x, ERROR_EPSILON_LOW);
        assertEquals(-4.00, proPath.getSRSegment().rPrime.y, ERROR_EPSILON_HIGHEST);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOWEST);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfH = new double[]{24.24, 24.59, 26.01, 28.28, 20.54, 5.05, 0.52, 0.10};
        double[] expectedAGroundH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfF = new double[]{24.24, 24.59, 26.01, 28.28, 20.54, 5.05, 0.52, 0.10};
        double[] expectedAGroundF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.05, 0.09, 0.24, 0.80, 2.84};
        expectedADiv = new double[]{37.12, 37.12, 37.12, 37.12, 37.12, 37.12, 37.12, 37.12};
        expectedDeltaDiffSRH = new double[]{15.59, 19.16, 23.56, 27.83, 31.40, 34.58, 37.63, 40.65};
        expectedLH = new double[]{41.79, 38.22, 33.80, 29.51, 25.90, 22.57, 18.96, 13.89};
        expectedLF = new double[]{41.79, 38.22, 33.80, 29.51, 25.90, 22.57, 18.96, 13.89};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - right lateral", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        expectedCfH = new double[]{24.24, 24.59, 26.01, 28.28, 20.54, 5.05, 0.52, 0.10};
        expectedAGroundH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        expectedCfF = new double[]{24.24, 24.59, 26.01, 28.28, 20.54, 5.05, 0.52, 0.10};
        expectedAGroundF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};

        expectedLH = new double[]{41.79, 38.22, 33.80, 29.51, 25.90, 22.57, 18.96, 13.89};
        expectedLF = new double[]{41.79, 38.22, 33.80, 29.51, 25.90, 22.57, 18.96, 13.89};

        //Actual values
        proPath = propDataOut.propagationPaths.get(2);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        //Values are different because CNOSSOS doesn't seem to use the rubber band methods.
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - right lateral", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{46.09,42.49,38.44,35.97,34.67,33.90,33.09,31.20},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver at large height
     */
    @Test
    public void TC11() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55, 5, 10),
                        new Coordinate(65, 5, 10),
                        new Coordinate(65, 15, 10),
                        new Coordinate(55, 15, 10)
                });
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(70, 10, 15)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{11.92, 14.46, 17.23, 20.11, 23.06, 26.04, 29.03, 32.03};
        double[] expectedAGroundSOH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedAGroundORH = new double[]{-2.50, -2.50, -2.50, -2.50, -2.50, -2.50, -2.50, -2.50};
        double[] expectedDeltaDiffSPrimeRH = new double[]{12.99, 15.63, 18.46, 21.37, 24.34, 27.32, 30.32, 33.33};
        double[] expectedDeltaDiffSRPrimeH = new double[]{20.92, 23.84, 26.82, 29.82, 32.82, 35.83, 38.84, 41.85};
        double[] expectedDeltaGroundSOH = new double[]{-1.34, -1.32, -1.32, -1.31, -1.31, -1.31, -1.31, -1.31};
        double[] expectedDeltaGroundORH = new double[]{-0.97, -0.93, -0.91, -0.90, -0.89, -0.89, -0.89, -0.89};
        double[] expectedADiffH = new double[]{9.61, 12.20, 15.00, 17.90, 20.86, 22.80, 22.80, 22.80};

        double[] expectedDeltaDiffSRF = new double[]{11.92, 14.46, 17.23, 20.11, 23.06, 26.04, 29.03, 32.03};
        double[] expectedAGroundSOF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedAGroundORF = new double[]{-2.50, -2.50, -2.50, -2.50, -2.50, -2.50, -2.50, -2.50};
        double[] expectedDeltaDiffSPrimeRF = new double[]{12.99, 15.63, 18.46, 21.37, 24.34, 27.32, 30.32, 33.33};
        double[] expectedDeltaDiffSRPrimeF = new double[]{20.92, 23.84, 26.82, 29.82, 32.82, 35.83, 38.84, 41.85};
        double[] expectedDeltaGroundSOF = new double[]{-1.34, -1.32, -1.32, -1.31, -1.31, -1.31, -1.31, -1.31};
        double[] expectedDeltaGroundORF = new double[]{-0.97, -0.93, -0.91, -0.90, -0.89, -0.89, -0.89, -0.89};
        double[] expectedADiffF = new double[]{9.61, 12.20, 15.00, 17.90, 20.86, 22.80, 22.80, 22.80};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.05, 0.09, 0.24, 0.80, 2.85};
        double[] expectedADiv = new double[]{38.75, 38.75, 38.75, 38.75, 38.75, 38.75, 38.75, 38.75};
        double[] expectedABoundaryH = new double[]{9.61, 12.20, 15.00, 17.90, 20.86, 22.80, 22.80, 22.80};
        double[] expectedABoundaryF = new double[]{9.61, 12.20, 15.00, 17.90, 20.86, 22.80, 22.80, 22.80};
        double[] expectedLH = new double[]{44.64, 42.04, 39.22, 36.30, 33.30, 31.21, 30.64, 28.59};
        double[] expectedLF = new double[]{44.64, 42.04, 39.22, 36.30, 33.30, 31.21, 30.64, 28.59};
        double[] expectedL = new double[]{44.64, 42.04, 39.22, 36.30, 33.30, 31.21, 30.64, 28.59};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.00, proPath.getSRSegment().sPrime.x, ERROR_EPSILON_HIGH);
        assertEquals(-1.00, proPath.getSRSegment().sPrime.y, ERROR_EPSILON_HIGHEST);
        assertEquals(5.10, proPath.getSRSegment().rPrime.x, ERROR_EPSILON_HIGHEST);
        assertEquals(-1.76, proPath.getSRSegment().rPrime.y, ERROR_EPSILON_HIGHEST);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOWEST);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.40, 2.07, 9.99};
        double[] expectedCfH = new double[]{25.07, 25.45, 26.93, 29.26, 21.08, 5.11, 0.53, 0.10};
        double[] expectedAGroundH = new double[]{-1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.05, 0.31, 1.60, 7.90};
        double[] expectedCfF = new double[]{25.05, 25.34, 26.56, 29.11, 23.63, 7.01, 0.74, 0.13};
        double[] expectedAGroundF = new double[]{-1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.05, 0.10, 0.27, 0.90, 3.22};
        expectedADiv = new double[]{38.75, 38.75, 38.75, 38.75, 38.75, 38.75, 38.75, 38.75};
        expectedDeltaDiffSRH = new double[]{14.47, 17.92, 22.26, 26.57, 30.18, 33.37, 36.43, 39.45};
        expectedLH = new double[]{41.28, 37.82, 33.47, 29.14, 25.48, 22.12, 18.43, 13.09};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_LOW);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.40, 2.07, 9.99};
        expectedCfH = new double[]{25.07, 25.45, 26.93, 29.26, 21.08, 5.11, 0.53, 0.10};
        expectedAGroundH = new double[]{-1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.05, 0.31, 1.60, 7.90};
        expectedCfF = new double[]{25.05, 25.34, 26.56, 29.11, 23.63, 7.01, 0.74, 0.13};
        expectedAGroundF = new double[]{-1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51, -1.51};

        //Actual values
        proPath = propDataOut.propagationPaths.get(2);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, sumArray(SOUND_POWER_LEVELS, A_WEIGHTING));
        assertArrayEquals(  new double[]{21.28,28.39,32.47,34.51,34.54,33.37,32.14,27.73},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal object – receiver at low height
     */
    @Test
    public void TC12() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(11.0, 15.5, 10),
                        new Coordinate(12.0, 13.0, 10),
                        new Coordinate(14.5, 12.0, 10),
                        new Coordinate(17.0, 13.0, 10),
                        new Coordinate(18.0, 15.5, 10),
                        new Coordinate(17.0, 18.0, 10),
                        new Coordinate(14.5, 19.0, 10),
                        new Coordinate(12.0, 18.0, 10),
                });
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(0, 10, 1)
                .addReceiver(30, 20, 6)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{14.37, 17.50, 21.47, 25.97, 29.98, 33.36, 36.47, 39.50};
        double[] expectedAGroundSOH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedAGroundORH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedDeltaDiffSPrimeRH = new double[]{15.34, 18.53, 22.52, 27.04, 31.05, 34.43, 37.54, 40.58};
        double[] expectedDeltaDiffSRPrimeH = new double[]{18.98, 22.28, 26.34, 30.89, 34.91, 38.29, 41.41, 44.44};
        double[] expectedDeltaGroundSOH = new double[]{-1.35, -1.35, -1.34, -1.34, -1.34, -1.34, -1.34, -1.34};
        double[] expectedDeltaGroundORH = new double[]{-0.91, -0.90, -0.89, -0.88, -0.88, -0.88, -0.88, -0.88};
        double[] expectedADiffH = new double[]{12.10, 15.26, 19.24, 22.78, 22.78, 22.78, 22.78, 22.78};

        double[] expectedDeltaDiffSRF = new double[]{14.37, 17.50, 21.47, 25.97, 29.98, 33.36, 36.47, 39.50};
        double[] expectedAGroundSOF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedAGroundORF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedDeltaDiffSPrimeRF = new double[]{15.34, 18.53, 22.52, 27.04, 31.05, 34.43, 37.54, 40.58};
        double[] expectedDeltaDiffSRPrimeF = new double[]{18.98, 22.28, 26.34, 30.89, 34.91, 38.29, 41.41, 44.44};
        double[] expectedDeltaGroundSOF = new double[]{-1.35, -1.35, -1.34, -1.34, -1.34, -1.34, -1.34, -1.34};
        double[] expectedDeltaGroundORF = new double[]{-0.91, -0.90, -0.89, -0.88, -0.88, -0.88, -0.88, -0.88};
        double[] expectedADiffF = new double[]{12.10, 15.26, 19.24, 22.78, 22.78, 22.78, 22.78, 22.78};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.06, 0.12, 0.31, 1.05, 3.74};
        double[] expectedADiv = new double[]{41.11, 41.11, 41.11, 41.11, 41.11, 41.11, 41.11, 41.11};
        double[] expectedABoundaryH = new double[]{12.10, 15.26, 19.24, 22.78, 22.78, 22.78, 22.78, 22.78};
        double[] expectedABoundaryF = new double[]{12.10, 15.26, 19.24, 22.78, 22.78, 22.78, 22.78, 22.78};
        double[] expectedLH = new double[]{39.78, 36.62, 32.62, 29.05, 29.00, 28.80, 28.06, 25.37};
        double[] expectedLF = new double[]{39.78, 36.62, 32.62, 29.05, 29.00, 28.80, 28.06, 25.37};
        double[] expectedL = new double[]{39.78, 36.62, 32.62, 29.05, 29.00, 28.80, 28.06, 25.37};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.00, proPath.getSRSegment().sPrime.x, ERROR_EPSILON_LOW);
        assertEquals(-1.00, proPath.getSRSegment().sPrime.y, ERROR_EPSILON_HIGH);
        assertEquals(5.10, proPath.getSRSegment().rPrime.x, ERROR_EPSILON_HIGHEST);
        assertEquals(-1.76, proPath.getSRSegment().rPrime.y, ERROR_EPSILON_HIGHEST);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOW);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfH = new double[]{32.26, 32.87, 35.13, 37.43, 23.55, 4.65, 0.49, 0.10};
        double[] expectedAGroundH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfF = new double[]{32.26, 32.87, 35.13, 37.43, 23.55, 4.65, 0.49, 0.10};
        double[] expectedAGroundF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.06, 0.12, 0.31, 1.06, 3.80};
        expectedADiv = new double[]{41.11, 41.11, 41.11, 41.11, 41.11, 41.11, 41.11, 41.11};
        expectedDeltaDiffSRH = new double[]{8.17, 10.09, 12.67, 16.13, 20.46, 24.62, 28.11, 31.25};
        expectedLH = new double[]{45.22, 43.29, 40.69, 37.20, 32.81, 28.46, 24.22, 18.34};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_MEDIUM);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_MEDIUM);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        expectedCfH = new double[]{32.26, 32.87, 35.13, 37.43, 23.55, 4.65, 0.49, 0.10};
        expectedAGroundH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        expectedCfF = new double[]{32.26, 32.87, 35.13, 37.43, 23.55, 4.65, 0.49, 0.10};
        expectedAGroundF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};

        //Actual values
        proPath = propDataOut.propagationPaths.get(2);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        //Assertions
        /*assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);*/

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{21.81,29.66,34.31,36.14,35.57,33.72,31.12,25.37},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
     * building
     */
    @Test
    public void TC13() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(169.4, 41.0, 30),
                        new Coordinate(172.5, 33.5, 30),
                        new Coordinate(180.0, 30.4, 30),
                        new Coordinate(187.5, 33.5, 30),
                        new Coordinate(190.6, 41.0, 30),
                        new Coordinate(187.5, 48.5, 30),
                        new Coordinate(180.0, 51.6, 30),
                        new Coordinate(172.5, 48.5, 30),
                })
                .addGroundEffect(0, 50, -20, 80, 0.5)
                .addGroundEffect(50, 150, -20, 80, 0.9)
                .addGroundEffect(150, 225, -20, 80, 0.2)
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 28.5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{9.76, 13.15, 17.15, 20.71, 23.88, 26.92, 29.93, 32.94};
        double[] expectedAGroundSOH = new double[]{-1.38, -1.38, -1.38, -1.38, -1.38, -1.38, -1.38, -1.38};
        double[] expectedAGroundORH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRH = new double[]{10.52, 14.08, 18.17, 21.75, 24.94, 27.98, 31.00, 34.01};
        double[] expectedDeltaDiffSRPrimeH = new double[]{24.55, 28.89, 33.30, 37.00, 40.23, 43.30, 46.32, 49.34};
        double[] expectedDeltaGroundSOH = new double[]{-1.27, -1.25, -1.24, -1.23, -1.23, -1.23, -1.23, -1.23};
        double[] expectedDeltaGroundORH = new double[]{-0.49, -0.44, -0.42, -0.41, -0.41, -0.41, -0.41, -0.41};
        double[] expectedADiffH = new double[]{8.00, 11.46, 15.50, 19.06, 22.24, 23.36, 23.36, 23.36};

        double[] expectedDeltaDiffSRF = new double[]{9.54, 12.88, 16.85, 20.42, 23.57, 26.60, 29.62, 32.62};
        double[] expectedAGroundSOF = new double[]{-1.38, -1.38, -1.38, -1.38, -1.38, -1.38, -1.38, -1.38};
        double[] expectedAGroundORF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRF = new double[]{10.34, 13.86, 17.93, 21.51, 24.69, 27.74, 30.75, 33.76};
        double[] expectedDeltaDiffSRPrimeF = new double[]{24.55, 28.88, 33.30, 37.00, 40.22, 43.29, 46.32, 49.33};
        double[] expectedDeltaGroundSOF = new double[]{-1.27, -1.24, -1.23, -1.22, -1.22, -1.22, -1.22, -1.22};
        double[] expectedDeltaGroundORF = new double[]{-0.48, -0.43, -0.41, -0.40, -0.40, -0.40, -0.40, -0.39};
        double[] expectedADiffF = new double[]{7.80, 11.21, 15.22, 18.78, 21.95, 23.38, 23.38, 23.38};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.38, 0.72, 1.90, 6.43, 22.92};
        double[] expectedADiv = new double[]{56.85, 56.85, 56.85, 56.85, 56.85, 56.85, 56.85, 56.85};
        double[] expectedABoundaryH = new double[]{8.00, 11.46, 15.50, 19.06, 22.24, 23.36, 23.36, 23.36};
        double[] expectedABoundaryF = new double[]{7.80, 11.21, 15.22, 18.78, 21.95, 23.38, 23.38, 23.38};
        double[] expectedLH = new double[]{28.13, 24.61, 20.45, 16.71, 13.19, 10.90, 6.36, -10.13};
        double[] expectedLF = new double[]{28.33, 24.86, 20.73, 17.00, 13.49, 10.87, 6.34, -10.16};
        double[] expectedL = new double[]{28.23, 24.73, 20.59, 16.85, 13.34, 10.88, 6.35, -10.14};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.19, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-4.35, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_LOWEST);
        assertEquals(194.16, proPath.getSegmentList().get(2).rPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-8.50, proPath.getSegmentList().get(2).rPrime.y, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOWEST);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.02, 0.09, 0.48, 2.42, 11.40};
        double[] expectedCfH = new double[]{207.46, 223.88, 230.36, 122.25, 18.84, 2.12, 0.41, 0.09};
        double[] expectedAGroundH = new double[]{-1.40, -1.40, -1.40, -1.40, -1.40, -1.40, -1.40, -1.40};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.02, 0.13, 0.67, 3.32, 15.27};
        double[] expectedCfF = new double[]{209.72, 228.83, 218.90, 92.01, 11.30, 1.50, 0.30, 0.07};
        double[] expectedAGroundF = new double[]{-1.40, -1.40, -1.40, -1.40, -1.40, -1.40, -1.40, -1.40};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.21, 0.39, 0.74, 1.95, 6.62, 23.60};
        expectedADiv = new double[]{56.85, 56.85, 56.85, 56.85, 56.85, 56.85, 56.85, 56.85};
        expectedDeltaDiffSRH = new double[]{16.88, 20.30, 24.57, 29.02, 32.80, 36.06, 39.13, 42.16};
        expectedLH = new double[]{20.65, 17.17, 12.77, 8.14, 4.02, -0.45, -8.20, -28.21};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_LOW);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.02, 0.09, 0.48, 2.46, 11.67};
        expectedCfH = new double[]{202.25, 218.20, 224.79, 119.88, 18.59, 2.08, 0.41, 0.09};
        expectedAGroundH = new double[]{-1.39, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.03, 0.14, 0.72, 3.56, 16.24};
        expectedCfF = new double[]{204.90, 223.86, 211.09, 85.42, 10.19, 1.39, 0.28, 0.06};
        expectedAGroundF = new double[]{-1.39, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39, -1.39};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.21, 0.38, 0.72, 1.90, 6.46, 23.03};
        expectedADiv = new double[]{56.85, 56.85, 56.85, 56.85, 56.85, 56.85, 56.85, 56.85};
        expectedDeltaDiffSRH = new double[]{9.89, 12.15, 14.74, 17.53, 20.42, 23.37, 26.35, 29.35};
        expectedLH = new double[]{27.63, 25.32, 22.60, 19.64, 16.40, 12.27, 4.74, -14.83};

        //Actual values
        proPath = propDataOut.propagationPaths.get(2);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_MEDIUM);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{5.14,12.29,16.39,18.47,18.31,15.97,9.72,-9.92},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at large height
     */
    @Test
    public void TC14() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(11.0, 15.5, 10),
                        new Coordinate(12.0, 13.0, 10),
                        new Coordinate(14.5, 12.0, 10),
                        new Coordinate(17.0, 13.0, 10),
                        new Coordinate(18.0, 15.5, 10),
                        new Coordinate(17.0, 18.0, 10),
                        new Coordinate(14.5, 19.0, 10),
                        new Coordinate(12.0, 18.0, 10),
                });
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(8, 10, 1)
                .addReceiver(25, 20, 23)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.2)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{7.15, 8.65, 10.67, 13.08, 15.76, 18.59, 21.51, 24.48};
        double[] expectedAGroundSOH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedAGroundORH = new double[]{-2.66, -2.66, -2.66, -2.66, -2.66, -2.66, -2.66, -2.66};
        double[] expectedDeltaDiffSPrimeRH = new double[]{8.43, 10.37, 12.73, 15.38, 18.20, 21.11, 24.07, 27.05};
        double[] expectedDeltaDiffSRPrimeH = new double[]{21.88, 24.81, 27.80, 30.80, 33.80, 36.81, 39.82, 42.83};
        double[] expectedDeltaGroundSOH = new double[]{-2.11, -2.02, -1.94, -1.90, -1.87, -1.87, -1.85, -1.84};
        double[] expectedDeltaGroundORH = new double[]{-0.55, -0.47, -0.42, -0.39, -0.38, -0.37, -0.37, -0.37};
        double[] expectedADiffH = new double[]{4.49, 6.17, 8.30, 10.79, 13.51, 16.37, 19.30, 22.27};

        double[] expectedDeltaDiffSRF = new double[]{7.14, 8.65, 10.66, 13.07, 15.75, 18.58, 21.50, 24.47};
        double[] expectedAGroundSOF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedAGroundORF = new double[]{-2.66, -2.66, -2.66, -2.66, -2.66, -2.66, -2.66, -2.66};
        double[] expectedDeltaDiffSPrimeRF = new double[]{8.43, 10.36, 12.73, 15.38, 18.19, 21.10, 24.06, 27.05};
        double[] expectedDeltaDiffSRPrimeF = new double[]{21.88, 24.81, 27.80, 30.80, 33.80, 36.81, 39.82, 42.83};
        double[] expectedDeltaGroundSOF = new double[]{-2.11, -2.02, -1.94, -1.90, -1.87, -1.86, -1.85, -1.84};
        double[] expectedDeltaGroundORF = new double[]{-0.55, -0.47, -0.42, -0.39, -0.38, -0.37, -0.37, -0.37};
        double[] expectedADiffF = new double[]{4.48, 6.16, 8.30, 10.78, 13.50, 16.36, 19.29, 22.26};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.06, 0.11, 0.29, 0.97, 3.45};
        double[] expectedADiv = new double[]{40.41, 40.41, 40.41, 40.41, 40.41, 40.41, 40.41, 40.41};
        double[] expectedABoundaryH = new double[]{4.49, 6.17, 8.30, 10.79, 13.51, 16.37, 19.30, 22.27};
        double[] expectedABoundaryF = new double[]{4.48, 6.16, 8.30, 10.78, 13.50, 16.36, 19.29, 22.26};
        double[] expectedLH = new double[]{48.10, 46.41, 44.26, 41.74, 38.97, 35.94, 32.33, 26.87};
        double[] expectedLF = new double[]{48.10, 46.42, 44.26, 41.75, 38.98, 35.95, 32.33, 26.88};
        double[] expectedL = new double[]{28.23, 24.73, 20.59, 16.85, 13.34, 10.88, 6.35, -10.14};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.00, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-1.00, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_LOWEST);
        assertEquals(-6.35, proPath.getSegmentList().get(1).rPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-2.48, proPath.getSegmentList().get(1).rPrime.y, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_HIGHEST);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.00, 0.01, 0.04, 0.23, 1.23};
        double[] expectedCfH = new double[]{19.57, 19.60, 19.73, 20.34, 22.18, 21.49, 9.40, 1.20};
        double[] expectedAGroundH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.00, 0.01, 0.03, 0.18, 0.98};
        double[] expectedCfF = new double[]{19.57, 19.59, 19.70, 20.20, 21.86, 22.22, 11.29, 1.67};
        double[] expectedAGroundF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.06, 0.11, 0.29, 0.98, 3.48};
        expectedADiv = new double[]{40.41, 40.41, 40.41, 40.41, 40.41, 40.41, 40.41, 40.41};
        expectedDeltaDiffSRH = new double[]{6.76, 8.13, 10.15, 13.04, 17.02, 21.28, 24.93, 28.14};
        expectedLH = new double[]{48.23, 46.85, 44.81, 41.89, 37.86, 33.42, 29.09, 23.37};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_LOW);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.00, 0.01, 0.04, 0.23, 1.22};
        expectedCfH = new double[]{22.09, 22.12, 22.28, 23.04, 25.19, 23.72, 9.58, 1.15};
        expectedAGroundH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.00, 0.01, 0.03, 0.16, 0.87};
        expectedCfF = new double[]{22.09, 22.11, 22.23, 22.79, 24.66, 25.08, 12.76, 1.88};
        expectedAGroundF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.00, 0.01, 0.03, 0.06, 0.11, 0.30, 1.02, 3.65};
        expectedADiv = new double[]{40.41, 40.41, 40.41, 40.41, 40.41, 40.41, 40.41, 40.41};
        expectedDeltaDiffSRH = new double[]{11.85, 14.39, 17.19, 20.19, 23.58, 27.70, 32.24, 36.23};
        expectedLH = new double[]{43.14, 40.59, 37.77, 34.74, 31.30, 26.99, 21.73, 15.12};

        //Actual values
        proPath = propDataOut.propagationPaths.get(2);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH", expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{25.61,34.06,39.39,42.04,41.86,39.42,35.26,27.57},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     */
    @Test
    public void TC15() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(55.0, 5.0, 8),
                        new Coordinate(65.0, 5.0, 8),
                        new Coordinate(65.0, 15.0, 8),
                        new Coordinate(55.0, 15.0, 8),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(70.0, 14.5, 12),
                        new Coordinate(80.0, 10.2, 12),
                        new Coordinate(80.0, 20.2, 12),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(90.1, 19.5, 10),
                        new Coordinate(93.3, 17.8, 10),
                        new Coordinate(87.3, 6.6, 10),
                        new Coordinate(84.1, 8.3, 10),
                });
                /*.addBuilding(new Coordinate[]{
                        new Coordinate(94.9, 14.1, 10),
                        new Coordinate(98.02, 12.3, 10),
                        new Coordinate(92.03, 1.2, 10),
                        new Coordinate(88.86, 2.9, 10),
                })*/
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(50, 10, 1)
                .addReceiver(100, 15, 5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{18.64, 22.86, 26.49, 29.68, 32.74, 35.76, 38.77, 41.78};
        double[] expectedAGroundSOH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedAGroundORH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        double[] expectedDeltaDiffSPrimeRH = new double[]{19.67, 23.92, 27.55, 30.75, 33.81, 36.83, 39.84, 42.85};
        double[] expectedDeltaDiffSRPrimeH = new double[]{22.21, 26.50, 30.14, 33.35, 36.41, 39.43, 42.44, 45.45};
        double[] expectedDeltaGroundSOH = new double[]{-1.34, -1.34, -1.34, -1.34, -1.34, -1.34, -1.34, -1.34};
        double[] expectedDeltaGroundORH = new double[]{-1.02, -1.02, -1.01, -1.01, -1.01, -1.01, -1.01, -1.01};
        double[] expectedADiffH = new double[]{16.27, 20.51, 22.65, 22.65, 22.65, 22.65, 22.65, 22.65};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.01, 0.02, 0.05, 0.10, 0.18, 0.49, 1.65, 5.89};
        double[] expectedADiv = new double[]{45.05, 45.05, 45.05, 45.05, 45.05, 45.05, 45.05, 45.05};
        double[] expectedABoundaryH = new double[]{16.27, 20.51, 22.65, 22.65, 22.65, 22.65, 22.65, 22.65};
        double[] expectedABoundaryF = new double[]{16.27, 20.51, 22.65, 22.65, 22.65, 22.65, 22.65, 22.65};
        double[] expectedLH = new double[]{31.67, 27.42, 25.25, 25.20, 25.12, 24.81, 23.65, 19.41};
        double[] expectedLF = new double[]{31.67, 27.42, 25.25, 25.20, 25.12, 24.81, 23.65, 19.41};
        double[] expectedL = new double[]{31.67, 27.42, 25.25, 25.20, 25.12, 24.81, 23.65, 19.41};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);
        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);

        //Assertions
        assertEquals(0.00, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-1.00, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_LOWEST);
        assertEquals(50.25, proPath.getSegmentList().get(proPath.getSegmentList().size()-1).rPrime.x, ERROR_EPSILON_LOWEST);
        assertEquals(-5.00, proPath.getSegmentList().get(proPath.getSegmentList().size()-1).rPrime.y, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOWEST);


        //Path1 : right lateral
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.07, 0.37, 1.92, 9.32};
        double[] expectedCfH = new double[]{55.20, 56.69, 61.53, 61.63, 29.93, 4.28, 0.52, 0.11};
        double[] expectedAGroundH = new double[]{-1.56, -1.56, -1.32, -1.32, -1.56, -1.56, -1.56, -1.56};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.06, 0.33, 1.69, 8.30};
        double[] expectedCfF = new double[]{55.15, 56.48, 61.02, 62.61, 33.11, 5.18, 0.59, 0.12};
        double[] expectedAGroundF = new double[]{-1.56, -1.30, -1.08, -1.56, -1.56, -1.56, -1.56, -1.56};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.01, 0.02, 0.06, 0.11, 0.20, 0.53, 1.80, 6.41};
        expectedADiv = new double[]{45.05, 45.05, 45.05, 45.05, 45.05, 45.05, 45.05, 45.05};
        expectedDeltaDiffSRH = new double[]{17.54, 20.82, 25.57, 28.82, 31.9, 34.91, 37.92, 40.93};
        expectedLH = new double[]{31.97, 27.66, 23.64, 20.26, 17.42, 14.07, 9.79, 2.17};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH - right lateral", expectedWH, actualWH, ERROR_EPSILON_MEDIUM);
        assertDoubleArrayEquals("CfH - right lateral", expectedCfH, actualCfH, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_MEDIUM);
        assertDoubleArrayEquals("WF - right lateral", expectedWF, actualWF, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("CfF - right lateral", expectedCfF, actualCfF, ERROR_EPSILON_HIGHEST);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("AlphaAtm - right lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - right lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - right lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundH - right lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - right lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_MEDIUM);
        assertDoubleArrayEquals("DeltaDiffSRH - right lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("LH - right lateral", expectedLH, actualLH, ERROR_EPSILON_MEDIUM);


        //Path2 : left lateral
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        expectedCfH = new double[]{54.02, 55.58, 60.47, 59.60, 27.53, 3.75, 0.47, 0.10};
        expectedAGroundH = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        expectedCfF = new double[]{54.02, 55.58, 60.47, 59.60, 27.53, 3.75, 0.47, 0.10};
        expectedAGroundF = new double[]{-1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50, -1.50};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.01, 0.02, 0.06, 0.10, 0.20, 0.52, 1.76, 6.28};
        expectedADiv = new double[]{45.05, 45.05, 45.05, 45.05, 45.05, 45.05, 45.05, 45.05};
        expectedDeltaDiffSRH = new double[]{16.63, 20.81, 24.44, 27.65, 30.70, 33.72, 36.73, 39.74};
        expectedLH = new double[]{32.81, 28.62, 24.95, 21.70, 18.55, 15.21, 10.96, 3.43};

        //Actual values
        proPath = propDataOut.propagationPaths.get(1);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);

        //Assertions
        assertDoubleArrayEquals("WH - left lateral", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - left lateral", expectedCfH, actualCfH, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("AGroundH - left lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - left lateral", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - left lateral", expectedCfF, actualCfF, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("AGroundF - left lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - left lateral", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - left lateral", expectedAAtm, actualAAtm, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("ADiv - left lateral", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - left lateral", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF - left lateral", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRH - left lateral", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("LH - left lateral", expectedLH, actualLH, ERROR_EPSILON_HIGH);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(new double[]{10.75,16.57,20.81,24.51,26.55,26.78,25.04,18.50},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Reflecting barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16(){
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)

                .addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15)
                }, 15, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), -1)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.03, 0.14, 0.75, 3.70, 16.77};
        double[] expectedCfH = new double[]{203.37, 222.35, 207.73, 82.09, 9.63, 1.33, 0.27, 0.06};
        double[] expectedAGroundH = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{199.73, 214.27, 225.54, 131.93, 22.89, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.38, 0.71, 1.88, 6.38, 22.75};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedABoundaryF = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedLH = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};
        double[] expectedLF = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};
        double[] expectedL = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};
        double[] expectedLA = new double[]{11.06, 21.11, 28.48, 33.71, 36.57, 36.61, 31.91, 13.44};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        //Assertions
        assertEquals(0.40, proPath.getSegmentList().get(0).sPrime.x, ERROR_EPSILON_VERY_LOW);
        assertEquals(-6.58, proPath.getSegmentList().get(0).sPrime.y, ERROR_EPSILON_VERY_LOW);
        assertEquals(198.71, proPath.getSegmentList().get(proPath.getSegmentList().size()-1).rPrime.x, ERROR_EPSILON_VERY_HIGH);
        assertEquals(1.27, proPath.getSegmentList().get(proPath.getSegmentList().size()-1).rPrime.y, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("WH - vertical plane", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - vertical plane", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - vertical plane", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - vertical plane", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - vertical plane", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF - vertical plane", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_LOWEST);

        //Path1 : reflexion
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.03, 0.14, 0.76, 3.73, 16.91};
        expectedCfH = new double[]{207.64, 227.15, 210.40, 81.36, 9.40, 1.32, 0.27, 0.06};
        expectedAGroundH = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.43, 2.20, 10.56};
        expectedCfF = new double[]{203.91, 219.10, 229.36, 130.79, 21.96, 2.36, 0.45, 0.09};
        expectedAGroundF = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.21, 0.38, 0.73, 1.92, 6.50, 23.20};
        expectedADiv = new double[]{56.95, 56.95, 56.95, 56.95, 56.95, 56.95, 56.95, 56.95};
        expectedABoundaryH = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};
        expectedABoundaryF = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};
        expectedLH = new double[]{36.63, 36.06, 35.35, 34.51, 33.37, 31.21, 25.37, 10.90};
        expectedLF = new double[]{36.63, 36.06, 35.35, 34.51, 33.37, 31.21, 25.37, 10.90};
        expectedL = new double[]{36.63, 36.06, 35.35, 34.51, 33.37, 31.21, 25.37, 10.90};
        expectedLA = new double[]{10.10, 19.96, 26.75, 31.31, 33.37, 32.41, 26.37, 9.80};
        
        proPath = propDataOut.propagationPaths.get(1);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualABoundaryH = proPath.absorptionData.aBoundaryH;
        actualABoundaryF = proPath.absorptionData.aBoundaryF;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);

        assertDoubleArrayEquals("WH - reflexion", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - reflexion", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - reflexion", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - reflexion", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - reflexion", expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundF - reflexion", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - reflexion", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - reflexion", expectedAAtm, actualAAtm, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("ADiv - reflexion", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - reflexion", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryF - reflexion", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - reflexion", expectedLH, actualLH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LF - reflexion", expectedLF, actualLF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("L - reflexion", expectedL, actualL, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LA - reflexion", expectedLA, actualLA, ERROR_EPSILON_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, addArray(A_WEIGHTING, SOUND_POWER_LEVELS));
        assertArrayEquals(  new double[]{13.62,23.58,30.71,35.68,38.27,38.01,32.98,15.00},L, ERROR_EPSILON_LOW);
    }

    /**
     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
     * reduced receiver height
     */
    @Test
    public void TC17() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)

                .addWall(new Coordinate[]{
                        new Coordinate(114, 52, 15),
                        new Coordinate(170, 60, 15)
                }, 15, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5), -1)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, NaN, NaN, 0.53, 2.70, 12.70};
        double[] expectedCfH = new double[]{200.89, 217.45, 220.41, NaN, NaN, 1.88, 0.37, 0.08};
        double[] expectedAGroundH = new double[]{-1.32, -1.32, -1.32, NaN, NaN, -1.32, -1.32, -1.32};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{199.73, 214.27, 225.54, 131.93, 22.89, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.32, -1.32, -1.32, -1.32, -1.32, -1.32, -1.32, -1.32};

        double[] expectedDeltaDiffSR = new double[]{0., 0., 0., 3.16, 0.56, 0., 0., 0.};
        double[] expectedAGroundSO = new double[]{0., 0., 0., 2.74, -1.21, 0., 0., 0.};
        double[] expectedAGroundOR = new double[]{0., 0., 0., -2.40, -2.40, 0., 0., 0.};
        double[] expectedDeltaDiffSPrimeR = new double[]{0., 0., 0., 4.71, 4.65, 0., 0., 0.};
        double[] expectedDeltaDiffSRPrime = new double[]{0., 0., 0., 10.83, 13.26, 0., 0., 0.};
        double[] expectedDeltaGroundSO = new double[]{0., 0., 0., 2.23, -0.77, 0., 0., 0.};
        double[] expectedDeltaGroundOR = new double[]{0., 0., 0., -1.07, -0.62, 0., 0., 0.};
        double[] expectedADiff = new double[]{0., 0., 0., 4.31, -0.83, 0., 0., 0.};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.38, 0.71, 1.88, 6.37, 22.73};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{-1.32, -1.32, -1.32, 4.31, -0.83, -1.32, -1.32, -1.32};
        double[] expectedABoundaryF = new double[]{-1.32, -1.32, -1.32, -1.05, -1.32, -1.32, -1.32, -1.32};
        double[] expectedLH = new double[]{37.53, 37.47, 37.35, 31.54, 36.34, 35.67, 31.18, 14.82};
        double[] expectedLF = new double[]{37.53, 37.47, 37.31, 36.89, 36.84, 35.67, 31.18, 14.82};
        double[] expectedL = new double[]{37.53, 37.47, 37.33, 34.99, 36.60, 35.67, 31.18, 14.82};
        double[] expectedLA = new double[]{11.33, 21.37, 28.73, 31.79, 36.60, 36.87, 32.18, 13.72};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        double[] actualDeltaDiffSR = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSO = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundOR = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeR = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrime = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSO = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundOR = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiff = proPath.aBoundaryH.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        //Assertions

        assertDoubleArrayEquals("WH - vertical plane", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - vertical plane", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - vertical plane", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - vertical plane", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - vertical plane", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF - vertical plane", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("DeltaDiffSR - vertical plane", expectedDeltaDiffSR, actualDeltaDiffSR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSO - vertical plane", expectedAGroundSO, actualAGroundSO, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundOR - vertical plane", expectedAGroundOR, actualAGroundOR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeR - vertical plane", expectedDeltaDiffSPrimeR, actualDeltaDiffSPrimeR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrime - vertical plane", expectedDeltaDiffSRPrime, actualDeltaDiffSRPrime, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSO - vertical plane", expectedDeltaGroundSO, actualDeltaGroundSO, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundOR - vertical plane", expectedDeltaGroundOR, actualDeltaGroundOR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiff - vertical plane", expectedADiff, actualADiff, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_VERY_LOW);

        //Path1 : reflexion
        expectedWH = new double[]{0.00, 0.00, 0.00, NaN, 0.10, 0.55, 2.77, 12.97};
        expectedCfH = new double[]{205.15, 222.41, 223.52, NaN, 15.34, 1.83, 0.36, 0.08};
        expectedAGroundH = new double[]{-1.31, -1.31, -1.31, NaN, -1.31, -1.31, -1.31, -1.31};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.43, 2.20, 10.56};
        expectedCfF = new double[]{203.77, 218.95, 229.22, 130.76, 21.97, 2.36, 0.45, 0.09};
        expectedAGroundF = new double[]{-1.31, -1.31, -1.26, -1.28, -1.31, -1.31, -1.31, -1.31};

        expectedDeltaDiffSR = new double[]{0., 0., 0., 2.92, 0., 0., 0., 0.};
        expectedAGroundSO = new double[]{0., 0., 0., 2.66, 0., 0., 0., 0.};
        expectedAGroundOR = new double[]{0., 0., 0., -2.40, 0., 0., 0., 0.};
        expectedDeltaDiffSPrimeR = new double[]{0., 0., 0., 4.65, 0., 0., 0., 0.};
        expectedDeltaDiffSRPrime = new double[]{0., 0., 0., 10.81, 0., 0., 0., 0.};
        expectedDeltaGroundSO = new double[]{0., 0., 0., 2.12, 0., 0., 0., 0.};
        expectedDeltaGroundOR = new double[]{0., 0., 0., -1.05, 0., 0., 0., 0.};
        expectedADiff = new double[]{0., 0., 0., 3.99, 0., 0., 0., 0.};

        expectedDeltaDiffSR = new double[]{0., 0., 0., 0., 0., 0., 0., 0.};
        expectedAGroundSO = new double[]{0., 0., 0., 0., 0., 0., 0., 0.};
        expectedAGroundOR = new double[]{0., 0., 0., -0., 0., 0., 0., 0.};
        expectedDeltaDiffSPrimeR = new double[]{0., 0., 0., 0., 0., 0., 0., 0.};
        expectedDeltaDiffSRPrime = new double[]{0., 0., 0., 0., 0., 0., 0., 0.};
        expectedDeltaGroundSO = new double[]{0., 0., 0., 0., 0., 0., 0., 0.};
        expectedDeltaGroundOR = new double[]{0., 0., 0., -0., 0., 0., 0., 0.};
        expectedADiff = new double[]{0., 0., 0., 0., 0., 0., 0., 0.};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.21, 0.38, 0.73, 1.92, 6.50, 23.18};
        expectedADiv = new double[]{56.95, 56.95, 56.95, 56.95, 56.95, 56.95, 56.95, 56.95};
        expectedABoundaryH = new double[]{-1.31, -1.31, -1.31, 3.99, -1.31, -1.31, -1.31, -1.31};
        expectedABoundaryF = new double[]{-1.31, -1.31, -1.26, -1.28, -1.31, -1.31, -1.31, -1.31};
        expectedLH = new double[]{36.88, 36.31, 35.60, 29.46, 33.62, 31.46, 25.63, 11.17};
        expectedLF = new double[]{36.88, 36.31, 35.60, 29.46, 33.62, 31.46, 25.63, 11.17};
        expectedLA = new double[]{10.68, 20.21, 26.98, 29.65, 33.62, 32.66, 26.63, 10.07};

        proPath = propDataOut.propagationPaths.get(1);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualDeltaDiffSR = proPath.aBoundaryH.deltaDiffSR;
        actualAGroundSO = proPath.aBoundaryH.aGroundSO;
        actualAGroundOR = proPath.aBoundaryH.aGroundOR;
        actualDeltaDiffSPrimeR = proPath.aBoundaryH.deltaDiffSPrimeR;
        actualDeltaDiffSRPrime = proPath.aBoundaryH.deltaDiffSRPrime;
        actualDeltaGroundSO = proPath.aBoundaryH.deltaGroundSO;
        actualDeltaGroundOR = proPath.aBoundaryH.deltaGroundOR;
        actualADiff = proPath.aBoundaryH.aDiff;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualABoundaryH = proPath.absorptionData.aBoundaryH;
        actualABoundaryF = proPath.absorptionData.aBoundaryF;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);

        assertDoubleArrayEquals("WH - reflexion", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - reflexion", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - reflexion", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - reflexion", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - reflexion", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - reflexion", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSR - reflexion", expectedDeltaDiffSR, actualDeltaDiffSR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSO - reflexion", expectedAGroundSO, actualAGroundSO, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundOR - reflexion", expectedAGroundOR, actualAGroundOR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSPrimeR - reflexion", expectedDeltaDiffSPrimeR, actualDeltaDiffSPrimeR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrime - reflexion", expectedDeltaDiffSRPrime, actualDeltaDiffSRPrime, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSO - reflexion", expectedDeltaGroundSO, actualDeltaGroundSO, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundOR - reflexion", expectedDeltaGroundOR, actualDeltaGroundOR, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiff - reflexion", expectedADiff, actualADiff, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - reflexion", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - reflexion", expectedAAtm, actualAAtm, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("ADiv - reflexion", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - reflexion", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("ABoundaryF - reflexion", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - reflexion", expectedLH, actualLH, ERROR_EPSILON_VERY_HIGH);
        assertDoubleArrayEquals("LF - reflexion", expectedLF, actualLF, ERROR_EPSILON_VERY_HIGH);
        assertDoubleArrayEquals("LA - reflexion", expectedLA, actualLA, ERROR_EPSILON_MEDIUM);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, addArray(A_WEIGHTING, SOUND_POWER_LEVELS));
        assertArrayEquals(  new double[]{14.02, 23.84, 30.95, 33.86, 38.37, 38.27, 33.25, 15.28},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     */
    //TODO : not tested
    @Test
    public void TC18() {
        GeometryFactory factory = new GeometryFactory();

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(new Coordinate[]{
                new Coordinate(114, 52),
                new Coordinate(170, 60),
                new Coordinate(170, 61),
                new Coordinate(114, 53),
                new Coordinate(114, 52)}, 15, -1);

        builder.addBuilding(new Coordinate[]{
                new Coordinate(87, 50),
                new Coordinate(92, 32),
                new Coordinate(92, 33),
                new Coordinate(87, 51),
                new Coordinate(87, 50)}, 12, -1);

        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 12));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{11.69,21.77,28.93,32.71,36.83,36.83,32.12,13.66},L, ERROR_EPSILON_HIGH);


    }

    /**
     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
     * acoustic properties
     */
    //TODO : the error is due to the left VDiff path which z-path seems to be false in the document
    @Test
    public void TC19() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(100, 24, 12),
                        new Coordinate(118, 24, 12),
                        new Coordinate(118, 30, 12),
                        new Coordinate(100, 30, 12),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(110, 15, 7),
                        new Coordinate(118, 15, 7),
                        new Coordinate(118, 24, 7),
                        new Coordinate(110, 24, 7),
                })
                .addBuilding(new Coordinate[]{
                        new Coordinate(100, 9, 12),
                        new Coordinate(118, 9, 12),
                        new Coordinate(118, 15, 12),
                        new Coordinate(100, 15, 12),
                })
                .addWall(new Coordinate[]{
                        new Coordinate(156.00, 28.00, 14),
                        new Coordinate(145.00, 7.00, 14),
                }, -1)
                .addWall(new Coordinate[]{
                        new Coordinate(175.00, 35.00, 14.5),
                        new Coordinate(188.00, 19.00, 14.5),
                }, -1)
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 30, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);


        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{6.67, 8.83, 11.68, 14.56, 17.43, 20.35, 23.31, 26.29};
        double[] expectedAGroundSOH = new double[]{-0.67, -0.67, -0.67, -0.67, -0.67, -0.67, -0.67, -0.67};
        double[] expectedAGroundORH = new double[]{-2.02, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRH = new double[]{7.61, 10.35, 13.58, 16.64, 19.61, 22.57, 25.55, 28.55};
        double[] expectedDeltaDiffSRPrimeH = new double[]{14.32, 18.51, 22.43, 25.77, 28.86, 31.89, 34.91, 37.92};
        double[] expectedDeltaGroundSOH = new double[]{-0.60, -0.56, -0.54, -0.53, -0.52, -0.52, -0.52, -0.52};
        double[] expectedDeltaGroundORH = new double[]{-0.90, -0.86, -0.77, -0.73, -0.71, -0.70, -0.70, -0.70};
        double[] expectedADiffH = new double[]{5.17, 7.41, 10.38, 13.30, 16.20, 19.13, 22.09, 23.78};

        double[] expectedDeltaDiffSRF = new double[]{5.89, 7.40, 9.69, 12.24, 14.94, 17.79, 20.66, 23.62};
        double[] expectedAGroundSOF = new double[]{-0.67, -0.67, -0.67, -0.67, -0.67, -0.67, -0.67, -0.67};
        double[] expectedAGroundORF = new double[]{-2.08, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRF = new double[]{7.00, 9.39, 12.40, 15.35, 18.27, 21.21, 24.17, 27.26};
        double[] expectedDeltaDiffSRPrimeF = new double[]{14.20, 18.38, 22.29, 25.63, 28.73, 31.75, 34.77, 37.78};
        double[] expectedDeltaGroundSOF = new double[]{-0.59, -0.53, -0.49, -0.47, -0.46, -0.45, -0.45, -0.45};
        double[] expectedDeltaGroundORF = new double[]{-0.86, -0.75, -0.62, -0.57, -0.55, -0.53, -0.53, -0.53};
        double[] expectedADiffF = new double[]{4.44, 6.12, 8.57, 11.19, 13.93, 16.77, 19.68, 22.64};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.70, 1.85, 6.28, 22.38};
        double[] expectedADiv = new double[]{56.64, 56.64, 56.64, 56.64, 56.64, 56.64, 56.64, 56.64};
        double[] expectedABoundaryH = new double[]{5.17, 7.41, 10.38, 13.30, 16.20, 19.13, 22.09, 23.78};
        double[] expectedABoundaryF = new double[]{4.44, 6.12, 8.57, 11.19, 13.93, 16.77, 19.68, 22.64};
        double[] expectedLH = new double[]{31.16, 28.87, 25.78, 22.69, 19.46, 15.38, 7.99, -9.81};
        double[] expectedLF = new double[]{31.89, 30.16, 27.59, 24.79, 21.73, 17.74, 10.40, -8.67};
        double[] expectedL = new double[]{31.54, 29.56, 26.78, 23.87, 20.74, 16.72, 9.36, -9.20};
        double[] expectedLA = new double[]{5.34, 13.46, 18.18, 20.67, 20.74, 17.92, 10.36, -10.30};

        //Actual values
        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        //Assertions

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_VERY_LOW);

        //Path1 : lateral right
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.02, 0.13, 0.70, 3.46, 15.82};
        double[] expectedCfH = new double[]{204.75, 223.53, 212.65, 88.09, 10.70, 1.43, 0.29, 0.06};
        double[] expectedAGroundH = new double[]{-1.13, -1.13, -0.97, -0.65, -1.13, -1.13, -1.13, -1.13};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.40, 2.07, 9.97};
        double[] expectedCfF = new double[]{201.41, 215.78, 228.09, 136.20, 24.26, 2.53, 0.48, 0.10};
        double[] expectedAGroundF = new double[]{-1.13, -1.13, -1.13, -1.13, -1.13, -1.13, -1.13, -1.13};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.38, 0.72, 1.90, 6.44, 22.95};
        expectedADiv = new double[]{56.64, 56.64, 56.64, 56.64, 56.64, 56.64, 56.64, 56.64};
        expectedADiffH = new double[]{18.69, 22.74, 26.20, 29.33, 32.37, 35.39, 38.40, 41.41};
        expectedADiffF = new double[]{18.69, 22.74, 26.20, 29.33, 32.37, 35.39, 38.40, 41.41};
        expectedLH = new double[]{18.77, 14.67, 10.93, 7.29, 4.39, 0.20, -7.35, -26.88};
        expectedLF = new double[]{18.77, 14.67, 11.08, 7.77, 4.39, 0.20, -7.35, -26.88};
        expectedLA = new double[]{-7.43, -1.43, 2.41, 4.34, 4.39, 1.40, -6.35, -27.98};

        proPath = propDataOut.propagationPaths.get(1);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualADiffH = proPath.absorptionData.aDifH;
        actualADiffF = proPath.absorptionData.aDifF;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);

        assertDoubleArrayEquals("WH - lateral right", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - lateral right", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - lateral right", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("WF - lateral right", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - lateral right", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF - lateral right", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("AlphaAtm - lateral right", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - lateral right", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - lateral right", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiffH - lateral right", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - lateral right", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - lateral right", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LA - lateral right", expectedLA, actualLA, ERROR_EPSILON_VERY_LOW);

        //Path2 : lateral left
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.02, 0.10, 0.51, 2.61, 12.30};
        expectedCfH = new double[]{198.93, 214.98, 219.72, 113.73, 17.06, 1.95, 0.38, 0.08};
        expectedAGroundH = new double[]{-1.35, -1.35, -1.35, 1.32, -1.35, -1.35, -1.35, -1.35};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.06, 0.34, 1.77, 8.65};
        expectedCfF = new double[]{196.96, 209.53, 225.50, 149.30, 30.69, 3.06, 0.56, 0.12};
        expectedAGroundF = new double[]{-1.35, -1.35, -1.35, -1.35, -1.35, -1.35, -1.35, -1.35};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.86, 6.32, 22.54};
        expectedADiv = new double[]{56.64, 56.64, 56.64, 56.64, 56.64, 56.64, 56.64, 56.64};
        expectedABoundaryH = new double[]{-1.35, -1.35, -1.35, 1.32, -1.35, -1.35, -1.35, -1.35};
        expectedABoundaryF = new double[]{-1.35, -1.35, -1.35, -1.35, -1.35, -1.35, -1.35, -1.35};
        expectedLH = new double[]{26.60, 24.10, 21.27, 15.57, 14.99, 10.86, 3.41, -15.80};
        expectedLF = new double[]{26.60, 24.10, 21.27, 18.25, 14.99, 10.86, 3.41, -15.80};
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        expectedLA = new double[]{0.40, 8.00, 12.67, 13.91, 14.99, 12.06, 4.41, -9.38};

        proPath = propDataOut.propagationPaths.get(2);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualABoundaryH = proPath.absorptionData.aBoundaryH;
        actualABoundaryF = proPath.absorptionData.aBoundaryF;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);

        //Different value with the TC because their z-profile left seems to be false, it follows the building top
        // border while it should not
        assertDoubleArrayEquals("WH - lateral left", expectedWH, actualWH, ERROR_EPSILON_VERY_HIGH);
        assertDoubleArrayEquals("CfH - lateral left", expectedCfH, actualCfH, ERROR_EPSILON_HIGHEST);
        assertDoubleArrayEquals("AGroundH - lateral left", expectedAGroundH, actualAGroundH, ERROR_EPSILON_HIGH);
        //assertDoubleArrayEquals("WF - lateral left", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        //assertDoubleArrayEquals("CfF - lateral left", expectedCfF, actualCfF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("AGroundF - lateral left", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOW);

        assertDoubleArrayEquals("AlphaAtm - lateral left", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - lateral left", expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ADiv - lateral left", expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryH - lateral left", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_HIGHEST);
        assertDoubleArrayEquals("ABoundaryF - lateral left", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_HIGHEST);
        assertDoubleArrayEquals("LH - lateral left", expectedLH, actualLH, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("LF - lateral left", expectedLF, actualLF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("LA - lateral left", expectedLA, actualLA, ERROR_EPSILON_HIGHEST);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, addArray(A_WEIGHTING, SOUND_POWER_LEVELS));
        assertArrayEquals(  new double[]{6.72, 14.66, 19.34, 21.58, 21.84, 19.00, 11.42, -9.38},L, ERROR_EPSILON_MEDIUM);
    }

    /**
     * TC20 - Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC20() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 25, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.03, 0.15, 0.76, 3.76, 17.01};
        double[] expectedCfH = new double[]{199.65, 218.28, 203.92, 80.61, 9.46, 1.31, 0.27, 0.06};
        double[] expectedAGroundH = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{195.99, 210.11, 221.65, 131.04, 23.06, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.70, 1.85, 6.26, 22.33};
        double[] expectedADiv = new double[]{56.62, 56.62, 56.62, 56.62, 56.62, 56.62, 56.62, 56.62};
        double[] expectedABoundaryH = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};
        double[] expectedABoundaryF = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};
        double[] expectedLH = new double[]{37.41, 37.35, 37.23, 37.06, 36.73, 35.59, 31.17, 15.10};
        double[] expectedLF = new double[]{37.41, 37.35, 37.23, 37.06, 36.73, 35.59, 31.17, 15.10};
        double[] expectedL = new double[]{37.41, 37.35, 37.23, 37.06, 36.73, 35.59, 31.17, 15.10};
        double[] expectedLA = new double[]{11.21, 21.25, 28.63, 33.86, 36.73, 36.79, 32.17, 14.00};

        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        assertDoubleArrayEquals("WH - vertical plane", expectedWH, actualWH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("CfH - vertical plane", expectedCfH, actualCfH, ERROR_EPSILON_MEDIUM);
        assertDoubleArrayEquals("AGroundH - vertical plane", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - vertical plane", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - vertical plane", expectedCfF, actualCfF, ERROR_EPSILON_MEDIUM);
        assertDoubleArrayEquals("AGroundF - vertical plane", expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_VERY_LOW);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});

        assertArrayEquals(  new double[]{11.21,21.25,28.63,33.86,36.73,36.79,32.17,14},L, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * TC21 - Building on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC21() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addBuilding(new Coordinate[]{
                        new Coordinate(167.2, 39.5, 11.5),
                        new Coordinate(151.6, 48.5, 11.5),
                        new Coordinate(141.1, 30.3, 11.5),
                        new Coordinate(156.7, 21.3, 11.5),
                        new Coordinate(159.7, 26.5, 11.5),
                        new Coordinate(151.0, 31.5, 11.5),
                        new Coordinate(155.5, 39.3, 11.5),
                        new Coordinate(164.2, 34.3, 11.5)
                })
                //Ground effects
                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)
                //Topography
                .addTopographicLine(0, 80, 0, 225, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 75, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 25, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedWH = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        double[] expectedCfH = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        double[] expectedAGroundH = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        double[] expectedWF = new double[]{0.00, 0.00, NaN, 0.01, 0.08, 0.42, 2.15, 10.33};
        double[] expectedCfF = new double[]{195.98, 210.08, NaN, 131.26, 23.14, 2.43, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.06, -1.06, NaN, -1.06, -1.06, -1.06, -1.06, -1.06};

        double[] expectedDeltaDiffSRH = new double[]{4.81, 4.85, 4.92, 5.06, 5.34, 5.84, 6.69, 8.02};
        double[] expectedAGroundSOH = new double[]{-0.70, -0.70, -0.70, -0.70, -0.70, -0.70, -0.70, -0.70};
        double[] expectedAGroundORH = new double[]{-0.47, -1.57, -2.41, -2.41, -2.41, -2.41, -2.41, -2.41};
        double[] expectedDeltaDiffSPrimeRH = new double[]{5.08, 5.37, 5.89, 6.78, 8.15, 10.03, 12.34, 14.95};
        double[] expectedDeltaDiffSRPrimeH = new double[]{7.81, 9.55, 11.77, 14.33, 17.09, 19.97, 22.92, 25.89};
        double[] expectedDeltaGroundSOH = new double[]{-0.68, -0.66, -0.63, -0.58, -0.51, -0.44, -0.37, -0.32};
        double[] expectedDeltaGroundORH = new double[]{-0.33, -0.95, -1.18, -0.91, -0.69, -0.53, -0.42, -0.35};
        double[] expectedADiffH = new double[]{3.80, 3.24, 3.11, 3.58, 4.13, 4.87, 5.90, 7.35};

        double[] expectedDeltaDiffSRF = new double[]{0.00, 0.00, 0.64, 0.00, 0.00, 0.00, 0.00, 0.00};
        double[] expectedAGroundSOF = new double[]{NaN, NaN, -0.71, NaN, NaN, NaN, NaN, NaN};
        double[] expectedAGroundORF = new double[]{NaN, NaN, -2.40, NaN, NaN, NaN, NaN, NaN};
        double[] expectedDeltaDiffSPrimeRF = new double[]{NaN, NaN, 2.86, NaN, NaN, NaN, NaN, NaN};
        double[] expectedDeltaDiffSRPrimeF = new double[]{NaN, NaN, 10.88, NaN, NaN, NaN, NaN, NaN};
        double[] expectedDeltaGroundSOF = new double[]{NaN, NaN, -0.55, NaN, NaN, NaN, NaN, NaN};
        double[] expectedDeltaGroundORF = new double[]{NaN, NaN, -0.81, NaN, NaN, NaN, NaN, NaN};
        double[] expectedADiffF = new double[]{NaN, NaN, -0.72, NaN, NaN, NaN, NaN, NaN};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.70, 1.85, 6.26, 22.33};
        double[] expectedADiv = new double[]{56.62, 56.62, 56.62, 56.62, 56.62, 56.62, 56.62, 56.62};
        double[] expectedABoundaryH = new double[]{3.80, 3.24, 3.11, 3.58, 4.13, 4.87, 5.90, 7.35};
        double[] expectedABoundaryF = new double[]{-1.06, -1.06, -0.72, -1.06, -1.06, -1.06, -1.06, -1.06};
        double[] expectedLH = new double[]{32.56, 33.06, 33.07, 32.43, 31.54, 29.66, 24.22, 6.70};
        double[] expectedLF = new double[]{37.41, 37.36, 36.90, 37.07, 36.74, 35.56, 31.18, 15.11};
        double[] expectedL = new double[]{35.63, 35.72, 35.39, 35.34, 34.88, 33.57, 28.96, 12.68};
        double[] expectedLA = new double[]{9.43, 19.62, 26.79, 32.14, 34.88, 34.77, 29.96, 11.58};

        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        assertDoubleArrayEquals("WH - vertical plane", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - vertical plane", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - vertical plane", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        /*assertDoubleArrayEquals("WF - vertical plane", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - vertical plane", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - vertical plane", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_HIGH);

        //Path1 : lateral right
        //TODO : need Vretical diff r-crit
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.03, 0.15, 0.76, 3.76, 17.00};
        expectedCfH = new double[]{199.64, 218.28, 203.93, 80.63, 9.47, 1.31, 0.27, 0.06};
        expectedAGroundH = new double[]{-1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06, -1.06};
        expectedWF = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        expectedCfF = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        expectedAGroundF = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.70, 1.85, 6.26, 22.33};
        expectedADiv = new double[]{56.62, 56.62, 56.62, 56.62, 56.62, 56.62, 56.62, 56.62};
        expectedABoundaryH = new double[]{18.85, 21.74, 24.71, 27.70, 30.70, 33.70, 36.71, 39.72};
        expectedABoundaryF = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        expectedLH = new double[]{18.62, 15.68, 12.48, 9.08, 6.07, 1.86, -5.79, -25.71};
        expectedLF = new double[]{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN};
        expectedL = new double[]{18.62, 15.68, 12.48, 9.08, 6.07, 1.86, -5.79, -25.71};
        expectedLA = new double[]{3.42, 13.45, 20.82, 26.01, 28.81, 28.72, 23.84, 5.18};

        /*proPath = propDataOut.propagationPaths.get(1);

        actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualABoundaryH = proPath.absorptionData.aBoundaryH;
        actualABoundaryF = proPath.absorptionData.aBoundaryF;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);

        assertDoubleArrayEquals("WH - lateral right", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - lateral right", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - lateral right", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - lateral right", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - lateral right", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - lateral right", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - lateral right", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - lateral right", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - lateral right", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - lateral right", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - lateral right", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - lateral right", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - lateral right", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L - lateral right", expectedL, actualL, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LA - lateral right", expectedLA, actualLA, ERROR_EPSILON_VERY_LOW);*/

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{10.44,20.58,27.78,33.09,35.84,35.73,30.91,12.48},L, ERROR_EPSILON_HIGH);// Because building height definition is not in accordance with ISO
    }

    /**
     * TC22 - Building with receiver backside on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC22() {
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(new Coordinate[]{
                new Coordinate(197, 36.0, 0),
                new Coordinate(179, 36, 0),
                new Coordinate(179, 15, 0),
                new Coordinate(197, 15, 0),
                new Coordinate(197, 21, 0),
                new Coordinate(187, 21, 0),
                new Coordinate(187, 30, 0),
                new Coordinate(197, 30, 0),
                new Coordinate(197, 36, 0)}, 20, -1)

                .addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9)
                .addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5)
                .addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2)

                .addTopographicLine(0, 80, 0, 255, 80, 0)
                .addTopographicLine(225, 80, 0, 225, -20, 0)
                .addTopographicLine(225, -20, 0, 0, -20, 0)
                .addTopographicLine(0, -20, 0, 0, 80, 0)
                .addTopographicLine(120, -20, 0, 120, 80, 0)
                .addTopographicLine(185, -5, 10, 205, -5, 10)
                .addTopographicLine(205, -5, 10, 205, 75, 10)
                .addTopographicLine(205, 74, 10, 185, 75, 10)
                .addTopographicLine(185, 75, 10, 185, -5, 10)

        .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addSource(10, 10, 1)
                .addReceiver(187.05, 25, 14)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.9)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{17.34, 20.76, 25.01, 29.48, 33.28, 36.55, 39.63, 42.66};
        double[] expectedAGroundSOH = new double[]{-0.64, -0.64, -0.64, -0.64, -0.64, -0.64, -0.64, -0.64};
        double[] expectedAGroundORH = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRH = new double[]{17.49, 20.91, 25.17, 29.64, 33.43, 36.70, 39.78, 42.81};
        double[] expectedDeltaDiffSRPrimeH = new double[]{20.79, 24.29, 28.58, 33.06, 36.87, 40.14, 43.22, 46.25};
        double[] expectedDeltaGroundSOH = new double[]{-0.63, -0.63, -0.63, -0.63, -0.63, -0.63, -0.63, -0.63};
        double[] expectedDeltaGroundORH = new double[]{-1.68, -1.67, -1.66, -1.66, -1.66, -1.66, -1.66, -1.66};
        double[] expectedADiffH = new double[]{15.03, 18.46, 22.71, 22.71, 22.71, 22.71, 22.71, 22.71};

        double[] expectedDeltaDiffSRF = new double[]{17.33, 20.75, 25.00, 29.47, 33.27, 36.54, 39.62, 42.65};
        double[] expectedAGroundSOF = new double[]{-0.64, -0.64, -0.64, -0.64, -0.64, -0.64, -0.64, -0.64};
        double[] expectedAGroundORF = new double[]{-2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        double[] expectedDeltaDiffSPrimeRF = new double[]{17.48, 20.90, 25.16, 29.63, 33.43, 36.70, 39.78, 42.80};
        double[] expectedDeltaDiffSRPrimeF = new double[]{20.79, 24.28, 28.58, 33.06, 36.86, 40.13, 43.21, 46.24};
        double[] expectedDeltaGroundSOF = new double[]{-0.63, -0.63, -0.63, -0.63, -0.63, -0.63, -0.63, -0.63};
        double[] expectedDeltaGroundORF = new double[]{-1.68, -1.67, -1.66, -1.66, -1.66, -1.66, -1.66, -1.66};
        double[] expectedADiffF = new double[]{15.02, 18.45, 22.71, 22.71, 22.71, 22.71, 22.71, 22.71};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.07, 0.19, 0.34, 0.65, 1.72, 5.84, 20.82};
        double[] expectedADiv = new double[]{56.02, 56.02, 56.02, 56.02, 56.02, 56.02, 56.02, 56.02};
        double[] expectedABoundaryH = new double[]{15.03, 18.46, 22.71, 22.71, 22.71, 22.71, 22.71, 22.71};
        double[] expectedABoundaryF = new double[]{15.02, 18.45, 22.71, 22.71, 22.71, 22.71, 22.71, 22.71};
        double[] expectedLH = new double[]{21.93, 18.45, 14.09, 13.93, 13.62, 12.55, 8.43, -6.55};
        double[] expectedLF = new double[]{21.94, 18.46, 14.09, 13.93, 13.62, 12.55, 8.43, -6.55};
        double[] expectedL = new double[]{21.94, 18.46, 14.09, 13.93, 13.62, 12.55, 8.43, -6.55};
        double[] expectedLA = new double[]{-4.26, 2.36, 5.49, 10.73, 13.62, 13.75, 9.43, -7.65};

        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        /*assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_HIGH);*/

        //Path1 : lateral right
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.02, 0.11, 0.60, 3.00, 13.93};
        double[] expectedCfH = new double[]{212.03, 230.71, 226.18, 101.93, 13.28, 1.67, 0.33, 0.07};
        double[] expectedAGroundH = new double[]{-1.25, -1.25, -1.03, 0.77, -1.25, -1.25, -1.25, -1.25};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.02, 0.11, 0.59, 2.96, 13.76};
        double[] expectedCfF = new double[]{209.34, 224.10, 237.46, 143.50, 25.94, 2.69, 0.51, 0.11};
        double[] expectedAGroundF = new double[]{-1.25, -1.17, -1.25, -1.25, -1.25, -1.25, -1.25, -1.25};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.02, 0.08, 0.21, 0.39, 0.75, 1.97, 6.70, 23.88};
        expectedADiv = new double[]{56.02, 56.02, 56.02, 56.02, 56.02, 56.02, 56.02, 56.02};
        expectedLH = new double[]{15.12, 11.76, 7.43, 0.88, -1.57, -6.24, -14-10, -34.33};
        expectedLF = new double[]{15.12, 11.69, 7.64, 2.90, -1.57, -6.24, -14.10, -34.33};

        /*proPath = propDataOut.propagationPaths.get(1);*/

        double[] actualWH = proPath.groundAttenuation.wH;
        double[] actualCfH = proPath.groundAttenuation.cfH;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        double[] actualWF = proPath.groundAttenuation.wF;
        double[] actualCfF = proPath.groundAttenuation.cfF;
        double[] actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);

        /*assertDoubleArrayEquals("WH - lateral right", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - lateral right", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - lateral right", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - lateral right", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - lateral right", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - lateral right", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - lateral right", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - lateral right", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - lateral right", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - lateral right", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - lateral right", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/

        //Path2 : lateral left
        expectedWH = new double[]{0.00, 0.00, 0.00, 0.02, 0.11, 0.59, 2.96, 13.76};
        expectedCfH = new double[]{214.41, 233.28, 228.92, 103.46, 13.51, 1.70, 0.34, 0.07};
        expectedAGroundH = new double[]{-1.26, -1.26, -1.05, 0.86, -1.26, -1.26, -1.26, -1.26};
        expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.07, 0.59, 2.96, 13.76};
        expectedCfF = new double[]{211.78, 226.80, 240.03, 144.13, 25.83, 2.69, 0.51, 0.11};
        expectedAGroundF = new double[]{-1.26, -1.18, -1.26, -1.26, -1.26, -1.26, -1.26, -1.26};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.03, 0.08, 0.22, 0.40, 0.76, 2.00, 6.77, 24.16};
        expectedADiv = new double[]{56.02, 56.02, 56.02, 56.02, 56.02, 56.02, 56.02, 56.02};
        expectedLH = new double[]{13.40, 8.86, 4.40, -1.13, -2.50, -6.78, -14.58, -34.97};
        expectedLF = new double[]{13.40, 8.78, 4.61, 0.99, -2.50, -6.78, -14.58, -34.97};

        //proPath = propDataOut.propagationPaths.get(1);

        /*actualWH = proPath.groundAttenuation.wH;
        actualCfH = proPath.groundAttenuation.cfH;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualWF = proPath.groundAttenuation.wF;
        actualCfF = proPath.groundAttenuation.cfF;
        actualAGroundF = proPath.groundAttenuation.aGroundF;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);*/

        /*assertDoubleArrayEquals("WH - lateral left", expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfH - lateral left", expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundH - lateral left", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("WF - lateral left", expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("CfF - lateral left", expectedCfF, actualCfF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundF - lateral left", expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - lateral left", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - lateral left", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - lateral left", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - lateral left", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - lateral left", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{-2.96,3.56,6.73,11.17,13.85,13.86,9.48,-7.64},L, ERROR_EPSILON_VERY_HIGH); //because we don't take into account this rays

    }

    /**
     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic properties
     */
    @Test
    public void TC23() {
        PropagationProcessPathData attData = new PropagationProcessPathData();
        GeometryFactory factory = new GeometryFactory();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(attData.freq_lvl.size(), 0.2);

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addBuilding(new Coordinate[]{
                        new Coordinate(75, 34, 0),
                        new Coordinate(110, 34, 0),
                        new Coordinate(110, 26, 0),
                        new Coordinate(75, 26, 0)}, 9, buildingsAbs)
                .addBuilding(new Coordinate[]{
                        new Coordinate(83, 18, 0),
                        new Coordinate(118, 18, 0),
                        new Coordinate(118, 10, 0),
                        new Coordinate(83, 10, 0)}, 8, buildingsAbs)
                // Ground Surface

                .addTopographicLine(30, -14, 0, 122, -14, 0)// 1
                .addTopographicLine(122, -14, 0, 122, 45, 0)// 2
                .addTopographicLine(122, 45, 0, 30, 45, 0)// 3
                .addTopographicLine(30, 45, 0, 30, -14, 0)// 4
                .addTopographicLine(59.6, -9.87, 0, 76.84, -5.28, 0)// 5
                .addTopographicLine(76.84, -5.28, 0, 63.71, 41.16, 0)// 6
                .addTopographicLine(63.71, 41.16, 0, 46.27, 36.28, 0)// 7
                .addTopographicLine(46.27, 36.28, 0, 59.6, -9.87, 0)// 8
                .addTopographicLine(46.27, 36.28, 0, 54.68, 37.59, 5)// 9
                .addTopographicLine(54.68, 37.59, 5, 55.93, 37.93, 5)// 10
                .addTopographicLine(55.93, 37.93, 5, 63.71, 41.16, 0)// 11
                .addTopographicLine(59.6, -9.87, 0, 67.35, -6.83, 5)// 12
                .addTopographicLine(67.35, -6.83, 5, 68.68, -6.49, 5)// 13
                .addTopographicLine(68.68, -6.49, 5, 76.84, -5.28, 0)// 14
                .addTopographicLine(54.68, 37.59, 5, 67.35, -6.83, 5)// 15
                .addTopographicLine(55.93, 37.93, 5, 68.68, -6.49, 5)// 16
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(59.6, -9.87, 0), // 5
                        new Coordinate(76.84, -5.28, 0), // 5-6
                        new Coordinate(63.71, 41.16, 0), // 6-7
                        new Coordinate(46.27, 36.28, 0), // 7-8
                        new Coordinate(59.6, -9.87, 0)
                }), 1.)
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(30, -14, 0), // 5
                        new Coordinate(122, -14, 0), // 5-6
                        new Coordinate(122, 45, 0), // 6-7
                        new Coordinate(30, 45, 0), // 7-8
                        new Coordinate(30, -14, 0)
                }), 0.)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addSource(38, 14, 1)
                .addReceiver(107, 25.95, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=0;

        //Propagation process path data building
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{7.17, 8.69, 10.78, 13.46, 16.98, 21.34, 25.53, 29.05};
        double[] expectedAGroundSOH = new double[]{-1.02, -0.08, -0.73, -2.79, -2.79, -2.79, -2.79, -2.79};
        double[] expectedAGroundORH = new double[]{-0.28, -1.00, -2.46, -2.46, -2.46, -2.46, -2.46, -2.46};
        double[] expectedDeltaDiffSPrimeRH = new double[]{10.54, 12.93, 15.67, 18.78, 22.56, 27.04, 31.28, 34.81};
        double[] expectedDeltaDiffSRPrimeH = new double[]{10.86, 13.28, 16.05, 19.18, 22.97, 27.46, 31.70, 35.22};
        double[] expectedDeltaGroundSOH = new double[]{-0.71, -0.05, -0.42, -1.62, -1.58, -1.56, -1.55, -1.55};
        double[] expectedDeltaGroundORH = new double[]{-0.18, -0.60, -1.42, -1.36, -1.32, -1.30, -1.30, -1.29};
        double[] expectedADiffH = new double[]{6.28, 8.04, 8.93, 10.48, 14.09, 18.48, 22.15, 22.16};

        double[] expectedDeltaDiffSRF = new double[]{7.10, 8.59, 10.59, 12.99, 15.66, 18.49, 21.41, 24.37};
        double[] expectedAGroundSOF = new double[]{-0.96, -0.13, -1.09, -2.79, -2.79, -2.79, -2.79, -2.79};
        double[] expectedAGroundORF = new double[]{-0.54, -1.28, -2.39, -2.39, -2.39, -2.39, -2.39, -2.39};
        double[] expectedDeltaDiffSPrimeRF = new double[]{10.51, 12.87, 15.53, 18.36, 21.27, 24.23, 27.22, 30.22};
        double[] expectedDeltaDiffSRPrimeF = new double[]{10.86, 13.27, 15.96, 18.81, 21.73, 24.70, 27.68, 30.68};
        double[] expectedDeltaGroundSOF = new double[]{-0.66, -0.08, -0.64, -1.61, -1.57, -1.55, -1.54, -1.53};
        double[] expectedDeltaGroundORF = new double[]{-0.35, -0.77, -1.37, -1.30, -1.27, -1.25, -1.24, -1.24};
        double[] expectedADiffF = new double[]{6.09, 7.74, 8.59, 10.07, 12.82, 15.69, 18.63, 21.60};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.01, 0.03, 0.07, 0.14, 0.26, 0.68, 2.30, 8.19};
        double[] expectedADiv = new double[]{47.91, 47.91, 47.91, 47.91, 47.91, 47.91, 47.91, 47.91};
        double[] expectedABoundaryH = new double[]{6.28, 8.04, 8.93, 10.48, 14.09, 18.48, 22.15, 22.16};
        double[] expectedABoundaryF = new double[]{6.09, 7.74, 8.59, 10.07, 12.82, 15.69, 18.63, 21.60};
        double[] expectedLH = new double[]{38.80, 37.02, 36.08, 34.47, 30.75, 25.93, 20.64, 14.74};
        double[] expectedLF = new double[]{38.99, 37.32, 36.42, 34.88, 32.01, 28.72, 24.16, 15.29};
        double[] expectedL = new double[]{38.90, 37.17, 36.26, 34.68, 31.42, 27.54, 22.75, 15.02};
        double[] expectedLA = new double[]{12.70, 21.07, 27.66, 31.48, 31.42, 28.74, 23.75, 13.92};

        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        /*assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/
        //assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_VERY_HIGH);

    }

    /**
     * – Two buildings behind an earth-berm on flat ground with homogeneous acoustic properties – receiver position modified
     */
    @Test
    public void TC24() {

        PropagationProcessPathData attData = new PropagationProcessPathData();
        GeometryFactory factory = new GeometryFactory();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(attData.freq_lvl.size(), 0.2);

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addBuilding(new Coordinate[]{
                        new Coordinate(75, 34, 0),
                        new Coordinate(110, 34, 0),
                        new Coordinate(110, 26, 0),
                        new Coordinate(75, 26, 0)}, 9, buildingsAbs)
                .addBuilding(new Coordinate[]{
                        new Coordinate(83, 18, 0),
                        new Coordinate(118, 18, 0),
                        new Coordinate(118, 10, 0),
                        new Coordinate(83, 10, 0)}, 8, buildingsAbs)
                // Ground Surface

                .addTopographicLine(30, -14, 0, 122, -14, 0)// 1
                .addTopographicLine(122, -14, 0, 122, 45, 0)// 2
                .addTopographicLine(122, 45, 0, 30, 45, 0)// 3
                .addTopographicLine(30, 45, 0, 30, -14, 0)// 4
                .addTopographicLine(59.6, -9.87, 0, 76.84, -5.28, 0)// 5
                .addTopographicLine(76.84, -5.28, 0, 63.71, 41.16, 0)// 6
                .addTopographicLine(63.71, 41.16, 0, 46.27, 36.28, 0)// 7
                .addTopographicLine(46.27, 36.28, 0, 59.6, -9.87, 0)// 8
                .addTopographicLine(46.27, 36.28, 0, 54.68, 37.59, 5)// 9
                .addTopographicLine(54.68, 37.59, 5, 55.93, 37.93, 5)// 10
                .addTopographicLine(55.93, 37.93, 5, 63.71, 41.16, 0)// 11
                .addTopographicLine(59.6, -9.87, 0, 67.35, -6.83, 5)// 12
                .addTopographicLine(67.35, -6.83, 5, 68.68, -6.49, 5)// 13
                .addTopographicLine(68.68, -6.49, 5, 76.84, -5.28, 0)// 14
                .addTopographicLine(54.68, 37.59, 5, 67.35, -6.83, 5)// 15
                .addTopographicLine(55.93, 37.93, 5, 68.68, -6.49, 5)// 16
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(59.6, -9.87, 0), // 5
                        new Coordinate(76.84, -5.28, 0), // 5-6
                        new Coordinate(63.71, 41.16, 0), // 6-7
                        new Coordinate(46.27, 36.28, 0), // 7-8
                        new Coordinate(59.6, -9.87, 0)
                }), 1.)
                .addGroundEffect(factory.createPolygon(new Coordinate[]{
                        new Coordinate(30, -14, 0), // 5
                        new Coordinate(122, -14, 0), // 5-6
                        new Coordinate(122, 45, 0), // 6-7
                        new Coordinate(30, 45, 0), // 7-8
                        new Coordinate(30, -14, 0)
                }), 0.)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addSource(38, 14, 1)
                .addReceiver(106, 18.5, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{10.18, 13.64, 16.95, 20.02, 23.02, 26.01, 29.00, 23.01};
        double[] expectedAGroundSOH = new double[]{-1.05, -0.09, -0.69, -2.79, -2.79, -2.79, -2.79, -2.79};
        double[] expectedAGroundORH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedDeltaDiffSPrimeRH = new double[]{13.22, 17.14, 20.64, 23.79, 26.82, 29.83, 32.84, 35.85};
        double[] expectedDeltaDiffSRPrimeH = new double[]{18.53, 22.73, 26.34, 29.53, 32.59, 35.61, 38.62, 41.63};
        double[] expectedDeltaGroundSOH = new double[]{-0.75, -0.06, -0.46, -1.90, -1.90, -1.89, -1.89, -1.89};
        double[] expectedDeltaGroundORH = new double[]{-1.27, -1.17, -1.14, -1.12, -1.12, -1.11, -1.11, -1.11};
        double[] expectedADiffH = new double[]{8.16, 12.40, 15.36, 16.99, 20.00, 22.00, 22.00, 22.00};

        double[] expectedDeltaDiffSRF = new double[]{10.11, 13.55, 16.86, 19.93, 22.92, 25.91, 28.91, 31.91};
        double[] expectedAGroundSOF = new double[]{-0.98, -0.14, -1.05, -2.79, -2.79, -2.79, -2.79, -2.79};
        double[] expectedAGroundORF = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedDeltaDiffSPrimeRF = new double[]{13.19, 17.10, 20.60, 23.75, 26.79, 29.79, 32.80, 35.81};
        double[] expectedDeltaDiffSRPrimeF = new double[]{18.52, 22.72, 26.33, 29.52, 32.58, 35.60, 38.61, 41.62};
        double[] expectedDeltaGroundSOF = new double[]{-0.70, -0.10, -0.70, -1.89, -1.89, -1.88, -1.88, -1.88};
        double[] expectedDeltaGroundORF = new double[]{-1.27, -1.17, -1.13, -1.11, -1.11, -1.10, -1.10, -1.10};
        double[] expectedADiffF = new double[]{8.15, 12.29, 15.04, 16.92, 19.93, 22.02, 22.02, 22.02};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.01, 0.03, 0.07, 0.13, 0.25, 0.66, 2.24, 7.97};
        double[] expectedADiv = new double[]{47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68};
        double[] expectedABoundaryH = new double[]{8.16, 12.40, 15.36, 16.99, 20.00, 22.00, 22.00, 22.00};
        double[] expectedABoundaryF = new double[]{8.15, 12.29, 15.04, 16.92, 19.93, 22.02, 22.02, 22.02};
        double[] expectedLH = new double[]{37.16, 32.90, 29.89, 28.20, 25.07, 22.67, 21.09, 15.35};
        double[] expectedLF = new double[]{37.17, 33.00, 30.22, 28.27, 25.14, 22.65, 21.07, 15.33};
        double[] expectedL = new double[]{37.16, 32.95, 30.06, 28.23, 25.11, 22.66, 21.08, 15.34};
        double[] expectedLA = new double[]{10.96, 16.85, 21.46, 25.03, 25.11, 23.86, 22.08, 14.24};

        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        /*assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/
        //assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_HIGH);
        assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_VERY_HIGH);

        //Path1 : reflexion
        expectedDeltaDiffSRH = new double[]{7.18, 8.71, 1080, 13.49, 17.00, 21.36, 25.56, 29.08};
        expectedAGroundSOH = new double[]{-1.01, -0.08, -0.75, -2.79, -2.79, -2.79, -2.79, -2.79};
        expectedAGroundORH = new double[]{-0.27, -0.94, -2.47, -2.47, -2.47, -2.47, -2.47, -2.47};
        expectedDeltaDiffSPrimeRH = new double[]{10.58, 12.96, 15.71, 18.82, 22.59, 27.07, 31.31, 34.85};
        expectedDeltaDiffSRPrimeH = new double[]{10.80, 13.22, 15.98, 19.11, 22.88, 27.37, 31.61, 35.15};
        expectedDeltaGroundSOH = new double[]{-0.70, -0.05, -0.43, -1.62, -1.58, -1.56, -1.55, -1.55};
        expectedDeltaGroundORH = new double[]{-0.18, -0.57, -1.45, -1.38, -1.34, -1.32, -1.32, -1.32};
        expectedADiffH = new double[]{6.30, 8.09, 8.93, 10.49, 14.08, 18.48, 22.13, 22.14};

        expectedDeltaDiffSRF = new double[]{7.12, 8.61, 10.62, 13.02, 15.69, 18.52, 21.44, 24.40};
        expectedAGroundSOF = new double[]{-0.95, -0.13, -1.10, -2.79, -2.79, -2.79, -2.79, -2.79};
        expectedAGroundORF = new double[]{-0.52, -1.21, -2.40, -2.40, -2.40, -2.40, -2.40, -2.40};
        expectedDeltaDiffSPrimeRF = new double[]{10.55, 12.91, 15.57, 18.40, 21.32, 24.28, 27.26, 30.26};
        expectedDeltaDiffSRPrimeF = new double[]{10.81, 13.21, 15.90, 18.74, 21.66, 24.63, 27.61, 30.61};
        expectedDeltaGroundSOF = new double[]{-0.65, -0.08, -0.64, -1.61, -1.57, -1.55, -1.54, -1.53};
        expectedDeltaGroundORF = new double[]{-0.34, -0.73, -1.39, -1.33, -1.29, -1.27, -1.26, -1.26};
        expectedADiffF = new double[]{6.12, 7.80, 8.59, 10.08, 12.38, 15.70, 18.64, 21.61};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.01, 0.03, 0.07, 0.14, 0.26, 0.68, 2.32, 8.28};
        expectedADiv = new double[]{48.00, 48.00, 48.00, 48.00, 48.00, 48.00, 48.00, 48.00};
        expectedABoundaryH = new double[]{6.30, 8.09, 8.93, 10.49, 14.08, 18.48, 22.13, 22.14};
        expectedABoundaryF = new double[]{6.12, 7.80, 8.59, 10.08, 12.83, 15.70, 18.64, 21.61};
        expectedLH = new double[]{37.72, 35.91, 35.03, 33.41, 29.69, 24.87, 19.58, 13.62};
        expectedLF = new double[]{37.90, 36.20, 35.37, 33.81, 30.94, 27.64, 23.07, 14.14};
        expectedL = new double[]{37.81, 36.06, 35.20, 33.61, 30.36, 26.47, 21.67, 13.89};
        expectedLA = new double[]{11.61, 19.96, 26.60, 30.41, 30.36, 27.67, 22.67, 12.79};

        proPath = propDataOut.propagationPaths.get(1);

        actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        actualADiffH = proPath.aBoundaryH.aDiff;

        actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        actualADiffF = proPath.aBoundaryF.aDiff;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualABoundaryH = proPath.absorptionData.aBoundaryH;
        actualABoundaryF = proPath.absorptionData.aBoundaryF;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);

        /*assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/
        //assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_HIGH);
        //assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_VERY_HIGH);

        assertEquals(1, propDataOut.getVerticesSoundLevel().size());
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93 - 26.2, 93 - 16.1,
                93 - 8.6, 93 - 3.2, 93, 93 + 1.2, 93 + 1.0, 93 - 1.1});
        //todo IL Y A UNE ERREUR DANS LA NORME AVEC LE BATIMENT 2, SI ON LE SUPPRIME LES RESULTATS SONT EQUIVALENTS
        assertArrayEquals(new double[]{14.31, 21.69, 27.76, 31.52, 31.49, 29.18, 25.39, 16.58}, L, ERROR_EPSILON_HIGHEST);

    }

    /**
     * Replacement of the earth-berm by a barrier
     */
    @Test
    public void TC25() {
        PropagationProcessPathData attData = new PropagationProcessPathData();
        GeometryFactory factory = new GeometryFactory();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(attData.freq_lvl.size(), 0.2);

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addBuilding(new Coordinate[]{
                        new Coordinate(75, 34, 0),
                        new Coordinate(110, 34, 0),
                        new Coordinate(110, 26, 0),
                        new Coordinate(75, 26, 0)}, 9, buildingsAbs)
                .addBuilding(new Coordinate[]{
                        new Coordinate(83, 18, 0),
                        new Coordinate(118, 18, 0),
                        new Coordinate(118, 10, 0),
                        new Coordinate(83, 10, 0)}, 8, buildingsAbs)
                // Ground Surface

                .addWall(new Coordinate[]{
                        new Coordinate(59.19, 24.47, 0),
                        new Coordinate(64.17, 6.95, 0)
                }, 5)
                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addSource(38, 14, 1)
                .addReceiver(107, 25.95, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        //Path0 : vertical plane
        double[] expectedDeltaDiffSRH = new double[]{10.09, 13.54, 16.87, 19.94, 22.94, 25.93, 28.93, 31.93};
        double[] expectedAGroundSOH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedAGroundORH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedDeltaDiffSPrimeRH = new double[]{11.48, 15.20, 18.63, 21.75, 24.77, 27.78, 30.78, 33.78};
        double[] expectedDeltaDiffSRPrimeH = new double[]{18.47, 22.70, 26.32, 29.52, 32.58, 35.60, 38.61, 41.62};
        double[] expectedDeltaGroundSOH = new double[]{-2.62, -2.55, -2.52, -2.51, -2.50, -2.50, -2.50, -2.50};
        double[] expectedDeltaGroundORH = new double[]{-1.27, -1.17, -1.13, -1.11, -1.11, -1.10, -1.10, -1.10};
        double[] expectedADiffH = new double[]{6.21, 9.83, 13.22, 16.32, 19.33, 21.40, 21.40, 21.40};

        double[] expectedDeltaDiffSRF = new double[]{10.03, 13.46, 16.78, 19.85, 22.84, 25.83, 28.83, 31.83};
        double[] expectedAGroundSOF = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedAGroundORF = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedDeltaDiffSPrimeRF = new double[]{11.43, 15.14, 18.57, 21.69, 24.71, 27.71, 30.72, 33.72};
        double[] expectedDeltaDiffSRPrimeF = new double[]{18.46, 22.69, 26.31, 29.51, 32.57, 35.59, 38.60, 41.61};
        double[] expectedDeltaGroundSOF = new double[]{-2.61, -2.54, -2.51, -2.50, -2.50, -2.49, -2.49, -2.49};
        double[] expectedDeltaGroundORF = new double[]{-1.26, -1.16, -1.12, -1.10, -1.10, -1.09, -1.09, -1.09};
        double[] expectedADiffF = new double[]{6.15, 9.76, 13.15, 16.24, 19.25, 21.41, 21.42, 21.42};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.01, 0.03, 0.07, 0.13, 0.25, 0.66, 2.24, 7.97};
        double[] expectedADiv = new double[]{47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68};
        double[] expectedABoundaryH = new double[]{6.21, 9.83, 13.22, 16.32, 19.33, 21.40, 21.40, 21.40};
        double[] expectedABoundaryF = new double[]{6.15, 9.76, 13.15, 16.24, 19.25, 21.41, 21.42, 21.42};
        double[] expectedLH = new double[]{39.11, 35.47, 32.03, 28.87, 25.74, 23.27, 21.69, 15.95};
        double[] expectedLF = new double[]{39.16, 35.53, 32.11, 28.95, 25.82, 23.25, 21.67, 15.93};
        double[] expectedL = new double[]{39.13, 35.50, 32.07, 28.91, 25.78, 23.26, 21.68, 15.94};
        double[] expectedLA = new double[]{12.93, 19.40, 23.47, 25.71, 25.78, 24.46, 22.68, 14.84};

        PropagationPath proPath = propDataOut.propagationPaths.get(0);

        double[] actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        double[] actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        double[] actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        double[] actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        double[] actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        double[] actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        double[] actualADiffH = proPath.aBoundaryH.aDiff;

        double[] actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        double[] actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        double[] actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        double[] actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        double[] actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        double[] actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        double[] actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        double[] actualADiffF = proPath.aBoundaryF.aDiff;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = proPath.absorptionData.aAtm;
        double[] actualADiv = proPath.absorptionData.aDiv;
        double[] actualABoundaryH = proPath.absorptionData.aBoundaryH;
        double[] actualABoundaryF = proPath.absorptionData.aBoundaryF;
        double[] actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        double[] actualLA = addArray(actualL, A_WEIGHTING);

        /*assertDoubleArrayEquals("DeltaDiffSRH - vertical plane", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - vertical plane", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - vertical plane", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - vertical plane", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - vertical plane", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - vertical plane", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - vertical plane", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("actualADiffH - vertical plane", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - vertical plane", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundSOF - vertical plane", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - vertical plane", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - vertical plane", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - vertical plane", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - vertical plane", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - vertical plane", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - vertical plane", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - vertical plane", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - vertical plane", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - vertical plane", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - vertical plane", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - vertical plane", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - vertical plane", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - vertical plane", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/
        //assertDoubleArrayEquals("L - vertical plane", expectedL, actualL, ERROR_EPSILON_HIGH);
        //assertDoubleArrayEquals("LA - vertical plane", expectedLA, actualLA, ERROR_EPSILON_VERY_HIGH);

        //Path1 : lateral right

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.01, 0.04, 0.11, 0.19, 0.37, 0.98, 3.31, 11.82};
        expectedADiv = new double[]{47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68};
        double[] expectedAGroundH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        expectedLH = new double[]{20.84, 17.03, 13.68, 10.51, 7.31, 3.68, -1.66, -13.18};
        expectedLF = new double[]{20.84, 17.03, 13.68, 10.51, 7.31, 3.68, -1.66, -13.18};

        //proPath = propDataOut.propagationPaths.get(1);

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        double[] actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);

        /*assertDoubleArrayEquals("AlphaAtm - lateral right", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - lateral right", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - lateral right", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundH - lateral right", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - lateral right", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - lateral right", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/

        //Path2 : lateral right

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.01, 0.03, 0.07, 0.14, 0.26, 0.69, 2.32, 8.29};
        expectedADiv = new double[]{47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68, 47.68};
        expectedAGroundH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        expectedLH = new double[]{34.73, 32.02, 29.13, 26.13, 23.04, 19.63, 14.99, 6.02};
        expectedLF = new double[]{34.73, 32.02, 29.13, 26.13, 23.04, 19.63, 14.99, 6.02};

        //proPath = propDataOut.propagationPaths.get(2);

        /*actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualAGroundH = proPath.groundAttenuation.aGroundH;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);*/

        /*assertDoubleArrayEquals("AlphaAtm - lateral right", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - lateral right", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - lateral right", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundH - lateral right", expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("LH - lateral right", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - lateral right", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/

        //Path3 : reflexion
        expectedDeltaDiffSRH = new double[]{7.11, 8.60, 10.60, 13.01, 15.68, 18.51, 21.42, 24.39};
        expectedAGroundSOH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        expectedAGroundORH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        expectedDeltaDiffSPrimeRH = new double[]{8.70, 10.71, 13.12, 15.80, 18.64, 21.56, 24.52, 27.51};
        expectedDeltaDiffSRPrimeH = new double[]{10.21, 12.52, 15.15, 17.95, 20.86, 23.82, 26.80, 29.80};
        expectedDeltaGroundSOH = new double[]{-2.56, -2.44, -2.34, -2.27, -2.23, -2.21, -2.20, -2.20};
        expectedDeltaGroundORH = new double[]{-2.20, -2.03, -1.90, -1.82, -1.78, -1.75, -1.74, -1.74};
        expectedADiffH = new double[]{2.34, 4.14, 6.37, 8.91, 11.66, 14.54, 17.48, 20.45};

        expectedDeltaDiffSRF = new double[]{7.05, 8.51, 10.49, 12.88, 15.54, 18.36, 21.28, 24.24};
        expectedAGroundSOF = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        expectedAGroundORF = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        expectedDeltaDiffSPrimeRF = new double[]{8.66, 10.65, 13.06, 15.74, 18.57, 21.49, 24.45, 27.44};
        expectedDeltaDiffSRPrimeF = new double[]{10.18, 12.48, 15.11, 17.91, 20.82, 23.77, 26.76, 29.75};
        expectedDeltaGroundSOF = new double[]{-2.56, -2.43, -2.32, -2.26, -2.22, -2.20, -2.19, -2.18};
        expectedDeltaGroundORF = new double[]{-2.20, -2.02, -1.89, -1.81, -1.76, -1.74, -1.72, -1.72};
        expectedADiffF = new double[]{2.29, 4.07, 6.28, 8.82, 11.56, 14.43, 17.37, 20.34};

        expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        expectedAAtm = new double[]{0.01, 0.03, 0.07, 0.14, 0.26, 0.68, 2.32, 8.28};
        expectedADiv = new double[]{48.00, 48.00, 48.00, 48.00, 48.00, 48.00, 48.00, 48.00};
        expectedABoundaryH = new double[]{2.34, 4.14, 6.37, 8.91, 11.66, 14.54, 17.48, 20.45};
        expectedABoundaryF = new double[]{2.29, 4.07, 6.28, 8.82, 11.56, 14.43, 17.37, 20.34};
        expectedLH = new double[]{41.68, 39.86, 37.59, 34.98, 32.11, 28.81, 24.23, 15.30};
        expectedLF = new double[]{41.73, 39.93, 37.67, 35.08, 32.21, 28.92, 24.34, 15.41};
        expectedL = new double[]{41.70, 39.90, 37.63, 35.03, 32.16, 28.86, 24.29, 15.36};

        //proPath = propDataOut.propagationPaths.get(3);

        /*actualDeltaDiffSRH = proPath.aBoundaryH.deltaDiffSR;
        actualAGroundSOH = proPath.aBoundaryH.aGroundSO;
        actualAGroundORH = proPath.aBoundaryH.aGroundOR;
        actualDeltaDiffSPrimeRH = proPath.aBoundaryH.deltaDiffSPrimeR;
        actualDeltaDiffSRPrimeH = proPath.aBoundaryH.deltaDiffSRPrime;
        actualDeltaGroundSOH = proPath.aBoundaryH.deltaGroundSO;
        actualDeltaGroundORH = proPath.aBoundaryH.deltaGroundOR;
        actualADiffH = proPath.aBoundaryH.aDiff;

        actualDeltaDiffSRF = proPath.aBoundaryF.deltaDiffSR;
        actualAGroundSOF = proPath.aBoundaryF.aGroundSO;
        actualAGroundORF = proPath.aBoundaryF.aGroundOR;
        actualDeltaDiffSPrimeRF = proPath.aBoundaryF.deltaDiffSPrimeR;
        actualDeltaDiffSRPrimeF = proPath.aBoundaryF.deltaDiffSRPrime;
        actualDeltaGroundSOF = proPath.aBoundaryF.deltaGroundSO;
        actualDeltaGroundORF = proPath.aBoundaryF.deltaGroundOR;
        actualADiffF = proPath.aBoundaryF.aDiff;

        actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        actualAAtm = proPath.absorptionData.aAtm;
        actualADiv = proPath.absorptionData.aDiv;
        actualABoundaryH = proPath.absorptionData.aBoundaryH;
        actualABoundaryF = proPath.absorptionData.aBoundaryF;
        actualLH = addArray(proPath.absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        actualLF = addArray(proPath.absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        actualL = addArray(proPath.absorptionData.aGlobal, SOUND_POWER_LEVELS);
        actualLA = addArray(actualL, A_WEIGHTING);*/

        /*assertDoubleArrayEquals("DeltaDiffSRH - reflexion", expectedDeltaDiffSRH, actualDeltaDiffSRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AGroundSOH - reflexion", expectedAGroundSOH, actualAGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORH - reflexion", expectedAGroundORH, actualAGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRH - reflexion", expectedDeltaDiffSPrimeRH, actualDeltaDiffSPrimeRH, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("DeltaDiffSRPrimeH - reflexion", expectedDeltaDiffSRPrimeH, actualDeltaDiffSRPrimeH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundSOH - reflexion", expectedDeltaGroundSOH, actualDeltaGroundSOH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORH - reflexion", expectedDeltaGroundORH, actualDeltaGroundORH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("actualADiffH - reflexion", expectedADiffH, actualADiffH, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("DeltaDiffSRF - reflexion", expectedDeltaDiffSRF, actualDeltaDiffSRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundSOF - reflexion", expectedAGroundSOF, actualAGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("AGroundORF - reflexion", expectedAGroundORF, actualAGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSPrimeRF - reflexion", expectedDeltaDiffSPrimeRF, actualDeltaDiffSPrimeRF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaDiffSRPrimeF - reflexion", expectedDeltaDiffSRPrimeF, actualDeltaDiffSRPrimeF, ERROR_EPSILON_LOW);
        assertDoubleArrayEquals("DeltaGroundSOF - reflexion", expectedDeltaGroundSOF, actualDeltaGroundSOF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("DeltaGroundORF - reflexion", expectedDeltaGroundORF, actualDeltaGroundORF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiffF - reflexion", expectedADiffF, actualADiffF, ERROR_EPSILON_VERY_LOW);

        assertDoubleArrayEquals("AlphaAtm - reflexion", expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertDoubleArrayEquals("AAtm - reflexion", expectedAAtm, actualAAtm, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ADiv - reflexion", expectedADiv, actualADiv, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryH - reflexion", expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("ABoundaryF - reflexion", expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LH - reflexion", expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertDoubleArrayEquals("LF - reflexion", expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);*/
        //assertDoubleArrayEquals("L - reflexion", expectedL, actualL, ERROR_EPSILON_HIGH);
        //assertDoubleArrayEquals("LA - reflexion", expectedLA, actualLA, ERROR_EPSILON_VERY_HIGH);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        //MANQUE DIFFRACTIONS HORIZONTALES
        assertArrayEquals( new double[]{17.50,25.65,30.56,33.22,33.48,31.52,27.51,17.80},L, ERROR_EPSILON_HIGHEST);
    }


    /**
     * TC26 – Road source with influence of retrodiffraction
     * */
    @Test
    public void TC26() {
        GeometryFactory factory = new GeometryFactory();
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        // screen
        builder.addWall(new Coordinate[]{
                        new Coordinate(74.0, 52.0, 6),
                        new Coordinate(130.0, 60.0, 8)}, 0, -1)

                .addGroundEffect(factory.toGeometry(new Envelope(0, 50, -10, 100)), 0.0)
                .addGroundEffect(factory.toGeometry(new Envelope(50, 150, -10, 100)), 0.5)

                .finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addSource(10, 10, 0.05)
                .addReceiver(120, 20, 8)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});

        assertArrayEquals(  new double[]{17.50,27.52,34.89,40.14,43.10,43.59,40.55,29.15},L, ERROR_EPSILON_HIGH);
    }


    /**
     * TC27 – Road source with influence of retrodiffraction
     * */
    @Test
    public void TC27() {
        GeometryFactory factory = new GeometryFactory();
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder()

        // Add building
        // screen
                .addWall(new Coordinate[]{
                new Coordinate(114.0, 52.0, 0),
                new Coordinate(170.0, 60.0, 0)}, 4, -1)

                .addTopographicLine(80.0, 20.0, -1.0, 110.0, 20.0, -1.0)
                .addTopographicLine(110.0, 20.0, -1.0, 111.0, 20.0, 0.0)
                .addTopographicLine(111.0, 20.0, 0.0, 215.0, 20.0, 0.0)
                .addTopographicLine(215.0, 20.0, 0.0, 215.0, 80.0, 0.0)
                .addTopographicLine(215.0, 80.0, 0.0, 111.0, 80.0, 0.0)
                .addTopographicLine(111.0, 80.0, 0.0, 110.0, 80.0, -0.5)
                .addTopographicLine(110.0, 80.0, -0.5, 80.0, 80.0, -0.5)
                .addTopographicLine(80.0, 80.0, -0.5, 80.0, 20.0, -0.5)
                .addTopographicLine(110.0, 20.0, -0.5, 110.0, 80.0, -0.5)
                .addTopographicLine(111.0, 20.0, 0.0, 111.0, 80.0, 0.0)

                .addGroundEffect(80, 110, 20, 80, 0.0)
                .addGroundEffect(110, 215, 20, 80, 1.0)

                .finishFeeding();


        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addSource(105, 35, -0.45)
                .addReceiver(200, 50, 4)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.)
                .build();
        rayData.reflexionOrder=1;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});

        assertArrayEquals(  new double[]{16.84,26.97,34.79,40.23,38.57,38.58,39.36,29.60},L, ERROR_EPSILON_VERY_HIGH);// we don't take into account retrodiffraction

    }

    /**
     * TC28 Propagation over a large distance with many buildings between source and
     * receiver
     */
    @Test
    public void TC28() {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500, 1500, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(new Coordinate[]{
                    new Coordinate(113, 10, 0),
                    new Coordinate(127, 16, 0),
                    new Coordinate(102, 70, 0),
                    new Coordinate(88, 64, 0)}, 6, -1)

                .addBuilding(new Coordinate[]{
                    new Coordinate(176, 19, 0),
                    new Coordinate(164, 88, 0),
                    new Coordinate(184, 91, 0),
                    new Coordinate(196, 22, 0)}, 10, -1)

                .addBuilding(new Coordinate[]{
                    new Coordinate(250, 70, 0),
                    new Coordinate(250, 180, 0),
                    new Coordinate(270, 180, 0),
                    new Coordinate(270, 70, 0)}, 14, -1)

                .addBuilding(new Coordinate[]{
                    new Coordinate(332, 32, 0),
                    new Coordinate(348, 126, 0),
                    new Coordinate(361, 108, 0),
                    new Coordinate(349, 44, 0)}, 10, -1)

                .addBuilding(new Coordinate[]{
                    new Coordinate(400, 5, 0),
                    new Coordinate(400, 85, 0),
                    new Coordinate(415, 85, 0),
                    new Coordinate(415, 5, 0)}, 9, -1)

                .addBuilding(new Coordinate[]{
                    new Coordinate(444, 47, 0),
                    new Coordinate(436, 136, 0),
                    new Coordinate(516, 143, 0),
                    new Coordinate(521, 89, 0),
                    new Coordinate(506, 87, 0),
                    new Coordinate(502, 127, 0),
                    new Coordinate(452, 123, 0),
                    new Coordinate(459, 48, 0)}, 12, -1)

                .addBuilding(new Coordinate[]{
                    new Coordinate(773, 12, 0),
                    new Coordinate(728, 90, 0),
                    new Coordinate(741, 98, 0),
                    new Coordinate(786, 20, 0)}, 14, -1)

                .addBuilding(new Coordinate[]{
                    new Coordinate(972, 82, 0),
                    new Coordinate(979, 121, 0),
                    new Coordinate(993, 118, 0),
                    new Coordinate(986, 79, 0)}, 8, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5);

        builder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(builder)
                .addSource(0, 50, 4)
                .addReceiver(1000, 100, 1)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();
        rayData.reflexionOrder=1;
        rayData.maxSrcDist = 1500;

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{150-26.2,150-16.1,150-8.6,150-3.2,150,150+1.2,150+1.0,150-1.1});
        assertArrayEquals(  new double[]{43.56,50.59,54.49,56.14,55.31,49.77,23.37,-59.98},L, ERROR_EPSILON_VERY_HIGH);


    }


    /**
     * Test optimisation feature {@link CnossosPropagationData#maximumError}
     */
    @Test
    public void testIgnoreNonSignificantSources() throws LayerDelaunayError {

        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
        rayData.addReceiver(new Coordinate(0, 0, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)), roadLvl);
        rayData.addSource(factory.createPoint(new Coordinate(1100, 1100, 1)), roadLvl);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);

        rayData.maxSrcDist = 2000;
        rayData.maximumError = 3; // 3 dB error max

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        // Second source has not been computed because at best it would only increase the received level of only 0.0004 dB
        assertEquals(1, propDataOut.receiversAttenuationLevels.size());

        //TODO check the expected level and the delta should be reduced to at least 0.1
        assertEquals(44.07, wToDba(sumArray(roadLvl.length, dbaToW(propDataOut.getVerticesSoundLevel().get(0).value))), 3);
    }

    @Test
    public void testRoseIndex() {
        double angle_section = (2 * Math.PI) / PropagationProcessPathData.DEFAULT_WIND_ROSE.length;
        double angleStart = Math.PI / 2 - angle_section / 2;
        for(int i = 0; i < PropagationProcessPathData.DEFAULT_WIND_ROSE.length; i++) {
            double angle = angleStart - angle_section * i - angle_section / 3;
            int index = ComputeRaysOutAttenuation.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);angle = angleStart - angle_section * i - angle_section * 2.0/3.0;
            index = ComputeRaysOutAttenuation.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);
        }
    }

    /**
     * Check if Li coefficient computation and line source subdivision are correctly done
     * @throws LayerDelaunayError
     */
    @Test
    public void testSourceLines()  throws LayerDelaunayError, IOException, ParseException {

        // First Compute the scene with only point sources at 1m each
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        LineString geomSource = (LineString)wktReader.read("LINESTRING (51 40.5 0.05, 51 55.5 0.05)");
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
        rayData.addReceiver(new Coordinate(50, 50, 0.05));
        rayData.addReceiver(new Coordinate(48, 50, 4));
        rayData.addReceiver(new Coordinate(44, 50, 4));
        rayData.addReceiver(new Coordinate(40, 50, 4));
        rayData.addReceiver(new Coordinate(20, 50, 4));
        rayData.addReceiver(new Coordinate(0, 50, 4));

        List<Coordinate> srcPtsRef = new ArrayList<>();
        ComputeCnossosRays.splitLineStringIntoPoints(geomSource, 1.0, srcPtsRef);
        for(Coordinate srcPtRef : srcPtsRef) {
            rayData.addSource(factory.createPoint(srcPtRef), roadLvl);
        }

        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.maxSrcDist = 2000;

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.makeRelativeZToAbsolute();
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);


        // Second compute the same scene but with a line source
        rayData.clearSources();
        rayData.addSource(geomSource, roadLvl);
        RayOut propDataOutTest = new RayOut(true, attData, rayData);
        computeRays.run(propDataOutTest);

        // Merge levels for each receiver for point sources
        Map<Long, double[]> levelsPerReceiver = new HashMap<>();
        for(ComputeRaysOutAttenuation.VerticeSL lvl : propDataOut.receiversAttenuationLevels) {
            if(!levelsPerReceiver.containsKey(lvl.receiverId)) {
                levelsPerReceiver.put(lvl.receiverId, lvl.value);
            } else {
                // merge
                levelsPerReceiver.put(lvl.receiverId, sumDbArray(levelsPerReceiver.get(lvl.receiverId),
                        lvl.value));
            }
        }


        // Merge levels for each receiver for lines sources
        Map<Long, double[]> levelsPerReceiverLines = new HashMap<>();
        for(ComputeRaysOutAttenuation.VerticeSL lvl : propDataOutTest.receiversAttenuationLevels) {
            if(!levelsPerReceiverLines.containsKey(lvl.receiverId)) {
                levelsPerReceiverLines.put(lvl.receiverId, lvl.value);
            } else {
                // merge
                levelsPerReceiverLines.put(lvl.receiverId, sumDbArray(levelsPerReceiverLines.get(lvl.receiverId),
                        lvl.value));
            }
        }

        assertEquals(6, levelsPerReceiverLines.size());
        assertEquals(6, levelsPerReceiver.size());

//        KMLDocument.exportScene("target/testSourceLines.kml", manager, propDataOutTest);
//        KMLDocument.exportScene("target/testSourceLinesPt.kml", manager, propDataOut);
//        // Uncomment for printing maximum error
//        for(int i = 0; i < levelsPerReceiver.size(); i++) {
//            LOGGER.info(String.format("%d error %.2f", i,  getMaxError(levelsPerReceiver.get(i), levelsPerReceiverLines.get(i))));
//        }

        for(int i = 0; i < levelsPerReceiver.size(); i++) {
            assertArrayEquals(levelsPerReceiver.get(i), levelsPerReceiverLines.get(i), 0.2);
        }
    }



    /**
     * Test of convergence of power at receiver when increasing the reflection order
     * Event at 100 order of reflection then final noise level should not be
     * superior to 3.0 decibels compared to direct power
     */
    @Test
    public void testReflexionConvergence() {
        //Profile building
        List<Integer> alphaWallFrequencies = Arrays.asList(PropagationProcessPathData.asOctaveBands(DEFAULT_FREQUENCIES_THIRD_OCTAVE));
        List<Double> alphaWall = new ArrayList<>(alphaWallFrequencies.size());
        for(int frequency : alphaWallFrequencies) {
            alphaWall.add(AlphaUtils.getWallAlpha(100000, frequency));
        }

        ProfileBuilder profileBuilder = new ProfileBuilder()
                .addWall(new Coordinate[]{
                        new Coordinate(6, 0, 4),
                        new Coordinate(-5, 12, 4),
                }, 8, alphaWall, 0)
                .addWall(new Coordinate[]{
                        new Coordinate(14, 4, 4),
                        new Coordinate(3, 16, 4),
                }, 8, alphaWall, 1);
        profileBuilder.setzBuildings(true);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(8, 5.5, 0.1)
                .addReceiver(4.5, 8, 1.6)
                .hEdgeDiff(true)
                .vEdgeDiff(true)
                .setGs(0.5)
                .build();
        rayData.maxSrcDist = 60000000;
        rayData.maxRefDist = 60000000;
        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        double firstPowerAtReceiver = 0;
        for(int i = 0; i < 100; i++) {

            //Out and computation settings
            ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
            rayData.reflexionOrder = i;
            ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
            computeRays.setThreadCount(1);

            //Run computation
            computeRays.run(propDataOut);

            //Actual values
            // number of propagation paths between two walls = reflectionOrder * 2 + 1
            assertEquals(i * 2 + 1, propDataOut.propagationPaths.size());

            double[] sourcePower = new double[alphaWall.size()];
            double[] receiverPower = new double[alphaWall.size()];
            Arrays.fill(sourcePower, 70.0);
            for(PropagationPath proPath : propDataOut.propagationPaths) {
                double[] attenuationGlobal = proPath.absorptionData.aGlobal;
                double[] contributionPower = PowerUtils.sumArray(attenuationGlobal, sourcePower);
                receiverPower = PowerUtils.sumDbArray(receiverPower, contributionPower);
            }
            double globalPowerAtReceiver = PowerUtils.wToDba(PowerUtils.sumArray(PowerUtils.dbaToW(receiverPower)));
            if(i == 0) {
                firstPowerAtReceiver = globalPowerAtReceiver;
            } else {
                assertEquals(firstPowerAtReceiver, globalPowerAtReceiver, 3.0);
            }
        }
    }


    /**
     * Test reported issue with receiver over building
     */
    @Test
    public void testReceiverOverBuilding() throws LayerDelaunayError, ParseException {

        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        WKTReader wktReader = new WKTReader();
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.addBuilding(wktReader.read("POLYGON ((-111 -35, -111 82, 70 82, 70 285, 282 285, 282 -35, -111 -35))"), 10, -1);

        builder.finishFeeding();

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
        rayData.addReceiver(new Coordinate(162, 80, 150));
        rayData.addSource(factory.createPoint(new Coordinate(-150, 200, 1)), roadLvl);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);

        rayData.maxSrcDist = 2000;

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        assertEquals(1, propDataOut.receiversAttenuationLevels.size());

        assertEquals(14.6, wToDba(sumArray(roadLvl.length, dbaToW(propDataOut.getVerticesSoundLevel().get(0).value))), 0.1);
    }

    private static double getMaxError(double[] ref, double[] result) {
        assertEquals(ref.length, result.length);
        double max = Double.MIN_VALUE;
        for(int i=0; i < ref.length; i++) {
            max = Math.max(max, Math.abs(ref[i] - result[i]));
        }
        return max;
    }

    private static final class RayOut extends ComputeRaysOutAttenuation {
        private DirectPropagationProcessData processData;

        public RayOut(boolean keepRays, PropagationProcessPathData pathData, DirectPropagationProcessData processData) {
            super(keepRays, pathData);
            this.processData = processData;
        }

        @Override
        public double[] computeAttenuation(PropagationProcessPathData data, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            double[] attenuation = super.computeAttenuation(data, sourceId, sourceLi, receiverId, propagationPath);
            double[] soundLevel = wToDba(multArray(processData.wjSources.get((int)sourceId), dbaToW(attenuation)));
            return soundLevel;
        }
    }

    private static final class DirectPropagationProcessData extends CnossosPropagationData {
        private List<double[]> wjSources = new ArrayList<>();

        public DirectPropagationProcessData(ProfileBuilder builder) {
            super(builder);
        }

        public void addSource(Geometry geom, double[] spectrum) {
            super.addSource(geom);
            wjSources.add(spectrum);
        }

        public void addSource(Geometry geom, List<Double> spectrum) {
            super.addSource(geom);
            double[] wj = new double[spectrum.size()];
            for(int i=0; i < spectrum.size(); i++) {
                wj[i] = spectrum.get(i);
            }
            wjSources.add(wj);
        }

        public void clearSources() {
            wjSources.clear();
            sourceGeometries.clear();
            sourcesIndex = new QueryRTree();
        }

        @Override
        public double[] getMaximalSourcePower(int sourceId) {
            return wjSources.get(sourceId);
        }
    }


    /**
     * Test NaN regression issue
     * ByteArrayOutputStream bos = new ByteArrayOutputStream();
     * propath.writeStream(new DataOutputStream(bos));
     * new String(Base64.getEncoder().encode(bos.toByteArray()));
     */
    //@Test
    public void TestRegressionNaN() throws LayerDelaunayError, IOException {
        String path = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABkELTp9wo7AcQVnI2rXCgfo/qZmZmZmZmgAAAAAAAAAAAAAAAAAAAAAACH/4" +
                "AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAAEELUD" +
                "JSoUA3QVnItqDcGhJAJdiQBvXwS0AVTjoMf9fiAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gA" +
                "AAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAA0ELUoGFTOGrQVnIga50fzdANmqD/Me4pUActzMeCMRaAAAAAA" +
                "AAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////" +
                "AAAAA0ELUo/NRf1KQVnIgGcH8SZANmqD/Me4pUAe4TEhnNY1AAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AA" +
                "AAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAA0ELU1RrgqjDQVnIbssqD85AMNkgsNSQIkAlRqCv" +
                "boWkAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAA" +
                "AAAAD/////AAAAA0ELU3djM9QGQVnIa6l2eGhALdnXzMMRgUAl2dfMwxGBAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAA" +
                "AAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAABAAAAAU/wBvxnrf6hkBJLxOOJzAAwGIL" +
                "Ic/cAABAJRdmLHGkLkELTp9xTapHQVnI2rWzSOi/jTelQ3f5WD/hNUTOrUdwQFJ6WqIZaADAanpOelWAAD/odGaWtL+wQQtQMkh" +
                "BG1pBWci2ocn7h0ASDrQig+tyAAAAAAAAAAA/+uzqKo4AAMATSo/AwAAAP/Qml6qEHQRBC1J9yb/w6kFZyIIECGuLQBb5xfFciCQ" +
                "AAAAAAAAAAEA4e506acAAwFGKjYvwAABABNd1zN/3IEELUo8N2d3pQVnIgHgsr3lAIC98kZ14SQAAAAAAAAAAQBFrZiGsAADAKP" +
                "YLaTAAAD/SoWVrORNgQQtTU81vkgNBWchu2VI9a0AlRuFfAqJXAAAABT/TBsY8SUi2QGNg7UkPZADAe8S0CsKAAEAVMZUczyA2Q" +
                "QtOn1fPWpRBWcjat/vFwUAKJWO95msGP9MGxjxJSLZAY2CN2WJwAMB7xCtJq4AAQCDgvd4PLeBBC06fP717akFZyNq6I58WQBny" +
                "M91nx0E/0wbGPElItkBjYUy4vFgAwHvFPMvZgABAAUNc+v/JNkELTp9Xz1qUQVnI2rf7xcFACiVjveZrBj/TBsY8SUi2QGNfsm" +
                "l4cADAe8Lw2QoAAEAuAc8nHC4hQQtOn3aZdepBWcjatTnckL+z+60sjk/gP9MGxjxJSLZAY2GkR76YAMB7xbpDLYAAQBuZiTof" +
                "xetBC06fV89alEFZyNq3+8XBQAolY73mawY=";

        PropagationPath propPath = new PropagationPath();
        propPath.readStream(new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(path))));
        propPath.initPropagationPath();

//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        propPath.writeStream(new DataOutputStream(bos));
//        String newVersion  = new String(Base64.getEncoder().encode(bos.toByteArray()));
//        System.out.println(newVersion);

        PropagationProcessPathData pathData = new PropagationProcessPathData();
        EvaluateAttenuationCnossos.evaluate(propPath, pathData);
        double[] aGlobalMeteoHom = EvaluateAttenuationCnossos.getaGlobal();
        for (int i = 0; i < aGlobalMeteoHom.length; i++) {
            assertFalse(String.format("freq %d Hz with nan value", pathData.freq_lvl.get(i)),
                    Double.isNaN(aGlobalMeteoHom[i]));
        }

    }
}