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
 */

package org.noise_planet.noisemodelling.scripts.Acoustic_Tools

import groovy.sql.Sql
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.util.regex.Matcher
import java.util.regex.Pattern

title = 'Combine two noise maps'
description = '&#10145;&#65039; Combines two receiver level tables (same receiver grid) by computing either their difference or their energetic sum.' +
              '<hr>' +
              'Each table must contain <b>IDRECEIVER</b>, <b>THE_GEOM</b> and at least one common level column ' +
              '(<b>HZ63</b>, <b>HZ125</b>, ... or <b>LAEQ</b>, <b>LEQA</b>, <b>LEQ</b>).' +
              '<br/><br/>' +
              'Difference : <b>firstTable - secondTable</b> (in dB).' +
              '<br/>' +
              'Energetic sum : <b>10 * log10(10^(firstTable/10) + 10^(secondTable/10))</b>.' +
              '<br/><br/>' +
              'Periods are never aggregated : the output contains one row per <b>(IDRECEIVER, PERIOD)</b>.'

inputs = [
        firstTable      : [
                name       : 'First table',
                title      : 'First table',
                description: 'Name of the first receiver level table. (STRING)</br> </br>' +
                             'Example : RECEIVERS_LEVEL_BEFORE',
                type       : String.class
        ],
        secondTable     : [
                name       : 'Second table',
                title      : 'Second table',
                description: 'Name of the second receiver level table. It must use the same receiver grid as the first table. (STRING)</br> </br>' +
                             'Example : RECEIVERS_LEVEL_AFTER',
                type       : String.class
        ],
        operation       : [
                name       : 'Operation',
                title      : 'Operation',
                description: 'Operation to apply :</br>' +
                             '<ul>' +
                             '<li><b>difference</b> : first table - second table (default)</li>' +
                             '<li><b>energetic_sum</b> : energetic sum of the two tables</li>' +
                             '</ul>',
                default    : 'difference',
                type       : String.class
        ],
        frequencyPrefix : [
                name       : 'Frequency prefix',
                title      : 'Frequency prefix',
                description: 'Optional prefix of the frequency band columns (e.g. <b>HZ</b>).</br> </br>' +
                             'Leave empty to automatically detect the common frequency columns.',
                default    : '',
                type       : String.class
        ],
        periodColumn    : [
                name       : 'Period column',
                title      : 'Period column',
                description: 'Optional name of the period column (e.g. <b>PERIOD</b>).</br> </br>' +
                             'Leave empty to automatically use <b>PERIOD</b>, then <b>TIMESTRING</b>. ' +
                             'If no period column is found, the join is done on <b>IDRECEIVER</b> only.',
                default    : '',
                type       : String.class
        ],
        outputTable     : [
                name       : 'Output table',
                title      : 'Output table',
                description: 'Optional name of the created table.</br> </br>' +
                             'If empty, <b>NOISE_MAP_DIFFERENCE</b> or <b>NOISE_MAP_ENERGETIC_SUM</b> is used.',
                default    : '',
                type       : String.class
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


def exec(Connection connection, Map input) {

    // output string, the information given back to the user
    String resultString

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    logger.info('Start : Combine Noise Maps')
    logger.info("inputs {}", input)

    DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class))

    // Need to change the ConnectionWrapper to WpsConnectionWrapper to work under PostGIS database
    connection = new ConnectionWrapper(connection)
    Sql sql = new Sql(connection)

    // -------------------
    // Get inputs
    // -------------------
    String firstTableName = (input['firstTable'] ?: '').toString().trim()
    String secondTableName = (input['secondTable'] ?: '').toString().trim()
    if (!firstTableName || !secondTableName) {
        throw new IllegalArgumentException('Both firstTable and secondTable must be provided.')
    }

    String operation = (input['operation'] ?: 'difference').toString().trim().toLowerCase()
    if (!(operation in ['difference', 'energetic_sum'])) {
        throw new IllegalArgumentException("Unknown operation '${operation}'. Use 'difference' or 'energetic_sum'.")
    }

    String frequencyPrefix = (input['frequencyPrefix'] ?: '').toString().trim()

    String outputTableName = (input['outputTable'] ?: '').toString().trim()
    if (!outputTableName) {
        outputTableName = operation == 'difference' ? 'NOISE_MAP_DIFFERENCE' : 'NOISE_MAP_ENERGETIC_SUM'
    }

    // -------------------
    // Checks
    // -------------------
    TableLocation firstTable = TableLocation.parse(firstTableName, dbType)
    TableLocation secondTable = TableLocation.parse(secondTableName, dbType)
    TableLocation outputTable = TableLocation.parse(outputTableName, dbType)

    if (outputTable.equals(firstTable) || outputTable.equals(secondTable)) {
        throw new IllegalArgumentException('outputTable must be different from firstTable and secondTable.')
    }

    if (!JDBCUtilities.tableExists(connection, firstTableName)) {
        throw new IllegalArgumentException("The table ${firstTableName} does not exist.")
    }
    if (!JDBCUtilities.tableExists(connection, secondTableName)) {
        throw new IllegalArgumentException("The table ${secondTableName} does not exist.")
    }

    Map<String, String> firstColumns = getColumnsByUpperCase(connection, firstTableName)
    Map<String, String> secondColumns = getColumnsByUpperCase(connection, secondTableName)

    String firstReceiverColumn = firstColumns.get('IDRECEIVER')
    String secondReceiverColumn = secondColumns.get('IDRECEIVER')
    if (!firstReceiverColumn || !secondReceiverColumn) {
        throw new IllegalArgumentException('Both tables must contain an IDRECEIVER column.')
    }

    String periodInput = (input['periodColumn'] ?: '').toString().trim()
    String periodColumn = findPeriodColumn(periodInput, firstColumns, secondColumns)

    List<String> frequencyColumns = findCommonFrequencyColumns(firstColumns, secondColumns, frequencyPrefix)
    if (frequencyPrefix && frequencyColumns.isEmpty()) {
        throw new IllegalArgumentException("No common frequency column found with prefix '${frequencyPrefix}'.")
    }

    List<String> indicatorColumns = ['LAEQ', 'LEQA', 'LEQ'].findAll { firstColumns.containsKey(it) && secondColumns.containsKey(it) }
    if (frequencyColumns.isEmpty() && indicatorColumns.isEmpty()) {
        throw new IllegalArgumentException('No common level column found. The tables must share frequency bands or LAEQ/LEQA/LEQ columns.')
    }

    List<String> firstGeometries = GeometryTableUtilities.getGeometryColumnNames(connection, firstTable)
    List<String> secondGeometries = GeometryTableUtilities.getGeometryColumnNames(connection, secondTable)
    if (firstGeometries.isEmpty() && secondGeometries.isEmpty()) {
        throw new IllegalArgumentException('At least one table must contain a geometry column.')
    }

    int firstSrid = firstGeometries.isEmpty() ? 0 : GeometryTableUtilities.getSRID(connection, firstTable)
    int secondSrid = secondGeometries.isEmpty() ? 0 : GeometryTableUtilities.getSRID(connection, secondTable)
    if (firstSrid != 0 && secondSrid != 0 && firstSrid != secondSrid) {
        throw new IllegalArgumentException("The SRID of ${firstTableName} (${firstSrid}) and ${secondTableName} (${secondSrid}) are different.")
    }

    String geometryExpression
    if (!firstGeometries.isEmpty()) {
        geometryExpression = "a.${quoteInputIdentifier(firstGeometries.get(0), dbType)}"
    } else {
        geometryExpression = "b.${quoteInputIdentifier(secondGeometries.get(0), dbType)}"
    }

    // -------------------
    // Validate the receiver grid
    // -------------------
    List<String> firstKeyColumns = new ArrayList<>()
    firstKeyColumns.add(firstReceiverColumn)
    if (periodColumn) {
        firstKeyColumns.add(periodColumn)
    }
    List<String> secondKeyColumns = new ArrayList<>()
    secondKeyColumns.add(secondReceiverColumn)
    if (periodColumn) {
        secondKeyColumns.add(secondColumns.get(periodColumn.toUpperCase()))
    }
    List<String> outputKeyColumns = new ArrayList<>()
    outputKeyColumns.add('IDRECEIVER')
    if (periodColumn) {
        outputKeyColumns.add(periodColumn)
    }

    String firstJoinCondition = joinCondition(firstReceiverColumn, secondReceiverColumn, periodColumn, secondColumns, dbType)

    long firstDuplicates = countDuplicateKeys(sql, firstTable.toString(), firstKeyColumns, dbType)
    long secondDuplicates = countDuplicateKeys(sql, secondTable.toString(), secondKeyColumns, dbType)
    if (firstDuplicates > 0 || secondDuplicates > 0) {
        throw new IllegalArgumentException(
                "Duplicate keys found. firstTable duplicates: ${firstDuplicates}, secondTable duplicates: ${secondDuplicates}. " +
                        "Each (IDRECEIVER, period) key must appear only once; no aggregation is performed.")
    }

    if (!firstGeometries.isEmpty() && !secondGeometries.isEmpty()) {
        String firstGeometryReference = "a.${quoteInputIdentifier(firstGeometries.get(0), dbType)}"
        String secondGeometryReference = "b.${quoteInputIdentifier(secondGeometries.get(0), dbType)}"
        String coordinateMismatch = "ST_AsBinary(${firstGeometryReference}) <> ST_AsBinary(${secondGeometryReference})"
        long coordinateMismatches = countRows(sql, "${firstTable} a INNER JOIN ${secondTable} b ON ${firstJoinCondition} WHERE ${coordinateMismatch}")
        if (coordinateMismatches > 0) {
            throw new IllegalArgumentException(
                    "The two tables do not use the same receiver coordinates. " +
                            "Matching IDRECEIVER/period keys must refer to the same (X, Y, Z) position.")
        }
    }

    long firstCount = countRows(sql, firstTable.toString())
    long secondCount = countRows(sql, secondTable.toString())
    long joinedCount = countRows(sql, "${firstTable} a INNER JOIN ${secondTable} b ON ${firstJoinCondition}")

    if (firstCount != secondCount || joinedCount != firstCount) {
        throw new IllegalArgumentException(
                "The two tables do not use the same receiver grid. " +
                        "firstTable rows: ${firstCount}, secondTable rows: ${secondCount}, matching rows: ${joinedCount}. " +
                        "Both tables must contain the same (IDRECEIVER, period) keys.")
    }

    // -------------------
    // Create the output table
    // -------------------
    List<String> selectParts = new ArrayList<>()
    selectParts.add("a.${quoteInputIdentifier(firstReceiverColumn, dbType)} AS ${quoteIdentifier('IDRECEIVER', dbType)}")
    if (periodColumn) {
        selectParts.add("a.${quoteInputIdentifier(periodColumn, dbType)} AS ${quoteIdentifier(periodColumn, dbType)}")
    }
    selectParts.add("${geometryExpression} AS ${quoteIdentifier('THE_GEOM', dbType)}")

    for (String column : frequencyColumns) {
        selectParts.add("${levelExpression(column, firstColumns, secondColumns, operation, dbType)} AS ${quoteIdentifier(firstColumns.get(column), dbType)}")
    }
    for (String column : indicatorColumns) {
        selectParts.add("${levelExpression(column, firstColumns, secondColumns, operation, dbType)} AS ${quoteIdentifier(firstColumns.get(column), dbType)}")
    }

    String dropQuery = "DROP TABLE IF EXISTS ${outputTable}"
    sql.execute(dropQuery)
    String query = "CREATE TABLE ${outputTable} AS SELECT ${selectParts.join(', ')} " +
            "FROM ${firstTable} a INNER JOIN ${secondTable} b ON ${firstJoinCondition}"
    logger.info(query)
    sql.execute(query)

    for (String keyColumn : outputKeyColumns) {
        String notNullQuery = "ALTER TABLE ${outputTable} ALTER COLUMN ${quoteIdentifier(keyColumn, dbType)} SET NOT NULL"
        sql.execute(notNullQuery)
    }
    String primaryKey = outputKeyColumns.collect { quoteIdentifier(it, dbType) }.join(', ')
    String primaryKeyQuery = "ALTER TABLE ${outputTable} ADD PRIMARY KEY (${primaryKey})"
    sql.execute(primaryKeyQuery)
    JDBCUtilities.createSpatialIndex(connection, outputTable, 'THE_GEOM')

    long outputCount = countRows(sql, outputTable.toString())
    resultString = "Process done. Table ${outputTableName} created with ${outputCount} row(s)."

    logger.info('End : Combine Noise Maps')
    logger.info('Result : ' + resultString)
    return resultString
}


