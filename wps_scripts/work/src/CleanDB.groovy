import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Database_Manager.Clean_Database

import java.sql.Connection

class CleanDB {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        cleanDB(connection);
    }

    public static void cleanDB(Connection connection) {
        println "-------------------------------"
        println "Cleaning Database"
        println "-------------------------------"
        new Clean_Database().exec(connection, [
                "areYouSure": true
        ])
    }
}