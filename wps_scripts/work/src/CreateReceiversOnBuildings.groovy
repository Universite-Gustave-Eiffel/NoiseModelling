import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid
import org.noise_planet.noisemodelling.wps.Receivers.Regular_Grid

import java.sql.Connection

class CreateReceiversOnBuildings {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        createReceiversOnBuildings(connection);
    }

    public static void createReceiversOnBuildings(Connection connection) {
        createReceiversOnBuildings(connection, [
                "delta"            : 5.0,
                "tableBuilding": "BUILDINGS",
                "receiversTableName": "ALL_RECEIVERS",
                "height": 4.0
        ])
    }

    public static void createReceiversOnBuildings(Connection connection, options) {
        println "-------------------------------"
        println "Creating Receivers grid"
        println "-------------------------------"
        new Building_Grid().exec(connection, options)
    }
}
