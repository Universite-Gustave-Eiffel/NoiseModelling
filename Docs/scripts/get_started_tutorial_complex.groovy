import org.h2gis.api.ProgressVisitor
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Export_Table
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Noise_level_from_traffic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection

title = 'Tutorial script'
description = 'Long description of tutorial script'

inputs = [:]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'Result table name. Can be used as input for another WPS process', type: String.class]]

def exec(Connection connection, Map input, ProgressVisitor progress) {
    Logger logger = LoggerFactory.getLogger("tutorial")
    ProgressVisitor tutorialProgress = progress.subProcess(7)
    // 7 steps in this task

    // Upload files to database
    new Import_File().exec(connection, ["pathFile": "resources/ground_type.shp"], tutorialProgress)

    new Import_File().exec(connection, ["pathFile": "resources/buildings.shp"], tutorialProgress)

    new Import_File().exec(connection, ["pathFile": "resources/receivers.shp"], tutorialProgress)

    new Import_File().exec(connection, ["pathFile": "resources/ROADS2.shp"], tutorialProgress)

    new Import_File().exec(connection, ["pathFile": "resources/dem.geojson"], tutorialProgress)

    // Run Calculation
    new Noise_level_from_traffic().exec(connection, ["tableBuilding": "BUILDINGS", "tableRoads": "ROADS2", "tableReceivers": "RECEIVERS",
                                                     "tableDEM"     : "DEM", "tableGroundAbs": "GROUND_TYPE"], tutorialProgress)

    // Export the results in a file
    new Export_Table().exec(connection, ["exportPath": "RECEIVERS_LEVEL.shp", "tableToExport": "RECEIVERS_LEVEL"], tutorialProgress)

    logger.info("Result have been exported to " + new File("RECEIVERS_LEVEL.shp").getAbsolutePath())
}
