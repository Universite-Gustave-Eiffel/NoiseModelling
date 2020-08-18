import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.wps.Matsim.Calculate_Noise_Map_Difference

import java.sql.Connection

class CalculateMapDifference {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
        calculateDifference(connection);
    }

    public static void calculateDifference(Connection connection) {
        calculateDifference(connection, [
                "mainMapTable" : "ALT_RESULT_GEOM_13H45_14H00",
                "secondMapTable" : "RESULT_GEOM_13H45_14H00",
                "invert" : true,
                "outTable" : "DIFF_RESULT_GEOM_13H45_14H00",
        ])
    }
    public static void calculateDifference(Connection connection, options) {
        println "-------------------------------"
        println "Calculating Noise Map Diff :  " + options.get("outTable")
        println "-------------------------------"
        new Calculate_Noise_Map_Difference().exec(connection, options)
    }
}
