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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Dynamic Road Traffic Emission'
description = 'Creation of the dynamic road using best configurations'

outputs = [
        result: [
                name: 'Dynamic Road Table',
                description: 'Receiver table created ',
                type: Sql.class
        ]
]
@CompileStatic
static def exec(Connection connection){
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Traffic calibration')

    Sql sql = new Sql(connection)

    sql.execute("DROP TABLE LW_ROADS_best IF EXISTS")
    // Create the DYNAMIC_ROADS table and populate it with dynamic road data by varying the traffic with the best configuration.
    sql.execute("CREATE TABLE LW_ROADS_best AS SELECT distinct r.* FROM LW_ROADS r, BEST_CONFIGURATION_FULL c WHERE c.PERIOD = r.PERIOD")

    // Add Z dimension to the road segments
    sql.execute("CREATE INDEX ON LW_ROADS_best(IDSOURCE, PERIOD)")

    logger.info('End Traffic calibration')
}




