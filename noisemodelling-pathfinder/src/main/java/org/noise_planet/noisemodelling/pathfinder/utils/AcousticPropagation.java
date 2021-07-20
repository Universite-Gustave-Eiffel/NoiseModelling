package org.noise_planet.noisemodelling.pathfinder.utils;

import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.wToDba;

public class AcousticPropagation {

    public static double getADiv(double distance) {
        return  wToDba(4 * Math.PI * Math.max(1, distance * distance));
    }


}
