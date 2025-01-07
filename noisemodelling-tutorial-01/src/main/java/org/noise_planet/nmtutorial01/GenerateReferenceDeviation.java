/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.nmtutorial01;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.path.Scene;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilderDecorator;
import org.noise_planet.noisemodelling.propagation.Attenuation;
import org.noise_planet.noisemodelling.propagation.AttenuationVisitor;
import org.noise_planet.noisemodelling.propagation.cnossos.AttenuationCnossosParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class GenerateReferenceDeviation {

    private static final double[] SOUND_POWER_LEVELS = new double[]{93, 93, 93, 93, 93, 93, 93, 93};
    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;

    private static CutProfile loadCutProfile(String utName) throws IOException {
        String testCaseFileName = utName + ".json";
        try(InputStream inputStream = PathFinder.class.getResourceAsStream("test_cases/"+testCaseFileName)) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(inputStream, CutProfile.class);
        }
    }

    public static double[] addArray(double[] first, double[] second) {
        int length = Math.min(first.length, second.length);
        double[] result = new double[length];

        for (int i = 0; i < length; i++) {
            result[i] = first[i] + second[i];
        }

        return result;
    }

    private static Attenuation computeCnossosPath(String... utNames)
            throws IOException {
        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .finishFeeding();

        //Propagation data building
        Scene rayData = new ProfileBuilderDecorator(profileBuilder)
                .build();

        //Propagation process path data building
        AttenuationCnossosParameters attData = new AttenuationCnossosParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        Attenuation propDataOut = new Attenuation(true, true, attData, rayData);

        AttenuationVisitor attenuationVisitor = new AttenuationVisitor(propDataOut, propDataOut.genericMeteoData);
        for (String utName : utNames) {
            CutProfile cutProfile = loadCutProfile(utName);
            attenuationVisitor.onNewCutPlane(cutProfile);
        }
        // merge attenuation per receiver
        attenuationVisitor.finalizeReceiver(0);

        return propDataOut;
    }

    private static double[] asArray(JsonNode arrayNode) {
        double[] doubleArray = new double[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            doubleArray[i] = arrayNode.get(i).asDouble();
        }
        return doubleArray;
    }

    /**
     * For each cnossos test case, compute the attenuation and compare with the expected value, generate the result
     * report in rst format.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(GenerateReferenceDeviation.class);
        try(InputStream referenceStream = GenerateReferenceDeviation.class.getResourceAsStream("reference_cnossos.json")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.getFactory().createParser(referenceStream).readValueAsTree();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> elt = it.next();
                String utName = elt.getKey();
                Attenuation attenuation = computeCnossosPath(utName);
                double[] actualLH = addArray(attenuation.getPropagationPaths().get(0).aGlobalH, SOUND_POWER_LEVELS);
                double[] actualLF = addArray(attenuation.getPropagationPaths().get(0).aGlobalF, SOUND_POWER_LEVELS);
                double[] expectedLH = asArray(elt.getValue().get("LH"));
                if(elt.getValue().has("LF")) {
                    double[] expectedLF = asArray(elt.getValue().get("LF"));
                }
            }
        }
    }

}
