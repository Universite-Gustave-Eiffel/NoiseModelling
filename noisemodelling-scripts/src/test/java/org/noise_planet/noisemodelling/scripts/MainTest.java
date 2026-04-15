package org.noise_planet.noisemodelling.scripts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.noise_planet.noisemodelling.runner.Main;

import java.io.File;
import java.util.UUID;

public class MainTest {
    String dbName = UUID.randomUUID().toString().replace("-", "");

    @Test
    public void testSetHeight(@TempDir File temp) throws Exception {
        String receiverPath = MainTest.class.getResource("receivers.shp").getPath();
        Main.main("-w", temp.getAbsolutePath(),
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Import_and_Export/Import_File.groovy",
                "--pathFile", receiverPath);

        Main.main("-w", temp.getAbsolutePath(),
                "-d", dbName,
                "-s", "src/main/groovy/org/noise_planet/noisemodelling/scripts/Geometric_Tools/Set_Height.groovy",
                "--tableName", "RECEIVERS", "--height" , "1.5");
    }

}
