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


import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector3D;
import org.noise_planet.noisemodelling.pathfinder.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.*;
import static java.lang.Math.log10;
import static org.noise_planet.noisemodelling.pathfinder.PointPath.POINT_TYPE.DIFH;
import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.*;

/**
 * Way to store data computed by threads.
 * Multiple threads use one instance.
 * This class must be thread safe
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class ComputeRaysOutAttenuation implements IComputeRaysOut {
    public ConcurrentLinkedDeque<VerticeSL> receiversAttenuationLevels = new ConcurrentLinkedDeque<>();
    public List<PropagationPath> propagationPaths = Collections.synchronizedList(new ArrayList<PropagationPath>());

    public PropagationProcessPathData genericMeteoData;
    public CnossosPropagationData inputData;

    public ComputeRaysOutAttenuation(boolean keepRays, PropagationProcessPathData pathData, CnossosPropagationData inputData) {
        this.keepRays = keepRays;
        this.genericMeteoData = pathData;
        this.inputData = inputData;
    }

    public ComputeRaysOutAttenuation(boolean keepRays, PropagationProcessPathData pathData) {
        this.keepRays = keepRays;
        this.genericMeteoData = pathData;
    }

    public ComputeRaysOutAttenuation(boolean keepRays, boolean keepAbsorption, PropagationProcessPathData pathData) {
        this.keepRays = keepRays;
        this.keepAbsorption = keepAbsorption;
        this.genericMeteoData = pathData;
    }

    public boolean keepRays;
    public boolean keepAbsorption = false;
    public AtomicLong rayCount = new AtomicLong();
    public AtomicLong nb_couple_receiver_src = new AtomicLong();
    public AtomicLong nb_obstr_test = new AtomicLong();
    public AtomicLong nb_image_receiver = new AtomicLong();
    public AtomicLong nb_reflexion_path = new AtomicLong();
    public AtomicLong nb_diffraction_path = new AtomicLong();
    public AtomicInteger cellComputed = new AtomicInteger();
    private static final double angle_section = (2 * Math.PI) / PropagationProcessPathData.DEFAULT_WIND_ROSE.length;

    /**
     * get the rose index to search the mean occurrence p of favourable conditions in the direction of the path (S,R):
     * @param receiver
     * @param source
     * @return rose index
     */
    public static int getRoseIndex(Coordinate receiver, Coordinate source) {
        return getRoseIndex(Angle.angle(receiver, source));
    }

    /**
     * The north slice is the last array index not the first one
     * Ex for slice width of 20°:
     *      - The first column 20° contain winds between 10 to 30 °
     *      - The last column 360° contains winds between 350° to 360° and 0 to 10°
     * get the rose index to search the mean occurrence p of favourable conditions in the direction of the angle:
     * @return rose index
     */
    public static int getRoseIndex(double angle) {
        // Angle from cos -1 sin 0
        double angleRad = -(angle - Math.PI);
        // Offset angle by PI / 2 (North),
        // the north slice ranges is [PI / 2 + angle_section / 2; PI / 2 - angle_section / 2]
        angleRad -= (Math.PI / 2 - angle_section / 2);
        // Fix out of bounds angle 0-2Pi
        if(angleRad < 0) {
            angleRad += Math.PI * 2;
        }
        int index = (int)(angleRad / angle_section) - 1;
        if(index < 0) {
            index = PropagationProcessPathData.DEFAULT_WIND_ROSE.length - 1;
        }
        return index;
    }

    @Override
    public void finalizeReceiver(long receiverId) {

    }

    public CnossosPropagationData getInputData() {
        return inputData;
    }

    @Override
    public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        rayCount.addAndGet(propagationPath.size());
        if(keepRays) {
            propagationPaths.addAll(propagationPath);
        }
        double[] aGlobalMeteo = computeAttenuation(genericMeteoData, sourceId, sourceLi, receiverId, propagationPath);
        if (aGlobalMeteo != null && aGlobalMeteo.length > 0) {
            if(inputData != null) {
                if(sourceId < inputData.sourcesPk.size()) {
                    sourceId = inputData.sourcesPk.get((int)sourceId);
                }
                if(receiverId < inputData.receiversPk.size()) {
                    receiverId = inputData.receiversPk.get((int)receiverId);
                }
            }
            receiversAttenuationLevels.add(new VerticeSL(receiverId, sourceId, aGlobalMeteo));
            return aGlobalMeteo;
        } else {
            return new double[0];
        }
    }

    public double[] computeAttenuation(PropagationProcessPathData data, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        if (data == null) {
            return new double[0];
        }
        // cache frequencies
        double[] frequencies = new double[0];
        if(inputData != null) {
            frequencies = new double[inputData.freq_lvl.size()];
            for (int idFrequency = 0; idFrequency < frequencies.length; idFrequency++) {
                frequencies[idFrequency] = inputData.freq_lvl.get(idFrequency);
            }
        }
        // Compute receiver/source attenuation
        double[] propagationAttenuationSpectrum = null;
        for (PropagationPath proPath : propagationPath) {
            if(keepAbsorption) {
                proPath.keepAbsorption = true;
                proPath.groundAttenuation.init(data.freq_lvl.size());
                proPath.absorptionData.init(data.freq_lvl.size());
            }
            EvaluateAttenuationCnossos.init(data);
            //ADiv computation
            double[] aDiv = EvaluateAttenuationCnossos.aDiv(proPath, data);
            //AAtm computation
            double[] aAtm = EvaluateAttenuationCnossos.aAtm(data, proPath.getSRSegment().d);
            //Reflexion computation
            double[] aRef = EvaluateAttenuationCnossos.evaluateAref(proPath, data);
            double[] aRetroDiff;
            //ABoundary computation
            double[] aBoundary;
            double[] aGlobalMeteoHom = new double[data.freq_lvl.size()];
            double[] aGlobalMeteoFav = new double[data.freq_lvl.size()];
            double[] deltaBodyScreen = new double[data.freq_lvl.size()];

            List<PointPath> ptList = proPath.getPointList();

            // todo get hRail from input data
            double hRail = 0.5;
            Coordinate src = ptList.get(0).coordinate;
            PointPath pDif = ptList.stream().filter(p -> p.type.equals(DIFH)).findFirst().orElse(null);

            if (pDif != null && pDif.alphaWall.size()>0) {
                if (pDif.bodyBarrier){

                    int n = 3;
                    Coordinate rcv = ptList.get(ptList.size() - 1).coordinate;
                    double[][] deltaGeo = new double[n+1][data.freq_lvl.size()];
                    double[][] deltaAbs = new double[n+1][data.freq_lvl.size()];
                    double[][] deltaDif = new double[n+1][data.freq_lvl.size()];
                    double[][] deltaRef = new double[n+1][data.freq_lvl.size()];
                    double[][] deltaRetroDifi = new double[n+1][data.freq_lvl.size()];
                    double[][] deltaRetroDif = new double[n+1][data.freq_lvl.size()];
                    double[] deltaL = new double[data.freq_lvl.size()];
                    Arrays.fill(deltaL,dbaToW(0.0));

                    double db = pDif.coordinate.x;
                    double hb = pDif.coordinate.y;
                    Coordinate B = new Coordinate(db,hb);

                    double Cref = 1;
                    double dr = rcv.x;
                    double h0 = ptList.get(0).altitude+hRail;
                    double hs = ptList.get(0).altitude+src.y-hRail;
                    double hr = ptList.get(ptList.size()-1).altitude + ptList.get(ptList.size()-1).coordinate.y-h0;
                    double[] r = new double[4];
                    if (db<5*hb) {
                        for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
                            if (pDif.alphaWall.get(idfreq)<0.8){

                                double dif0 =0 ;
                                double ch = 1.;
                                double lambda = 340.0 / data.freq_lvl.get(idfreq);
                                double hi = hs;
                                double cSecond = 1;

                                for (int i = 0; i <= n; i++) {
                                    double di = -2 * i * db;

                                    Coordinate si = new Coordinate(src.x+di, src.y);
                                    r[i] = sqrt(pow(di - (db + dr), 2) + pow(hi - hr, 2));
                                    deltaGeo[i][idfreq] =  20 * log10(r[0] / r[i]);
                                    double deltai = si.distance(B)+B.distance(rcv)-si.distance(rcv);

                                    double dif = 0;
                                    double testForm = (40/lambda)*cSecond*deltai;
                                    if (testForm>=-2) {
                                        dif = 10*ch*log10(3+testForm);
                                    }

                                    if (i==0){
                                        dif0=dif;
                                        deltaRetroDif[i][idfreq] = dif;
                                    }else{
                                        deltaDif[i][idfreq] = dif0-dif;
                                    }

                                    deltaAbs[i][idfreq] = 10 * i * log10(1 - pDif.alphaWall.get(idfreq));
                                    deltaRef[i][idfreq] = 10 * i * log10(Cref);

                                    double retroDif =0 ;
                                    Coordinate Pi = new Coordinate(-(2 * i -1)* db,hb);
                                    Coordinate RcvPrime = new Coordinate(dr,max(hr,hb*(db+dr-di)/(db-di)));
                                    deltai = -(si.distance(Pi)+Pi.distance(RcvPrime)-si.distance(RcvPrime));

                                    testForm = (40/lambda)*cSecond*deltai;
                                    if (testForm>=-2) {
                                        retroDif = 10*ch*log10(3+testForm);
                                    }

                                    if (i==0){
                                        deltaRetroDifi[i][idfreq] = 0;
                                    }else{
                                        deltaRetroDifi[i][idfreq] = retroDif;
                                    }


                                }
                                // Compute deltaRetroDif
                                deltaRetroDif[0][idfreq] = 0;
                                for (int i = 1; i <= n; i++) {
                                    double sumRetrodif = 0;
                                    for (int j = 1; j <= i; j++) {
                                        sumRetrodif = sumRetrodif + deltaRetroDifi[j][idfreq];
                                    }
                                    deltaRetroDif[i][idfreq] = - sumRetrodif;
                                }
                                // Compute deltaL
                                for (int i = 0; i <= n; i++) {
                                    deltaL[idfreq] = deltaL[idfreq] + dbaToW(deltaGeo[i][idfreq] + deltaDif[i][idfreq] + deltaAbs[i][idfreq] + deltaRef[i][idfreq] + deltaRetroDif[i][idfreq]);
                                }
                            }
                        }
                        deltaBodyScreen = wToDba(deltaL);
                    }
                }

            }

            // restore the Map relative propagation direction from the emission propagation relative to the sound source orientation
            // just swap the inverse boolean parameter
            // @see ComputeCnossosRays#computeOrientation
            Vector3D fieldVectorPropagation = Orientation.rotate(proPath.getSourceOrientation(),
                    Orientation.toVector(proPath.raySourceReceiverDirectivity), false);
            int roseIndex = getRoseIndex(Math.atan2(fieldVectorPropagation.getY(), fieldVectorPropagation.getX()));
            // Homogenous conditions
            if (data.getWindRose()[roseIndex] != 1) {
                proPath.setFavorable(false);

                aBoundary = EvaluateAttenuationCnossos.aBoundary(proPath, data);
                aRetroDiff = EvaluateAttenuationCnossos.deltaRetrodif(proPath, data);
                for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
                    aGlobalMeteoHom[idfreq] = -(aDiv[idfreq] + aAtm[idfreq] + aBoundary[idfreq] + aRef[idfreq] + aRetroDiff[idfreq] - deltaBodyScreen[idfreq]); // Eq. 2.5.6
                }
                //For testing purpose
                if(keepAbsorption) {
                    proPath.absorptionData.aBoundaryH = aBoundary.clone();
                    proPath.absorptionData.aGlobalH = aGlobalMeteoHom.clone();
                }
            }
            // Favorable conditions
            if (data.getWindRose()[roseIndex] != 0) {
                proPath.setFavorable(true);
                aBoundary = EvaluateAttenuationCnossos.aBoundary(proPath, data);
                aRetroDiff = EvaluateAttenuationCnossos.deltaRetrodif(proPath, data);
                for (int idfreq = 0; idfreq < data.freq_lvl.size(); idfreq++) {
                    aGlobalMeteoFav[idfreq] = -(aDiv[idfreq] + aAtm[idfreq] + aBoundary[idfreq]+ aRef[idfreq] + aRetroDiff[idfreq] -deltaBodyScreen[idfreq]); // Eq. 2.5.8
                }
                //For testing purpose
                if(keepAbsorption) {
                    proPath.absorptionData.aBoundaryF = aBoundary.clone();
                    proPath.absorptionData.aGlobalF = aGlobalMeteoFav.clone();
                }
            }

            //For testing purpose
            if(keepAbsorption) {
                proPath.keepAbsorption = true;
                proPath.absorptionData.aDiv = aDiv.clone();
                proPath.absorptionData.aAtm = aAtm.clone();
            }

            // Compute attenuation under the wind conditions using the ray direction
            double[] aGlobalMeteoRay = sumArrayWithPonderation(aGlobalMeteoFav, aGlobalMeteoHom, data.getWindRose()[roseIndex]);

            // Apply attenuation due to sound direction
            if(inputData != null && !inputData.isOmnidirectional((int)sourceId)) {
                Orientation directivityToPick = proPath.raySourceReceiverDirectivity;
                double[] attSource = inputData.getSourceAttenuation((int) sourceId,
                        frequencies, Math.toRadians(directivityToPick.yaw),
                        Math.toRadians(directivityToPick.pitch));
                if(keepAbsorption) {
                    proPath.absorptionData.aSource = attSource;
                }
                aGlobalMeteoRay = sumArray(aGlobalMeteoRay, attSource);
            }

            // For line source, take account of li coefficient
            if(sourceLi > 1.0) {
                for (int i = 0; i < aGlobalMeteoRay.length; i++) {
                    aGlobalMeteoRay[i] = wToDba(dbaToW(aGlobalMeteoRay[i]) * sourceLi);
                }
            }
            // Keep global attenuation
            if(keepAbsorption) {
                proPath.absorptionData.aGlobal = aGlobalMeteoRay.clone();
            }

            if (propagationAttenuationSpectrum != null) {
                propagationAttenuationSpectrum = sumDbArray(aGlobalMeteoRay, propagationAttenuationSpectrum);
            } else {
                propagationAttenuationSpectrum = aGlobalMeteoRay;
            }
        }
        if (propagationAttenuationSpectrum != null) {
            return propagationAttenuationSpectrum;
        } else {
            return new double[0];
        }
    }

    @Override
    public IComputeRaysOut subProcess() {
        return new ThreadRaysOut(this, genericMeteoData);
    }

    public List<VerticeSL> getVerticesSoundLevel() {
        return new ArrayList<>(receiversAttenuationLevels);
    }

    public List<PropagationPath> getPropagationPaths() {
        return propagationPaths;
    }

    public void clearPropagationPaths() { this.propagationPaths.clear();}

    public void appendReflexionPath(long added) {
        nb_reflexion_path.addAndGet(added);
    }

    public void appendDiffractionPath(long added) {
        nb_diffraction_path.addAndGet(added);
    }

    public void appendImageReceiver(long added) {
        nb_image_receiver.addAndGet(added);
    }

    public void appendSourceCount(long srcCount) {
        nb_couple_receiver_src.addAndGet(srcCount);
    }

    public void appendFreeFieldTestCount(long freeFieldTestCount) {
        nb_obstr_test.addAndGet(freeFieldTestCount);
    }

    public synchronized void log(String str) {

    }

    /**
     * Increment cell computed counter by 1
     */
    public synchronized void appendCellComputed() {
        cellComputed.addAndGet(1);
    }

    public synchronized long getCellComputed() {
        return cellComputed.get();
    }

    /**
     * Noise level or attenuation level for each source/receiver
     */
    public static class VerticeSL {
        public final long sourceId;
        public final long receiverId;
        public final double[] value;

        /**
         *
         * @param receiverId Receiver identifier
         * @param sourceId Source identifier
         * @param value Noise level in dB
         */
        public VerticeSL(long receiverId, long sourceId, double[] value) {
            this.sourceId = sourceId;
            this.receiverId = receiverId;
            this.value = value;
        }
    }

    public static class ThreadRaysOut implements IComputeRaysOut {
        public ComputeRaysOutAttenuation multiThreadParent;
        public List<VerticeSL> receiverAttenuationLevels = new ArrayList<>();
        public List<PropagationPath> propagationPaths = new ArrayList<PropagationPath>();
        public PropagationProcessPathData propagationProcessPathData;
        public boolean keepRays = false;

        public ThreadRaysOut(ComputeRaysOutAttenuation multiThreadParent, PropagationProcessPathData propagationProcessPathData) {
            this.multiThreadParent = multiThreadParent;
            this.keepRays = multiThreadParent.keepRays;
            this.propagationProcessPathData = propagationProcessPathData;
        }

        @Override
        public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            double[] aGlobalMeteo = multiThreadParent.computeAttenuation(propagationProcessPathData, sourceId, sourceLi, receiverId, propagationPath);
            multiThreadParent.rayCount.addAndGet(propagationPath.size());
            if(keepRays) {
                if(multiThreadParent.inputData != null && sourceId < multiThreadParent.inputData.sourcesPk.size() &&
                      receiverId < multiThreadParent.inputData.receiversPk.size()) {
                    for(PropagationPath path : propagationPath) {
                        // Copy path content in order to keep original ids for other method calls
                        PropagationPath pathPk = new PropagationPath(path);
                        pathPk.setIdReceiver(multiThreadParent.inputData.receiversPk.get((int)receiverId).intValue());
                        pathPk.setIdSource(multiThreadParent.inputData.sourcesPk.get((int)sourceId).intValue());
                        pathPk.setSourceOrientation(path.getSourceOrientation());
                        pathPk.setGs(path.getGs());
                        propagationPaths.add(pathPk);
                    }
                } else {
                    propagationPaths.addAll(propagationPath);
                }
            }
            if (aGlobalMeteo != null) {
                receiverAttenuationLevels.add(new VerticeSL(receiverId, sourceId, aGlobalMeteo));
                return aGlobalMeteo;
            } else {
                return new double[0];
            }
        }

        protected void pushResult(long receiverId, long sourceId, double[] level) {
            multiThreadParent.receiversAttenuationLevels.add(new VerticeSL(receiverId, sourceId, level));
        }

        @Override
        public void finalizeReceiver(final long receiverId) {
            if(keepRays && !propagationPaths.isEmpty()) {
                multiThreadParent.propagationPaths.addAll(propagationPaths);
                propagationPaths.clear();
            }
            long receiverPK = receiverId;
            if(multiThreadParent.inputData != null) {
                if(receiverId < multiThreadParent.inputData.receiversPk.size()) {
                    receiverPK = multiThreadParent.inputData.receiversPk.get((int)receiverId);
                }
            }
            multiThreadParent.finalizeReceiver(receiverId);
            if(multiThreadParent.receiversAttenuationLevels != null) {
                // Push merged sources into multi-thread parent
                // Merge levels for each receiver for lines sources
                Map<Long, double[]> levelsPerSourceLines = new HashMap<>();
                for (VerticeSL lvl : receiverAttenuationLevels) {
                    if (!levelsPerSourceLines.containsKey(lvl.sourceId)) {
                        levelsPerSourceLines.put(lvl.sourceId, lvl.value);
                    } else {
                        // merge
                        levelsPerSourceLines.put(lvl.sourceId, sumDbArray(levelsPerSourceLines.get(lvl.sourceId),
                                lvl.value));
                    }
                }
                long sourcePK;
                for (Map.Entry<Long, double[]> entry : levelsPerSourceLines.entrySet()) {
                    final long sourceId = entry.getKey();
                    sourcePK = sourceId;
                    if(multiThreadParent.inputData != null) {
                        // Retrieve original identifier
                        if(entry.getKey() < multiThreadParent.inputData.sourcesPk.size()) {
                            sourcePK = multiThreadParent.inputData.sourcesPk.get((int)sourceId);
                        }
                    }
                    pushResult(receiverPK, sourcePK, entry.getValue());
                }
            }
            receiverAttenuationLevels.clear();
        }

        @Override
        public IComputeRaysOut subProcess() {
            return multiThreadParent.subProcess();
        }
    }
}
