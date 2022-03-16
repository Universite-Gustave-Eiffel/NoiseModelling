/*
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

import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.PointPath;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.SegmentPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;
import static org.noise_planet.noisemodelling.pathfinder.PointPath.POINT_TYPE.*;

/**
 * Return the dB value corresponding to the parameters
 * Following Directive 2015/996/EN
 * https://circabc.europa.eu/sd/a/9566c5b9-8607-4118-8427-906dab7632e2/Directive_2015_996_EN.pdf
 * @author Pierre Aumond
 */

public class EvaluateAttenuationCnossos {
    private static double[] freq_lambda;
    private static double[] aGlobal;

    public static double[] getaGlobal() {
        return aGlobal;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateAttenuationCnossos.class);

    /**
     * Eq 2.5.21
     * @param srpath
     * @param data
     * @return
     */
    public static double[] getDeltaDif(SegmentPath srpath, PropagationProcessPathData data) {
        double[] DeltaDif = new double[data.freq_lvl.size()];
        double cprime;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
            double Ch = 1; // Eq 2.5.21
            if (srpath.eLength > 0.3) {
                double gammaPart = pow((5 * freq_lambda[idfreq]) / srpath.eLength, 2);
                cprime = (1. + gammaPart) / (1./3. + gammaPart); // Eq. 2.5.23
            } else {
                cprime = 1.;
            }

            //(7.11) NMP2008 P.32
            double testForm = (40 / freq_lambda[idfreq])
                    * cprime * srpath.getDelta();

            double deltaDif = 0.;
            if (testForm >= -2.) {
                deltaDif = 10 * Ch * log10(Math.max(0, 3 + testForm));
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
    private static double getADiv(double distance) {
        //return Utils.wToDb(4 * Math.PI * Math.max(1, distance * distance));
        return 20*log10(distance)+11;
    }

    /**
     * Compute the attenuation of atmospheric absorption
     * @param dist       Propagation distance
     * @param alpha_atmo Atmospheric alpha (dB/km)
     * @return
     */
    private static double getAAtm(double dist, double alpha_atmo) {
        return alpha_atmo * dist / 1000.;
    }

    /**
     * Eq. 2.5.15
     * Compute Aground
     * @return
     */
    public static double[] getAGroundCore(PropagationPath path, SegmentPath segmentPath, PropagationProcessPathData data) {

        double[] aGround = new double[data.freq_lvl.size()];
        double aGroundMin;
        double AGround;

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
            int fm = data.freq_lvl.get(idfreq);
            double gw = segmentPath.gw;
            double dp = segmentPath.dp;

            //NF S 31-133 page 41 c
            double k = 2 * Math.PI * fm / data.getCelerity();
            //NF S 31-113 page 41 w
            //eq 2.5.17
            double w = 0.0185 * pow(fm, 2.5) * pow(gw, 2.6) /
                    (pow(fm, 1.5) * pow(gw, 2.6) + 1.3e3 * pow(fm, 0.75) * pow(gw, 1.3) + 1.16e6);
            //NF S 31-113 page 41 Cf
            //eq 2.5.16
            double cf = dp * (1 + 3 * w * dp * pow(Math.E, -sqrt(w * dp))) / (1 + w * dp);
            //NF S 31-113 page 41 A sol

            if (path.isFavorable()) {
                if (data.isPrime2520()) {
                    if (segmentPath.testFormF <= 1) {
                        aGroundMin = -3 * (1 - segmentPath.gm);
                    } else {
                        aGroundMin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testFormF)));
                    }
                } else {
                    if (segmentPath.testFormH <= 1) {
                        aGroundMin = -3 * (1 - segmentPath.gm);
                    } else {
                        aGroundMin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testFormH)));
                    }
                }
                /* eq. 2.5.20 */
                AGround = -10 * log10(4 * pow(k, 2) / pow(segmentPath.dp, 2) *
                        (pow(segmentPath.zsF, 2) - sqrt(2 * cf / k) * segmentPath.zsF + cf / k) *
                        (pow(segmentPath.zrF, 2) - sqrt(2 * cf / k) * segmentPath.zrF + cf / k));
            } else {
                /* eq. 2.5.15 */
                AGround = -10 * log10(4 * pow(k, 2) / pow(segmentPath.dp, 2) *
                        (pow(segmentPath.zsH, 2) - sqrt(2 * cf / k) * segmentPath.zsH + cf / k) *
                        (pow(segmentPath.zrH, 2) - sqrt(2 * cf / k) * segmentPath.zrH + cf / k));
                /* eq. 2.5.18 */
                aGroundMin = -3 * (1 - segmentPath.gm);
            }
            aGround[idfreq] = Math.max(AGround, aGroundMin);

            //For testing purpose
            if(path.keepAbsorption) {
                if(path.isFavorable()) {
                    path.groundAttenuation.wF[idfreq] += w;
                    path.groundAttenuation.cfF[idfreq] = cf;
                    path.groundAttenuation.aGroundF[idfreq] = aGround[idfreq];
                }
                else{
                    path.groundAttenuation.wH[idfreq] += w;
                    path.groundAttenuation.cfH[idfreq] = cf;
                    path.groundAttenuation.aGroundH[idfreq] = aGround[idfreq];
                }
            }

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
    private static double getDeltaGround(double aGround, double deltaDifPrim, double deltaDif) {
        double attArg = 1 + (pow(10, -aGround / 20) - 1) * pow(10, -(deltaDifPrim - deltaDif) / 20);
        if(Double.isNaN(attArg)){
            attArg = Double.MAX_VALUE;
        }
        else if (attArg < 0) {
            attArg = 0;
        }
        return -20 * log10(attArg);
    }

    /**
     *
     * @param path
     * @param data
     * @return
     */
    private static double[] getARef(PropagationPath path, PropagationProcessPathData data) {
        double[] aRef = new double[data.freq_lvl.size()];
        Arrays.fill(aRef, 0.0);
        for (int idf = 0; idf < data.freq_lvl.size(); idf++) {
            for (int idRef = 0; idRef < path.refPoints.size(); idRef++) {
                List<Double> alpha = path.getPointList().get(path.refPoints.get(idRef)).alphaWall;
                if(alpha != null && !alpha.isEmpty()) {
                    aRef[idf] += -10 * log10(1 - alpha.get(idf));
                }
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
    private static double[] aGround(SegmentPath segmentPath, PropagationPath path, PropagationProcessPathData data) {
        // Here there is a debate if use the condition isgDisc or not
        // In Directive 2015-2019, isgDisc == true because the term – 3(1 – Gm) takes into account the fact that when the source and the receiver are far apart, the first reflection source side is no longer on the platform but on natural land.
        if (!(segmentPath.gPath == 0 && data.isgDisc())) {
            return getAGroundCore(path, segmentPath, data);
        } else {
            double aGroundMin;
            //For testing purpose
            if(path.keepAbsorption) {
                //Used to calculate value ignored like Cf
                getAGroundCore(path, segmentPath, data);
            }

            if (path.isFavorable()) {
                // The lower bound of Aground,F (calculated with unmodified heights) depends on the geometry of the path
                if (segmentPath.testFormH <= 1) {
                    aGroundMin = -3 * (1 - segmentPath.gm);
                } else {
                    aGroundMin = -3 * (1 - segmentPath.gm) * (1 + 2 * (1 - (1 / segmentPath.testFormH)));
                }
            } else {
                aGroundMin = -3;
            }

            double[] aGround = new double[data.freq_lvl.size()];
            Arrays.fill(aGround, aGroundMin);

            //For testing purpose
            if(path.keepAbsorption) {
                if(path.isFavorable()) {
                    path.groundAttenuation.aGroundF = aGround;
                }
                else{
                    path.groundAttenuation.aGroundH = aGround;
                }
            }
            return aGround;
        }
    }

    /**
     *
     * @param path
     * @param data
     * @return
     */
    private static double[] getABoundary(PropagationPath path, PropagationProcessPathData data) {

        SegmentPath srPath = path.getSRSegment();
        List<SegmentPath> segments = path.getSegmentList();

        double[] aGround;
        double[] aDif = new double[data.freq_lvl.size()];

        double[] aBoundary;

        // Set Gm and Gw for AGround SR - Table 2.5.b
        if (path.isFavorable()) {
            srPath.setGw(srPath.gPath);
        } else {
            srPath.setGw(srPath.gPathPrime);
        }
        srPath.setGm(srPath.gPathPrime);

        List<Integer> difBands = new ArrayList<>();
        List<Integer> noDifBands = new ArrayList<>();
        double deltaD = srPath.d - (segments.get(0).d + segments.get(1).dp);
        double deltaDPrime = -srPath.dPrime + segments.get(0).dPrime + segments.get(1).dPrime;
        for(int freq : data.freq_lvl) {
            double lambda = 340.0 / freq;
            if(deltaD > -lambda/20) {
                if(deltaD > (lambda/4 - deltaDPrime)) {
                    difBands.add(data.freq_lvl.indexOf(freq));
                }
                else {
                    noDifBands.add(data.freq_lvl.indexOf(freq));
                }
            }
        }

        //if (path.difHPoints.size() > 0) {
            // Adif is calculated with diffraction. The ground effect is taken into account in the Adif equation itself (Aground = 0 dB). This therefore gives Aboundary = Adif
            List<SegmentPath> segmentPath = path.getSegmentList();

            double[] deltaDifSR; // is the attenuation due to the diffraction between the source S and the receiver R
            double[] DeltaDifSpR;
            double[] deltaDifSRp;
            double[] aGroundSO; // is the attenuation due to the ground effect on the source side, weighted by the diffraction on the source side; where it is understood that O = O1 in case of multiple diffractions as in Figure 2.5.f
            double[] aGroundOR; // is the attenuation due to the ground effect on the receiver side, weighted by the diffraction on the receiver side.

            deltaDifSR = getDeltaDif(srPath, data);
            DeltaDifSpR = getDeltaDif(segments.get(segments.size() - 2), data);
            deltaDifSRp = getDeltaDif(segments.get(segments.size() - 1), data);

            // Set Gm and Gw for AGround SO - Table 2.5.b
            if (path.isFavorable()) {
                segmentPath.get(0).setGw(segmentPath.get(0).gPath);
            } else {
                segmentPath.get(0).setGw(segmentPath.get(0).gPathPrime);
            }
            segmentPath.get(0).setGm(segmentPath.get(0).gPathPrime);
            aGroundSO = aGround(segmentPath.get(0), path, data);

            // Set Gm and Gw for AGround OR - Table 2.5.b
            segmentPath.get(segmentPath.size() - 1).setGw(segmentPath.get(segmentPath.size() - 1).gPath);
            segmentPath.get(segmentPath.size() - 1).setGm(segmentPath.get(segmentPath.size() - 1).gPath);
            aGroundOR = aGround(segmentPath.get(segmentPath.size() - 1), path, data);

            double[] deltaGroundSO = new double[data.freq_lvl.size()];
            double[] deltaGroundOR = new double[data.freq_lvl.size()];
            // Eq 2.5.30 - Eq. 2.5.31 - Eq. 2.5.32
            for (int idf : difBands) {
                // if Deltadif > 25: Deltadif = 25 dB for a diffraction on a horizontal edge and only on the term Deltadif which figures in the calculation of Adif. This upper bound shall not be applied in the Deltadif terms that intervene in the calculation of Deltaground, or for a diffraction on a vertical edge (lateral diffraction) in the case of industrial noise mapping
                if (segmentPath.get(segmentPath.size() - 1).zrH > 0.0000001) {// see 5.3 Equivalent heights from AFNOR document
                    deltaGroundSO[idf]  = getDeltaGround(aGroundSO[idf], DeltaDifSpR[idf],deltaDifSR[idf]);
                    deltaGroundOR[idf] = getDeltaGround(aGroundOR[idf], deltaDifSRp[idf], deltaDifSR[idf]);
                }else{
                    deltaGroundSO[idf]  = getDeltaGround(aGroundSO[idf], DeltaDifSpR[idf],deltaDifSR[idf]);
                    deltaGroundOR[idf]  = aGroundOR[idf];
                }
                aDif[idf] = Math.min(25, deltaDifSR[idf]) + deltaGroundSO[idf] + deltaGroundOR[idf]; // Eq. 2.5.30
            }

            aBoundary = aDif;
        //} else {
            // Aground is calculated with no diffraction (Adif = 0 dB) and Aboundary = Aground;
            // In addition, Aatm and Aground shall be calculated from the total length of the propagation path.
            aGround = aGround(srPath, path, data);
            aBoundary = aGround;

            if (path.difVPoints.size() > 0 ) {

                aDif = getDeltaDif(srPath, data);

                // Eq. 2.5.33 - Eq. 2.5.34
                for (int idf : noDifBands) {
                    aBoundary[idf] = aDif[idf] + aGround[idf];
                }

            }
        //}

        return aBoundary;
    }

    /**
     *
     * @param data
     */
    public static void init(PropagationProcessPathData data) {
        // init
        aGlobal = new double[data.freq_lvl.size()];

        // Init wave length for each frequency
        freq_lambda = new double[data.freq_lvl.size()];
        for (int idf = 0; idf < data.freq_lvl.size(); idf++) {
            if (data.freq_lvl.get(idf) > 0) {
                freq_lambda[idf] = data.getCelerity() / data.freq_lvl.get(idf);
            } else {
                freq_lambda[idf] = 1;
            }
        }
    }

    public static double[] aDiv(PropagationPath path, PropagationProcessPathData data) {
        double[] aDiv = new double[data.freq_lvl.size()];
        Arrays.fill(aDiv, getADiv(path.difVPoints.isEmpty() ? path.getSRSegment().d : path.getSRSegment().dc));
        return aDiv;
    }

    /**
     *
     * @param data
     * @param distance
     * @return
     */
    public static double[] aAtm(PropagationProcessPathData data, double distance) {
        // init
        double[] aAtm = new double[data.freq_lvl.size()];
        // init atmosphere
        double[] alpha_atmo = data.getAlpha_atmo();

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
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
    public static double[] evaluateAref(PropagationPath path, PropagationProcessPathData data) {
        return getARef(path, data);
    }

    /**
     * Only for propagation Path Cnossos
     * // todo erase evaluate
     * @param path
     * @param data
     * @return
     */
    public static double[] evaluate(PropagationPath path, PropagationProcessPathData data) {
        // init
        aGlobal = new double[data.freq_lvl.size()];
        double[] aBoundary;
        double[] aRef;

        // Init wave length for each frequency
        freq_lambda = new double[data.freq_lvl.size()];
        for (int idf = 0; idf < data.freq_lvl.size(); idf++) {
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
            aDiv = getADiv(path.getSRSegment().dPath);
        } else {
            aDiv = getADiv(path.getSRSegment().d);
        }


        // boundary (ground + diffration)
        aBoundary = getABoundary(path, data);

        // reflections
        aRef = getARef(path, data);

        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
            // atm
            double aAtm;
            if (path.difVPoints.size() > 0 || path.refPoints.size() > 0) {
                aAtm = getAAtm(path.getSRSegment().dPath, alpha_atmo[idfreq]);
            } else {
                aAtm = getAAtm(path.getSRSegment().d, alpha_atmo[idfreq]);
            }

            aGlobal[idfreq] = -(aDiv + aAtm + aBoundary[idfreq] + aRef[idfreq]);

        }
        return aGlobal;
    }

    private static boolean isValidRcrit(PropagationPath pp, int freq, boolean favorable) {
        double lambda = 340.0/freq;
        return favorable ?
                pp.deltaF > -lambda / 20 && pp.deltaF > lambda / 4 - pp.deltaPrimeF || pp.deltaF > 0 :
                pp.deltaH > -lambda / 20 && pp.deltaH > lambda / 4 - pp.deltaPrimeH || pp.deltaH > 0 ;
    }

    public static double[] aBoundary(PropagationPath path, PropagationProcessPathData data) {
        double[] aGround = new double[data.freq_lvl.size()];
        double[] aDif = new double[data.freq_lvl.size()];
        List<PointPath> diffPts = new ArrayList<>();
        for(int i=0; i<path.getPointList().size(); i++) {
            if(path.difHPoints.contains(i)) {
                diffPts.add(path.getPointList().get(i));
            }
            else if(path.difVPoints.contains(i)) {
                diffPts.add(path.getPointList().get(i));
            }
        }
        path.aBoundaryH.init(data.freq_lvl.size());
        path.aBoundaryF.init(data.freq_lvl.size());
        // Without diff
        for(int i=0; i<data.freq_lvl.size(); i++) {
            int finalI = i;
            PointPath first = diffPts.stream()
                    .filter(pp -> pp.type.equals(PointPath.POINT_TYPE.DIFH) || pp.type.equals(DIFV) ||
                            (pp.type.equals(DIFH_RCRIT) &&
                                    isValidRcrit(path, data.freq_lvl.get(finalI), path.isFavorable())))
                    .findFirst()
                    .orElse(null);
            aGround[i] = path.isFavorable() ?
                    aGroundF(path, path.getSRSegment(), data, i) :
                    aGroundH(path, path.getSRSegment(), data, i);
            if(path.groundAttenuation != null && path.groundAttenuation.aGroundF != null) {
                if (path.isFavorable()) {
                    path.groundAttenuation.aGroundF[i] = aGround[i];
                } else {
                    path.groundAttenuation.aGroundH[i] = aGround[i];
                }
            }
            if (first != null) {
                aDif[i] = aDif(path, data, i, first.type);
                if(!first.type.equals(DIFV)) {
                    aGround[i] = 0.;
                }
            }
            // With diff
            else {
                aDif[i] = 0.;
            }

        }
        if(path.keepAbsorption) {
            if (path.isFavorable()) {
                path.absorptionData.aDifF = aDif;
            } else {
                path.absorptionData.aDifH = aDif;
            }
        }
        double[] aBoundary = new double[data.freq_lvl.size()];
        for(int i=0; i<data.freq_lvl.size(); i++) {
            aBoundary[i] = aGround[i] + aDif[i];
        }
        return aBoundary;
    }

    public static double[] deltaRetrodif(PropagationPath reflect, PropagationProcessPathData data) {
        double[] retroDiff = new double[data.freq_lvl.size()];
        for(int i=0; i<data.freq_lvl.size(); i++) {
            if(reflect.refPoints.size() == 0) {
                retroDiff[i]=0;
            }
            PointPath pp = reflect.getPointList().get(reflect.refPoints.get(0));
            double ch = 1.;
            double lambda = 340.0 / data.freq_lvl.get(i);
            double deltaPrime = reflect.isFavorable() ? reflect.deltaRetroF : reflect.deltaRetroH;
            double testForm = 40.0/lambda*deltaPrime;
            double dLRetro = testForm >= -2 ? 10*ch*log10(3+testForm) : 0;
            double dLAbs = 10*log10(1-pp.alphaWall.get(i));
            if(reflect.keepAbsorption) {
                if(reflect.reflectionAttenuation.dLRetro == null) {
                    reflect.reflectionAttenuation.init(data.freq_lvl.size());
                }
                reflect.reflectionAttenuation.dLRetro[i] = dLRetro;
                reflect.reflectionAttenuation.dLAbs[i] = dLAbs;
            }
            retroDiff[i]= dLRetro + dLAbs;
        }
        return retroDiff;
    }

    private static double aDif(PropagationPath proPath, PropagationProcessPathData data, int i, PointPath.POINT_TYPE type) {
        SegmentPath first = proPath.getSegmentList().get(0);
        SegmentPath last = proPath.getSegmentList().get(proPath.getSegmentList().size()-1);

        double ch = 1.;
        double lambda = 340.0 / data.freq_lvl.get(i);
        double cSecond = (type.equals(DIFH) && proPath.difHPoints.size() <= 1) || (type.equals(DIFV) && proPath.difVPoints.size() <= 1) || proPath.e <= 0.3 ? 1. :
                (1+pow(5*lambda/proPath.e, 2))/(1./3+pow(5*lambda/proPath.e, 2));

        double _delta = proPath.isFavorable() && (type.equals(DIFH) || type.equals(DIFH_RCRIT)) ? proPath.deltaF : proPath.deltaH;
        double deltaDStar = (proPath.getSegmentList().get(0).dPrime+proPath.getSegmentList().get(proPath.getSegmentList().size()-1).dPrime-proPath.getSRSegment().dPrime);
        double deltaDiffSR = 0;
        double testForm = 40/lambda*cSecond*_delta;
        if(_delta >= 0 || (_delta > -lambda/20 && _delta > lambda/4 - deltaDStar)) {
            deltaDiffSR = testForm>=-2 ? 10*ch*log10(3+testForm) : 0;
        }

        if(type.equals(DIFV)) {
            if(proPath.keepAbsorption) {
                if(proPath.isFavorable()) {
                    proPath.aBoundaryF.deltaDiffSR[i] = deltaDiffSR;
                }
                else {
                    proPath.aBoundaryH.deltaDiffSR[i] = deltaDiffSR;
                }
            }
            return deltaDiffSR;
        }

        _delta = proPath.isFavorable() ? proPath.deltaSPrimeRF : proPath.deltaSPrimeRH;
        testForm = 40/lambda*cSecond*_delta;
        double deltaDiffSPrimeR = testForm>=-2 ? 10*ch*log10(3+testForm) : 0;

        _delta = proPath.isFavorable() ? proPath.deltaSRPrimeF : proPath.deltaSRPrimeH;
        testForm = 40/lambda*cSecond*_delta;
        double deltaDiffSRPrime = testForm>=-2 ? 10*ch*log10(3+testForm) : 0;

        double aGroundSO = proPath.isFavorable() ? aGroundF(proPath, first, data, i) : aGroundH(proPath, first, data, i);
        double aGroundOR = proPath.isFavorable() ? aGroundF(proPath, last, data, i, true) : aGroundH(proPath, last, data, i, true);

        //If the source or the receiver are under the mean plane, change the computation of deltaDffSR and deltaGround
        Coordinate s = proPath.getSRSegment().s;
        double deltaGroundSO;
        if(s.y < first.a*s.x+first.b){
            deltaGroundSO = aGroundSO;
            deltaDiffSR = deltaDiffSPrimeR;
        } else {
            deltaGroundSO = -20*log10(1+(pow(10, -aGroundSO/20)-1)*pow(10, -(deltaDiffSPrimeR-deltaDiffSR)/20));
        }

        Coordinate r = proPath.getSRSegment().r;
        double deltaGroundOR;
        if(r.y < first.a*r.x+first.b){
            deltaGroundOR = aGroundOR;
            deltaDiffSR = deltaDiffSPrimeR;
        } else {
            deltaGroundOR = -20 * log10(1 + (pow(10, -aGroundOR / 20) - 1) * pow(10, -(deltaDiffSRPrime - deltaDiffSR) / 20));
        }

        //Double check NaN values
        if(Double.isNaN(deltaGroundSO)) {
            LOGGER.error("The deltaGroundSO value is NaN. Has been fixed but should be checked");
            deltaGroundSO = aGroundSO;
            deltaDiffSR = deltaDiffSPrimeR;
        }
        if(Double.isNaN(deltaGroundOR)) {
            LOGGER.error("The deltaGroundOR value is NaN. Has been fixed but should be checked");
            deltaGroundOR = aGroundOR;
            deltaDiffSR = deltaDiffSPrimeR;
        }

        double aDiff = min(25, max(0, deltaDiffSR)) + deltaGroundSO + deltaGroundOR;
        if(proPath.keepAbsorption) {
            if(proPath.isFavorable()) {
                proPath.aBoundaryF.deltaDiffSR[i] = deltaDiffSR;
                proPath.aBoundaryF.aGroundSO[i] = aGroundSO;
                proPath.aBoundaryF.aGroundOR[i] = aGroundOR;
                proPath.aBoundaryF.deltaDiffSPrimeR[i] = deltaDiffSPrimeR;
                proPath.aBoundaryF.deltaDiffSRPrime[i] = deltaDiffSRPrime;
                proPath.aBoundaryF.deltaGroundSO[i] = deltaGroundSO;
                proPath.aBoundaryF.deltaGroundOR[i] = deltaGroundOR;
                proPath.aBoundaryF.aDiff[i] = aDiff;
            }
            else {
                proPath.aBoundaryH.deltaDiffSR[i] = deltaDiffSR;
                proPath.aBoundaryH.aGroundSO[i] = aGroundSO;
                proPath.aBoundaryH.aGroundOR[i] = aGroundOR;
                proPath.aBoundaryH.deltaDiffSPrimeR[i] = deltaDiffSPrimeR;
                proPath.aBoundaryH.deltaDiffSRPrime[i] = deltaDiffSRPrime;
                proPath.aBoundaryH.deltaGroundSO[i] = deltaGroundSO;
                proPath.aBoundaryH.deltaGroundOR[i] = deltaGroundOR;
                proPath.aBoundaryH.aDiff[i] = aDiff;
            }
        }

        return aDiff;
    }

    private static double[] computeCfKValues(PropagationPath proPath, SegmentPath path, PropagationProcessPathData data, int idFreq) {
        return computeCfKValues(proPath, path, data, idFreq, false);
    }
    private static double[] computeCfKValues(PropagationPath proPath, SegmentPath path, PropagationProcessPathData data, int idFreq, boolean forceGPath) {
        int fm = data.freq_lvl.get(idFreq);
        double c = data.getCelerity();
        double dp = path.dp;
        double k = 2*PI*fm/c;
        double gw = forceGPath ? path.gPath : proPath.isFavorable() ? path.gPath : path.gPathPrime;
        double w = 0.0185 * pow(fm, 2.5) * pow(gw, 2.6) /
                (pow(fm, 1.5) * pow(gw, 2.6) + 1.3e3 * pow(fm, 0.75) * pow(gw, 1.3) + 1.16e6);
        double cf = dp * (1 + 3 * w * dp * exp(-sqrt(w * dp))) / (1 + w * dp);
        return new double[]{cf, k, w};
    }

    public static double aGroundH(PropagationPath proPath, SegmentPath path, PropagationProcessPathData data, int idFreq) {
        return aGroundH(proPath, path, data, idFreq, false);
    }

    public static double aGroundH(PropagationPath proPath, SegmentPath path, PropagationProcessPathData data, int idFreq, boolean forceGPath) {
        double[] values = computeCfKValues(proPath, path, data, idFreq, forceGPath);
        double cf = values[0];
        double k = values[1];
        double w = values[2];
        if(proPath.keepAbsorption && path == proPath.getSRSegment()) {
            proPath.groundAttenuation.wH[idFreq] = w;
            proPath.groundAttenuation.cfH[idFreq] = cf;
        }
        if(path.gPath == 0) {
            return -3;
        }
        double dp = path.dp;
        double gm = forceGPath ? path.gPath : path.gPathPrime;
        double aGroundHMin = -3*(1-gm);
        double zs = path.zsH;
        double zr = path.zrH;
        double aGroundHComputed = -10 * log10(4 * (k*k) / (dp*dp) *
                (zs*zs - sqrt(2 * cf / k) * zs + cf / k) *
                (zr*zr - sqrt(2 * cf / k) * zr + cf / k));
        return max(aGroundHComputed, aGroundHMin);
    }

    //Todo check if the favorable testform should be use instead
    public static double aGroundF(PropagationPath proPath, SegmentPath path, PropagationProcessPathData data, int idFreq) {
        return aGroundF(proPath, path, data, idFreq, false);
    }
    public static double aGroundF(PropagationPath proPath, SegmentPath path, PropagationProcessPathData data, int idFreq, boolean forceGPath) {
        double[] values = computeCfKValues(proPath, path, data, idFreq);
        double cf = values[0];
        double k = values[1];
        double w = values[2];
        if(proPath.keepAbsorption && path == proPath.getSRSegment()) {
            proPath.groundAttenuation.wF[idFreq] = w;
            proPath.groundAttenuation.cfF[idFreq] = cf;
        }
        double gm = forceGPath ? path.gPath : path.gPathPrime;
        double aGroundFMin = path.testFormH <= 1 ? -3 * (1 - gm) : -3 * (1 - gm) * (1 + 2 * (1 - (1 / path.testFormH)));
        if(path.gPath == 0) {
            return aGroundFMin;
        }
        else {
            double dp = path.dp;
            double zs = path.zsF;
            double zr = path.zrF;
            double aGroundFComputed = -10 * log10(4 * (k*k) / (dp*dp) *
                    (zs*zs - sqrt(2 * cf / k) * zs + cf / k) *
                    (zr*zr - sqrt(2 * cf / k) * zr + cf / k));
            return max(aGroundFComputed, aGroundFMin);
        }
    }
}
