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
 * @Author Gwendall Petit, Lab-STICC CNRS UMR 6285 
 */

package org.noise_planet.noisemodelling.scripts.Geometric_Tools

import groovy.sql.Sql
import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileStatic
import org.h2.util.ScriptReader
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.jdbc.railway.RailWayLWIterator
import org.noise_planet.noisemodelling.jdbc.railway.RailwayPlatform
import org.noise_planet.noisemodelling.jdbc.utils.DataBaseUtilities
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.Locale

title = 'Enrich Landcover with railways'
description = '&#10145;&#65039; Insert rail ground surfaces into the input LANDCOVER.</br>' +
              '<hr>' +
              'This script works with two input layers:</br>' +
              ' <ul>' +
                '<li>Landcover to be enriched</li>' +
                '<li>Railways</li>' +
              '</ul>' +
              'And four parameters:</br>' +
              ' <ul>' +
                '<li>Railways width (railWidth): Name of the column where the total railway platform width is stored (Optional). If the column is present, it overrides the total width (d4) defined in the railway platform data file</li>' +
                '<li>Rail platform (platformColumn): Name of column where the platform identifier is stored (Optional). Default value = "PLATFORM"</li>' +
                '<li>Input SRID (inputSRID): SRID of the input tables (Optional)</li>' +
                '<li>Output suffix (outputsuffix): suffix applied at the end of the resuling table name (Optional). If not specified, "ENRICHED" is applied</li>' +
                '<li>Railway platform data file (railwayPlatformDataFile): URL of the railway platform data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used (Optional)</li>' +
              '</ul>' +
              '<hr>' +
              'The railway platform geometry (track gauge, ground factors, heights and section widths) is read from the ' +
              '<b>PLATFORM</b> column of the input railways table and resolved against the railway platform data file. ' +
              'In the schema below, the points (in orange) will be inserted into the DEM. d2, d3 and d4 (the section half-widths) are taken from the platform definition ' +
              '(d2_0, d3_0 and d4_0 respectively), and d4 can be overridden by the <b>railWidth</b> column when it is present in the railways table:' +
              '<ul>' +
                '<li>d2 = d2_0</li>' +
                '<li>d3 = d3_0</li>' +
                '<li>d4 = d4_0, or railWidth if the railWidth column is present</li>' +
              '</ul>' +
              '<img src="wps_images/railway_plateform.png" alt="Railways platform" width="95%" align="center">'

