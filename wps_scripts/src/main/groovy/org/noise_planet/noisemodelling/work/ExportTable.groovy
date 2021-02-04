package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Import_and_Export.Export_Table

import java.sql.Connection

class ExportTable {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "file:///home/valoo/Projects/IFSTTAR/Scenarios/output_entd_25p/nantes_25p/noise_modelling.db"

        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));
        exportTable(connection);
    }

    public static void exportTable(Connection connection) {
        exportTable(connection, [
                "tableToExport" : "TMP_SCREENS_MERGE",
                // "exportPath": "C:\\Users\\valen\\Documents\\IFSTTAR\\Results\\receivers.shp"
                "exportPath": "/home/valoo/Projects/IFSTTAR/OsmMaps/nantes_line_merge.shp"
        ])
    }
    public static void exportTable(Connection connection, options) {
        println "-------------------------------"
        println "Exporting Table " + options.get("tableToExport")
        println "-------------------------------"
        new Export_Table().exec(connection, options)
    }
}
