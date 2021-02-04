package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid

import java.sql.Connection

class CreateReceiversGrid {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/scenario_25_percent.db"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        createReceiversGrid(connection);
    }

    public static void createReceiversGrid(Connection connection) {
        createReceiversGrid(connection, [
                "delta"            : 50,
                "buildingTableName": "BUILDINGS",
        ])
    }

    public static void createReceiversGrid(Connection connection, options) {
        println "-------------------------------"
        println "Creating Receivers grid"
        println "-------------------------------"
        new Regular_Grid().exec(connection, options)
    }
}