inputs = [
        inputSRID : [
                name       : 'Input SRID',
                title      : 'Input SRID',
                description: '&#127757; SRID of the input tables. </br> </br>'+
                             '&#128736; If not specified, the SRID from DEM layer is applied. If DEM has no SRID, 0 is applied',
                min        : 0, max: 1,
                type       : Integer.class
        ],
        inputLandcover : [
                name       : 'Input landcover',
                title      : 'Input landcover table',
                description: 'Name of the input landcover table',
                type       : String.class
        ],
        gColumn : [
                name       : 'G column',
                title      : 'G column',
                description: 'Ground absorption coefficient (G) column name',
                type       : String.class
        ],
        inputRail : [
                name       : 'Input Railways',
                title      : 'Input railways table',
                description: 'Name of the input railways table',
                type       : String.class
        ],
        railWidth : [
                name       : 'Railways width',
                title      : 'Railways width',
                description: 'Name of column where the total railways platform width is stored. When present, it overrides the total width (d4) defined in the railway platform data file.',
                min        : 0, max: 1,
                type       : String.class
        ],
        platformColumn : [
                name       : 'Platform column',
                title      : 'Platform column',
                description: 'Name of column where the platform identifier is stored. The value is resolved against the railway platform data file.',
                default    : 'PLATFORM',
                min        : 0, max: 1,
                type       : String.class
        ],
        railwayPlatformDataFile : [
                name       : 'Railway platform data file',
                title      : 'Railway platform data file',
                description: 'URL of the railway platform data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used.',
                min        : 0, max: 1,
                type       : String.class
        ],
        outputsuffix : [
                name       : 'Output suffix',
                title      : 'Output suffix',
                description: 'Suffix applied at the end of the resulting table name',
                default    : 'ENRICHED',
                type       : String.class
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

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger(Thread.currentThread().getName())

    ProgressVisitor progressVisitor

    if("progressVisitor" in input) {
        progressVisitor = input["progressVisitor"] as ProgressVisitor
    } else {
        progressVisitor = new RootProgressVisitor(1, true, 1);
    }

    ProgressVisitor progress = progressVisitor.subProcess(2)

    // Get provided parameters
    
    String inputLandcover = input["inputLandcover"]
    String gColumn = input["gColumn"]
    String inputRail = input["inputRail"]

    List<String> railColumns = JDBCUtilities.getColumnNames(connection, TableLocation.parse(inputRail))

    String railWidth = null
    if ('railWidth' in input && input["railWidth"] != null && !(input["railWidth"] as String).trim().isEmpty()) {
        railWidth = input["railWidth"] as String
    }

    // Whether the railWidth column is actually present in the railways table. When present, its value
    // overrides the total platform width (d4) defined in the railway platform data file.
    boolean hasRailWidth = false
    if (railWidth != null) {
        hasRailWidth = railColumns.any { String c -> c.equalsIgnoreCase(railWidth) }
    }

    // Platform column holding the platform identifier of each rail section
    String platformColumn = 'PLATFORM'
    if ('platformColumn' in input && input["platformColumn"] != null && !(input["platformColumn"] as String).trim().isEmpty()) {
        platformColumn = input["platformColumn"] as String
    }
    // Whether the platform column is actually present in the railways table. When absent, every rail
    // section uses the default platform.
    boolean hasPlatformColumn = railColumns.any { String c -> c.equalsIgnoreCase(platformColumn) }

    // Railway platform data file (JSON). By default the one provided with NoiseModelling is used.
    String railwayPlatformDataFile = RailWayLWIterator.RAILWAY_PLATFORMS_JSON
    if ('railwayPlatformDataFile' in input && input["railwayPlatformDataFile"] != null && !(input["railwayPlatformDataFile"] as String).trim().isEmpty()) {
        railwayPlatformDataFile = input["railwayPlatformDataFile"] as String
    }

    // Load the platform definitions (track gauge, ground factors, heights) from the data file
    Map<String, RailwayPlatform> platforms = RailwayPlatform.loadFromJSON(railwayPlatformDataFile)
    if (platforms.isEmpty()) {
        throw new IllegalArgumentException("No platform definition found in the railway platform data file: " + railwayPlatformDataFile)
    }
    String platformInsert = buildPlatformInsert(platforms)

    // Default platform identifier used when the railways table has no platform column or a NULL/empty value
    String defaultPlatform = platforms.containsKey("DEFAULT") ? "DEFAULT" : platforms.keySet().iterator().next()

    // SQL predicate resolving the platform of each rail section. When the platform column is absent, every
    // section uses the default platform. When present, values are uppercased to match the loaded platform keys.
    String platformPredicate = "p.idplatform = '$defaultPlatform'"
    if (hasPlatformColumn) {
        // Validate that every non-empty platform value in the railways table is a known platform
        Enrich_DEM_with_rail.checkPlatforms(connection, platformColumn, inputRail, platforms)
        platformPredicate = "p.idplatform = UPPER(a.$platformColumn)"
    }


    // If no SRID provided, the one from DEM layer is applied
    Integer srid = 0
    if ('inputSRID' in input) {
        srid = input["inputSRID"] as Integer
    }
    else {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(inputLandcover))
    }

    // If no output table name (outputsuffix) provided, ENRICHED is applied
    String outputsuffix = 'ENRICHED'
    if ('outputsuffix' in input) {
        outputsuffix = input["outputsuffix"] as String
    }
    String enrichedLandcover = inputLandcover + "_" + outputsuffix

    // print to command window
    logger.info('List of the input parameters:')
    logger.info('--------------------------------------------')
    logger.info('# SRID: ' + srid)
    logger.info('# Landcover table: ' + inputLandcover)
    logger.info('# Landcover G column: ' + gColumn)
    logger.info('# Railways network table: ' + inputRail)
    logger.info('# Railways width column: ' + (railWidth != null ? railWidth : 'not provided'))
    logger.info('# Railways platform column: ' + platformColumn)
    logger.info('# Railway platform data file: ' + railwayPlatformDataFile)
    logger.info('# Output suffix: ' + outputsuffix)
    logger.info('--------------------------------------------')

    logger.info('Start enrich the Landcover')

    def sql = new Sql(connection)

    // d4 expression: when the railWidth column is present, its value overrides the platform total width (d4)
    String d4Expr = hasRailWidth ? "a.$railWidth" : "p.d4"


    def initPlatform = """
    -- Initialize the rail platform table

    DROP TABLE IF EXISTS PLATEFORM;
    CREATE TABLE PLATEFORM (idPlatform varchar Primary Key, d1 float, d2 float, d3 float, d4 float, g1 float, g2 float, g3 float, h1 float, h2 float);

    $platformInsert
    -- Rail platform: layer PLATEFORM imported
    """


    def import_landcover = """
    ------------
    -- Import Landcover

    DROP TABLE IF EXISTS landcover_to_enrich;
    CREATE TABLE landcover_to_enrich AS SELECT THE_GEOM, CAST($gColumn AS DOUBLE) as g FROM $inputLandcover;
    CREATE SPATIAL INDEX ON landcover_to_enrich(the_geom);

    -- Landcover: layer $inputLandcover imported
    """

    def import_rail = """
    ------------
    -- Import railways (that are on the floor --> POS_SOL=0)

    DROP TABLE IF EXISTS landcover_rail;    
    CREATE TABLE landcover_rail AS SELECT a.THE_GEOM, p.d2 as d2, p.d3 as d3, $d4Expr as d4, 
        p.idplatform, p.d1, p.g1, p.g2, p.g3
        FROM $inputRail a, PLATEFORM p 
        WHERE st_zmin(a.THE_GEOM) > 0 AND $platformPredicate;

    CREATE SPATIAL INDEX ON landcover_rail(THE_GEOM);
    ALTER TABLE landcover_rail ADD PK_LINE INT AUTO_INCREMENT NOT NULL;
    ALTER TABLE landcover_rail add primary key(PK_LINE);
    
    -- Railways: layer $inputRail imported
    """

    def queries_landcover_rail = """
    -- Integrates RAIL_SECTIONS into the Landcover
    ------------------------------------------------------------------
    DROP TABLE IF EXISTS rail_buff_d1, rail_buff_d3, rail_buff_d4;
    CREATE TABLE rail_buff_d1 AS SELECT idplatform, ST_UNION(ST_ACCUM(ST_BUFFER(the_geom, d1/2))) as the_geom FROM landcover_rail GROUP BY idplatform;
    CREATE TABLE rail_buff_d3 AS SELECT idplatform, ST_UNION(ST_ACCUM(ST_BUFFER(the_geom, d3/2))) as the_geom FROM landcover_rail GROUP BY idplatform;
    CREATE TABLE rail_buff_d4 AS SELECT idplatform, ST_UNION(ST_ACCUM(ST_BUFFER(the_geom, d4/2))) as the_geom FROM landcover_rail GROUP BY idplatform;

    DROP TABLE IF EXISTS rail_diff_d3_d1, rail_diff_d4_d3;
    CREATE TABLE rail_diff_d3_d1 as select a.idplatform, ST_SymDifference(a.the_geom, b.the_geom) as the_geom from rail_buff_d3 a, rail_buff_d1 b where a.idplatform = b.idplatform and a.the_geom && b.the_geom and st_intersects(a.the_geom, b.the_geom);
    CREATE TABLE rail_diff_d4_d3 as select a.idplatform, ST_SymDifference(a.the_geom, b.the_geom) as the_geom from rail_buff_d4 a, rail_buff_d3 b where a.idplatform = b.idplatform and a.the_geom && b.the_geom and st_intersects(a.the_geom, b.the_geom);
    
    DROP TABLE IF EXISTS rail_buff_d1_expl, rail_buff_d3_expl, rail_buff_d4_expl;
    CREATE TABLE rail_buff_d1_expl AS SELECT a.the_geom, p.g3 as g FROM ST_Explode('RAIL_BUFF_D1') a, PLATEFORM p WHERE p.IDPLATFORM = a.idplatform;
    CREATE TABLE rail_buff_d3_expl AS SELECT a.the_geom, p.g2 as g FROM ST_Explode('RAIL_DIFF_D3_D1') a, PLATEFORM p WHERE p.IDPLATFORM = a.idplatform;
    CREATE TABLE rail_buff_d4_expl AS SELECT a.the_geom, p.g1 as g FROM ST_Explode('RAIL_DIFF_D4_D3') a, PLATEFORM p WHERE p.IDPLATFORM = a.idplatform;

    DROP TABLE IF EXISTS LANDCOVER_DIFF_D4, LANDCOVER_EXPL;
    -- Union the d4 buffer of every platform into a single geometry, then subtract it once from each polygon.
    -- Otherwise, with several platforms, each polygon would be cut by each platform buffer separately and the
    -- later union would restore the area removed by the other platform buffers.
    CREATE TABLE LANDCOVER_DIFF_D4 AS SELECT ST_Difference(b.the_geom, a.the_geom) as the_geom, b.g
        FROM (SELECT ST_UNION(ST_ACCUM(the_geom)) AS the_geom FROM rail_buff_d4) a, landcover_to_enrich b;
    CREATE TABLE LANDCOVER_EXPL AS SELECT the_geom, g FROM ST_Explode('LANDCOVER_DIFF_D4');

    -- Unifiy tables
    DROP TABLE IF EXISTS LANDCOVER_UNION;
    CREATE TABLE LANDCOVER_UNION AS SELECT * FROM LANDCOVER_EXPL UNION ALL SELECT * FROM RAIL_BUFF_D1_EXPL UNION ALL SELECT * FROM RAIL_BUFF_D3_EXPL UNION ALL SELECT * FROM RAIL_BUFF_D4_EXPL ; 

    -- Create final table
    DROP TABLE IF EXISTS $enrichedLandcover;
    CREATE TABLE $enrichedLandcover AS SELECT ST_SETSRID(the_geom,$srid) as the_geom, g FROM ST_Explode('LANDCOVER_UNION');
    -- Create spatial index on final table
    CREATE SPATIAL INDEX ON $enrichedLandcover(THE_GEOM);

    -- Remove non-needed tables
    DROP TABLE IF EXISTS rail_buff_d1, rail_buff_d3, rail_buff_d4, rail_diff_d3_d1, rail_diff_d4_d3, rail_buff_d1_expl, 
    rail_buff_d3_expl, rail_buff_d4_expl, LANDCOVER_DIFF_D4, LANDCOVER_EXPL, 
    LANDCOVER_UNION, landcover_rail, landcover_to_enrich, PLATEFORM;


    -- Landcover successfully enriched in the table $enrichedLandcover
    """

    StringBuilder stringBuilder = new StringBuilder()
    // print to command window
    def engine = new SimpleTemplateEngine()


    stringBuilder.append(initPlatform)
    stringBuilder.append(import_landcover)
    stringBuilder.append(import_rail)
    stringBuilder.append(queries_landcover_rail)

    def binding = ["inputLandcover": inputLandcover, "gColumn": gColumn, "inputRail": inputRail, "railWidth": railWidth, "platformColumn": platformColumn, "outputsuffix": outputsuffix, "srid": srid, "platformInsert": platformInsert, "d4Expr": d4Expr, "platformPredicate": platformPredicate]
    def template = engine.createTemplate(stringBuilder.toString()).make(binding)
    DataBaseUtilities.parseScript(template.toString(), connection, progress, logger)

    return [result: enrichedLandcover]

}

/**
 * Builds the SQL INSERT statements that populate the PLATEFORM lookup table from the
 * platform definitions loaded from a JSON data file.
 * <p>
 * Each platform contributes one row holding the track gauge (d1), the section half-widths
 * (d2, d3, d4), the ground factors of the platform (g1), the shoulder (g2) and the track (g3)
 * zones, and the platform heights (h1, h2). The half-widths d2, d3 and d4 are taken from the
 * platform definition (d2_0, d3_0 and d4_0 respectively).
 *
 * @param platforms the map of platform identifier (upper case) to its definition
 * @return a string of SQL INSERT statements, one per platform
 */
private static String buildPlatformInsert(Map<String, RailwayPlatform> platforms) {
    StringBuilder sql = new StringBuilder()
    platforms.each { String name, RailwayPlatform p ->
        sql.append(String.format(Locale.ROOT, "INSERT INTO PLATEFORM VALUES ('%s', %f, %f, %f, %f, %f, %f, %f, %f, %f);%n",
                name, p.d1, p.d2_0, p.d3_0, p.d4_0, p.g1, p.g2, p.g3, p.h1, p.h2))
    }
    return sql.toString()
}