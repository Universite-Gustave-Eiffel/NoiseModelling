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

package org.noise_planet.noisemodelling.wps.DataAssimilation

import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.h2gis.utilities.JDBCUtilities

import java.sql.Connection


title = 'Creation of the result table'
description = 'Creation of the result table.'

inputs = [
        bestConfigTable: [
                name: 'The best configuration table',
                description: 'The best configuration table',
                type: String.class
        ],
        receiverLevel: [
                name: 'The receivers Level table',
                description: 'The receivers Level table ',
                type:  String.class
        ],
        outputTable: [
                name: 'The results table',
                description: 'The results table',
                type: String.class
        ]
]

@CompileStatic
static def exec(Connection connection,inputs) {
    String bestConfigTable = inputs['bestConfigTable'] as String
    String receiverLevel = inputs['receiverLevel'] as String
    String outputTable = inputs['outputTable'] as String

    Sql sql = new Sql(connection)
    // Add Timestamp to the NMs
    sql.execute("DROP TABLE "+outputTable+" IF EXISTS;")
    sql.execute("CREATE TABLE "+outputTable+" AS SELECT b.T TIMESTAMP, b.IT IMAP, a.LAEQ, a.THE_GEOM, a.IDRECEIVER FROM "+bestConfigTable+" b  LEFT JOIN "+receiverLevel+" a ON a.PERIOD = b.PERIOD ; ")
    sql.execute("ALTER TABLE "+outputTable+" ALTER COLUMN TIMESTAMP SET DATA TYPE INTEGER")

    def columnNames = JDBCUtilities.getColumnNames(connection, outputTable)

    columnNames.containsAll(Arrays.asList("IMAP", "LAEQ"))


}
