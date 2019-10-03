package org.noise_planet.noisemodelling.wps

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.junit.After
import org.junit.Before
import org.junit.Ignore

import java.sql.Connection

class JdbcTestCase  extends GroovyTestCase {
    Connection connection;

    @Ignore
    void testVoid() {
    }

    @Before
    void setUp() {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(UUID.randomUUID().toString(), true, "MODE=PostgreSQL"))
    }

    @After
    void tearDown() {
        connection.close()
    }
}
