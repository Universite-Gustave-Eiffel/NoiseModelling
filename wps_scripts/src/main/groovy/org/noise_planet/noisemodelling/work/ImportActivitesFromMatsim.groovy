package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Import_Activities

import java.sql.Connection

class ImportActivitesFromMatsim {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/Scenarios/output_entd_25p/nantes_25p/noise_modelling.db"
        // String dbFilePath = (new File(dbName)).getAbsolutePath();
        // connection = DriverManager.getConnection("jdbc:h2:" + dbFilePath + ";LOCK_MODE=0;LOG=0;DB_CLOSE_DELAY=5", "sa", "sa");
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        importActivitesFromMatsim(connection);
    }

    public static void importActivitesFromMatsim(Connection connection) {
        importActivitesFromMatsim(connection, [
                // "facilitiesPath" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01\\nantes_facilities.xml.gz",
                "facilitiesPath" : "/home/valoo/Projects/IFSTTAR/Scenarios/output_entd_25p/nantes_25p/simulation_output/output_facilities.xml.gz",
                "SRID" : 2154,
                "outTableName" : "ACTIVITIES"
        ])
    }

    public static void importActivitesFromMatsim(Connection connection, options) {
        println "-------------------------------"
        println "Creating Receivers from Matsim's facilities file"
        println "-------------------------------"
        new Import_Activities().exec(connection, options)
    }
}

