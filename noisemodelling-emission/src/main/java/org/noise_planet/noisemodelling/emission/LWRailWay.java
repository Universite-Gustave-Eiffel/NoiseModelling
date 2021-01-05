package org.noise_planet.noisemodelling.emission;

public class LWRailWay {

    private double[] LWRolling;
    private double[] LWTraction;
    private double[] LWAerodynamic;
    private double LWBridge;
    private double LWSqueal;



    public void setLWRolling(double[] LWRolling) {
        this.LWRolling = LWRolling;
    }
    public void setLWTraction(double[] LWTraction) {
        this.LWTraction = LWTraction;
    }
    public void setLWAerodynamic(double[] LWAerodynamic) {
        this.LWAerodynamic = LWAerodynamic;
    }
    public void setLWBridge(double LWBridge) {
        this.LWBridge = LWBridge;
    }
    public void setLWSqueal(double LWSqueal) {
        this.LWSqueal = LWSqueal;
    }

    public double[] getLWRolling() {
        return LWRolling;
    }
    public double[] getLWTraction() {
        return LWTraction;
    }
    public double[] getLWAerodynamic() {
        return LWAerodynamic;
    }
    public double getLWBridge() {
        return LWBridge;
    }
    public double getLWSqueal() {
        return LWSqueal;
    }

    public LWRailWay(double[] LWRolling,double[] LWTraction,double[] LWAerodynamic,double LWBridge,double LWSqueal){

        setLWRolling(LWRolling);
        setLWTraction(LWTraction);
        setLWAerodynamic(LWAerodynamic);
        setLWBridge(LWBridge);
        setLWSqueal(LWSqueal);

    }
}
