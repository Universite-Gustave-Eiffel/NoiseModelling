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
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

 /**
 * Instead of feeding a list and returning all vertical cut planes.
 * A visitor instance that implement this interface can skip planes and intervene in the search of cut planes.
 */
public interface CutPlaneVisitor {

    /**
     * A new vertical profile between a receiver and a source has been found
     *
     * @param cutProfile vertical profile
     * @return Will skip or not the next processing depending on this value.
     */
    PathSearchStrategy onNewCutPlane(CutProfile cutProfile);

    /**
     * Called before looking for vertical cut planes between the receiver and the sources.
     *
     * @param receiver        Receiver information
     * @param sourceList      All sources in the range of this receiver sorted by the distance from the receiver
     * @param cutProfileCount
     */
    void startReceiver(PathFinder.ReceiverPointInfo receiver, Collection<PathFinder.SourcePointInfo> sourceList, AtomicInteger cutProfileCount);

    enum PathSearchStrategy {
        /**
         * Continue looking for vertical cut planes
         */
        CONTINUE,
        /**
         * Skip remaining potential vertical planes for this source point
         */
        SKIP_SOURCE,
        /**
         * Process remaining potential vertical planes for this source but ignore the farthest sources,
         * then proceed to the next receivers
         */
        PROCESS_SOURCE_BUT_SKIP_RECEIVER,
        /**
         * Skip remaining potential vertical planes for this source point and
         * ignore then remaining farthest sources, proceed directly to the next receiver
         */
        SKIP_RECEIVER
    }

    /**
     * No more propagation paths will be pushed for this receiver identifier
     *
     * @param receiver
     */
    void finalizeReceiver(PathFinder.ReceiverPointInfo receiver);

}
