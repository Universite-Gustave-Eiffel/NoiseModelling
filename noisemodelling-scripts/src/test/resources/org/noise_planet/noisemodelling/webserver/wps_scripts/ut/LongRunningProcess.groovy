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

package org.noise_planet.noisemodelling.webserver.wps_scripts.ut

import org.h2gis.api.ProgressVisitor

import java.sql.Connection

title = 'Long running process'
description = 'Long running process'

inputs = [
        waitTime: [
                name       : 'Wait time',
                title      : 'Wait time',
                description: 'Time to wait (seconds)',
                type       : Integer.class
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]

def exec(Connection connection, Map input, ProgressVisitor progressVisitor) {
    int waitTime = input.waitTime as Integer
    int count = 0
    ProgressVisitor progress = progressVisitor.subProcess(waitTime)
    while (count < waitTime) {
        Thread.sleep(1000)
        count++
        progress.endStep()
    }
    String resultString = "Waited for ${waitTime} seconds"

    // print to WPS Builder
    return resultString
}


