/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;


import org.locationtech.jts.geom.LineString;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;

/**
 * Output of the attenuation computation (data class).
 *
 * @author Martin Glesser
 */
public class AttenuationOutput {
    public CutProfile cutProfile;
    public String timePeriod=""; // time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
    public LineString lineString; // ray from src to rcv through potential reflexion and diffraction points
    public MeteoType meteoType; // Type of meteo parameters used to obtain the attenuation

    /**
     * Final attenuation (dB)
     */
    public  double[] aGlobal = new double[0];

    public void init(int size) {
        this.aGlobal = new double[size];
    }

    public AttenuationOutput() {
    }

    public AttenuationOutput(CutProfile cutProfile) {
        this.cutProfile = cutProfile;
    }

    public AttenuationOutput(AttenuationOutput other) {
        this.cutProfile = other.cutProfile;
        this.aGlobal = other.aGlobal;
        this.timePeriod = other.timePeriod;
        this.lineString = other.lineString;
        this.meteoType = other.meteoType;
    }

    /**
     * @return time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
     */
    public String getTimePeriod() {
        return timePeriod;
    }

    /**
     * @param timePeriod time period if relevant (day, evening, night or other parameters, use LDenConfig.TIME_PERIOD)
     */
    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
    }

    /**
     * @return Get vertical plane between source and receiver
     */
    public CutProfile getCutProfile() {
        return cutProfile;
    }

    /**
     * @param cutProfile vertical plane between source and receiver
     */
    public void setCutProfile(CutProfile cutProfile) {
        this.cutProfile = cutProfile;
    }

    /**
     * @return global attenuation
     */
    public double[] getaGlobal() { return aGlobal; }

    public LineString getLineString() {
        return lineString;
    }

    public void setLineString(LineString lineString) {
        this.lineString = lineString;
    }

    public String getMeteoType() {
        return meteoType.getMeteoType();
    }

    public void setMeteoType(MeteoType meteoType){
        this.meteoType = meteoType;
    }
}