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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

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
        return 1 / (4 * Math.PI * Math.max(1, distance * distance));
    }

    /**
     * Compute the attenuation of atmospheric absorption
     * @param dist       Propagation distance
     * @param alpha_atmo Atmospheric alpha (dB/km)
     * @return
     */
    public static double getAAtm(double dist, double alpha_atmo) {
        return dbaToW(- (alpha_atmo * dist) / 1000.);
    }


    public static double getAGround() {
        return 0;
    }

    public static double getADif(double dp, double zs, double zr, double gS, double gPath, double gPathPrime) {

        double testForm = dp / (30 * (zs + zr));

        if (testForm <= 1) {
            gPathPrime = testForm * gPath + (1 - testForm) * gS;
        } else {
            gPathPrime = gPath;
        }

        double Gm = gPathPrime;
        double AGroundHmin = -3 * (1 - Gm);


        return 0;
    }

    private double getABoundary() {
        double AGround = getAGround();
        double ADif= getADif(200,10,10,0, 0,0 );

        return 0;
    }






    public double[] evaluate(PropagationPath propagationPath, PropagationProcessPathData data) {

        // init
        aGlobal = new double[data.freq_lvl.size()];
        nbfreq = data.freq_lvl.size();
        alpha_atmo = data.getAlpha_atmo();
        double distancePath = propagationPath.getDistances(propagationPath).distancePath;
        double distanceDirect = propagationPath.getDistances(propagationPath).distanceDirect;
        double eLength = propagationPath.getDistances(propagationPath).eLength;



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
        double aDiv = getADiv(distancePath);

        for (int idfreq = 0; idfreq < nbfreq; idfreq++) {
            // atm
            double aAtm = getAAtm(distancePath,alpha_atmo[idfreq]);
            double aBoundary = getABoundary();


            aGlobal[idfreq] = wToDba(aDiv)+wToDba(aAtm) + wToDba(aBoundary);

        }
        return aGlobal;
    }
}
