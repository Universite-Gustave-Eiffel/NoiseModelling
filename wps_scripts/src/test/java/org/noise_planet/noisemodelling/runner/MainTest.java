package org.noise_planet.noisemodelling.runner;

import org.junit.jupiter.api.Test;
import org.noisemodelling.runner.Main;

public class MainTest {

    @Test
    public void testDoubleArg() throws Exception {
        String receiverPath = MainTest.class.getResource("../wps/receivers.shp").getPath();
        Main.main("-w", "target",
                "-s", "groovy/org/noise_planet/noisemodelling/wps/Import_and_Export/Import_File.groovy",
                "--pathFile", receiverPath);
    }
}
