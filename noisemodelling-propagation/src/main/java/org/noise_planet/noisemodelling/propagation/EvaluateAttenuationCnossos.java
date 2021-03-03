/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 * <p>
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 * <p>
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 * <p>
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 * <p>
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.noise_planet.noisemodelling.propagation;

import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.SegmentPath;

import java.util.Arrays;
import java.util.List;

/**
 * Return the dB value corresponding to the parameters
 * Following Directive 2015/996/EN
 * https://circabc.europa.eu/sd/a/9566c5b9-8607-4118-8427-906dab7632e2/Directive_2015_996_EN.pdf
 * @author Pierre Aumond
 */

public class EvaluateAttenuationCnossos {
    private final static double ONETHIRD = 1. / 3.;
    private int nbfreq;
    private double[] freq_lambda;
    private double[] aGlobal;
    boolean gToSigma = false; // Todo publish parameter issue #13

    public void setaGlobal(double[] aGlobal) {
        this.aGlobal = aGlobal;
    }

    public void setFreq_lambda(double[] freq_lambda) {
        this.freq_lambda = freq_lambda;
    }



    public double[] getaGlobal() {
        return aGlobal;
    }

    public boolean isgToSigma() {
        return gToSigma;
    }

    public void setgToSigma(boolean gToSigma) {
        this.gToSigma = gToSigma;
    }

    /**
     * Eq 2.5.21
     * @param srpath
     * @param data
     * @return
     */
    public double[] getDeltaDif(SegmentPath srpath, PropagationProcessPathData data) {
        double[] DeltaDif = new double[data.freq_lvl.size()];
        double cprime;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {

            double Ch = 1; // Eq 2.5.21

            if (srpath.eLength > 0.3) {
                double gammaPart = Math.pow((5 * freq_lambda[idfreq]) / srpath.eLength, 2);
                cprime = (1. + gammaPart) / (ONETHIRD + gammaPart); // Eq. 2.5.23
            } else {
                cprime = 1.;
            }

            //(7.11) NMP2008 P.32
            double testForm = (40 / freq_lambda[idfreq])
                    * cprime * srpath.getDelta();

            double deltaDif = 0.;

            if (testForm >= -2.) {
                deltaDif = 10 * Ch * Math
                        .log10(Math.max(0, 3 + testForm));
            }

            DeltaDif[idfreq] = Math.max(0, deltaDif);

        }
        return DeltaDif;

    }


    /**
     * Compute attenuation of sound energy by distance. Minimum distance is one
     * meter.
     * Eq. 2.5.12
     * @param distance Distance in meter
     * @return Attenuated sound level. Take only account of geometric dispersion
     * of sound wave.
     */
    public static double getADiv(double distance) {
        return Utils.wToDb(4 * Math.PI * Math.max(1, distance * distance));
    }

    /**
     * Compute the attenuation of atmospheric absorption
     * @param dist       Propagation distance
     * @param alpha_atmo Atmospheric alpha (dB/km)
     * @return
     */
    public static double getAAtm(double dist, double alpha_atmo) {
        return (alpha_atmo * dist) / 1000.;
    }

