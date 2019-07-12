package org.noise_planet.nmtutorial01;


import org.noise_planet.noisemodelling.propagation.ComputeRays;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.GeoJSONDocument;
import org.noise_planet.noisemodelling.propagation.IComputeRaysOut;
import org.noise_planet.noisemodelling.propagation.PropagationPath;
import org.noise_planet.noisemodelling.propagation.PropagationProcessData;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Collect path computed by ComputeRays and store it into provided queue (with consecutive receiverId)
 */
class PropagationPathStorage extends ComputeRaysOut {
    // Thread safe queue object
    protected TrafficPropagationProcessData inputData;
    ConcurrentLinkedDeque<PointToPointPaths> pathQueue;

    PropagationPathStorage(PropagationProcessData inputData, PropagationProcessPathData pathData, ConcurrentLinkedDeque<PointToPointPaths> pathQueue) {
        super(false, pathData, inputData);
        this.inputData = (TrafficPropagationProcessData)inputData;
        this.pathQueue = pathQueue;
    }

    @Override
    public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        return new double[0];
    }

    @Override
    public double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
        double[] attenuation = super.computeAttenuation(pathData, sourceId, sourceLi, receiverId, propagationPath);
        double[] soundLevel = ComputeRays.wToDba(ComputeRays.multArray(inputData.wjSourcesD.get((int)sourceId), ComputeRays.dbaToW(attenuation)));
        return soundLevel;
    }

    @Override
    public void finalizeReceiver(long l) {

    }

    @Override
    public IComputeRaysOut subProcess(int i, int i1) {
        return new PropagationPathStorageThread(this);
    }

    static class PropagationPathStorageThread implements IComputeRaysOut {
        // In order to keep consecutive receivers into the deque an intermediate list is built for each thread
        private List<PointToPointPaths> receiverPaths = new ArrayList<>();
        private PropagationPathStorage propagationPathStorage;

        PropagationPathStorageThread(PropagationPathStorage propagationPathStorage) {
            this.propagationPathStorage = propagationPathStorage;
        }

        @Override
        public double[] addPropagationPaths(long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            PointToPointPaths paths = new PointToPointPaths();
            paths.li = sourceLi;
            paths.receiverId = (propagationPathStorage.inputData.receiversPk.get((int) receiverId).intValue());
            paths.sourceId = propagationPathStorage.inputData.sourcesPk.get((int) sourceId).intValue();
            paths.propagationPathList = new ArrayList<>(propagationPath.size());
            for (PropagationPath path : propagationPath) {
                // Copy path content in order to keep original ids for other method calls
                PropagationPath pathPk = new PropagationPath(path.isFavorable(), path.getPointList(),
                        path.getSegmentList(), path.getSRList());
                pathPk.setIdReceiver((int)paths.receiverId);
                pathPk.setIdSource((int)paths.sourceId);
                paths.propagationPathList.add(pathPk);
                receiverPaths.add(paths);
            }
            double[] aGlobalMeteo = propagationPathStorage.computeAttenuation(propagationPathStorage.genericMeteoData, sourceId, sourceLi, receiverId, propagationPath);
            if (aGlobalMeteo != null) {
                return aGlobalMeteo;
            } else {
                return new double[0];
            }
        }

        @Override
        public void finalizeReceiver(long receiverId) {
            //propagationPathStorage.pathQueue.addAll(receiverPaths);
            receiverPaths.clear();
        }

        @Override
        public IComputeRaysOut subProcess(int receiverStart, int receiverEnd) {
            return null;
        }
    }
}
