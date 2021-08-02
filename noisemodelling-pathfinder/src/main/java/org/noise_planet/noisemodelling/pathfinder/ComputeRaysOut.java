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
package org.noise_planet.noisemodelling.pathfinder;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Way to store data computed by threads.
 * Multiple threads use one instance.
 * This class must be thread safe
 * Store only propagation rays
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class ComputeRaysOut implements IComputeRaysOut {
    public List<PropagationPath> propagationPaths = Collections.synchronizedList(new ArrayList<PropagationPath>());
    public PropagationProcessData inputData;

    public ComputeRaysOut(boolean keepRays, PropagationProcessData inputData) {
        this.keepRays = keepRays;
        this.inputData = inputData;
    }

    public ComputeRaysOut(boolean keepRays) {
        this.keepRays = keepRays;
    }

    public boolean keepRays = true;
    public AtomicLong rayCount = new AtomicLong();

    @Override
    public void finalizeReceiver(long receiverId) {

    }

    public PropagationProcessData getInputData() {
        return inputData;
    }

    @Override
    public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        rayCount.addAndGet(propagationPath.size());
        if (keepRays) {
            propagationPaths.addAll(propagationPath);
        }
        return new double[0];
    }

    @Override
    public IComputeRaysOut subProcess() {
        return new ThreadRaysOut(this);
    }

    public List<PropagationPath> getPropagationPaths() {
        return propagationPaths;
    }

    public void clearPropagationPaths() {
        this.propagationPaths.clear();
    }

    public static class ThreadRaysOut implements IComputeRaysOut {
        protected ComputeRaysOut multiThreadParent;
        public List<PropagationPath> propagationPaths = new ArrayList<PropagationPath>();

        public ThreadRaysOut(ComputeRaysOut multiThreadParent) {
            this.multiThreadParent = multiThreadParent;
        }

        @Override
        public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            multiThreadParent.rayCount.addAndGet(propagationPath.size());
            if (multiThreadParent.keepRays) {
                if (multiThreadParent.inputData != null && sourceId < multiThreadParent.inputData.sourcesPk.size() &&
                        receiverId < multiThreadParent.inputData.receiversPk.size()) {
                    for (PropagationPath path : propagationPath) {
                        // Copy path content in order to keep original ids for other method calls
                        PropagationPath pathPk = new PropagationPath(path.isFavorable(), path.getPointList(),
                                path.getSegmentList(), path.getSRList());
                        pathPk.setIdReceiver(multiThreadParent.inputData.receiversPk.get((int) receiverId).intValue());
                        pathPk.setIdSource(multiThreadParent.inputData.sourcesPk.get((int) sourceId).intValue());
                        propagationPaths.add(pathPk);
                    }
                } else {
                    propagationPaths.addAll(propagationPath);
                }
            }
            return new double[0];
        }

    @Override
    public void finalizeReceiver(final long receiverId) {
        if (multiThreadParent.keepRays && !propagationPaths.isEmpty()) {
            multiThreadParent.propagationPaths.addAll(propagationPaths);
            propagationPaths.clear();
        }
        long receiverPK = receiverId;
        if (multiThreadParent.inputData != null) {
            if (receiverId < multiThreadParent.inputData.receiversPk.size()) {
                receiverPK = multiThreadParent.inputData.receiversPk.get((int) receiverId);
            }
        }
        multiThreadParent.finalizeReceiver(receiverId);

    }

    @Override
    public IComputeRaysOut subProcess() {
        return multiThreadParent.subProcess();
    }
}
}
