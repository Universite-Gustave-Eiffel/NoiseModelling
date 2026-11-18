/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.propagation.cnossos;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.AttenuationComputeOutput;
import org.noise_planet.noisemodelling.propagation.AttenuationOutput;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Favourable reflection on a low building behind the receiver. The source is 200 m from the receiver and the
 * reflecting facade 50 m behind the receiver, so the unfolded path is 300 m long. In favourable conditions the
 * reflection point is constructed with curved lines (Directive 2002/49/EC, Annex II): the curved ray passes
 * about 3.9 m above the ground at the facade. A facade of 3 m cannot reflect it; a facade of 6 m can.
 */
class CurvedReflectionPathTest {

    private static int countReflectionPaths(double buildingHeight, boolean favourable) {
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.addBuilding(new Coordinate[]{
                new Coordinate(250, -50, buildingHeight),
                new Coordinate(260, -50, buildingHeight),
                new Coordinate(260, 50, buildingHeight),
                new Coordinate(250, 50, buildingHeight)
        });
        profileBuilder.finishFeeding();

        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);
        scene.addSource(new GeometryFactory().createPoint(new Coordinate(0, 0, 0.05)));
        scene.addReceiver(new Coordinate(200, 0, 1.5));
        scene.defaultGroundAttenuation = 0.0;
        scene.reflexionOrder = 1;
        scene.setComputeHorizontalDiffraction(false);
        scene.defaultCnossosParameters = new AttenuationParameters();

        AttenuationComputeOutput propDataOut = new AttenuationComputeOutput(true, false, scene);
        PathFinder computeRays = new PathFinder(scene);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        int count = 0;
        for (AttenuationOutput output : propDataOut.getAttenuationOutputs()) {
            CnossosAttenuationOutput cnossosOutput = (CnossosAttenuationOutput) output;
            if (cnossosOutput.getCutProfile().getProfileType() == CutProfile.PROFILE_TYPE.REFLECTION
                    && cnossosOutput.propagationPath.isFavourable() == favourable) {
                count++;
            }
        }
        return count;
    }

    @Test
    void favourableRayPassesOverLowFacadeBehindReceiver() {
        assertEquals(1, countReflectionPaths(3.0, false));
        assertEquals(0, countReflectionPaths(3.0, true));
    }

    @Test
    void favourableRayReflectsOnHighFacadeBehindReceiver() {
        assertEquals(1, countReflectionPaths(6.0, false));
        assertEquals(1, countReflectionPaths(6.0, true));
    }
}
