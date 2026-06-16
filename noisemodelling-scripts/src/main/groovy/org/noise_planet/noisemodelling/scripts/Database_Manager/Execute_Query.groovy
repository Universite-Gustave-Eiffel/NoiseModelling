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


package org.noise_planet.noisemodelling.scripts.Database_Manager


import com.fasterxml.jackson.databind.ObjectWriter
import groovy.sql.Sql
import org.h2.util.ScriptReader
import org.h2.util.StringUtils
import org.h2gis.api.ProgressVisitor
import org.noise_planet.noisemodelling.jdbc.output.NoiseMapWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Run SQL queries and display the results'
description = '&#10145;&#65039; Run multiple SQL queries and display the results.</br>'

inputs = [sqlQueries  : [name       : 'SQL queries',
                        title      : 'SQL queries',
                        description: 'SQL queries (e.g., <code>CREATE TABLE mytable AS SELECT * FROM othertable; SELECT * FROM mytable;</code>)',
                        type       : String.class],
          outputFormat: [name       : 'Output format',
                        title      : 'Output format',
                        description: 'Choose the output format for the result.',
                        type       : String.class,
                        default    : 'HTML',
                        allowedValues : ['HTML', 'JSON']]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'The output of the SQL query execution, formatted according to the selected output format.',
                type       : String.class
        ]
]

def exec(Connection connection, Map input, ProgressVisitor progress) {

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    final StringBuilder outputData = new StringBuilder()
    Sql sql = new Sql(connection)
    ObjectWriter writer = NoiseMapWriter.createJsonWriter()
    def exportInHTML = !input.containsKey("outputFormat") || "HTML".equalsIgnoreCase(input.outputFormat as String)

    List<String> statementList = new LinkedList<>()
    ScriptReader scriptReader = new ScriptReader(new StringReader(input.sqlQueries as String))
    scriptReader.setSkipRemarks(true)

    String statement = scriptReader.readStatement()
    while (statement != null && !StringUtils.isWhitespaceOrEmpty(statement)) {
        statementList.add(statement)
        statement = scriptReader.readStatement()
    }
    ProgressVisitor subProgress = progress.subProcess(statementList.size())
    for(final String query in statementList) {
        logger.info("Executing query: ${query}")
        long startTime = System.currentTimeMillis()
        sql.execute(query, { isResultSet, result ->
            if(isResultSet) {
                if (exportInHTML) {
                    outputData.append(Table_Visualization_Data.mapToTable(result as List, query, connection, false))
                } else {
                    if (outputData.isEmpty()) {
                        outputData.append("[\n")
                    } else {
                        outputData.append(", \n")
                    }
                    outputData.append(writer.writeValueAsString([query: query, result: result]))
                }
            } else {
                if(exportInHTML) {
                    outputData.append("<p>SQL Query: <code>${query}</code></p><p>Updated ${result as Integer} ${result > 1 ? 'rows' : 'row'} in ${System.currentTimeMillis() - startTime} ms</p>")
                }
            }
        })
        subProgress.endStep()
    }
    if(!exportInHTML) {
        if (outputData.isEmpty()) {
            outputData.append("[\n")
        }
        outputData.append("]")
    }

    return [result : outputData.toString()]

}
