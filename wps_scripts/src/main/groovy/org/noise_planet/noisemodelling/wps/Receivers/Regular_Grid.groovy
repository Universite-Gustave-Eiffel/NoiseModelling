/**
 * @Author Aumond Pierre, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Geometry
import java.sql.*
import groovy.sql.Sql

title = 'Regular Grid'
description = 'Calculates a regular grid of receivers based on a single Geometry geom or a table tableName of Geometries with delta as offset in the Cartesian plane in meters.'

inputs = [tableBuilding   : [name       : 'Buildings table name', title: 'Buildings table name',
                             description: '<b>Name of the Buildings table.</b>  </br>  ' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                                     '- <b> HEIGHT </b> : the height of the building (FLOAT)' +
                                     '- <b> POP </b> : optional field, building population to add in the receiver attribute (FLOAT)',
                             type       : String.class],
          fence           : [name         : 'Fence geometry', title: 'Extent filter', description: 'Create receivers only in the' +
                  ' provided polygon', min: 0, max: 1, type: Geometry.class],
          fenceTableName  : [name                                                         : 'Fence geometry from table', title: 'Filter using table bounding box',
                             description                                                  : 'Extract the bounding box of the specified table then create only receivers' +
                                     ' on the table bounding box' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : any geometry type. </br>', min: 0, max: 1, type: String.class],
          sourcesTableName: [name          : 'Sources table name', title: 'Sources table name', description: 'Keep only receivers at least at 1 meters of' +
                  ' provided sources geometries' +
                  '<br>  The table shall contain : </br>' +
                  '- <b> THE_GEOM </b> : any geometry type. </br>', min: 0, max: 1, type: String.class],
          delta           : [name       : 'Receivers minimal distance', title: 'Distance between receivers',
                             description: 'Distance between receivers in the Cartesian plane in meters', type: Double.class],
          height          : [name                               : 'height', title: 'height', description: 'Height of receivers in meters (FLOAT)' +
                  '</br> </br> <b> Default value : 4 </b> ', min: 0, max: 1, type: Double.class]]


outputs = [result: [name: 'Result output string', title: 'Result output string', description: 'This type of result does not allow the blocks to be linked together.', type: String.class]]

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
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}



def exec(Connection connection, input) {

    // output string, the information given back to the user
    String resultString = null

    // print to command window
    System.out.println('Start : Regular grid')
    def start = new java.util.Date()

    String receivers_table_name = "RECEIVERS"


    String fence_table_name = "FENCE"
    if (input['fenceTableName']) {
        fence_table_name = input['fenceTableName']
    }
    fence_table_name = fence_table_name.toUpperCase()


    Double delta = 10
    if (input['delta']) {
        delta = input['delta']
    }

    Double h = 4
    if (input['height']) {
        h = input['height']
    }

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()

    String building_table_name = "BUILDINGS"
    if (input['buildingTableName']) {
        building_table_name = input['buildingTableName']
    }
    building_table_name = building_table_name.toUpperCase()

    String fence = null
    if (input['fence']) {
        fence = (String) input['fence']
    }


    //get SRID of the table
    int srid = SFSUtilities.getSRID(connection, TableLocation.parse(building_table_name))

    Sql sql = new Sql(connection)
    //Delete previous receivers grid.
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))
    String queryGrid = null


    if (input['fence']) {
        sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))
        sql.execute(String.format("CREATE TABLE FENCE AS SELECT ST_AsText('" + fence + "') the_geom"))
        sql.execute(String.format("DROP TABLE IF EXISTS FENCE_2154"))
        sql.execute(String.format("CREATE TABLE FENCE_2154 AS SELECT ST_TRANSFORM(ST_SetSRID(the_geom,4326),2154) the_geom from FENCE"))
        sql.execute(String.format("DROP TABLE IF EXISTS FENCE"))

        queryGrid = String.format("CREATE TABLE " + receivers_table_name + " AS SELECT * FROM ST_MakeGridPoints('FENCE_2154'," + delta + "," + delta + ");")


    } else {
        if (input['fenceTableName']) {

            queryGrid = String.format("CREATE TABLE " + receivers_table_name + " AS SELECT * FROM ST_MakeGridPoints('" + fence_table_name + "'," + delta + "," + delta + ");")


        } else {
            queryGrid = String.format("CREATE TABLE " + receivers_table_name + " AS SELECT * FROM ST_MakeGridPoints('" + building_table_name + "'," + delta + "," + delta + ");")
        }
    }

    sql.execute(queryGrid)

    //New receivers grid created .
    sql.execute("Create spatial index on " + receivers_table_name + "(the_geom);")
    System.out.println('Add SRID to receivers table...')
    // if a SRID exists
    if (srid > 0) {
        sql.execute("UPDATE " + receivers_table_name + " SET THE_GEOM = ST_SetSRID(ST_UPDATEZ(The_geom," + h + "), "+srid+" );")
    }else{
        resultString = "The Buildings table doesn't have associated SRID."
    }


    sql.execute("ALTER TABLE " + receivers_table_name + " ADD pk INT AUTO_INCREMENT PRIMARY KEY;")
    sql.execute("ALTER TABLE " + receivers_table_name + " DROP ID;")
    sql.execute("ALTER TABLE " + receivers_table_name + " DROP ID_COL;")
    sql.execute("ALTER TABLE " + receivers_table_name + " DROP ID_ROW;")

    if (input['fence']) {
        //Delete receivers
        sql.execute("Create spatial index on FENCE_2154(the_geom);")
        sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from FENCE_2154 r where ST_Disjoint(g.the_geom, r.the_geom) limit 1);")
    }
    if (input['fenceTableName']) {
        //Delete receivers
        sql.execute("Create spatial index on " + fence_table_name + "(the_geom);")
        sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from " + fence_table_name + " r where ST_Disjoint(g.the_geom, r.the_geom) limit 1);")
    }

    System.out.println('Delete receivers where buildings...')
    if (input['buildingTableName']) {
        //Delete receivers inside buildings .
        sql.execute("Create spatial index on " + building_table_name + "(the_geom);")
        sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from " + building_table_name + " b where g.the_geom && b.the_geom and ST_distance(b.the_geom, g.the_geom) < 1 and b.height >= "+h+" limit 1);")
    }

    System.out.println('Delete receivers where sound sources...')
    if (input['sourcesTableName']) {
        //Delete receivers near sources
        sql.execute("Create spatial index on " + sources_table_name + "(the_geom);")
        sql.execute("delete from " + receivers_table_name + " g where exists (select 1 from " + sources_table_name + " r where st_expand(g.the_geom, 1) && r.the_geom and st_distance(g.the_geom, r.the_geom) < 1 limit 1);")
    }

    // Process Done
    resultString = "Process done. Table of receivers " + receivers_table_name + " created !"

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Regular Grid')
    System.out.println('Duration : ' + TimeCategory.minus(new java.util.Date(), start))

    // print to WPS Builder
    return resultString

}


