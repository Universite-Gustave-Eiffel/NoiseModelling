/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.jdbc;

import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;

/**
 * Compute noise levels for Lden
 */
public class LdenNoiseMapLoader extends ReceiverGridNoiseMapLoader {
    AttenuationCnossosParameters attenuationCnossosParametersDay = new AttenuationCnossosParameters();
    AttenuationCnossosParameters attenuationCnossosParametersEvening = new AttenuationCnossosParameters();
    AttenuationCnossosParameters attenuationCnossosParametersNight = new AttenuationCnossosParameters();

    public LdenNoiseMapLoader(LdenNoiseMapParameters ldenNoiseMapParameters) {
        super(ldenNoiseMapParameters);
    }

    /**
     * Sets the propagation process path data for the specified time period.
     * @param time_period the time period for which to set the propagation process path data.
     * @param attenuationCnossosParameters the attenuation Cnossos parameters to set.
     */
    public void setPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD time_period, AttenuationCnossosParameters attenuationCnossosParameters) {
        switch (time_period) {
            case DAY:
                attenuationCnossosParametersDay = attenuationCnossosParameters;
            case EVENING:
                attenuationCnossosParametersEvening = attenuationCnossosParameters;
            default:
                attenuationCnossosParametersNight = attenuationCnossosParameters;
        }
    }
    public AttenuationCnossosParameters getPropagationProcessPathDataDay() {
        return attenuationCnossosParametersDay;
    }

    public void setPropagationProcessPathDataDay(AttenuationCnossosParameters attenuationCnossosParametersDay) {
        this.attenuationCnossosParametersDay = attenuationCnossosParametersDay;
    }

    public AttenuationCnossosParameters getPropagationProcessPathDataEvening() {
        return attenuationCnossosParametersEvening;
    }

    public void setPropagationProcessPathDataEvening(AttenuationCnossosParameters attenuationCnossosParametersEvening) {
        this.attenuationCnossosParametersEvening = attenuationCnossosParametersEvening;
    }

    public AttenuationCnossosParameters getPropagationProcessPathDataNight() {
        return attenuationCnossosParametersNight;
    }

    public void setPropagationProcessPathDataNight(AttenuationCnossosParameters attenuationCnossosParametersNight) {
        this.attenuationCnossosParametersNight = attenuationCnossosParametersNight;
    }

    /**
     * Retrieves the propagation process path data for the specified time period.
     * @param time_period the time period for which to retrieve the propagation process path data.
     * @return the attenuation Cnossos parameters for the specified time period.
     */
    public AttenuationCnossosParameters getPropagationProcessPathData(LdenNoiseMapParameters.TIME_PERIOD time_period) {
        switch (time_period) {
            case DAY:
                return attenuationCnossosParametersDay;
            case EVENING:
                return attenuationCnossosParametersEvening;
            default:
                return attenuationCnossosParametersNight;
        }
    }


    public void setComputeRaysOutFactory(IComputeRaysOutFactory computeRaysOutFactory) {
        this.computeRaysOutFactory = computeRaysOutFactory;
    }


}
