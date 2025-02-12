/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation;


import org.h2gis.api.ProgressVisitor;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;


import java.util.*;
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
public class AttenuationComputeOutput implements CutPlaneVisitorFactory {
    public ConcurrentLinkedDeque<ReceiverNoiseLevel> receiversAttenuationLevels = new ConcurrentLinkedDeque<>();
    public Deque<CnossosPath> pathParameters = new ConcurrentLinkedDeque<>();
    public AtomicInteger propagationPathsSize = new AtomicInteger(0);
    public boolean exportPaths;
    public boolean exportAttenuationMatrix;
    public AtomicLong cnossosPathCount = new AtomicLong();
    public AtomicLong nb_couple_receiver_src = new AtomicLong();
    public AtomicLong nb_obstr_test = new AtomicLong();
    public AtomicLong nb_image_receiver = new AtomicLong();
    public AtomicLong nb_reflexion_path = new AtomicLong();
    public AtomicLong nb_diffraction_path = new AtomicLong();
    public AtomicInteger cellComputed = new AtomicInteger();
    public SceneWithAttenuation scene;

    public AttenuationComputeOutput(boolean exportPaths, SceneWithAttenuation scene) {
        this.exportPaths = exportPaths;
        this.exportAttenuationMatrix = false;
        this.scene = scene;
    }

    public AttenuationComputeOutput(boolean exportPaths, boolean exportAttenuationMatrix, SceneWithAttenuation scene) {
        this.exportPaths = exportPaths;
        this.exportAttenuationMatrix = exportAttenuationMatrix;
        this.scene = scene;
    }

    public Scene getScene() {
        return scene;
    }

    /**
     *
     * @return an instance of the interface IComputePathsOut
     */
    @Override
    public CutPlaneVisitor subProcess(ProgressVisitor visitor) {
        return new AttenuationVisitor(this);
    }

    /**
     *
     * @return a list of SourceReceiverAttenuation
     */
    public List<ReceiverNoiseLevel> getVerticesSoundLevel() {
        return new ArrayList<>(receiversAttenuationLevels);
    }

    /**
     *
     * @return a list of Path propagation
     */
    public List<CnossosPath> getPropagationPaths() {
        return new ArrayList<>(pathParameters);
    }

    public void clearPropagationPaths() {
        pathParameters.clear();
        propagationPathsSize.set(0);
    }

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
}
