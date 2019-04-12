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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private List<ComputeRaysOut.verticeSL> receiverAttenuationLevels = Collections.synchronizedList(new ArrayList<>());
    private PropagationProcessPathData pathData;

    public ComputeRaysOut(boolean keepRays, PropagationProcessPathData pathData) {
        this.keepRays = keepRays;
        this.pathData = pathData;
    }

    public boolean keepRays = true;
    public AtomicLong rayCount = new AtomicLong();
    private AtomicLong nb_couple_receiver_src = new AtomicLong();
    private AtomicLong nb_obstr_test = new AtomicLong();
    private AtomicLong nb_image_receiver = new AtomicLong();
    private AtomicLong nb_reflexion_path = new AtomicLong();
    private AtomicLong nb_diffraction_path = new AtomicLong();
    private AtomicInteger cellComputed = new AtomicInteger();

    private List<PropagationPath> propagationPaths = Collections.synchronizedList(new ArrayList<PropagationPath>());

    @Override
    public double addPropagationPaths(int sourceId, int receiverId, List<PropagationPath> propagationPath) {
        rayCount.addAndGet(1);
        if(keepRays) {
            propagationPaths.addAll(propagationPath);
        }
        if(pathData != null) {
            // Compute receiver/source attenuation
            EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
            double[] aGlobalMeteo = null;
            for (PropagationPath propath : propagationPath) {
                List<PropagationPath.PointPath> ptList = propath.getPointList();
                double angleRad = Angle.angle(ptList.get(0).coordinate, ptList.get(ptList.size() - 1).coordinate);
                double rad2rose = (-angleRad + Math.PI / 2);
                int roseindex = (int) Math.round(rad2rose / (2 * Math.PI / pathData.getWindRose().length));

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
                    aGlobalMeteo = ComputeRays.sumArray(aGlobalMeteoRay, aGlobalMeteo);
                } else {
                    aGlobalMeteo = aGlobalMeteoRay;
                }
            }
            if (aGlobalMeteo != null) {
                receiverAttenuationLevels.add(new ComputeRaysOut.verticeSL(receiverId, sourceId, aGlobalMeteo));
                double globalValue = 0;
                for (double att : aGlobalMeteo) {
                    globalValue += Math.pow(10, att / 10.0);
                }
                return 10 * Math.log10(globalValue);
            } else {
                return Double.NaN;
            }
        } else {
            return Double.NaN;
        }
    }

    @Override
    public IComputeRaysOut subProcess(int receiverStart, int receiverEnd) {
        return this;
    }

    public List<ComputeRaysOut.verticeSL> getVerticesSoundLevel() {
        return receiverAttenuationLevels;
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

    public static final class verticeSL {
        public final int sourceId;
        public final int receiverId;
        public final double[] value;

        verticeSL(int receiverId, int sourceId, double[] value) {
            this.sourceId = sourceId;
            this.receiverId = receiverId;
            this.value = value;
        }
    }
}
