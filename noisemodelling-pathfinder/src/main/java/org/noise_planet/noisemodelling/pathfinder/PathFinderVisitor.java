/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.pathfinder;


import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.pathfinder.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Way to store data computed by threads.
 * Multiple threads use one instance.
 * This class must be thread safe
 * Store only propagation rays
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class PathFinderVisitor implements IComputePathsOut {
    public List<CnossosPath> pathParameters = Collections.synchronizedList(new ArrayList<>());
    public Scene inputData;

    public PathFinderVisitor(boolean keepRays, Scene inputData) {
        this.keepRays = keepRays;
        this.inputData = inputData;
    }

    public PathFinderVisitor(boolean keepRays) {
        this.keepRays = keepRays;
    }

    public boolean keepRays = true;
    public AtomicLong rayCount = new AtomicLong();

    /**
     * No more propagation paths will be pushed for this receiver identifier
     * @param receiverId
     */
    @Override
    public void finalizeReceiver(long receiverId) {

    }

    public Scene getInputData() {
        return inputData;
    }


    /**
     * Get propagation path result
     * @param sourceId Source identifier
     * @param sourceLi Source power per meter coefficient
     * @param path Propagation path result
     */
    @Override
    public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<CnossosPath> path) {
        rayCount.addAndGet(path.size());
        if (keepRays) {
            pathParameters.addAll(path);
        }
        return new double[0];
    }

    /**
     *
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public IComputePathsOut subProcess() {
        return new ThreadPathsOut(this);
    }

    public List<CnossosPath> getPropagationPaths() {
        return pathParameters;
    }

    /*public void clearPropagationPaths() {
        this.propagationPaths.clear();
    }*/

    public static class ThreadPathsOut implements IComputePathsOut {
        protected PathFinderVisitor multiThreadParent;
        public List<CnossosPath> pathParameters = new ArrayList<>();

        public ThreadPathsOut(PathFinderVisitor multiThreadParent) {
            this.multiThreadParent = multiThreadParent;
        }

        /**
         * Get propagation path result
         * @param sourceId Source identifier
         * @param sourceLi Source power per meter coefficient
         * @param path path result
         */
        @Override
        public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<CnossosPath> path) {
            multiThreadParent.rayCount.addAndGet(path.size());
            if (multiThreadParent.keepRays) {
                if (multiThreadParent.inputData != null && sourceId < multiThreadParent.inputData.sourcesPk.size() &&
                        receiverId < multiThreadParent.inputData.receiversPk.size()) {
                    for (CnossosPath pathParameter : path) {
                        // Copy path content in order to keep original ids for other method calls
                        //CnossosPathParameters pathParametersPk = new CnossosPathParameters(pathParameter);
                        pathParameter.setIdReceiver(multiThreadParent.inputData.receiversPk.get((int) receiverId).intValue());
                        pathParameter.setIdSource(multiThreadParent.inputData.sourcesPk.get((int) sourceId).intValue());
                        //pathParametersPk.init(multiThreadParent.inputData.freq_lvl.size());
                        pathParameters.add(pathParameter);
                    }
                } else {
                    pathParameters.addAll(path);
                }
            }
            return new double[0];
        }

        /**
         * No more propagation paths will be pushed for this receiver identifier
         * @param receiverId
         */
    @Override
    public void finalizeReceiver(final long receiverId) {
        if (multiThreadParent.keepRays && !pathParameters.isEmpty()) {
            multiThreadParent.pathParameters.addAll(pathParameters);
            pathParameters.clear();
        }
        long receiverPK = receiverId;
        if (multiThreadParent.inputData != null) {
            if (receiverId < multiThreadParent.inputData.receiversPk.size()) {
                receiverPK = multiThreadParent.inputData.receiversPk.get((int) receiverId);
            }
        }
        multiThreadParent.finalizeReceiver(receiverId);

    }

        /**
         *
         * @return an instance of the interface IComputePathsOut
         */

    @Override
    public IComputePathsOut subProcess() {
        return multiThreadParent.subProcess();
    }
}

}
