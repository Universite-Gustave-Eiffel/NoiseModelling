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
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.noise_planet.noisemodelling.jdbc.NoiseMapByReceiverMaker
import org.noise_planet.noisemodelling.jdbc.NoiseMapDatabaseParameters
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor
import org.noise_planet.noisemodelling.propagation.AttenuationParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.SQLException
import java.time.LocalDateTime

title = 'Compute noise level directly from road traffic data'
description = '&#10145;&#65039; Computes Noise map from each period from the traffic flow rate and speed estimates' +
        ' (specific format, see input details). <hr>' +
        '&#127757; Tables must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed.</br> </br>' +
        '&#x2705; The output table is <b> RECEIVERS_LEVEL </b> </br></br>' +
        'The output tables contain: </br> <ul>' +
        '<li><b> IDRECEIVER</b>: an identifier (INTEGER, PRIMARY KEY)</li>' +
        '<li><b> IDSOURCE</b>: an identifier of the source (INTEGER) if keepSource is true</li>' +
        '<li><b> THE_GEOM </b>: the 3D geometry of the receivers (POINT)</li>' +
        '<li><b> PERIOD </b>: time period ex. D, E, N, DEN (Varchar)</li>' +
        '<li><b> Lw63, Lw125, Lw250, Lw500, Lw1000, Lw2000, Lw4000, Lw8000, Laeq, Leq</b>: noise level at receiver (REAL)</li> </ul>'

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
                description: '&#128739; Name of the Roads table, traffic can be provided here but are limited to DAY EVENING NIGHT periods </br> </br>' +
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
        tableRoadsTraffic              : [
                name       : 'Roads traffic table name',
                title      : 'Roads traffic table name',
                description: '&#128739; Name of the Roads traffic table per period </br> </br>' +
                        'This function recognize the following columns (* mandatory): </br> <ul>' +
                        '<li><b> IDSOURCE </b>* : an identifier. It shall be linked to the primary key of tableRoads (INTEGER)</li>' +
                        '<li><b> PERIOD </b>* : Time period, you will find this column on the output (VARCHAR)</li>' +
                        '<li><b> LV </b>  : Hourly average light vehicle count (DOUBLE)</li>' +
                        '<li><b> MV </b> : Hourly average medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle count (DOUBLE)</li>' +
                        '<li><b> HGV </b>:  Hourly average heavy duty vehicles, touring cars, buses, with three or more axles (DOUBLE)</li>' +
                        '<li><b> WAV </b>:  Hourly average mopeds, tricycles or quads &le; 50 cc count (DOUBLE)</li>' +
                        '<li><b> WBV </b>:  Hourly average motorcycles, tricycles or quads > 50 cc count (DOUBLE)</li>' +
                        '<li><b> LV_SPD </b> :  Hourly average light vehicle speed (DOUBLE)</li>' +
                        '<li><b> MV_SPD </b> :  Hourly average medium heavy vehicles speed (DOUBLE)</li>' +
                        '<li><b> HGV_SPD </b> :  Hourly average heavy duty vehicles speed (DOUBLE)</li>' +
                        '<li><b> WAV_SPD </b> :  Hourly average mopeds, tricycles or quads &le; 50 cc speed (DOUBLE)</li>' +
                        '<li><b> WBV_SPD </b> :  Hourly average motorcycles, tricycles or quads > 50 cc speed (DOUBLE)</li>' +
                        '<li><b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05)(default NL08) (VARCHAR)</li>' +
                        '<li><b> TS_STUD </b> : A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres (0-12) (DOUBLE)</li>' +
                        '<li><b> PM_STUD </b> : Average proportion of vehicles equipped with studded tyres during TS_STUD period (0-1) (DOUBLE)</li>' +
                        '<li><b> JUNC_DIST </b> : Distance to junction in meters (DOUBLE)</li>' +
                        '<li><b> JUNC_TYPE </b> : Type of junction (k=0 none, k = 1 for a crossing with traffic lights ; k = 2 for a roundabout) (INTEGER)</li>' +
                        '<li><b> SLOPE </b> : Slope (in %) of the road section. If the field is not filled in, the LINESTRING z-values will be used to calculate the slope and the traffic direction (way field) will be force to 3 (bidirectional). (DOUBLE)</li>' +
                        '<li><b> WAY </b> : Define the way of the road section. 1 = one way road section and the traffic goes in the same way that the slope definition you have used, 2 = one way road section and the traffic goes in the inverse way that the slope definition you have used, 3 = bi-directional traffic flow, the flow is split into two components and correct half for uphill and half for downhill (INTEGER)</li>' +
                        '</ul></br>',
                min        : 0, max: 1, type: String.class
        ],
        tableSourceDirectivity          : [
                name       : 'Source directivity table name',
                title      : 'Source directivity table name',
                description: 'Name of the emission directivity table </br> </br>' +
                        'If not specified the default is train directivity of CNOSSOS-EU</b> </br> </br>' +
                        'The table must contain the following columns: </br> <ul>' +
                        '<li> <b> DIR_ID </b>: identifier of the directivity sphere (INTEGER) </li> ' +
                        '<li> <b> THETA </b>: [-90;90] Vertical angle in degree. 0&#176; front 90&#176; top -90&#176; bottom (FLOAT) </li> ' +
                        '<li> <b> PHI </b>: [0;360] Horizontal angle in degree. 0&#176; front 90&#176; right (FLOAT) </li> ' +
                        '<li> <b> LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000 </b>: attenuation levels in dB for each octave or third octave (FLOAT) </li> </ul> ' ,
                min        : 0, max: 1, type: String.class
        ],
        tablePeriodAtmosphericSettings          : [
                name       : 'Atmospheric settings table name for each time period',
                title      : 'Atmospheric settings table name for each time period',
                description: 'Name of the Atmospheric settings table </br> </br>' +
                        'The table must contain the following columns: </br> <ul>' +
                        '<li> <b> PERIOD </b>: time period (VARCHAR PRIMARY KEY) </li> ' +
                        '<li> <b> WINDROSE </b>: probability of occurrences of favourable propagation conditions (ARRAY(16)) </li> ' +
                        '<li> <b> TEMPERATURE </b>: Temperature in celsius (FLOAT) </li> ' +
                        '<li> <b> PRESSURE </b>: air pressure in pascal (FLOAT) </li> ' +
                        '<li> <b> HUMIDITY </b>: air humidity in percentage (FLOAT) </li> ' +
                        '<li> <b> GDISC </b>: choose between accept G discontinuity or not (BOOLEAN) default true </li> ' +
                        '<li> <b> PRIME2520 </b>: choose to use prime values to compute eq. 2.5.20 (BOOLEAN) default false </li> ' +
                        '</ul>' ,
                min        : 0, max: 1, type: String.class
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
                        '&#128736; Default humidity value: <b> 70</b>',
                min        : 0, max: 1,
                type: Double.class
        ],
        confTemperature         : [
                name       : 'Temperature',
                title      : 'Air temperature',
                description: '&#127777; Default Air temperature in degree celsius. </br> </br>' +
                        '&#128736; Default value: <b> 15</b>',
                min        : 0, max: 1,
                type: Double.class
        ],
        confFavorableOccurrencesDefault: [
                name       : 'Probability of occurrences',
                title      : 'Probability of occurrences',
                description: 'Comma-delimited string containing the default probability of occurrences of favourable propagation conditions. </br> </br>' +
                        'The north slice is the last array index not the first one <br/>' +
                        'Slice width are 22.5&#176;: (16 slices)</br> <ul>' +
                        '<li>The first column 22.5&#176; contain occurrences between 11.25 to 33.75 &#176; </li>' +
                        '<li>The last column 360&#176; contains occurrences between 348.75&#176; to 360&#176; and 0 to 11.25&#176; </li> </ul>' +
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
        ],
        confMaxError            : [
                name       : 'Max Error (dB)',
                title      : 'Max Error (dB)',
                description: 'Threshold for excluding negligible sound sources in calculations. Default value: <b>0.1</b>',
                min        : 0, max: 1,
                type       : Double.class
        ],
        frequencyFieldPrepend            : [
                name       : 'Frequency field name',
                title      : 'Frequency field name',
                description: 'Frequency field name prepend. Ex. for 1000 Hz frequency the default column name is HZ1000.' +
                        '&#128736; Default value: <b>HZ</b>',
                min        : 0, max: 1, type: String.class
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
def exec(Connection connection, Map input) {
    int maximumRaysToExport = 5000

    DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class))

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

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

    String tableSourceDirectivity = ""
    if (input['tableSourceDirectivity']) {
        tableSourceDirectivity = input['tableSourceDirectivity']
        // do it case-insensitive
        tableSourceDirectivity = tableSourceDirectivity.toUpperCase()
    }

    boolean recordProfile = false
    if (input['confRecordProfile']) {
        recordProfile = input['confRecordProfile']
    }

    int reflexion_order = 0
    if (input['confReflOrder']) {
        reflexion_order = Integer.valueOf(input['confReflOrder'] as String)
    }

    double max_src_dist = 150
    if (input['confMaxSrcDist']) {
        max_src_dist = Double.valueOf(input['confMaxSrcDist'] as String)
    }

    double max_ref_dist = 50
    if (input['confMaxReflDist']) {
        max_ref_dist = Double.valueOf(input['confMaxReflDist'] as String)
    }

    double wall_alpha = 0.1
    if (input['paramWallAlpha']) {
        wall_alpha = Double.valueOf(input['paramWallAlpha'] as String)
    }

    int n_thread = 0
    if (input['confThreadNumber']) {
        n_thread = Integer.valueOf(input['confThreadNumber'] as String)
    }

    boolean compute_vertical_diffraction = false
    if (input['confDiffVertical']) {
        compute_vertical_diffraction = input['confDiffVertical']
    }

    boolean compute_horizontal_diffraction = false
    if (input['confDiffHorizontal']) {
        compute_horizontal_diffraction = input['confDiffHorizontal']
    }

    boolean confExportSourceId = false
    if (input['confExportSourceId']) {
        confExportSourceId = input['confExportSourceId']
    }

    double confMaxError = 0.1
    if (input['confMaxError']) {
        confMaxError = Double.valueOf(input['confMaxError'] as String)
    }

    String frequencyFieldPrepend = "HZ"
    if (input['frequencyFieldPrepend']) {
        frequencyFieldPrepend = input['frequencyFieldPrepend'] as String
    }

    // --------------------------------------------
    // Initialize NoiseModelling propagation part
    // --------------------------------------------

    NoiseMapByReceiverMaker pointNoiseMap = new NoiseMapByReceiverMaker(building_table_name, sources_table_name, receivers_table_name)

    def parameters = pointNoiseMap.getNoiseMapDatabaseParameters()

    parameters.setMergeSources(!confExportSourceId)
    parameters.exportReceiverPosition = true

    if (input['tableRoadsTraffic']) {
        // Use the right default database caps according to db type
        String tableRoadsTraffic = TableLocation.capsIdentifier(input['tableRoadsTraffic'] as String, dbType)
        pointNoiseMap.setSourcesEmissionTableName(tableRoadsTraffic)
    }

    // add optional discrete directivity table name
    if(tableSourceDirectivity.isEmpty()) {
        // Use train directivity functions instead of discrete directivity
        pointNoiseMap.sceneInputSettings.setUseTrainDirectivity(true)
    } else {
        // Load table into specialized class
        pointNoiseMap.sceneInputSettings.setDirectivityTableName(tableSourceDirectivity)
        logger.info(String.format(Locale.ROOT, "Loaded directivity from %s table", tableSourceDirectivity))
    }

    sql.execute("drop table if exists " + TableLocation.parse(pointNoiseMap.noiseMapDatabaseParameters.receiversLevelTable))

    if (input['confRaysName'] && !((input['confRaysName'] as String).isEmpty())) {
        parameters.setRaysTable(input['confRaysName'] as String)
        parameters.setExportRaysMethod(NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE)
        parameters.setRaysTable(input['confRaysName'] as String)
        parameters.keepAbsorption = true
        parameters.setMaximumRaysOutputCount(maximumRaysToExport)
    }

    pointNoiseMap.setComputeHorizontalDiffraction(compute_vertical_diffraction)
    pointNoiseMap.setComputeVerticalDiffraction(compute_horizontal_diffraction)
    pointNoiseMap.setSoundReflectionOrder(reflexion_order)
    pointNoiseMap.setFrequencyFieldPrepend(frequencyFieldPrepend)


    // Set environmental parameters
    DefaultTableLoader defaultTableLoader = (DefaultTableLoader)pointNoiseMap.tableLoader
    AttenuationParameters environmentalData = defaultTableLoader.defaultParameters

    if (input.containsKey('confFavorableOccurrencesDefault')) {
        StringTokenizer tk = new StringTokenizer(input['confFavorableOccurrencesDefault'] as String, ',')
        double[] favOccurrences = new double[AttenuationParameters.DEFAULT_WIND_ROSE.length]
        for (int i = 0; i < favOccurrences.length; i++) {
            favOccurrences[i] = Math.max(0, Math.min(1, Double.valueOf(tk.nextToken().trim())))
        }
        environmentalData.setWindRose(favOccurrences)
    }
    if (input.containsKey('confHumidity')) {
        environmentalData.setHumidity(input['confHumidity'] as Double)
    }
    if (input.containsKey('confTemperature')) {
        environmentalData.setTemperature(input['confTemperature'] as Double)
    }
    if(input.containsKey("tablePeriodAtmosphericSettings")) {
        pointNoiseMap.getSceneInputSettings().setPeriodAtmosphericSettingsTableName(input.get("tablePeriodAtmosphericSettings") as String)
    }

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


    if(recordProfile) {
        LocalDateTime now = LocalDateTime.now()
        pointNoiseMap.noiseMapDatabaseParameters.CSVProfilerOutputPath = new File(String.format("profile_%d_%d_%d_%dh%d.csv",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute()))
        pointNoiseMap.noiseMapDatabaseParameters.CSVProfilerWriteInterval = 120 // delay write csv line in seconds
    }

    // Do not propagate for low emission or far away sources
    // Maximum error in dB
    parameters.setMaximumError(confMaxError)

    // --------------------------------------------
    // Run Calculations
    // --------------------------------------------

    // Init ProgressLogger (loading bar)
    RootProgressVisitor progressLogger = new RootProgressVisitor(1, true, 1)

    logger.info("Start calculation... ")

    pointNoiseMap.run(connection, progressLogger)

    return "Calculation Done ! The table $pointNoiseMap.noiseMapDatabaseParameters.receiversLevelTable have been created."
}

