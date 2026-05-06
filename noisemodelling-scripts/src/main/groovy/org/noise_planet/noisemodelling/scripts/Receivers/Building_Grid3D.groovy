/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team FROM the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */


package org.noise_planet.noisemodelling.scripts.Receivers



import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBUtils
import org.locationtech.jts.geom.*
import org.locationtech.jts.io.WKTReader
import org.noise_planet.noisemodelling.pathfinder.PathFinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Buildings Grid'
description = '&#10145;&#65039; Generates 3D receivers around the buildings and at different levels.</br>' +
              '<hr>' +
              'Main parameters: </br><ul>' +
              '<li>"Height between levels": coupled with the building height, allows to determine the number of levels,</li>' +
              '<li>"Distance from wall": set the distance between the receivers and the building facades,</li>'+
              '<li>"Distance between receivers": set the number of receivers around the buildings.</li></ul></br>' +
              '&#x2705; The output table is called <b>RECEIVERS</b> </br></br>'+
              '<img src="wps_images/Building_Grid3D.png" alt="Building grid output" width="95%" align="center">'

inputs = [
        tableBuilding   : [
                name       : 'Buildings table name',
                title      : 'Buildings table name',
                description: 'Name of the Buildings table. <br></br>' +
                             'The table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON)</li>' +
                             '<li> <b>HEIGHT</b> : the height of the building (in meter) (FLOAT)</li>' +
                             '<li> <b>POP</b> : building population to add in the receiver attribute (FLOAT) (Optionnal)</li></ul>',
                type       : String.class
        ],
        fence           : [
                name       : 'Fence geometry',
                title      : 'Extent filter',
                description: 'Create receivers only in the provided polygon (fence)',
                min        : 0, max: 1,
                type       : Geometry.class
        ],
        fenceTableName  : [
                name       : 'Fence geometry from table',
                title      : 'Filter using table bounding box',
                description: 'Filter receivers, using the bounding box of the given table name:<br><ol>' +
                             '<li> Extract the bounding box of the specified table,</li>' +
                             '<li> then create only receivers on the table bounding box.</li></ol>' +
                             'The given table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : any geometry type. </li></ul>',
                min        : 0, max: 1,
                type       : String.class
        ],
        sourcesTableName: [
                name       : 'Sources table name',
                title      : 'Sources table name',
                description: 'Keep only receivers that are at least 1 meter from the provided source geometries.</br></br>' +
                             'The source geometries table must contain: <ul>' +
                             '<li> <b>THE_GEOM</b> : any geometry type. </li></ul>',
                min        : 0, max: 1,
                type       : String.class
        ],
        delta           : [
                name       : 'Receivers minimal distance',
                title      : 'Distance between receivers',
                description: 'Distance between receivers (in the Cartesian plane - in meters) (FLOAT) </br></br>'+
                             '&#128736; Default value: <b>10 </b>',
                type       : Double.class
        ],
        heightLevels          : [
                name       : 'Height between levels',
                title      : 'Height between levels',
                description: 'Height between each level of receivers (in meters) (FLOAT) </br> </br>' +
                             '&#128736; Default value: <b>2.5 </b> ',
                min        : 0, max: 1,
                type       : Double.class
        ],
        distance          : [
                name       : 'Distance',
                title      : 'Distance from wall',
                description: 'Distance of receivers from the wall (in meters) (FLOAT) </br></br>' +
                             '&#128736; Default value: <b>2 </b>',
                min        : 0, max: 1,
                type       : Double.class
        ]
]

outputs = [
        result: [
                name       : 'Created table',
                title      : 'Created table',
                description: 'Name of the table containing the results of the computation. Can be used as input for another process.',
                type       : String.class
        ]
]








