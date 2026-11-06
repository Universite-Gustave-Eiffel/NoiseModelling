/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.emission.railway;

import org.junit.jupiter.api.Test;
import org.noise_planet.noisemodelling.emission.railway.cnossos.RailwayTrackCnossosParameters;
import org.noise_planet.noisemodelling.emission.railway.cnossosvar.RailwayCnossosvar;
import org.noise_planet.noisemodelling.emission.railway.cnossosvar.RailwayVehicleCnossosParametersvar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RailwayCnossosvarTest {

    @Test
    public void testMinimumSpeedWarning() throws IOException {
        RailwayCnossosvar railway = new RailwayCnossosvar();
        railway.setVehicleDataFile("RailwayVehiclesCnossos.json");
        railway.setRailwayDataFile("RailwayEmissionCnossos.json");

        Logger logger = Logger.getLogger(RailwayCnossosvar.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            RailwayVehicleCnossosParametersvar vehicleParameters =
                    new RailwayVehicleCnossosParametersvar("NL_CAT8", 20, 0, 0);
            RailwayTrackCnossosParameters trackParameters =
                    new RailwayTrackCnossosParameters(160, "NL_BB1", "NL", "1", "0", 0, 140, false, 1);
            railway.evaluate(vehicleParameters, trackParameters);
        } finally {
            logger.removeHandler(handler);
        }

        assertTrue(records.stream().anyMatch(record -> record.getLevel() == Level.WARNING
                        && record.getMessage().contains("below the CNOSSOS minimum")),
                "A warning must be logged when the speed is below the CNOSSOS minimum");
    }
}
