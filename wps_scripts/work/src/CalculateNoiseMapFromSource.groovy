import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.NoiseModelling.Noise_level_from_source

import java.sql.Connection

class CalculateNoiseMapFromSource {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        calculateNoiseMap(connection);
    }

    public static void calculateNoiseMap(Connection connection) {
        calculateNoiseMap(connection, [
                "tableBuilding": "BUILDINGS",
                "tableReceivers" : "RECEIVERS",
                "tableSources" : "SOURCES_0DB",
                "confMaxSrcDist": 750,
                "confMaxReflDist": 50,
                "confReflOrder": 1,
                "confSkipLevening": true,
                "confSkipLnight": true,
                "confSkipLden": true,
                "confThreadNumber": 14,
                "confExportSourceId": true,
                "confDiffVertical": true,
                "confDiffHorizontal": true
        ])
    }

    public static void calculateNoiseMap(Connection connection, options) {
        println "-------------------------------"
        println "Calculate Noise Map From Source - " + options.get("tableSources")
        println "-------------------------------"
        new Noise_level_from_source().exec(connection, options)
    }
}

