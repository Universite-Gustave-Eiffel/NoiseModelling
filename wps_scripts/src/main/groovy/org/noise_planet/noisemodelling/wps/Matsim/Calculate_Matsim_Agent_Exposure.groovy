package org.noise_planet.noisemodelling.wps.Matsim

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.locationtech.jts.geom.Coordinate
import org.matsim.api.core.v01.Coord
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.Scenario
import org.matsim.api.core.v01.events.PersonArrivalEvent
import org.matsim.api.core.v01.events.PersonDepartureEvent
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler
import org.matsim.api.core.v01.network.Link
import org.matsim.api.core.v01.population.Activity
import org.matsim.api.core.v01.population.Person
import org.matsim.api.core.v01.population.Plan
import org.matsim.api.core.v01.population.PlanElement
import org.matsim.api.core.v01.population.Population
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.EventsUtils
import org.matsim.core.events.MatsimEventsReader
import org.matsim.core.population.io.PopulationReader
import org.matsim.core.population.io.PopulationWriter
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.utils.misc.OptionalTime
import org.matsim.vehicles.Vehicle
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RSParametersCnossos

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

title = 'Calculate Mastim agents exposure'

description = 'Calculate Mastim agents exposure'

inputs = [
        folder: [
                name: 'Path of the Matsim output folder',
                title: 'Path of the Matsim output folder',
                description: 'Path of the Matsim output folder </br> For example : c:/home/mastim/output',
                type: String.class
        ],
        dataTablePrefix: [
                name: 'Table Prefix For the noise data',
                title: 'Table Prefix For the noise data',
                description: 'Table Prefix For the noise data',
                min: 0,
                max: 1,
                type: String.class
        ],
        timeSlice: [
                name: 'How to separate Roads statistics ? DEN, hour, quarter',
                title: 'How to separate Roads statistics ? DEN, hour, quarter',
                description: 'How to separate Roads statistics ? DEN, hour, quarter',
                type: String.class
        ],
        outTableName: [
                name: 'Output table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create from the file. </br> <b> Default value : ROADS</b>',
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

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            exec(connection, input)
            return [result: "OK"]
    }
}

