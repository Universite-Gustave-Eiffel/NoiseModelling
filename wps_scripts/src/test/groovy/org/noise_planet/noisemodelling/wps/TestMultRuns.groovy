package org.noise_planet.noisemodelling.wps
import org.junit.Test
import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Experimental.Multi_Runs

class TestMultRuns extends JdbcTestCase  {

    @Test
    void testMultiRun() {

        Object res = new Display_Database().exec(connection, [])
        assertEquals("", res)


        new Multi_Runs().exec(connection,
                ["workingDir":TestMultRuns.class.getResource("multirun/").getPath(),
                 "nbSimu" : 4])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("MULTIRUNSRESULTS_GEOM"))
    }


}
