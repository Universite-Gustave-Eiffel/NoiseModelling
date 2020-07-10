
package org.noise_planet.noisemodelling.wps.Matsim

import geoserver.GeoServer
import geoserver.catalog.Store

import org.geotools.jdbc.JDBCDataStore

import org.locationtech.jts.geom.Coordinate
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos;
import org.noise_planet.noisemodelling.emission.RSParametersCnossos;

import java.sql.Connection

import groovy.sql.Sql
import groovy.sql.GroovyRowResult

title = 'Import data from Mastim output'

description = 'Read Mastim events output file in order to get traffic NoiseModelling input'

inputs = [
    folder: [
        name: 'Path of the Matsim output folder',
        title: 'Path of the Matsim output folder',
        description: 'Path of the Matsim output folder </br> For example : c:/home/mastim/output',
        type: String.class
    ],
    link2GeometryTable: [
        name: 'table name of the pt2matsim file generated when importing OSM network',
        title: 'Table name of the geometry file',
        description: 'Table name of the geometry file',
        min: 0,
        max: 1,
        type: String.class
    ],
    link2GeometryFile: [
        name: 'File of the pt2matsim file generated when importing OSM network',
        title: 'File of the geometry file',
        description: 'File of the geometry file',
        min: 0,
        max: 1,
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
    
    String outTableName = "MATSIM_ROADS";
    if (input['outTableName']) {
        outTableName = input['outTableName'];
    }

    String statsTableName = outTableName + "_STATS"

    String link2GeometryTable = "LINK2GEOM";
    if (input['link2GeometryTable']) {
        link2GeometryTable = input['link2GeometryTable'];
    }

    String link2GeometryFile = "";
    if (input["link2GeometryFile"]) {
        link2GeometryFile = input["link2GeometryFile"];
    }

    String eventFile = folder + "\\output_events.xml.gz";
    String networkFile = folder + "\\nantes_network.xml.gz";
    String configFile = folder + "\\nantes_config.xml";

    Network network = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
    networkReader.readFile(networkFile);

    Map<Id<Link>, Link> links = (Map<Id<Link>, Link>) network.getLinks();

    EventsManager evMgr = EventsUtils.createEventsManager();
    ProcessOutputEventHandler evHandler = new ProcessOutputEventHandler();

    evHandler.initLinks((Map<Id<Link>, Link>) links);

    evMgr.addHandler(evHandler);

    MatsimEventsReader eventsReader = new MatsimEventsReader(evMgr);

    eventsReader.readFile(eventFile);
    
    // Open connection
    Sql sql = new Sql(connection)
    sql.execute("DROP TABLE IF EXISTS " + outTableName)
    sql.execute("CREATE TABLE " + outTableName + '''( 
        id integer PRIMARY KEY AUTO_INCREMENT, 
        LINK_ID varchar(255),
        OSM_ID varchar(255),
        THE_GEOM geometry
    );''')
    sql.execute("CREATE INDEX " + outTableName + "_LINK_ID_IDX ON " + outTableName + " (LINK_ID);")

    sql.execute("DROP TABLE IF EXISTS " + statsTableName)
    sql.execute("CREATE TABLE " + statsTableName + '''( 
        id integer PRIMARY KEY AUTO_INCREMENT, 
        LINK_ID varchar(255),
        TV  integer,
        TV_SPD double,
        TIMESTRING varchar(10)
    );''')
    sql.execute("CREATE INDEX " + statsTableName + "_LINK_ID_IDX ON " + statsTableName + " (LINK_ID);")

    Map<String, String> link2geomData = new HashMap<>();
    if (!link2GeometryFile.isEmpty()) {
        BufferedReader br = new BufferedReader(new FileReader(folder + "\\" + link2GeometryFile));
        String line =  null;
        while ((line = br.readLine()) != null) {
            String[] str = line.split(",", 2);
            if (str.size() > 1) {
                link2geomData.put(str[0], str[1].trim().replace("\"", ""));
            }
        }
    }

    try {
        FileWriter outFile = new FileWriter(folder + "\\analysis.csv");
        outFile.write(LinkStatStruct.getTableStringHeader(false) + "\n");
        for (Map.Entry<Id<Link>, LinkStatStruct> entry : evHandler.links.entrySet()) {
            String linkId = entry.getKey().toString();
            String geomString = "";
            if (!link2GeometryTable.isEmpty() && link2GeometryFile.isEmpty()) {
                GroovyRowResult result = sql.firstRow("SELECT GEOMETRY FROM " + link2GeometryTable + " WHERE LINKID='" + linkId + "'")
                if (result) {
                    geomString = result[0];
                }
            }
            if (!link2GeometryFile.isEmpty()) {
                geomString = link2geomData.get(linkId);
            }

            LinkStatStruct linkStatStruct = entry.getValue();
            outFile.write(linkStatStruct.toTableString() + "\n");
            sql.execute(linkStatStruct.toSqlInsertWithGeom(outTableName, geomString));
        }
        outFile.close();
    } catch (IOException e) {
        e.printStackTrace();
    }

}

public class ProcessOutputEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

    Map<Id<Link>, LinkStatStruct> links = new HashMap<Id<Link>, LinkStatStruct>();

    @Override
    public void handleEvent(LinkEnterEvent event) {
        // System.out.println("Link Entered ! " + event.toString());

        Id<Link> linkId = event.getLinkId();
        Id<Vehicle> vehicleId = event.getVehicleId();
        double time = event.getTime();

        if (!links.containsKey(linkId)) {
            LinkStatStruct stats = new LinkStatStruct();
            links.put(linkId, stats);
        }

        LinkStatStruct stats = links.get(linkId);

        stats.vehicleEnterAt(vehicleId, time);

        links.put(linkId, stats);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        // System.out.println("Link Leaved ! " + event.toString());

        Id<Link> linkId = event.getLinkId();
        Id<Vehicle> vehicleId = event.getVehicleId();

        double time = event.getTime();

        if (!links.containsKey(linkId)) {
            LinkStatStruct stats = new LinkStatStruct();
            links.put(linkId, stats);
        }

        LinkStatStruct stats = links.get(linkId);
        stats.vehicleLeaveAt(vehicleId, time);
        links.put(linkId, stats);
    }

    public void initLinks(Map<Id<Link>, Link> netLinks) {
        for (Map.Entry<Id<Link>, Link> entry: netLinks.entrySet()) {
            Id<Link> linkId = entry.getKey();
            Link link = entry.getValue();

            if (!links.containsKey(linkId)) {
                LinkStatStruct stats = new LinkStatStruct();
                stats.setLink(link);
                links.put(linkId, stats);
            }
        }
    }
}

