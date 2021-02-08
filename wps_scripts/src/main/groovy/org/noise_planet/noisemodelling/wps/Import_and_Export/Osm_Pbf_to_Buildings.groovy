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

package org.noise_planet.noisemodelling.wps.Import_and_Export;

import geoserver.GeoServer;
import geoserver.catalog.Store;

import groovy.sql.Sql;

import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Coordinate;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
 
import crosby.binary.osmosis.OsmosisReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import java.sql.Connection

title = 'Import buidlings from an OSM PBF file'
description = 'Import Buidlings from an OSM PBF file.'

inputs = [
    pathFile: [
        name: 'Path of the OSM file',
        title: 'Path of the OSM file',
        description: 'Path of the OSM file including extension. </br> For example : c:/home/area.osm.pbf',
        type: String.class
    ],
    targetSRID: [
        name: 'Target projection identifier',
        title: 'Target projection identifier',
        description: 'Target projection identifier (also called SRID) of your table. ' +
                'It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection).' +
                '</br>The target SRID must be in metric coordinates. </br>',
        type: Integer.class
    ],
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
    String dbName = "postgis"

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
    logger.info('Start : Get Buildings from PBF OSM')
    logger.info("inputs {}", input)

    // -------------------
    // Get every inputs
    // -------------------

    String pathFile = input["pathFile"] as String

    Integer srid = 3857
    if ('targetSRID' in input) {
        srid = input['targetSRID'] as Integer
    }

    InputStream inputStream = new FileInputStream(pathFile);
    OsmosisReader reader = new OsmosisReader(inputStream);
    OsmHandler handler = new OsmHandler()
    reader.setSink(handler);
    reader.run();

    logger.info('PBF Read done')

    String tableName = "MAP_BUILDINGS_GEOM";

    sql.execute("DROP TABLE IF EXISTS " + tableName)
    sql.execute("CREATE TABLE " + tableName + '''( 
        ID_WAY integer PRIMARY KEY, 
        THE_GEOM geometry,
        HEIGHT real
    );''')

    for (Building building: handler.buildings) {
        sql.execute("INSERT INTO " + tableName + " VALUES (" + building.id + ", ST_MakeValid(ST_SIMPLIFYPRESERVETOPOLOGY(ST_Transform(ST_GeomFromText('" + building.geom + "', 4326), "+srid+"),0.1)), " + building.height + ")")
    }

    logger.info('SQL INSERT done')

    sql.execute('''
        CREATE SPATIAL INDEX IF NOT EXISTS BUILDINGS_INDEX ON ''' + tableName + '''(the_geom);
        -- list buildings that intersects with other buildings that have a greater area
        drop table if exists tmp_relation_buildings_buildings;
        create table tmp_relation_buildings_buildings as select s1.ID_WAY as PK_BUILDING, S2.ID_WAY as PK2_BUILDING FROM MAP_BUILDINGS_GEOM S1, MAP_BUILDINGS_GEOM S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.1;
        
        -- Alter that small area buildings by removing shared area
        drop table if exists tmp_buildings_truncated;
        create table tmp_buildings_truncated as select PK_BUILDING, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_Collect(s2.the_geom), 0.1, 'join=mitre')) the_geom, s1.HEIGHT HEIGHT from tmp_relation_buildings_buildings r, MAP_BUILDINGS_GEOM s1, MAP_BUILDINGS_GEOM s2 WHERE PK_BUILDING = S1.ID_WAY AND PK2_BUILDING = S2.ID_WAY  GROUP BY PK_BUILDING;
        
        -- merge original buildings with altered buildings 
        DROP TABLE IF EXISTS BUILDINGS;
        create table BUILDINGS(PK INTEGER PRIMARY KEY, THE_GEOM GEOMETRY, HEIGHT real)  as select s.id_way, ST_SETSRID(s.the_geom, '''+srid+'''), s.HEIGHT from  MAP_BUILDINGS_GEOM s where id_way not in (select PK_BUILDING from tmp_buildings_truncated) UNION ALL select PK_BUILDING, ST_SETSRID(the_geom, '''+srid+'''), HEIGHT from tmp_buildings_truncated WHERE NOT st_isempty(the_geom);

        drop table if exists tmp_buildings_truncated;
        drop table if exists tmp_relation_buildings_buildings;
        drop table if exists MAP_BUILDINGS_GEOM;
    ''');

    resultString = "nodes : " + handler.nb_nodes
    resultString += "<br>\n"
    resultString += "ways : " + handler.nb_ways
    resultString += "<br>\n"
    resultString += "relations : " + handler.nb_relations
    resultString += "<br>\n"
    resultString += "buildings : " + handler.nb_buildings
    resultString += "<br>\n"

    logger.info('End : Get Buildings from PBF OSM')
    logger.info('Result : ' + resultString)
    return resultString
}