def exec(Connection connection, Map input) {

    // output string, the information given back to the user
    String resultString = null

   // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : 3D Receivers grid around buildings')
    logger.info("inputs {}", input) // log inputs of the run


    String receivers_table_name = "RECEIVERS"

    double delta = input.getOrDefault("delta",10) as Double


    double h = input.getOrDefault("heightLevels",2.5d) as Double

    double distance = input.getOrDefault("distance",2.0d) as Double

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
        resultString = "To run this script, your input Buildings table must have column named HEIGHT."
        return resultString
    }

    //Statement sql = connection.createStatement()
    Sql sql = new Sql(connection)
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))

    // Reproject fence
    int targetSrid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name))
    if (targetSrid == 0 && input['sourcesTableName']) {
        targetSrid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
    }

    Geometry fenceGeom = null
    if (input['fence']) {
        if (targetSrid != 0) {
            // Transform fence to the same coordinate system than the buildings & sources
            WKTReader wktReader = new WKTReader()
            fence = wktReader.read(input['fence'] as String)
            fenceGeom = ST_Transform.ST_Transform(connection, ST_SetSRID.setSRID(fence, 4326), targetSrid)
        } else {
            throw new Exception("Unable to find buildings or sources SRID, ignore fence parameters")
        }
    } else if (input['fenceTableName']) {
        fenceGeom = GeometryTableUtilities.getEnvelope(connection, TableLocation.parse(input['fenceTableName'] as String), "THE_GEOM")
    }

    def buildingPk = JDBCUtilities.getColumnName(connection, building_table_name,
            JDBCUtilities.getIntegerPrimaryKey(connection,
                    TableLocation.parse(building_table_name, DBUtils.getDBType(connection))))
    logger.info('The input building table has a Primary Key named ' + buildingPk)
    if (buildingPk == "" || !buildingPk) {
        return "To run this script, your input Buildings table must have a Primary Key."
    }

    sql.execute("DROP TABLE IF EXISTS tmp_receivers_lines")
    def filter_geom_query = ""
    if (fenceGeom != null) {
        filter_geom_query = " WHERE the_geom && :fenceGeom AND ST_INTERSECTS(the_geom, :fenceGeom)";
    }
    // create line of receivers
    sql.execute("CREATE TABLE tmp_receivers_lines as SELECT " + buildingPk + " as pk_building, " +
                "ST_SimplifyPreserveTopology(ST_ToMultiLine(ST_Buffer(the_geom, :distance_wall, 'join=bevel')), 0.05) the_geom, HEIGHT " +
                "FROM " + building_table_name + filter_geom_query, [fenceGeom : fenceGeom, distance_wall: distance])
    sql.execute("CREATE SPATIAL INDEX ON tmp_receivers_lines(the_geom)")

    // union of truncated receivers and non tructated, split line to points
    sql.execute("DROP TABLE IF EXISTS TMP_SCREENS_MERGE")
    sql.execute("CREATE TABLE TMP_SCREENS_MERGE (the_geom geometry, hBuilding float, pk_building integer) as SELECT s.the_geom, s.height, s.pk_building FROM tmp_receivers_lines s WHERE not st_isempty(s.the_geom) ;")
    sql.execute("ALTER TABLE TMP_SCREENS_MERGE ADD COLUMN PK SERIAL PRIMARY KEY")

    // Collect all lines and convert into points using custom method
    sql.execute("DROP TABLE IF EXISTS TMP_SCREENS")
    sql.execute("CREATE TABLE TMP_SCREENS(pk integer, the_geom geometry, level int, pk_building int)")
    def qry = 'INSERT INTO TMP_SCREENS(pk, the_geom, level, pk_building) VALUES (?,?,?,?);'
    GeometryFactory factory = new GeometryFactory(new PrecisionModel(), targetSrid);
    sql.withBatch(100, qry) { ps ->
        sql.eachRow("SELECT pk, the_geom, hBuilding, pk_building FROM TMP_SCREENS_MERGE") { row ->
            List<Coordinate> pts = new ArrayList<Coordinate>()
            def geom = row[1] as Geometry
            def hBuilding = row[2] as Double
            int pk_building = row[3] as Integer
            if (geom instanceof LineString) {
                PathFinder.splitLineStringIntoPoints(geom as LineString, delta, pts)
            } else if (geom instanceof MultiLineString) {
                for (int idgeom = 0; idgeom < geom.numGeometries; idgeom++) {
                    PathFinder.splitLineStringIntoPoints(geom.getGeometryN(idgeom) as LineString, delta, pts)
                }
            }
            int nLevels = (int) Math.ceil((hBuilding - 1.5) / h)
            if (hBuilding > 1.5) {
                for (int i = 0; i < nLevels; i++) {
                    for (int idp = 0; idp < pts.size(); idp++) {
                        Coordinate pt = pts.get(idp);
                        if (!Double.isNaN(pt.x) && !Double.isNaN(pt.y)) {
                            // define coordinates of receivers
                            Coordinate newCoord = new Coordinate(pt.x, pt.y, 1.5 + i * h)
                            ps.addBatch(row[0] as Integer, factory.createPoint(newCoord), i, pk_building)
                        }
                    }
                }
            }
        }
    }
    sql.execute("DROP TABLE IF EXISTS TMP_SCREENS_MERGE")
    sql.execute("DROP TABLE IF EXISTS " + receivers_table_name)


    if (!hasPop) {
        // buildings have no population attribute
        logger.info('Create RECEIVERS table...')

        sql.execute("CREATE TABLE " + receivers_table_name + "(pk serial, the_geom geometry, level integer, pk_building integer);")
        sql.execute("INSERT INTO " + receivers_table_name + " (the_geom, level, pk_building) " +
                        "SELECT ST_SetSRID(the_geom," + targetSrid.toInteger() + "), level, pk_building FROM TMP_SCREENS;")
        sql.execute("CREATE SPATIAL INDEX ON " + receivers_table_name + "(the_geom);")

        if (input['sourcesTableName']) {
            // Delete receivers near sources
            logger.info('Delete receivers near sources...')
            sql.execute("CREATE SPATIAL INDEX ON " + sources_table_name + "(the_geom);")
            sql.execute("DELETE FROM " + receivers_table_name + " g WHERE exists  " +
                            "(SELECT 1 FROM " + sources_table_name + " r  " +
                                "WHERE st_expand(g.the_geom, 1, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

        if (fenceGeom != null) {
            // Delete receiver not in fence filter
            logger.info('Delete receivers that are not in the fence')
            sql.execute("DELETE FROM " + receivers_table_name + " g WHERE not ST_INTERSECTS(g.the_geom , :fenceGeom);", [fenceGeom : fenceGeom])
        }
    } else {
        // buildings have population attribute
        // set population attribute divided by number of receiver to each receiver

        logger.info('Create RECEIVERS table...')
        
        sql.execute("DROP TABLE IF EXISTS tmp_receivers")
        sql.execute("CREATE TABLE tmp_receivers(the_geom geometry, build_pk integer, level integer, pk_building integer)")
        sql.execute("ALTER TABLE tmp_receivers ADD COLUMN PK SERIAL PRIMARY KEY")// Ajout Gwen
        sql.execute("INSERT INTO tmp_receivers(the_geom, build_pk, level, pk_building) " +
                        "SELECT ST_SetSRID(the_geom," + targetSrid.toInteger() + "), pk, level, pk_building FROM TMP_SCREENS;")

        if (input['sourcesTableName']) {
            // Delete receivers near sources
            logger.info('Delete receivers near sources...')
            sql.execute("CREATE SPATIAL INDEX ON " + sources_table_name + "(the_geom);")
            sql.execute("DELETE FROM tmp_receivers g WHERE exists " +
                            "(SELECT 1 FROM " + sources_table_name + " r " +
                                "WHERE st_expand(g.the_geom, 1, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

        if (fenceGeom != null) {
            // Delete receiver not in fence filter
            logger.info('Delete receivers that are not in the fence')
            sql.execute("DELETE FROM tmp_receivers g WHERE not ST_INTERSECTS(g.the_geom , " +
                    "ST_SETSRID(ST_GeomFromText('" + fenceGeom + "'), "+targetSrid.toInteger()+"));")
        }

        sql.execute("CREATE INDEX ON tmp_receivers(build_pk)")
        sql.execute("CREATE TABLE " + receivers_table_name + "(pk serial, the_geom geometry, level integer, pop float, pk_building integer)")
        sql.execute("INSERT INTO " + receivers_table_name + " (the_geom, level, pop, pk_building) " +
                        "SELECT a.the_geom, a.level, b.pop/COUNT(DISTINCT aa.pk)::float, a.pk_building " +
                        "FROM tmp_receivers a, " + building_table_name + " b,tmp_receivers aa " +
                        "WHERE b." + buildingPk + " = a.pk_building AND a.build_pk = aa.build_pk " +
                        "GROUP BY a.the_geom, a.build_pk, a.level, b.pop")
        sql.execute("DROP TABLE IF EXISTS tmp_receivers")
    }

    logger.info("Delete receivers inside buildings")
    sql.execute("DELETE FROM " + receivers_table_name + " g WHERE exists " +
                    "(SELECT 1 FROM " + building_table_name + " b " +
                        "WHERE ST_Z(g.the_geom) < b.HEIGHT and g.the_geom && b.the_geom and ST_INTERSECTS(g.the_geom, b.the_geom) limit 1);")


    // cleaning
    sql.execute("drop table TMP_SCREENS if exists")
    sql.execute("drop table tmp_screen_truncated if exists")
    sql.execute("drop table tmp_relation_screen_building if exists")
    sql.execute("drop table tmp_receivers_lines if exists")
    sql.execute("DROP TABLE IF EXISTS tmp_buildings if exists;")
    // Process Done
    resultString = "Process done. The receivers table named " + receivers_table_name + " has been created!"

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : 3D Receivers grid around buildings')

    return [result: receivers_table_name]

}
