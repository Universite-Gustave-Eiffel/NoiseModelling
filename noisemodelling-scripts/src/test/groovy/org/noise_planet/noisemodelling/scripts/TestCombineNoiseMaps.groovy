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

package org.noise_planet.noisemodelling.scripts

import groovy.sql.Sql
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.junit.jupiter.api.Test
import org.noise_planet.noisemodelling.scripts.Acoustic_Tools.Combine_Noise_Maps

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class TestCombineNoiseMaps extends JdbcTestCase {

    @Test
    void testDifference() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 55, laeq: 60, leq: 62]
        ])
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 55]
        ])

        String result = new Combine_Noise_Maps().exec(connection, [
                firstTable : 'MAP_A',
                secondTable: 'MAP_B',
                operation  : 'difference',
                outputTable: 'MAP_DIFF'
        ])

        def sql = new Sql(connection)
        def row = sql.firstRow('SELECT * FROM MAP_DIFF')
        assertEquals(1L, (row.IDRECEIVER as Number).longValue())
        assertEquals('L', row.PERIOD)
        assertEquals(10.0d, (row.HZ63 as Number).doubleValue(), 1e-6)
        assertEquals(5.0d, (row.HZ125 as Number).doubleValue(), 1e-6)
        assertEquals(10.0d, (row.LAEQ as Number).doubleValue(), 1e-6)
        assertEquals(7.0d, (row.LEQ as Number).doubleValue(), 1e-6)
        assertEquals(2154, GeometryTableUtilities.getSRID(connection, TableLocation.parse('MAP_DIFF')))
        assertTrue(result.contains('MAP_DIFF'))
    }

    @Test
    void testEnergeticSum() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ])
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 50, laeq: 60, leq: 50]
        ])

        new Combine_Noise_Maps().exec(connection, [
                firstTable : 'MAP_A',
                secondTable: 'MAP_B',
                operation  : 'energetic_sum',
                outputTable: 'MAP_SUM'
        ])

        def sql = new Sql(connection)
        def row = sql.firstRow('SELECT * FROM MAP_SUM')
        assertEquals(63.0102999566d, (row.HZ63 as Number).doubleValue(), 1e-6)
        assertEquals(60.4139268516d, (row.HZ125 as Number).doubleValue(), 1e-6)
        assertEquals(63.0102999566d, (row.LAEQ as Number).doubleValue(), 1e-6)
        assertEquals(60.4139268516d, (row.LEQ as Number).doubleValue(), 1e-6)
    }

    @Test
    void testPeriodsAreNotAggregated() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60],
                [id: 1, period: 'D', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50],
                [id: 1, period: 'D', x: 0, y: 0, hz63: 40, hz125: 40, laeq: 40, leq: 40]
        ])

        new Combine_Noise_Maps().exec(connection, [
                firstTable : 'MAP_A',
                secondTable: 'MAP_B',
                operation  : 'difference',
                outputTable: 'MAP_DIFF'
        ])

        def sql = new Sql(connection)
        def rows = sql.rows('SELECT * FROM MAP_DIFF ORDER BY PERIOD')
        assertEquals(2, rows.size())
        assertEquals('D', rows[0].PERIOD)
        assertEquals(10.0d, (rows[0].HZ63 as Number).doubleValue(), 1e-6)
        assertEquals('L', rows[1].PERIOD)
        assertEquals(10.0d, (rows[1].HZ63 as Number).doubleValue(), 1e-6)
    }

    @Test
    void testNoPeriodColumn() {
        createMapTable('MAP_A', false, [
                [id: 1, x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ])
        createMapTable('MAP_B', false, [
                [id: 1, x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])

        new Combine_Noise_Maps().exec(connection, [
                firstTable : 'MAP_A',
                secondTable: 'MAP_B',
                operation  : 'difference',
                outputTable: 'MAP_DIFF'
        ])

        def sql = new Sql(connection)
        def row = sql.firstRow('SELECT * FROM MAP_DIFF')
        assertEquals(10.0d, (row.HZ63 as Number).doubleValue(), 1e-6)
        assertFalse(JDBCUtilities.getColumnNames(connection, 'MAP_DIFF').contains('PERIOD'))
    }

    @Test
    void testAutomaticFrequencyPrefixDetection() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ], 'HZD')
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ], 'HZD')

        new Combine_Noise_Maps().exec(connection, [
                firstTable : 'MAP_A',
                secondTable: 'MAP_B',
                operation  : 'difference',
                outputTable: 'MAP_DIFF'
        ])

        def sql = new Sql(connection)
        def row = sql.firstRow('SELECT * FROM MAP_DIFF')
        assertEquals(10.0d, (row.HZD63 as Number).doubleValue(), 1e-6)
        assertEquals(10.0d, (row.HZD125 as Number).doubleValue(), 1e-6)
    }

    @Test
    void testExplicitFrequencyPrefix() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ], 'HZD')
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ], 'HZD')

        new Combine_Noise_Maps().exec(connection, [
                firstTable      : 'MAP_A',
                secondTable     : 'MAP_B',
                operation       : 'difference',
                frequencyPrefix : 'HZD',
                outputTable     : 'MAP_DIFF'
        ])

        def sql = new Sql(connection)
        def row = sql.firstRow('SELECT * FROM MAP_DIFF')
        assertEquals(10.0d, (row.HZD63 as Number).doubleValue(), 1e-6)
    }

    @Test
    void testDifferentReceiverGridIsRejected() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ])
        createMapTable('MAP_B', true, [
                [id: 2, period: 'L', x: 1, y: 1, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, {
            new Combine_Noise_Maps().exec(connection, [
                    firstTable : 'MAP_A',
                    secondTable: 'MAP_B',
                    operation  : 'difference',
                    outputTable: 'MAP_DIFF'
            ])
        })
        assertTrue(exception.message.contains('same receiver grid'))
    }

    @Test
    void testDifferentCoordinatesAreRejected() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ])
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 10, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, {
            new Combine_Noise_Maps().exec(connection, [
                    firstTable : 'MAP_A',
                    secondTable: 'MAP_B',
                    operation  : 'difference',
                    outputTable: 'MAP_DIFF'
            ])
        })
        assertTrue(exception.message.contains('receiver coordinates'))
    }

    @Test
    void testMixedPeriodColumnsAreRejected() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ], 'HZ', 'PERIOD')
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ], 'HZ', 'TIMESTRING')

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, {
            new Combine_Noise_Maps().exec(connection, [
                    firstTable : 'MAP_A',
                    secondTable: 'MAP_B',
                    operation  : 'difference',
                    outputTable: 'MAP_DIFF'
            ])
        })
        assertTrue(exception.message.contains('common period column'))
    }

    @Test
    void testLowerCaseColumnNames() {
        createLowerCaseMapTable('MAP_A', [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ])
        createLowerCaseMapTable('MAP_B', [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])

        new Combine_Noise_Maps().exec(connection, [
                firstTable : 'MAP_A',
                secondTable: 'MAP_B',
                operation  : 'difference',
                outputTable: 'MAP_DIFF'
        ])

        def sql = new Sql(connection)
        def row = sql.firstRow('SELECT * FROM MAP_DIFF')
        assertEquals('L', row.PERIOD)
        assertEquals(10.0d, (row.HZ63 as Number).doubleValue(), 1e-6)
        assertEquals(10.0d, (row.HZ125 as Number).doubleValue(), 1e-6)
        assertEquals(10.0d, (row.LAEQ as Number).doubleValue(), 1e-6)
    }

    @Test
    void testDuplicateKeysAreRejected() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60],
                [id: 1, period: 'L', x: 0, y: 0, hz63: 61, hz125: 61, laeq: 61, leq: 61]
        ])
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, {
            new Combine_Noise_Maps().exec(connection, [
                    firstTable : 'MAP_A',
                    secondTable: 'MAP_B',
                    operation  : 'difference',
                    outputTable: 'MAP_DIFF'
            ])
        })
        assertTrue(exception.message.contains('Duplicate keys'))
    }

    @Test
    void testUnknownOperationIsRejected() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ])
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, {
            new Combine_Noise_Maps().exec(connection, [
                    firstTable : 'MAP_A',
                    secondTable: 'MAP_B',
                    operation  : 'average',
                    outputTable: 'MAP_DIFF'
            ])
        })
        assertTrue(exception.message.contains('Unknown operation'))
    }

    @Test
    void testDefaultOutputTableName() {
        createMapTable('MAP_A', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 60, hz125: 60, laeq: 60, leq: 60]
        ])
        createMapTable('MAP_B', true, [
                [id: 1, period: 'L', x: 0, y: 0, hz63: 50, hz125: 50, laeq: 50, leq: 50]
        ])

        new Combine_Noise_Maps().exec(connection, [
                firstTable : 'MAP_A',
                secondTable: 'MAP_B',
                operation  : 'energetic_sum'
        ])

        assertTrue(JDBCUtilities.tableExists(connection, 'NOISE_MAP_ENERGETIC_SUM'))
    }

    private void createMapTable(String tableName, boolean withPeriod, List<Map> rows, String prefix = 'HZ',
                               String periodName = 'PERIOD') {
        def sql = new Sql(connection)
        String periodColumn = withPeriod ? periodName + ' VARCHAR, ' : ''
        String createSql = 'CREATE TABLE ' + tableName + ' (IDRECEIVER BIGINT, ' + periodColumn +
                'THE_GEOM GEOMETRY(POINT, 2154), ' + prefix + '63 DOUBLE, ' + prefix + '125 DOUBLE, LAEQ DOUBLE, LEQ DOUBLE)'
        sql.execute(createSql)
        for (Map row : rows) {
            String periodValue = withPeriod ? "'" + row.period + "', " : ''
            String insertSql = 'INSERT INTO ' + tableName + ' VALUES (' + row.id + ', ' + periodValue +
                    "ST_GeomFromText('POINT(" + row.x + ' ' + row.y + ")', 2154), " +
                    row.hz63 + ', ' + row.hz125 + ', ' + row.laeq + ', ' + row.leq + ')'
            sql.execute(insertSql)
        }
    }

    private void createLowerCaseMapTable(String tableName, List<Map> rows) {
        def sql = new Sql(connection)
        sql.execute('CREATE TABLE ' + tableName + ' ("idreceiver" BIGINT, "period" VARCHAR, ' +
                '"the_geom" GEOMETRY(POINT, 2154), "hz63" DOUBLE, "hz125" DOUBLE, "laeq" DOUBLE, "leq" DOUBLE)')
        for (Map row : rows) {
            String insertSql = 'INSERT INTO ' + tableName +
                    ' ("idreceiver", "period", "the_geom", "hz63", "hz125", "laeq", "leq") VALUES (' +
                    row.id + ", '" + row.period + "', ST_GeomFromText('POINT(" + row.x + ' ' + row.y + ")', 2154), " +
                    row.hz63 + ', ' + row.hz125 + ', ' + row.laeq + ', ' + row.leq + ')'
            sql.execute(insertSql)
        }
    }
}
