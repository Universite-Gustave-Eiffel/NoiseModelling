import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Calculate_Matsim_Agent_Exposure

import java.sql.Connection

class CalculateMatsimAgentExposure {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        calculateMatsimAgentExposure(connection);
    }

    public static void calculateMatsimAgentExposure(Connection connection) {
        calculateMatsimAgentExposure(connection, [
                // "folder" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01",
                "folder" : "/home/valoo/Projects/IFSTTAR/Scenarios/nantes_0.01",
                "outTableName" : "AGENTS",
                "timeSlice": "hour" // DEN, hour, quarter
        ])
    }
    public static void calculateMatsimAgentExposure(Connection connection, options) {
        println "-------------------------------"
        println "Caltulating Matsim agents exposures"
        println "-------------------------------"
        new Calculate_Matsim_Agent_Exposure().exec(connection, options)
    }
}
