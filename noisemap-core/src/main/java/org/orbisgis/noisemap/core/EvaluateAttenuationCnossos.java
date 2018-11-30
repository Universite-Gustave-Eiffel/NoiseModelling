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

import org.locationtech.jts.algorithm.CGAlgorithms3D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
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


    /**
     *
     * @param idfreq
     * @param eLength
     * @param deltaDistance
     * @param h0
     * @return
     */
    public double computeDeltaDiffraction(int idfreq, double eLength, double deltaDistance, double h0) {
        double cprime;
        //C" NMPB 2008 P.33
        //Multiple diffraction
        //CPRIME=( 1+(5*gamma)^2)/((1/3)+(5*gamma)^2)
        double gammaPart = Math.pow((5 * freq_lambda[idfreq]) / eLength, 2);
        double Ch=Math.min(h0*(data.celerity/freq_lambda[idfreq])/250,1);
        //NFS 31-133 page 46
        if (eLength > 0.3) {
            cprime = (1. + gammaPart) / (ONETHIRD + gammaPart);
        } else {
            cprime = 1.;
        }

        //(7.11) NMP2008 P.32
        double testForm = (40 / freq_lambda[idfreq])
                * cprime * deltaDistance;
        double diffractionAttenuation = 0.;
        if (testForm >= -2.) {
            diffractionAttenuation = 10*Ch * Math
                    .log10(3 + testForm);
        }
        // Limit to 0<=DiffractionAttenuation
        diffractionAttenuation = Math.max(0,
                diffractionAttenuation);

        return  diffractionAttenuation;
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
        double AGroundHmin = -3 * (1 - segmentPath.getGm());

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
            //NF S 31-133 page 41 c
            double k = 2 * Math.PI *  data.freq_lvl.get(idfreq) / data.celerity;
            //NF S 31-113 page 41 w
            double w = 0.0185 * Math.pow(data.freq_lvl.get(idfreq), 2.5) * Math.pow(segmentPath.getGw(), 2.6) /
                    (Math.pow(data.freq_lvl.get(idfreq), 1.5) * Math.pow(segmentPath.getGw(), 2.6) + 1.3 * Math.pow(10, 3) * Math.pow(data.freq_lvl.get(idfreq), 0.75) * Math.pow(segmentPath.getGw(), 1.3) + 1.16 * Math.pow(10, 6));
            //NF S 31-113 page 41 Cf
            double cf = path.distancePath * (1 + 3 * w * path.distancePath * Math.pow(Math.E, -Math.sqrt(w * path.distancePath))) / (1 + w * path.distancePath);
            //NF S 31-113 page 41 A sol
            double ASoil = -10 * Math.log10(4 * Math.pow(k, 2) / Math.pow(path.distancePath, 2) *
                    (Math.pow(segmentPath.getZs(path), 2) - Math.sqrt(2 * cf / k) * segmentPath.getZs(path) + cf / k) * (Math.pow(segmentPath.getZr(path), 2) - Math.sqrt(2 * cf / k) * segmentPath.getZr(path) + cf / k));

            aGround[idfreq] =  Math.max(ASoil, AGroundHmin);

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

        double[] aDifFreq = new double[data.freq_lvl.size()];
        double aDif ;
        double[] aGround = new double[data.freq_lvl.size()];
        java.util.Arrays.fill(aGround,-3);
        if (segmentPath.size() == 1) {

            segmentPath.get(0).setGw(segmentPath.get(0).getgPathPrime(path));
            segmentPath.get(0).setGm(segmentPath.get(0).getgPathPrime(path));

            // Here debate if use this condition or not
            if (segmentPath.get(0).gPath!=0) {
                aGround = getAGround(path, segmentPath.get(0), data);
            }

            /*aDif= getADif(segmentPath.get(0).getgPathPrime(path));

            for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
                aDifFreq[idfreq] =  aDif;
            }*/
        }
            // if not direct ...



        return aGround;
    }






    public double[] evaluate(PropagationPath path, PropagationProcessPathData data) {

        // init
        aGlobal = new double[data.freq_lvl.size()];
        double[] aBoundary ;
        nbfreq = data.freq_lvl.size();
        alpha_atmo = data.getAlpha_atmo();


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
        double aDiv = getADiv(path.getDistancePath());

        aBoundary = getABoundary(path,data);

        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            // atm
            double aAtm = getAAtm(path.getDistancePath(),alpha_atmo[idfreq]);


            aGlobal[idfreq] = -(aDiv + aAtm + aBoundary[idfreq]);

        }
        return aGlobal;
    }
}
