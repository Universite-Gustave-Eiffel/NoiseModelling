/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
/**
 * @Author Valentin Le Bescond, Université Gustave Eiffel
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.scripts.Dynamic

import groovy.transform.CompileStatic
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.*
import groovy.sql.Sql

title = 'Noise Map From Attenuation Matrix'
description = 'Noise Map From Attenuation Matrix.' +
        '<br/>'

inputs = [
        lwTable : [
                name: 'LW(PERIOD)',
                title: 'LW(PERIOD)',
                description: 'LW(PERIOD) ex. SOURCES_EMISSION' +
                        '<br/>The table must contain the following fields :' +
                        '<br/>IDSOURCE, PERIOD, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000' +
                        '<br/> IDSOURCE link to primary key of attenuation table and PERIOD a varchar',
                type: String.class
        ],
        lwTable_sourceId: [
                name: 'LW(PERIOD) source index field',
                title: 'LW(PERIOD) source index field',
                description: 'LW(PERIOD) source index field. Default is IDSOURCE',
                min        : 0,
                max        : 1,
                type: String.class
        ],
        attenuationTable : [
        name: 'Attenuation Matrix Table name',
        title: 'Attenuation Matrix Table name',
        description: 'Attenuation Matrix Table name, Obtained from the Noise_level_from_source script with "confExportSourceId" enabled. Should be RECEIVERS_LEVEL' +
                '<br/>The table must contain the following fields :' +
                '<br/>IDRECEIVER, IDSOURCE, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000',
        type: String.class
    ],
        outputTable : [
                name: 'outputTable Matrix Table name',
                title: 'outputTable Matrix Table name',
                description: 'outputTable',
                type: String.class
        ]
]

outputs = [
    result: [
        name: 'Result output string',
        title: 'Result output string',
        description: 'This type of result does not allow the blocks to be linked together.',
        type: String.class
    ]
]

/**
 * Combines the attenuation matrix with the per period emissions.
 *
 * The SQL join computed 10*LOG10(SUM(POWER(10,(LW + ATT)/10))) per receiver and period.
 * Since POWER(10,(LW + ATT)/10) = POWER(10,LW/10) * POWER(10,ATT/10), this is, in linear
 * power, one sparse matrix product per frequency band between the attenuation matrix
 * (receivers x sources, one non zero per table row) and the emission matrix
 * (sources x periods). This class computes that product directly: each attenuation row is
 * read once and touched once per period, and the receivers x periods intermediate of the
 * SQL GROUP BY is never materialized.
 */
@CompileStatic
class AttenuationMatrixProduct {
    static final int BANDS = 8
    static final String[] BAND_FIELDS = ["HZ63", "HZ125", "HZ250", "HZ500", "HZ1000", "HZ2000", "HZ4000", "HZ8000"]

