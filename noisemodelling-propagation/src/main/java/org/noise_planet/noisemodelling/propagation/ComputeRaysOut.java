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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Way to store data computed by threads.
 * Multiple threads use one instance.
 * This class must be thread safe
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class ComputeRaysOut implements IComputeRaysOut {
    public ConcurrentLinkedDeque<VerticeSL> receiversAttenuationLevels = new ConcurrentLinkedDeque<>();
    public List<PropagationPath> propagationPaths = Collections.synchronizedList(new ArrayList<PropagationPath>());

    public PropagationProcessPathData genericMeteoData;
    public PropagationProcessData inputData;

    public ComputeRaysOut(boolean keepRays, PropagationProcessPathData pathData, PropagationProcessData inputData) {
        this.keepRays = keepRays;
        this.genericMeteoData = pathData;
        this.inputData = inputData;
    }

    public ComputeRaysOut(boolean keepRays, PropagationProcessPathData pathData) {
        this.keepRays = keepRays;
        this.genericMeteoData = pathData;
    }

    public boolean keepRays = true;
    public AtomicLong rayCount = new AtomicLong();
    public AtomicLong nb_couple_receiver_src = new AtomicLong();
    public AtomicLong nb_obstr_test = new AtomicLong();
    public AtomicLong nb_image_receiver = new AtomicLong();
    public AtomicLong nb_reflexion_path = new AtomicLong();
    public AtomicLong nb_diffraction_path = new AtomicLong();
    public AtomicInteger cellComputed = new AtomicInteger();
    private static final double angle_section = (2 * Math.PI) / PropagationProcessPathData.DEFAULT_WIND_ROSE.length;


    public static int getRoseIndex(Coordinate receiver, Coordinate source) {
        // Angle from cos -1 sin 0
        double angleRad = -(Angle.angle(receiver, source) - Math.PI);
        // Offset angle by PI / 2 (North),
        // the north slice ranges is [PI / 2 + angle_section / 2; PI / 2 - angle_section / 2]
        angleRad -= (Math.PI / 2 - angle_section / 2);
        // Fix out of bounds angle 0-2Pi
        if(angleRad < 0) {
            angleRad += Math.PI * 2;
        }
        // The north slice is the last array index not the first one
        // Ex for slice width of 20°:
        //      - The first column 20° contain winds between 10 to 30 °
        //      - The last column 360° contains winds between 350° to 360° and 0 to 10°
        int index = (int)(angleRad / angle_section) - 1;
        if(index < 0) {
            index = PropagationProcessPathData.DEFAULT_WIND_ROSE.length - 1;
        }
        return index;
    }

    @Override
    public void finalizeReceiver(long receiverId) {

    }

    public PropagationProcessData getInputData() {
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

    public double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        if(pathData != null) {
            // Compute receiver/source attenuation
            EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
            double[] aGlobalMeteo = null;
            for (PropagationPath propath : propagationPath) {
                List<PointPath> ptList = propath.getPointList();
                int roseindex = getRoseIndex(ptList.get(0).coordinate, ptList.get(ptList.size() - 1).coordinate);

                // Compute homogeneous conditions attenuation
                propath.setFavorable(false);
                evaluateAttenuationCnossos.evaluate(propath, pathData);
                double[] aGlobalMeteoHom = evaluateAttenuationCnossos.getaGlobal();

                // Compute favorable conditions attenuation
                propath.setFavorable(true);
                evaluateAttenuationCnossos.evaluate(propath, pathData);
                double[] aGlobalMeteoFav = evaluateAttenuationCnossos.getaGlobal();

                // Compute attenuation under the wind conditions using the ray direction
                double[] aGlobalMeteoRay = ComputeRays.sumArrayWithPonderation(aGlobalMeteoFav, aGlobalMeteoHom, pathData.getWindRose()[roseindex]);

                if (aGlobalMeteo != null) {
                    aGlobalMeteo = ComputeRays.sumDbArray(aGlobalMeteoRay, aGlobalMeteo);
                } else {
                    aGlobalMeteo = aGlobalMeteoRay;
                }
            }
            if (aGlobalMeteo != null) {
                // For line source, take account of li coefficient
                if(sourceLi > 1.0) {
                    for (int i = 0; i < aGlobalMeteo.length; i++) {
                        aGlobalMeteo[i] = ComputeRays.wToDba(ComputeRays.dbaToW(aGlobalMeteo[i]) * sourceLi);
                    }
                }
                return aGlobalMeteo;
            } else {
                return new double[0];
            }
        } else {
            return new double[0];
        }
    }

    @Override
    public IComputeRaysOut subProcess(int receiverStart, int receiverEnd) {
        return new ThreadRaysOut(this);
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

        public VerticeSL(long receiverId, long sourceId, double[] value) {
            this.sourceId = sourceId;
            this.receiverId = receiverId;
            this.value = value;
        }
    }

    public static class ThreadRaysOut implements IComputeRaysOut {
        protected ComputeRaysOut multiThreadParent;
        protected List<VerticeSL> receiverAttenuationLevels = new ArrayList<>();

        public ThreadRaysOut(ComputeRaysOut multiThreadParent) {
            this.multiThreadParent = multiThreadParent;
        }

        @Override
        public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            double[] aGlobalMeteo = multiThreadParent.computeAttenuation(multiThreadParent.genericMeteoData, sourceId, sourceLi, receiverId, propagationPath);
            multiThreadParent.rayCount.addAndGet(propagationPath.size());
            if(multiThreadParent.keepRays) {
                if(multiThreadParent.inputData != null && sourceId < multiThreadParent.inputData.sourcesPk.size() &&
                      receiverId < multiThreadParent.inputData.receiversPk.size()) {
                    for(PropagationPath path : propagationPath) {
                        // Copy path content in order to keep original ids for other method calls
                        PropagationPath pathPk = new PropagationPath(path.isFavorable(), path.getPointList(),
                                path.getSegmentList(), path.getSRList());
                        pathPk.idReceiver = multiThreadParent.inputData.receiversPk.get((int)receiverId).intValue();
                        pathPk.idSource = multiThreadParent.inputData.sourcesPk.get((int)sourceId).intValue();
                        multiThreadParent.propagationPaths.add(pathPk);
                    }
                } else {
                    multiThreadParent.propagationPaths.addAll(propagationPath);
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
                        levelsPerSourceLines.put(lvl.sourceId, ComputeRays.sumDbArray(levelsPerSourceLines.get(lvl.sourceId),
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
        public IComputeRaysOut subProcess(int receiverStart, int receiverEnd) {
            return multiThreadParent.subProcess(receiverStart, receiverEnd);
        }
    }
}
