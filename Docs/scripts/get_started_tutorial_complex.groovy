import org.h2gis.api.ProgressVisitor
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Noise_level_from_traffic
import org.noise_planet.noisemodelling.webserver.utilities.Logging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.sql.Sql

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
    def groundTable = new Import_File().exec(connection, ["pathFile": "resources/ground_type.shp"], tutorialProgress)["outputTable"]

    def buildingTable = new Import_File().exec(connection, ["pathFile": "resources/buildings.shp"], tutorialProgress)["outputTable"]

    def receiversTable = new Import_File().exec(connection, ["pathFile": "resources/receivers.shp"], tutorialProgress)["outputTable"]

    def roadsTable = new Import_File().exec(connection, ["pathFile": "resources/ROADS2.shp"], tutorialProgress)["outputTable"]

    def demTable = new Import_File().exec(connection, ["pathFile": "resources/dem.geojson"], tutorialProgress)["outputTable"]

    // Run Calculation
    def resultTable = new Noise_level_from_traffic().exec(connection, ["tableBuilding": buildingTable, "tableRoads": roadsTable, "tableReceivers": receiversTable,
                                                                       "tableDEM"     : demTable, "tableGroundAbs": groundTable], tutorialProgress)

    // Return results
    return Logging.formatSqlQueryResult(new Sql(connection), "SELECT * FROM $resultTable.result LIMIT 5" as String, 120)
}