    static void process(Connection connection, String attenuationTable, String lwTable,
                        String lwSourceId, String outputTable) throws SQLException {
        String bandColumns = String.join(", ", BAND_FIELDS)

        // Read the attenuation matrix: one sparse entry per row, factors in linear power
        int nnz = 0
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + attenuationTable)) {
            rs.next()
            nnz = rs.getInt(1)
        }
        int[] nzReceiver = new int[nnz]
        int[] nzSource = new int[nnz]
        double[] nzFactor = new double[nnz * BANDS]
        Map<Long, Integer> receiverIndex = new HashMap<>()
        Map<Long, Integer> sourceIndex = new HashMap<>()
        List<Long> receiverIds = new ArrayList<>()
        List<Object> receiverGeometries = new ArrayList<>()
        String receiverColumnType
        String geometryColumnType
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT IDRECEIVER, IDSOURCE, THE_GEOM, " + bandColumns +
                     " FROM " + attenuationTable)) {
            ResultSetMetaData meta = rs.getMetaData()
            receiverColumnType = meta.getColumnTypeName(1)
            geometryColumnType = meta.getColumnTypeName(3)
            int i = 0
            while (rs.next()) {
                long receiverId = rs.getLong(1)
                Integer receiver = receiverIndex.get(receiverId)
                if (receiver == null) {
                    receiver = receiverIndex.size()
                    receiverIndex.put(receiverId, receiver)
                    receiverIds.add(receiverId)
                    receiverGeometries.add(rs.getObject(3))
                }
                Integer source = sourceIndex.computeIfAbsent(rs.getLong(2), { Long k -> sourceIndex.size() })
                nzReceiver[i] = receiver
                nzSource[i] = source
                for (int f = 0; f < BANDS; f++) {
                    nzFactor[i * BANDS + f] = Math.pow(10.0d, rs.getDouble(4 + f) / 10.0d)
                }
                i++
            }
            nnz = i
        }
        int nReceivers = receiverIndex.size()
        int nSources = sourceIndex.size()

        // Read the emissions: dense sources x periods matrix in linear power.
        // Sources without attenuation row cannot contribute, they are skipped.
        int nPeriods = 0
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT PERIOD) FROM " + lwTable)) {
            rs.next()
            nPeriods = rs.getInt(1)
        }
        Map<String, Integer> periodIndex = new LinkedHashMap<>()
        List<String> periods = new ArrayList<>()
        double[] power = new double[nPeriods * nSources * BANDS]
        boolean[] hasEmission = new boolean[nPeriods * nSources]
        String periodColumnType
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT PERIOD, " + lwSourceId + ", " + bandColumns +
                     " FROM " + lwTable)) {
            periodColumnType = rs.getMetaData().getColumnTypeName(1)
            while (rs.next()) {
                Integer source = sourceIndex.get(rs.getLong(2))
                if (source == null) {
                    continue
                }
                String periodName = rs.getString(1)
                Integer period = periodIndex.get(periodName)
                if (period == null) {
                    period = periodIndex.size()
                    periodIndex.put(periodName, period)
                    periods.add(periodName)
                }
                int cell = period * nSources + source
                hasEmission[cell] = true
                for (int f = 0; f < BANDS; f++) {
                    // += reproduces the join, duplicate emission rows add their power
                    power[cell * BANDS + f] += Math.pow(10.0d, rs.getDouble(3 + f) / 10.0d)
                }
            }
        }
        nPeriods = periodIndex.size()

        StringBuilder createTable = new StringBuilder("CREATE TABLE " + outputTable +
                " (IDRECEIVER " + receiverColumnType + ", PERIOD " + periodColumnType +
                ", THE_GEOM " + geometryColumnType)
        for (String band : BAND_FIELDS) {
            createTable.append(", ").append(band).append(" DOUBLE PRECISION")
        }
        createTable.append(")")
        try (Statement st = connection.createStatement()) {
            st.execute(createTable.toString())
        }

        // One product per period: level[r] += factor[r,s] * power[s, period]
        double[] level = new double[nReceivers * BANDS]
        boolean[] matched = new boolean[nReceivers]
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + outputTable +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (int period = 0; period < nPeriods; period++) {
                Arrays.fill(level, 0.0d)
                Arrays.fill(matched, false)
                int periodBase = period * nSources
                for (int i = 0; i < nnz; i++) {
                    int source = nzSource[i]
                    if (!hasEmission[periodBase + source]) {
                        continue
                    }
                    int receiver = nzReceiver[i]
                    matched[receiver] = true
                    int factorOffset = i * BANDS
                    int powerOffset = (periodBase + source) * BANDS
                    int levelOffset = receiver * BANDS
                    for (int f = 0; f < BANDS; f++) {
                        level[levelOffset + f] += nzFactor[factorOffset + f] * power[powerOffset + f]
                    }
                }
                String periodName = periods.get(period)
                int pending = 0
                for (int receiver = 0; receiver < nReceivers; receiver++) {
                    if (!matched[receiver]) {
                        continue
                    }
                    insert.setLong(1, receiverIds.get(receiver))
                    insert.setString(2, periodName)
                    insert.setObject(3, receiverGeometries.get(receiver))
                    for (int f = 0; f < BANDS; f++) {
                        insert.setDouble(4 + f, 10.0d * Math.log10(level[receiver * BANDS + f]))
                    }
                    insert.addBatch()
                    if (++pending >= 1000) {
                        insert.executeBatch()
                        pending = 0
                    }
                }
                if (pending > 0) {
                    insert.executeBatch()
                }
            }
        }
    }
}

// main function of the script
def exec(Connection connection, input) {

    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    String resultString

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Noise_From_Attenuation_Matrix')
    logger.info("inputs {}", input)



    String lwTable_sourceId = "IDSOURCE"
    if (input['lwTable_sourceId']) {
        lwTable_sourceId = input['lwTable_sourceId']
    }


    String outputTable = input['outputTable'].toString().toUpperCase()
    String attenuationTable = input['attenuationTable'].toString().toUpperCase()
    String lwTable = input['lwTable'].toString().toUpperCase()
    String prefix = "HZ"

    AttenuationMatrixProduct.process(connection, attenuationTable, lwTable, lwTable_sourceId, outputTable)

    def query2 = $/
        ALTER TABLE  $outputTable ADD COLUMN LAEQ float as 10*log10((power(10,(${prefix}63-26.2)/10)+power(10,(${prefix}125-16.1)/10)+power(10,(${prefix}250-8.6)/10)+power(10,(${prefix}500-3.2)/10)+power(10,(${prefix}1000)/10)+power(10,(${prefix}2000+1.2)/10)+power(10,(${prefix}4000+1)/10)+power(10,(${prefix}8000-1.1)/10)));
        ALTER TABLE $outputTable ADD COLUMN LEQ float as 10*log10((power(10,(${prefix}63)/10)+power(10,(${prefix}125)/10)+power(10,(${prefix}250)/10)+power(10,(${prefix}500)/10)+power(10,(${prefix}1000)/10)+power(10,(${prefix}2000)/10)+power(10,(${prefix}4000)/10)+power(10,(${prefix}8000)/10)));
        CREATE UNIQUE INDEX ON $outputTable (IDRECEIVER, PERIOD);
    /$

    sql.execute(query2.toString())

    logger.info('End : Noise_From_Attenuation_Matrix_MatSim')
    resultString = "Process done. Table of receivers LT_GEOM created !"
    logger.info('Result : ' + resultString)
    return resultString
}
