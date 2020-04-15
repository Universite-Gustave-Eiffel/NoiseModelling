/**
 * NoiseModelling is a free and open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Université Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 * as part of:
 * the Eval-PDU project (ANR-08-VILL-0005) 2008-2011, funded by the Agence Nationale de la Recherche (French)
 * the CENSE project (ANR-16-CE22-0012) 2017-2021, funded by the Agence Nationale de la Recherche (French)
 * the Nature4cities (N4C) project, funded by European Union’s Horizon 2020 research and innovation programme under grant agreement No 730468
 *
 * Noisemap is distributed under GPL 3 license.
 *
 * Contact: contact@noise-planet.org
 *
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488) and Ifsttar
 * Copyright (C) 2013-2019 Ifsttar and CNRS
 * Copyright (C) 2020 Université Gustave Eiffel and CNRS
 *
 * @Author Nicolas Fortin, Université Gustave Eiffel
 * @Author Pierre Aumond, Université Gustave Eiffel
 */


package org.noise_planet.noisemodelling.wps.Receivers

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.time.TimeCategory
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.functions.spatial.crs.ST_SetSRID
import org.h2gis.functions.spatial.crs.ST_Transform
import org.h2gis.utilities.SFSUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTReader

import java.sql.Connection

import groovy.sql.Sql

title = 'Random Grid'
description = '[H2GIS] Calculates a random grid of receivers. Return a table named RECEIVERS'

inputs = [buildingTableName: [name       : 'Buildings table name', title: 'Buildings table name',
                              description: '<b>Name of the Buildings table.</b>  </br>  ' +
                                      '<br>  The table shall contain : </br>' +
                                      '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                                      '- <b> HEIGHT </b> : the height of the building (FLOAT)',
                              type       : String.class],
          sourcesTableName : [name                                     : 'Sources table name', title: 'Sources table name', description: 'Keep only receivers at least at 1 meters of provided sources geometries' +
                  '<br>  The table shall contain : </br>' +
                  '- <b> THE_GEOM </b> : any geometry type. </br>', type: String.class],
          nReceivers       : [name: 'Number of receivers', title: 'Number of receivers', description: 'Number of receivers to return </br> </br> <b> Default value : 100 </b> ', type: Integer.class],
          height          : [name                               : 'height', title: 'height', description: 'Height of receivers in meters (FLOAT)' +
                  '</br> </br> <b> Default value : 4 </b> ', min: 0, max: 1, type: Double.class],
          fence           : [name         : 'Fence geometry', title: 'Extent filter', description: 'Create receivers only in the' +
        ' provided polygon', min: 0, max: 1, type: Geometry.class],
          fenceTableName  : [name                                                         : 'Fence geometry from table', title: 'Filter using table bounding box',
                             description                                                  : 'Extract the bounding box of the specified table then create only receivers' +
                                     ' on the table bounding box' +
                                     '<br>  The table shall contain : </br>' +
                                     '- <b> THE_GEOM </b> : any geometry type. </br>', min: 0, max: 1, type: String.class]]

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
    System.out.println('Start : Random grid')
    def start = new Date()

    String receivers_table_name = "RECEIVERS"

    Integer nReceivers = 100
    if (input['nReceivers']) {
        nReceivers = input['nReceivers']
    }

    Double h = 4.0d
    if (input['height']) {
        h = input['height'] as Double
    }

    String sources_table_name = "SOURCES"
    if (input['sourcesTableName']) {
        sources_table_name = input['sourcesTableName']
    }
    sources_table_name = sources_table_name.toUpperCase()


    String building_table_name = input['buildingTableName']
    building_table_name = building_table_name.toUpperCase()

    Sql sql = new Sql(connection)

    // Reproject fence
    int targetSrid = SFSUtilities.getSRID(connection, TableLocation.parse(building_table_name))
    if (targetSrid == 0 && input['sourcesTableName']) {
        targetSrid = SFSUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
    }

    Geometry fenceGeom = null
    if (input['fence']) {
        if (targetSrid != 0) {
            // Transform fence to the same coordinate system than the buildings & sources
            WKTReader wktReader = new WKTReader()
            fence = wktReader.read(input['fence'] as String)
            fenceGeom = ST_Transform.ST_Transform(connection, ST_SetSRID.setSRID(fence, 4326), targetSrid)
        } else {
            System.err.println("Unable to find buildings or sources SRID, ignore fence parameters")
        }
    } else if (input['fenceTableName']) {
        fenceGeom = (new GeometryFactory()).toGeometry(SFSUtilities.getTableEnvelope(connection, TableLocation.parse(input['fenceTableName'] as String), "THE_GEOM"))
    }


    //Delete previous receivers grid...
    sql.execute(String.format("DROP TABLE IF EXISTS %s", receivers_table_name))

    def filter_geom_query = ""

    Envelope envelope
    if (fenceGeom == null) {
        envelope = SFSUtilities.getTableEnvelope(connection, TableLocation.parse(sources_table_name), "THE_GEOM");
        envelope.expandToInclude(SFSUtilities.getTableEnvelope(connection, TableLocation.parse(building_table_name), "THE_GEOM"))
    } else {
        envelope = fenceGeom.envelopeInternal
    }


    sql.execute("create table " + receivers_table_name + " as select ST_SetSRID(ST_MAKEPOINT(RAND()*(" + envelope.maxX + " - " + envelope.minX.toString() + ") + " + envelope.minX.toString() + ", RAND()*(" + envelope.maxY.toString() + " - " + envelope.minY.toString() + ") + " + envelope.minY.toString() + ", "+h+"), " + targetSrid.toInteger() + ") as the_geom from system_range(0," + nReceivers.toString() + ");")
    sql.execute("Create spatial index on " + receivers_table_name + "(the_geom);")

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

    System.out.println('Add Primary Key column...')
    sql.execute("ALTER TABLE " + receivers_table_name + " ADD pk INT AUTO_INCREMENT PRIMARY KEY;")

    // Process Done
    resultString = "Process done. Table of receivers " + receivers_table_name + " created !"

    // print to command window
    System.out.println('Result : ' + resultString)
    System.out.println('End : Random receivers ')
    System.out.println('Duration : ' + TimeCategory.minus(new Date(), start))

    // print to WPS Builder
    return resultString

}
