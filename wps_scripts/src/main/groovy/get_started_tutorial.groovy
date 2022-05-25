/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */


import org.h2gis.api.ProgressVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

def exec(Connection connection, input) {
    ProgressVisitor progressVisitor = input["progressVisitor"]
    Logger logger = LoggerFactory.getLogger("script")

    // Step 4: Upload files to database
    GroovyShell shell = new GroovyShell()
    Script importFile=shell.parse(new File("noisemodelling/wps/Import_and_Export/Import_File.groovy"))

    Map<String, Object> inputs = new HashMap<>()
    inputs.put("progressVisitor", progressVisitor)
    inputs.put("pathFile", "resources/org/noise_planet/noisemodelling/wps/ground_type.shp")
    Object result = importFile.invokeMethod("exec", [connection, inputs])
    if(result != null) {
        logger.info(result.toString())
    }

    inputs.put("pathFile", "resources/org/noise_planet/noisemodelling/wps/buildings.shp")
    result = importFile.invokeMethod("exec", [connection, inputs])
    if(result != null) {
        logger.info(result.toString())
    }

    inputs.put("pathFile", "resources/org/noise_planet/noisemodelling/wps/receivers.shp")
    result = importFile.invokeMethod("exec", [connection, inputs])
    if(result != null) {
        logger.info(result.toString())
    }

    inputs.put("pathFile", "resources/org/noise_planet/noisemodelling/wps/ROADS2.shp")
    result = importFile.invokeMethod("exec", [connection, inputs])
    if(result != null) {
        logger.info(result.toString())
    }

    inputs.put("pathFile", "resources/org/noise_planet/noisemodelling/wps/dem.geojson")
    result = importFile.invokeMethod("exec", [connection, inputs])
    if(result != null) {
        logger.info(result.toString())
    }

    Script noiseLevelFromTraffic=shell.parse(new File("noisemodelling/wps/NoiseModelling/Noise_level_from_traffic.groovy"))
    inputs = new HashMap<>()
    inputs.put("progressVisitor", progressVisitor)
    inputs.put("tableBuilding", "BUILDINGS")
    inputs.put("tableRoads", "ROADS2")
    inputs.put("tableReceivers", "RECEIVERS")
    inputs.put("tableDEM", "DEM")
    inputs.put("tableGroundAbs", "GROUND_TYPE")
    result = noiseLevelFromTraffic.invokeMethod("exec", [connection, inputs])
    if(result != null) {
        logger.info(result.toString())
    }

    // Step 6: Export (& see) the results
    Script exportTable=shell.parse(new File("noisemodelling/wps/Import_and_Export/Export_Table.groovy"))
    inputs = new HashMap<>()
    inputs.put("progressVisitor", progressVisitor)
    inputs.put("exportPath", "LDAY_GEOM.shp")
    inputs.put("tableToExport", "LDAY_GEOM")
    result = exportTable.invokeMethod("exec", [connection, inputs])
    if(result != null) {
        logger.info(result.toString())
    }
}

