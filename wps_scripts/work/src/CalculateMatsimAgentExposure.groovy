import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Calculate_Matsim_Agent_Exposure

import java.sql.Connection

class CalculateMatsimAgentExposure {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        calculateMatsimAgentExposure(connection);
    }

    public static void calculateMatsimAgentExposure(Connection connection) {
        calculateMatsimAgentExposure(connection, [
                // "folder" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01",
                "folder" : "/home/valoo/Projects/IFSTTAR/Scenarios/nantes_0.01",
                "outTableName" : "EXPOSURES",
                "dataTablePrefix": "RESULT_GEOM_",
                "timeSlice": "quarter", // DEN, hour, quarter,
                "plotOneAgentId": 0,
        ])
    }
    public static void calculateMatsimAgentExposure(Connection connection, options) {
        println "-------------------------------"
        println "Caltulating Matsim agents exposures"
        println "-------------------------------"
        new Calculate_Matsim_Agent_Exposure().exec(connection, options)
    }
}