// main function of the script
def exec(Connection connection, input) {

    String folder = input["folder"];

    String outTableName = "AGENTS";
    if (input['outTableName']) {
        outTableName = input['outTableName'];
    }

    String dataTablePrefix = "RESULT_GEOM_";
    if (input["dataTablePrefix"]) {
        dataTablePrefix = input["dataTablePrefix"];
    }

    String timeSlice = "hour";
    if (input["timeSlice"] == "DEN") {
        timeSlice = input["timeSlice"];
    }
    if (input["timeSlice"] == "quarter") {
        timeSlice = input["timeSlice"];
    }

    String[] den = ["D", "E", "N"];
    String[] hourClock = ["0_1", "1_2", "2_3", "3_4", "4_5", "5_6", "6_7", "7_8", "8_9", "9_10", "10_11", "11_12", "12_13", "13_14", "14_15", "15_16", "16_17", "17_18", "18_19", "19_20", "20_21", "21_22", "22_23", "23_24"];
    String[] quarterClock = ["0h00_0h15", "0h15_0h30", "0h30_0h45", "0h45_1h00", "1h00_1h15", "1h15_1h30", "1h30_1h45", "1h45_2h00", "2h00_2h15", "2h15_2h30", "2h30_2h45", "2h45_3h00", "3h00_3h15", "3h15_3h30", "3h30_3h45", "3h45_4h00", "4h00_4h15", "4h15_4h30", "4h30_4h45", "4h45_5h00", "5h00_5h15", "5h15_5h30", "5h30_5h45", "5h45_6h00", "6h00_6h15", "6h15_6h30", "6h30_6h45", "6h45_7h00", "7h00_7h15", "7h15_7h30", "7h30_7h45", "7h45_8h00", "8h00_8h15", "8h15_8h30", "8h30_8h45", "8h45_9h00", "9h00_9h15", "9h15_9h30", "9h30_9h45", "9h45_10h00", "10h00_10h15", "10h15_10h30", "10h30_10h45", "10h45_11h00", "11h00_11h15", "11h15_11h30", "11h30_11h45", "11h45_12h00", "12h00_12h15", "12h15_12h30", "12h30_12h45", "12h45_13h00", "13h00_13h15", "13h15_13h30", "13h30_13h45", "13h45_14h00", "14h00_14h15", "14h15_14h30", "14h30_14h45", "14h45_15h00", "15h00_15h15", "15h15_15h30", "15h30_15h45", "15h45_16h00", "16h00_16h15", "16h15_16h30", "16h30_16h45", "16h45_17h00", "17h00_17h15", "17h15_17h30", "17h30_17h45", "17h45_18h00", "18h00_18h15", "18h15_18h30", "18h30_18h45", "18h45_19h00", "19h00_19h15", "19h15_19h30", "19h30_19h45", "19h45_20h00", "20h00_20h15", "20h15_20h30", "20h30_20h45", "20h45_21h00", "21h00_21h15", "21h15_21h30", "21h30_21h45", "21h45_22h00", "22h00_22h15", "22h15_22h30", "22h30_22h45", "22h45_23h00", "23h00_23h15", "23h15_23h30", "23h30_23h45", "23h45_24h00"];


    String eventFile = folder + "/output_events.xml.gz";
    String networkFile = folder + "/nantes_network.xml.gz";
    String populationFile = folder + "/nantes_population.xml.gz";
    String configFile = folder + "/nantes_config.xml";

    Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
    PopulationReader populationReader = new PopulationReader(scenario);
    populationReader.readFile(populationFile);
    Population population = scenario.getPopulation();

    Map<Id<Person>, Person> persons = (Map<Id<Person>, Person>) population.getPersons();

    Sql sql = new Sql(connection)

    sql.execute("DROP TABLE IF EXISTS " + outTableName)
    sql.execute("CREATE TABLE " + outTableName + ''' ( 
        ID integer PRIMARY KEY AUTO_INCREMENT, 
        PERSON_ID varchar(255),
        HOME_FACILITY_ID varchar(255),
        HOME_GEOM geometry,
        WORK_FACILITY_ID varchar(255),
        LAEQ real
    );''')

    Statement stmt = connection.createStatement();

    for (Map.Entry<Id<Person>, Person> entry : persons.entrySet()) {
        String personId = entry.getKey().toString();
        Person person = entry.getValue();
        Plan plan = person.getSelectedPlan();

        String homeId = "";
        String homeGeom = "";
        String workId = "";
        for (PlanElement element : plan.getPlanElements()) {
            if (!(element instanceof Activity)) {
                continue;
            }
            Activity activity = (Activity) element;
            String activityId = activity.getFacilityId().toString();
            if (activity.getType() == "home") {
                homeId = activityId;
                homeGeom = String.format("POINT(%s %s)", Double.toString(activity.getCoord().getX()), Double.toString(activity.getCoord().getY()))
            }
            if (activity.getType().contains("work")) {
                workId = activityId;
            }
        }

        double LAeq = -99.0;

        if (timeSlice == "hour") {
            for (int h = 0; h < 24; h++) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (!(element instanceof Activity)) {
                        continue;
                    }
                    Activity activity = (Activity) element;
                    String activityId = activity.getFacilityId().toString();
                    double activityStart = 0;
                    if (activity.getStartTime().isDefined()) {
                        activityStart = activity.getStartTime().seconds();
                    }
                    double activityEnd = 86400;
                    if (activity.getEndTime().isDefined()) {
                        activityEnd = activity.getEndTime().seconds();
                    }
                    double timeSliceStart = 3600 * h;
                    double timeSliceEnd = 3600 * (h+1);
                    double timeWeight = 0.0;

                    if (activityStart >= activityEnd) {
                        continue;
                    }
                    if (activityStart >= timeSliceEnd || activityEnd < timeSliceStart) {
                        continue;
                    }

                    // exemples with timeslice : 1_2
                    if (activityStart <= timeSliceStart) { // activity starts before the current timeslice  (ie. 00:05:07)
                        if (activityEnd > timeSliceEnd) { // activity ends after the current timeslice (ie. 02:30:00)
                            timeWeight = 1 / 24;
                        }
                        if (activityEnd < timeSliceEnd) { // activity ends in current timeslice (ie. 01:38:00)
                            timeWeight = ((activityEnd - timeSliceStart) / 3600) / 24;
                        }
                    }
                    if (activityStart > timeSliceStart && activityStart < timeSliceEnd) { // activity start is in the current timeslice  (ie. 01:05:07)
                        if (activityEnd > timeSliceEnd) { // activity ends after the current timeslice (ie. 02:30:00)
                            timeWeight = ((timeSliceEnd - activityStart) / 3600) / 24;

                        }
                        if (activityEnd < timeSliceEnd) { // activity ends in current timeslice (ie. 01:38:00)
                            timeWeight = ((activityEnd - activityStart) / 3600) / 24;
                        }
                    }
                    String timeString = hourClock[h];
                    String query = '''
                                    SELECT D.LEQA
                                    FROM ''' + dataTablePrefix + timeString + ''' D
                                    INNER JOIN RECEIVERS R
                                    ON D.IDRECEIVER = R.PK
                                    WHERE FACILITY_ID = \'''' + activityId + '''\'
                                '''
                    ResultSet result = stmt.executeQuery(query);
                    while(result.next()) {
                        LAeq = 10 * Math.log10(Math.pow(10, LAeq / 10) + timeWeight * Math.pow(10, result.getDouble("LEQA") / 10));
                    }
                }
            }
        }

        if (timeSlice == "quarter") {
            for (int q = 0; q < (24 * 4); q++) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (!(element instanceof Activity)) {
                        continue;
                    }
                    Activity activity = (Activity) element;
                    String activityId = activity.getFacilityId().toString();
                    double activityStart = 0;
                    if (activity.getStartTime().isDefined()) {
                        activityStart = activity.getStartTime().seconds();
                    }
                    double activityEnd = 86400;
                    if (activity.getEndTime().isDefined()) {
                        activityEnd = activity.getEndTime().seconds();
                    }
                    double timeSliceStart = 900 * q;
                    double timeSliceEnd = 900 * (q+1);
                    double timeWeight = 0.0;

                    if (activityStart >= activityEnd) {
                        continue;
                    }
                    if (activityStart >= timeSliceEnd || activityEnd < timeSliceStart) {
                        continue;
                    }

                    // exemples with timeslice : 1_2
                    if (activityStart <= timeSliceStart) { // activity starts before the current timeslice  (ie. 00:05:07)
                        if (activityEnd > timeSliceEnd) { // activity ends after the current timeslice (ie. 02:30:00)
                            timeWeight = 1 / (24 * 4);
                        }
                        if (activityEnd < timeSliceEnd) { // activity ends in current timeslice (ie. 01:38:00)
                            timeWeight = ((activityEnd - timeSliceStart) / 900) / (24 * 4);
                        }
                    }
                    if (activityStart > timeSliceStart && activityStart < timeSliceEnd) { // activity start is in the current timeslice  (ie. 01:05:07)
                        if (activityEnd > timeSliceEnd) { // activity ends after the current timeslice (ie. 02:30:00)
                            timeWeight = ((timeSliceEnd - activityStart) / 900) / (24 * 4);

                        }
                        if (activityEnd < timeSliceEnd) { // activity ends in current timeslice (ie. 01:38:00)
                            timeWeight = ((activityEnd - activityStart) / 900) / (24 * 4);
                        }
                    }
                    String timeString = hourClock[h];
                    String query = '''
                                    SELECT D.LEQA
                                    FROM ''' + dataTablePrefix + timeString + ''' D
                                    INNER JOIN RECEIVERS R
                                    ON D.IDRECEIVER = R.PK
                                    WHERE FACILITY_ID = ''' + activityId + '''
                                '''
                    ResultSet result = stmt.executeQuery(query);
                    while(result.next()) {
                        LAeq = 10 * Math.log10(Math.pow(10, LAeq / 10) + timeWeight * Math.pow(10, result.getDouble("LEQA") / 10));
                    }
                }
            }
        }
        println "Person id : " + personId + " : " + String.format("%.1f", LAeq) + " dB(A)"

        sql.execute("INSERT INTO " + outTableName + " VALUES(NULL, \'" + personId + "\', \'" + homeId + "\', ST_GeomFromText(\'" + homeGeom + "\', 2154), \'" + workId + "\', " + LAeq + ")")
    }
}
