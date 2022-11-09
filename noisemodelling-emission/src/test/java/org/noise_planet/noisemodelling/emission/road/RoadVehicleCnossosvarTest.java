/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.road;

import org.junit.Test;
import org.noise_planet.noisemodelling.emission.road.cnossosvar.RoadVehicleCnossosvar;
import org.noise_planet.noisemodelling.emission.road.cnossosvar.RoadVehicleCnossosvarParameters;

import static org.junit.Assert.assertEquals;

/**
 * Test the Road model CNOSSOS as implemented in RoadVehicleCnossosVar.java
 * @author Pierre Aumond, Univ. Gustave eiffel
 * @author Arnaud Can, Univ. Gustave eiffel
 */

public class RoadVehicleCnossosvarTest {
    private static final double EPSILON_TEST1 = 0.1;

    @Test
    public void testRoadNoise1() {
        double speed = 50;
        int acc = 1;
        int FreqParam = 500;
        double Temperature = 0;
        String RoadSurface = "DEF";
        boolean Stud = true;
        double Junc_dist = 200;
        int Junc_type = 1;
        String veh_type = "1";
        int acc_type = 1;
        double LwStd = 1;
        int VehId = 10;

        RoadVehicleCnossosvarParameters rsParameters = new RoadVehicleCnossosvarParameters(speed, acc, veh_type, acc_type, Stud, LwStd, VehId);
        rsParameters.setSlopePercentage(0);
        rsParameters.setFileVersion(2);
        rsParameters.setFrequency(FreqParam);
        rsParameters.setTemperature(Temperature);
        rsParameters.setRoadSurface(RoadSurface);
        rsParameters.setJunc_dist(Junc_dist);
        rsParameters.setJunc_type(Junc_type);
        assertEquals(91.66, RoadVehicleCnossosvar.evaluate(rsParameters), EPSILON_TEST1);
    }

    @Test
    public void testRoadNoise2_speed0() {
        double speed = 0;
        int acc = 1;
        int FreqParam = 500;
        double Temperature = 0;
        String RoadSurface = "DEF";
        boolean Stud = false;
        double Junc_dist = 200;
        int Junc_type = 1;
        String veh_type = "3";
        int acc_type = 1;
        double LwStd = 1;
        int VehId = 10;

        RoadVehicleCnossosvarParameters rsParameters = new RoadVehicleCnossosvarParameters(speed, acc, veh_type, acc_type, Stud, LwStd, VehId);
        rsParameters.setFrequency(FreqParam);
        rsParameters.setTemperature(Temperature);
        rsParameters.setRoadSurface(RoadSurface);
        rsParameters.setJunc_dist(Junc_dist);
        rsParameters.setJunc_type(Junc_type);
        rsParameters.setSlopePercentage(0);
        assertEquals(100.08, RoadVehicleCnossosvar.evaluate(rsParameters), EPSILON_TEST1);
    }


    @Test
    public void testRoadNoise3_speed60() {
        int FreqParam = 8000;
        double speed = 60;
        int acc = 0;

        double Temperature = 15;
        String RoadSurface = "NL08";
        boolean Stud = false;
        double Junc_dist = 200;
        int Junc_type = 1;
        String veh_type = "1";
        int acc_type = 1;
        double LwStd = 0;
        int VehId = 1;
        RoadVehicleCnossosvarParameters rsParameters = new RoadVehicleCnossosvarParameters(speed, acc, veh_type, acc_type, Stud, LwStd, VehId);
        rsParameters.setSlopePercentage(0);
        rsParameters.setFrequency(FreqParam);
        rsParameters.setTemperature(Temperature);
        rsParameters.setRoadSurface(RoadSurface);
        rsParameters.setJunc_dist(Junc_dist);
        rsParameters.setJunc_type(Junc_type);
        assertEquals(78.62, RoadVehicleCnossosvar.evaluate(rsParameters), EPSILON_TEST1);
    }
}