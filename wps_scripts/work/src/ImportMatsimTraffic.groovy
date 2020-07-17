import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Traffic_from_matsim_events

import java.sql.Connection

class ImportMatsimTraffic {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        importMatsimTraffic(connection);
    }

    public static void importMatsimTraffic(Connection connection) {
        importMatsimTraffic(connection, [
                "folder" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01",
                "outTableName" : "MATSIM_ROADS",
                "link2GeometryFile" : "network.csv", // relative path
                "timeSlice": "quarter" // DEN, hour, quarter
        ])
    }
    public static void importMatsimTraffic(Connection connection, options) {
        println "-------------------------------"
        println "Importing Matsim traffic results"
        println "-------------------------------"
        new Traffic_from_matsim_events().exec(connection, options)
    }
}
