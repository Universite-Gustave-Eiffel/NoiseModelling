package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Database_Manager.Clean_Database

import java.sql.Connection

class CleanDB {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        cleanDB(connection);
    }

    public static void cleanDB(Connection connection) {
        println "-------------------------------"
        println "Cleaning Database"
        println "-------------------------------"
        new Clean_Database().exec(connection, [
                "areYouSure": true
        ])
    }
}