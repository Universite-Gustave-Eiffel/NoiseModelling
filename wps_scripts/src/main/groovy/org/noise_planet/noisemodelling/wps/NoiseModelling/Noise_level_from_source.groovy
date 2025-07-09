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
 * @Author Hesry Quentin, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.transform.CompileStatic
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
import java.util.concurrent.TimeUnit

title = 'Computes the propagation from the sounds sources to the receivers'
description = '&#10145;&#65039; Computes the propagation from the sounds sources to the receivers location using the noise emission table.' +
        '<hr>' +
        '&#127757; Tables must be projected in a metric coordinate system (SRID). Use "Change_SRID" WPS Block if needed. </br></br>' +
        '&#x2705; The output table are called: <b> RECEIVERS_LEVEL </b> </br></br>' +
        'The output table contain: </br> <ul>' +
        '<li><b> IDRECEIVER</b>: receiver an identifier (INTEGER) linked to RECEIVERS table primary key</li>' +
        '<li><b> IDSOURCE</b>: source identifier (INTEGER) linked to SOURCES_GEOM primary key. Only if Keep source id is checked.</li>' +
        '<li><b> PERIOD </b>: Time period (VARCHAR) ex. L D E and DEN. Only if you provide emission power to sources or the atmospheric settings table.</li>' +
        '<li><b> THE_GEOM </b>: the 3D geometry of the receivers with the Z as the altitude (POINTZ)</li>' +
        '<li><b> Hz63, Hz125, Hz250, Hz500, Hz1000,Hz2000, Hz4000, Hz8000 </b>: 8 columns giving the sound level for each octave band (FLOAT)</li></ul>'

