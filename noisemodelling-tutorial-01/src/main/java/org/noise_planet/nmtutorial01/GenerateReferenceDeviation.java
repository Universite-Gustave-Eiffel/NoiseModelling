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
import org.h2gis.api.EmptyProgressVisitor;
import org.locationtech.jts.geom.Coordinate;
import org.noise_planet.noisemodelling.pathfinder.PathFinder;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.CutProfile;
import org.noise_planet.noisemodelling.pathfinder.profilebuilder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions;
import org.noise_planet.noisemodelling.propagation.AttenuationComputeOutput;
import org.noise_planet.noisemodelling.propagation.AttenuationParameters;
import org.noise_planet.noisemodelling.propagation.AttenuationVisitor;
import org.noise_planet.noisemodelling.propagation.SceneWithAttenuation;

import org.noise_planet.noisemodelling.propagation.cnossos.CnossosPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class GenerateReferenceDeviation {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateReferenceDeviation.class);
    private static final List<Integer> FREQ_LVL = Arrays.asList(
            AcousticIndicatorsFunctions.asOctaveBands(ProfileBuilder.DEFAULT_FREQUENCIES_THIRD_OCTAVE));
    private static final double[] SOUND_POWER_LEVELS = new double[]{93, 93, 93, 93, 93, 93, 93, 93};
    private static final double[] A_WEIGHTING = new double[]{-26.2, -16.1, -8.6, -3.2, 0.0, 1.2, 1.0, -1.1};
    private static final String[] PATH_NAMES = new String[] {"Vertical Plane", "Left Lateral", "Right Lateral", "Reflection"};
    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;
    private static final String CHECKED = "☑";
    private static final String UNCHECKED = "□";
    private static final String REPORT_HEADER = "Conformity to ISO 17534-1:2015\n" +
            "==============================\n" +
            ".. DO NOT UPDATE THIS FILE!!\n" +
            ".. This document has been automatically generated with noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateReferenceDeviation.java\n" +
            "\n" +
            "\n" +
            "Clarifications on the ISO Standard and Identified Issues\n" +
            "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n" +
            "\n" +
            "It is important to note that the ISO standard provides recommendations rather than regulatory obligations. While it serves as a reference framework, its application is not mandatory from a legal standpoint.\n" +
            "\n" +
            "During our analysis, we identified several issues within the standard that hinder a complete and reliable comparison. Notably, we observed inconsistencies between 2D and 3D visualizations, preventing us from achieving a coherent assessment. Additionally, discrepancies exist between the geometric description of the scene and the corresponding acoustic response, raising concerns about the accuracy and reliability of the standard’s methodology.\n" +
            "\n" +
            "Furthermore, with respect to favourable rays, our findings indicate a different implementation of CNOSSOS compared to the approach suggested by the standard. This divergence may have implications for the interpretation and reproducibility of results, necessitating further clarification and alignment.\n" +
            "\n" +
            "\n" +
            "Conformity table\n" +
            "^^^^^^^^^^^^^^^^\n";

    private static final String TABLE_HEADER = ".. list-table::\n" +
            "   :widths: 10 20 20 25 30\n" +
            "\n" +
            "   * - Test Case\n" +
            "     - Conform ? \n" +
            "     - NLD Conform ?\n" +
            "     - Largest Deviation\n" +
            "     - Details\n";

    private static CutProfile loadCutProfile(String utName) throws IOException {
        String testCaseFileName = utName + ".json";
        try(InputStream inputStream = PathFinder.class.getResourceAsStream("test_cases/"+testCaseFileName)) {
            if(inputStream == null) {
                return null;
            }
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

    private static AttenuationComputeOutput computeCnossosPath(String... utNames)
            throws IOException {
        //Create profile builder
        ProfileBuilder profileBuilder = new ProfileBuilder()
                .finishFeeding();

        //Propagation data building
        SceneWithAttenuation scene = new SceneWithAttenuation(profileBuilder);

        //Propagation process path data building
        AttenuationParameters attData = new AttenuationParameters();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        scene.defaultCnossosParameters = attData;

        //Out and computation settings
        AttenuationComputeOutput propDataOut = new AttenuationComputeOutput(true, true, scene);

        AttenuationVisitor attenuationVisitor = (AttenuationVisitor)propDataOut.subProcess(new EmptyProgressVisitor());
        PathFinder.ReceiverPointInfo lastReceiver = new PathFinder.ReceiverPointInfo(-1,-1,new Coordinate());
        for (String utName : utNames) {
            CutProfile cutProfile = loadCutProfile(utName);
            if(cutProfile != null) {
                attenuationVisitor.onNewCutPlane(cutProfile);
                if (lastReceiver.receiverPk != -1 && cutProfile.getReceiver().receiverPk != lastReceiver.receiverPk) {
                    // merge attenuation per receiver
                    attenuationVisitor.finalizeReceiver(new PathFinder.ReceiverPointInfo(cutProfile.getReceiver()));
                }
                lastReceiver = new PathFinder.ReceiverPointInfo(cutProfile.getReceiver());
            }
        }
        // merge attenuation per receiver
        attenuationVisitor.finalizeReceiver(lastReceiver);

        return propDataOut;
    }

    private static double[] asArray(JsonNode arrayNode) {
        double[] doubleArray = new double[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            doubleArray[i] = arrayNode.get(i).asDouble();
        }
        return doubleArray;
    }

    private static DeviationResult computeDeviation(double[] expected, double[] actual) {
        assert expected.length == actual.length;
        assert expected.length == FREQ_LVL.size();
        int largestDiffIndex = IntStream.range(0, expected.length)
                .boxed()
                .max(Comparator.comparingDouble(i -> Math.abs(expected[i] - actual[i])))
                .orElse(-1);
        if(largestDiffIndex >= 0) {
            return new DeviationResult(Math.abs(expected[largestDiffIndex] - actual[largestDiffIndex]),
                    FREQ_LVL.get(largestDiffIndex));
        } else {
            return new DeviationResult(0, 0);
        }
    }

    /**
     * Generate documentatin content for a unit test according to the deviation
     * @param utName Unit test name
     * @param sb Output of documentation
     * @param expectedValues Expected results
     * @param actual Computed results
     * @param actualWithoutLateral Computed results without lateral diffraction
     * @param powerLevel Emission level at the source
     */
    private static void addUTDeviation(String utName, StringBuilder sb, JsonNode expectedValues,
                                       AttenuationComputeOutput actual, AttenuationComputeOutput actualWithoutLateral,
                                       double[] powerLevel, AtomicInteger fullConform, AtomicInteger directConform) {

        double[] expectedLA = asArray(expectedValues.get("LA"));
        double[] expectedLAWithoutLateral = asArray(expectedValues.get("LA_WL"));
        double[] actualLA = addArray(powerLevel, addArray(actual.receiversAttenuationLevels.getFirst().levels,
                A_WEIGHTING));
        double[] actualLAWithoutLateral = addArray(powerLevel,
                addArray(actualWithoutLateral.receiversAttenuationLevels.getFirst().levels,
                A_WEIGHTING));
        DeviationResult lADeviation = computeDeviation(expectedLA, actualLA);
        DeviationResult lADeviationWithoutLateral = computeDeviation(expectedLAWithoutLateral, actualLAWithoutLateral);
        if(lADeviation.deviation <= 0.1) {
            fullConform.incrementAndGet();
        }
        if(lADeviationWithoutLateral.deviation <= 0.1) {
            directConform.incrementAndGet();
        }
        sb.append(String.format(Locale.ROOT, "   * - %s\n" +
                "     - %s\n" +
                "     - %s\n" +
                "     - %s\n" +
                "     - `%s`_\n",
                utName,
                lADeviation.deviation <= 0.1 ? CHECKED : UNCHECKED,
                lADeviationWithoutLateral.deviation <= 0.1 ? CHECKED : UNCHECKED,
                lADeviation.deviation > lADeviationWithoutLateral.deviation ?
                        String.format(Locale.ROOT, "%.2f dB @ %d Hz",
                                lADeviation.deviation,
                                lADeviation.frequency)
                        :
                        String.format(Locale.ROOT, "%.2f dB @ %d Hz",
                                lADeviationWithoutLateral.deviation,
                                lADeviationWithoutLateral.frequency),
                utName));
    }

    private static CnossosPath fetchPath(List<CnossosPath> paths, CutProfile.PROFILE_TYPE profileType, boolean favourable) {
        for(CnossosPath path : paths) {
            if(path.getCutProfile().profileType == profileType && path.isFavourable() == favourable) {
                return path;
            }
        }
        return null;
    }

    private static void addUTDeviationDetails(CutProfile.PROFILE_TYPE profileType, StringBuilder sb, JsonNode expectedValues, List<CnossosPath> paths, double[] powerLevel) {
        CnossosPath homogenous = fetchPath(paths, profileType, false);
        String utName = PATH_NAMES[profileType.ordinal()];
        assert homogenous != null;
        double[] actualLH = addArray(homogenous.aGlobalRaw, powerLevel);
        double[] expectedLH = asArray(expectedValues.get("LH"));
        DeviationResult lhDeviation = computeDeviation(expectedLH, actualLH);
        sb.append(String.format(Locale.ROOT, "\n\n%s \n" +
                "\n" +
                "================\n" +
                "\n" +
                ".. list-table::\n" +
                "   :widths: 25 25 25\n" +
                "\n" +
                "   * - Parameters\n" +
                "     - Maximum Difference\n" +
                "     - Frequency\n" +
                "   * - Lʜ\n" +
                "     - %.2f dB\n" +
                "     - %d\n", utName.replace("_", " "), lhDeviation.deviation, lhDeviation.frequency));

        if(expectedValues.has("LF")) {
            CnossosPath favourablePath = fetchPath(paths, profileType, true);
            if(favourablePath != null) {
                double[] actualLF = addArray(favourablePath.aGlobalRaw, powerLevel);
                double[] expectedLF = asArray(expectedValues.get("LF"));
                DeviationResult lfDeviation = computeDeviation(expectedLF, actualLF);
                sb.append(String.format(Locale.ROOT, "   * - Lꜰ\n" +
                        "     - %.2f dB\n" +
                        "     - %d\n", lfDeviation.deviation, lfDeviation.frequency));
            } else {
                sb.append("   * - Lꜰ\n" +
                        "     - -\n" +
                        "     - -\n");
            }
        }
    }

    /**
     * For each cnossos test case, compute the attenuation and compare with the expected value, generate the result
     * report in rst format.
     * @param args var args
     * @throws IOException exception
     */
    public static void main(String[] args) throws IOException {
        // Read working directory argument
        String workingDir = "Docs";
        if (args.length > 0) {
            workingDir = args[0];
        }
        File workingDirPath = new File(workingDir).getAbsoluteFile();
        if(!workingDirPath.exists()) {
            LOGGER.error("Working directory {} does not exists", workingDir);
            return;
        }
        File documentPath = new File(workingDirPath, "Cnossos_Report.rst");
        try(FileWriter fileWriter = new FileWriter(documentPath)) {
            fileWriter.write(REPORT_HEADER);
            int total = 0;
            AtomicInteger directPass = new AtomicInteger(0);
            AtomicInteger fullPass = new AtomicInteger(0);
            try (InputStream referenceStream = GenerateReferenceDeviation.class.getResourceAsStream("reference_cnossos.json")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.getFactory().createParser(referenceStream).readValueAsTree();
                StringBuilder stringBuilderDetail = new StringBuilder();
                StringBuilder stringBuilderTable = new StringBuilder();
                for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                    total += 1;
                    Map.Entry<String, JsonNode> elt = it.next();
                    String utName = elt.getKey();
                    JsonNode pathsExpected = elt.getValue();
                    double[] powerLevel = SOUND_POWER_LEVELS;
                    if(pathsExpected.has("PL")) {
                        powerLevel = asArray(pathsExpected.get("PL"));
                    }
                    List<String> verticalCutFileNames = new ArrayList<>();
                    List<String> verticalCutFileNamesWithoutLateral = new ArrayList<>();
                    verticalCutFileNames.add(utName+"_Direct");
                    verticalCutFileNamesWithoutLateral.add(utName+"_Direct");
                    if(pathsExpected.has("Right")) {
                        verticalCutFileNames.add(utName+"_Right");
                        verticalCutFileNames.add(utName+"_Right_Curved");
                    }
                    if(pathsExpected.has("Left")) {
                        verticalCutFileNames.add(utName+"_Left");
                        verticalCutFileNames.add(utName+"_Left_Curved");
                    }
                    if(pathsExpected.has("Reflection")) {
                        verticalCutFileNames.add(utName+"_Reflection");
                        verticalCutFileNamesWithoutLateral.add(utName+"_Reflection");
                    }
                    AttenuationComputeOutput attenuationComputeOutput = computeCnossosPath(verticalCutFileNames.toArray(new String[]{}));
                    AttenuationComputeOutput attenuationComputeOutputWithoutLateral = computeCnossosPath(verticalCutFileNamesWithoutLateral.toArray(new String[]{}));
                    addUTDeviation(utName, stringBuilderTable, pathsExpected, attenuationComputeOutput, attenuationComputeOutputWithoutLateral, powerLevel, fullPass, directPass);
                    // Write details
                    stringBuilderDetail.append("\n").append(utName).append("\n^^^^\n");
                    addUTDeviationDetails(CutProfile.PROFILE_TYPE.DIRECT, stringBuilderDetail, pathsExpected.get("Direct"), attenuationComputeOutput.getPropagationPaths(), powerLevel);
                    if(pathsExpected.has("Right")) {
                        addUTDeviationDetails(CutProfile.PROFILE_TYPE.RIGHT, stringBuilderDetail, pathsExpected.get("Right"), attenuationComputeOutput.getPropagationPaths(), powerLevel);
                    }
                    if(pathsExpected.has("Left")) {
                        addUTDeviationDetails(CutProfile.PROFILE_TYPE.LEFT, stringBuilderDetail, pathsExpected.get("Left"), attenuationComputeOutput.getPropagationPaths(), powerLevel);
                    }
                    if(pathsExpected.has("Reflection")) {
                        addUTDeviationDetails(CutProfile.PROFILE_TYPE.REFLECTION, stringBuilderDetail, pathsExpected.get("Reflection"), attenuationComputeOutput.getPropagationPaths(), powerLevel);
                    }
                }
                fileWriter.write(String.format(Locale.ROOT, "| Conform\n" +
                                "\n" +
                                "* Do not the deviate more than ±0,1 dB\n" +
                                "* Percentage of conformity : %d%% (%d/%d)\n" +
                                "\n" +
                                "| NLD Conform\n" +
                                "\n" +
                                "* Do not the deviate more than ±0,1 dB neglecting lateral diffraction\n" +
                                "* Percentage of conformity : %d%% (%d/%d)\n\n",
                        (int)Math.ceil(fullPass.get()/(double)total * 100), fullPass.get(), total,
                        (int)Math.ceil(directPass.get()/(double)total * 100), directPass.get(), total));
                fileWriter.write(TABLE_HEADER);
                fileWriter.write(stringBuilderTable.toString());
                fileWriter.write(stringBuilderDetail.toString());
            }
        }
        LOGGER.info("Document written to {}", documentPath.getAbsolutePath());
    }

    static class DeviationResult {
        public DeviationResult(double deviation, int frequency) {
            this.deviation = deviation;
            this.frequency = frequency;
        }

        double deviation;
        int frequency;
    }
}