private static Map<String, String> getColumnsByUpperCase(Connection connection, String tableName) {
    Map<String, String> columns = new LinkedHashMap<>()
    for (String column : JDBCUtilities.getColumnNames(connection, tableName)) {
        columns.put(column.toUpperCase(), column)
    }
    return columns
}

private static String findPeriodColumn(String periodInput, Map<String, String> firstColumns, Map<String, String> secondColumns) {
    if (periodInput) {
        String firstPeriod = firstColumns.get(periodInput.toUpperCase())
        String secondPeriod = secondColumns.get(periodInput.toUpperCase())
        if (!firstPeriod || !secondPeriod) {
            throw new IllegalArgumentException("The period column ${periodInput} must exist in both tables.")
        }
        return firstPeriod
    }

    List<String> firstPeriodColumns = ['PERIOD', 'TIMESTRING'].findAll { firstColumns.containsKey(it) }
    List<String> secondPeriodColumns = ['PERIOD', 'TIMESTRING'].findAll { secondColumns.containsKey(it) }
    for (String candidate : ['PERIOD', 'TIMESTRING']) {
        if (firstColumns.containsKey(candidate) && secondColumns.containsKey(candidate)) {
            return firstColumns.get(candidate)
        }
    }

    if (!firstPeriodColumns.isEmpty() || !secondPeriodColumns.isEmpty()) {
        throw new IllegalArgumentException(
                "The tables do not share a common period column. firstTable: ${firstPeriodColumns}, secondTable: ${secondPeriodColumns}.")
    }
    return null
}

