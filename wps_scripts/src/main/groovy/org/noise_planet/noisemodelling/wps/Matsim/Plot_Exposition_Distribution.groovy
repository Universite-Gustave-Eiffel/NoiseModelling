package org.noise_planet.noisemodelling.wps.Matsim

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.XYPlot
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.matsim.api.core.v01.Coord
import org.matsim.api.core.v01.Scenario
import org.matsim.core.config.ConfigUtils
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.facilities.ActivityFacilities
import org.matsim.facilities.ActivityFacility
import org.matsim.facilities.MatsimFacilitiesReader

import javax.swing.JFrame
import java.awt.BorderLayout
import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = 'Create receivers based on a Matsim "facilities" file.'

inputs = [
    expositionsTableName : [
            name: 'Name of the table containing the expositions',
            title: 'Name of the table containing the expositions',
            description: 'Name of the table containing the expositions',
            type: String.class
    ],
    expositionField: [
            name: 'Field containing noise exposition',
            title: 'Field containing noise exposition',
            description: 'Field containing noise exposition',
            min: 0,
            max: 1,
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

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    // Get name of the database
    String dbName = "h2gis"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection -> exec(connection, input)
    }
}

def exec(connection, input) {

    String expositionsTableName = "AGENTS"
    if (input['expositionsTableName']) {
        expositionsTableName = input['expositionsTableName']
    }
    String expositionField = "LAEQ"
    if (input['expositionField']) {
        expositionField = input['expositionField']
    }
    String otherExpositionField = ""
    if (input['otherExpositionField']) {
        otherExpositionField = input['otherExpositionField']
    }

    Statement stmt = connection.createStatement();

    int startLAeq = 20
    int endLAeq = 100

    XYSeries expositionSeries = new XYSeries("AGENTS Exposition");
    XYSeries otherExpositionSeries = new XYSeries("OTHER Exposition");

    for (int level = startLAeq; level <= endLAeq; level++) {
        String query = '''
            SELECT COUNT(''' + expositionField + ''') * 100 / ( SELECT COUNT(*) FROM ''' + expositionsTableName + ''') as NB_AGENTS
            FROM ''' + expositionsTableName + '''
            WHERE ''' + expositionField + ''' >= ''' + Double.toString(level) + '''
        ''';
        ResultSet result = stmt.executeQuery(query);

        while (result.next()) {
            expositionSeries.add(level, result.getInt("NB_AGENTS"))
        }
    }

    if (otherExpositionField != "") {
        for (int level = startLAeq; level <= endLAeq; level++) {
            String query = '''
                SELECT COUNT(''' + expositionField + ''') * 100 / ( SELECT COUNT(*) FROM ''' + expositionsTableName + ''') as NB_AGENTS
                FROM ''' + expositionsTableName + '''
                WHERE ''' + otherExpositionField + ''' >= ''' + Double.toString(level) + '''
            ''';
            ResultSet result = stmt.executeQuery(query);

            while (result.next()) {
                otherExpositionSeries.add(level, result.getInt("NB_AGENTS"))
            }
        }
    }

    XYSeriesCollection dataset = new XYSeriesCollection();

    if (otherExpositionField != "") {
        dataset.addSeries(otherExpositionSeries);
    }
    dataset.addSeries(expositionSeries);
    JFreeChart chart = ChartFactory.createXYAreaChart("Exposition", "LAeq", "% Agents", dataset);
    ChartPanel chartPanel = new ChartPanel(chart);
    XYPlot plot = chart.getXYPlot();
    JFrame f = new JFrame("RESULT");
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setLayout(new BorderLayout(0, 5));
    f.add(chartPanel, BorderLayout.CENTER);
    f.pack();
    f.setLocationRelativeTo(null);
    f.setVisible(true);

    // Delete previous receivers
    return [result: "Process done."]
}