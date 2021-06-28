package org.noise_planet.noisemodelling.jdbc;

import org.junit.Test;
import org.noise_planet.noisemodelling.emission.RailWayLW;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Statement;
import java.util.Locale;

public class DirectivityTest {

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
            for(RailWayLW.TrainNoiseSource noiseSource : RailWayLW.TrainNoiseSource.values()) {
                bw.write("<div class=\"wrapper\">");
                bw.write("<div class=\"one\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(noiseSource, 500,
                        -35, 0, PolarGraphDirectivity.ORIENTATION.TOP));
                bw.write(String.format(Locale.ROOT, "<p>%s Horizontal</p>",noiseSource.toString()));
                bw.write("</div><div class=\"two\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(noiseSource, 500,
                        -35, 0, PolarGraphDirectivity.ORIENTATION.SIDE));
                bw.write(String.format(Locale.ROOT, "<p>%s Side</p>",noiseSource.toString()));
                bw.write("</div></div>");
            }


            bw.write("</body>\n" +
                    "</html>\n ");
        }
    }
}
