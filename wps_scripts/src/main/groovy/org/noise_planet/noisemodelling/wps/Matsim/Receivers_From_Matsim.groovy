package org.noise_planet.noisemodelling.wps.Matsim

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.matsim.api.core.v01.Coord
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.Scenario
import org.matsim.core.config.ConfigUtils
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.facilities.ActivityFacilities
import org.matsim.facilities.ActivityFacility
import org.matsim.facilities.MatsimFacilitiesReader

import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = 'Create receivers based on a Matsim "facilities" file.'

inputs = [
    facilitiesPath : [
        name: 'Path of the Matsim facilities file',
        title: 'Path of the Matsim facilities file',
        description: 'Path of the Matsim facilities file',
        type: String.class
    ],
    buildingsOsmPbfPath : [
            name: 'Path of the osm pbf file containing the buildings',
            title: 'Path of the osm pbf file containing the buildings',
            description: 'Path of the osm pbf file containing the buildings',
            min: 0,
            max: 1,
            type: String.class
    ],
    buildingsTableName : [
            name: 'Name of the table containing the buildings',
            title: 'Name of the table containing the buildings',
            description: 'Name of the table containing the buildings',
            min: 0,
            max: 1,
            type: String.class
    ],
    filter: [
            name: 'Filter on facilities Ids : default \'*\'',
            title: 'Filter on facilities Ids',
            description: 'Filter on facilities Ids',
            min: 0,
            max: 1,
            type: String.class
    ],
    outTableName: [
            name: 'Output table name',
            title: 'Name of created table',
            description: 'Name of the table you want to create: RECEIVERS',
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

    String outTableName = "RECEIVERS"
    if (input['outTableName']) {
        outTableName = input['outTableName']
    }
    outTableName = outTableName.toUpperCase()


    String buildingTableName = "BUILDINGS"
    if (input['buildingTableName']) {
        buildingTableName = input['buildingTableName']
    }
    buildingTableName = buildingTableName.toUpperCase()

    String filter = "*"
    if (input['filter']) {
        filter = input['filter']
    }

    double height = 4.0;

    String facilitiesPath = input['facilitiesPath']

    Sql sql = new Sql(connection)
    //Delete previous receivers
    sql.execute(String.format("DROP TABLE IF EXISTS %s", outTableName))
    sql.execute("CREATE TABLE " + outTableName + '''( 
        PK integer PRIMARY KEY AUTO_INCREMENT,
        FACILITY_ID varchar(255),
        THE_GEOM geometry,
        TYPES varchar(255)
    );''')
    sql.execute("CREATE INDEX ON " + outTableName + "(FACILITY_ID)");

    Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig())
    MatsimFacilitiesReader reader = new MatsimFacilitiesReader(scenario)
    reader.readFile(facilitiesPath);

    ActivityFacilities facilities = scenario.getActivityFacilities();

    for (def entry : facilities.getFacilities().entrySet()) {
        String facilityId = entry.getKey().toString();
        ActivityFacility facility = entry.getValue();
        Coord c = facility.getCoord();
        String geom = String.format("POINT(%s %s %s)", Double.toString(c.getX()), Double.toString(c.getY()), Double.toString(height));
        String types = facility.getActivityOptions().keySet().join(',');
        String query = "INSERT INTO " + outTableName + "(FACILITY_ID, THE_GEOM, TYPES) VALUES( '" + facilityId + "', '" + geom + "', '" + types + "')";
        sql.execute(query);
    }
    // TODO : sql.execute("CREATE SPATIAL INDEX ON ");

    return [result: "Process done. Table of receivers " + outTableName + " created !"]
}

