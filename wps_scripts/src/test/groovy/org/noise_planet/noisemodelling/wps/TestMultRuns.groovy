package org.noise_planet.noisemodelling.wps
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Experimental.Multi_Runs

class TestMultRuns extends JdbcTestCase  {

    @Test
    void testMultiRun() {
        new Multi_Runs().exec(connection,
                ["workingDir":TestMultRuns.class.getResource("multirun/").getPath(),
                 "nbSimu" : 4])
    }


}
