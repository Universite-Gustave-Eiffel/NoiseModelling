package org.noise_planet.noisemodelling.wps;

import junit.framework.TestCase;
import org.junit.Test;
import org.noisemodelling.runner.Main;

import java.util.UUID;

public class MainTest extends TestCase {
    String dbName = UUID.randomUUID().toString().replace("-", "");

    @Test
    public void testSetHeight() throws Exception {
        String receiverPath = MainTest.class.getResource("receivers.shp").getPath();
        Main.main("-w", "build/tmp",
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/wps/Import_and_Export/Import_File.groovy",
                "-pathFile", receiverPath);

        Main.main("-w", "build/tmp",
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/wps/Geometric_Tools/Set_Height.groovy",
                "-tableName", "RECEIVERS", "-height" , "1.5");
    }

}
