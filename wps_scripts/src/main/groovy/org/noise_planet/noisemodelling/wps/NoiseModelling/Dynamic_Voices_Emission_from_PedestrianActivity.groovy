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

import crosby.binary.osmosis.OsmosisReader
import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry

import org.noise_planet.noisemodelling.emission.*
import org.noise_planet.noisemodelling.pathfinder.*
import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.jdbc.*
import org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import groovy.json.JsonSlurper

title = 'Compute voice emission noise map from pedestrians table.'
description = '&#10145;&#65039; -----------details). </br>' +
        '<hr>' +
        '&#x2705; The output table is called: <b>LW_PEDESTRIAN </b> '

inputs = [

        pathBDD: [
                name       : 'pathBDD table name',
                title      : 'pathBDD table name',
                description: "<b>Name of the Pedestrians table.</b>  </br>  " +
                        "<br>  This function recognize the following columns (* mandatory) : </br><ul>" +
                        "<li><b> PK </b>* : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)</li>" +
                        "</ul></br><b> This table can be generated from the WPS Block 'PedestrianLocalisation'. </b>.",
                type       : String.class,
                min        : 0, max: 1
        ],

        pathSpectrums: [
                name       : 'pathSpectrums table name',
                title      : 'pathSpectrums table name',
                description: "<b>Name of the Pedestrians table.</b>  </br>  " +
                        "<br>  This function recognize the following columns (* mandatory) : </br><ul>" +
                        "<li><b> PK </b>* : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)</li>" +
                        "</ul></br><b> This table can be generated from the WPS Block 'PedestrianLocalisation'. </b>.",
                type       : String.class,
                min        : 0, max: 1
        ],

        tablePedestrian: [
                name       : 'Pedestrians table name',
                title      : 'Pedestrians table name',
                description: "<b>Name of the Pedestrians table.</b>  </br>  " +
                        "<br>  This function recognize the following columns (* mandatory) : </br><ul>" +
                        "<li><b> PK </b>* : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)</li>" +
                        "</ul></br><b> This table can be generated from the WPS Block 'PedestrianLocalisation'. </b>.",
                type       : String.class
        ],
        populationDistribution: [
                name       : 'Population distribution',
                title      : 'Population distribution',
                description: "<b>This parameter allows the user to populate the area in terms of 3 different types of voice:</b>  </br>  " +
                        "<br>  Male, Female, Children </br><ul>" +
                        "<li><b> This is an optional input parameter</li>" +
                        "<li><b> This variable takes the percentage (%) of male, female and children in the study area</li>" +
                        "<li><b> The percentages should be separated by a coma (H,F,C)</li>",
                type       : Double.class,
                min        : 0, max: 1
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

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Pedestrian Emission')
    logger.info("inputs {}", input) // log inputs of the run


    // -------------------
    // Get every inputs
    // -------------------
    def BDD_Info = null
    def spectrumDB = null
    if (input['pathBDD']!=null){
        // Read the database info table. This table contains the information of the audio database
        BDD_Info = new JsonSlurper().parse(new File(input['pathBDD'] as String))
        logger.info('\nTHIS IS THE BDD_INFO FILE  : ' + BDD_Info)
    }
    if (input['pathBDD']!=null){
        // Read the spectrum table. This table contains the spectrum of the voice database
        spectrumDB = new JsonSlurper().parse(new File( input['pathSpectrums'] as String))
        logger.info('\nTHIS IS THE spectrumDB FILE  : ' + spectrumDB)
    }
    String sources_table_name = input['tablePedestrian']
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
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse( sources_table_name))
    if (pkIndex < 1) {
        throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", sourceTableIdentifier))
    }


    // -------------------
    // Init table LW_ROADS
    // -------------------

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // drop table LW_PEDESTRIAN if exists and then create and prepare the table
    sql.execute("drop table if exists LW_PEDESTRIAN;")
    sql.execute("create table LW_PEDESTRIAN (pk integer, the_geom Geometry, " +
            "HZ63 double precision, HZ125 double precision, HZ250 double precision, HZ500 double precision, HZ1000 double precision, HZ2000 double precision, HZ4000 double precision, HZ8000 double precision," +
            "TIMESTEP VARCHAR);")

    def qry = 'INSERT INTO LW_PEDESTRIAN(pk,the_geom, ' +
            'HZ63, HZ125, HZ250, HZ500, HZ1000,HZ2000, HZ4000, HZ8000,' +
            'TIMESTEP) ' +
            'VALUES (?,?,?,?,?,?,?,?,?,?,?);'


    // --------------------------------------
    // Start calculation and fill the table
    // --------------------------------------


    if (spectrumDB!=null){
        // We create a new table BDD_INFO in SQL that we are going to populate next
        sql.execute("DROP TABLE IF EXISTS BDD_INFO;")
        sql.execute("CREATE TABLE BDD_INFO (ID integer, NbPers_min integer, NbPers_max integer);")

        // We convert the Spectrum object into SQL in order to perform basic operations (T4)
        BDD_Info.each { bdd ->
            sql.execute("""
        INSERT INTO BDD_INFO (ID, NbPers_min,NbPers_max)
        VALUES (?, ?,?)
    """, [bdd.ID, bdd.NbPers_min, bdd.NbPers_max])
        }
    }
    def dataType = BDD_Info.getClass().getName()
    println("data type: $dataType")

    if (spectrumDB!=null){
        // We create a new table Spectrum in SQL that we are going to populate next
        sql.execute("DROP TABLE IF EXISTS Voice_SPECTRUM;")
        sql.execute("CREATE TABLE Voice_SPECTRUM(ID_g integer, ID_File integer, HZ63 double, HZ125 double, HZ250 double, HZ500 double, HZ1000 double, HZ2000 double, HZ4000 double, HZ8000 double," +
                "Alpha double, T integer);")

        // We convert the Spectrum object into SQL in order to perform basic operations (T5)
        spectrumDB.each { spect ->
            sql.execute("""
                INSERT INTO Voice_SPECTRUM(ID_g, ID_File, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, Alpha, T)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, [spect.ID_g, spect.ID_File, spect.HZ63, spect.HZ125, spect.HZ250, spect.HZ500, spect.HZ1000, spect.HZ2000, spect.HZ4000, spect.HZ8000, spect.Alpha, spect.T])
        }
    }

    // We load the PEDESTRIANS table into a list/object
    def query = 'SELECT * from PEDESTRIANS'
    def pedestriansTable = sql.rows(query)

    def resultTable = []

    // Attribution TEST

    // I've decided to perform this step (Obtention of T4) in SQL for simplicity

    // We add a new column AudioFileID to our PEDESTRIANS table. This column stores the id of the audio file corresponding to the number of pedestrians calculated in PEDESTRIANS and BDD_INFO
    sql.execute("ALTER TABLE PEDESTRIANS ADD COLUMN AudioFileID INT;")
    // We store the unique audio file identifiers from BDD_INFO. The point is that, even though we have many audio files for a single number of pedestrians, the assignment will only take one of them randomly
    //  sql.execute("SELECT DISTINCT NbPers_min FROM BDD_INFO;")
    // This is the equivalent of the AudioChooser function. We assign an audio file identifier to our PEDESTRIANS table where it corresponds
    sql.execute("UPDATE PEDESTRIANS AS spc SET AudioFileID = (SELECT ID FROM BDD_INFO AS bi WHERE bi.NbPers_min <= spc.NBPEDESTRIAN AND bi.NbPers_max >= spc.NBPEDESTRIAN  ORDER BY RAND() LIMIT 1) WHERE spc.NBPEDESTRIAN IN (SELECT DISTINCT NbPers_min FROM BDD_INFO);")
    // Since not all of the points in PEDESTRIANS correspond to a particular number of pedestrians in BDD_INFO, we'll take them out and only work with those who correspond
    sql.execute("DELETE FROM PEDESTRIANS WHERE AudioFileID IS NULL;")

    // Now we assign the Voice_SPECTRUM values to PEDESTRIANS in function of the AudioFileID
    sql.execute("DROP TABLE T6,T5, PED_SPECTRUMS IF EXISTS;")
    sql.execute("CREATE TABLE T5 AS SELECT * FROM PEDESTRIANS INNER JOIN Voice_SPECTRUM ON PEDESTRIANS.AudioFileID = Voice_SPECTRUM.ID_File")
    // Add PK
    String queryPK = '''
                    ALTER TABLE T5 DROP COLUMN PK;
                    ALTER TABLE T5 ADD PK INT AUTO_INCREMENT PRIMARY KEY;
                    '''
    sql.execute(queryPK)

    sql.execute('''
    DROP TABLE T6,T5, PED_SPECTRUMS, PED_SPECTRUMS_LW,PED_SPECTRUMS_GEOM IF EXISTS;
                CREATE TABLE T6 (PK INT AUTO_INCREMENT PRIMARY KEY,THE_GEOM Geometry,LINK_ID int,T int,HZ63 real,HZ125 real,HZ250 real,HZ500 real,HZ1000 real,HZ2000 real,HZ4000 real,HZ8000 real);
                
                INSERT INTO T6(THE_GEOM ,LINK_ID ,T ,HZ63 ,HZ125 ,HZ250 ,HZ500 ,HZ1000 ,HZ2000 ,HZ4000,HZ8000) SELECT pd.THE_GEOM, pd.PK AS LINK_ID, sp.T, sp.HZ63, sp.HZ125, sp.HZ250, sp.HZ500, sp.HZ1000, sp.HZ2000, sp.HZ4000, sp.HZ8000 FROM PEDESTRIANS pd INNER JOIN Voice_SPECTRUM sp ON pd.AUDIOFILEID = sp.ID_File;
                
                -- SELECT pd.THE_GEOM, pd.PK AS LINK_ID, sp.T, sp.HZ63, sp.HZ125, sp.HZ250, sp.HZ500, sp.HZ1000, sp.HZ2000, sp.HZ4000, sp.HZ8000 FROM PEDESTRIANS pd INNER JOIN Voice_SPECTRUM sp ON pd.AUDIOFILEID = sp.ID_File;
                
                UPDATE T6 SET THE_GEOM = ST_UPDATEZ(The_geom,1.5);
                ALTER TABLE T6 RENAME TO PED_SPECTRUMS;
                
                CREATE TABLE PED_SPECTRUMS_LW AS (SELECT PK, LINK_ID, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, T AS TIME FROM PED_SPECTRUMS);
                
                CREATE TABLE PED_SPECTRUMS_geom AS SELECT min(PK) AS PK, LINK_ID, THE_GEOM FROM PED_SPECTRUMS GROUP BY LINK_ID;
                ALTER TABLE PED_SPECTRUMS_geom ALTER COLUMN PK INT NOT NULL;
                ALTER TABLE PED_SPECTRUMS_geom ADD PRIMARY KEY (PK);
                DROP TABLE T5 IF EXISTS; 
            ''')

    sql.execute("DROP TABLE IF EXISTS PED_SPECTRUMS_0DB")
    sql.execute("CREATE TABLE PED_SPECTRUMS_0DB(PK int NOT NULL PRIMARY KEY,PED_ID long, THE_GEOM geometry, HZ63 real, HZ125 real, HZ250 real, HZ500 real, HZ1000 real, HZ2000 real, HZ4000 real, HZ8000 real) AS SELECT r.PK, r.LINK_ID, r.THE_GEOM, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 FROM PED_SPECTRUMS_GEOM AS r;")



    // Get Class to compute LW
    LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW)
    ldenConfig.setCoefficientVersion(2)
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.DAY, new PropagationProcessPathData(false));
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.EVENING, new PropagationProcessPathData(false));
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.NIGHT, new PropagationProcessPathData(false));

    LDENPropagationProcessData ldenData = new LDENPropagationProcessData(null, ldenConfig)

    // At this step, the LW_PEDESTRIAN table is filled using the class computation and the number of pedestrian in each cell
    /*
    // We can actually skip this since the T5 would be the correct/equivalent table to use here. I'm not going to touch this for the moment. Discuss with Pierre about optimizing implementation

    // THIS APPROACH IS NOT USED ANYMORE AS THE SPECTRUM IS ATTRIBUTED BY THE AUDIO FILE DATABASE
    // Get size of the table (number of pedestrians points)
    PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) AS total FROM " + sources_table_name)
    ResultSet rs1 = st.executeQuery().unwrap(ResultSet.class)
    int nbPedestrianPoints = 0
    while (rs1.next()) {
        nbRoads = rs1.getInt("total")
        logger.info('The table Pedestrian has ' + nbPedestrianPoints + ' pedestrian positions.')
    }

    int k = 0
    sql.withBatch(100, qry) { ps ->
        st = connection.prepareStatement("SELECT * FROM " + sources_table_name)
        SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)

        while (rs.next()) {
            k++
            //logger.info(rs)
            Geometry geo = rs.getGeometry()
            int nbPedestrianOnPoint = rs.getDouble("NBPEDESTRIAN")
            // Compute emission sound level for each point source
            def results = ldenData.computeLw(rs)
            // fill the LW_PEDESTRIAN table
            ps.addBatch(rs.getLong(pkIndex) as Integer, geo as Geometry,
                    70 + 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double,
                    70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70 + 10*Math.log10(nbPedestrianOnPoint) as Double,
                    70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, "DAY")
        }
    }

    // Add Z dimension to the pedestrian points
    sql.execute("UPDATE T5 SET THE_GEOM = ST_UPDATEZ(The_geom,1.5);")

    // Add primary key to the pedestrian table
    sql.execute("ALTER TABLE LW_PEDESTRIAN ALTER COLUMN PK INT NOT NULL;")
    sql.execute("ALTER TABLE LW_PEDESTRIAN ADD PRIMARY KEY (PK);  ")
    // Clean the unused attributes in T5
    sql.execute("ALTER TABLE T5 DROP COLUMN NBPEDESTRIAN, AUDIOFILEID, ID_G, ID_FILE, ALPHA;")

    // In order to produce a dynamic map, we will follow the architecture of the Matsim script
    // This will require to "separate" T5 in 2 different tables: One containing the geometry and the other one containing the sound power levels
    // These 2 tables will be "linked" by a LINK_ID attribute
    sql.execute("ALTER TABLE T5 ADD COLUMN LINK_ID INT;") // We add the LINK_ID column
    sql.execute("SET @counter = 0;") // We initialize a counter for LINK_ID
    sql.execute("UPDATE T5 SET LINK_ID = (@counter := @counter + 1);") // We update the LINK_ID column

    // Now we are going to "divide" our T5 table in T5_geom (geometries) and T5_Lw (Sound levels)
    // First, we obtain T5_LW (PK, LINK_ID, HZ..., TIME)
    sql.execute("CREATE TABLE T5_LW AS (SELECT PK, LINK_ID, HZ63, HZ125, HZ250, HZ500, HZ1000, HZ2000, HZ4000, HZ8000, T FROM T5);")
    sql.execute("ALTER TABLE T5_LW RENAME COLUMN T TO TIME; ") // We rename the column T to TIME

    // Then, we obtain T5_geom (PK, LINK_ID, THE_GEOM)
    sql.execute("CREATE TABLE T5_geom AS (SELECT PK, LINK_ID, THE_GEOM FROM T5);")
    sql.execute("ALTER TABLE T5_geom RENAME TO ")
*/
    resultString = "Calculation Done ! The table LW_PEDESTRIAN has been created."

    // print to command window
    logger.info('\nResult : ' + resultString)
    logger.info('End : LW_PEDESTRIAN from Emission')

    // print to WPS Builder
    return resultString

}



