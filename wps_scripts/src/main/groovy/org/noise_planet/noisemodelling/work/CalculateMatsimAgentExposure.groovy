package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Agent_Exposure

import java.sql.Connection

class CalculateMatsimAgentExposure {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/scenario_25_percent.db"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
        calculateMatsimAgentExposure(connection);
    }

    public static void calculateMatsimAgentExposure(Connection connection) {
        calculateMatsimAgentExposure(connection, [
                // "folder" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01",
                "plansFile" : "/home/valoo/Projects/IFSTTAR/eqasim-nantes/output_0.25/simulation_output/output_plans.xml.gz",
                "receiversTable": "ACTIVITIES_RECEIVERS",
                "outTableName" : "EXPOSURES",
                "dataTable": "RESULT_GEOM",
                "timeSlice": "quarter", // DEN, hour, quarter,
                "plotOneAgentId": 162448,
                // "plotOneAgentId": 228798,
        ])
    }

    public static void calculateMatsimAgentExposure(Connection connection, options) {
        println "-------------------------------"
        println "Caltulating Matsim agents exposures"
        println "-------------------------------"
        new Agent_Exposure().exec(connection, options)
    }
}
