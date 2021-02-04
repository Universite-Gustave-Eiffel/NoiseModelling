package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Sources_From_TimeString

import java.sql.Connection

class CreateSourcesFromTimeString {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        createSourcesFromTimeString(connection);
    }

    public static void createSourcesFromTimeString(Connection connection) {
        createSourcesFromTimeString(connection, [
                "roadsTableName" : "MATSIM_ROADS",
                "statsTableName" : "MATSIM_ROADS_STATS",
                "timeString" : "0_1",
                "outTableName" : "ROADS_0_1",
                "computeLw": true
        ])
    }

    public static void createSourcesFromTimeString(Connection connection, options) {
        String timeString = options.get("timeString");
        println "-------------------------------"
        println "Getting roads for " + timeString
        println "-------------------------------"
        new Sources_From_TimeString().exec(connection, options)
    }
}

