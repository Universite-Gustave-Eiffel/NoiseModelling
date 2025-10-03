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

/**
 * Attenuation computed from vertical Profile and scene settings following CNOSSOS-EU method.
 */
public class CnossosPath extends Path {
    public  double[] aAtm = new double[0];
    public  double[] aDiv = new double[0];
    public  double[] aRef = new double[0];
    public  double[] double_aBoundary = new double[0];
    public  double[] aRetroDiff = new double[0]; // Alpha Retro Diffraction
    /**
     * Final attenuation (dB)
     * aGlobalRaw but with attenuation (dB) from the ponderation of
     * - the directivity attenuation (favourable or homogeneous atmospheric conditions)
     * - The source directivity attenuation
     */
    public  double[] aGlobal = new double[0];
    /**
     * Global attenuation (dB) without source directivity or atmospheric conditions probability ponderation
     */
    public  double[] aGlobalRaw = new double[0];
    public double[] aDif = new double[0];
    public double[] aSource = new double[0]; // directivity attenuation
    public double delta = Double.MAX_VALUE;
    public double deltaPrime= Double.MAX_VALUE;;
    public double deltaSPrimeR= Double.MAX_VALUE;
    public double deltaSRPrime= Double.MAX_VALUE;
    public ABoundary aBoundary = new ABoundary();;
    public GroundAttenuation groundAttenuation = new GroundAttenuation();
    public double e=0;
    public double deltaRetro= Double.MAX_VALUE;

    public void init(int size) {
        this.aAtm = new double[size];
        this.aDiv = new double[size];
        this.aRef = new double[size];
        this.double_aBoundary = new double[size];;
        this.aGlobal = new double[size];;
        this.aDif = new double[size];
        this.aGlobal = new double[size];
        this.aSource = new double[size];
        this.aRetroDiff = new double[size];
    }

    public CnossosPath() {
    }

    public CnossosPath(CutProfile cutProfile) {
        super(cutProfile);
    }

    public CnossosPath(CnossosPath other) {
        super(other);
        this.aAtm = other.aAtm;
        this.aDiv = other.aDiv;
        this.aRef = other.aRef;
        this.double_aBoundary = other.double_aBoundary;
        this.aRetroDiff = other.aRetroDiff;
        this.aGlobal = other.aGlobal;
        this.aDif = other.aDif;
        this.aSource = other.aSource;
        this.delta = other.delta;
        this.deltaPrime = other.deltaPrime;
        this.deltaSPrimeR = other.deltaSPrimeR;
        this.deltaSRPrime = other.deltaSRPrime;
        this.aBoundary = other.aBoundary;
        this.groundAttenuation = other.groundAttenuation;
        this.e = other.e;
        this.deltaRetro = other.deltaRetro;
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