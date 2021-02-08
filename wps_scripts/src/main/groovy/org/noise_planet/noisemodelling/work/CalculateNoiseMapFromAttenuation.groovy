package org.noise_planet.noisemodelling.work

import groovy.sql.Sql
import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Noise_From_Attenuation_Matrix

import java.sql.Connection

class CalculateNoiseMapFromAttenuation {

    public static void main(String[] args) {
        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/scenario_25_percent.db"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
        calculateNoiseMap(connection);
    }

    public static void calculateNoiseMap(Connection connection) {
        calculateNoiseMap(connection, [
            "matsimRoads": "MATSIM_ROADS",
            "matsimRoadsStats": "MATSIM_ROADS_STATS",
            "attenuationTable" : "LDAY_GEOM",
            // "timeString": "6h30_6h45",
            "outTableName" : "RESULT_GEOM",
        ])
    }

    public static void calculateNoiseMap(Connection connection, options) {
        println "-------------------------------"
        println "Calculate Noise Map From Attenuation Matrice - " + options.get("timeString")
        println "-------------------------------"
        new Noise_From_Attenuation_Matrix().exec(connection, options)
    }
}

