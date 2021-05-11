package org.noise_planet.noisemodelling.jdbc;

import org.junit.Test;
import org.noise_planet.noisemodelling.emission.RailWayLW;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DirectivityTest {

    @Test
    public void exportDirectivity() throws IOException {
        PolarGraphDirectivity polarGraphDirectivity = new PolarGraphDirectivity();
        try(BufferedWriter bw = new BufferedWriter(new FileWriter("target/test.html"))) {
            bw.write("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n");
            bw.write(polarGraphDirectivity.generatePolarGraph(RailWayLW.TrainNoiseSource.ROLLING, 500,
                    -25, 0));

            bw.write("</body>\n" +
                    "</html>\n ");
        }
    }
}
