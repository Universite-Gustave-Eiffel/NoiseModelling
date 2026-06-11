package org.noise_planet.noisemodelling.propagation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2gis.api.EmptyProgressVisitor;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.CutPlaneVisitor;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.sumArray;
import static org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions.sumDbArray;

public class AttenuationComputeOutputTemplateTest {
    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;

    private static CutProfile loadCutProfile(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, CutProfile.class);
    }

    private static List<double[]> computeTemplateAttenuation(String utName)
            throws IOException {
        //Get test data
        URL url = AttenuationComputeOutputTemplateTest.class.getResource("template/" + utName + ".json");

        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .finishFeeding();

        //Propagation data building
        SceneWithAttenuation sceneWithAttenuation = new SceneWithAttenuation(profileBuilder);
        sceneWithAttenuation.sourceGs.put(-1L, 0.5);

        //Propagation process path data building
        sceneWithAttenuation.defaultCnossosParameters.setHumidity(HUMIDITY);
        sceneWithAttenuation.defaultCnossosParameters.setTemperature(TEMPERATURE);

        //Out and computation settings
        CutProfile cutProfile;
        try(InputStream inputStream = url.openStream()) {
            cutProfile = loadCutProfile(inputStream);
        }

        PropagationModel propagationModel = PropagationModelFactory.create(
                "template", sceneWithAttenuation, cutProfile);

        return propagationModel.computeAttenuation(
                sceneWithAttenuation.defaultCnossosParameters, false);
    }

    /**
     * Dummy for template P2P propagation model
     */
    @Test
    public void test_template_01() throws IOException {

        List<double[]> attenuation = computeTemplateAttenuation("case_1_4");

        //Assertion
        assertEquals(0, attenuation.get(0)[0]);
        assertEquals(0, attenuation.get(1)[0]);
    }
}