private static List<String> findCommonFrequencyColumns(Map<String, String> firstColumns,
                                                       Map<String, String> secondColumns,
                                                       String frequencyPrefix) {
    Set<Integer> knownFrequencies = [50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800, 1000,
                                     1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000] as Set
    Pattern pattern = frequencyPrefix ?
            Pattern.compile('^' + Pattern.quote(frequencyPrefix.toUpperCase()) + '(\\d+)$', Pattern.CASE_INSENSITIVE) :
            Pattern.compile('^([A-Za-z_][A-Za-z0-9_]*?)(\\d+)$')

    List<Map<String, Object>> matches = new ArrayList<>()
    for (String column : firstColumns.keySet()) {
        if (!secondColumns.containsKey(column)) {
            continue
        }
        Matcher matcher = pattern.matcher(column)
        if (!matcher.matches()) {
            continue
        }
        int frequency = Integer.parseInt(matcher.group(matcher.groupCount()))
        if (!frequencyPrefix && !knownFrequencies.contains(frequency)) {
            continue
        }
        matches.add([key: column, name: firstColumns.get(column), frequency: frequency])
    }

    matches.sort { a, b -> a.frequency == b.frequency ? a.name <=> b.name : a.frequency <=> b.frequency }
    return matches.collect { it.key }
}

