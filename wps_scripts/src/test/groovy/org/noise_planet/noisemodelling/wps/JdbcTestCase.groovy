/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

package org.noise_planet.noisemodelling.wps

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.JDBCUtilities
import org.junit.After
import org.junit.Before
import org.junit.Ignore

import java.sql.Connection

@Ignore
class JdbcTestCase  extends GroovyTestCase {
    Connection connection;

    @Before
    void setUp() {
       // connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(UUID.randomUUID().toString().replace("-", ""), true))
        connection = JDBCUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase("test", true))
    }

    @After
    void tearDown() {
        connection.close()
    }
}
