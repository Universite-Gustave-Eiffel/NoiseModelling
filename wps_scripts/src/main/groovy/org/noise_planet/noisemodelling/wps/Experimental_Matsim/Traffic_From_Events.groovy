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
import org.locationtech.jts.geom.Coordinate
import org.matsim.api.core.v01.Coord
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.events.LinkEnterEvent
import org.matsim.api.core.v01.events.LinkLeaveEvent
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler
import org.matsim.api.core.v01.network.Link
import org.matsim.api.core.v01.network.Network
import org.matsim.api.core.v01.population.Person
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.EventsUtils
import org.matsim.core.events.MatsimEventsReader
import org.matsim.core.network.io.MatsimNetworkReader
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.vehicles.Vehicle
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos
import org.noise_planet.noisemodelling.emission.RoadSourceParametersCnossos
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Import traffic data from Mastim simultaion output folder'
description = 'Read Mastim events output file in order to get traffic NoiseModelling input'

inputs = [
    folder: [
        name: 'Path of the Matsim output folder',
        title: 'Path of the Matsim output folder',
        description: 'Path of the Matsim output folder </br> For example : /home/mastim/simulation_output' +
                '<br/>The folder must contain at least the following files: ' +
                '<br/><br/> - output_network.xml.gz' +
                '<br/><br/> - output_events.xml.gz',
        type: String.class
    ],
    timeSlice: [
        name: 'Time Quantification',
        title: 'Time Quantification',
        description: 'How to handle time when reading traffic data ?' +
                '<br/>Must be one of the following strings: ' +
                '<br/><br/> - <b>DEN</b>, to analyse data in day (6h-18h), evening (18h-22h) and night (22h-6h) time periods' +
                '<br/><br/> - <b>hour</b>, to analyse data in 60-minutes time periods' +
                '<br/><br/> - <b>quarter</b>, to analyse data in 15-minutes time periods' +
                '<br/>Default : <b>hour</b>',
        type: String.class
    ],
    populationFactor: [
            name: 'Population Factor',
            title: 'Population Factor',
            description: 'Set the population factor of the MATSim simulation' +
                    '<br/>Must be a decimal number between 0 and 1' +
                    '<br/>Default: 1.0',
            min: 0,
            max: 1,
            type: String.class
    ],
    link2GeometryFile: [
        name: 'Network CSV file',
        title: 'Network CSV file',
        description: 'The path of the pt2matsim CSV file generated when importing OSM network. Ignored if not set.' +
                '<br/>The file must contain at least two columns : ' +
                '<br/><br/> - The link ID' +
                '<br/><br/> - The WKT geometry',
        min: 0,
        max: 1,
        type: String.class
    ],
    SRID : [
            name: 'Projection identifier',
            title: 'Projection identifier',
            description: 'Projection identifier (also called SRID) of the geometric data.' +
                    'It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).' +
                    '</br><b> Default value : 4326 </b> ',
            min: 0,
            max: 1,
            type: Integer.class
    ],
    exportTraffic: [
            name: 'Export additionnal traffic data ?',
            title: 'Export additionnal traffic data ?',
            description: 'Define if you want to output average speed and flow per vehicle category in an additional table' +
                    '<br/>Default: False',
            min: 0,
            max: 1,
            type: Boolean.class
    ],
    skipUnused: [
        name: 'Skip unused links ?',
        title: 'Skip unused links ?',
        description: 'Define if links with unused traffic should be omitted in the output table.' +
                '<br/>Default: True',
        min: 0,
        max: 1,
        type: Boolean.class
    ],
    perVehicleLevel: [
            name: 'Calculate All vehicles noise source ?',
            title: 'Calculate All vehicles noise source ?',
            description: 'Choose between :' +
                    '<br/><b>False</b>Calculating the average speed of all vehicles and the applying CNOSSOS model to get noise source power level' +
                    '<br/><b>True</b>Applying CNOSSOS model to every vehicle and sum all vehicles noise power per time perdiod to get the total noise source power level' +
                    '<br/>Default: False',
            min: 0,
            max: 1,
            type: Boolean.class
    ],
    ignoreAgents: [
        name: 'List of agents ids to ignore in import',
        title: 'List of agents ids to ignore in import',
        description: 'List of agents ids to ignore in import. These agents will be filtered out when reading the event file.' +
                '<br/>Please note that their contribution to the general traffic (congestion, vehicle or pt occupancy, etc.) still exists in the other agents bahavior.' +
                '<br/>Default: [], an empty list',
        min: 0,
        max: 1,
        type: String.class
    ],
    outTableName: [
        name: 'Output table name',
        title: 'Output table name',
        description: 'Name of the table you want to create.' +
                '<br/>A table with this name will be created plus another with a "_STATS" suffix' +
                '<br/>For exemple if set to "MATSIM_ROADS (default value)":' +
                '<br/><br/> - the table MATSIM_ROADS, with the link ID and the geometry field' +
                '<br/><br/> - the table MATSIM_ROADS_STATS, with the link ID and the traffic data',
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
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, input) {

    connection = new ConnectionWrapper(connection)
    Sql sql = new Sql(connection)

    String resultString = null

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Traffic_From_Events')
    logger.info("inputs {}", input)

    String folder = input["folder"];

    String outTableName = "MATSIM_ROADS";
    if (input["outTableName"]) {
        outTableName = input["outTableName"];
    }
    String statsTableName = outTableName + "_STATS"
    String trafficTableName = outTableName + "_TRAFFIC"
    String altStatsTableName = statsTableName + "_ALT"
    String altTrafficTableName = trafficTableName + "_ALT"

    String link2GeometryFile = "";
    if (input["link2GeometryFile"]) {
        link2GeometryFile = input["link2GeometryFile"];
    }

    String timeSlice = "hour";
    if (input["timeSlice"] == "DEN") {
        timeSlice = input["timeSlice"];
    }
    if (input["timeSlice"] == "quarter") {
        timeSlice = input["timeSlice"];
    }
     if (!["DEN", "hour", "quarter"].contains(timeSlice)) {
         logger.warn('timeSlice not in ["DEN", "hour", "quarter"], setting it to "hour"')
     }

    String SRID = "4326"
    if (input['SRID']) {
        SRID = input['SRID'];
    }

    boolean skipUnused = false;
    if (input["skipUnused"]) {
        skipUnused = input["skipUnused"] as boolean;
    }

    boolean perVehicleLevel = false;
    if (input["perVehicleLevel"]) {
        perVehicleLevel = input["perVehicleLevel"] as boolean;
    }

    boolean exportTraffic = false;
    if (input["exportTraffic"]) {
        exportTraffic = input["exportTraffic"] as boolean;
    }

    double populationFactor = 1.0;
    if (input["populationFactor"]) {
        populationFactor = input["populationFactor"] as double;
    }

    String[] ignoreAgents = new String[0];
    if (input["ignoreAgents"]) {
        String inputIgnoreAgents = input["ignoreAgents"] as String;
        ignoreAgents = inputIgnoreAgents.trim().split("\\s*,\\s*");
    }
    File f;
    String eventFile = folder + "/output_events.xml.gz";
    f = new File(eventFile);
    if(!f.exists() || f.isDirectory()) {
        throw new FileNotFoundException(eventFile, "output_events.xml.gz not found in MATSim folder");
    }
    String networkFile = folder + "/output_network.xml.gz";
    f = new File(networkFile);
    if(!f.exists() || f.isDirectory()) {
        throw new FileNotFoundException(networkFile, "output_network.xml.gz not found in MATSim folder");
    }
    if (link2GeometryFile != "") {
        f = new File(link2GeometryFile);
        if(!f.exists() || f.isDirectory()) {
            throw new FileNotFoundException(link2GeometryFile, link2GeometryFile + " not found");
        }
    }

    logger.info("Create SQL tables : " + outTableName + " & " + statsTableName);
    // Open connection
    sql.execute("DROP TABLE IF EXISTS " + outTableName)
    sql.execute("CREATE TABLE " + outTableName + '''( 
        PK integer PRIMARY KEY AUTO_INCREMENT, 
        LINK_ID varchar(255),
        OSM_ID varchar(255),
        THE_GEOM geometry
    );''')

    sql.execute("DROP TABLE IF EXISTS " + statsTableName)
    sql.execute("CREATE TABLE " + statsTableName + '''( 
        PK integer PRIMARY KEY AUTO_INCREMENT, 
        LINK_ID varchar(255),
        LW63 double precision, LW125 double precision, LW250 double precision, LW500 double precision, LW1000 double precision, LW2000 double precision, LW4000 double precision, LW8000 double precision,
        TIMESTRING varchar(255)
    );''')

    if (exportTraffic) {
        sql.execute("DROP TABLE IF EXISTS " + trafficTableName)
        sql.execute("CREATE TABLE " + trafficTableName + '''( 
            PK integer PRIMARY KEY AUTO_INCREMENT, 
            LINK_ID varchar(255),
            LV_D double precision,
            LV_SPD_D double precision,
            MV_D double precision,
            MV_SPD_D double precision,
            HGV_D double precision,
            HGV_SPD_D double precision,
            TIMESTRING varchar(255)
        );''')
    }

    if (ignoreAgents.length > 0) {
        sql.execute("DROP TABLE IF EXISTS " + altStatsTableName)
        sql.execute("CREATE TABLE " + altStatsTableName + '''( 
            PK integer PRIMARY KEY AUTO_INCREMENT, 
            LINK_ID varchar(255),
            LW63 double precision, LW125 double precision, LW250 double precision, LW500 double precision, LW1000 double precision, LW2000 double precision, LW4000 double precision, LW8000 double precision,
            TIMESTRING varchar(255)
        );''')
        if (exportTraffic) {
            sql.execute("DROP TABLE IF EXISTS " + altTrafficTableName)
            sql.execute("CREATE TABLE " + altTrafficTableName + '''( 
            PK integer PRIMARY KEY AUTO_INCREMENT, 
            LINK_ID varchar(255),
            LV_D double precision,
            LV_SPD_D double precision,
            MV_D double precision,
            MV_SPD_D double precision,
            HGV_D double precision,
            HGV_SPD_D double precision,
            TIMESTRING varchar(255)
        );''')
        }
    }

    logger.info("Done Creating SQL tables");

    Network network = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
    logger.info("Start reading network file ... ");
    networkReader.readFile(networkFile);
    logger.info("Done reading network file ");

    Map<Id<Link>, Link> links = (Map<Id<Link>, Link>) network.getLinks();

    EventsManager evMgr = EventsUtils.createEventsManager();
    ProcessOutputEventHandler evHandler = new ProcessOutputEventHandler();

    evHandler.setTimeSlice(timeSlice);
    evHandler.setSRID(SRID);
    evHandler.setIgnoreAgents(ignoreAgents);
    evHandler.setPerVehicleLevel(perVehicleLevel);
    evHandler.setPopulationFactor(populationFactor);
    evHandler.initLinks((Map<Id<Link>, Link>) links);

    evMgr.addHandler(evHandler);

    MatsimEventsReader eventsReader = new MatsimEventsReader(evMgr);

    logger.info("Start reading event file ... ");
    eventsReader.readFile(eventFile);
    logger.info("Done reading event file ");

    Map<String, String> link2geomData = new HashMap<>();
    if (!link2GeometryFile.isEmpty()) {
        logger.info("Start Reading link2geom file ...");
        BufferedReader br = new BufferedReader(new FileReader(link2GeometryFile));
        String line =  null;
        while ((line = br.readLine()) != null) {
            String[] str = line.split(",", 2);
            if (str.size() > 1) {
                link2geomData.put(str[0], str[1].trim().replace("\"", ""));
            }
        }
        logger.info("Done Reading link2geom file");
    }

    logger.info("Start Inserting Into SQL tables...");
    int counter = 0;
    int doprint = 1;
    for (Map.Entry<Id<Link>, LinkStatStruct> entry : evHandler.links.entrySet()) {
        String linkId = entry.getKey().toString();
        LinkStatStruct linkStatStruct = entry.getValue();
        if (counter >= doprint) {
            logger.info("link # " + counter)
            doprint *= 4
        }
        counter ++

        if (skipUnused && !linkStatStruct.isUsed) {
            continue;
        }

        String geomString = "";
        if (!link2GeometryFile.isEmpty()) {
            geomString = link2geomData.get(linkId);
        }

        sql.execute(linkStatStruct.toSqlInsertWithGeom(outTableName, statsTableName, geomString, false));
        if (ignoreAgents.length > 0) {
            sql.execute(linkStatStruct.toSqlInsertWithGeom(outTableName, altStatsTableName, geomString, true));
        }
        if (exportTraffic) {
            sql.execute(linkStatStruct.toSqlInsertTraffic(trafficTableName, false));
            if (ignoreAgents.length > 0) {
                sql.execute(linkStatStruct.toSqlInsertTraffic(altTrafficTableName, true));
            }
        }
    }
    logger.info("DONE Inserting Into SQL tables...");

    logger.info("Start Creating indexes on tables ...")
    logger.info("CREATE INDEX " + outTableName + "_LINK_ID_IDX ON " + outTableName + " (LINK_ID);")
    sql.execute("CREATE INDEX " + outTableName + "_LINK_ID_IDX ON " + outTableName + " (LINK_ID);")
    logger.info("CREATE INDEX " + statsTableName + "_LINK_ID_IDX ON " + statsTableName + " (LINK_ID);")
    sql.execute("CREATE INDEX " + statsTableName + "_LINK_ID_IDX ON " + statsTableName + " (LINK_ID);")
    logger.info("CREATE INDEX " + statsTableName + "_TIMESTRING_IDX ON " + statsTableName + " (TIMESTRING);")
    sql.execute("CREATE INDEX " + statsTableName + "_TIMESTRING_IDX ON " + statsTableName + " (TIMESTRING);")
    if (ignoreAgents.length > 0) {
        logger.info("CREATE INDEX " + altStatsTableName + "_LINK_ID_IDX ON " + altStatsTableName + " (LINK_ID);")
        sql.execute("CREATE INDEX " + altStatsTableName + "_LINK_ID_IDX ON " + altStatsTableName + " (LINK_ID);")
        logger.info("CREATE INDEX " + altStatsTableName + "_TIMESTRING_IDX ON " + statsTableName + " (TIMESTRING);")
        sql.execute("CREATE INDEX " + altStatsTableName + "_TIMESTRING_IDX ON " + altStatsTableName + " (TIMESTRING);")
    }

    logger.info("Done Creating indexes on tables.")
    resultString = "Roads stats imported from matsim traffic output"
    logger.info('Result : ' + resultString)
    return resultString
}

public class ProcessOutputEventHandler implements
        LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    Map<Id<Link>, LinkStatStruct> links = new HashMap<Id<Link>, LinkStatStruct>();
    Map<Id<Vehicle>, Id<Person>> personsInVehicle = new HashMap<Id<Vehicle>, Id<Person>>();
    String timeSlice;
    String SRID = 4326;
    boolean perVehicleLevel = false;
    double populationFactor = 1.0;

    String[] ignoreAgents = new String[0];

    public void setIgnoreAgents(String[] ignoreAgents) {
        this.ignoreAgents = ignoreAgents;
    }

    public void setSRID(String srid) {
        this.SRID = srid;
    }

    public void setTimeSlice(String slice) {
        timeSlice = slice;
    }

    public void setPerVehicleLevel(boolean perVehicleLevel) {
        this.perVehicleLevel = perVehicleLevel;
    }

    public void setPopulationFactor(double populationFactor) {
        this.populationFactor = populationFactor;
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (!personsInVehicle.containsKey(event.getVehicleId())) {
            personsInVehicle.put(event.getVehicleId(), event.getPersonId());
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if (personsInVehicle.containsKey(event.getVehicleId())) {
            personsInVehicle.remove(event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

        Id<Link> linkId = event.getLinkId();
        Id<Vehicle> vehicleId = event.getVehicleId();

        double time = event.getTime();

        if (!links.containsKey(linkId)) {
            LinkStatStruct stats = new LinkStatStruct(timeSlice, populationFactor, perVehicleLevel, SRID);
            links.put(linkId, stats);
        }

        LinkStatStruct stats = links.get(linkId);

        stats.vehicleEnterAt(vehicleId, time);

        links.put(linkId, stats);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

        Id<Link> linkId = event.getLinkId();
        Id<Vehicle> vehicleId = event.getVehicleId();
        boolean isIgnored = false;
        if (personsInVehicle.containsKey(vehicleId)) {
            Id<Person> personId =  personsInVehicle.get(vehicleId);
            if (ignoreAgents.contains(personId.toString())) {
                isIgnored = true;
            }
        }
        double time = event.getTime();

        if (!links.containsKey(linkId)) {
            LinkStatStruct stats = new LinkStatStruct(timeSlice, populationFactor, perVehicleLevel, SRID);
            links.put(linkId, stats);
        }

        LinkStatStruct stats = links.get(linkId);
        stats.vehicleLeaveAt(vehicleId, time, isIgnored);
        links.put(linkId, stats);
    }

    public void initLinks(Map<Id<Link>, Link> netLinks) {
        for (Map.Entry<Id<Link>, Link> entry: netLinks.entrySet()) {
            Id<Link> linkId = entry.getKey();
            Link link = entry.getValue();

            if (!links.containsKey(linkId)) {
                LinkStatStruct stats = new LinkStatStruct(timeSlice, populationFactor, perVehicleLevel, SRID);
                stats.setLink(link);
                links.put(linkId, stats);
            }
        }
    }

}

public class Trip {
    enum Type {
        LV, MV, HV
    };

    public String timeString;
    public Id<Vehicle> vehicleId;
    public Type type;
    public boolean ignored;
    public double travelTime;

    public Trip(String timeString, Id<Vehicle> vehicleId, Type type, boolean ignored, double travelTime) {
        this.timeString = timeString;
        this.vehicleId = vehicleId;
        this.type = type;
        this.ignored = ignored;
        this.travelTime = travelTime;
    }
}

public class LinkStatStruct {

    private Map<String, ArrayList<Trip>> trips = new HashMap<String, ArrayList<Trip> >();
    private Map<Id<Vehicle>, Double> enterTimes = new HashMap<Id<Vehicle>, Double>();
    private Link link;

    private String SRID = 4326;
    private boolean perVehicleLevel = false;
    private double populationFactor = 1.0;

    public boolean isUsed = false;

    String timeSlice = "hour";

    static String[] den = ["D", "E", "N"];
    static String[] hourClock = ["0_1", "1_2", "2_3", "3_4", "4_5", "5_6", "6_7", "7_8", "8_9", "9_10", "10_11", "11_12", "12_13", "13_14", "14_15", "15_16", "16_17", "17_18", "18_19", "19_20", "20_21", "21_22", "22_23", "23_24"];
    static String[] quarterClock = ["0h00_0h15", "0h15_0h30", "0h30_0h45", "0h45_1h00", "1h00_1h15", "1h15_1h30", "1h30_1h45", "1h45_2h00", "2h00_2h15", "2h15_2h30", "2h30_2h45", "2h45_3h00", "3h00_3h15", "3h15_3h30", "3h30_3h45", "3h45_4h00", "4h00_4h15", "4h15_4h30", "4h30_4h45", "4h45_5h00", "5h00_5h15", "5h15_5h30", "5h30_5h45", "5h45_6h00", "6h00_6h15", "6h15_6h30", "6h30_6h45", "6h45_7h00", "7h00_7h15", "7h15_7h30", "7h30_7h45", "7h45_8h00", "8h00_8h15", "8h15_8h30", "8h30_8h45", "8h45_9h00", "9h00_9h15", "9h15_9h30", "9h30_9h45", "9h45_10h00", "10h00_10h15", "10h15_10h30", "10h30_10h45", "10h45_11h00", "11h00_11h15", "11h15_11h30", "11h30_11h45", "11h45_12h00", "12h00_12h15", "12h15_12h30", "12h30_12h45", "12h45_13h00", "13h00_13h15", "13h15_13h30", "13h30_13h45", "13h45_14h00", "14h00_14h15", "14h15_14h30", "14h30_14h45", "14h45_15h00", "15h00_15h15", "15h15_15h30", "15h30_15h45", "15h45_16h00", "16h00_16h15", "16h15_16h30", "16h30_16h45", "16h45_17h00", "17h00_17h15", "17h15_17h30", "17h30_17h45", "17h45_18h00", "18h00_18h15", "18h15_18h30", "18h30_18h45", "18h45_19h00", "19h00_19h15", "19h15_19h30", "19h30_19h45", "19h45_20h00", "20h00_20h15", "20h15_20h30", "20h30_20h45", "20h45_21h00", "21h00_21h15", "21h15_21h30", "21h30_21h45", "21h45_22h00", "22h00_22h15", "22h15_22h30", "22h30_22h45", "22h45_23h00", "23h00_23h15", "23h15_23h30", "23h30_23h45", "23h45_24h00"];

    public LinkStatStruct(String timeSlice, double populationFactor) {
        this.timeSlice = timeSlice;
        this.populationFactor = populationFactor;
    }

    public LinkStatStruct(String timeSlice, double populationFactor, boolean perVehicleLevel, SRID) {
        this(timeSlice, populationFactor);
        this.perVehicleLevel = perVehicleLevel;
        this.SRID = SRID;
    }

    public void vehicleEnterAt(Id<Vehicle> vehicleId, double time) {
        isUsed = true;
        if (!enterTimes.containsKey(vehicleId)) {
            enterTimes.put(vehicleId, time);
        }
    }
    public void vehicleLeaveAt(Id<Vehicle> vehicleId, double time) {
        vehicleLeaveAt(vehicleId, time, false);
    }
    public void vehicleLeaveAt(Id<Vehicle> vehicleId, double time, boolean isIgnored) {
        String timeString = getTimeString(time);
        if (!trips.containsKey(timeString)) {
            trips.put(timeString, new ArrayList<Trip>());
        }
        if (enterTimes.containsKey(vehicleId)) {
            double enterTime = enterTimes.get(vehicleId);
            double travelTime = time - enterTime;
            Trip.Type type = Trip.Type.LV;
            if (vehicleId.toString().endsWith("_bus")) {
                type = Trip.Type.MV;
            }
            Trip trip = new Trip(timeString, vehicleId, type, isIgnored, travelTime);
            trips.get(timeString).add(trip);
            enterTimes.remove(vehicleId);
        }
    }
    public int getVehicleCount(Trip.Type type, String timeString, boolean skipIgnored) {
        if (!trips.containsKey(timeString)) {
            return 0;
        }
        int count = 0;
        for (Trip trip in trips.get(timeString)) {
            if (trip.type == type) {
                if (skipIgnored && trip.ignored) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }
    public double getMeanTravelTime(Trip.Type type, String timeString, boolean skipIgnored) {
        int vehicleCount = getVehicleCount(type, timeString, skipIgnored);
        if (vehicleCount == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (Trip trip in trips.get(timeString)) {
            if (skipIgnored && trip.ignored) {
                continue;
            }
            if (trip.type == type) {
                sum += trip.travelTime;
            }
        }
        return (sum / vehicleCount);
    }
    public double getMaxTravelTime(Trip.Type type, String timeString, boolean skipIgnored) {
        int vehicleCount = getVehicleCount(type, timeString, skipIgnored);
        if (vehicleCount == 0) {
            return 0.0;
        }
        double max = 0.0;
        for (Trip trip in trips.get(timeString)) {
            if (skipIgnored && trip.ignored) {
                continue;
            }
            if (trip.type == type) {
                if (trip.travelTime > max) {
                    max = trip.travelTime;
                }
            }
        }
        return max;
    }
    public double getMinTravelTime(Trip.Type type, String timeString, boolean skipIgnored) {
        int vehicleCount = getVehicleCount(type, timeString, skipIgnored);
        if (vehicleCount == 0) {
            return 0.0;
        }
        double min = -1;
        for (Trip trip in trips.get(timeString)) {
            if (skipIgnored && trip.ignored) {
                continue;
            }
            if (trip.type == type) {
                if (min <= 0 || trip.travelTime < min) {
                    min = trip.travelTime;
                }
            }
        }
        return min;
    }
    private String getTimeString(double time) {
        time = time % 86400
        if (timeSlice == "DEN") {
            String timeString = "D";
            if (time >= 6 * 3600 && time < 18 * 3600) {
                timeString = "D";
            }
            if (time >= 18 * 3600 && time < 22 * 3600) {
                timeString = "E";
            }
            if (time >= 22 * 3600 || time < 6 * 3600) {
                timeString = "N";
            }
            return timeString;
        }
        else if (timeSlice == "quarter") {
            int hour = (int) (time / 3600);
            int min = (int) (time - (hour * 3600)) / 60;
            if (min >= 0 && min < 15) {
                return hour + "h00" + "_" + hour + "h15";
            }
            if (min >= 15 && min < 30) {
                return hour + "h15" + "_" + hour + "h30";
            }
            if (min >= 30 && min < 45) {
                return hour + "h30" + "_" + hour + "h45";
            }
            if (min >= 45 && min < 60) {
                return hour + "h45" + "_" + (hour + 1) + "h00";
            }
        }
        else {
            int start = (int) (time / 3600);
            return start + "_" + (start + 1);
        }
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public double[] getSourceLevels(String timeString, boolean perVehicleLevel, boolean skipIgnored) {
        if (!trips.containsKey(timeString)) {
            return calculateSourceLevels(0, 0, 0, 0, 0, 0);
        }
        if (!perVehicleLevel) {
            double LVCount = getVehicleCount(Trip.Type.LV, timeString, skipIgnored);
            double MVCount = getVehicleCount(Trip.Type.MV, timeString, skipIgnored);
            double HVCount = getVehicleCount(Trip.Type.HV, timeString, skipIgnored);
            if (timeSlice == "quarter") {
                LVCount *= 4;
                MVCount *= 4;
                HVCount *= 4;
            }
            LVCount /= populationFactor;
            MVCount /= populationFactor;
            HVCount /= populationFactor;
            double VLAvgSpeed = 0;
            double MVAvgSpeed = 0;
            double HVAvgSpeed = 0;
            if (LVCount > 0) {
                VLAvgSpeed = Math.round(3.6 * link.getLength() / getMeanTravelTime(Trip.Type.LV, timeString, skipIgnored));
            }
            if (HVCount > 0) {
                MVAvgSpeed = Math.round(3.6 * link.getLength() / getMeanTravelTime(Trip.Type.MV, timeString, skipIgnored));
            }
            if (HVCount > 0) {
                HVAvgSpeed = Math.round(3.6 * link.getLength() / getMeanTravelTime(Trip.Type.HV, timeString, skipIgnored));
            }
            return calculateSourceLevels(LVCount, VLAvgSpeed, MVCount, MVAvgSpeed, HVCount, HVAvgSpeed);
        }
        else {
            double vehicleCount = 1;
            if (timeSlice == "quarter") {
                vehicleCount *= 4;
            }
            vehicleCount /= populationFactor;
            double[] result = [-99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0];

            for (Trip trip in trips.get(timeString)) {
                if (skipIgnored && trip.ignored) {
                    continue;
                }
                double speed = Math.round(3.6 * link.getLength() / trip.travelTime);
                double[] levels = [-99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0, -99.0];
                if (trip.type == Trip.Type.LV) {
                    levels = calculateSourceLevels(vehicleCount, speed, 0, 0, 0, 0);
                }
                else if (trip.type == Trip.Type.MV) {
                    levels = calculateSourceLevels(0, 0, 0, 0, vehicleCount, speed);
                }
                else if (trip.type == Trip.Type.HV) {
                    levels = calculateSourceLevels(0, 0, vehicleCount, speed, 0, 0);
                }
                for (int freq = 0; freq < result.size(); freq++) {
                    result[freq] = 10 * Math.log10(Math.pow(10, result[freq] / 10) + Math.pow(10, levels[freq] / 10));
                }
            }
            return result;
        }
    }

    public static double[] calculateSourceLevels(double LVCount, double LVAvgSpeed, double MVCount, double MVAvgSpeed, double HVCount, double HVAvgSpeed) {
        int[] freqs = [63, 125, 250, 500, 1000, 2000, 4000, 8000];
        double[] result = new double[freqs.length];
        if (LVCount == 0 && MVCount == 0 && HVCount == 0) {
            for (int i = 0; i < freqs.length; i++) {
                result[i] = -99.0;
            }
            return result;
        }
        for (int i = 0; i < freqs.length; i++) {
            RoadSourceParametersCnossos rsParametersCnossos = new RoadSourceParametersCnossos(
                    LVAvgSpeed,MVAvgSpeed,HVAvgSpeed,0.0,0.0,
                    LVCount,MVCount,HVCount,0.0,0.0,
                    freqs[i],20.0,"NL08",0.0,0.0,
                    100,2);

            result[i] = EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos);
        }
        return result;
    }
    public Coordinate[] getGeometry() {
        if (link.getAttributes().getAsMap().containsKey("geometry")) {
            Coord[] coords = ((Coord[]) link.getAttributes().getAttribute("geometry"));
            Coordinate[] result = new Coordinate[coords.length];
            for (int i = 0; i < coords.length; i++) {
                result[i] = new Coordinate(coords[i].getX(), coords[i].getY(), 0.5);
            }
            return result;
        } else {
            Coordinate[] result = new Coordinate[2];
            result[0] = new Coordinate(
                    link.getFromNode().getCoord().getX(),
                    link.getFromNode().getCoord().getY(),
                    0.5
            );
            result[1] = new Coordinate(
                    link.getToNode().getCoord().getX(),
                    link.getToNode().getCoord().getY(),
                    0.5
            );
            return result;
        }
    }
    public String getGeometryString() {
        if (link.getAttributes().getAsMap().containsKey("geometry")) {
            Coord[] coords = ((Coord[]) link.getAttributes().getAttribute("geometry"));
            String result = "LINESTRING (";
            for (int i = 0; i < coords.length; i++) {
                if (i > 0) {
                    result += ", ";
                }
                result += coords[i].getX() + " " + coords[i].getY();
            }
            result += ")";
            return result;
        } else {
            String result = "LINESTRING (";
            result += link.getFromNode().getCoord().getX() + " " + link.getFromNode().getCoord().getY();
            result += ", ";
            result += link.getToNode().getCoord().getX() + " " + link.getToNode().getCoord().getY();
            result += ")";
            return result;
        }
    }
    public String getOsmId() {
        if (link.getAttributes().getAsMap().containsKey("origid")) {
            return link.getAttributes().getAttribute("origid").toString();
        } else if (link.getId().toString().contains("_")) {
            return link.getId().toString().split("_")[0];
        } else {
            return String.valueOf(Long.parseLong(link.getId().toString()));
        }
    }
    public String toSqlInsertWithGeom(String tableName, String statsTableName, String geom, boolean alt) {

        if (geom == '' || geom == null || geom.matches("LINESTRING\\(\\d+\\.\\d+ \\d+\\.\\d+\\)")) {
            geom = getGeometryString();
        }
        String insert_start = "INSERT INTO " + tableName + " (LINK_ID, OSM_ID, THE_GEOM) VALUES ( ";
        String insert_end = " );  ";
        String sql = "";

        sql += insert_start
        sql += "'" + link.getId().toString() + "', ";
        sql += "'" + getOsmId() + "', ";
        sql += "ST_GeomFromText('" + geom + "', " + SRID + ")";
        sql += insert_end

        insert_start = "INSERT INTO " + statsTableName + ''' (LINK_ID,
            LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000,
            TIMESTRING) VALUES (''';

        String[] timeStrings;
        if (timeSlice == "den") {
            timeStrings = den;
        }
        else if (timeSlice == "hour") {
            timeStrings = hourClock;
        }
        else if (timeSlice == "quarter") {
            timeStrings = quarterClock;
        }
        for (String timeString : timeStrings) {
            sql += insert_start
            sql += "'" + link.getId().toString() + "', ";
            double[] levels;
            levels = getSourceLevels(timeString, perVehicleLevel, alt);
            for (int i = 0; i < levels.length; i++) {
                sql += String.format(Locale.ROOT,"%.2f", levels[i]);
                sql += ", ";
            }
            // sql += Boolean.toString(isAlternativeDifferent(timeString)) + ", "
            sql += "'" + timeString + "'";
            sql += insert_end
        }
        return sql;
    }

    public String toSqlInsertTraffic(String statsTableName, boolean alt) {

        String insert_start = "INSERT INTO " + statsTableName + ''' (LINK_ID,
            LV_D, LV_SPD_D, MV_D, MV_SPD_D, HGV_D, HGV_SPD_D,
            TIMESTRING) VALUES (''';
        String insert_end = " );  ";
        String sql = "";

        String[] timeStrings;
        if (timeSlice == "den") {
            timeStrings = den;
        }
        else if (timeSlice == "hour") {
            timeStrings = hourClock;
        }
        else if (timeSlice == "quarter") {
            timeStrings = quarterClock;
        }
        for (String timeString : timeStrings) {
            sql += insert_start
            sql += "'" + link.getId().toString() + "', ";
            Trip.Type[] types = [Trip.Type.LV, Trip.Type.MV, Trip.Type.HV]
            for (Trip.Type type in types) {
                int count = getVehicleCount(type, timeString, alt);
                double speed = 0.0;
                if (count > 0) {
                    speed = Math.round(3.6 * link.getLength() / getMeanTravelTime(type, timeString, alt));
                }
                sql += Integer.toString(count)
                sql += ", ";
                sql += Double.toString(speed);
                sql += ", ";
            }
            sql += "'" + timeString + "'";
            sql += insert_end
        }
        return sql;
    }
    
}
