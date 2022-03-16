package org.noise_planet.noisemodelling.propagation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;
import org.noise_planet.noisemodelling.pathfinder.PointPath;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;

import java.io.IOException;

import static org.junit.Assert.assertFalse;

public class RayAttenuationTest {



    @Test
    public void testPropagationPathReceiverUnder() throws IOException {
        JsonMapper.Builder builder = JsonMapper.builder();
        JsonMapper mapper = builder.build();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        PropagationPath path = mapper.readValue(
                RayAttenuationTest.class.getResourceAsStream("special_ray.json"), PropagationPath.class);
        PropagationProcessPathData propagationProcessPathData = new PropagationProcessPathData(false);
        double[] aBoundary = EvaluateAttenuationCnossos.aBoundary(path, propagationProcessPathData);
        for(double value : aBoundary) {
            assertFalse(Double.isNaN(value));
        }
    }
}
