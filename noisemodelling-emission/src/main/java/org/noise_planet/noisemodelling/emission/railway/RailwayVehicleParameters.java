/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.railway;

/**
 * Railway noise evaluation from Cnossos reference : COMMISSION DIRECTIVE (EU) 2015/996
 * of 19 May 2015 establishing common noise assessment methods according to Directive 2002/49/EC
 * of the European Parliament and of the Council
 *
 * amending, for the purposes of adapting to scientific and technical progress, Annex II to
 * Directive 2002/49/EC of the European Parliament and of the Council as regards
 * common noise assessment methods
 *
 * part 2.3. Railway noise
 *
 * @author Adrien Le Bellec, Université Gustave Eiffel
 * @author Olivier Chiello, Université Gustave Eiffel
 */

/**
 * Parameters Vehicule
 */

public class RailwayVehicleParameters {

    // set default value
    private String typeVehicle = ""; // name of the vehicles
    private double speedVehicle; // speed of the vehicles (km/h)
    private double vehiclePerHour = 1; // Average light vehicle per hour


    public RailwayVehicleParameters() {

        setTypeVehicle(typeVehicle);
        setSpeedVehicle(speedVehicle);
        setVehiclePerHour(vehiclePerHour);

    }


    public void setVehiclePerHour(double vehiclePerHour) {
        this.vehiclePerHour = vehiclePerHour;
    }

    public String getTypeVehicle() {
        return typeVehicle;
    }

    public void setTypeVehicle(String typeVehicle) {
        this.typeVehicle = typeVehicle;
    }

    public double getSpeedVehicle() {
        return speedVehicle;
    }

    public void setSpeedVehicle(double speedVehicle) {
        this.speedVehicle = speedVehicle;
    }

    public double getNumberVehicle() {
        return vehiclePerHour;
    }

}
