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
package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.cts.crs.CRSException
import org.cts.op.CoordinateOperationException
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.jdbc.LDENConfig
import org.noise_planet.noisemodelling.jdbc.LDENPointNoiseMapFactory
import org.noise_planet.noisemodelling.jdbc.PointNoiseMap
import org.noise_planet.noisemodelling.pathfinder.IComputeRaysOut
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor
import org.noise_planet.noisemodelling.pathfinder.utils.*
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.xml.stream.XMLStreamException
import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDateTime

title = 'Compute LDay,Levening,LNight,Lden from road traffic'
description = '&#10145;&#65039; Computes Lden, LDay, LEvening, LNight noise map from Day Evening Night traffic flow rate and speed estimates (specific format, see input details).' +
              '<hr>' +
              '&#127757; Tables must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed.</br> </br>' +
              '&#x2705; The output table are called: <b> LDEN_GEOM, LDAY_GEOM, LEVENING_GEOM, LNIGHT_GEOM </b> </br></br>' +
              'These tables contain: </br> <ul>' +
              '<li><b> IDRECEIVER</b>: an identifier (INTEGER, PRIMARY KEY)</li>' +
              '<li><b> THE_GEOM </b>: the 3D geometry of the receivers (POINT)</li>' +
              '<li><b> Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000 </b>: 8 columns giving the day (evening, night or den) emission sound level for each octave band (FLOAT)</li> </ul>'

