package org.noise_planet.noisemodelling.scripts

import org.h2gis.api.ProgressVisitor
import org.noise_planet.noisemodelling.scripts.Import_and_Export.Import_File
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Noise_level_from_source
import org.noise_planet.noisemodelling.scripts.NoiseModelling.Road_Emission_from_Traffic
import org.noise_planet.noisemodelling.webserver.utilities.Logging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.sql.Sql
import java.io.File

import java.sql.Connection

title = 'Tutorial script'
description = 'Long description of tutorial script'

inputs = [ resourcesFolder : [
        description: "Path of the resource folder for input data",
        title: "Resource folder",
        default: 'resources',
        type: String.class
]]

outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'Result table name. Can be used as input for another WPS process', type: String.class]]

def exec(Connection connection, Map input, ProgressVisitor progress) {
    Logger logger = LoggerFactory.getLogger("tutorial")
    ProgressVisitor tutorialProgress = progress.subProcess(7)
    // 7 steps in this task

    def resourceFolder = input.resourcesFolder as String
    // Upload files to database
    def groundTable = new Import_File().exec(connection, ["pathFile": new File(resourceFolder, "ground_type.shp")], tutorialProgress)["outputTable"]

    def buildingTable = new Import_File().exec(connection, ["pathFile": new File(resourceFolder, "buildings.shp")], tutorialProgress)["outputTable"]

    def receiversTable = new Import_File().exec(connection, ["pathFile": new File(resourceFolder, "receivers.shp")], tutorialProgress)["outputTable"]

    def roadsTable = new Import_File().exec(connection, ["pathFile": new File(resourceFolder, "ROADS2.shp")], tutorialProgress)["outputTable"]

    def demTable = new Import_File().exec(connection, ["pathFile": new File(resourceFolder, "dem.geojson")], tutorialProgress)["outputTable"]

    def roadEmissionTable = new Road_Emission_from_Traffic().exec(connection, [tableRoads : roadsTable]).result

    // print some lines of road emission
    Logging.formatSqlQueryResult(new Sql(connection), "SELECT * FROM $roadEmissionTable LIMIT 10" as String)

    // Run Calculation
    def resultTable = new Noise_level_from_source().exec(connection, ["tableBuilding": buildingTable, "tableSources": roadEmissionTable, "tableReceivers": receiversTable,
                                                                      "tableDEM"     : demTable, "tableGroundAbs": groundTable], tutorialProgress)

    // Return results
    return Logging.formatSqlQueryResult(new Sql(connection), "SELECT * FROM $resultTable.result ORDER BY IDRECEIVER, PERIOD LIMIT 10" as String, 120)
}
