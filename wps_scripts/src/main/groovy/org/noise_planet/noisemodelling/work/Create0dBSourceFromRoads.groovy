package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Geometric_Tools.ZerodB_Source_From_Roads

import java.sql.Connection

class Create0dBSourceFromRoads {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/scenario_25_percent.db"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        create0dBSourceFromRoads(connection);
    }

    public static void create0dBSourceFromRoads(Connection connection) {
        create0dBSourceFromRoads(connection, [
                "roadsTableName": "MATSIM_ROADS",
                "sourcesTableName": "SOURCES_0DB"
        ])
    }

    public static void create0dBSourceFromRoads(Connection connection, options) {
        println "-------------------------------"
        println "Creating Roads with 0dB Lw"
        println "-------------------------------"
        new ZerodB_Source_From_Roads().exec(connection, options)
    }
}

