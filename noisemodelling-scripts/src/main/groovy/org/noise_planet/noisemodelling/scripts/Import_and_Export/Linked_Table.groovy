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
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.scripts.Import_and_Export

import org.h2gis.api.ProgressVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection

title = 'Linked Table'
description = '&#10145;&#65039; Create a table into the database linked to an external database. The data is not stored into the database'

inputs = [
        localTableName: [
                name       : 'Local table name',
                title      : 'Name of created table',
                description: 'Name of the local linked table.',
                type       : String.class
        ],
        driverClass: [
                name       : 'Driver name',
                title      : 'Driver name',
                description: 'Name of the class to connect to the external database.',
                allowedValues: ["org.h2gis.postgis_jts.Driver", "org.h2.Driver"],
                default      : "org.h2gis.postgis_jts.Driver",
                type         : String.class
        ],
        databaseUrl: [
                name       : 'Database URL',
                title      : 'Database URL',
                description: 'Connection url of the database. ' +
                             'For PostGIS <pre>jdbc:postgresql_h2://hostname:5432/databaseName</pre>. </br>' +
                             'For H2 <pre>jdbc:h2:tcp://localhost/D:/data/test</pre>',
                type       : String.class
        ],
        username: [
                name       : 'User name',
                title      : 'User name',
                description: 'User name when connecting to the external database',
                type       : String.class
        ],
        password: [
                name       : 'User password',
                title      : 'User password',
                description: 'User password when connecting to the external database',
                type       : String.class
        ],
        remoteSchemaName: [
                name       : 'External table schema',
                title      : 'External table schema',
                description: 'External Table Schema ex: public',
                default: 'public',
                type       : String.class
        ],
        remoteTableName: [
                name       : 'External table name',
                title      : 'External table name',
                description: 'External Table name or query. If a query is used instead of the original table name, then' +
                             ' the table is read only. Queries must be enclosed in parenthesis: (SELECT * FROM ORDERS).',
                type       : String.class
        ],
        force: [
                name       : 'Force',
                title      : 'Force',
                description: 'Create the LINKED TABLE even if the remote database/table does not exist.',
                min: 0,
                max: 1,
                type       : Boolean.class
        ],
        fetchSize: [
                name       : 'Fetch size',
                title      : 'Fetch size',
                description: 'the number of rows fetched, a hint with non-negative number of rows to fetch from' +
                        ' the external table at once, may be ignored by the driver of external database.' +
                        ' 0 is default and means no hint. The value is passed to java.sql.Statement.setFetchSize() method.',
                default: 0,
                type       : Integer.class
        ]
]

outputs = [
        result: [
                name       : 'Local table name',
                title      : 'Local table name',
                description: 'The name of the local linked table, can be used as an input for another process',
                type       : String.class
        ]
]

def exec(Connection connection, Map input, ProgressVisitor progress) {

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    def fetchSizeStatement = ""
    if (input['fetchSize'] && input['fetchSize'] instanceof Integer) {
        fetchSizeStatement = " FETCH_SIZE " + (input['fetchSize'] as Integer)
    }

    def localTableName = input['localTableName'] as String
    def driverClass = input.getOrDefault('driverClass', 'org.h2gis.postgis_jts.Driver')
    def databaseUrl = input['databaseUrl'] as String
    def username = input['username'] as String
    def password = input['password'] as String
    def remoteSchemaName = input['remoteSchemaName'] ? (input['remoteSchemaName'] as String) : "public"
    def remoteTableName = input['remoteTableName'] as String
    def force = input['force'] ? "FORCE" : ""

    logger.info("Create linked table $localTableName with the server $databaseUrl")

    connection.createStatement().with {
        execute("""CREATE $force LINKED TABLE $localTableName(${enquoteLiteral(driverClass)}, ${
            enquoteLiteral(databaseUrl)}, ${enquoteLiteral(username)}, ${enquoteLiteral(password)}, ${
            enquoteLiteral(remoteSchemaName)}, ${enquoteLiteral(remoteTableName)}) $fetchSizeStatement;""".toString())
        close()
    }

    def result = "Table $localTableName created"
    logger.info(result)

    return [result: result]
}

