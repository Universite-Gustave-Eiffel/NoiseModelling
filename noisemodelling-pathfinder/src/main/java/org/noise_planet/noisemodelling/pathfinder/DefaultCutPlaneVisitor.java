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
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Way to store data computed by threads.
 * Multiple threads use one instance.
 * This class must be thread safe
 * Store only vertical cut planes
 * @author Nicolas Fortin
 * @author Pierre Aumond
 */
public class DefaultCutPlaneVisitor implements CutPlaneVisitor, CutPlaneVisitorFactory {
    /** This list is thread safe so can be used in a multi-thread environment */
    public ConcurrentLinkedDeque<CutProfile> cutProfiles = new ConcurrentLinkedDeque<>();
    public Scene inputData;

    public boolean keepCutPlanes = true;
    public AtomicLong pathCount = new AtomicLong();

    public DefaultCutPlaneVisitor(boolean keepCutPlanes, Scene inputData) {
        this.keepCutPlanes = keepCutPlanes;
        this.inputData = inputData;
    }

    public DefaultCutPlaneVisitor(boolean keepCutPlanes) {
        this.keepCutPlanes = keepCutPlanes;
    }

    /**
     * No more propagation paths will be pushed for this receiver identifier
     *
     * @param receiver
     */
    @Override
    public void finalizeReceiver(PathFinder.ReceiverPointInfo receiver) {

    }

    public Scene getInputData() {
        return inputData;
    }


    @Override
    public PathSearchStrategy onNewCutPlane(CutProfile cutProfile) {
        pathCount.addAndGet(1);
        if (keepCutPlanes) {
            cutProfiles.add(cutProfile);
        }
        return PathSearchStrategy.CONTINUE;
    }

    @Override
    public void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList, AtomicInteger cutProfileCount) {

    }

    /**
     *
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public CutPlaneVisitor subProcess(ProgressVisitor visitor) {
        return this;
    }

    public Collection<CutProfile> getCutProfiles() {
        return cutProfiles;
    }
}
