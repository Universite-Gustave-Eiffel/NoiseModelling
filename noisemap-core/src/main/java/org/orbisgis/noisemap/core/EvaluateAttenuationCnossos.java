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



    public double[][] getDeltaDif(PropagationPath path, PropagationProcessPathData data) {
        double[][] DeltaDif = new double[data.freq_lvl.size()][3];
        double cprime;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {

            double gammaPart = Math.pow((5 * freq_lambda[idfreq]) / path.getSRList().get(0).eLength, 2);
            double Ch = 1;// Math.min(h0 * (data.celerity / freq_lambda[idfreq]) / 250, 1);

            if (path.getSRList().get(0).eLength > 0.3) {
                cprime = (1. + gammaPart) / (ONETHIRD + gammaPart);
            } else {
                cprime = 1.;
            }

            //(7.11) NMP2008 P.32
            double testForm = (40 / freq_lambda[idfreq])
                    * cprime * path.getSRList().get(0).delta;

            double deltaDif= 0.;

            if (testForm >= -2.) {
                deltaDif = 10 * Ch * Math
                        .log10(3 + testForm);
            }
            // todo upper bound 25 dB
            DeltaDif[idfreq][0] = Math.max(0,deltaDif);
            DeltaDif[idfreq][1] = Math.max(0,deltaDif);
            DeltaDif[idfreq][2] = Math.max(0,deltaDif);

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
    public static double[] getAGround(PropagationPath path, PropagationPath.SegmentPath segmentPath, PropagationProcessPathData data) {

        double[] aGround = new double[data.freq_lvl.size()];
        double aGroundmin;
        double AGround;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
            //NF S 31-133 page 41 c
            double k = 2 * Math.PI *  data.freq_lvl.get(idfreq) / data.celerity;
            //NF S 31-113 page 41 w
            double w = 0.0185 * Math.pow(data.freq_lvl.get(idfreq), 2.5) * Math.pow(segmentPath.getGw(), 2.6) /
                    (Math.pow(data.freq_lvl.get(idfreq), 1.5) * Math.pow(segmentPath.getGw(), 2.6) + 1.3 * Math.pow(10, 3) * Math.pow(data.freq_lvl.get(idfreq), 0.75) * Math.pow(segmentPath.getGw(), 1.3) + 1.16 * Math.pow(10, 6));
            //NF S 31-113 page 41 Cf
            double cf = segmentPath.dp * (1 + 3 * w * segmentPath.dp * Math.pow(Math.E, -Math.sqrt(w * segmentPath.dp))) / (1 + w * segmentPath.dp);
            //NF S 31-113 page 41 A sol

            if (path.isFavorable()){
                AGround = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(segmentPath.dp, 2) *
                        (Math.pow(segmentPath.zsPrime, 2) - Math.sqrt(2 * cf / k) * segmentPath.zsPrime + cf / k) *
                        (Math.pow(segmentPath.zrPrime, 2) - Math.sqrt(2 * cf / k) * segmentPath.zrPrime + cf / k));

                if (segmentPath.testFormPrime <= 1) {
                    aGroundmin = -3 * (1 - segmentPath.gm);
                } else {
                    aGroundmin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testFormPrime)));
                }

            }
            else
                {
                AGround = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(segmentPath.dp, 2) *
                            (Math.pow(segmentPath.zs, 2) - Math.sqrt(2 * cf / k) * segmentPath.zs + cf / k) *
                            (Math.pow(segmentPath.zr, 2) - Math.sqrt(2 * cf / k) * segmentPath.zr + cf / k));
                aGroundmin = -3 * (1 - segmentPath.getGm());
            }



            aGround[idfreq] =  Math.max(AGround, aGroundmin);

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
        List<PropagationPath.SegmentPath> segmentPath = path.getSegmentList();

        double[] aGround = new double[data.freq_lvl.size()];
        double[] aDif = new double[data.freq_lvl.size()];
        double[] aBoundary = new double[data.freq_lvl.size()];



        if (segmentPath.size() == 1) {
            segmentPath.get(0).setGm(segmentPath.get(0).getgPathPrime(path));
            if (path.isFavorable()){
                segmentPath.get(0).setGw(segmentPath.get(0).getgPathPrime(path));
            }else{
                segmentPath.get(0).setGw(segmentPath.get(0).gPath);
            }
            // Here there is a debate if use this condition or not
            if (segmentPath.get(0).gPath!=0) {
                aGround = getAGround(path, segmentPath.get(0), data);
            }
            else{
                double aGroundmin;
                if (path.isFavorable()) {
                    aGroundmin = -3;
                }else{
                    aGroundmin = -3 * (1 - segmentPath.get(0).getGm());
                }
                java.util.Arrays.fill(aGround, aGroundmin);
            }

            aBoundary = aGround;
        }else{
            double[][] DeltaDif = new double[data.freq_lvl.size()][3];

            DeltaDif = getDeltaDif(path, data);


            aBoundary = aDif;
        }

        return aBoundary;
    }






    public double[] evaluate(PropagationPath path, PropagationProcessPathData data) {

        // init
        aGlobal = new double[data.freq_lvl.size()];
        double[] aBoundary ;
        nbfreq = data.freq_lvl.size();
        alpha_atmo = data.getAlpha_atmo();
        // init evolved path
        path.initPropagationPath();

        // Init wave length for each frequency
        freq_lambda = new double[nbfreq];
        for (int idf = 0; idf < nbfreq; idf++) {
            if (data.freq_lvl.get(idf) > 0) {
                freq_lambda[idf] = data.celerity / data.freq_lvl.get(idf);
            } else {
                freq_lambda[idf] = 1;
            }
        }


        // divergence
        double aDiv = getADiv(path.getSRList().get(0).d);

        aBoundary = getABoundary(path,data);

        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            // atm
            double aAtm = getAAtm(path.getSRList().get(0).d,alpha_atmo[idfreq]);


            aGlobal[idfreq] = -(aDiv + aAtm + aBoundary[idfreq]);

        }
        return aGlobal;
    }
}
