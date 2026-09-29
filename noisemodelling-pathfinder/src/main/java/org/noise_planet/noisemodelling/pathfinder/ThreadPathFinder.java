/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.pathfinder;

import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.noise_planet.noisemodelling.pathfinder.PathFinder.LOGGER;

/**
 * A Thread class to evaluate all receivers cut planes.
 * Return true if the computation is done without issues
 */
public final class ThreadPathFinder implements Callable<Boolean> {
    ConcurrentLinkedDeque<Integer> receivers;
    PathFinder propagationProcess;
    ProgressVisitor visitor;
    CutPlaneVisitor dataOut;
    Scene data;


    /**
     * Create the ThreadPathFinder constructor
     * @param receivers Receivers to process, this queue will be consumed by this instance
     * @param propagationProcess Propagation process instance
     * @param visitor Keep track of the computation progress and cancellation
     * @param dataOut Output data instance
     * @param data Input data
     */
    public ThreadPathFinder(ConcurrentLinkedDeque<Integer> receivers, PathFinder propagationProcess,
                            ProgressVisitor visitor, CutPlaneVisitor dataOut,
                            Scene data) {
        this.receivers = receivers;
        this.propagationProcess = propagationProcess;
        this.visitor = visitor;
        this.dataOut = dataOut;
        this.data = data;
    }

    /**
     * Executes the computation of ray paths for each receiver in the specified range.
     */
    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Batch thread #{} started", Thread.currentThread().threadId());
        try {
            Integer idReceiver;
            while((idReceiver = receivers.poll()) != null) {
                if (visitor != null) {
                    if (visitor.isCanceled()) {
                        break;
                    }
                }
                long receiverPk = idReceiver;
                if(idReceiver < data.receiversPk.size()) {
                    receiverPk = data.receiversPk.get(idReceiver);
                }
                PathFinder.ReceiverPointInfo rcv = new PathFinder.ReceiverPointInfo(idReceiver, receiverPk, data.receivers.get(idReceiver));


                propagationProcess.computeRaysAtPosition(rcv, dataOut, visitor);

                if (visitor != null) {
                    visitor.endStep();
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
            if (visitor != null) {
                visitor.cancel();
            }
            throw ex;
        }
        LOGGER.info("Batch thread #{} completed", Thread.currentThread().threadId());
        return true;
    }
}