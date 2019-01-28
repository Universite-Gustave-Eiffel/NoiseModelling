/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 */
package org.orbisgis.noisemap.core;
import java.util.List;

/**
 * Return the dB value corresponding to the parameters
 * @author Pierre Aumond - 27/04/2017.
 */

public class EvaluateAttenuationCnossos {
    private final static double BASE_LVL = 1.; // 0dB lvl
    private final static double ONETHIRD = 1. / 3.;
    private PropagationProcessPathData data;
    private PropagationPath propagationPath;
    private int nbfreq;
    private double[] alpha_atmo;
    private double[] freq_lambda;
    private double[] aGlobal;

    /**
     *
     * @param nbfreq
     * @param energeticSum
     * @return
     */
    private static double GetGlobalLevel(int nbfreq, double energeticSum[]) {
        double globlvl = 0;
        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            globlvl += energeticSum[idfreq];
        }
        return globlvl;
    }


    public static double dbaToW(double dBA) {
        return Math.pow(10., dBA / 10.);
    }

    public static double wToDba(double w) {
        return 10 * Math.log10(w);
    }

    public double[] getaGlobal() {
        return aGlobal;
    }

    public double[] getDeltaDif(PropagationPath.SegmentPath srpath, PropagationProcessPathData data) {
        double[] DeltaDif = new double[data.freq_lvl.size()];
        double cprime;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {

            double Ch = 1;// Math.min(h0 * (data.celerity / freq_lambda[idfreq]) / 250, 1);

            if (srpath.eLength > 0.3) {
                double gammaPart = Math.pow((5 * freq_lambda[idfreq]) / srpath.eLength, 2);
                cprime = (1. + gammaPart) / (ONETHIRD + gammaPart);
            } else {
                cprime = 1.;
            }

            //(7.11) NMP2008 P.32
            double testForm = (40 / freq_lambda[idfreq])
                    * cprime * srpath.delta;

            double deltaDif= 0.;

            if (testForm >= -2.) {
                deltaDif = 10 * Ch * Math
                        .log10(3 + testForm);
            }

            DeltaDif[idfreq] = Math.max(0,deltaDif);

        }
        return  DeltaDif;

    }


    /**
     * Compute attenuation of sound energy by distance. Minimum distance is one
     * meter.
     * @param distance Distance in meter
     * @return Attenuated sound level. Take only account of geometric dispersion
     * of sound wave.
     */
    public static double getADiv(double distance) {
        return  wToDba(4 * Math.PI * Math.max(1, distance * distance));
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
     *
     * @return
     */
    public static double[] getAGroundCore(PropagationPath path, PropagationPath.SegmentPath segmentPath, PropagationProcessPathData data) {

        double[] aGround = new double[data.freq_lvl.size()];
        double aGroundmin;
        double AGround;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
            //NF S 31-133 page 41 c
            double k = 2 * Math.PI *  data.freq_lvl.get(idfreq) / data.celerity;
            //NF S 31-113 page 41 w
            double w = 0.0185 * Math.pow(data.freq_lvl.get(idfreq), 2.5) * Math.pow(segmentPath.gw, 2.6) /
                    (Math.pow(data.freq_lvl.get(idfreq), 1.5) * Math.pow(segmentPath.gw, 2.6) + 1.3 * Math.pow(10, 3) * Math.pow(data.freq_lvl.get(idfreq), 0.75) * Math.pow(segmentPath.gw, 1.3) + 1.16 * Math.pow(10, 6));
            //NF S 31-113 page 41 Cf
            double cf = segmentPath.dp * (1 + 3 * w * segmentPath.dp * Math.pow(Math.E, -Math.sqrt(w * segmentPath.dp))) / (1 + w * segmentPath.dp);
            //NF S 31-113 page 41 A sol

            if (path.isFavorable()){
                if (data.prime2520) {
                    if (segmentPath.testFormPrime <= 1) {
                        aGroundmin = -3 * (1 - segmentPath.gm);
                    } else {
                        aGroundmin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testFormPrime)));
                    }
                }
                else{
                    if (segmentPath.testForm <= 1) {
                        aGroundmin = -3 * (1 - segmentPath.gm);
                    } else {
                        aGroundmin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testForm)));
                    }
                }
                /** eq. 2.5.19**/
                AGround = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(segmentPath.dp, 2) *
                        (Math.pow(segmentPath.zsPrime, 2) - Math.sqrt(2 * cf / k) * segmentPath.zsPrime + cf / k) *
                        (Math.pow(segmentPath.zrPrime, 2) - Math.sqrt(2 * cf / k) * segmentPath.zrPrime + cf / k));
            }
            else
                {
                 /** eq. 2.5.15**/
                AGround = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(segmentPath.dp, 2) *
                            (Math.pow(segmentPath.zs, 2) - Math.sqrt(2 * cf / k) * segmentPath.zs + cf / k) *
                            (Math.pow(segmentPath.zr, 2) - Math.sqrt(2 * cf / k) * segmentPath.zr + cf / k));
                /** eq. 2.5.18**/
                aGroundmin = -3 * (1 - segmentPath.gm);
            }
            aGround[idfreq] =  Math.max(AGround, aGroundmin);

        }
        return aGround;
    }

    /**
     * Formulae Eq. 2.5.31 - Eq. 2.5.32
     *
     * @param aGround        Asol(O,R) or Asol(S,O) (sol mean ground)
     * @param deltaDifPrim Δdif(S,R') if Asol(S,O) is given or Δdif(S', R) if Asol(O,R)
     * @param deltaDif     Δdif(S, R)
     * @return Δsol(S, O) if Asol(S,O) is given or Δsol(O,R) if Asol(O,R) is given
     */
    private double getDeltaGround(double aGround, double deltaDifPrim, double deltaDif) {
        return -20 * Math.log10(1 + (Math.pow(10, -aGround / 20) - 1) * Math.pow(10, -(deltaDifPrim - deltaDif) / 20));
    }


    private double[] getARef(PropagationPath path, PropagationProcessPathData data) {
        double[] aRef = new double[data.freq_lvl.size()];

        for (int idf = 0; idf < nbfreq; idf++) {
            for (int idRef = 0; idRef < path.refPoints.size(); idRef++) {
                aRef[idf] += - 10 * Math.log10(path.getPointList().get(path.refPoints.get(idRef)).alphaWall);
            }
        }
        return aRef ;
    }


    private double[] getAGround(PropagationPath.SegmentPath segmentPath,PropagationPath path, PropagationProcessPathData data) {
        double[] aGround = new double[data.freq_lvl.size()];
        double aGroundmin;


        // Here there is a debate if use this condition or not
        if (segmentPath.gPath == 0 && data.gDisc == true) {

            if (path.isFavorable()) {
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
    private double[] getABoundary(PropagationPath path, PropagationProcessPathData data) {

        List<PropagationPath.SegmentPath> srPath = path.getSRList();

        double[] aGround;
        double[] aBoundary ;
        double[] aDif = new double[data.freq_lvl.size()];

        // Set Gm and Gw for AGround SR - Table 2.5.b
        if (path.isFavorable()) {
            srPath.get(0).setGw(srPath.get(0).gPath);
            srPath.get(0).setGm(srPath.get(0).gPathPrime);
        } else {
            srPath.get(0).setGw(srPath.get(0).gPathPrime);
            srPath.get(0).setGm(srPath.get(0).gPathPrime);
        }

        aGround = getAGround(srPath.get(0), path,data);
        aBoundary = aGround;
        if (path.difVPoints.size() > 0) {
            List<PropagationPath.SegmentPath> segmentPath = path.getSegmentList();
            double[] DeltaDifSR;
            DeltaDifSR = getDeltaDif(path.getSRList().get(0), data);

            // Eq 2.5.30 - Eq. 2.5.31 - Eq. 2.5.32
            for (int idf = 0; idf < nbfreq; idf++) {
                aDif[idf]=DeltaDifSR[idf];
                aBoundary[idf] = aDif[idf] + aGround[idf];
            }

             ;
        }
        if (path.difHPoints.size() > 0) {
            List<PropagationPath.SegmentPath> segmentPath = path.getSegmentList();

            double[] DeltaDifSR;
            double[] DeltaDifSpR;
            double[] DeltaDifSRp;
            double[] aGroundSO;
            double[] aGroundOR;

            DeltaDifSR = getDeltaDif(path.getSRList().get(0), data);
            DeltaDifSpR = getDeltaDif(path.getSRList().get(1), data);
            DeltaDifSRp = getDeltaDif(path.getSRList().get(2), data);

            // Set Gm and Gw for AGround SO - Table 2.5.b
            if (path.isFavorable()) {
                segmentPath.get(0).setGw(srPath.get(0).gPath);
                segmentPath.get(0).setGm(srPath.get(0).gPathPrime);
            } else {
                segmentPath.get(0).setGw(srPath.get(0).gPathPrime);
                segmentPath.get(0).setGm(srPath.get(0).gPathPrime);
            }

            aGroundSO = getAGround(segmentPath.get(0), path,data);

            // Set Gm and Gw for AGround OR - Table 2.5.b
            if (path.isFavorable()) {
                segmentPath.get(segmentPath.size()-1).setGw(srPath.get(0).gPath);
                segmentPath.get(segmentPath.size()-1).setGm(srPath.get(0).gPath);
            } else {
                segmentPath.get(segmentPath.size()-1).setGw(srPath.get(0).gPath);
                segmentPath.get(segmentPath.size()-1).setGm(srPath.get(0).gPath);
            }
            aGroundOR = getAGround(segmentPath.get(segmentPath.size()-1), path,data);


            // Eq 2.5.30 - Eq. 2.5.31 - Eq. 2.5.32
            for (int idf = 0; idf < nbfreq; idf++) {
                aDif[idf]=Math.min(25,DeltaDifSR[idf])+getDeltaGround(aGroundSO[idf], DeltaDifSpR[idf], DeltaDifSR[idf]) + getDeltaGround(aGroundOR[idf], DeltaDifSRp[idf], DeltaDifSR[idf]);
            }

            aBoundary =  aDif;
        }

        return aBoundary;
    }






    public double[] evaluate(PropagationPath path, PropagationProcessPathData data) {

        // init
        aGlobal = new double[data.freq_lvl.size()];
        double[] aBoundary ;
        double[] aRef ;
        nbfreq = data.freq_lvl.size();

        // Init wave length for each frequency
        freq_lambda = new double[nbfreq];
        for (int idf = 0; idf < nbfreq; idf++) {
            if (data.freq_lvl.get(idf) > 0) {
                freq_lambda[idf] = data.celerity / data.freq_lvl.get(idf);
            } else {
                freq_lambda[idf] = 1;
            }
        }

        // init evolved path
        path.initPropagationPath();

        // init atmosphere
        alpha_atmo = data.getAlpha_atmo();

        double aDiv;
        // divergence
        if (path.refPoints.size() > 0) {
            aDiv = getADiv(path.getSRList().get(0).dPath);
        }else{
            aDiv = getADiv(path.getSRList().get(0).d);
        }


        // boundary (ground + diffration)
        aBoundary = getABoundary(path,data);

        // reflections
        aRef = getARef(path,data);

        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            // atm
            double aAtm;
            if (path.difVPoints.size() > 0 || path.refPoints.size() > 0) {
                aAtm = getAAtm(path.getSRList().get(0).dPath, alpha_atmo[idfreq]);
            }else{
                aAtm = getAAtm(path.getSRList().get(0).d, alpha_atmo[idfreq]);
            }

            aGlobal[idfreq] = -(aDiv + aAtm + aBoundary[idfreq] + aRef[idfreq]);

        }
        return aGlobal;
    }
}