    /**
     * Eq. 2.5.15
     * Compute Aground
     * @return
     */
    public static double[] getAGroundCore(PropagationPath path, SegmentPath segmentPath, PropagationProcessPathData data) {

        double[] aGround = new double[data.freq_lvl.size()];
        double aGroundmin;
        double AGround;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
            //NF S 31-133 page 41 c
            double k = 2 * Math.PI * data.freq_lvl.get(idfreq) / data.getCelerity();
            //NF S 31-113 page 41 w
            double w = 0.0185 * Math.pow(data.freq_lvl.get(idfreq), 2.5) * Math.pow(segmentPath.gw, 2.6) /
                    (Math.pow(data.freq_lvl.get(idfreq), 1.5) * Math.pow(segmentPath.gw, 2.6) + 1.3 * Math.pow(10, 3) * Math.pow(data.freq_lvl.get(idfreq), 0.75) * Math.pow(segmentPath.gw, 1.3) + 1.16 * Math.pow(10, 6));
            //NF S 31-113 page 41 Cf
            double cf = segmentPath.dp * (1 + 3 * w * segmentPath.dp * Math.pow(Math.E, -Math.sqrt(w * segmentPath.dp))) / (1 + w * segmentPath.dp);
            //NF S 31-113 page 41 A sol

            if (path.isFavorable()) {
                if (data.isPrime2520()) {
                    if (segmentPath.testFormPrime <= 1) {
                        aGroundmin = -3 * (1 - segmentPath.gm);
                    } else {
                        aGroundmin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testFormPrime)));
                    }
                } else {
                    if (segmentPath.testForm <= 1) {
                        aGroundmin = -3 * (1 - segmentPath.gm);
                    } else {
                        aGroundmin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testForm)));
                    }
                }
                /** eq. 2.5.20**/
                AGround = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(segmentPath.dp, 2) *
                        (Math.pow(segmentPath.zsPrime, 2) - Math.sqrt(2 * cf / k) * segmentPath.zsPrime + cf / k) *
                        (Math.pow(segmentPath.zrPrime, 2) - Math.sqrt(2 * cf / k) * segmentPath.zrPrime + cf / k));
            } else {
                /** eq. 2.5.15**/
                AGround = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(segmentPath.dp, 2) *
                        (Math.pow(segmentPath.zs, 2) - Math.sqrt(2 * cf / k) * segmentPath.zs + cf / k) *
                        (Math.pow(segmentPath.zr, 2) - Math.sqrt(2 * cf / k) * segmentPath.zr + cf / k));
                /** eq. 2.5.18**/
                aGroundmin = -3 * (1 - segmentPath.gm);
            }
            aGround[idfreq] = Math.max(AGround, aGroundmin);

        }
        return aGround;
    }

    /**
     * Formulae Eq. 2.5.31 - Eq. 2.5.32
     * @param aGround        Asol(O,R) or Asol(S,O) (sol mean ground)
     * @param deltaDifPrim Δdif(S,R') if Asol(S,O) is given or Δdif(S', R) if Asol(O,R)
     * @param deltaDif     Δdif(S, R)
     * @return Δsol(S, O) if Asol(S,O) is given or Δsol(O,R) if Asol(O,R) is given
     */
    public double getDeltaGround(double aGround, double deltaDifPrim, double deltaDif) {
        double attArg = 1 + (Math.pow(10, -aGround / 20) - 1) * Math.pow(10, -(deltaDifPrim - deltaDif) / 20);
        if (attArg < 0) {
            attArg = 0;
        }
        return -20 * Math.log10(attArg);
    }

    /**
     *
     * @param path
     * @param data
     * @return
     */
    public double[] getARef(PropagationPath path, PropagationProcessPathData data) {
        double[] aRef = new double[data.freq_lvl.size()];
        Arrays.fill(aRef, 0.0);
        for (int idf = 0; idf < nbfreq; idf++) {
            for (int idRef = 0; idRef < path.refPoints.size(); idRef++) {
                List<Double> alpha = path.getPointList().get(path.refPoints.get(idRef)).alphaWall;
                /*if (gToSigma || alphaUniqueValue > 1){
                    PropagationProcessData.getWallAlpha(alphaUniqueValue, data.freq_lvl.get(idf));
                }*/
                aRef[idf] += -10 * Math.log10(1 - alpha.get(idf));
            }
        }
        return aRef;
    }

    /**
     *
     * @param segmentPath
     * @param path
     * @param data
     * @return
     */
    public double[] getAGround(SegmentPath segmentPath, PropagationPath path, PropagationProcessPathData data) {
        double[] aGround = new double[data.freq_lvl.size()];
        double aGroundmin;

        // Here there is a debate if use the condition isgDisc or not
        // In Directive 2015-2019, isgDisc == true because the term – 3(1 – Gm) takes into account the fact that when the source and the receiver are far apart, the first reflection source side is no longer on the platform but on natural land.
        if (segmentPath.gPath == 0 && data.isgDisc()) {
            if (path.isFavorable()) {
                // The lower bound of Aground,F (calculated with unmodified heights) depends on the geometry of the path
                if (segmentPath.testForm <= 1) {
                    aGroundmin = -3 * (1 - segmentPath.gm);
                } else {
                    aGroundmin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testForm)));
                }
            } else {
                aGroundmin = -3;
            }
            java.util.Arrays.fill(aGround, aGroundmin);
        } else {
            aGround = getAGroundCore(path, segmentPath, data);
        }
        return aGround;
    }

    /**
     *
     * @param path
     * @param data
     * @return
     */
    public double[] getABoundary(PropagationPath path, PropagationProcessPathData data) {

        List<SegmentPath> srPath = path.getSRList();

        double[] aGround;
        double[] aDif = new double[data.freq_lvl.size()];

        double[] aBoundary;


        // Set Gm and Gw for AGround SR - Table 2.5.b
        if (path.isFavorable()) {
            srPath.get(0).setGw(srPath.get(0).gPath);
            srPath.get(0).setGm(srPath.get(0).gPathPrime);
        } else {
            srPath.get(0).setGw(srPath.get(0).gPathPrime);
            srPath.get(0).setGm(srPath.get(0).gPathPrime);
        }

        if (path.difHPoints.size() > 0) {
            // Adif is calculated with diffraction. The ground effect is taken into account in the Adif equation itself (Aground = 0 dB). This therefore gives Aboundary = Adif
            List<SegmentPath> segmentPath = path.getSegmentList();

            double[] DeltaDifSR; // is the attenuation due to the diffraction between the source S and the receiver R
            double[] DeltaDifSpR;
            double[] DeltaDifSRp;
            double[] aGroundSO; // is the attenuation due to the ground effect on the source side, weighted by the diffraction on the source side; where it is understood that O = O1 in case of multiple diffractions as in Figure 2.5.f
            double[] aGroundOR; // is the attenuation due to the ground effect on the receiver side, weighted by the diffraction on the receiver side.

            DeltaDifSR = getDeltaDif(srPath.get(0), data);
            DeltaDifSpR = getDeltaDif(srPath.get(srPath.size() - 2), data);
            DeltaDifSRp = getDeltaDif(srPath.get(srPath.size() - 1), data);

            // Set Gm and Gw for AGround SO - Table 2.5.b
            if (path.isFavorable()) {
                segmentPath.get(0).setGw(segmentPath.get(0).gPath);
                segmentPath.get(0).setGm(segmentPath.get(0).gPathPrime);
            } else {
                segmentPath.get(0).setGw(segmentPath.get(0).gPathPrime);
                segmentPath.get(0).setGm(segmentPath.get(0).gPathPrime);
            }
            aGroundSO = getAGround(segmentPath.get(0), path, data);

            // Set Gm and Gw for AGround OR - Table 2.5.b
            if (path.isFavorable()) {
                segmentPath.get(segmentPath.size() - 1).setGw(segmentPath.get(segmentPath.size() - 1).gPath);
                segmentPath.get(segmentPath.size() - 1).setGm(segmentPath.get(segmentPath.size() - 1).gPath);
            } else {
                segmentPath.get(segmentPath.size() - 1).setGw(segmentPath.get(segmentPath.size() - 1).gPath);
                segmentPath.get(segmentPath.size() - 1).setGm(segmentPath.get(segmentPath.size() - 1).gPath);
            }
            aGroundOR = getAGround(segmentPath.get(segmentPath.size() - 1), path, data);

            double[] deltaGroundSO = new double[data.freq_lvl.size()];
            double[] deltaGroundOR = new double[data.freq_lvl.size()];
            // Eq 2.5.30 - Eq. 2.5.31 - Eq. 2.5.32
            for (int idf = 0; idf < nbfreq; idf++) {
                // if Deltadif > 25: Deltadif = 25 dB for a diffraction on a horizontal edge and only on the term Deltadif which figures in the calculation of Adif. This upper bound shall not be applied in the Deltadif terms that intervene in the calculation of Deltaground, or for a diffraction on a vertical edge (lateral diffraction) in the case of industrial noise mapping
                if (segmentPath.get(segmentPath.size() - 1).zr > 0.0000001) {// see 5.3 Equivalent heights from AFNOR document
                    deltaGroundSO[idf]  = getDeltaGround(aGroundSO[idf], DeltaDifSpR[idf],DeltaDifSR[idf]);
                    deltaGroundOR[idf] = getDeltaGround(aGroundOR[idf], DeltaDifSRp[idf], DeltaDifSR[idf]);
                }else{
                    deltaGroundSO[idf]  = getDeltaGround(aGroundSO[idf], DeltaDifSpR[idf],DeltaDifSR[idf]);
                    deltaGroundOR[idf]  = aGroundOR[idf];
                }
                aDif[idf] = Math.min(25, DeltaDifSR[idf]) + deltaGroundSO[idf] + deltaGroundOR[idf]; // Eq. 2.5.30
            }

            aBoundary = aDif;
        } else {
            // Aground is calculated with no diffraction (Adif = 0 dB) and Aboundary = Aground;
            // In addition, Aatm and Aground shall be calculated from the total length of the propagation path.
            aGround = getAGround(srPath.get(0), path, data);
            aBoundary = aGround;

            if (path.difVPoints.size() > 0 ) {

                aDif = getDeltaDif(srPath.get(0), data);

                // Eq. 2.5.33 - Eq. 2.5.34
                for (int idf = 0; idf < nbfreq; idf++) {
                    aBoundary[idf] = aDif[idf] + aGround[idf];
                }

            }
        }



        return aBoundary;
    }

    /**
     *
     * @param data
     */
    public void initEvaluateAttenutation(PropagationProcessPathData data) {
        // init
        aGlobal = new double[data.freq_lvl.size()];
        nbfreq = data.freq_lvl.size();

        // Init wave length for each frequency
        freq_lambda = new double[nbfreq];
        for (int idf = 0; idf < nbfreq; idf++) {
            if (data.freq_lvl.get(idf) > 0) {
                freq_lambda[idf] = data.getCelerity() / data.freq_lvl.get(idf);
            } else {
                freq_lambda[idf] = 1;
            }
        }

    }


    public double[] evaluateAdiv(PropagationPath path, PropagationProcessPathData data) {
        double[] aDiv = new double[data.freq_lvl.size()];
        double att ;
        att = getADiv(path.getSRList().get(0).d);
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            aDiv[idfreq] = att;
        }
        return aDiv;
    }

    /**
     *
     * @param data
     * @param distance
     * @return
     */
    public double[] evaluateAatm(PropagationProcessPathData data, double distance) {
        // init
        double[] aAtm = new double[data.freq_lvl.size()];
        // init atmosphere
        double[] alpha_atmo = data.getAlpha_atmo();

        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            aAtm[idfreq] = getAAtm(distance, alpha_atmo[idfreq]);
        }
        return aAtm;
    }

    /**
     *
     * @param path
     * @param data
     * @return
     */
    public double[] evaluateAref(PropagationPath path, PropagationProcessPathData data) {
        return getARef(path, data);
    }

    /**
     *
     * @param path
     * @param data
     * @param Favorable
     * @return
     */
    public double[] evaluateAboundary(PropagationPath path, PropagationProcessPathData data, boolean Favorable) {
        double[] aBoundary;
        // boundary (ground + diffration)
        aBoundary = getABoundary(path, data);
        return aBoundary;
    }



    /**
     * Only for propagation Path Cnossos
     * // todo erase evaluate
     * @param path
     * @param data
     * @return
     */
    public double[] evaluate(PropagationPath path, PropagationProcessPathData data) {
        // init
        aGlobal = new double[data.freq_lvl.size()];
        double[] aBoundary;
        double[] aRef;
        nbfreq = data.freq_lvl.size();

        // Init wave length for each frequency
        freq_lambda = new double[nbfreq];
        for (int idf = 0; idf < nbfreq; idf++) {
            if (data.freq_lvl.get(idf) > 0) {
                freq_lambda[idf] = data.getCelerity() / data.freq_lvl.get(idf);
            } else {
                freq_lambda[idf] = 1;
            }
        }

        // init evolved path
        path.initPropagationPath();

        // init atmosphere
        double[] alpha_atmo = data.getAlpha_atmo();

        double aDiv;
        // divergence
        if (path.refPoints.size() > 0) {
            aDiv = getADiv(path.getSRList().get(0).dPath);
        } else {
            aDiv = getADiv(path.getSRList().get(0).d);
        }


        // boundary (ground + diffration)
        aBoundary = getABoundary(path, data);

        // reflections
        aRef = getARef(path, data);

        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            // atm
            double aAtm;
            if (path.difVPoints.size() > 0 || path.refPoints.size() > 0) {
                aAtm = getAAtm(path.getSRList().get(0).dPath, alpha_atmo[idfreq]);
            } else {
                aAtm = getAAtm(path.getSRList().get(0).d, alpha_atmo[idfreq]);
            }

            aGlobal[idfreq] = -(aDiv + aAtm + aBoundary[idfreq] + aRef[idfreq]);

        }
        return aGlobal;
    }
}
