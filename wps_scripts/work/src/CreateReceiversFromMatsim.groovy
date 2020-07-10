
import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Receivers_From_Matsim

import java.sql.Connection

class CreateReceiversFromMatsim {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        createReceiversFromMatsim(connection);
    }

    public static void createReceiversFromMatsim(Connection connection) {
        createReceiversFromMatsim(connection, [
                "facilitiesPath" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01\\nantes_facilities.xml.gz",
                "filter" : "*",
                "outTableName" : "RECEIVERS"
        ])
    }

    public static void createReceiversFromMatsim(Connection connection, options) {
        println "-------------------------------"
        println "Creating Receivers from Matsim's facilities file"
        println "-------------------------------"
        new Receivers_From_Matsim().exec(connection, options)
    }
}

