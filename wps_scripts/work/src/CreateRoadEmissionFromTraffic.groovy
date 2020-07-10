import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.NoiseModelling.Road_Emission_from_Traffic

import java.sql.Connection

class CreateRoadEmissionFromTraffic {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        createRoadEmissionFromTraffic(connection);
    }

    public static void createRoadEmissionFromTraffic(Connection connection) {
        createRoadEmissionFromTraffic(connection, [
                "tableRoads": "ROADS_7_8",
                "outTable": "ROADS_7_8_LW"
        ])
    }
    public static void createRoadEmissionFromTraffic(Connection connection, options) {
        println "-------------------------------"
        println "Creating Road Emission From Traffic"
        println "-------------------------------"
        new Road_Emission_from_Traffic().exec(connection, options)
    }
}