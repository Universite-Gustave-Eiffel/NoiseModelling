package org.noise_planet.nmtutorial01;

import org.h2.Driver;
import org.h2gis.functions.factory.H2GISFunctions;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DbUtilities {


    private static String getDataBasePath(String dbName) {
        return dbName.startsWith("file:/") ? (new File(URI.create(dbName))).getAbsolutePath() : (new File(dbName)).getAbsolutePath();
    }


    static Connection createSpatialDataBase(String dbName, boolean initSpatial) throws SQLException {
        String dbFilePath = getDataBasePath(dbName);
        File dbFile = new File(dbFilePath + ".mv.db");

        String databasePath = "jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5";

        if (dbFile.exists()) {
            dbFile.delete();
        }

        dbFile = new File(dbFilePath + ".mv.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
        Driver.load();
        Connection connection = DriverManager.getConnection(databasePath, "sa", "sa");
        if (initSpatial) {
            H2GISFunctions.load(connection);
        }

        return connection;
    }
}