public class LinkStatStruct {

    private Map<String, Integer> vehicleCounter = new HashMap<String, Integer>();
    private Map<String, ArrayList<Double> > travelTimes = new HashMap<String, ArrayList<Double> >();
    private Map<Id<Vehicle>, Double> enterTimes = new HashMap<Id<Vehicle>, Double>();
    private Map<String, ArrayList<Double> > acousticLevels = new HashMap<String, ArrayList<Double> >();
    private Link link;
    private boolean DEN = false;

    public void vehicleEnterAt(Id<Vehicle> vehicleId, double time) {
        String timeString = getTimeString(time);
        if (!enterTimes.containsKey(vehicleId)) {
            enterTimes.put(vehicleId, time);
        }
    }
    public void vehicleLeaveAt(Id<Vehicle> vehicleId, double time) {
        String timeString = getTimeString(time);
        if (!travelTimes.containsKey(timeString)) {
            travelTimes.put(timeString, new ArrayList<Double>());
        }
        if (enterTimes.containsKey(vehicleId)) {
            double enterTime = enterTimes.get(vehicleId);
            travelTimes.get(timeString).add(time - enterTime);
            enterTimes.remove(vehicleId);
            incrementVehicleCount(timeString);
        }
    }
    public void incrementVehicleCount(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            vehicleCounter.put(timeString, 1);
            return;
        }
        vehicleCounter.put(timeString, vehicleCounter.get(timeString) + 1);
    }
    public int getVehicleCount(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0;
        }
        return vehicleCounter.get(timeString);
    }
    public double getMeanTravelTime(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0.0;
        }
        if (vehicleCounter.get(timeString) == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < travelTimes.get(timeString).size(); i++) {
            sum += travelTimes.get(timeString).get(i);
        }
        return (sum / vehicleCounter.get(timeString));
    }

    public double getMaxTravelTime(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0.0;
        }
        double max = 0.0;
        for (int i = 0; i < travelTimes.get(timeString).size(); i++) {
            if (travelTimes.get(timeString).get(i) > max) {
                max = travelTimes.get(timeString).get(i);
            }
        }
        return max;
    }
    public double getMinTravelTime(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0.0;
        }
        double min = -1;
        for (int i = 0; i < travelTimes.get(timeString).size(); i++) {
            if (min <= 0 || travelTimes.get(timeString).get(i) < min) {
                min = travelTimes.get(timeString).get(i);
            }
        }
        return min;
    }
    private String getTimeString(double time) {
        if (DEN) {
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
        } else {
            int start = (int) (time / 3600);
            return start + "_" + (start + 1);
        }
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public double[] getSourceLevels(String timeString) {
        double vehicleCount = getVehicleCount(timeString);
        double averageSpeed = Math.round(3.6 * link.getLength() / getMeanTravelTime(timeString));

        int[] freqs = [63, 125, 250, 500, 1000, 2000, 4000, 8000];
        double[] result = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(
                    averageSpeed,0.0,0.0,0.0,0.0,
                    vehicleCount,0.0,0.0,0.0,0.0,
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
            return String.valueOf(Long.parseLong(link.getId().toString()) / 1000);
        }
    }
    public String toString() {
        String out = "";
        out += "Link Id : " + link.getId().toString() + " ----------- \n";
        out += "Osm Id : " + getOsmId() + "\n";
        out += "Geometry : " + getGeometryString() + "\n";
        String[] den = ["D", "E", "N"];
        String[] clock = new String[24];
        for (int i = 0; i < 24; i++) {
            clock[i] = (i + "_" + (i+1));
        }
        String[] timeStrings = DEN ? den : clock;
        for (String timeString : timeStrings) {
            out += ("\tTime : " + timeString + " ----------- \n");
            out += ("\t\tVehicle Counter : " + getVehicleCount(timeString) + "\n");
            if (getVehicleCount(timeString) != 0) {
                int[] freqs = [63, 125, 250, 500, 1000, 2000, 4000, 8000];
                out += ("\t\tLw : [");
                double[] levels = getSourceLevels(timeString);
                for (int i = 0; i < levels.length; i++) {
                    if (i > 0) {
                        out += ", ";
                    }
                    out += String.format(Locale.ROOT,"%.2f", levels[i]);
                }
                out += ("]\n");
                //outFile.write("\t\tTravel Times : " + linkStatStruct.travelTimes.toString() + "\n");
                //System.out.println("\t\tTravel Times : " + linkStatStruct.travelTimes.toString() + "");
                out += ("\t\tMean Travel Times : " + Math.round(getMeanTravelTime(timeString)) + " seconds\n");
                out += ("\t\tLength : " + link.getLength() + " meters\n");
                String minSpeed = Math.round(3.6 * link.getLength() / getMaxTravelTime(timeString));
                out += ("\t\tMin Speed : " + minSpeed + " km/h\n");
                String meanSpeed = Math.round(3.6 * link.getLength() / getMeanTravelTime(timeString));
                out += ("\t\tMean Speed : " + meanSpeed + " km/h\n");
                String maxSpeed = Math.round(3.6 * link.getLength() / getMinTravelTime(timeString));
                out += ("\t\tMax Speed : " + maxSpeed + " km/h\n");
                out += ("\t\tSpeed Limit : " + Math.round(3.6 * link.getFreespeed()) + " km/h\n");
            }
        }
        return out;
    }
    public static String getTableStringHeader(boolean DEN) {
        String out = "";
        out += "LINK_ID\t";
        out += "OSM_ID\t";
        out += "THE_GEOM\t";
        String[] den = ["D", "E", "N"];
        String[] clock = new String[24];
        for (int i = 0; i < 24; i++) {
            clock[i] = (i + "_" + (i+1));
        }
        String[] timeStrings = DEN ? den : clock;
        for (String timeString : timeStrings) {
            out += "LV_" + timeString + "\t";
        }
        for (String timeString : timeStrings) {
            out += "LV_SPD_" + timeString + "\t";
        }
        return out;
    }
    public String toTableString() {
        String out = "";
        out += link.getId().toString() + "\t";
        out += getOsmId() + "\t";
        out += getGeometryString() + "\t";
        String[] den = ["D", "E", "N"];
        String[] clock = new String[24];
        for (int i = 0; i < 24; i++) {
            clock[i] = (i + "_" + (i+1));
        }
        String[] timeStrings = DEN ? den : clock;
        for (String timeString : timeStrings) {
            out += (getVehicleCount(timeString) + "\t");
        }
        for (String timeString : timeStrings) {
            if (getVehicleCount(timeString) != 0) {
                out += (Math.round(3.6 * link.getLength() / getMeanTravelTime(timeString)) + "\t");
            } else {
                out += "0\t";
            }
        }
        return out;
    }

    public String toSqlInsertWithGeom(String tableName, String geom) {

        if (geom == '' || geom == null || geom.matches("LINESTRING\\(\\d+\\.\\d+ \\d+\\.\\d+\\)")) {
            geom = getGeometryString();
        }
        String insert_start = "INSERT INTO " + tableName + " (LINK_ID, OSM_ID, THE_GEOM) VALUES ( ";
        String insert_end = " );  ";
        String sql = "";

        sql += insert_start
        sql += "'" + link.getId().toString() + "', ";
        sql += "'" + getOsmId() + "', ";
        sql += "'" + geom + "'";
        sql += insert_end
        
        insert_start = "INSERT INTO " + tableName + "_STATS (LINK_ID, TV, TV_SPD, TIMESTRING) VALUES ( ";

        String[] den = ["D", "E", "N"];
        String[] clock = new String[24];
        for (int i = 0; i < 24; i++) {
            clock[i] = (i + "_" + (i+1));
        }
        String[] timeStrings = DEN ? den : clock;
        for (String timeString : timeStrings) {
            sql += insert_start
            sql += "'" + link.getId().toString() + "', ";
            sql += getVehicleCount(timeString) + ", ";
            if (getVehicleCount(timeString) != 0) {
                sql += (Math.round(3.6 * link.getLength() / getMeanTravelTime(timeString))) + ", ";
            } else {
                sql += "0, ";
            }
            sql += "'" + timeString + "'";
            sql += insert_end
        }
        return sql;
    }
    
}
