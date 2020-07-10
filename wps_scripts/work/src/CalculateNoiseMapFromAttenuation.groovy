import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Experimental.Noise_Map_From_Attenuation_Matrice
import org.noise_planet.noisemodelling.wps.Others_Tools.Add_Laeq_Leq_columns

import java.sql.Connection

class CalculateNoiseMapFromAttenuation {

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
                "roadsTableWithLw": "LW_ROADS",
                "attenuationTable" : "LDAY_GEOM",
                "outTableName" : "RESULT_GEOM",
        ])
    }

    public static void calculateNoiseMap(Connection connection, options) {
        println "-------------------------------"
        println "Calculate Noise Map From Attenuation Matrice - " + options.get("roadsTableWithLw")
        println "-------------------------------"
        new Noise_Map_From_Attenuation_Matrice().exec(connection, options)
        new Add_Laeq_Leq_columns().exec(connection, [
                "prefix": "HZ",
                "tableName": options.get("outTableName")
        ])
    }
}

