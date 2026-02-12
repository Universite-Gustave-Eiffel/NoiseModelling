/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas.
 * It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in
 * Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE
 * provided with this software.
 *
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.wps.Database_Manager

import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.TableLocation
import org.noise_planet.noisemodelling.jdbc.utils.GeometrySqlHelper
import org.locationtech.jts.geom.Geometry

import java.sql.Connection
import java.sql.ResultSet

/**
 * Helper class for cross-database compatibility (H2GIS / PostGIS) in WPS Groovy scripts.
 * Wraps {@link GeometrySqlHelper} with Groovy-friendly methods.
 * @author Pierre Aumond, Université Gustave Eiffel
 */
class DatabaseHelper {

    /** Check if connection is PostgreSQL */
    static boolean isPostgreSQL(Connection connection) {
        return GeometrySqlHelper.isPostgreSQL(DBUtils.getDBType(connection))
    }

    /** Returns "SERIAL" for PostgreSQL, "AUTO_INCREMENT" for H2 */
    static String autoIncrement(Connection connection) {
        return isPostgreSQL(connection) ? "SERIAL" : "AUTO_INCREMENT"
    }

    /** Returns "DOUBLE PRECISION" for PostgreSQL, "DOUBLE" for H2 */
    static String doublePrecision(Connection connection) {
        return isPostgreSQL(connection) ? "DOUBLE PRECISION" : "DOUBLE"
    }

    /** Resolve the underlying connection (unwrap if wrapped) */
    static Connection resolveConnection(Connection connection) {
        return GeometrySqlHelper.resolveConnection(connection)
    }

    /** Get the SRID of a geometry column */
    static int getTableSRID(Connection connection, String tableName, String geometryColumn) {
        return GeometrySqlHelper.getTableSRID(connection, tableName, geometryColumn)
    }

    /** Get integer primary key name */
    static String getIntegerPrimaryKey(Connection connection, String tableName) {
        def dbType = DBUtils.getDBType(resolveConnection(connection))
        def pk = GeometrySqlHelper.getIntegerPrimaryKey(resolveConnection(connection), TableLocation.parse(tableName, dbType))
        return pk != null ? pk.first() : null
    }

    /** Read a Geometry from a ResultSet column (works on both H2GIS and PostGIS) */
    static Geometry getGeometry(ResultSet rs, String columnName) {
        DBTypes dbType = GeometrySqlHelper.resolveDbType(rs.getStatement().getConnection())
        return GeometrySqlHelper.getGeometry(rs, columnName, dbType)
    }

    /** Read a Geometry from a ResultSet using the first geometry column */
    static Geometry getGeometry(ResultSet rs) {
        return getGeometry(rs, "THE_GEOM")
    }

    /**
     * Generate SQL to create a spatial index on a geometry column.
     * H2GIS: CREATE SPATIAL INDEX IF NOT EXISTS idx ON table(col)
     * PostGIS: CREATE INDEX IF NOT EXISTS idx ON table USING GIST(col)
     */
    static String createSpatialIndex(Connection connection, String tableName, String geometryColumn) {
        String indexName = tableName + "_" + geometryColumn + "_IDX"
        if (isPostgreSQL(connection)) {
            return "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + " USING GIST(" + geometryColumn + ")"
        } else {
            return "CREATE SPATIAL INDEX IF NOT EXISTS " + indexName + " ON " + tableName + "(" + geometryColumn + ")"
        }
    }

    /**
     * Normalize table name for the target database.
     * H2GIS: uppercase (case-insensitive, stores uppercase)
     * PostGIS: lowercase (case-sensitive, unquoted identifiers are lowercased)
     *
     * This ensures consistent table name handling across databases.
     */
    static String normalizeTableName(Connection connection, String tableName) {
        if (tableName == null) {
            return null
        }
        return isPostgreSQL(connection) ? tableName.toLowerCase() : tableName.toUpperCase()
    }
}
