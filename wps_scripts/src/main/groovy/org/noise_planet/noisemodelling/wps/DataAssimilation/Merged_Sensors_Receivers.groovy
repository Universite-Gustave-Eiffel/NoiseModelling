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

import java.sql.Connection


title = 'Merged Sensors and Receivers'
description = 'Adding the sensors into the RECEIVERS after creating a regular grid of receivers.'

inputs = [
        tableReceivers: [
                name: 'The receiver table',
                description: 'The receiver table',
                type: Sql.class
        ],
        tableSensors: [
                name: 'The Sensors table',
                description: 'The Sensors table ',
                type: Sql.class
        ]
]

@CompileStatic
static def exec(Connection connection,inputs) {
    String receiverTable = inputs['tableReceivers'] as String
    String tableSensors = inputs['tableSensors'] as String

    Sql sql = new Sql(connection)
    sql.execute("ALTER TABLE " + receiverTable + " DROP COLUMN ID_ROW, ID_COL")
    sql.execute("ALTER TABLE " + receiverTable + " ADD COLUMN IDNAME VARCHAR;")
    sql.execute("UPDATE " + receiverTable + "  SET IDNAME = 'REC_MAP'")

    sql.execute("INSERT INTO  " + receiverTable + "  (THE_GEOM, IDNAME) SELECT The_GEOM THE_GEOM , IDSENSOR IDNAME FROM " + tableSensors)
}