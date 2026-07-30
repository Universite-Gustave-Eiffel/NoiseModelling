/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation.cnossos;

import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.propagation.AttenuationOutput;

/**
 * Output of the Cnossos attenuation computation (data class).
 *
 * @author Nicolas Fortin
 * @author Pierre Aumond
 * @author Martin Glesser
 */
public class CnossosAttenuationOutput extends AttenuationOutput {
    /**
     * Intermediate attenuation.
     */
    public  double[] aAtm = new double[0];
    public  double[] aDiv = new double[0];
    public  double[] aRef = new double[0];
    public  double[] double_aBoundary = new double[0];
    public  double[] aRetroDiff = new double[0]; // Alpha Retro Diffraction

    /**
     * Global attenuation (dB) without source directivity or atmospheric conditions probability ponderation
     */
    public  double[] aGlobalRaw = new double[0];
    public double[] aDif = new double[0];
    public double[] aSource = new double[0]; // directivity attenuation

    public ABoundary aBoundary = new ABoundary();
    public GroundAttenuation groundAttenuation = new GroundAttenuation();
    public double deltaRetro= Double.MAX_VALUE;
    public boolean keepAbsorption = false;

    public CnossosPath propagationPath = new CnossosPath();

    public void init(int size) {
        super.init(size);
        this.aAtm = new double[size];
        this.aDiv = new double[size];
        this.aRef = new double[size];
        this.double_aBoundary = new double[size];
        this.aDif = new double[size];
        this.aSource = new double[size];
        this.aRetroDiff = new double[size];
    }

    public CnossosAttenuationOutput() {
    }

    public CnossosAttenuationOutput(CutProfile cutProfile) {
        this.cutProfile = cutProfile;
    }

    public CnossosAttenuationOutput(CnossosAttenuationOutput other) {
        super(other);
        this.aGlobalRaw = other.aGlobalRaw;
        this.aAtm = other.aAtm;
        this.aDiv = other.aDiv;
        this.aRef = other.aRef;
        this.double_aBoundary = other.double_aBoundary;
        this.aRetroDiff = other.aRetroDiff;
        this.aDif = other.aDif;
        this.aSource = other.aSource;
        this.aBoundary = other.aBoundary;
        this.groundAttenuation = other.groundAttenuation;
        this.deltaRetro = other.deltaRetro;
        this.propagationPath = other.propagationPath;
        this.keepAbsorption = other.keepAbsorption;
    }

    public static class ABoundary {
        public double[] deltaDiffSR;
        public double[] aGroundSO;
        public double[] aGroundOR;
        public double[] deltaDiffSPrimeR;
        public double[] deltaDiffSRPrime;
        public double[] deltaGroundSO;
        public double[] deltaGroundOR;
        public double[] aDiff;

        private boolean init = false;

        public void init(int freqCount) {
            if(!init) {
                deltaDiffSR = new double[freqCount];
                aGroundSO = new double[freqCount];
                aGroundOR = new double[freqCount];
                deltaDiffSPrimeR = new double[freqCount];
                deltaDiffSRPrime = new double[freqCount];
                deltaGroundSO = new double[freqCount];
                deltaGroundOR = new double[freqCount];
                aDiff = new double[freqCount];
                init = true;
            }
        }
    }

    public static class GroundAttenuation {
        public double[] w;
        public double[] cf;
        public double[] aGround;

        public void init(int size) {
            w = new double[size];
            cf = new double[size];
            aGround = new double[size];
        }

        public GroundAttenuation() {
        }

        public GroundAttenuation(GroundAttenuation other) {
            this.w = other.w;
            this.cf = other.cf;
            this.aGround = other.aGround;
        }
    }
}
