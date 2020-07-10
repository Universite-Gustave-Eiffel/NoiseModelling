import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid

import java.sql.Connection

class CreateReceiversGrid {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        createReceiversGrid(connection);
    }

    public static void createReceiversGrid(Connection connection) {
        createReceiversGrid(connection, [
                "delta"            : 20,
                "buildingTableName": "BUILDINGS",
        ])
    }

    public static void createReceiversGrid(Connection connection, options) {
        println "-------------------------------"
        println "Creating Receivers grid"
        println "-------------------------------"
        new Regular_Grid().exec(connection, options)
    }
}
