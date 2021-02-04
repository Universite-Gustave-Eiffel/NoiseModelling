package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Receivers_From_Activities_Closest

import java.sql.Connection

class AssignClosestReceviersToActivities {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/scenario_25_percent.db"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        activitiesRandomReceiver(connection);
    }

    public static void activitiesRandomReceiver(Connection connection) {
        activitiesRandomReceiver(connection, [
                "activitiesTable": "ACTIVITIES",
                "receiversTable": "ALL_RECEIVERS",
                "outTableName": "ACTIVITIES_RECEIVERS"
        ])
    }

    public static void activitiesRandomReceiver(Connection connection, options) {
        println "-------------------------------"
        println "Creating Receivers grid"
        println "-------------------------------"
        new Receivers_From_Activities_Closest().exec(connection, options)
    }
}
