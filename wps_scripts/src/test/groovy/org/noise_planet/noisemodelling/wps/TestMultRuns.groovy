/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 */


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
                 "nbSimu" : 4, "threadNumber" : 0])

        // Check database
        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("MULTIRUNSRESULTS_GEOM"))
    }


}
