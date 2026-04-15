package org.noise_planet.noisemodelling;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VersionUtilsTest {

    @Test
    public void testGetVersion() {
        String version = VersionUtils.getVersion();
        assertNotNull(version);
        assertNotEquals("Unknown", version);
    }
}
