package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Plot_Exposition_Distribution

import java.sql.Connection

class PlotMatsimAgentsExposure {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/scenario_25_percent.db"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        plotMatsimAgentsExposure(connection);
    }

    public static void plotMatsimAgentsExposure(Connection connection) {
        plotMatsimAgentsExposure(connection, [
                "expositionsTableName" : "EXPOSURES",
                "expositionField" : "LAEQ",
                "otherExpositionField" : "HOME_LAEQ",
        ])
    }
    public static void plotMatsimAgentsExposure(Connection connection, options) {
        println "-------------------------------"
        println "Plotting Matsim agents exposures"
        println "-------------------------------"
        new Plot_Exposition_Distribution().exec(connection, options)
    }
}
