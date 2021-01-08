package org.noise_planet.noisemodelling.emission;

public class LWRailWay {

    private double[] lWRolling;
    private double[] lWTractionA;
    private double[] lWTractionB;
    private double[] lWAerodynamicA;
    private double[] lWAerodynamicB;
    private double[] lWBridge;



    public void setLWRolling(double[] LWRolling) {
        this.lWRolling = LWRolling;
    }
    public void setLWTractionA(double[] LWTractionA) {
        this.lWTractionA = LWTractionA;
    }
    public void setLWTractionB(double[] LWTractionB) {
        this.lWTractionB = LWTractionB;
    }
    public void setLWAerodynamicA(double[] LWAerodynamicA) {
        this.lWAerodynamicA = LWAerodynamicA;
    }
    public void setLWAerodynamicB(double[] LWAerodynamicB) {
        this.lWAerodynamicB = LWAerodynamicB;
    }
    public void setLWBridge(double[] LWBridge) {
        this.lWBridge = LWBridge;
    }

    public double[] getLWRolling() {
        return lWRolling;
    }
    public double[] getLWTractionA() {
        return lWTractionA;
    }
    public double[] getLWTractionB() {
        return lWTractionB;
    }
    public double[] getLWAerodynamicA() {
        return lWAerodynamicA;
    }
    public double[] getLWAerodynamicB() {
        return lWAerodynamicB;
    }
    public double[] getLWBridge() {
        return lWBridge;
    }

    public LWRailWay(double[] lWRolling,double[] lWTractionA,double[] lWTractionB,double[] lWAerodynamicA,double[] lWAerodynamicB,double[] lWBridge){

        setLWRolling(lWRolling);
        setLWTractionA(lWTractionA);
        setLWTractionB(lWTractionB);
        setLWAerodynamicA(lWAerodynamicA);
        setLWAerodynamicB(lWAerodynamicB);
        setLWBridge(lWBridge);

    }
}
