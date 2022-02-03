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
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Import_and_Export

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.io.FileType
import groovy.sql.Sql
import org.apache.commons.io.FilenameUtils
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.DBFDriverFunction
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.functions.io.gpx.GPXDriverFunction
import org.h2gis.functions.io.osm.OSMDriverFunction
import org.h2gis.functions.io.shp.SHPDriverFunction
import org.h2gis.functions.io.tsv.TSVDriverFunction
import org.h2gis.utilities.GeometryMetaData
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

title = 'Clean and fence BBBike tables - https://extract.bbbike.org/'
description = 'Clean and fence BBBike tables - Convert shp folder from BBBike (https://extract.bbbike.org/) to BUILDINGS, GROUND AND ROADS tables. ' +
        ' <br> This script is more robust than IMPORT_OSM but it doesn\'t allow you to retrieve all the information present in OSM (such as the number of floors or the height of buildings for example).' +
        '<br> The user can choose to create one to three output tables : <br>' +
        '-  <b> BUILDINGS  </b> : a table containing the building. </br>' +
        '-  <b> GROUND  </b> : surface/ground acoustic absorption table. </br>' +
        '-  <b> ROADS  </b> : a table containing the roads. </br>'

inputs = [
        importFolder    : [
                name       : 'BBBike Folder',
                title      : 'BBBike Folder',
                description: 'ImportFoler',
                type       : String.class
        ],
        convert2Building: [
                name       : 'Do not import Buildings',
                title      : 'Do not import Buildings',
                description: 'If the box is checked, the table BUILDINGS will NOT be extracted. ' +
                        '<br>  The table will contain : </br>' +
                        '- <b> THE_GEOM </b> : the 2D geometry of the building (POLYGON or MULTIPOLYGON). </br>' +
                        '- <b> HEIGHT </b> : the height of the building (FLOAT). ' +
                        'If the height of the buildings is not available then it is deducted from the number of floors (if available) with the addition of a small random variation from one building to another. ' +
                        'Finally, if no information is available, a height of 5 m is set by default.',
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        convert2Ground  : [
                name       : 'Do not import Surface acoustic absorption',
                title      : 'Do not import Surface acoustic absorption',
                description: 'If the box is checked, the table GROUND will NOT be extracted.' +
                        '</br>The table will contain : </br> ' +
                        '- <b> THE_GEOM </b> : the 2D geometry of the sources (POLYGON or MULTIPOLYGON).</br> ' +
                        '- <b> G </b> : the acoustic absorption of a ground (FLOAT between 0 : very hard and 1 : very soft).</br> ',
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        convert2Roads   : [
                name       : 'Do not import Roads',
                title      : 'Do not import Roads',
                description: 'If the box is checked, the table ROADS will NOT be extracted. ' +
                        "<br>  The table will contain : </br>" +
                        "- <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)<br/>" +
                        "- <b> TV_D </b> : Hourly average light and heavy vehicle count (6-18h) (DOUBLE)<br/>" +
                        "- <b>TV_E </b> :  Hourly average light and heavy vehicle count (18-22h) (DOUBLE)<br/>" +
                        "- <b> TV_N </b> :  Hourly average light and heavy vehicle count (22-6h) (DOUBLE)<br/>" +
                        "- <b> HV_D </b> :  Hourly average heavy vehicle count (6-18h) (DOUBLE)<br/>" +
                        "- <b> HV_E </b> :  Hourly average heavy vehicle count (18-22h) (DOUBLE)<br/>" +
                        "- <b> HV_N </b> :  Hourly average heavy vehicle count (22-6h) (DOUBLE)<br/>" +
                        "- <b> LV_SPD_D </b> :  Hourly average light vehicle speed (6-18h) (DOUBLE)<br/>" +
                        "- <b> LV_SPD_E </b> :  Hourly average light vehicle speed (18-22h) (DOUBLE)<br/>" +
                        "- <b> LV_SPD_N </b> :  Hourly average light vehicle speed (22-6h) (DOUBLE)<br/>" +
                        "- <b> HV_SPD_D </b> :  Hourly average heavy vehicle speed (6-18h) (DOUBLE)<br/>" +
                        "- <b> HV_SPD_E </b> :  Hourly average heavy vehicle speed (18-22h) (DOUBLE)<br/>" +
                        "- <b> HV_SPD_N </b> :  Hourly average heavy vehicle speed (22-6h) (DOUBLE)<br/>" +
                        "- <b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05) (VARCHAR)" +
                        "</br> </br> <b> This information is created using the importance of the roads in OSM.</b>.",
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        inputSRID       : [
                name       : 'Projection identifier',
                title      : 'Projection identifier',
                description: 'New projection identifier (also called SRID) of your table. ' +
                        'It should be an EPSG code, a integer with 4 or 5 digits (ex: 3857 is Web Mercator projection). ' +
                        '</br>  All coordinates will be projected from the specified EPSG to WGS84 coordinates. ' +
                        '</br> This entry is optional because many formats already include the projection and you can also import files without geometry attributes.',
                type       : Integer.class
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

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()

    // output string, the information given back to the user
    String resultString = ""

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Get Input Data from OSM')
    logger.info("inputs {}", input) // log inputs of the run


    // -------------------
    // Get every inputs
    // -------------------
    // Get new SRID
    int input_srid = input['inputSRID'] as Integer

    // Get BBBike extraction folder
    String pathFolder = input['importFolder'] as String

    Boolean ignoreBuilding = false
    if ('convert2Building' in input) {
        ignoreBuilding = input['convert2Building'] as Boolean
    }

    Boolean ignoreGround = false
    if ('convert2Ground' in input) {
        ignoreGround = input['convert2Ground'] as Boolean
    }

    Boolean ignoreRoads = false
    if ('convert2Roads' in input) {
        ignoreRoads = input['convert2Roads'] as Boolean
    }


// -------------------------
    // Begin Import Folder
    // -------------------------

    // Inputs
    int srid = input_srid
    String importExt = "shp"
    String folder = pathFolder
    def dir = new File(folder)
    // name of the imported tables
    String outputTableName_full = ""

    dir.eachFileRecurse(FileType.FILES) { file ->

        String pathFile = file as String
        String ext = pathFile.substring(pathFile.lastIndexOf('.') + 1, pathFile.length())

        if (ext == importExt) {

            // get the name of the fileName
            String fileName = FilenameUtils.removeExtension(new File(pathFile).getName())
            // replace whitespaces by _ in the file name
            fileName.replaceAll("\\s", "_")
            // remove special characters in the file name
            fileName.replaceAll("[^a-zA-Z0-9 ]+", "_")
            // the tableName will be called as the fileName
            String outputTableName = fileName.toUpperCase()
            TableLocation outputTableIdentifier = TableLocation.parse(outputTableName, DBUtils.getDBType(connection))

            // Drop the table if already exists
            String dropOutputTable = "drop table if exists \"" + outputTableName + "\";"
            stmt.execute(dropOutputTable)

            switch (ext) {
                case "csv":
                    CSVDriverFunction csvDriver = new CSVDriverFunction()
                    csvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "dbf":
                    DBFDriverFunction dbfDriver = new DBFDriverFunction()
                    dbfDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "geojson":
                    GeoJsonDriverFunction geoJsonDriver = new GeoJsonDriverFunction()
                    geoJsonDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "gpx":
                    GPXDriverFunction gpxDriver = new GPXDriverFunction()
                    gpxDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "bz2":
                    OSMDriverFunction osmDriver = new OSMDriverFunction()
                    osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "gz":
                    OSMDriverFunction osmDriver = new OSMDriverFunction()
                    osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "osm":
                    OSMDriverFunction osmDriver = new OSMDriverFunction()
                    osmDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "shp":
                    SHPDriverFunction shpDriver = new SHPDriverFunction()
                    shpDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
                case "tsv":
                    TSVDriverFunction tsvDriver = new TSVDriverFunction()
                    tsvDriver.importFile(connection, outputTableName, new File(pathFile), new EmptyProgressVisitor())
                    outputTableName_full = outputTableName + " & " + outputTableName_full
                    break
            }


            ResultSet rs = stmt.executeQuery("SELECT * FROM \"" + outputTableName + "\"")

            int pk2Field = JDBCUtilities.getFieldIndex(rs.getMetaData(), "PK2")
            int pkField = JDBCUtilities.getFieldIndex(rs.getMetaData(), "PK")

            if (pk2Field > 0 && pkField > 0) {
                stmt.execute("ALTER TABLE " + outputTableName + " DROP COLUMN PK2;")
                logger.warn("The PK2 column automatically created by the SHP driver has been deleted.")
            }

            // Read Geometry Index and type of the table
            List<String> spatialFieldNames = GeometryTableUtilities.getGeometryColumnNames(connection, TableLocation.parse(outputTableName, DBUtils.getDBType(connection)))

            // If the table does not contain a geometry field
            if (spatialFieldNames.isEmpty()) {
                logger.warn("The table " + outputTableName + " does not contain a geometry field.")
            } else {
                stmt.execute('CREATE SPATIAL INDEX IF NOT EXISTS ' + outputTableName + '_INDEX ON ' + TableLocation.parse(outputTableName) + '(the_geom);')
                // Get the SRID of the table
                Integer tableSrid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(outputTableName))

                if (tableSrid != 0 && tableSrid != srid ) {
                    resultString = "The table " + outputTableName + " already has a different SRID than the one you gave."
                    throw new Exception('ERROR : ' + resultString)
                }

                // Replace default SRID by the srid of the table
                if (tableSrid != 0) srid = tableSrid

                // Display the actual SRID in the command window
                logger.info("The SRID of the table " + outputTableName + " is " + srid)

                // If the table does not have an associated SRID, add a SRID
                if (tableSrid == 0) {
                    Statement st = connection.createStatement()
                    GeometryMetaData metaData = GeometryTableUtilities.getMetaData(connection, outputTableIdentifier, spatialFieldNames.get(0));
                    metaData.setSRID(srid);
                    st.execute(String.format("ALTER TABLE %s ALTER COLUMN %s %s USING ST_SetSRID(%s,%d)", outputTableIdentifier, spatialFieldNames.get(0), metaData.getSQL(),spatialFieldNames.get(0) ,srid))
                }
            }

            // If the table has a PK column and doesn't have any Primary Key Constraint, then automatically associate a Primary Key
            ResultSet rs2 = stmt.executeQuery("SELECT * FROM \"" + outputTableName + "\"")
            int pkUserIndex = JDBCUtilities.getFieldIndex(rs2.getMetaData(), "PK")
            int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, outputTableIdentifier)

            if (pkIndex == 0) {
                if (pkUserIndex > 0) {
                    stmt.execute("ALTER TABLE " + outputTableIdentifier + " ALTER COLUMN PK INT NOT NULL;")
                    stmt.execute("ALTER TABLE " + outputTableIdentifier + " ADD PRIMARY KEY (PK);  ")
                    resultString = resultString + String.format(outputTableIdentifier.toString() + " has a new primary key constraint on PK")
                    logger.info(String.format(outputTableIdentifier.toString() + " has a new primary key constraint on PK"))
                }
            }

        }
    }
    // -------------------------
    // End Import Folder
    // -------------------------

    // -------------------------
    // Initialize some variables
    // -------------------------

    // This is the name of the OSM tables when the file is imported.
    String[] osm_tables = ["BUILDINGS", "LANDUSE", "NATURAL",
                           "PLACES", "POINTS", "RAILWAYS", "ROADS", "WATERWAYS"]

    String[] osm_tables_temp = ["BUILDINGS_TEMP", "TMP_RELATION_BUILDINGS", "NATURAL","LANDUSE","LANDUSE_TEMP", "NATURAL_TEMP",
                                "PLACES_TEMP", "POINTS_TEMP", "RAILWAYS_TEMP", "ROADS_TEMP", "WATERWAYS_TEMP", "PLACES", "POINTS", "RAILWAYS", "WATERWAYS"]

    // Loop over every OSM BBBike tables.
    // Add SRID or define SRID
    osm_tables.each { tableName ->
        TableLocation tableIdentifier = TableLocation.parse(tableName, DBUtils.getDBType(connection))
        // Get the PrimaryKey field if exists to keep it in the final table
        int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(tableName, DBUtils.getDBType(connection)))

        // Build the result string with every tables
        StringBuilder sbFields = new StringBuilder()

        // Get the column names to keep all column in the final table
        List<String> fields = JDBCUtilities.getColumnNames(connection, tableIdentifier)
        int k = 1
        String pkField = ""
        fields.each {
            f ->
                if (f != "THE_GEOM") {
                    sbFields.append(String.format(" , %s ", f))
                }
                if (pkIndex == k) pkField = f.toString()
                k++
        }

        if(pkField.isEmpty()) {
            throw new SQLException("No pk field in the table " + tableName)
        }
        //get SRID of the table
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(tableName))

        // if a SRID exists
        if (srid > 0) {
            if (srid == input_srid)
                logger.info("The table already counts " + input_srid.toString() + " as SRID.")
            else {
                sql.execute("CREATE table temp as select ST_Transform(the_geom," + input_srid.toInteger() + ") THE_GEOM" + sbFields + " FROM " + TableLocation.parse(tableName).toString())
                sql.execute("DROP TABLE" + TableLocation.parse(tableName).toString())
                sql.execute("CREATE TABLE" + TableLocation.parse(tableName).toString() + " AS SELECT * FROM TEMP")
                sql.execute("DROP TABLE TEMP")
                logger.info("SRID changed from " + srid.toString() + " to " + input_srid.toString() + ".")
            }
        } else {     // if the table doesn't have any associated SRID
            sql.execute("CREATE table temp as select ST_SetSRID(the_geom," + input_srid.toInteger() + ") THE_GEOM" + sbFields + " FROM " + TableLocation.parse(tableName).toString())
            sql.execute("DROP TABLE" + TableLocation.parse(tableName).toString())
            sql.execute("CREATE TABLE" + TableLocation.parse(tableName).toString() + " AS SELECT * FROM TEMP")
            sql.execute("DROP TABLE TEMP")
            logger.warn("No SRID found ! Table " + tableName.toString() + " has now the SRID : " + input_srid.toString() + ".")
        }

        // get the index of the primary key column (if exists > 0)
        pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(tableName))

        // Reattribute Primary key
        if (pkIndex ==0) {
            sql.execute("ALTER TABLE " + tableName.toString() + " ALTER COLUMN " + pkField + " INT NOT NULL;")
            sql.execute("ALTER TABLE " + tableName.toString() + " ADD PRIMARY KEY (" + pkField + ");  ")
        }

    }

    logger.info('SRID ok')


    // IMPORT BUILDINGS
    if (!ignoreBuilding) {

        sql.execute('drop table if exists buildings_temp;'+
                'create table buildings_temp as select ST_MAKEVALID(ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(THE_GEOM,0.1),1)) THE_GEOM, PK, OSM_ID, NAME, "TYPE" from buildings  WHERE ST_Perimeter(THE_GEOM)<1000;')


        System.out.println('Make valid ok')

        sql.execute("ALTER TABLE buildings_temp ALTER COLUMN PK INT NOT NULL;")
        sql.execute("ALTER TABLE buildings_temp ADD PRIMARY KEY (PK); ")
        sql.execute('CREATE SPATIAL INDEX IF NOT EXISTS BUILDINGS_INDEX ON buildings_temp(the_geom);'+
                'drop table if exists tmp_relation_buildings;'+
                'create table tmp_relation_buildings as select s1.PK as PK_BUILDING, S2.PK as PK2_BUILDING FROM buildings_temp S1, buildings_temp S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.1;')

        System.out.println('Intersection founded')

        sql.execute("CREATE INDEX ON tmp_relation_buildings(PK_BUILDING);"+
                "drop table if exists tmp_buildings_truncated;" +
                "create table tmp_buildings_truncated as select PK_BUILDING, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_ACCUM(s2.the_geom), 0.1, 'join=mitre')) the_geom from tmp_relation_buildings r, buildings_temp s1, buildings_temp s2 WHERE PK_BUILDING = S1.PK  AND PK2_BUILDING = S2.PK   GROUP BY PK_BUILDING;")

        System.out.println('Intersection tmp_buildings_truncated')
        sql.execute("DROP TABLE IF EXISTS BUILDINGS2;")
        sql.execute("create table BUILDINGS2(PK INTEGER PRIMARY KEY, THE_GEOM GEOMETRY)  as select s.PK, s.the_geom from  BUILDINGS_TEMP s where PK not in (select PK_BUILDING from tmp_buildings_truncated) UNION ALL select PK_BUILDING, the_geom from tmp_buildings_truncated WHERE NOT st_isempty(the_geom);")

        sql.execute("drop table if exists tmp_buildings_truncated;")

        sql.execute("DROP TABLE IF EXISTS BUILDINGS;")
        sql.execute("create table BUILDINGS as select * FROM BUILDINGS2;")
        sql.execute("alter table BUILDINGS add column height double;  ")
        sql.execute("update BUILDINGS set height = round(4 + RAND() * 2,1) where height is null;")

        GeometryMetaData metaData = GeometryTableUtilities.getMetaData(connection, "BUILDINGS", "THE_GEOM");
        metaData.setSRID(input_srid.toInteger());
        sql.execute(String.format("ALTER TABLE %s ALTER COLUMN %s %s USING ST_SetSRID(%s,%d)", TableLocation.parse("BUILDINGS"), "THE_GEOM", metaData.getSQL(),"THE_GEOM" ,input_srid.toInteger()))

        sql.execute('CREATE SPATIAL INDEX IF NOT EXISTS BUILDINGS_INDEX ON BUILDINGS(the_geom);')

        sql.execute("DROP TABLE IF EXISTS BUILDINGS2;")


        // -------------------
        // Get every inputs
        // -------------------

        // import building_table_name
        String building_table_name =  "BUILDINGS"

        // do it case-insensitive
        building_table_name = building_table_name.toUpperCase()

        //get SRID of the table
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name))
        if (srid == 3785 || srid == 4326) throw new IllegalArgumentException("Error : This SRID is not metric. Please use another SRID for your table.")
        if (srid == 0) throw new IllegalArgumentException("Error : The table does not have an associated SRID.")

        // -------------------------
        // Initialize some variables
        // -------------------------

        sql.execute('drop table if exists buildings_temp;' +
                'create table buildings_temp as select ST_MAKEVALID(ST_precisionreducer(ST_SIMPLIFYPRESERVETOPOLOGY(THE_GEOM,0.1),0.1)) THE_GEOM, PK, HEIGHT from '+building_table_name+'  WHERE ST_Perimeter(THE_GEOM)<1000;')

        logger.info('Make valid every buildings - ok')

        sql.execute("ALTER TABLE buildings_temp ALTER COLUMN PK INT NOT NULL;")
        sql.execute("ALTER TABLE buildings_temp ADD PRIMARY KEY (PK); ")
        sql.execute('CREATE SPATIAL INDEX IF NOT EXISTS BUILDINGS_INDEX ON buildings_temp(the_geom);' +
                'drop table if exists tmp_relation_buildings;' +
                'create table tmp_relation_buildings as select s1.PK as PK_BUILDING, S2.PK as PK2_BUILDING FROM buildings_temp S1, buildings_temp S2 WHERE ST_AREA(S1.THE_GEOM) < ST_AREA(S2.THE_GEOM) AND S1.THE_GEOM && S2.THE_GEOM AND ST_DISTANCE(S1.THE_GEOM, S2.THE_GEOM) <= 0.1;')

        logger.info('Intersection founded')

        sql.execute("CREATE INDEX ON tmp_relation_buildings(PK_BUILDING);" +
                "drop table if exists tmp_buildings_truncated;" +
                "create table tmp_buildings_truncated as select PK_BUILDING, ST_DIFFERENCE(s1.the_geom,  ST_BUFFER(ST_ACCUM(s2.the_geom), 0.1, 'join=mitre')) the_geom, s1.HEIGHT from tmp_relation_buildings r, buildings_temp s1, buildings_temp s2 WHERE PK_BUILDING = S1.PK  AND PK2_BUILDING = S2.PK   GROUP BY PK_BUILDING;")

        logger.info('Intersection remove buildings with intersections')

        sql.execute("DROP TABLE IF EXISTS "+building_table_name+";")
        sql.execute("create table "+building_table_name+"(PK INTEGER PRIMARY KEY, THE_GEOM GEOMETRY, HEIGHT FLOAT)  as select s.PK, s.the_geom, s.HEIGHT from  BUILDINGS_TEMP s where PK not in (select PK_BUILDING from tmp_buildings_truncated) UNION ALL select PK_BUILDING, the_geom, HEIGHT from tmp_buildings_truncated WHERE NOT st_isempty(the_geom);")

        sql.execute("drop table if exists tmp_buildings_truncated;")


        logger.info('The table BUILDINGS has been created.')
        resultString = resultString + ' <br> The table BUILDINGS has been created.'
    }

    // IMPORT GROUND
    if (!ignoreGround) {
        // LANDUSE TO GROUND
        String Ground_Import = "DROP TABLE GROUND IF EXISTS;" +
                "create table GROUND(the_geom geometry, surfcat varchar, G double) as " +
                "select   l.THE_GEOM the_geom , l.TYPE, 1 from LANDUSE l where l.TYPE IN ('grass', 'village_green', 'park');" +
                "ALTER TABLE GROUND ADD COLUMN PK SERIAL;"

        sql.execute(Ground_Import)

        sql.execute('CREATE SPATIAL INDEX IF NOT EXISTS GROUND_INDEX ON GROUND(the_geom);')

        logger.info('The table GROUND has been created.')
        resultString = resultString + ' <br> The table GROUND has been created.'
    }

    // IMPORT ROADS
    if (!ignoreRoads) {


        sql.execute("drop table ROADS_TEMP if exists;")
        sql.execute("create table ROADS_TEMP as select ST_UNION(a.the_geom) the_geom, a.PK, a.OSM_ID, a.NAME, a.REF,a.TYPE, a.ONEWAY, a.MAXSPEED,  a.BRIDGE from ROADS a WHERE ST_GeometryTypeCode(a.THE_GEOM) = 2 ;")
        sql.execute("drop table ROADS_TEMP2 if exists;")
        sql.execute("CREATE TABLE ROADS_TEMP2 AS SELECT ST_UpdateZ(ST_FORCE3D(THE_GEOM),0.05) THE_GEOM, PK, OSM_ID, NAME,\"REF\" R, \"TYPE\" T,ONEWAY,  MAXSPEED MAX_SPEED, BRIDGE FROM ROADS_TEMP;")

        // Create a second sql connection to interact with the database in SQL
        def aadf_d = [17936, 7124, 1400, 700, 350, 175]
        def aadf_e = [3826, 1069, 400, 200, 100, 50]
        def aadf_n = [2152, 712, 200, 100, 50, 25]
        def hv_d = [0.2, 0.2, 0.15, 0.10, 0.05, 0.02]
        def hv_e = [0.2, 0.15, 0.10, 0.06, 0.02, 0.01]
        def hv_n = [0.2, 0.05, 0.05, 0.03, 0.01, 0.0]
        def speed = [110, 80, 50, 50, 30, 30]

        String Roads_Import2 = "DROP TABLE IF EXISTS ROADS_AADF;\n" +
                "CREATE TABLE ROADS_AADF(OSM_ID long , THE_GEOM GEOMETRY(LINESTRING, "+srid+"), CLAS_ADM int, AADF int, CLAS_ALT int) as SELECT OSM_ID, THE_GEOM,\n" +
                "CASEWHEN(T = 'trunk', 21,\n" +
                "CASEWHEN(T = 'primary', 41,\n" +
                "CASEWHEN(T = 'secondary', 41,\n" +
                "CASEWHEN(T = 'tertiary',41, 57)))) CLAS_ADM,\n" +

                "CASEWHEN(T = 'trunk', 47000,\n" +
                "CASEWHEN(T = 'primary', 35000,\n" +
                "CASEWHEN(T = 'secondary', 12000,\n" +
                "CASEWHEN(T = 'tertiary',7800,\n" +
                "CASEWHEN(T = 'residential',4000, 1600\n" +
                "))))) AADF," +

                "CASEWHEN(T = 'trunk', 1,\n" +
                "CASEWHEN(T = 'trunk_link', 1,\n" +
                "CASEWHEN(T = 'primary', 2,\n" +
                "CASEWHEN(T = 'primary_link', 2,\n" +
                "CASEWHEN(T = 'secondary', 3,\n" +
                "CASEWHEN(T = 'secondary_link', 3,\n" +
                "CASEWHEN(T = 'tertiary', 3,\n" +
                "CASEWHEN(T = 'tertiary_link' AND MAX_SPEED > 40, 4,\n" +
                "CASEWHEN(T = 'residential' AND MAX_SPEED > 40, 4,\n" +
                "CASEWHEN(T = 'unclassified' AND MAX_SPEED > 40, 4,\n" +
                "CASEWHEN(T = 'tertiary_link' AND MAX_SPEED <= 40, 5,\n" +
                "CASEWHEN(T = 'residential' AND MAX_SPEED <= 40, 5,\n" +
                "CASEWHEN(T = 'unclassified' AND MAX_SPEED <= 40, 5,\n" +
                "CASEWHEN(T = 'tertiary_link' AND MAX_SPEED IS NULL, 5,\n" +
                "CASEWHEN(T = 'residential' AND MAX_SPEED IS NULL, 5,\n" +
                "CASEWHEN(T = 'unclassified' AND MAX_SPEED IS NULL, 5,\n" +
                "CASEWHEN(T = 'service', 6,\n" +
                "CASEWHEN(T = 'living_street',6, 6)))))))))))))))))) CLAS_ALT  FROM ROADS_TEMP2 ;" +
                "ALTER TABLE ROADS_AADF ADD COLUMN ID SERIAL;"

        sql.execute(Roads_Import2)

        Sql sql2 = new Sql(connection)

        // Create final Road table
        sql.execute("DROP TABLE ROADS IF EXISTS;")
        sql.execute("drop table if exists ROADS;")
        sql.execute("create table ROADS (PK serial, ID_WAY integer, THE_GEOM geometry, TV_D integer, TV_E integer,TV_N integer,HV_D integer,HV_E integer,HV_N integer,LV_SPD_D integer,LV_SPD_E integer,LV_SPD_N integer,HV_SPD_D integer, HV_SPD_E integer,HV_SPD_N integer, PVMT varchar(10));")
        def qry = 'INSERT INTO ROADS(ID_WAY, THE_GEOM, TV_D, TV_E,TV_N,HV_D,HV_E,HV_N,LV_SPD_D,LV_SPD_E,LV_SPD_N,HV_SPD_D , HV_SPD_E ,HV_SPD_N , PVMT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);'

        sql2.eachRow('SELECT OSM_ID, THE_GEOM, CLAS_ALT FROM ROADS_AADF ;') { row ->
            int idway = (int) row[0]
            Geometry the_geom = (Geometry) row[1]
            int classif = (int) row[2] - 1

            sql2.withBatch(100, qry) { ps ->
                ps.addBatch(idway as Integer, the_geom as Geometry,
                        aadf_d[classif] as Integer, aadf_e[classif] as Integer, aadf_n[classif] as Integer,
                        aadf_d[classif] * hv_d[classif] as Integer, aadf_e[classif] * hv_e[classif] as Integer, aadf_n[classif] * hv_n[classif] as Integer,
                        speed[classif] as Integer, speed[classif] as Integer, speed[classif] as Integer,
                        speed[classif] as Integer, speed[classif] as Integer, speed[classif] as Integer,
                        'NL08' as String)

            }
        }

        sql.execute("DROP TABLE ROADS_AADF IF EXISTS;")
        sql.execute("DROP TABLE ROADS_TEMP IF EXISTS;")
        sql.execute("DROP TABLE ROADS_TEMP2 IF EXISTS;")

        sql.execute('CREATE SPATIAL INDEX IF NOT EXISTS ROADS_INDEX ON ROADS(the_geom);')


        logger.info('The table ROADS has been created.')
        resultString = resultString + ' <br> The table ROADS has been created.'
    }


    osm_tables_temp.each { tableName ->
        sql.execute("DROP TABLE " + TableLocation.parse(tableName, DBUtils.getDBType(connection)).toString() + " IF EXISTS;")
    }


    resultString = resultString + "<br> Calculation Done !"

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : Osm To Input Data')

    // print to WPS Builder
    return resultString


}

