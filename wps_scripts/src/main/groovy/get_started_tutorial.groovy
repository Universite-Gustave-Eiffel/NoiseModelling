/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps
 * on very large urban areas. It can be used as a Java library or be controlled through
 * a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the
 * Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this
 * License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

import org.h2gis.api.ProgressVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection

title = 'Tutorial script'
description = 'Long description of tutorial script'

inputs = []

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]


def runScript(connection, scriptFile, arguments) {
    Logger logger = LoggerFactory.getLogger("script")
    GroovyShell shell = new GroovyShell()
    Script scriptInstance = shell.parse(new File(scriptFile))
    Object result = scriptInstance.invokeMethod("exec", [connection, arguments])
    if(result != null) {
        logger.info(result.toString())
    }
}

def exec(Connection connection, input) {

    // Step 4: Upload files to database
    runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
            ["pathFile":"resources/org/noise_planet/noisemodelling/wps/ground_type.shp"])

    runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
            ["pathFile":"resources/org/noise_planet/noisemodelling/wps/buildings.shp"])

    runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
            ["pathFile":"resources/org/noise_planet/noisemodelling/wps/receivers.shp"])

    runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
            ["pathFile":"resources/org/noise_planet/noisemodelling/wps/ROADS2.shp"])

    runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
            ["pathFile":"resources/org/noise_planet/noisemodelling/wps/dem.geojson"])

    // Step 5: Run Calculation
    runScript(connection, "noisemodelling/wps/NoiseModelling/Noise_level_from_traffic.groovy",
            ["tableBuilding":"BUILDINGS", "tableRoads":"ROADS2", "tableReceivers":"RECEIVERS",
             "tableDEM":"DEM", "tableGroundAbs":"GROUND_TYPE"])

    // Step 6: Export (& see) the results
    runScript(connection, "noisemodelling/wps/Import_and_Export/Export_Table.groovy",
            ["exportPath":"LDAY_GEOM.shp", "tableToExport":"LDAY_GEOM"])
}
