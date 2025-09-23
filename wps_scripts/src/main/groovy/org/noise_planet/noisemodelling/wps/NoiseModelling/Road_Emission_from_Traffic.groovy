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
 */

package org.noise_planet.noisemodelling.wps.NoiseModelling

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.Tuple
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.hsqldb.Table
import org.locationtech.jts.geom.Geometry
import org.noise_planet.noisemodelling.jdbc.EmissionTableGenerator
import org.noise_planet.noisemodelling.jdbc.input.DefaultTableLoader
import org.noise_planet.noisemodelling.pathfinder.utils.AcousticIndicatorsFunctions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.stream.Collectors

title = 'Compute road emission noise map from road table.'
description = '&#10145;&#65039; Compute Road Emission Noise Map from Day Evening Night traffic flow rate and speed estimates (specific format, see input details). </br>' +
              '<hr>' +
              '&#x2705; The output table is called: <b>LW_ROADS </b> '

inputs = [
        tableRoads: [
                name       : 'Roads table name',
                title      : 'Roads table name',
                description: "<b>Name of the Roads table.</b>  </br>  " +
                        "<br> If you provide the PERIOD field you do not provide the fields with the extension  _D _E _N." +
                        " This function recognize the following columns (* mandatory) : </br><ul>" +
                        '<li> <b> PK </b> : If there is a primary key defined, it will be copied with the same name and set as a primary for the output table </li> ' +
                        '<li><b> IDSOURCE </b> : an identifier, if present will be copied as is. It is expected if you will use LW_ROADS as SOURCES_EMISSION in the Noise_Level_From_Source script input (INTEGER)</li>' +
                        "<li><b> PERIOD </b> Any text that could be time period ex. D, E, N, DEN (Varchar), if present will be copied as is</li>" +
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
                        "<li><b> LV_D </b><b>LV_E </b><b>LV_N </b> : Hourly average light vehicle count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> MV_D </b><b>MV_E </b><b>MV_N </b> : Hourly average medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> HGV_D </b><b> HGV_E </b><b> HGV_N </b> :  Hourly average heavy duty vehicles, touring cars, buses, with three or more axles (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> WAV_D </b><b> WAV_E </b><b> WAV_N </b> :  Hourly average mopeds, tricycles or quads &le; 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> WBV_D </b><b> WBV_E </b><b> WBV_N </b> :  Hourly average motorcycles, tricycles or quads > 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> LV_SPD_D </b><b> LV_SPD_E </b><b>LV_SPD_N </b> :  Hourly average light vehicle speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> MV_SPD_D </b><b> MV_SPD_E </b><b>MV_SPD_N </b> :  Hourly average medium heavy vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> HGV_SPD_D </b><b> HGV_SPD_E </b><b> HGV_SPD_N </b> :  Hourly average heavy duty vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> WAV_SPD_D </b><b> WAV_SPD_E </b><b> WAV_SPD_N </b> :  Hourly average mopeds, tricycles or quads &le; 50 cc speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> WBV_SPD_D </b><b> WBV_SPD_E </b><b> WBV_SPD_N </b> :  Hourly average motorcycles, tricycles or quads > 50 cc speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> PVMT </b> :  CNOSSOS road pavement identifier (ex: NL05)(default NL08) (VARCHAR)</li>" +
                        "<li><b> TS_STUD </b> : A limited period Ts (in months) over the year where a average proportion pm of light vehicles are equipped with studded tyres (0-12) (DOUBLE)</li>" +
                        "<li><b> PM_STUD </b> : Average proportion of vehicles equipped with studded tyres during TS_STUD period (0-1) (DOUBLE)</li>" +
                        "<li><b> JUNC_DIST </b> : Distance to junction in meters (DOUBLE)</li>" +
                        "<li><b> JUNC_TYPE </b> : Type of junction (k=0 none, k = 1 for a crossing with traffic lights ; k = 2 for a roundabout) (INTEGER)</li>" +
                        "<li><b> SLOPE </b> : Slope (in %) of the road section. If the field is not filled in, the LINESTRING z-values will be used to calculate the slope and the traffic direction (way field) will be force to 3 (bidirectional). (DOUBLE)</li>" +
                        "<li><b> WAY </b> : Define the way of the road section. 1 = one way road section and the traffic goes in the same way that the slope definition you have used, 2 = one way road section and the traffic goes in the inverse way that the slope definition you have used, 3 = bi-directional traffic flow, the flow is split into two components and correct half for uphill and half for downhill (INTEGER)</li>" +
                        "</ul></br><b> This table can be generated from the WPS Block 'Import_OSM'. </b>.",
                type       : String.class,
                coefficientVersion            : [
                        name       : 'Coefficient version',
                        title      : 'Coefficient version',
                        description: '&#127783; Cnossos coefficient version  (1 = 2015, 2 = 2020) </br> </br>' +
                                '&#128736; Default value: <b>2</b>',
                        min        : 0, max: 1,
                        type       : Double.class
                ],
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

    int coefficientVersion = 2
    if (input.containsKey('coefficientVersion')) {
        coefficientVersion = Integer.parseInt(input['confHumidity'] as String)
    }

    DBTypes dbType = DBUtils.getDBType(connection)

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Road Emission from DEN')
    logger.info("inputs {}", input) // log inputs of the run


    // -------------------
    // Get every inputs
    // -------------------

    String sources_table_name = input['tableRoads']
    TableLocation sourceTableIdentifier = TableLocation.parse(sources_table_name, dbType)

    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()

    //Get optional geometry field of the source table
    List<String> geomFields = GeometryTableUtilities.getGeometryColumnNames(connection, sourceTableIdentifier)

    //Get the primary key field of the source table
    Tuple<String, Integer> primaryKeyColumn = JDBCUtilities.getIntegerPrimaryKeyNameAndIndex(connection, TableLocation.parse( sources_table_name))

    // -------------------
    // Init table LW_ROADS
    // -------------------

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    def lowerCaseColumnNames = JDBCUtilities.getColumnNames(connection, sourceTableIdentifier).stream()
            .map { it.toLowerCase() }
            .collect(Collectors.toList())

    // If there is a period field, it means that we will not found the D E N fields before traffic fields names
    boolean hasPeriodField = lowerCaseColumnNames.contains("period")
    boolean hasIdSourceField = lowerCaseColumnNames.contains("idsource")

    // drop table LW_ROADS if exists and the create and prepare the table
    sql.execute("drop table if exists LW_ROADS;")

    def createTableQuery = new StringBuilder("CREATE TABLE LW_ROADS (")
    def preparedInsertQuery = new StringBuilder("INSERT INTO LW_ROADS(")
    int fieldCount = 0

    if(primaryKeyColumn != null) {
        createTableQuery.append(primaryKeyColumn.first())
        createTableQuery.append(" integer not null, ")
        preparedInsertQuery.append(primaryKeyColumn.first())
        preparedInsertQuery.append(", ")
        fieldCount++
    }

    if(hasIdSourceField) {
        createTableQuery.append("IDSOURCE integer, ")
        preparedInsertQuery.append("IDSOURCE, ")
        fieldCount++
    }

    def force3D = false
    if(geomFields.size() > 0) {
        createTableQuery.append(geomFields.get(0))
        createTableQuery.append(" Geometry, ")
        preparedInsertQuery.append(geomFields.get(0))
        preparedInsertQuery.append(", ")
        fieldCount++
        def tupMeta = GeometryTableUtilities.getFirstColumnMetaData(connection, sourceTableIdentifier)
        if(tupMeta != null && !tupMeta.second().hasZ()) {
            force3D = true
            logger.warn("The geometry field "+geomFields.get(0)+" is not 3D. The z value will be forced to 0.05m height.")
        }
    }

    if(!hasPeriodField) {
        ["D", "E", "N"].each { period ->
            ["63", "125", "250", "500", "1000", "2000", "4000", "8000"].each { freq ->
                    createTableQuery.append("HZ")
                    createTableQuery.append(period)
                    createTableQuery.append(freq)
                    createTableQuery.append(" double precision, ")
                    preparedInsertQuery.append("HZ")
                    preparedInsertQuery.append(period)
                    preparedInsertQuery.append(freq)
                    preparedInsertQuery.append(", ")
                    fieldCount++
            }
        }
    } else {
        createTableQuery.append("PERIOD varchar, ")
        fieldCount++
        preparedInsertQuery.append("PERIOD, ")
        ["63", "125", "250", "500", "1000", "2000", "4000", "8000"].each { freq ->
            createTableQuery.append("HZ")
            createTableQuery.append(freq)
            createTableQuery.append(" double precision, ")
            preparedInsertQuery.append("HZ")
            preparedInsertQuery.append(freq)
            preparedInsertQuery.append(", ")
            fieldCount++
        }
    }

    // Create table
    createTableQuery.setLength(createTableQuery.length() - 2) // remove last comma
    createTableQuery.append(");")
    sql.execute(createTableQuery.toString())

    // Prepared insert query
    preparedInsertQuery.setLength(preparedInsertQuery.length() - 2) // remove last comma
    preparedInsertQuery.append(") VALUES (")
    String.join(", ", Collections.nCopies(fieldCount, "?")).each {
        preparedInsertQuery.append(it)
    }
    preparedInsertQuery.append(");")
    def qry = preparedInsertQuery.toString()

    // --------------------------------------
    // Start calculation and fill the table
    // --------------------------------------

    // Get size of the table (number of road segments
    PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) AS total FROM " + sources_table_name)
    ResultSet rs1 = st.executeQuery().unwrap(ResultSet.class)
    while (rs1.next()) {
        def nbRoads = rs1.getInt("total")
        logger.info('The table '+sources_table_name+' has ' + nbRoads + ' lines.')
    }

    sql.withBatch(100, qry) { ps ->
        st = connection.prepareStatement("SELECT * FROM " + sources_table_name)
        SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)

        Map<String, Integer> sourceFieldsCache = new HashMap<>()
        while (rs.next()) {
            List<Object> parameters = new ArrayList<>()
            if(primaryKeyColumn != null) {
                parameters.add(rs.getInt(primaryKeyColumn.first()))
            }
            if(hasIdSourceField) {
                parameters.add(rs.getInt("IDSOURCE"))
            }
            if(geomFields.size() > 0) {
                parameters.add(rs.getGeometry(geomFields.get(0)))
            }
            if(hasPeriodField) {
                parameters.add(rs.getString("PERIOD"))
                // Slope value will be overwritten if the slope field is present
                double slope = EmissionTableGenerator.getSlope(rs)
                double[] emissionValues = EmissionTableGenerator.getEmissionFromTrafficTable(rs, "", slope, coefficientVersion, sourceFieldsCache)
                for(double val : emissionValues) {
                    parameters.add(val)
                }
            } else {
                double[][] results = EmissionTableGenerator.computeLw(rs, coefficientVersion, sourceFieldsCache)
                def lday = AcousticIndicatorsFunctions.wToDb(results[0])
                def levening = AcousticIndicatorsFunctions.wToDb(results[1])
                def lnight = AcousticIndicatorsFunctions.wToDb(results[2])
                for(def val : lday) {
                    parameters.add(val)
                }
                for(def val : levening) {
                    parameters.add(val)
                }
                for(def val : lnight) {
                    parameters.add(val)
                }
            }
            ps.addBatch(parameters)
        }
    }

    if(force3D) {
        // Force the Z height to the road segments
        sql.execute("UPDATE LW_ROADS SET THE_GEOM = ST_UPDATEZ(The_geom, 0.05);")
    }

    if(primaryKeyColumn != null) {
        // Set primary key to the road table
        sql.execute("ALTER TABLE LW_ROADS ADD PRIMARY KEY ("+primaryKeyColumn.first()+");  ")
    }

    resultString = "Calculation Done ! The table LW_ROADS has been created."

    // print to command window
    logger.info('\nResult : ' + resultString)
    logger.info('End : LW_ROADS from Emission')

    // print to WPS Builder
    return resultString

}


