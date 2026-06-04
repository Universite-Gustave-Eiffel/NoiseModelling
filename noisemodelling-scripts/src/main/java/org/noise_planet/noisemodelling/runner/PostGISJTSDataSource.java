/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and
 * education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE
 * provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.runner;

import net.postgis.jdbc.PGbox2d;
import net.postgis.jdbc.PGbox3d;
import org.h2gis.postgis_jts.ConnectionWrapper;
import org.h2gis.postgis_jts.JtsGeometry;
import org.postgresql.PGConnection;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles JTS objects from PosGIS connection
 */
public class PostGISJTSDataSource extends PGSimpleDataSource {
    public PostGISJTSDataSource() {
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        return configureConnection(connection);
    }

    private Connection configureConnection(Connection connection) throws SQLException {
        if(connection instanceof PGConnection) {
            ((PGConnection) connection).addDataType("geometry", JtsGeometry.class);
            ((PGConnection) connection).addDataType("box3d", PGbox3d.class);
            ((PGConnection) connection).addDataType("box2d", PGbox2d.class);
        }
        return new ConnectionWrapper(connection);
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException {
        Connection connection = super.getConnection(user, password);
        return configureConnection(connection);
    }
}
