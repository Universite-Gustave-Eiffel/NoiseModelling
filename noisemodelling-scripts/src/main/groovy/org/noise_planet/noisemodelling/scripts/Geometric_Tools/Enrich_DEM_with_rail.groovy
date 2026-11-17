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
import groovy.transform.CompileStatic
import org.h2.util.ScriptReader
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
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

title = 'Enrich DEM with railways'
description = '&#10145;&#65039; Insert altimetric points coming from railways into the input DEM.</br>' +
              '<hr>' +
              'This script works with two input layers:</br>' +
              ' <ul>' +
                '<li>Digital Elevation Model (DEM) to be enriched</li>' +
                '<li>Railways</li>' +
              '</ul>' +
              'And seven parameters:</br>' +
              ' <ul>' +
                '<li>Railways width (railWidth): Name of the column where the total railway platform width is stored (Optional). If the column is present, it overrides the total width (d4) defined in the railway platform data file</li>' +
                '<li>Rail platform (platformColumn): Name of column where the platform identifier is stored (Optional). Default value = "PLATFORM"</li>' +
                '<li>Simplify tolerance (simplifyTolerance): Distance (in meters) used by the ST_Simplify function to simplify the rail geometries before computing the buffers (Optional). Default value = 0.5m</li>' +
                '<li>Densify distance (densifyDistance): Distance (in meters) between consecutive points created by the ST_Densify function on the buffered rail geometries (Optional). Default value = 5m</li>' +
                '<li>Input SRID (inputSRID): SRID of the input tables (Optional)</li>' +
                '<li>Output suffix (outputsuffix): suffix applied at the end of the resuling table name (Optional). If not specified, "ENRICHED" is applied</li>' +
                '<li>Railway platform data file (railwayPlatformDataFile): URL of the railway platform data file in CNOSSOS format (json). By default, the file provided with NoiseModelling is used (Optional)</li>' +
              '</ul>' +
              '<hr>' +
              'The railway platform geometry (section widths and platform height) is read from the ' +
              '<b>PLATFORM</b> column of the input railways table and resolved against the railway platform data file. ' +
              'In the schema below, orange points will be inserted into the DEM. d2, d3 and d4 (the section half-widths) are taken from the platform definition ' +
              '(d2_0, d3_0 and d4_0 respectively), and d4 can be overridden by the <b>railWidth</b> column when it is present in the railways table. ' +
              'The platform height used to lower the inserted points is the platform h2 (hRail) value:' +
              '<ul>' +
                '<li>d2 = d2_0</li>' +
                '<li>d3 = d3_0</li>' +
                '<li>d4 = d4_0, or railWidth if the railWidth column is present</li>' +
                '<li>hRail = h2</li>' +
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
        inputDEM : [
                name       : 'Input DEM',
                title      : 'Input DEM table',
                description: 'Name of the input DEM table to be enriched',
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
        simplifyTolerance : [
                name       : 'Simplify tolerance',
                title      : 'Simplify tolerance',
                description: 'Distance (in meters) used by the ST_Simplify function to simplify the rail geometries before computing the buffers.',
                default    : 0.5,
                min        : 0, max: 1,
                type       : double.class
        ],
        densifyDistance : [
                name       : 'Densify distance',
                title      : 'Densify distance',
                description: 'Distance (in meters) between consecutive points created by the ST_Densify function on the buffered rail geometries.',
                default    : 5,
                min        : 0, max: 1,
                type       : double.class
        ],
        outputsuffix : [
                name       : 'Output suffix',
                title      : 'Output suffix',
                description: 'Suffix applied at the end of the resulting table name </br> </br>'+
                             '&#128736; If not specified, "ENRICHED" is applied',
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

    DBTypes dbType = DBUtils.getDBType(connection)
    // output string, the information given back to the user
    String resultString = null

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
    
    String inputDEM = input["inputDEM"]
    //String enrichedDEM = input["inputDEM"] += "_ENRICHED"
    String inputRail = input["inputRail"]

    List<String> railColumns = JDBCUtilities.getColumnNames(connection, TableLocation.parse(inputRail, dbType))

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

    // Load the platform definitions (section widths, platform heights) from the data file
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
        checkPlatforms(connection, platformColumn, inputRail, platforms)
        platformPredicate = "p.idplatform = UPPER(a.$platformColumn)"
    }

    // Simplify tolerance for rail geometries before computing the buffers. Default value is 0.5m
    double simplifyTolerance = input.getOrDefault("simplifyTolerance", 0.5) as Double

    // Densify distance between consecutive points created on the buffered rail geometries. Default value is 5m
    double densifyDistance = input.getOrDefault("densifyDistance", 5) as Double

    // If no SRID provided, the one from DEM layer is applied
    Integer srid = 0
    if ('inputSRID' in input) {
        srid = input["inputSRID"] as Integer
    }
    else {
        srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(inputDEM, dbType))
    }

    // If no output table name (outputsuffix) provided, ENRICHED is applied
    String outputsuffix = 'ENRICHED'
    if ('outputsuffix' in input) {
        outputsuffix = input["outputsuffix"] as String
    }
    String enrichedDEM = input["inputDEM"] + "_" + outputsuffix

    // print to command window
    logger.info('List of the input parameters:')
    logger.info('--------------------------------------------')
    logger.info('# SRID: ' + srid)
    logger.info('# DEM table: ' + inputDEM)
    logger.info('# Railways network table: ' + inputRail)
    logger.info('# Railways width column: ' + (railWidth != null ? railWidth : 'not provided'))
    logger.info('# Railways platform column: ' + platformColumn)
    logger.info('# Railway platform data file: ' + railwayPlatformDataFile)
    logger.info('# Simplify tolerance: ' + simplifyTolerance)
    logger.info('# Densify distance: ' + densifyDistance)
    logger.info('# Output suffix: ' + outputsuffix)
    logger.info('--------------------------------------------')

    logger.info('Start enrich the DEM')

    // d4 expression: when the railWidth column is present, its value overrides the platform total width (d4)
    String d4Expr = hasRailWidth ? "a.$railWidth" : "p.d4"

    def initPlatform = """
    -- Initialize the rail platform table

    DROP TABLE IF EXISTS PLATEFORM;
    CREATE TABLE PLATEFORM (idPlatform varchar Primary Key, d1 float, d2 float, d3 float, d4 float, g1 float, g2 float, g3 float, h1 float, h2 float);

    $platformInsert
    -- Rail platform: layer PLATEFORM imported
    """

    def import_dem = """
    ------------
    -- Import DEM

    DROP TABLE IF EXISTS dem_to_enrich;
    CREATE TABLE dem_to_enrich AS SELECT THE_GEOM, 'DEM' as SOURCE FROM $inputDEM;
    
    -- DEM: layer $inputDEM imported
    """

    // d4 expression: when the railWidth column is present, its value overrides the platform total width (d4)
    def import_rail = """
    ------------
    -- Import railways (remove geometries without Z data)

    DROP TABLE IF EXISTS dem_rail;    
    CREATE TABLE dem_rail AS SELECT ST_LineMerge(ST_Accum(a.THE_GEOM)) THE_GEOM, 'RAIL' as SOURCE, p.d2 as D2, p.d3 as D3, $d4Expr as D4, p.h2 as HRAIL
        FROM $inputRail a, PLATEFORM p 
        WHERE not (st_zmin(a.THE_GEOM) = 0 AND st_zmax(a.THE_GEOM) = 0) AND $platformPredicate GROUP BY p.d2, p.d3, $d4Expr, p.h2, p.idplatform;
    CREATE SPATIAL INDEX ON dem_rail(THE_GEOM);
    ALTER TABLE dem_rail ADD PK_LINE INT AUTO_INCREMENT NOT NULL;
    ALTER TABLE dem_rail add primary key(PK_LINE);
    
    -- Railways: layer $inputRail imported
    """
    
    def enrich_rail = """
    ------------
    -- Start enrich the DEM in a new layer called $enrichedDEM

    -- Insert rail platform into $enrichedDEM

    DROP TABLE DEM_WITHOUT_PTLINE IF EXISTS;
    CREATE TABLE DEM_WITHOUT_PTLINE(THE_GEOM geometry(POINTZ, $srid), source varchar) AS SELECT st_setsrid(THE_GEOM, $srid), SOURCE FROM dem_to_enrich;
    
    -- Remove DEM points that are less than "D4/2" far FROM rails
    DELETE FROM DEM_WITHOUT_PTLINE WHERE EXISTS (SELECT 1 FROM dem_rail b 
        WHERE ST_EXPAND(DEM_WITHOUT_PTLINE.THE_GEOM, 20) && b.THE_GEOM 
        AND ST_DISTANCE(DEM_WITHOUT_PTLINE.THE_GEOM, b.THE_GEOM)< b.D4/2  LIMIT 1) ;
    
    -- Create buffer points from rails and copy the elevation from the rails to the point
    DROP TABLE IF EXISTS BUFFERED_D2, BUFFERED_D3, BUFFERED_D4;
    -- The buffer sizes correspond to: 
    -- # d2 = D2/2
    CREATE TABLE BUFFERED_D2 AS SELECT ST_ToMultiPoint(ST_Densify(ST_Buffer(ST_Simplify(st_force2D(THE_GEOM), $simplifyTolerance), D2/2, 'endcap=flat join=mitre'), $densifyDistance)) THE_GEOM, PK_LINE FROM dem_rail WHERE st_length(st_simplify(THE_GEOM, $simplifyTolerance)) > 0 ;
    INSERT INTO DEM_WITHOUT_PTLINE(THE_GEOM, SOURCE) SELECT st_setsrid(ST_MakePoint(ST_X(P.THE_GEOM), ST_Y(P.THE_GEOM), ST_Z(ST_ProjectPoint(P.THE_GEOM,L.THE_GEOM))), $srid) THE_GEOM, 'RAI' FROM ST_EXPLODE('BUFFERED_D2') P, dem_rail L WHERE P.PK_LINE = L.PK_LINE;
    
    -- # d3 = D3/2
    CREATE TABLE BUFFERED_D3 AS SELECT ST_ToMultiPoint(ST_Densify(ST_Buffer(ST_Simplify(THE_GEOM, $simplifyTolerance), D3/2, 'endcap=flat join=mitre'), $densifyDistance)) THE_GEOM, PK_LINE FROM dem_rail WHERE st_length(st_simplify(THE_GEOM, $simplifyTolerance)) > 0 ;
    INSERT INTO DEM_WITHOUT_PTLINE(THE_GEOM, SOURCE) SELECT st_setsrid(ST_MakePoint(ST_X(P.THE_GEOM), ST_Y(P.THE_GEOM), ST_Z(ST_ProjectPoint(P.THE_GEOM,L.THE_GEOM))-L.HRAIL), $srid) THE_GEOM, 'RAI' FROM ST_EXPLODE('BUFFERED_D3') P, dem_rail L WHERE P.PK_LINE = L.PK_LINE;

    -- # d4 = D4/2
    CREATE TABLE BUFFERED_D4 AS SELECT ST_ToMultiPoint(ST_Densify(ST_Buffer(ST_Simplify(THE_GEOM, $simplifyTolerance), D4/2, 'endcap=flat join=mitre'), $densifyDistance)) THE_GEOM, PK_LINE FROM dem_rail WHERE st_length(st_simplify(THE_GEOM, $simplifyTolerance)) > 0 ;
    INSERT INTO DEM_WITHOUT_PTLINE(THE_GEOM, SOURCE) SELECT st_setsrid(ST_MakePoint(ST_X(P.THE_GEOM), ST_Y(P.THE_GEOM), ST_Z(ST_ProjectPoint(P.THE_GEOM,L.THE_GEOM))-L.HRAIL), $srid) THE_GEOM, 'RAI' FROM ST_EXPLODE('BUFFERED_D4') P, dem_rail L WHERE P.PK_LINE = L.PK_LINE;
    
    -- $enrichedDEM enriched with railways
    """
    
    def enrich_final = """
    
    DROP TABLE IF EXISTS $enrichedDEM;
    ALTER TABLE DEM_WITHOUT_PTLINE RENAME TO $enrichedDEM;
    -- Create a spatial index on $enrichedDEM
    CREATE SPATIAL INDEX ON $enrichedDEM (THE_GEOM);

    ----------------------------------
    -- Remove non needed tables
    
    DROP TABLE IF EXISTS DEM_RAIL, BUFFERED_D2, BUFFERED_D3, BUFFERED_D4, BUFFERED_PTLINE, dem_to_enrich;

    -- DEM successfully enriched in the table $enrichedDEM
    """

    StringBuilder stringBuilder = new StringBuilder()
    stringBuilder.append(initPlatform)
    stringBuilder.append(import_dem)
    stringBuilder.append(import_rail)
    stringBuilder.append(enrich_rail)
    stringBuilder.append(enrich_final)

    DataBaseUtilities.parseScript(stringBuilder.toString(), connection, progress, logger)

    return [result: enrichedDEM]

}

static checkPlatforms(Connection connection, String platformColumn, String inputRail, Map<String, RailwayPlatform> platforms) {
// Validate that every non-empty platform value in the railways table is a known platform
    Sql validationSql = new Sql(connection)
    List<String> unknownPlatforms = []
    String validationQuery = "SELECT DISTINCT CAST($platformColumn AS VARCHAR) AS PLATFORM_NAME FROM $inputRail WHERE $platformColumn IS NOT NULL"
    validationSql.eachRow(validationQuery) { row ->
        String name = row.PLATFORM_NAME
        if (name != null && !name.trim().isEmpty() && !platforms.containsKey(name.toUpperCase())) {
            unknownPlatforms.add(name)
        }
    }
    if (!unknownPlatforms.isEmpty()) {
        throw new IllegalArgumentException("Unknown platform(s) " + unknownPlatforms +
                " in column $platformColumn of table $inputRail. Available platforms: " + platforms.keySet())
    }
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