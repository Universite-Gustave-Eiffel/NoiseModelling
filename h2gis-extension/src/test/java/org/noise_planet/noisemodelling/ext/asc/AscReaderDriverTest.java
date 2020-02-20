package org.noise_planet.noisemodelling.ext.asc;

import org.h2gis.api.EmptyProgressVisitor;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.utilities.SFSUtilities;
import org.junit.After;
    import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class AscReaderDriverTest {

    private Connection connection;

    @Before
    public void tearUp() throws Exception {
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.createSpatialDataBase(AscReaderDriverTest.class.getSimpleName(), true, ""));
    }

    @After
    public void tearDown() throws Exception {
        if(connection != null) {
            connection.close();
        }
    }

    @Test
    public void testReadPrecip() throws IOException, SQLException {
        AscReaderDriver reader = new AscReaderDriver();
        try(InputStream inputStream = AscReaderDriverTest.class.getResourceAsStream("precip30min.asc")) {
            reader.read(connection, inputStream, new EmptyProgressVisitor(), "PRECIP30MIN", 4326);
        }
    }
}