package org.noise_planet.noisemodelling.jdbc;

import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.JDBCUtilities;
import org.junit.Test;
import org.noise_planet.noisemodelling.emission.DiscreteDirectionAttributes;
import org.noise_planet.noisemodelling.emission.RailWayLW;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

public class DirectivityTest {
    final static double[] freqTest = new double[] {125, 250, 500, 1000, 2000, 4000, 8000, 16000};

    @Test
    public void exportDirectivityCardioid() throws Exception {
        PolarGraphDirectivity polarGraphDirectivity = new PolarGraphDirectivity();
        Connection connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(DirectivityTableLoaderTest.class.getSimpleName(), true, ""));
        Statement st = connection.createStatement();
        st.execute("CREATE TABLE DIRECTIVITY(DIR_ID INTEGER, THETA FLOAT, PHI FLOAT, LW500 FLOAT)");
        PreparedStatement pst = connection.prepareStatement("INSERT INTO DIRECTIVITY VALUES(?, ?, ?, ?)");
        for(int phi = 0; phi < 360; phi+=2) {
            for(int theta=-90; theta <= 90; theta += 1) {
                pst.setInt(1, 1);
                pst.setDouble(2, theta);
                pst.setDouble(3, phi);
                double att =  30 * (Math.cos(Math.toRadians(phi)) * Math.cos(Math.toRadians(theta)));
                pst.setDouble(4, att);
                pst.execute();
            }
        }
        // Data is inserted now fetch it from the database
        Map<Integer, DiscreteDirectionAttributes> directivities =
                DirectivityTableLoader.loadTable(connection, "DIRECTIVITY", 1);

        try(BufferedWriter bw = new BufferedWriter(new FileWriter("target/cardioid_dir.html"))) {
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
                    ".three {\n" +
                    "  grid-column: 1 / 2;\n" +
                    "  grid-row: 2;\n" +
                    "}" +
                    "</style>" +
                    "<head><body><h1>500 Hz</h1>\n");
            for (Map.Entry<Integer, DiscreteDirectionAttributes> entry : directivities.entrySet()) {
                bw.write("<div class=\"wrapper\">");
                bw.write("<div class=\"one\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(entry.getValue(), 500,
                        -40, 0, PolarGraphDirectivity.ORIENTATION.TOP));
                bw.write(String.format(Locale.ROOT, "<p>%d Top</p>",entry.getKey()));
                bw.write("</div>");
                bw.write("<div class=\"two\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(entry.getValue(), 500,
                        -40, 0, PolarGraphDirectivity.ORIENTATION.SIDE));
                bw.write(String.format(Locale.ROOT, "<p>%d Side</p>",entry.getKey()));
                bw.write("</div>");
                bw.write("<div class=\"three\">");
                bw.write(polarGraphDirectivity.generatePolarGraph(entry.getValue(), 500,
                        -40, 0, PolarGraphDirectivity.ORIENTATION.FRONT));
                bw.write(String.format(Locale.ROOT, "<p>%d Front</p>",entry.getKey()));
                bw.write("</div>");
                bw.write("</div>");
            }
            bw.write("</body>\n" +
                    "</html>\n ");
        }
    }

    @Test
    public void exportDirectivityDiscrete() throws IOException {


        DiscreteDirectionAttributes noiseSource = new DiscreteDirectionAttributes(1, freqTest);
        noiseSource.setInterpolationMethod(1);

        RailWayLW.TrainAttenuation att = new RailWayLW.TrainAttenuation(RailWayLW.TrainNoiseSource.ROLLING);

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
