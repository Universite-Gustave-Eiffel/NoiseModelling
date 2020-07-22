import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Matsim.Chose_Receivers_From_Matsim_Activities
import org.noise_planet.noisemodelling.wps.Receivers.Building_Grid

import java.sql.Connection

class ChoseReceiversFromActivities {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        choseReceiversFromActivities(connection);
    }

    public static void choseReceiversFromActivities(Connection connection) {
        choseReceiversFromActivities(connection, [
                "activitiesTable": "ACTIVITIES",
                "receiversTableName": "ALL_RECEIVERS",
                "outTableName": "RECEIVERS"
        ])
    }

    public static void choseReceiversFromActivities(Connection connection, options) {
        println "-------------------------------"
        println "Creating Receivers grid"
        println "-------------------------------"
        new Chose_Receivers_From_Matsim_Activities().exec(connection, options)
    }
}
