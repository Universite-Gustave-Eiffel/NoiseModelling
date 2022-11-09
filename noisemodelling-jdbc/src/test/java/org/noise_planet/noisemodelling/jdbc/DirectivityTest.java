package org.noise_planet.noisemodelling.jdbc;

import org.junit.Test;
import org.noise_planet.noisemodelling.emission.LineSource;
import org.noise_planet.noisemodelling.emission.directivity.DiscreteDirectivitySphere;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailWayCnossosParameters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DirectivityTest {
    final static double[] freqTest = new double[] {125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    List<String> noiseSource = Arrays.asList("ROLLING","TRACTIONA", "TRACTIONB","AERODYNAMICA","AERODYNAMICB","BRIDGE");

    @Test
    public void exportDirectivity() throws IOException {
        PolarGraphDirectivity polarGraphDirectivity = new PolarGraphDirectivity();
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("target/test.html"))) {
            bw.write("<!DOCTYPE html>\n" +
                    "<html><head><meta charset=\"utf-8\">\n" +
                    "<style>" +
                    ".wrapper {\n" +
                    "  display: grid;\n" +
                    "  grid-template-columns: repeat(3, 1fr);\n" +
                    "  grid-gap: 10px;\n" +
                    "  grid-auto-rows: minmax(100px, auto);\n" +
                    "}" +
                    "P { text-align: center; font-size: xx-large; }" +
                    ".one {\n" +
                    "  grid-column: 1 / 2;\n" +
                    "  grid-row: 1;\n" +
                    "}\n" +
                    ".two {\n" +
                    "  grid-column: 2 / 2;\n" +
                    "  grid-row: 1;\n" +
                    "}" +
                    "</style>" +
                    "<head><body><h1>500 Hz</h1>\n");
            for(String typeSource : noiseSource) {
                RailWayCnossosParameters.RailwayDirectivitySphere trainSource = new RailWayCnossosParameters.RailwayDirectivitySphere(new LineSource(typeSource));
                bw.write("<div class=\"wrapper\">");
                bw.write("<div class=\"one\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(trainSource, 500,
                        -35, 0, PolarGraphDirectivity.ORIENTATION.TOP));
                bw.write(String.format(Locale.ROOT, "<p>%s Horizontal</p>",noiseSource.toString()));
                bw.write("</div><div class=\"two\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(trainSource, 500,
                        -35, 0, PolarGraphDirectivity.ORIENTATION.SIDE));
                bw.write(String.format(Locale.ROOT, "<p>%s Side</p>",noiseSource.toString()));
                bw.write("</div></div>");
            }


            bw.write("</body>\n" +
                    "</html>\n ");
        }
    }


    @Test
    public void exportDirectivityDiscrete() throws IOException {


        DiscreteDirectivitySphere noiseSource = new DiscreteDirectivitySphere(1, freqTest);
        noiseSource.setInterpolationMethod(1);


        RailWayCnossosParameters.RailwayDirectivitySphere att = new RailWayCnossosParameters.RailwayDirectivitySphere(new LineSource("ROLLING"));

        for(int yaw = 0; yaw < 360; yaw += 5) {
            double phi = Math.toRadians(yaw);
            for(int pitch = -90; pitch <= 90; pitch += 5) {
                double theta = Math.toRadians(pitch);
                double[] attSpectrum = att.getAttenuationArray(freqTest, phi, theta);
                noiseSource.addDirectivityRecord(theta, phi,
                        attSpectrum);
            }
        }



        PolarGraphDirectivity polarGraphDirectivity = new PolarGraphDirectivity();
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("target/testDiscrete.html"))) {
            bw.write("<!DOCTYPE html>\n" +
                    "<html><head><meta charset=\"utf-8\">\n" +
                    "<style>" +
                    ".wrapper {\n" +
                    "  display: grid;\n" +
                    "  grid-template-columns: repeat(3, 1fr);\n" +
                    "  grid-gap: 10px;\n" +
                    "  grid-auto-rows: minmax(100px, auto);\n" +
                    "}" +
                    "P { text-align: center; font-size: xx-large; }" +
                    ".one {\n" +
                    "  grid-column: 1 / 2;\n" +
                    "  grid-row: 1;\n" +
                    "}\n" +
                    ".two {\n" +
                    "  grid-column: 2 / 2;\n" +
                    "  grid-row: 1;\n" +
                    "}" +
                    "</style>" +
                    "<head><body><h1>500 Hz</h1>\n");
                bw.write("<div class=\"wrapper\">");
                bw.write("<div class=\"one\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(noiseSource, 500,
                        -35, 0, PolarGraphDirectivity.ORIENTATION.TOP));
                bw.write(String.format(Locale.ROOT, "<p>%s Horizontal</p>", "Discrete directivity"));
                bw.write("</div><div class=\"two\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(noiseSource, 500,
                        -35, 0, PolarGraphDirectivity.ORIENTATION.SIDE));
                bw.write(String.format(Locale.ROOT, "<p>%s Side</p>", "Discrete directivity"));
                bw.write("</div></div>");


            bw.write("</body>\n" +
                    "</html>\n ");
        }
    }
}