inputs = [
        tableBuilding           : [
                name       : 'Buildings table name',
                title      : 'Buildings table name',
                description: '&#127968; Name of the Buildings table </br> </br>' +
                             'The table must contain: </br> <ul>' +
                             '<li> <b> THE_GEOM </b>: the 2D geometry of the building (POLYGON or MULTIPOLYGON) </li>' +
                             '<li> <b> HEIGHT </b>: the height of the building (FLOAT)</li> </ul>',
                type       : String.class
        ],
        tableRoads              : [
                name       : 'Roads table name',
                title      : 'Roads table name',
                description: '&#128739; Name of the Roads table </br> </br>' +
                             'This function recognize the following columns (* mandatory): </br> <ul>' +
                             '<li><b> PK </b>* : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)</li>' +
                             '<li><b> LV_D </b><b>TV_E </b><b> TV_N </b> : Hourly average light vehicle count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> MV_D </b><b>MV_E </b><b>MV_N </b> : Hourly average medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> HGV_D </b><b> HGV_E </b><b> HGV_N </b> :  Hourly average heavy duty vehicles, touring cars, buses, with three or more axles (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> WAV_D </b><b> WAV_E </b><b> WAV_N </b> :  Hourly average mopeds, tricycles or quads &le; 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> WBV_D </b><b> WBV_E </b><b> WBV_N </b> :  Hourly average motorcycles, tricycles or quads > 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> LV_SPD_D </b><b> LV_SPD_E </b><b>LV_SPD_N </b> :  Hourly average light vehicle speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> MV_SPD_D </b><b> MV_SPD_E </b><b>MV_SPD_N </b> :  Hourly average medium heavy vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> HGV_SPD_D </b><b> HGV_SPD_E </b><b> HGV_SPD_N </b> :  Hourly average heavy duty vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> WAV_SPD_D </b><b> WAV_SPD_E </b><b> WAV_SPD_N </b> :  Hourly average mopeds, tricycles or quads &le; 50 cc speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> WBV_SPD_D </b><b> WBV_SPD_E </b><b> WBV_SPD_N </b> :  Hourly average motorcycles, tricycles or quads > 50 cc speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>' +
                             '<li><b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05)(default NL08) (VARCHAR)</li>' +
                             '<li><b> TEMP_D </b><b> TEMP_E </b><b> TEMP_N </b> : Average day, evening, night temperature (default 20&#x2103;) (6-18h)(18-22h)(22-6h)(DOUBLE)</li>' +
                             '<li><b> TS_STUD </b> : A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres (0-12) (DOUBLE)</li>' +
                             '<li><b> PM_STUD </b> : Average proportion of vehicles equipped with studded tyres during TS_STUD period (0-1) (DOUBLE)</li>' +
                             '<li><b> JUNC_DIST </b> : Distance to junction in meters (DOUBLE)</li>' +
                             '<li><b> JUNC_TYPE </b> : Type of junction (k=0 none, k = 1 for a crossing with traffic lights ; k = 2 for a roundabout) (INTEGER)</li>' +
                             '<li><b> SLOPE </b> : Slope (in %) of the road section. If the field is not filled in, the LINESTRING z-values will be used to calculate the slope and the traffic direction (way field) will be force to 3 (bidirectional). (DOUBLE)</li>' +
                             '<li><b> WAY </b> : Define the way of the road section. 1 = one way road section and the traffic goes in the same way that the slope definition you have used, 2 = one way road section and the traffic goes in the inverse way that the slope definition you have used, 3 = bi-directional traffic flow, the flow is split into two components and correct half for uphill and half for downhill (INTEGER)</li>' +
                             '</ul></br>'+
                             '&#128161; This table can be generated from the WPS Block "Import_OSM"',
                type       : String.class
        ],
        tableReceivers          : [
                name       : 'Receivers table name',
                title      : 'Receivers table name',
                description: 'Name of the Receivers table </br> </br>' +
                             'The table must contain: </br> <ul>' +
                             '<li><b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY) </li> ' +
                             '<li><b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT) </li> </ul>' +
                             '&#128161; This table can be generated from the WPS Blocks in the "Receivers" folder',
                type       : String.class
        ],
        tableDEM                : [
                name       : 'DEM table name',
                title      : 'DEM table name',
                description: 'Name of the Digital Elevation Model (DEM) table </br> </br>' +
                             'The table must contain: </br> <ul>' +
                             '<li><b> THE_GEOM </b>: the 3D geometry of the sources (POINT, MULTIPOINT).</li> </ul>' +
                             '&#128161; This table can be generated from the WPS Block "Import_Asc_File"',
                min        : 0, max: 1,
                type       : String.class
        ],
        tableGroundAbs          : [
                name       : 'Ground absorption table name',
                title      : 'Ground absorption table name',
                description: 'Name of the surface/ground acoustic absorption table </br> </br>' +
                             'The table must contain: </br> <ul>' +
                             '<li> <b> THE_GEOM </b>: the 2D geometry of the sources (POLYGON or MULTIPOLYGON)</li>' +
                             '<li> <b> G </b>: the acoustic absorption of a ground (FLOAT between 0 : very hard and 1 : very soft)</li> </ul>',
                min        : 0, max: 1,
                type       : String.class
        ],
        paramWallAlpha          : [
                name       : 'wallAlpha',
                title      : 'Wall absorption coefficient',
                description: 'Wall absorption coefficient (FLOAT) </br> </br>' +
                             'This coefficient is going <br> <ul>' +
                             '<li> from 0 : fully absorbent </li>' +
                             '<li> to strictly less than 1 : fully reflective. </li> </ul>' +
                             '&#128736; Default value: <b>0.1 </b> ',
                min        : 0, max: 1,
                type       : String.class
        ],
        confReflOrder           : [
                name       : 'Order of reflexion',
                title      : 'Order of reflexion',
                description: 'Maximum number of reflections to be taken into account (INTEGER). </br> </br>' +
                             '&#x1F6A8; Adding 1 order of reflexion can significantly increase the processing time. </br> </br>' +
                             '&#128736; Default value: <b>1 </b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confMaxSrcDist          : [
                name       : 'Maximum source-receiver distance',
                title      : 'Maximum source-receiver distance',
                description: 'Maximum distance between source and receiver (FLOAT, in meters). </br> </br>' +
                             '&#128736; Default value: <b>150 </b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confMaxReflDist         : [
                name       : 'Maximum source-reflexion distance',
                title      : 'Maximum source-reflexion distance',
                description: 'Maximum reflection distance from the source (FLOAT, in meters). </br> </br>' +
                             '&#128736; Default value: <b>50 </b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confThreadNumber        : [
                name       : 'Thread number',
                title      : 'Thread number',
                description: 'Number of thread to use on the computer (INTEGER). </br> </br>' +
                             'To set this value, look at the number of cores you have. </br>' +
                             'If it is set to 0, use the maximum number of cores available.</br> </br>' +
                             '&#128736; Default value: <b>0 </b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confDiffVertical        : [
                name       : 'Diffraction on vertical edges',
                title      : 'Diffraction on vertical edges',
                description: 'Compute or not the diffraction on vertical edges. Following Directive 2015/996, enable this option for rail and industrial sources only. </br> </br>' +
                             '&#128736; Default value: <b>false </b>',
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        confDiffHorizontal      : [
                name       : 'Diffraction on horizontal edges',
                title      : 'Diffraction on horizontal edges',
                description: 'Compute or not the diffraction on horizontal edges. </br> </br>' +
                             '&#128736; Default value: <b>false </b>',
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        confSkipLday            : [
                name       : 'Skip LDAY_GEOM table',
                title      : 'Do not compute LDAY_GEOM table',
                description: 'Skip the creation of this table. </br> </br>' +
                             '&#128736; Default value: <b>false </b>',
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        confSkipLevening        :
                [name       : 'Skip LEVENING_GEOM table',
                 title      : 'Do not compute LEVENING_GEOM table',
                 description: 'Skip the creation of this table. </br> </br> ' +
                              '&#128736; Default value: <b>false </b>',
                 min        : 0, max: 1, 
                 type: Boolean.class
                ],
        confSkipLnight          : [
                name       : 'Skip LNIGHT_GEOM table',
                title      : 'Do not compute LNIGHT_GEOM table',
                description: 'Skip the creation of this table. </br> </br>' +
                             '&#128736; Default value: <b>false </b>',
                min        : 0, max: 1, type: Boolean.class
        ],
        confSkipLden            : [
                name       : 'Skip LDEN_GEOM table',
                title      : 'Do not compute LDEN_GEOM table',
                description: 'Skip the creation of this table. </br> </br>' +
                             '&#128736; Default value : <b> false </b>',
                min        : 0, max: 1, 
                type: Boolean.class
        ],
        confExportSourceId      : [
                name       : 'keep source id',
                title      : 'Separate receiver level by source identifier',
                description: 'Keep source identifier in output in order to get noise contribution of each noise source. </br> </br>' +
                             '&#128736; Default value: <b> false </b>',
                min        : 0, max: 1, 
                type: Boolean.class
        ],
        confHumidity            : [
                name       : 'Relative humidity',
                title      : 'Relative humidity',
                description: '&#127783; Humidity for noise propagation. </br> </br>' +
                             '&#128736; Default value: <b> 70</b>',
                min        : 0, max: 1, 
                type: Double.class
        ],
        confTemperature         : [
                name       : 'Temperature',
                title      : 'Air temperature',
                description: '&#127777; Air temperature in degree celsius. </br> </br>' + 
                             '&#128736; Default value: <b> 15</b>',
                min        : 0, max: 1, 
                type: Double.class
        ],
        confFavorableOccurrencesDay: [
                name       : 'Probability of occurrences (Day)',
                title      : 'Probability of occurrences (Day)',
                description: 'Comma-delimited string containing the probability of occurrences of favourable propagation conditions. </br> </br>' +
                             'The north slice is the last array index not the first one <br/>' +
                             'Slice width are 22.5&#176;: (16 slices)<br/><ul>' +
                             '<li>The first column 22.5&#176; contain occurrences between 11.25 to 33.75 &#176;</li>' +
                             '<li>The last column 360&#176; contains occurrences between 348.75&#176; to 360&#176; and 0 to 11.25&#176;</li></ul>' +
                             '&#128736; Default value: <b>0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5</b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confFavorableOccurrencesEvening: [
                name       : 'Probability of occurrences (Evening)',
                title      : 'Probability of occurrences (Evening)',
                description: 'Comma-delimited string containing the probability of occurrences of favourable propagation conditions. </br> </br>' +
                             'The north slice is the last array index not the first one <br/>' +
                             'Slice width are 22.5&#176;: (16 slices)<br/><ul>' +
                             '<li>The first column 22.5&#176; contain occurrences between 11.25 to 33.75 &#176;</li>' +
                             '<li>The last column 360&#176; contains occurrences between 348.75&#176; to 360&#176; and 0 to 11.25&#176;</li></ul>' +
                             '&#128736; Default value: <b>0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5</b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confFavorableOccurrencesNight: [
                name       : 'Probability of occurrences (Night)',
                title      : 'Probability of occurrences (Night)',
                description: 'Comma-delimited string containing the probability of occurrences of favourable propagation conditions. </br> </br>' +
                             'The north slice is the last array index not the first one <br/>' +
                             'Slice width are 22.5&#176;: (16 slices)<br/><ul>' +
                             '<li>The first column 22.5&#176; contain occurrences between 11.25 to 33.75 &#176;</li>' +
                             '<li>The last column 360&#176; contains occurrences between 348.75&#176; to 360&#176; and 0 to 11.25&#176;</li></ul>' +
                             '&#128736; Default value: <b>0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5</b>',
                min        : 0, max: 1,
                type       : String.class
        ],
        confRaysName            : [
                name       : '',
                title      : 'Export scene',
                description: 'Save each mnt, buildings and propagation rays into the specified table (ex:RAYS) or file URL (ex: file:///Z:/dir/map.kml) </br> </br>' +
                             'You can set a table name here in order to save all the rays computed by NoiseModelling. </br> </br>' +
                             'The number of rays has been limited in this script in order to avoid memory exception. </br> </br>' +
                             '&#128736; Default value: <b>empty (do not keep rays)</b>',
                min        : 0, max: 1, 
                type: String.class
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

def forgeCreateTable(Sql sql, String tableName, LDENConfig ldenConfig, String geomField, String tableReceiver, String tableResult) {
    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    StringBuilder sb = new StringBuilder("create table ");
    sb.append(tableName);
    if (!ldenConfig.mergeSources) {
        sb.append(" (IDRECEIVER bigint NOT NULL");
        sb.append(", IDSOURCE bigint NOT NULL");
    } else {
        sb.append(" (IDRECEIVER bigint NOT NULL");
    }
    sb.append(", THE_GEOM geometry")
    PropagationProcessPathData pathData = ldenConfig.getPropagationProcessPathData(LDENConfig.TIME_PERIOD.DAY);
    for (int idfreq = 0; idfreq < pathData.freq_lvl.size(); idfreq++) {
        sb.append(", HZ");
        sb.append(pathData.freq_lvl.get(idfreq));
        sb.append(" REAL");
    }
    sb.append(", LAEQ REAL, LEQ REAL ) AS SELECT PK");
    if (!ldenConfig.mergeSources) {
        sb.append(", IDSOURCE");
    }
    sb.append(", ")
    sb.append(geomField)
    for (int idfreq = 0; idfreq < pathData.freq_lvl.size(); idfreq++) {
        sb.append(", HZ");
        sb.append(pathData.freq_lvl.get(idfreq));
    }
    sb.append(", LAEQ, LEQ FROM ")
    sb.append(tableReceiver)
    if (!ldenConfig.mergeSources) {
        // idsource can't be null so we can't left join
        sb.append(" a, ")
        sb.append(tableResult)
        sb.append(" b WHERE a.PK = b.IDRECEIVER")
    } else {
        sb.append(" a LEFT JOIN ")
        sb.append(tableResult)
        sb.append(" b ON a.PK = b.IDRECEIVER")
    }
    sql.execute(sb.toString())
    // apply pk
    logger.info("Add primary key on " + tableName)
    if (!ldenConfig.mergeSources) {
        sql.execute("ALTER TABLE " + tableName + " ADD PRIMARY KEY(IDRECEIVER, IDSOURCE)")
    } else {
        sql.execute("ALTER TABLE " + tableName + " ADD PRIMARY KEY(IDRECEIVER)")
    }
}


static void exportScene(String name, ProfileBuilder builder, ComputeRaysOutAttenuation result, int crs) throws IOException {
    try {
        FileOutputStream outData = new FileOutputStream(name);
        KMLDocument kmlDocument = new KMLDocument(outData);
        kmlDocument.setInputCRS("EPSG:" + crs);
        kmlDocument.writeHeader();
        if(builder != null) {
            kmlDocument.writeTopographic(builder.getTriangles(), builder.getVertices());
        }
        if(result != null) {
            kmlDocument.writeRays(result.getPropagationPaths());
        }
        if(builder != null) {
            kmlDocument.writeBuildings(builder);
        }
        kmlDocument.writeFooter();
    } catch (XMLStreamException | CoordinateOperationException | CRSException ex) {
        throw new IOException(ex);
    }
}


// main function of the script
def exec(Connection connection, input) {
    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Noise level from Traffic')
    logger.info("inputs {}", input) // log inputs of the run

    // -------------------
    // Get every inputs
    // -------------------

    String sources_table_name = input['tableRoads']
    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()
    // Check if srid are in metric projection.
    int sridSources = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
    if (sridSources == 3785 || sridSources == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+sources_table_name+".")
    if (sridSources == 0) throw new IllegalArgumentException("Error : The table "+sources_table_name+" does not have an associated SRID.")

    //Get the geometry field of the source table
    TableLocation sourceTableIdentifier = TableLocation.parse(sources_table_name)
    List<String> geomFields = GeometryTableUtilities.getGeometryColumnNames(connection, sourceTableIdentifier)
    if (geomFields.isEmpty()) {
        throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier))
    }

    //Get the primary key field of the source table
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(sources_table_name))
    if (pkIndex < 1) {
        throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", sourceTableIdentifier))
    }

    String receivers_table_name = input['tableReceivers']
    // do it case-insensitive
    receivers_table_name = receivers_table_name.toUpperCase()
    //Get the geometry field of the receiver table
    TableLocation receiverTableIdentifier = TableLocation.parse(receivers_table_name)
    List<String> geomFieldsRcv = GeometryTableUtilities.getGeometryColumnNames(connection, receiverTableIdentifier)
    if (geomFieldsRcv.isEmpty()) {
        throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", receiverTableIdentifier))
    }
    // Check if srid are in metric projection and are all the same.
    int sridReceivers = GeometryTableUtilities.getSRID(connection, TableLocation.parse(receivers_table_name))
    if (sridReceivers == 3785 || sridReceivers == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+receivers_table_name+".")
    if (sridReceivers == 0) throw new IllegalArgumentException("Error : The table "+receivers_table_name+" does not have an associated SRID.")
    if (sridReceivers != sridSources) throw new IllegalArgumentException("Error : The SRID of table "+sources_table_name+" and "+receivers_table_name+" are not the same.")


    //Get the primary key field of the receiver table
    int pkIndexRecv = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(receivers_table_name))
    if (pkIndexRecv < 1) {
        throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", receiverTableIdentifier))
    }

    String building_table_name = input['tableBuilding']
    // do it case-insensitive
    building_table_name = building_table_name.toUpperCase()
    // Check if srid are in metric projection and are all the same.
    int sridBuildings = GeometryTableUtilities.getSRID(connection, TableLocation.parse(building_table_name))
    if (sridBuildings == 3785 || sridReceivers == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+building_table_name+".")
    if (sridBuildings == 0) throw new IllegalArgumentException("Error : The table "+building_table_name+" does not have an associated SRID.")
    if (sridReceivers != sridBuildings) throw new IllegalArgumentException("Error : The SRID of table "+building_table_name+" and "+receivers_table_name+" are not the same.")

    String dem_table_name = ""
    if (input['tableDEM']) {
        dem_table_name = input['tableDEM']
        // do it case-insensitive
        dem_table_name = dem_table_name.toUpperCase()
        // Check if srid are in metric projection and are all the same.
        int sridDEM = GeometryTableUtilities.getSRID(connection, TableLocation.parse(dem_table_name))
        if (sridDEM == 3785 || sridReceivers == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+dem_table_name+".")
        if (sridDEM == 0) throw new IllegalArgumentException("Error : The table "+dem_table_name+" does not have an associated SRID.")
        if (sridDEM != sridSources) throw new IllegalArgumentException("Error : The SRID of table "+sources_table_name+" and "+dem_table_name+" are not the same.")
    }


    String ground_table_name = ""
    if (input['tableGroundAbs']) {
        ground_table_name = input['tableGroundAbs']
        // do it case-insensitive
        ground_table_name = ground_table_name.toUpperCase()
        // Check if srid are in metric projection and are all the same.
        int sridGROUND = GeometryTableUtilities.getSRID(connection, TableLocation.parse(ground_table_name))
        if (sridGROUND == 3785 || sridReceivers == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+ground_table_name+".")
        if (sridGROUND == 0) throw new IllegalArgumentException("Error : The table "+ground_table_name+" does not have an associated SRID.")
        if (sridGROUND != sridSources) throw new IllegalArgumentException("Error : The SRID of table "+ground_table_name+" and "+sources_table_name+" are not the same.")
    }

    int reflexion_order = 0
    if (input['confReflOrder']) {
        reflexion_order = Integer.valueOf(input['confReflOrder'])
    }

    double max_src_dist = 150
    if (input['confMaxSrcDist']) {
        max_src_dist = Double.valueOf(input['confMaxSrcDist'])
    }

    double max_ref_dist = 50
    if (input['confMaxReflDist']) {
        max_ref_dist = Double.valueOf(input['confMaxReflDist'])
    }

    double wall_alpha = 0.1
    if (input['paramWallAlpha']) {
        wall_alpha = Double.valueOf(input['paramWallAlpha'])
    }

    int n_thread = 0
    if (input['confThreadNumber']) {
        n_thread = Integer.valueOf(input['confThreadNumber'])
    }

    boolean compute_vertical_diffraction = false
    if (input['confDiffVertical']) {
        compute_vertical_diffraction = input['confDiffVertical']
    }

    boolean compute_horizontal_diffraction = false
    if (input['confDiffHorizontal']) {
        compute_horizontal_diffraction = input['confDiffHorizontal']
    }

    boolean confSkipLday = false;
    if (input['confSkipLday']) {
        confSkipLday = input['confSkipLday']
    }

    boolean confSkipLevening = false;
    if (input['confSkipLevening']) {
        confSkipLevening = input['confSkipLevening']
    }

    boolean confSkipLnight = false;
    if (input['confSkipLnight']) {
        confSkipLnight = input['confSkipLnight']
    }

    boolean confSkipLden = false;
    if (input['confSkipLden']) {
        confSkipLden = input['confSkipLden']
    }

    boolean confExportSourceId = false;
    if (input['confExportSourceId']) {
        confExportSourceId = input['confExportSourceId']
    }

    // -------------------------
    // Initialize some variables
    // -------------------------

    // Set of already processed receivers
    Set<Long> receivers = new HashSet<>()
    // --------------------------------------------
    // Initialize NoiseModelling propagation part
    // --------------------------------------------

    PointNoiseMap pointNoiseMap = new PointNoiseMap(building_table_name, sources_table_name, receivers_table_name)

    LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW)

    ldenConfig.setComputeLDay(!confSkipLday)
    ldenConfig.setComputeLEvening(!confSkipLevening)
    ldenConfig.setComputeLNight(!confSkipLnight)
    ldenConfig.setComputeLDEN(!confSkipLden)
    ldenConfig.setMergeSources(!confExportSourceId)

    int maximumRaysToExport = 5000

    File folderExportKML = null
    String kmlFileNamePrepend = ""
    if (input['confRaysName'] && !((input['confRaysName'] as String).isEmpty())) {
        String confRaysName = input['confRaysName'] as String
        if(confRaysName.startsWith("file:")) {
            ldenConfig.setExportRaysMethod(LDENConfig.ExportRaysMethods.TO_MEMORY)
            URL url = new URL(confRaysName)
            File urlFile = new File(url.toURI())
            if(urlFile.isDirectory()) {
                folderExportKML = urlFile
            } else {
                folderExportKML = urlFile.getParentFile()
                kmlFileNamePrepend = confRaysName.substring(
                        Math.max(0, confRaysName.lastIndexOf(File.separator) + 1),
                        Math.max(0, confRaysName.lastIndexOf(".")))
            }
        } else {
            ldenConfig.setExportRaysMethod(LDENConfig.ExportRaysMethods.TO_RAYS_TABLE)
            ldenConfig.setRaysTable(input['confRaysName'] as String)
        }
        ldenConfig.setKeepAbsorption(true);
        ldenConfig.setMaximumRaysOutputCount(maximumRaysToExport);
    }

    LDENPointNoiseMapFactory ldenProcessing = new LDENPointNoiseMapFactory(connection, ldenConfig)
    pointNoiseMap.setComputeHorizontalDiffraction(compute_vertical_diffraction)
    pointNoiseMap.setComputeVerticalDiffraction(compute_horizontal_diffraction)
    pointNoiseMap.setSoundReflectionOrder(reflexion_order)


    // Set environmental parameters
    PropagationProcessPathData environmentalDataDay = new PropagationProcessPathData(false)

    if (input.containsKey('confHumidity')) {
        environmentalDataDay.setHumidity(input['confHumidity'] as Double)
    }
    if (input.containsKey('confTemperature')) {
        environmentalDataDay.setTemperature(input['confTemperature'] as Double)
    }

    PropagationProcessPathData environmentalDataEvening = new PropagationProcessPathData(environmentalDataDay)
    PropagationProcessPathData environmentalDataNight = new PropagationProcessPathData(environmentalDataDay)
    if (input.containsKey('confFavorableOccurrencesDay')) {
        StringTokenizer tk = new StringTokenizer(input['confFavorableOccurrencesDay'] as String, ',')
        double[] favOccurrences = new double[PropagationProcessPathData.DEFAULT_WIND_ROSE.length]
        for (int i = 0; i < favOccurrences.length; i++) {
            favOccurrences[i] = Math.max(0, Math.min(1, Double.valueOf(tk.nextToken().trim())))
        }
        environmentalDataDay.setWindRose(favOccurrences)
    }
    if (input.containsKey('confFavorableOccurrencesEvening')) {
        StringTokenizer tk = new StringTokenizer(input['confFavorableOccurrencesEvening'] as String, ',')
        double[] favOccurrences = new double[PropagationProcessPathData.DEFAULT_WIND_ROSE.length]
        for (int i = 0; i < favOccurrences.length; i++) {
            favOccurrences[i] = Math.max(0, Math.min(1, Double.valueOf(tk.nextToken().trim())))
        }
        environmentalDataEvening.setWindRose(favOccurrences)
    }
    if (input.containsKey('confFavorableOccurrencesNight')) {
        StringTokenizer tk = new StringTokenizer(input['confFavorableOccurrencesNight'] as String, ',')
        double[] favOccurrences = new double[PropagationProcessPathData.DEFAULT_WIND_ROSE.length]
        for (int i = 0; i < favOccurrences.length; i++) {
            favOccurrences[i] = Math.max(0, Math.min(1, Double.valueOf(tk.nextToken().trim())))
        }
        environmentalDataNight.setWindRose(favOccurrences)
    }

    pointNoiseMap.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.DAY, environmentalDataDay)
    pointNoiseMap.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.EVENING, environmentalDataEvening)
    pointNoiseMap.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.NIGHT, environmentalDataNight)

    // Building height field name
    pointNoiseMap.setHeightField("HEIGHT")
    // Import table with Snow, Forest, Grass, Pasture field polygons. Attribute G is associated with each polygon
    if (ground_table_name != "") {
        pointNoiseMap.setSoilTableName(ground_table_name)
    }
    // Point cloud height above sea level POINT(X Y Z)
    if (dem_table_name != "") {
        pointNoiseMap.setDemTable(dem_table_name)
    }

    pointNoiseMap.setMaximumPropagationDistance(max_src_dist)
    pointNoiseMap.setMaximumReflectionDistance(max_ref_dist)
    pointNoiseMap.setWallAbsorption(wall_alpha)
    pointNoiseMap.setThreadCount(n_thread)

    // Do not propagate for low emission or far away sources
    // Maximum error in dB
    pointNoiseMap.setMaximumError(0.1d)

    // --------------------------------------------
    // Initialize NoiseModelling emission part
    // --------------------------------------------
    pointNoiseMap.setComputeRaysOutFactory(ldenProcessing)
    pointNoiseMap.setPropagationProcessDataFactory(ldenProcessing)

    // Init Map
    pointNoiseMap.initialize(connection, new EmptyProgressVisitor())

    // --------------------------------------------
    // Run Calculations
    // --------------------------------------------

    // Init ProgressLogger (loading bar)
    RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1)


    logger.info("Start calculation... ")
    LocalDateTime now = LocalDateTime.now();
    ProfilerThread profilerThread = new ProfilerThread(new File(String.format("profile_%d_%d_%d_%dh%d.csv",
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute())));
    profilerThread.addMetric(ldenProcessing);
    profilerThread.addMetric(new ProgressMetric(progressLogger));
    profilerThread.addMetric(new JVMMemoryMetric());
    profilerThread.addMetric(new ReceiverStatsMetric());
    profilerThread.setWriteInterval(300);
    profilerThread.setFlushInterval(300);
    pointNoiseMap.setProfilerThread(profilerThread);
    try {
        ldenProcessing.start()
        new Thread(profilerThread).start();
        // Iterate over computation areas
        int k = 0
        Map cells = pointNoiseMap.searchPopulatedCells(connection)
        ProgressVisitor progressVisitor = progressLogger.subProcess(cells.size())
        new TreeSet<>(cells.keySet()).each { cellIndex ->
            // Run ray propagation
            logger.info(String.format("Compute... %.3f %% (%d receivers in this cell)", 100 * k++ / cells.size(), cells.get(cellIndex)))
            IComputeRaysOut out = pointNoiseMap.evaluateCell(connection, cellIndex.getLatitudeIndex(), cellIndex.getLongitudeIndex(), progressVisitor, receivers)
            // Export as a Google Earth 3d scene
            if (out instanceof ComputeRaysOutAttenuation && folderExportKML != null) {
                ComputeRaysOutAttenuation cellStorage = (ComputeRaysOutAttenuation) out;
                exportScene(new File(folderExportKML.getPath(),
                        String.format(Locale.ROOT, kmlFileNamePrepend + "_%d_%d.kml", cellIndex.getLatitudeIndex(),
                                cellIndex.getLongitudeIndex())).getPath(),
                        cellStorage.inputData.profileBuilder, cellStorage, sridSources)
            }
        }
    } catch(IllegalArgumentException | IllegalStateException ex) {
        System.err.println(ex);
        throw ex;
    } finally {
        profilerThread.stop();
        ldenProcessing.stop()
    }

    // Associate Geometry column to the table LDEN
    StringBuilder createdTables = new StringBuilder()

    if (ldenConfig.computeLDay) {
        sql.execute("drop table if exists LDAY_GEOM;")
        logger.info('create table LDAY_GEOM')
        forgeCreateTable(sql, "LDAY_GEOM", ldenConfig, geomFieldsRcv.get(0), receivers_table_name,
                ldenConfig.lDayTable)
        createdTables.append(" LDAY_GEOM")
        sql.execute("drop table if exists " + TableLocation.parse(ldenConfig.getlDayTable()))
    }
    if (ldenConfig.computeLEvening) {
        sql.execute("drop table if exists LEVENING_GEOM;")
        logger.info('create table LEVENING_GEOM')
        forgeCreateTable(sql, "LEVENING_GEOM", ldenConfig, geomFieldsRcv.get(0), receivers_table_name,
                ldenConfig.lEveningTable)
        createdTables.append(" LEVENING_GEOM")
        sql.execute("drop table if exists " + TableLocation.parse(ldenConfig.getlEveningTable()))
    }
    if (ldenConfig.computeLNight) {
        sql.execute("drop table if exists LNIGHT_GEOM;")
        logger.info('create table LNIGHT_GEOM')
        forgeCreateTable(sql, "LNIGHT_GEOM", ldenConfig, geomFieldsRcv.get(0), receivers_table_name,
                ldenConfig.lNightTable)
        createdTables.append(" LNIGHT_GEOM")
        sql.execute("drop table if exists " + TableLocation.parse(ldenConfig.getlNightTable()))
    }
    if (ldenConfig.computeLDEN) {
        sql.execute("drop table if exists LDEN_GEOM;")
        logger.info('create table LDEN_GEOM')
        forgeCreateTable(sql, "LDEN_GEOM", ldenConfig, geomFieldsRcv.get(0), receivers_table_name,
                ldenConfig.lDenTable)
        createdTables.append(" LDEN_GEOM")
        sql.execute("drop table if exists " + TableLocation.parse(ldenConfig.getlDenTable()))
    }

    resultString = "Calculation Done ! " + createdTables.toString() + " table(s) have been created."

    // print to command window
    logger.info('Result : ' + resultString)
    logger.info('End : LDAY from Traffic')

    // print to WPS Builder
    return resultString


}

