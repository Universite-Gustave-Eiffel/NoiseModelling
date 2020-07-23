import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table

import java.sql.Connection

class ExportTable {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        exportTable(connection);
    }

    public static void exportTable(Connection connection) {
        exportTable(connection, [
                "tableToExport" : "AGENTS",
                // "exportPath": "C:\\Users\\valen\\Documents\\IFSTTAR\\Results\\receivers.shp"
                "exportPath": "/home/valoo/Projects/IFSTTAR/Results/agents.geojson"
        ])
    }
    public static void exportTable(Connection connection, options) {
        println "-------------------------------"
        println "Exporting Table " + options.get("tableToExport")
        println "-------------------------------"
        new Export_Table().exec(connection, options)
    }
}
