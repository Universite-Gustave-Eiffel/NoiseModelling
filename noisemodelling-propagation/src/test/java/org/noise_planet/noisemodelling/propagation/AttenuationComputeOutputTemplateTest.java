package org.noise_planet.noisemodelling.propagation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.propagation.template.TemplatePropagationModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AttenuationComputeOutputTemplateTest {
    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;

    private static CutProfile loadCutProfile(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, CutProfile.class);
    }

    private static double[] computeTemplateAttenuation(String utName)
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

        PropagationModel propagationModel = new TemplatePropagationModel();

        return propagationModel.computeAttenuation(sceneWithAttenuation, cutProfile,
                sceneWithAttenuation.defaultCnossosParameters, false).getFirst().getaGlobal();
    }

    /**
     * Dummy for template P2P propagation model
     */
    @Test
    public void test_template_01() throws IOException {

        double[] attenuation = computeTemplateAttenuation("case_1_4");

        //Assertion
        assertEquals(0, attenuation[0]);

    }
}
