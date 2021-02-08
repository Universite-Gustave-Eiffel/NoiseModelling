package org.noise_planet.noisemodelling.work

import org.h2gis.functions.factory.H2GISDBFactory
import org.h2gis.utilities.SFSUtilities
import org.noise_planet.noisemodelling.wps.Experimental_Matsim.Traffic_From_Events

import java.sql.Connection

class ImportMatsimTraffic {

    public static void main(String[] args) {

        Connection connection;
        String dbName = "h2gisdb"
        connection = SFSUtilities.wrapConnection(H2GISDBFactory.openSpatialDataBase(dbName));

        importMatsimTraffic(connection);
    }

    public static void importMatsimTraffic(Connection connection) {
        importMatsimTraffic(connection, [
                // "folder" : "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\nantes_0.01",
                "folder" : "/home/valoo/Projects/IFSTTAR/eqasim-nantes/output_0.25/simulation_output",
                "outTableName" : "MATSIM_ROADS",
                "link2GeometryFile" : "network.csv", // relative path
                "timeSlice": "quarter", // DEN, hour, quarter
                "skipUnused": "true",
                //"ignoreAgents": "464936, 385804, 14029, 14027, 14026, 14028, 54174, 60249, 498240, 45774, 45775, 45776, 45048, 45049, 45715, 17974, 17972, 17973, 114131, 114130, 116206, 116207, 413345, 484682, 484681, 388796, 3560, 3561, 3562, 3563, 3564, 11145, 11146, 11147, 11148, 138573, 138571, 138572, 138574, 292303, 292302, 292304, 15165, 310899, 390518, 390519, 390517, 141430, 57869, 133712, 133713, 133711, 212389, 71185, 71186, 11314, 11313, 66492, 113753, 69668, 69669, 69670, 313576, 71197, 293806, 293807, 293808, 393224, 57624, 77862, 475450, 475449, 475451, 279665, 279664, 279663, 279666, 25194, 504990, 93913, 93912, 93911, 14616, 14617, 237707, 237704, 237706, 237705, 389869, 389870, 389871, 389872, 409795, 62692, 314322, 487079, 502354, 502355, 52385, 52386, 348906, 348905, 54852, 54851, 379569, 379568, 402402, 402403, 402404, 402401, 22183, 406400, 278219, 162296, 162297, 28573, 28574, 28575, 28576, 195546, 154648, 154649, 509829, 32069, 32070, 292008, 403357, 403355, 403358, 403356, 35654, 505445, 505444, 424886, 202302, 208063, 237085, 211359, 211358, 211357, 50297, 237233, 237232, 355628, 355627, 311117, 311118, 342970, 59013, 100038, 63603, 336090, 209902, 209905, 209903, 209904, 154230, 474732, 125104, 131856, 135692, 195936, 195935, 212623, 422393, 252609, 436862, 409223"
                "ignoreAgents": "",
                "perVehicleLevel": "true",
                "populationFactor": "0.25"
        ])
    }
    public static void importMatsimTraffic(Connection connection, options) {
        println "-------------------------------"
        println "Importing Matsim traffic results"
        println "-------------------------------"
        new Traffic_From_Events().exec(connection, options)
    }
}
