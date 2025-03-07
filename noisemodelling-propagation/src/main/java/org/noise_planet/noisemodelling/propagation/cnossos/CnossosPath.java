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
 * All the datas Path of Cnossos
 */
public class CnossosPath extends Path {
    public  double[] aAtm = new double[0];
    public  double[] aDiv = new double[0];
    public  double[] aRef = new double[0];
    public  double[] double_aBoundaryH = new double[0];
    public  double[] double_aBoundaryF = new double[0];
    public  double[] aRetroDiffH = new double[0]; // Alpha Retro Diffraction homogenous
    public  double[] aRetroDiffF = new double[0]; // Alpha Retro Diffraction favorable
    public  double[] aGlobalH = new double[0];
    public double[] aGlobalF = new double[0];
    public double[] aDifH = new double[0];
    public double[] aDifF = new double[0];
    public double[] aGlobal = new double[0];
    public double[] aSource = new double[0]; // directivity attenuation
    public double deltaH = Double.MAX_VALUE;
    public double deltaF= Double.MAX_VALUE;
    public double deltaPrimeH= Double.MAX_VALUE;
    public double deltaPrimeF= Double.MAX_VALUE;
    public double deltaSPrimeRH= Double.MAX_VALUE;
    public double deltaSRPrimeH= Double.MAX_VALUE;
    public ABoundary aBoundaryH = new ABoundary();
    public ABoundary aBoundaryF = new ABoundary();
    public GroundAttenuation groundAttenuation = new GroundAttenuation();
    public double deltaSPrimeRF= Double.MAX_VALUE;
    public double deltaSRPrimeF= Double.MAX_VALUE;
    public double e=0;
    public double deltaRetroH= Double.MAX_VALUE;
    public double deltaRetroF= Double.MAX_VALUE;

    public void init(int size) {
        this.aAtm = new double[size];
        this.aDiv = new double[size];
        this.aRef = new double[size];
        this.double_aBoundaryH = new double[size];
        this.double_aBoundaryF = new double[size];
        this.aGlobalH = new double[size];
        this.aGlobalF = new double[size];
        this.aDifH = new double[size];
        this.aDifF = new double[size];
        this.aGlobal = new double[size];
        this.aSource = new double[size];
        this.aRetroDiffH = new double[size];
        this.aRetroDiffF = new double[size];
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
        this.double_aBoundaryH = other.double_aBoundaryH;
        this.double_aBoundaryF = other.double_aBoundaryF;
        this.aRetroDiffH = other.aRetroDiffH;
        this.aRetroDiffF = other.aRetroDiffF;
        this.aGlobalH = other.aGlobalH;
        this.aGlobalF = other.aGlobalF;
        this.aDifH = other.aDifH;
        this.aDifF = other.aDifF;
        this.aGlobal = other.aGlobal;
        this.aSource = other.aSource;
        this.deltaH = other.deltaH;
        this.deltaF = other.deltaF;
        this.deltaPrimeH = other.deltaPrimeH;
        this.deltaPrimeF = other.deltaPrimeF;
        this.deltaSPrimeRH = other.deltaSPrimeRH;
        this.deltaSRPrimeH = other.deltaSRPrimeH;
        this.aBoundaryH = other.aBoundaryH;
        this.aBoundaryF = other.aBoundaryF;
        this.groundAttenuation = other.groundAttenuation;
        this.deltaSPrimeRF = other.deltaSPrimeRF;
        this.deltaSRPrimeF = other.deltaSRPrimeF;
        this.e = other.e;
        this.deltaRetroH = other.deltaRetroH;
        this.deltaRetroF = other.deltaRetroF;
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
        public double[] wH;
        public double[] cfH;
        public double[] aGroundH;
        public double[] wF;
        public double[] cfF;
        public double[] aGroundF;

        public void init(int size) {
            wH = new double[size];
            cfH = new double[size];
            aGroundH = new double[size];
            wF = new double[size];
            cfF = new double[size];
            aGroundF = new double[size];
        }

        public GroundAttenuation() {
        }

        public GroundAttenuation(GroundAttenuation other) {
            this.wH = other.wH;
            this.cfH = other.cfH;
            this.aGroundH = other.aGroundH;
            this.wF = other.wF;
            this.cfF = other.cfF;
            this.aGroundF = other.aGroundF;
        }
    }
}