inputs = [
        tableBuilding           : [
                name       : 'Buildings table name',
                title      : 'Buildings table name',
                description: '&#127968; Name of the Buildings table</br> </br>' +
                        'The table must contain: </br><ul>' +
                        '<li><b> THE_GEOM </b>: the 2D geometry of the building (POLYGON or MULTIPOLYGON)</li>' +
                        '<li><b> HEIGHT </b>: the height of the building (FLOAT)</li>' +
                        '<li><b> G </b>: Optional, Wall absorption value if g is [0, 1] or wall surface impedance' +
                        ' ([N.s.m-4] static air flow resistivity of material) if G is [20, 20000]' +
                        ' (default is 0.1 if the column G does not exists) (FLOAT)</li></ul>',
                type       : String.class
        ],
        tableSources            : [
                name       : 'Sources geometry table name',
                title      : 'Sources geometry table name',
                description: 'Name of the Sources table (if only geometry is specified) </br> </br>' +
                        'The table must contain (* mandatory): </br> <ul>' +
                        '<li> <b> PK *</b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY) </li> ' +
                        '<li> <b> THE_GEOM *</b> : the 3D geometry of the sources (POINT, MULTIPOINT, LINESTRING, MULTILINESTRING). According to CNOSSOS-EU, you need to set a height of 0.05 m for a road traffic emission </li> ' +
                        '<li> <b> LWD63, LWD125, LWD250, LWD500, LWD1000, LWD2000, LWD4000, LWD8000 </b> : 8 columns giving the day emission sound level for each octave band (FLOAT) </li> ' +
                        '<li> <b> LWE </b> : 8 columns giving the evening emission sound level for each octave band (FLOAT) </li> ' +
                        '<li> <b> LWN </b> : 8 columns giving the night emission sound level for each octave band (FLOAT) </li> ' +
                        '<li> <b> YAW </b> : Source horizontal orientation in degrees. For points 0&#176; North, 90&#176; East. For lines 0&#176; line direction, 90&#176; right of the line direction.  (FLOAT) </li> ' +
                        '<li> <b> PITCH </b> : Source vertical orientation in degrees. 0&#176; front, 90&#176; top, -90&#176; bottom. (FLOAT) </li> ' +
                        '<li> <b> ROLL </b> : Source roll in degrees (FLOAT) </li> ' +
                        '<li> <b> DIR_ID </b> : identifier of the directivity sphere from tableSourceDirectivity parameter or train directivity if not provided -> OMNIDIRECTIONAL(0), ROLLING(1), TRACTIONA(2), TRACTIONB(3), AERODYNAMICA(4), AERODYNAMICB(5), BRIDGE(6) (INTEGER) </li> </ul> ' +
                        '&#128161; This table can be generated from the WPS Block "Road_Emission_from_Traffic"',
                type       : String.class
        ],
        tableSourcesEmission            : [
                name       : 'Sources emission table name',
                title      : 'Sources emission table name',
                description: 'Name of the Sources table (ex. SOURCES_EMISSION) </br> </br>' +
                        'The table must contain: </br> <ul>' +
                        '<li><b> IDSOURCE </b>* : an identifier. It shall be linked to the primary key of tableRoads (INTEGER)</li>' +
                        '<li><b> PERIOD </b>* : Time period, you will find this column on the output (VARCHAR)</li>' +
                        '<li> <b> LW63, LW125, LW250, LW500, LW1000, LW2000, LW4000, LW8000 </b> : Emission noise level in dB can be third-octave 50Hz to 10000Hz (FLOAT) </li> ',
                min        : 0, max: 1, type: String.class
        ],
        tableReceivers          : [
                name       : 'Receivers table name',
                title      : 'Receivers table name',
                description: 'Name of the Receivers table </br> </br>' +
                        'The table must contain: </br> <ul>' +
                        '<li> <b> PK </b> : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY) </li> ' +
                        '<li> <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT) </li> </ul>' +
                        '&#128161; This table can be generated from the WPS Blocks in the "Receivers" folder',
                type       : String.class
        ],
        tableDEM                : [
                name       : 'DEM table name',
                title      : 'DEM table name',
                description: 'Name of the Digital Elevation Model (DEM) table </br> </br>' +
                        'The table must contain: </br> <ul>' +
                        '<li> <b> THE_GEOM </b> : the 3D geometry of the sources (POINT, MULTIPOINT) </li> </ul>' +
                        '&#128161; This table can be generated from the WPS Block "Import_Asc_File"',
                min        : 0, max: 1, type: String.class
        ],
        tableGroundAbs          : [
                name       : 'Ground absorption table name',
                title      : 'Ground absorption table name',
                description: 'Name of the surface/ground acoustic absorption table </br> </br>' +
                        'The table must contain: </br> <ul>' +
                        '<li> <b> THE_GEOM </b>: the 2D geometry of the sources (POLYGON or MULTIPOLYGON) </li>' +
                        '<li> <b> G </b>: the acoustic absorption of a ground (FLOAT between 0 : very hard and 1 : very soft) </li> </ul> ',
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
        paramWallAlpha          : [
                name       : 'wallAlpha',
                title      : 'Wall absorption coefficient',
                description: 'Wall absorption coefficient (FLOAT) </br> </br>' +
                        'This coefficient is going <br> <ul>' +
                        '<li> from 0 : fully absorbent </li>' +
                        '<li> to strictly less than 1 : fully reflective. </li> </ul>' +
                        '&#128736; Default value: <b>0.1 </b> ',
                min        : 0, max: 1, type: String.class
        ],
        confReflOrder           : [
                name       : 'Order of reflexion',
                title      : 'Order of reflexion',
                description: 'Maximum number of reflections to be taken into account (INTEGER). </br> </br>' +
                        '&#x1F6A8; Adding 1 order of reflexion can significantly increase the processing time. </br> </br>' +
                        '&#128736; Default value: <b>1 </b>',
                min        : 0, max: 1, type: String.class
        ],
        confMaxSrcDist          : [
                name       : 'Maximum source-receiver distance',
                title      : 'Maximum source-receiver distance',
                description: 'Maximum distance between source and receiver (FLOAT, in meters). </br> </br>' +
                        '&#128736; Default value: <b>150 </b>',
                min        : 0, max: 1, type: String.class
        ],
        confMaxReflDist         : [
                name       : 'Maximum source-reflexion distance',
                title      : 'Maximum source-reflexion distance',
                description: 'Maximum reflection distance from the source (FLOAT, in meters). </br> </br>' +
                        '&#128736; Default value: <b>50 </b>',
                min        : 0, max: 1, type: String.class
        ],
        confThreadNumber        : [
                name       : 'Thread number',
                title      : 'Thread number',
                description: 'Number of thread to use on the computer (INTEGER). </br> </br>' +
                        'To set this value, look at the number of cores you have. </br>' +
                        'If it is set to 0, use the maximum number of cores available.</br> </br>' +
                        '&#128736; Default value: <b>0 </b>',
                min        : 0, max: 1, type: String.class
        ],
        confDiffVertical        : [
                name       : 'Diffraction on vertical edges',
                title      : 'Diffraction on vertical edges',
                description: 'Compute or not the diffraction on vertical edges. Following Directive 2015/996, enable this option for rail and industrial sources only. </br> </br>' +
                        '&#128736; Default value: <b>false </b>',
                min        : 0, max: 1, type: Boolean.class
        ],
        confDiffHorizontal      : [
                name       : 'Diffraction on horizontal edges',
                title      : 'Diffraction on horizontal edges',
                description: 'Compute or not the diffraction on horizontal edges. </br> </br>' +
                        '&#128736; Default value: <b>false </b>',
                min        : 0, max: 1, type: Boolean.class
        ],
        confExportSourceId      : [
                name       : 'Keep source id',
                title      : 'Separate receiver level by source identifier',
                description: 'Keep source identifier in output in order to get noise contribution of each noise source. </br> </br>' +
                        '&#128736; Default value: <b>false </b>',
                min        : 0, max: 1,
                type       : Boolean.class
        ],
        confHumidity            : [
                name       : 'Relative humidity',
                title      : 'Relative humidity',
                description: '&#127783; Humidity for noise propagation. </br> </br>' +
                        '&#128736; Default value: <b>70</b>',
                min        : 0, max: 1,
                type       : Double.class
        ],
        confTemperature         : [
                name       : 'Temperature',
                title      : 'Air temperature',
                description: '&#127777; Air temperature in degree celsius </br> </br>' +
                        '&#128736; Default value: <b> 15</b>',
                min        : 0, max: 1,
                type       : Double.class
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
                min        : 0, max: 1, type: String.class
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
@CompileStatic
def exec(Connection connection, Map input) {
    long startCompute = System.currentTimeMillis()

    DBTypes dbType = DBUtils.getDBType(connection.unwrap(Connection.class))

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    sql.execute("DROP TABLE RECEIVERS_LEVEL IF EXISTS;")
    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Level from Emission')
    logger.info("inputs {}", input) // log inputs of the run


    // -------------------
    // Get every inputs
    // -------------------

    String sources_table_name = input['tableSources']
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

    if (input['tableSourcesEmission']) {
        // Use the right default database caps according to db type
        String tableSourcesEmission = TableLocation.capsIdentifier(input['tableSourcesEmission'] as String, dbType)
        pointNoiseMap.setSourcesEmissionTableName(tableSourcesEmission)
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

    if (input['tableSourceEmission']) {
        // Use the right default database caps according to db type
        String tableSourceEmission = TableLocation.capsIdentifier(input['tableSourceEmission'] as String, dbType)
        pointNoiseMap.setSourcesEmissionTableName(tableSourceEmission)
    }

    sql.execute("drop table if exists " + TableLocation.parse(pointNoiseMap.noiseMapDatabaseParameters.receiversLevelTable))

    if (input['confRaysName'] && !((input['confRaysName'] as String).isEmpty())) {
        parameters.setRaysTable(input['confRaysName'] as String)
        parameters.setExportRaysMethod(NoiseMapDatabaseParameters.ExportRaysMethods.TO_RAYS_TABLE)
        parameters.exportAttenuationMatrix = true
        parameters.exportCnossosPathWithAttenuation = true
        parameters.keepAbsorption = true
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

    long elapsed = System.currentTimeMillis() - startCompute;
    long hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    elapsed -= TimeUnit.HOURS.toMillis(hours)
    long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    elapsed -= TimeUnit.MINUTES.toMillis(minutes)
    long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed)
    String timeString = String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    logger.info( "Calculation Done in $timeString ! ")

    return "Calculation Done ! The table $pointNoiseMap.noiseMapDatabaseParameters.receiversLevelTable have been created."
}
