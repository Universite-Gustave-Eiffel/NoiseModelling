import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Plot_Exposition_Distribution

import java.sql.Connection

class PlotMatsimAgentsExposure {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        plotMatsimAgentsExposure(connection);
    }

    public static void plotMatsimAgentsExposure(Connection connection) {
        plotMatsimAgentsExposure(connection, [
                "expositionsTableName" : "AGENTS",
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
