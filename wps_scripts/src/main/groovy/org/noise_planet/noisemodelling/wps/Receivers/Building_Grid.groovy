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
 * @Author Nicolas Fortin, Université Gustave Eiffel
 * @Contributor Ignacio Soto Molina, Ministry for Ecological Transition (MITECO), Spain - Delete Receivers Inside Buildings
 */


package org.noise_planet.noisemodelling.wps.Receivers

import org.noise_planet.noisemodelling.wps.Database_Manager.DatabaseHelper
import groovy.sql.Sql
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2.util.geometry.EWKTUtils
import org.h2.util.geometry.JTSUtils
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBUtils
import org.locationtech.jts.geom.*
import org.locationtech.jts.io.WKTReader
import org.noise_planet.noisemodelling.jdbc.DelaunayReceiversMaker
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Buildings Grid'
description = '&#10145;&#65039; Generates receivers, 2m around the building facades, at a given height. </br>' +
              '<hr>' +
              '&#x2705; The output table is called <b>RECEIVERS</b> and contain a field <b>build_pk</b> corresponding to the primary key of the buildings table</br></br>'+
              '<img src="/wps_images/building_grid_output.png" alt="Building grid output" width="95%" align="center">'

inputs = [
        tableBuilding : [
                name       : 'Buildings table name',
                title      : 'Buildings table name',
                description: 'Name of the Buildings table. <br></br>' +
                             'The table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON)</li>' +
                             '<li> <b>HEIGHT</b> : the height of the building (in meter) (FLOAT)</li>' +
                             '<li> <b>POP</b> : (optional field) building population to add in the receiver attribute (FLOAT)</li></ul>',
                type       : String.class
        ],
        fence : [
                name       : 'Fence geometry',
                title      : 'Extent filter',
                description: 'Create receivers only in the provided polygon (fence)',
                min        : 0,
                max        : 1,
                type       : Geometry.class
        ],
        fenceTableName : [
                name       : 'Fence geometry from table',
                title      : 'Filter using table bounding box',
                description: 'Filter receivers, using the bounding box of the given table name:<br><ol>' +
                             '<li> Extract the bounding box of the specified table,</li>' +
                             '<li> then create only receivers on the table bounding box.</li></ol>' +
                             'The given table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : any geometry type. </li></ul>',
                min        : 0, 
                max        : 1,
                type       : String.class
        ],
        sourcesTableName : [
                name       : 'Sources table name',
                title      : 'Sources table name',
                description: 'Keep only receivers that are at least 1 meter from the provided source geometries.</br></br>' +
                             'The source geometries table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : any geometry type. </li></ul>',
                min        : 0, 
                max        : 1,
                type       : String.class
        ],
        delta : [
                name       : 'Receivers minimal distance',
                title      : 'Distance between receivers',
                description: 'Distance between receivers (in the Cartesian plane - in meter) (FLOAT) </br></br>'+
                             '&#128736; Default value: <b>10 </b>',
                min        : 0, 
                max        : 1,
                type       : Double.class
        ],
        height : [
                name       : 'Height',
                title      : 'Height',
                description: 'Height of receivers (in meter) (FLOAT) </br></br>' +
                             '&#128736; Default value: <b>4</b>',
                min        : 0, 
                max        : 1,
                type       : Double.class
        ],
        distance          : [
                name       : 'Distance',
                title      : 'Distance from wall',
                description: 'Distance of receivers from the wall in meters (FLOAT) </br></br>' +
                             '&#128736; Default value: <b>2 </b>',
                min        : 0, max: 1,
                type       : Double.class
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



def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Receivers grid around buildings')
    logger.info("inputs {}", input) // log inputs of the run


    String receivers_table_name = "RECEIVERS"

    Double delta = 10
    if (input['delta']) {
        delta = input['delta'] as Double
    }

    Double h = 4.0d
    if (input['height']) {
        h = input['height'] as Double
    }

    Double distance = 2.0d
    if (input['distance']) {
        distance = input['distance'] as Double
    }

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()


    String building_table_name = input['tableBuilding']
    building_table_name = building_table_name.toUpperCase()

    Boolean hasPop = JDBCUtilities.hasField(connection, building_table_name, "POP")
    if (hasPop) logger.info("The building table has a column named POP.")
    if (!hasPop) logger.info("The building table has not a column named POP.")

    if (!JDBCUtilities.hasField(connection, building_table_name, "HEIGHT")) {
        resultString = "Buildings table must have HEIGHT field"
        return resultString
    }

    //Statement sql = connection.createStatement()
    Sql sql = new Sql(connection)
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))

    // Reproject fence
    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name))
    if (srid == 0 && input['sourcesTableName']) {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
    }

    Geometry fence = null
    if (input['fence']) {
        if(input['fence'] instanceof Geometry) {
            fence = input['fence']
        } else {
            fence = JTSUtils.ewkb2geometry(EWKTUtils.ewkt2ewkb(input['fence'] as String))
        }
    }

    // Fence handling (two options):
    //  1) Direct geometry passed as 'fence' (handled above)
    //  2) Bounding box extracted from another table via 'fenceTableName'
    // Implemented by IsotoCedex (adapted from Building_Grid.groovy)
    if (input['fenceTableName']) {
        fence = GeometryTableUtilities.getEnvelope(connection, TableLocation.parse(input['fenceTableName'] as String), 'THE_GEOM')
    }

    if (fence != null && srid != 0 && fence.getSRID() != srid) {
        if (fence.getSRID() == 0) {
            // If the provided srid is not known, it is considered being in the WGS84 projection system
            fence = ST_SetSRID.setSRID(fence, 4326)
        }
        // Transform fence to the same coordinate system than the buildings & sources
        fence = ST_Transform.ST_Transform(connection, fence, srid)
    }


    def buildingPk = JDBCUtilities.getColumnName(connection, building_table_name,
            JDBCUtilities.getIntegerPrimaryKey(connection,
                    TableLocation.parse(building_table_name, DBUtils.getDBType(connection))))
    if (!buildingPk || buildingPk == "") {
        throw new IllegalArgumentException(building_table_name + " table must have a primary key")
    }

    //---------------------------------------------------------------------
    logger.info('Create line of receivers')

    sql.execute("DROP TABLE IF EXISTS tmp_receivers_lines")

    if (fence != null) {
        sql.execute("CREATE TABLE tmp_receivers_lines(pk int not null primary key, the_geom geometry) as select " + buildingPk + " as pk, st_simplifypreservetopology(ST_ToMultiLine(ST_Buffer(the_geom, :distance_wall, 'join=bevel')), 0.05) the_geom from " + building_table_name + " WHERE the_geom && :fenceGeom AND ST_INTERSECTS(the_geom, :fenceGeom)", [fenceGeom : fence, distance_wall : distance])
    } else {
        sql.execute("CREATE TABLE tmp_receivers_lines(pk int not null primary key, the_geom geometry) as select " + buildingPk + " as pk, st_simplifypreservetopology(ST_ToMultiLine(ST_Buffer(the_geom, :distance_wall, 'join=bevel')), 0.05) the_geom from " + building_table_name, [distance_wall : distance])
    }
    sql.execute("CREATE SPATIAL INDEX ON tmp_receivers_lines(the_geom)")

    //---------------------------------------------------------------------
    logger.info('List buildings that will remove receivers (if height is superior than receiver height)')

    sql.execute("DROP TABLE IF EXISTS tmp_relation_screen_building;")
    sql.execute("CREATE SPATIAL INDEX ON tmp_receivers_lines(the_geom)")
    sql.execute("CREATE TABLE tmp_relation_screen_building as select b." + buildingPk + " as PK_building, s.pk as pk_screen from " + building_table_name + " b, tmp_receivers_lines s where b.the_geom && s.the_geom and s.pk != b." + buildingPk + " and ST_Intersects(b.the_geom, s.the_geom) and b.height > " + h)
    sql.execute("CREATE INDEX ON tmp_relation_screen_building(PK_building);")
    sql.execute("CREATE INDEX ON tmp_relation_screen_building(pk_screen);")

    //---------------------------------------------------------------------
    logger.info('Truncate receiver lines')

    // First, for each screen, the aggregate geometry of the associated buildings is calculated.
    sql.execute("DROP TABLE IF EXISTS tmp_screen_buildings_geom;")
    sql.execute("CREATE TABLE tmp_screen_buildings_geom (pk_screen integer not null, the_geom geometry) as select r.pk_screen, ST_ACCUM(b.the_geom) as the_geom FROM tmp_relation_screen_building r JOIN " + building_table_name + " b ON r.PK_building = b." + buildingPk + " GROUP BY r.pk_screen;")
    sql.execute("ALTER TABLE tmp_screen_buildings_geom add primary key(pk_screen)")
    sql.execute("CREATE INDEX ON tmp_screen_buildings_geom (pk_screen);")
    sql.execute("CREATE SPATIAL INDEX ON tmp_screen_buildings_geom (the_geom)")

    // We now apply the buffer and the difference with the geometry of the screens.
    sql.execute("DROP TABLE IF EXISTS tmp_screen_truncated;")
    sql.execute("CREATE TABLE tmp_screen_truncated (pk_screen integer not null, the_geom geometry) AS SELECT s.pk as pk_screen, ST_DIFFERENCE(s.the_geom, ST_BUFFER(b.the_geom, :distance_wall)) as the_geom FROM tmp_receivers_lines s JOIN tmp_screen_buildings_geom b ON s.pk = b.pk_screen;", [distance_wall : distance])

    logger.info('Add primary key on tmp_screen_truncated')
    sql.execute("ALTER TABLE tmp_screen_truncated add primary key(pk_screen)")

    //---------------------------------------------------------------------
    logger.info('Union of truncated and non truncated receivers')

    sql.execute("DROP TABLE IF EXISTS TMP_SCREENS_MERGE;")
    sql.execute("CREATE TABLE TMP_SCREENS_MERGE (pk integer not null, the_geom geometry) as select s.pk, s.the_geom the_geom from tmp_receivers_lines s where not st_isempty(s.the_geom) and pk not in (select pk_screen from tmp_screen_truncated) UNION ALL select pk_screen, the_geom from tmp_screen_truncated where not st_isempty(the_geom);")

    logger.info('Add primary key on TMP_SCREENS_MERGE')
    sql.execute("ALTER TABLE TMP_SCREENS_MERGE add primary key(pk)")

    //---------------------------------------------------------------------
    logger.info('Collect all lines and convert into points using custom method')

    sql.execute("DROP TABLE IF EXISTS TMP_SCREENS;")
    sql.execute("CREATE TABLE TMP_SCREENS(pk integer, the_geom geometry)")
    def qry = 'INSERT INTO TMP_SCREENS(pk , the_geom) VALUES (?,?);'
    GeometryFactory factory = new GeometryFactory(new PrecisionModel(), srid);
    logger.info('Split line to points')
    int nrows = sql.firstRow('SELECT COUNT(*) FROM TMP_SCREENS_MERGE')[0] as Integer
    RootProgressVisitor progressLogger = new RootProgressVisitor(nrows, true, 1)
    sql.withBatch(100, qry) { ps ->
        sql.eachRow("SELECT pk, the_geom from TMP_SCREENS_MERGE") { row ->
            List<Coordinate> pts = new ArrayList<Coordinate>()
            def geom = row[1] as Geometry
            if (geom instanceof LineString) {
                splitLineStringIntoPoints(geom as LineString, delta, pts)
            } else if (geom instanceof MultiLineString) {
                for (int idgeom = 0; idgeom < geom.numGeometries; idgeom++) {
                    splitLineStringIntoPoints(geom.getGeometryN(idgeom) as LineString, delta, pts)
                }
            }
            for (int idp = 0; idp < pts.size(); idp++) {
                Coordinate pt = pts.get(idp);
                if (!Double.isNaN(pt.x) && !Double.isNaN(pt.y)) {
                    // define coordinates of receivers
                    Coordinate newCoord = new Coordinate(pt.x, pt.y, h)
                    ps.addBatch(row[0] as Integer, factory.createPoint(newCoord))
                }
            }
            progressLogger.endStep()
        }
    }
    sql.execute("DROP TABLE IF EXISTS TMP_SCREENS_MERGE")
    sql.execute("DROP TABLE IF EXISTS " + receivers_table_name)

    if (!hasPop) {
        logger.info('Create RECEIVERS table...')


        sql.execute("CREATE TABLE " + receivers_table_name + "(pk integer not null " + DatabaseHelper.autoIncrement(connection) + ", the_geom geometry,build_pk integer)")
        sql.execute("INSERT INTO " + receivers_table_name + "(the_geom, build_pk) select ST_SetSRID(the_geom," + srid.toInteger() + ") , pk building_pk from TMP_SCREENS;")
        logger.info('Add primary key')
        sql.execute("ALTER TABLE "+receivers_table_name+" add primary key(pk)")

        if (input['sourcesTableName']) {
            // Delete receivers near sources
            logger.info('Delete receivers near sources...')
            sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from " + sources_table_name + " r where st_expand(g.the_geom, 1, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

        if (fence != null) {
            // Delete receiver not in fence filter
            sql.execute("delete from " + receivers_table_name + " g where not ST_INTERSECTS(g.the_geom , :fenceGeom);", [fenceGeom : fence])
        }
    } else {
        logger.info('Create RECEIVERS table...')
        // Building have population attribute
        // Set population attribute divided by number of receiver to each receiver
        sql.execute("DROP TABLE IF EXISTS tmp_receivers")
        sql.execute("CREATE TABLE tmp_receivers(pk integer not null " + DatabaseHelper.autoIncrement(connection) + ", the_geom geometry,build_pk integer not null)")

        sql.execute("INSERT INTO tmp_receivers(the_geom, build_pk) select ST_SetSRID(the_geom," + srid.toInteger() + "), pk building_pk from TMP_SCREENS;")
        logger.info('Add primary key')
        sql.execute("ALTER TABLE tmp_receivers add primary key(pk)")

        if (input['sourcesTableName']) {
            // Delete receivers near sources
            logger.info('Delete receivers near sources...')
            sql.execute("delete from tmp_receivers g where exists (select 1 from " + sources_table_name + " r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

        if (fence != null) {
            // delete receiver not in fence filter
            sql.execute("delete from tmp_receivers g where not ST_INTERSECTS(g.the_geom , :fenceGeom);", [fenceGeom : fence])
        }
        logger.info('Create index on build_pk')
        sql.execute("CREATE INDEX ON tmp_receivers(build_pk)")

        //---------------------------------------------------------------------
        logger.info('Distribute population over receivers')

        sql.execute("DROP TABLE IF EXISTS BUILDINGS_RECEIVERS_POP")
        sql.execute("CREATE TABLE BUILDINGS_RECEIVERS_POP(" + buildingPk + " integer primary key, pop float) AS SELECT b." + buildingPk + ", b.pop / COUNT(a.PK)::float FROM tmp_receivers a, " + building_table_name + " b where b." + buildingPk + " = a.build_pk GROUP BY b." + buildingPk)
        sql.execute("CREATE TABLE " + receivers_table_name + "(pk integer not null " + DatabaseHelper.autoIncrement(connection) + ", the_geom geometry,build_pk integer, pop float)");
        sql.execute("INSERT INTO "+receivers_table_name+"(the_geom, build_pk, pop) select a.the_geom, a.build_pk, b.pop from tmp_receivers a,  BUILDINGS_RECEIVERS_POP b where b." + buildingPk + " = a.build_pk;");

        logger.info('Add primary key on ' +receivers_table_name)
        sql.execute("ALTER TABLE "+receivers_table_name+" add primary key(pk)")

        sql.execute("DROP TABLE IF EXISTS tmp_receivers")
        sql.execute("DROP TABLE BUILDINGS_RECEIVERS_POP;")
    }

    //---------------------------------------------------------------------
    // Cleaning
    sql.execute("DROP TABLE TMP_SCREENS")
    sql.execute("DROP TABLE tmp_screen_truncated")
    sql.execute("DROP TABLE tmp_relation_screen_building")
    sql.execute("DROP TABLE tmp_receivers_lines")
    sql.execute("DROP TABLE TMP_SCREEN_BUILDINGS_GEOM;")

    //---------------------------------------------------------------------
    // Process Done
    resultString = "Process done. The table of receivers " + receivers_table_name + " has been created !"

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : Receivers grid around buildings')

    // print to WPS Builder
    return resultString

}


/**
 *
 * @param geom Geometry
 * @param segmentSizeConstraint Maximal distance between points
 * @param [out]pts computed points
 * @return Fixed distance between points
 */
double splitLineStringIntoPoints(LineString geom, double segmentSizeConstraint,
                                 List<Coordinate> pts) {
    // If the linear sound source length is inferior than half the distance between the nearest point of the sound
    // source and the receiver then it can be modelled as a single point source
    double geomLength = geom.getLength();
    if (geomLength < segmentSizeConstraint) {
        // Return mid point
        Coordinate[] points = geom.getCoordinates();
        double segmentLength = 0;
        final double targetSegmentSize = geomLength / 2.0;
        for (int i = 0; i < points.length - 1; i++) {
            Coordinate a = points[i];
            final Coordinate b = points[i + 1];
            double length = a.distance3D(b)
            if(Double.isNaN(length)) {
                length = a.distance(b)
            }
            if (length + segmentLength > targetSegmentSize) {
                double segmentLengthFraction = (targetSegmentSize - segmentLength) / length;
                Coordinate midPoint = new Coordinate(a.x + segmentLengthFraction * (b.x - a.x),
                        a.y + segmentLengthFraction * (b.y - a.y),
                        Double.isNaN(a.z) || Double.isNaN(b.z) ? Double.NaN : a.z + segmentLengthFraction * (b.z - a.z));
                pts.add(midPoint);
                break;
            }
            segmentLength += length;
        }
        return geom.getLength();
    } else {
        double targetSegmentSize = geomLength / Math.ceil(geomLength / segmentSizeConstraint as double);
        Coordinate[] points = geom.getCoordinates();
        double segmentLength = 0.0;

        // Mid point of segmented line source
        def midPoint = null;
        for (int i = 0; i < points.length - 1; i++) {
            Coordinate a = points[i];
            final Coordinate b = points[i + 1];
            double length = a.distance3D(b);
            if (Double.isNaN(length)) {
                length = a.distance(b);
            }
            while (length + segmentLength > targetSegmentSize) {
                //LineSegment segment = new LineSegment(a, b);
                double segmentLengthFraction = (targetSegmentSize - segmentLength) / length;
                Coordinate splitPoint = new Coordinate();
                splitPoint.x = a.x + segmentLengthFraction * (b.x - a.x);
                splitPoint.y = a.y + segmentLengthFraction * (b.y - a.y);
                splitPoint.z = a.z + segmentLengthFraction * (b.z - a.z);
                if (midPoint == null && length + segmentLength > targetSegmentSize / 2) {
                    segmentLengthFraction = (targetSegmentSize / 2.0 - segmentLength) / length;
                    midPoint = new Coordinate(a.x + segmentLengthFraction * (b.x - a.x),
                            a.y + segmentLengthFraction * (b.y - a.y),
                            a.z + segmentLengthFraction * (b.z - a.z));
                }
                pts.add(midPoint);
                a = splitPoint;
                length = a.distance3D(b);
                if (Double.isNaN(length)) {
                    length = a.distance(b);
                }
                segmentLength = 0;
                midPoint = null;
            }
            if (midPoint == null && length + segmentLength > targetSegmentSize / 2) {
                double segmentLengthFraction = (targetSegmentSize / 2.0 - segmentLength) / length;
                midPoint = new Coordinate(a.x + segmentLengthFraction * (b.x - a.x),
                        a.y + segmentLengthFraction * (b.y - a.y),
                        a.z + segmentLengthFraction * (b.z - a.z));
            }
            segmentLength += length;
        }
        if (midPoint != null) {
            pts.add(midPoint);
        }
        return targetSegmentSize;
    }
}
