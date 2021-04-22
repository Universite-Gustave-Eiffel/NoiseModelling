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
 */

package org.noise_planet.noisemodelling.wps.Experimental_Matsim

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.annotations.XYBoxAnnotation
import org.jfree.chart.annotations.XYLineAnnotation
import org.jfree.chart.annotations.XYTextAnnotation
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.AbstractRenderer
import org.jfree.data.time.FixedMillisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.Scenario
import org.matsim.api.core.v01.population.Activity
import org.matsim.api.core.v01.population.Leg
import org.matsim.api.core.v01.population.Person
import org.matsim.api.core.v01.population.Plan
import org.matsim.api.core.v01.population.PlanElement
import org.matsim.api.core.v01.population.Population
import org.matsim.core.config.ConfigUtils
import org.matsim.core.population.io.PopulationReader
import org.matsim.core.scenario.ScenarioUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.sql.DatabaseMetaData
import javax.swing.JFrame
import java.awt.BorderLayout
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

title = 'Calculate Mastim agents exposure'
description = "Loads a Matsim plans.xml file and calculate agents noise exposure, based on previously claculated timesliced noisemap at receiver positions, linked with matsim activities (facilities)"

inputs = [
        plansFile: [
                name: 'Path of the Matsim output_plans file',
                title: 'Path of the Matsim output_plans file',
                description: 'Path of the Matsim output_plans file </br> For example : /home/mastim/simulation_output/output_plans.xml.gz',
                type: String.class
        ],
        receiversTable: [
                name: 'Table containing the receivers position',
                title: 'Table containing the receivers position',
                description: 'Table containing the receivers position' +
                        '<br/>The table must contain the following fields :' +
                        '<br/>PK, FACILITY, ORIGIN_GEOM, THE_GEOM, TYPES',
                type: String.class
        ],
        dataTable: [
                name: 'Table containing the noise data',
                title: 'Table containing the noise data',
                description: 'Table containing the noise data' +
                        '<br/>The table must contain the following fields :' +
                        '<br/>PK, IDRECEIVER, THE_GEOM, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ000, HZ8000, TIMESTRING',
                type: String.class
        ],
        timeSlice: [
                name: 'How to separate Roads statistics ? hour, quarter',
                title: 'How to separate Roads statistics ? hour, quarter',
                description: 'How to separate Roads statistics ? hour, quarter. "DEN" timslice is not supported for now' +
                        '<br/>This will determine the timstrings used in analysing the time data' +
                        '<br/>For exemple with a timeSlice "hour", the data will be analysed using the following timStrings: ' +
                        '<br/>0_1, 1_2, 2_3, ..., 22_23, 23_24',
                type: String.class
        ],
        SRID : [
                name: 'Projection identifier',
                title: 'Projection identifier',
                description: 'Original projection identifier (also called SRID) of your tables.' +
                        'It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).' +
                        '</br><b> Default value : 4326 </b> ',
                min: 0,
                max: 1,
                type: Integer.class
        ],
        outTableName: [
                name: 'Output table name',
                title: 'Name of created table',
                description: 'Name of the table you want to create from the file.' +
                        '<br/>The table will contain the following fields :' +
                        '<br/>PK, PERSON_ID, HOME_FACILITY, HOME_GEOM, WORK_FACILITY, WORK_GEOM, LAEQ, HOME_LAEQ, DIFF_LAEQ',
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
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, input) {

    connection = new ConnectionWrapper(connection)
    Sql sql = new Sql(connection)

    String resultString

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Agent_Exposure')
    logger.info("inputs {}", input)

    String plansFile = input["plansFile"];
    String receiversTable = input["receiversTable"];
    String dataTable = input["dataTable"];
    String outTableName = input['outTableName'];

    String timeSlice = "hour";
    /*
    if (input["timeSlice"] == "DEN") {
        timeSlice = input["timeSlice"];
    }
    */
    if (input["timeSlice"] == "quarter") {
        timeSlice = input["timeSlice"];
    }
    if (!["hour", "quarter"].contains(timeSlice)) {
        logger.warn('timeSlice not in ["hour", "quarter"], setting it to "hour"')
    }

    String SRID = "4326"
    if (input['SRID']) {
        SRID = input['SRID'];
    }

    // Undocumented on purpose for now
    int agentId = 0;
    if (input["plotOneAgentId"] && input["plotOneAgentId"] as int != 0) {
        agentId = input["plotOneAgentId"];
    }

    String[] den = ["D", "E", "N"];
    String[] hourClock = ["0_1", "1_2", "2_3", "3_4", "4_5", "5_6", "6_7", "7_8", "8_9", "9_10", "10_11", "11_12", "12_13", "13_14", "14_15", "15_16", "16_17", "17_18", "18_19", "19_20", "20_21", "21_22", "22_23", "23_24"];
    String[] quarterClock = ["0h00_0h15", "0h15_0h30", "0h30_0h45", "0h45_1h00", "1h00_1h15", "1h15_1h30", "1h30_1h45", "1h45_2h00", "2h00_2h15", "2h15_2h30", "2h30_2h45", "2h45_3h00", "3h00_3h15", "3h15_3h30", "3h30_3h45", "3h45_4h00", "4h00_4h15", "4h15_4h30", "4h30_4h45", "4h45_5h00", "5h00_5h15", "5h15_5h30", "5h30_5h45", "5h45_6h00", "6h00_6h15", "6h15_6h30", "6h30_6h45", "6h45_7h00", "7h00_7h15", "7h15_7h30", "7h30_7h45", "7h45_8h00", "8h00_8h15", "8h15_8h30", "8h30_8h45", "8h45_9h00", "9h00_9h15", "9h15_9h30", "9h30_9h45", "9h45_10h00", "10h00_10h15", "10h15_10h30", "10h30_10h45", "10h45_11h00", "11h00_11h15", "11h15_11h30", "11h30_11h45", "11h45_12h00", "12h00_12h15", "12h15_12h30", "12h30_12h45", "12h45_13h00", "13h00_13h15", "13h15_13h30", "13h30_13h45", "13h45_14h00", "14h00_14h15", "14h15_14h30", "14h30_14h45", "14h45_15h00", "15h00_15h15", "15h15_15h30", "15h30_15h45", "15h45_16h00", "16h00_16h15", "16h15_16h30", "16h30_16h45", "16h45_17h00", "17h00_17h15", "17h15_17h30", "17h30_17h45", "17h45_18h00", "18h00_18h15", "18h15_18h30", "18h30_18h45", "18h45_19h00", "19h00_19h15", "19h15_19h30", "19h30_19h45", "19h45_20h00", "20h00_20h15", "20h15_20h30", "20h30_20h45", "20h45_21h00", "21h00_21h15", "21h15_21h30", "21h30_21h45", "21h45_22h00", "22h00_22h15", "22h15_22h30", "22h30_22h45", "22h45_23h00", "23h00_23h15", "23h15_23h30", "23h30_23h45", "23h45_24h00"];

    String populationFile = plansFile;
    File file = new File(populationFile);
    if(!file.exists() || file.isDirectory()) {
        throw new FileNotFoundException(populationFile, populationFile + " not found");
    }
    DatabaseMetaData dbMeta = connection.getMetaData();

    logger.info("searching index on data table... ")
    ResultSet rs = dbMeta.getIndexInfo(null, null, dataTable, false, false);
    boolean indexIDRECEIVER = false;
    boolean indexTIMESTRING = false;
    while (rs.next()) {
        String column = rs.getString("COLUMN_NAME");
        if (column == "IDRECEIVER") {
            indexIDRECEIVER = true;
            logger.info("index on data table IDSOURCE found")
        }
        else if (column == "TIMESTRING") {
            indexTIMESTRING = true;
            logger.info("index on data table TIMESTRING found")
        }
    }

    if (!indexIDRECEIVER) {
        logger.info("index on data table IDRECEIVER, NOT found, creating one...")
        sql.execute("CREATE INDEX ON " + dataTable + " (IDRECEIVER)");
    }
    if (!indexTIMESTRING) {
        logger.info("index on data table TIMESTRING, NOT found, creating one...")
        sql.execute("CREATE INDEX ON " + dataTable + " (TIMESTRING)");
    }

    logger.info("searching index on receivers table... ")
    rs = dbMeta.getIndexInfo(null, null, receiversTable, false, false);
    boolean indexFACILITY = false;
    while (rs.next()) {
        String column = rs.getString("COLUMN_NAME");
        if (column == "FACILITY") {
            indexFACILITY = true;
            logger.info("index on receivers table FACILITY found")
        }
    }

    if (!indexFACILITY) {
        logger.info("index on receivers table FACILITY, NOT found, creating one...")
        sql.execute("CREATE INDEX ON " + receiversTable + " (FACILITY)");
    }

    Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
    PopulationReader populationReader = new PopulationReader(scenario);
    populationReader.readFile(populationFile);
    Population population = scenario.getPopulation();

    Map<Id<Person>, Person> persons = (Map<Id<Person>, Person>) population.getPersons();

    if (agentId == 0) {
        sql.execute("DROP TABLE IF EXISTS " + outTableName)
        sql.execute("DROP TABLE IF EXISTS " + outTableName + "_SEQUENCE")
        sql.execute("CREATE TABLE " + outTableName + ''' (
            PK integer PRIMARY KEY AUTO_INCREMENT,
            PERSON_ID varchar(255),
            AGE int,
            SEX varchar,
            INCOME double,
            EMPLOYED double,
            HOME_FACILITY varchar(255),
            HOME_GEOM geometry,
            WORK_FACILITY varchar(255),
            WORK_GEOM geometry,
            LDEN real,
            HOME_LDEN real,
            DIFF_LDEN real
        );''')
        sql.execute("CREATE TABLE " + outTableName + '''_SEQUENCE (
            PK integer PRIMARY KEY AUTO_INCREMENT,
            PERSON_ID varchar(255),
            TIMESTRING varchar,
            TIMEINDEX int,
            LEVEL double,
            ACTIVITY varchar,
            THE_GEOM geometry
        );''')
    }
    Statement stmt = connection.createStatement();

    int doprint = 1
    int counter = 0
    for (Map.Entry<Id<Person>, Person> entry : persons.entrySet()) {
        String personId = entry.getKey().toString();
        Person person = entry.getValue();
        def attributes = person.getAttributes();
        Integer age = attributes.getAttribute("age")
        String sex = attributes.getAttribute("sex")
        Double income = attributes.getAttribute("householdIncome")
        Boolean employed = attributes.getAttribute("employed")
        Plan plan = person.getSelectedPlan();

        if (agentId > 0 && personId != agentId.toString()) {
            continue;
        }
        if (agentId > 0) {
            logger.info(plan.dump());
        }

        TimeSeries laeqSeries = new TimeSeries("Agent_LAeq");
        TimeSeries homeLaeqSeries = new TimeSeries("Home_LAeq");
        TimeSeries doseSeries = new TimeSeries("Agent_Dose");
        TimeSeries homeDoseSeries = new TimeSeries("Home_Dose");

        String homeId = "";
        String homeGeom = "";
        String workId = "";
        String workGeom = "";
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
                workGeom = String.format("POINT(%s %s)", Double.toString(activity.getCoord().getX()), Double.toString(activity.getCoord().getY()))
            }
        }

        double LAeq = -99.0;
        double homeLAeq = -99.0;

        String[] clock;
        if (timeSlice == "hour") {
            clock = hourClock;
        }
        else if (timeSlice == "quarter") {
            clock = quarterClock;
        }
        int nbSlices = clock.size();
        int secondsInSlice = (int) 86400 / nbSlices;
        double correctionLDEN = 0.0;

        String[] activityGeomSequence = new String[clock.length];
        Double[] laeqSequence = new Double[clock.length];
        String[] activitySequence = new String[clock.length];

        for (int slice = 0; slice < nbSlices; slice++) {

            double timeSliceStart = secondsInSlice * slice;
            double timeSliceEnd = secondsInSlice * (slice+1);

            if (timeSliceStart < (6 * 60 * 60) || timeSliceStart >= (22 * 60 * 60)) { // 0h - 6h ; 22h - 0h
                correctionLDEN = 10.0;
            }
            else if (timeSliceStart >= (18 * 60 * 60) && timeSliceStart < (22 * 60 * 60)) { // 18h - 22h
                correctionLDEN = 5.0;
            }

            boolean hasActivity = false;
            boolean isOutside = false;
            boolean hasLevel = false; // in cas there is no propagation path arriving to this facility's receiver.
            boolean hasHomeLevel = false; // idem for home
            for (PlanElement element : plan.getPlanElements()) {
                if (!(element instanceof Activity)) {
                    continue;
                }
                Activity activity = (Activity) element;
                String activityId = activity.getFacilityId().toString();
                if (activityId == "null") { // pt interaction ?
                    continue;
                }
                if (activity.type == "outside") {
                    isOutside = true
                    continue;
                }
                double activityStart = 0;
                if (activity.getStartTime() > 0) {
                    activityStart = activity.getStartTime();
                }
                double activityEnd = 86400;
                if (activity.getEndTime() > 0) {
                    activityEnd = activity.getEndTime();
                }
                double timeWeight = 0.0;

                if (activityStart >= activityEnd) {
                    continue;
                }
                if (activityStart >= timeSliceEnd || activityEnd < timeSliceStart) {
                    continue;
                }

                hasActivity = true;
                activitySequence[slice] = activity.type
                activityGeomSequence[slice] = String.format("POINT(%s %s)", Double.toString(activity.getCoord().getX()), Double.toString(activity.getCoord().getY()))

                // exemples with timeslice : 1_2
                if (activityStart <= timeSliceStart) { // activity starts before the current timeslice  (ie. 00:05:07)
                    if (activityEnd > timeSliceEnd) { // activity ends after the current timeslice (ie. 02:30:00)
                        timeWeight = 1 / nbSlices;
                    }
                    if (activityEnd < timeSliceEnd) { // activity ends in current timeslice (ie. 01:38:00)
                        timeWeight = ((activityEnd - timeSliceStart) / secondsInSlice) / nbSlices;
                    }
                }
                if (activityStart > timeSliceStart && activityStart < timeSliceEnd) { // activity start is in the current timeslice  (ie. 01:05:07)
                    if (activityEnd > timeSliceEnd) { // activity ends after the current timeslice (ie. 02:30:00)
                        timeWeight = ((timeSliceEnd - activityStart) / secondsInSlice) / nbSlices;
                    }
                    if (activityEnd < timeSliceEnd) { // activity ends in current timeslice (ie. 01:38:00)
                        timeWeight = ((activityEnd - activityStart) / secondsInSlice) / nbSlices;
                    }
                }
                String timeString = clock[slice];
                String query = '''
                                    SELECT D.LEQA
                                    FROM ''' + dataTable + ''' D
                                    INNER JOIN ''' + receiversTable + ''' R
                                    ON D.IDRECEIVER = R.PK
                                    WHERE D.TIMESTRING = \''''+timeString+'''\' 
                                    AND R.FACILITY = \'''' + activityId + '''\'
                                '''
                ResultSet result = stmt.executeQuery(query);
                while(result.next()) {
                    LAeq = 10 * Math.log10(Math.pow(10, LAeq / 10) + timeWeight * Math.pow(10, (result.getDouble("LEQA") + correctionLDEN) / 10));
                    laeqSeries.addOrUpdate(new FixedMillisecond(slice * secondsInSlice * 1000), result.getDouble("LEQA") + correctionLDEN);
                    laeqSequence[slice] = result.getDouble("LEQA")
                    if (homeId == activityId) {
                        homeLAeq = 10 * Math.log10(Math.pow(10, homeLAeq / 10) + timeWeight * Math.pow(10,  (result.getDouble("LEQA") + correctionLDEN) / 10));
                        homeLaeqSeries.addOrUpdate(new FixedMillisecond(slice * secondsInSlice * 1000), result.getDouble("LEQA") + correctionLDEN);
                        hasHomeLevel = true;
                    }
                    hasLevel = true;
                }
                if (homeId != activityId) {
                    String homeQuery = '''
                                    SELECT D.LEQA
                                    FROM ''' + dataTable + ''' D
                                    INNER JOIN ''' + receiversTable + ''' R
                                    ON D.IDRECEIVER = R.PK
                                    WHERE D.TIMESTRING = \''''+timeString+'''\'
                                    AND R.FACILITY = \'''' + homeId + '''\'
                                '''
                    ResultSet homeResult = stmt.executeQuery(homeQuery);
                    while (homeResult.next()) {
                        homeLAeq = 10 * Math.log10(Math.pow(10, homeLAeq / 10) + timeWeight * Math.pow(10, (homeResult.getDouble("LEQA") + correctionLDEN) / 10));
                        homeLaeqSeries.addOrUpdate(new FixedMillisecond(slice * secondsInSlice * 1000), homeResult.getDouble("LEQA") + correctionLDEN);
                        hasHomeLevel = true;
                    }
                }
            }

            doseSeries.addOrUpdate(new FixedMillisecond(slice * secondsInSlice * 1000), LAeq);
            homeDoseSeries.addOrUpdate(new FixedMillisecond(slice * secondsInSlice * 1000), homeLAeq);

            if (!hasLevel) {
                laeqSeries.addOrUpdate(new FixedMillisecond(slice * secondsInSlice * 1000), null);
                laeqSequence[slice] = -99.0
            }
            if (!hasHomeLevel) {
                homeLaeqSeries.addOrUpdate(new FixedMillisecond(slice * secondsInSlice * 1000), null);
            }
            if (!hasActivity) {
                if (isOutside) {
                    activitySequence[slice] = "outside"
                }
                else {
                    activitySequence[slice] = "travelling"
                }
                activityGeomSequence[slice] = "NULL"
            }
        }

        if (agentId > 0) {
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            dataset.addSeries(laeqSeries);
            dataset.addSeries(doseSeries);
            dataset.addSeries(homeLaeqSeries);
            dataset.addSeries(homeDoseSeries);
            JFreeChart chart = ChartFactory.createXYStepChart("AgentId : " + agentId, "Time", "Noise Level [dB(A)]", dataset);
            ChartPanel chartPanel = new ChartPanel(chart);
            XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(Color.WHITE)
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlinePaint(Color.DARK_GRAY);
            AbstractRenderer render = (AbstractRenderer) plot.getRenderer(0);

            render.setBaseStroke(new BasicStroke(2.0f));
            render.setAutoPopulateSeriesStroke(false);

            render.setSeriesPaint(0, Color.RED)
            render.setSeriesPaint(1, Color.ORANGE)
            render.setSeriesPaint(2, Color.BLUE)
            render.setSeriesPaint(3, Color.CYAN)
            //plot.setShadowGenerator(new DefaultShadowGenerator());
            NumberAxis rangeAxis = (NumberAxis)plot.getRangeAxis();
            rangeAxis.setRange(0.0, 90.0);
            DateAxis timeAxis = (DateAxis)plot.getDomainAxis();
            timeAxis.setTimeZone(TimeZone.getTimeZone("UTC"))
            double yAnnotationPos = 1;
            for (PlanElement element : plan.getPlanElements()) {
                if (element instanceof Activity) {
                    Activity activity = (Activity) element;
                    String activityId = activity.getFacilityId().toString();
                    if (activityId == "null") { // pt interaction ?
                        continue;
                    }
                    double activityStart = 0;
                    if (activity.getStartTime() > 0) {
                        activityStart = activity.getStartTime();
                    }
                    double activityEnd = 86400;
                    if (activity.getEndTime() > 0) {
                        activityEnd = activity.getEndTime();
                    }

                    if (activityStart >= activityEnd) {
                        continue;
                    }
                    double lineX1 = (activityStart * 1000);
                    double lineX2 = (activityEnd * 1000);
                    double textX = (activityStart + (activityEnd - activityStart) / 2) * 1000;
                    XYLineAnnotation lineAnnotation = new XYLineAnnotation(lineX1, yAnnotationPos, lineX2, yAnnotationPos);
                    XYTextAnnotation textAnnotation = new XYTextAnnotation(activity.getType().toUpperCase(), textX, yAnnotationPos + 2);
                    textAnnotation.setFont(new Font("Default", Font.BOLD, 14));
                    plot.addAnnotation(lineAnnotation);
                    plot.addAnnotation(textAnnotation);
                    yAnnotationPos = (yAnnotationPos + 2) % 4;
                }
                else if (element instanceof Leg) {
                    Leg leg = (Leg) element;
                    double legStart = 0;
                    if (leg.getDepartureTime() > 0) {
                        legStart = leg.getDepartureTime();
                    }
                    double legEnd = 86400;
                    if (leg.getTravelTime() > 0) {
                        legEnd = legStart + leg.getTravelTime();
                    }
                    double boxX1 = (legStart * 1000);
                    double boxX2 = (legEnd * 1000);
                    XYBoxAnnotation boxAnnotation = new XYBoxAnnotation(boxX1, 0, boxX2, 15);
                    plot.addAnnotation(boxAnnotation);
                }
            }
            JFrame f = new JFrame("RESULT");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLayout(new BorderLayout(0, 5));
            f.add(chartPanel, BorderLayout.CENTER);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        }
        if (counter == doprint) {
            logger.info("Person # " + counter)
            doprint *= 4
        }

        if (agentId == 0) {
            /*
            ID integer PRIMARY KEY AUTO_INCREMENT,
            PERSON_ID varchar(255),
            HOME_FACILITY varchar(255),
            HOME_GEOM geometry,
            WORK_FACILITY varchar(255),
            WORK_GEOM geometry,
            LAEQ real,
            HOME_LAEQ real
            DIFF_LAEQ real,
             */
            sql.execute("INSERT INTO " + outTableName + " VALUES(" +
                    "NULL, '" + personId + "', " + age + ", '" + sex + "', " + income + ", " + employed + ", " +
                    "'" + homeId + "', ST_GeomFromText('" + homeGeom + "', "+SRID+"), '" + workId + "', ST_GeomFromText('" + workGeom + "', "+SRID+"), " + LAeq + ", " + homeLAeq + ", " + (LAeq - homeLAeq)+ ")")

            String laeqQuery = "INSERT INTO " + outTableName + "_SEQUENCE VALUES(NULL, '" + personId + "'"
            for (int i = 0; i < clock.length; i++) {
                String query = laeqQuery + ", "
                query += "'" + clock[i] + "', "
                query += i + ", "
                query += laeqSequence[i] + ", "
                query += "'" + activitySequence[i] + "', "
                if (activityGeomSequence[i] == "NULL") {
                    query += "NULL"
                } else {
                    query += "ST_GeomFromText('" + activityGeomSequence[i] + "', "+SRID+")"
                }
                query += ")"
                sql.execute(query)
            }
        }
        counter++;
    }

    logger.info('End : Agent_Exposure')
    resultString = "Process done. Table " + outTableName + " created !"
    logger.info('Result : ' + resultString)
    return resultString
}