private static String joinCondition(String firstReceiverColumn,
                                    String secondReceiverColumn,
                                    String periodColumn,
                                    Map<String, String> secondColumns,
                                    DBTypes dbType) {
    List<String> conditions = new ArrayList<>()
    conditions.add("a.${quoteInputIdentifier(firstReceiverColumn, dbType)} = b.${quoteInputIdentifier(secondReceiverColumn, dbType)}")
    if (periodColumn) {
        String secondPeriodColumn = secondColumns.get(periodColumn.toUpperCase())
        conditions.add("a.${quoteInputIdentifier(periodColumn, dbType)} = b.${quoteInputIdentifier(secondPeriodColumn, dbType)}")
    }
    return conditions.join(' AND ')
}

private static String levelExpression(String column,
                                      Map<String, String> firstColumns,
                                      Map<String, String> secondColumns,
                                      String operation,
                                      DBTypes dbType) {
    String firstReference = "a.${quoteInputIdentifier(firstColumns.get(column), dbType)}"
    String secondReference = "b.${quoteInputIdentifier(secondColumns.get(column), dbType)}"
    if (operation == 'difference') {
        return "(${firstReference} - ${secondReference})"
    }
    return "10 * LOG10(POWER(10, ${firstReference} / 10.0) + POWER(10, ${secondReference} / 10.0))"
}

private static long countRows(Sql sql, String from) {
    String query = "SELECT COUNT(*) AS C FROM ${from}"
    return (sql.firstRow(query).C as Number).longValue()
}

private static long countDuplicateKeys(Sql sql, String table, List<String> keyColumns, DBTypes dbType) {
    String keys = keyColumns.collect { quoteInputIdentifier(it, dbType) }.join(', ')
    String query = "SELECT COUNT(*) AS C FROM (SELECT ${keys} FROM ${table} GROUP BY ${keys} HAVING COUNT(*) > 1) DUPLICATES"
    return (sql.firstRow(query).C as Number).longValue()
}

private static String quoteInputIdentifier(String identifier, DBTypes dbType) {
    return TableLocation.quoteIdentifier(identifier, dbType)
}

private static String quoteIdentifier(String identifier, DBTypes dbType) {
    if (identifier ==~ /[A-Za-z_][A-Za-z0-9_]*/) {
        return identifier
    }
    return TableLocation.quoteIdentifier(identifier, dbType)
}