public class OsmHandler implements Sink {
 
    public int nb_ways = 0;
    public int nb_nodes = 0;
    public int nb_relations = 0;
    public int nb_buildings = 0;

    Random rand = new Random();
    
    public Map<Long, Node> nodes = new HashMap<Long, Node>();
    public Map<Long, Way> ways = new HashMap<Long, Way>();
    public Map<Long, Relation> relations = new HashMap<Long, Relation>();
    public List<Building> buildings = new ArrayList<Building>();

    @Override
    public void initialize(Map<String, Object> arg0) {
    }
 
    @Override
    public void process(EntityContainer entityContainer) {
        if (entityContainer instanceof NodeContainer) {
            nb_nodes++;
            Node node = ((NodeContainer) entityContainer).getEntity();
            nodes.put(node.getId(), node);
        } else if (entityContainer instanceof WayContainer) {
            nb_ways++;
            Way way = ((WayContainer) entityContainer).getEntity();
            ways.put(way.getId(), way);
            boolean isBuilding = false;
            double height = 4.0 + rand.nextDouble() * 2.1;
            boolean trueHeightFound = false;
            for (Tag tag : way.getTags()) {
                if ("building".equalsIgnoreCase(tag.getKey())) {
                    isBuilding = true;
                }
                if (!trueHeightFound && "building:levels".equalsIgnoreCase(tag.getKey())) {
                    height = height - 4 + Double.parseDouble(tag.getValue().replaceAll("[^0-9]+", "")) * 3.0;
                }
                if ("height".equalsIgnoreCase(tag.getKey())) {
                    height = Double.parseDouble(tag.getValue().replaceAll("[^0-9]+", ""));
                    trueHeightFound = true;
                }
            }
            if (isBuilding && way.isClosed()) {
                nb_buildings++;
                buildings.add(new Building(way, height));
            }
        } else if (entityContainer instanceof RelationContainer) {
            nb_relations++;
            Relation rel = ((RelationContainer) entityContainer).getEntity();
            relations.put(rel.getId(), rel);
        } else {
            System.out.println("Unknown Entity!");
        }
    }
 
    @Override
    public void complete() {
        for(Building building: buildings) {
            building.setGeom(calculateGeometry(building.way));
        }
    }
 
    @Override
    public void close() {
    }

    public Geometry calculateGeometry(Way way) {
        double EARTH_RADIUS = 6378137.0;
        double PI = 3.14159265359;
        GeometryFactory geomFactory = new GeometryFactory();
        if (way == null) {
            return geomFactory.createPolygon();
        }
        List<WayNode> wayNodes = way.getWayNodes();
        if (wayNodes.size() < 4) {
            return geomFactory.createPolygon();
        }
        Coordinate[] shell = new Coordinate[wayNodes.size()];
        for(int i = 0; i < wayNodes.size(); i++) {
            Node node = nodes.get(wayNodes.get(i).getNodeId());
            double x = node.getLongitude();
            double y = node.getLatitude();
            shell[i] = new Coordinate(x, y, 0.0);
        }
        return geomFactory.createPolygon(shell);
    }
}


public class Building {

    int id;
    Way way;
    Geometry geom;
    double height = 0.0;

    Building(Way way) {
        this.way = way;
        this.id = way.getId();
        double h = 4.0 + rand.nextDouble() * 2.1;
        boolean trueHeightFound = false;
        for (Tag tag : way.getTags()) {
            if (!trueHeightFound && "building:levels".equalsIgnoreCase(tag.getKey())) {
                h = h - 4 + Double.parseDouble(tag.getValue().replaceAll("[^0-9]+", "")) * 3.0;
            }
            if ("height".equalsIgnoreCase(tag.getKey())) {
                h = Double.parseDouble(tag.getValue().replaceAll("[^0-9]+", ""));
                trueHeightFound = true;
            }
        }
        this.height = h;
    }
    Building(Way way, double height) {
        this.way = way;
        this.id = way.getId();
        this.height = height;
    }

    void setGeom(Geometry geom) {
        this.geom = geom;
    }

    void setHeight(double height) {
        this.height = height;
    }


}