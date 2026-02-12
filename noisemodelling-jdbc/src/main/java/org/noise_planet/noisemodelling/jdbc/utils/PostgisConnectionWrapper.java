package org.noise_planet.noisemodelling.jdbc.utils;

import org.h2gis.postgis_jts.ConnectionWrapper;
import org.h2gis.postgis_jts.JtsGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL connection wrapper that automatically converts PostGIS geometry types to JTS Geometry objects.
 * 
 * <p>This wrapper extends H2GIS's {@link ConnectionWrapper} and adds automatic JTS type registration,
 * replicating the behavior of H2GIS's {@link org.h2gis.postgis_jts.DataSourceWrapper#configureConnection}.
 * Once wrapped, {@link java.sql.ResultSet#getObject(String)} will return JTS {@link org.locationtech.jts.geom.Geometry}
 * objects directly, eliminating the need for manual WKB/WKT conversion.</p>
 * 
 * <h3>Usage</h3>
 * <pre>
 * // Wrap PostgreSQL connection at creation time
 * Connection raw = DriverManager.getConnection(url, user, password);
 * Connection wrapped = new PostgisConnectionWrapper(raw);
 * 
 * // Use normally - geometries are automatically converted to JTS
 * ResultSet rs = wrapped.createStatement().executeQuery("SELECT geom FROM table");
 * Geometry geom = (Geometry) rs.getObject("geom");  // Direct JTS Geometry!
 * </pre>
 * 
 * <h3>How It Works</h3>
 * <ul>
 *   <li>Extends H2GIS {@link ConnectionWrapper} for base PostGIS/JTS compatibility</li>
 *   <li>Registers {@link JtsGeometry} type via {@code PGConnection.addDataType("geometry", JtsGeometry.class)}</li>
 *   <li>Uses reflection to avoid compile-time dependency on PostgreSQL JDBC driver</li>
 *   <li>Enhanced {@link #unwrap(Class)} prevents bypassing the wrapper</li>
 * </ul>
 * 
 * <h3>Benefits vs Manual Conversion</h3>
 * <ul>
 *   <li>❌ <b>Before:</b> Manual WKB parsing: {@code new WKBReader().read(hexStringToByteArray(obj.toString()))}</li>
 *   <li>✅ <b>After:</b> Direct cast: {@code (Geometry) rs.getObject("geom")}</li>
 * </ul>
 * 
 * @see org.h2gis.postgis_jts.DataSourceWrapper
 * @see org.h2gis.postgis_jts.ConnectionWrapper
 * @see JtsGeometry
 */
public class PostgisConnectionWrapper extends ConnectionWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgisConnectionWrapper.class);
    private final Connection delegate;

    /**
     * Creates a new PostGIS connection wrapper and configures it to use JTS geometry types.
     * 
     * @param delegate The underlying PostgreSQL connection to wrap
     * @throws SQLException If the connection cannot be configured
     */
    public PostgisConnectionWrapper(Connection delegate) throws SQLException {
        super(delegate);
        this.delegate = delegate;
        configurePostGISTypes(delegate);
    }
    
    /**
     * Configure PostgreSQL connection to use JTS geometry types instead of PostGIS native types.
     * Uses reflection to avoid compile-time dependency on PostgreSQL JDBC driver.
     * This replicates the behavior of {@link org.h2gis.postgis_jts.DataSourceWrapper#configureConnection}.
     * 
     * @param connection The connection to configure
     * @throws SQLException If configuration fails
     */
    private static void configurePostGISTypes(Connection connection) throws SQLException {
        try {
            // Use reflection to avoid compile-time dependency on org.postgresql.PGConnection
            Class<?> pgConnectionClass = Class.forName("org.postgresql.PGConnection");
            
            if (pgConnectionClass.isInstance(connection)) {
                // Get the addDataType method
                java.lang.reflect.Method addDataType = pgConnectionClass.getMethod("addDataType", String.class, Class.class);
                
                // Register JTS geometry type (primary)
                addDataType.invoke(connection, "geometry", JtsGeometry.class);
                
                // Register PostGIS box types (required by some PostGIS operations)
                addDataType.invoke(connection, "box3d", Class.forName("net.postgis.jdbc.PGbox3d"));
                addDataType.invoke(connection, "box2d", Class.forName("net.postgis.jdbc.PGbox2d"));
                
                LOGGER.debug("✓ Configured PostgreSQL connection to use JTS geometry types via reflection");
            }
        } catch (ClassNotFoundException e) {
            // PostgreSQL JDBC driver not available - this is expected for H2GIS-only usage
            LOGGER.debug("PostgreSQL JDBC driver not available - skipping JTS type registration");
        } catch (Exception e) {
            LOGGER.warn("Failed to configure PostgreSQL JTS geometry types via reflection: {}", e.getMessage());
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return true;
        }
        return delegate.isWrapperFor(iface);
    }
}
