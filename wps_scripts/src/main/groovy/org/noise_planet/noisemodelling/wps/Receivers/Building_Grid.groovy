/**
 * @Author Aumond Pierre, Université Gustave Eiffel
 * @Author Can Arnaud, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.spatial.convert.ST_Force3D
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.noise_planet.noisemodelling.propagation.ComputeRays

import java.sql.Connection

// Change code and use : createReceiversFromBuildings see down
title = 'Buildings Grid'
description = 'Calculates a regular grid of receivers around buildings.' +
        '</br> </br> <b> The output table is called : RECEIVERS </b> '

inputs = [
          tableBuilding     : [name       : 'Buildings table name', title: 'Buildings table name',
                             description: '<b>Name of the Buildings table.</b>  </br>  ' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                                     '- <b> HEIGHT </b> : the height of the building (FLOAT)' +
                                     '- <b> POP </b> : optional field, building population to add in the receiver attribute (FLOAT)',
                             type       : String.class],
          fence  : [name: 'Fence geometry', title: 'Extent filter', description: 'Create receivers only in the' +
                  ' provided polygon', min: 0, max: 1, type: Geometry.class],
          fenceTableName  : [name: 'Fence geometry from table', title: 'Filter using table bouding box',
                             description: 'Extract the bounding box of the specified table then create only receivers' +
                                     ' on the table bounding box' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : any geometry type. </br>', min: 0, max: 1, type: String.class],
          sourcesTableName  : [name: 'Sources table name', title: 'Keep only receivers at least at 1 meters of' +
                  ' provided sources geometries' +
                  '<br>  The table shall contain : </br>' +
                  '- <b> THE_GEOM </b> : any geometry type. </br>', min: 0, max: 1, type: String.class],
          delta    : [name: 'Receivers minimal distance', title: 'Distance between receivers',
                      description: 'Distance between receivers in the Cartesian plane in meters', type: Double.class],
          height    : [name: 'height', title: 'height', description: 'Height of receivers in meters ' +
                  '</br> </br> <b> Default value : 4 </b> ', min: 0, max: 1, type: Double.class]]

outputs = [tableNameCreated: [name: 'tableNameCreated', title: 'tableNameCreated', type: String.class]]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if(dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore)store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return exec(connection, input)
    }
}

def exec(connection, input) {

    String receivers_table_name = "RECEIVERS"

    String fence_table_name = "FENCE_2154"
    if (input['fenceTableName']) {
        fence_table_name = input['fenceTableName']
    }
    fence_table_name = fence_table_name.toUpperCase()

    Double delta = 10
    if (input['delta']) {
        delta = input['delta']
    }

    Double h = 4.0d
    if (input['height']) {
        h = input['height']
    }

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()



    String building_table_name = input['tableBuilding']
    building_table_name = building_table_name.toUpperCase()

    Boolean hasPop = JDBCUtilities.hasField(connection, building_table_name, "POP")

    //Statement sql = connection.createStatement()
    Sql sql = new Sql(connection)
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))
    String queryGrid = null

    // Reproject fence
    def fenceGeom = null
    if (input['fence']) {
        int targetSrid = SFSUtilities.getSRID(connection, TableLocation.parse(building_table_name))
        if (targetSrid == 0 && input['sourcesTableName']) {
            targetSrid = SFSUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
        }
        if (targetSrid != 0) {
            // Transform fence to the same coordinate system than the buildings & sources
            fenceGeom = ST_Transform.ST_Transform(connection, ST_SetSRID.setSRID(input['fence'] as Geometry, 4326), targetSrid)
        } else {
            System.err.println("Unable to find buildings or sources SRID, ignore fence parameters")
        }
    } else if (input['fenceTableName']) {
        fenceGeom = sql.firstRow("SELECT ST_ENVELOPE(ST_COLLECT(the_geom)) the_geom from " + input['fenceTableName'])[0] as Geometry
    }

    sql.execute("drop table if exists tmp_receivers_lines")
    def filter_geom_query = ""
    if(fenceGeom != null) {
        filter_geom_query = " WHERE the_geom && ST_GeomFromText('" + fenceGeom + "') AND ST_INTERSECTS(the_geom, ST_GeomFromText('" + fenceGeom + "'))";
    }
    // create line of receivers
    sql.execute("create table tmp_receivers_lines as select pk, st_simplifypreservetopology(ST_ToMultiLine(ST_Buffer(the_geom, 2, 'join=bevel')), 0.5) the_geom from "+building_table_name+filter_geom_query)
    sql.execute("drop table if exists tmp_relation_screen_building;")
    sql.execute("create spatial index on tmp_receivers_lines(the_geom)")
    // list buildings that will remove receivers (if height is superior than receiver height
    sql.execute("create table tmp_relation_screen_building as select b.pk as PK_building, s.pk as pk_screen from "+building_table_name+" b, tmp_receivers_lines s where b.the_geom && s.the_geom and s.pk != b.pk and ST_Intersects(b.the_geom, s.the_geom) and b.height > " + h)
    sql.execute("drop table if exists tmp_screen_truncated;")
    // truncate receiver lines
    sql.execute("create table tmp_screen_truncated as select r.pk_screen, ST_DIFFERENCE(s.the_geom, ST_BUFFER(ST_ACCUM(b.the_geom), 2)) the_geom from tmp_relation_screen_building r, "+building_table_name+" b, tmp_receivers_lines s WHERE PK_building = b.pk AND pk_screen = s.pk  GROUP BY pk_screen;")
    sql.execute("DROP TABLE IF EXISTS TMP_SCREENS;")
    // union of truncated receivers and non tructated, split line to points
    sql.execute("create table TMP_SCREENS as select s.pk, st_tomultipoint(st_densify(s.the_geom, 5)) the_geom from tmp_receivers_lines s where not st_isempty(s.the_geom) and pk not in (select pk_screen from tmp_screen_truncated) UNION ALL select pk_screen, st_tomultipoint(st_densify(the_geom, 5)) from tmp_screen_truncated where not st_isempty(the_geom);")
    sql.execute("drop table if exists " + receivers_table_name)
    if(!hasPop) {
        sql.execute("create table " + receivers_table_name + "(pk serial, the_geom geometry,build_pk integer) as select null, st_updatez(the_geom," + h + "), pk building_pk from ST_EXPLODE('TMP_SCREENS');")

        if (input['sourcesTableName']) {
            // Delete receivers near sources
            sql.execute("Create spatial index on "+sources_table_name+"(the_geom);")
            sql.execute("delete from "+receivers_table_name+" g where exists (select 1 from "+sources_table_name+" r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

    } else {
        // building have population attribute
        // set population attribute divided by number of receiver to each receiver
        sql.execute("DROP TABLE IF EXISTS tmp_receivers")
        sql.execute("create table tmp_receivers(pk serial, the_geom geometry,build_pk integer) as select null, st_updatez(the_geom," + h + "), pk building_pk from ST_EXPLODE('TMP_SCREENS');")

        if (input['sourcesTableName']) {
            // Delete receivers near sources
            sql.execute("Create spatial index on "+sources_table_name+"(the_geom);")
            sql.execute("delete from tmp_receivers g where exists (select 1 from "+sources_table_name+" r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
        }

        sql.execute("CREATE INDEX REC_BUILD ON tmp_receivers(build_pk)")
        sql.execute("create table " + receivers_table_name + "(pk serial, the_geom geometry,build_pk integer, pop float) as select r.pk, r.the_geom, r.build_pk, b.pop / (select count(*) from tmp_receivers rr where rr.build_pk = r.build_pk limit 1) as pop from tmp_receivers r,"+building_table_name+ " b where r.build_pk = b.pk;")
        sql.execute("drop table if exists tmp_receivers")
    }
    // cleaning
    sql.execute("drop table TMP_SCREENS")
    sql.execute("drop table tmp_screen_truncated")
    sql.execute("drop table tmp_relation_screen_building")
    sql.execute("drop table tmp_receivers_lines")

    // Process Done
    return [tableNameCreated: "Process done. Table of receivers "+ receivers_table_name +" created !"]
}


