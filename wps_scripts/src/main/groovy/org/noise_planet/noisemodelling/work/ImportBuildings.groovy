package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Import_and_Export.Osm_Pbf_to_Buildings

import java.sql.Connection

class ImportBuildings {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        importBuildings(connection);
    }

    public static void importBuildings(Connection connection) {
        importBuildings(connection, [
                // "pathFile"        : "C:\\Users\\valen\\Documents\\IFSTTAR\\OsmMaps\\proce-plus.pbf",
                "pathFile"        : "/home/valoo/Projects/IFSTTAR/OsmMaps/nantes.pbf",
                "targetSRID"      : 2154
        ])
    }
    public static void importBuildings(Connection connection, options) {
        println "-------------------------------"
        println "Importing Buildings from Osm"
        println "-------------------------------"
        new Osm_Pbf_to_Buildings().exec(connection, options)
    }
}