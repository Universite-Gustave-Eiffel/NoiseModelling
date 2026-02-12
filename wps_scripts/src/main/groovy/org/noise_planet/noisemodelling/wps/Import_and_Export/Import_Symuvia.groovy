/**
 * @Author Aumond Pierre, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Import_and_Export

import org.noise_planet.noisemodelling.wps.Database_Manager.DatabaseHelper
import geoserver.GeoServer
import geoserver.catalog.Store
import jdk.internal.org.xml.sax.SAXException
import org.apache.commons.io.FilenameUtils
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.api.ProgressVisitor
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.TableUtilities
import org.h2gis.utilities.dbtypes.DBTypes
import org.h2gis.utilities.dbtypes.DBUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import org.xml.sax.XMLReader
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.helpers.XMLReaderFactory
import org.xml.sax.SAXException

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import java.nio.channels.FileChannel
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import org.xml.sax.Attributes


title = 'Import Symuvia File'
description = '&#10145;&#65039; Import <a href="https://github.com/licit-lab/Open-SymuVia" target="_blank">Symuvia</a> outputs (as .xml) into the database'

inputs = [
        pathFile : [
                name       : 'Path of the input File',
                title      : 'Path of the input File',
                description: '&#128194; Path of the input File (including extension .xml)</br> </br>' +
                             'For example: c:/home/mysymuviafile.xml',
                type       : String.class
        ],
        inputSRID: [
                name       : 'Symuvia output file SRID',
                title      : 'Symuvia output file SRID',
                description: 'Symuvia output file SRID </br> </br>' +
                             '&#128736; Default value: French Lambert 93 (<a href="https://epsg.io/2154" target="_blank">2154</a>)',
                min        : 0, max: 1,
                type       : Integer.class
        ],
        tableName: [
                name       : 'outputTableName',
                title      : 'Name of created table',
                description: 'Do not write the name of a table that contains a space </br> </br>' +
                             '&#128736; Default value: <b>it will take the name of the file without its extension</b> (special characters will be removed and whitespaces will be replace by an underscore.',
                min        : 0, max: 1,
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

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Import File')
    logger.info("inputs {}", input) // log inputs of the run

    // Default SRID (French Lambert 93)
    Integer srid = 2154
    // Get user SRID
    if (input['inputSRID']) {
        srid = input['inputSRID'] as Integer
    }

    // Get the path of the file to import
    String pathFile = input["pathFile"] as String
    if (!input["pathFile"]) {
        resultString = "pathFile argument has not been provided."
        throw new Exception('ERROR : ' + resultString)
    }

    def file = new File(pathFile)
    if (!file.exists()) {
        resultString = pathFile + " is not found."
        throw new Exception('ERROR : ' + resultString)
    }

    // Get name of the table
    String tableName = input["tableName"] as String

    // By default the name of the output table is the same than the file name
    if (!tableName) {
        // get the name of the fileName
        String fileName = FilenameUtils.removeExtension(new File(pathFile).getName())
        // replace whitespaces by _ in the file name
        fileName.replaceAll("\\s", "_")
        // remove special characters in the file name
        fileName.replaceAll("[^a-zA-Z0-9 ]+", "_")
        // the tableName will be called as the fileName
        tableName = fileName
    }

    // do it case-insensitive
    tableName = tableName.toUpperCase()
    tableNameTraj = tableName + "_TRAJ"
    // Create a connection statement to interact with the database in SQL
    Statement stmt = connection.createStatement()

    // Drop the table if already exists
    stmt.execute("drop table if exists " + tableNameTraj)
    stmt.execute("drop table if exists " + tableName)
    stmt.execute("drop table if exists " + tableNameTraj +"_XML")
    stmt.execute("drop table if exists " + tableName +"_INST")

    String ext = pathFile.substring(pathFile.lastIndexOf('.') + 1, pathFile.length())
    switch (ext) {
        case "xml":
            SYMUVIADriverFunction symuviaDriver = new SYMUVIADriverFunction()
            symuviaDriver.importFile(connection, tableName, new File(pathFile), new EmptyProgressVisitor())
            break
    }

    stmt.execute("CREATE TABLE " + tableNameTraj + " AS SELECT id ID_VEH, ST_Point(abs, ord) THE_GEOM, type TYPE,vit SPEED, acc ACC,inst TIME FROM " +tableNameTraj + "_xml;")
    stmt.execute("DROP TABLE " +tableNameTraj + "_xml IF EXISTS;")
    stmt.execute("DROP TABLE " +tableName + "_INST IF EXISTS;")


    // Read Geometry Index and type of the table
    List<String> spatialFieldNames = GeometryTableUtilities.getGeometryColumnNames(connection, TableLocation.parse(tableNameTraj, DBUtils.getDBType(connection)))
    //JDBCUtilities.getTableNames(connection, null, "PUBLIC", "%", null)
    // If the table does not contain a geometry field
    if (spatialFieldNames.isEmpty()) {
        logger.warn("The table " + tableNameTraj + " does not contain a geometry field.")
    } else {
        stmt.execute('CREATE SPATIAL INDEX IF NOT EXISTS ' + tableNameTraj + '_INDEX ON ' + tableNameTraj + '(the_geom);')

        // Get the SRID of the table
        Integer tableSrid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(tableNameTraj))

        if (tableSrid != 0 && tableSrid != srid && input['inputSRID']) {
            resultString = "The table already has a different SRID than the one you gave."
            throw new Exception('ERROR : ' + resultString)
        }

        // Replace default SRID by the srid of the table
        if (tableSrid != 0) srid = tableSrid

        // Display the actual SRID in the command window
        logger.info("The SRID of the table is " + srid)

        // If the table does not have an associated SRID, add a SRID
        if (tableSrid == 0 && !spatialFieldNames.isEmpty()) {
            connection.createStatement().execute(String.format("SELECT UpdateGeometrySRID('%s', '" + spatialFieldNames.get(0) + "', %d);",
                    TableLocation.parse(tableNameTraj).toString(), srid))
        }

    }


    // get the index of the primary key column (if exists > 0)
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse(tableNameTraj))

    // get the index of the column given by the user (if exists > 0)
    ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableNameTraj)
    int pkUserIndex = JDBCUtilities.getFieldIndex(rs.getMetaData(), "PK")

    if (pkIndex > 0) {
        resultString = String.format("Warning : Source table %s did already contain a primary key. The constraint has been removed. </br>", table)
        logger.warn(String.format("Warning : Source table %s did already contain a primary key. The constraint has been removed.", table))
        stmt.execute("ALTER TABLE " + tableNameTraj + " DROP PRIMARY KEY;")
    }

    if (pkUserIndex > 0) {
        stmt.execute("ALTER TABLE " + tableNameTraj + " ALTER COLUMN PK INT NOT NULL;")
        stmt.execute("ALTER TABLE " + tableNameTraj + " ADD PRIMARY KEY (PK);  ")
    } else {
        stmt.execute("ALTER TABLE " + tableNameTraj + " ADD PK INT ${DatabaseHelper.autoIncrement(connection)} PRIMARY KEY;")
    }

    resultString = "The table " + tableNameTraj + " has been uploaded to database!"

    // print to command window
    logger.info(resultString)
    logger.info('End : Import File')

    // print to WPS Builder
    return resultString


}


class SYMUVIADriverFunction {
    public static String DESCRIPTION = "SYMUVIA file (ver. x.x)";

    void importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress) throws SQLException, IOException {
        importFile(connection, tableReference, fileName, progress, false)


    }

    /**
     *
     * @param connection Active connection, do not close this connection.
     * @param tableReference prefix uses to store the SYMUVIA tables
     * @param fileName File path to read
     * @param progress
     * @param deleteTables true to delete the existing tables
     * @throws SQLException Table write error
     * @throws IOException File read error
     */
    void importFile(Connection connection, String tableReference, File fileName, ProgressVisitor progress, boolean deleteTables) throws SQLException, IOException {
        if (fileName == null || !(fileName.getName().endsWith(".xml"))) {
            throw new IOException(new IllegalArgumentException("This driver handle only .xml files"))
        }
        if (deleteTables) {
            SYMUVIATablesFactory.dropSYMUVIATables(connection, JDBCUtilities.isH2DataBase(connection), tableReference)
        }
        SYMUVIAParser symuviap = new SYMUVIAParser()
        symuviap.read(connection, tableReference, fileName, progress)
    }


    class InstSYMUVIAElement {

        private double val

        /**
         * Constructor
         * @param val Latitude value
         */
        InstSYMUVIAElement(double val) {
            this.val = val
        }


        /**
         * The val of the element
         *
         * @return
         */
        double getVAL() {
            return val
        }


    }

    class SYMUVIAParser extends DefaultHandler {

        private static final int BATCH_SIZE = 100;
        private PreparedStatement instPreparedStmt;
        private PreparedStatement trajPreparedStmt;

        private int instPreparedStmtBatchSize = 0;
        private int trajPreparedStmtBatchSize = 0;


        private InstSYMUVIAElement instSYMUVIAElement;
        private TrajSYMUVIAElement trajSYMUVIAElement;

        private ProgressVisitor progress = new EmptyProgressVisitor();
        private FileChannel fc;
        private long fileSize = 0;
        private long readFileSizeEachNode = 1;
        private long nodeCountProgress = 0;
        // For progression information return
        private static final int AVERAGE_NODE_SIZE = 500;
        private double indice_val = 0;

        SYMUVIAParser() {

        }

        /**
         * Read the SYMUVIA file and create its corresponding tables.
         *
         * @param inputFile
         * @param tableName
         * @param connection
         * @param progress
         * @return
         * @throws SQLException
         */
         boolean read(Connection connection, String tableName, File inputFile, ProgressVisitor progress) throws SQLException {
            this.progress = progress.subProcess(100);
            // Initialisation
            final boolean isH2 = JDBCUtilities.isH2DataBase(connection);
            boolean success = false;
            TableLocation requestedTable = TableLocation.parse(tableName, DBUtils.getDBType(connection));
            String symuviaTableName = requestedTable.getTable();
            checkSYMUVIATables(connection, requestedTable, symuviaTableName);
            createSYMUVIADatabaseModel(connection, requestedTable, symuviaTableName);

            FileInputStream fs = null;
            try {
                fs = new FileInputStream(inputFile);
                this.fc = fs.getChannel();
                this.fileSize = fc.size();
                // Given the file size and an average node file size.
                // Skip how many nodes in order to update progression at a step of 1%
                readFileSizeEachNode = Math.max((long) 1, (long) ((this.fileSize / AVERAGE_NODE_SIZE) / 100))
                nodeCountProgress = 0;
                //SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                //SAXParser parser = parserFactory.newSAXParser();
                //XMLReader reader = parser.getXMLReader();
                XMLReader parser = XMLReaderFactory.createXMLReader();
                parser.setErrorHandler(this);
                parser.setContentHandler(this);
                if (inputFile.getName().endsWith(".xml")) {
                    parser.parse(new InputSource(fs));
                } else {
                    throw new SQLException("Supported formats are .xml");
                }
                success = true;
            } catch (SAXException ex) {
                throw new SQLException(ex);
            } catch (IOException ex) {
                throw new SQLException("Cannot parse the file " + inputFile.getAbsolutePath(), ex);
            } finally {
                try {
                    if (fs != null) {
                        fs.close();
                    }
                } catch (IOException ex) {
                    throw new SQLException("Cannot close the file " + inputFile.getAbsolutePath(), ex);
                }
                // When the reading ends, close() method has to be called
                if (instPreparedStmt != null) {
                    instPreparedStmt.close();
                }
                if (trajPreparedStmt != null) {
                    trajPreparedStmt.close();
                }

            }

            return success;
        }

        /**
         * Check if one table already exists
         *
         * @param connection
         * @param isH2
         * @param requestedTable
         * @param symuviaTableName
         * @throws SQLException
         */
        private void checkSYMUVIATables(Connection connection, TableLocation requestedTable, String symuviaTableName) throws SQLException {
            String[] omsTables = [SYMUVIATablesFactory.INST, SYMUVIATablesFactory.TRAJ]
            for (String omsTableSuffix : omsTables) {
                String symuviaTable = TableUtilities.caseIdentifier(requestedTable, symuviaTableName + omsTableSuffix, DBUtils.getDBType(connection));
                if (JDBCUtilities.tableExists(connection, symuviaTable)) {
                    throw new SQLException("The table " + symuviaTable + " already exists.");
                }
            }
        }

        /**
         * Create the OMS data model to store the content of the file
         *
         * @param connection
         * @param isH2
         * @param requestedTable
         * @param symuviaTableName
         * @throws SQLException
         */
        private void createSYMUVIADatabaseModel(Connection connection, TableLocation requestedTable, String symuviaTableName) throws SQLException {
            DBTypes dbTypes = DBUtils.getDBType(connection);
            String instTableName = TableUtilities.caseIdentifier(requestedTable, symuviaTableName + SYMUVIATablesFactory.INST, dbTypes);
            instPreparedStmt = SYMUVIATablesFactory.createInstTable(connection, instTableName);
            String trajTableName = TableUtilities.caseIdentifier(requestedTable, symuviaTableName + SYMUVIATablesFactory.TRAJ, dbTypes);
            trajPreparedStmt = SYMUVIATablesFactory.createTrajTable(connection, trajTableName);


        }


        @Override
        void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (progress.isCanceled()) {
                throw new SAXException("Canceled by user");
            }
            if (localName.compareToIgnoreCase("INST") == 0) {
                instSYMUVIAElement = new InstSYMUVIAElement(Double.valueOf(attributes.getValue("val")));

            } else if (localName.compareToIgnoreCase("TRAJ") == 0) {
                trajSYMUVIAElement = new TrajSYMUVIAElement(Double.valueOf(attributes.getValue("abs")), Double.valueOf(attributes.getValue("acc")), Double.valueOf(attributes.getValue("dst")), Long.valueOf(attributes.getValue("id")), Double.valueOf(attributes.getValue("ord")), String.valueOf(attributes.getValue("type")), Double.valueOf(attributes.getValue("vit")));
            }
        }

        @Override
        public void endDocument() throws SAXException {
            // Execute remaining batch
            try {
                instPreparedStmtBatchSize = insertBatch(instPreparedStmt, instPreparedStmtBatchSize, 1);
                trajPreparedStmtBatchSize = insertBatch(trajPreparedStmt, trajPreparedStmtBatchSize, 1);
            } catch (SQLException ex) {
                throw new SAXException("Could not insert sql batch", ex);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {

            if (localName.compareToIgnoreCase("INST") == 0) {
                try {

                    instPreparedStmt.setObject(1, instSYMUVIAElement.getVAL());
                    indice_val = instSYMUVIAElement.getVAL();
                    instPreparedStmt.addBatch();
                    instPreparedStmtBatchSize++;
                } catch (SQLException ex) {
                    throw new SAXException("Cannot insert the node  :  " + instSYMUVIAElement.getVAL(), ex);
                }
            } else if (localName.compareToIgnoreCase("TRAJ") == 0) {
                try {
                    trajPreparedStmt.setObject(1, indice_val);
                    trajPreparedStmt.setObject(2, trajSYMUVIAElement.getABS());
                    trajPreparedStmt.setObject(3, trajSYMUVIAElement.getACC());
                    trajPreparedStmt.setObject(4, trajSYMUVIAElement.getDST());
                    trajPreparedStmt.setObject(5, trajSYMUVIAElement.getID());
                    trajPreparedStmt.setObject(6, trajSYMUVIAElement.getORD());
                    trajPreparedStmt.setString(7, trajSYMUVIAElement.getType());
                    trajPreparedStmt.setObject(8, trajSYMUVIAElement.getVIT());
                    trajPreparedStmt.addBatch();
                    trajPreparedStmtBatchSize++;
                } catch (SQLException ex) {
                    throw new SAXException("Cannot insert the traj  :  " + trajSYMUVIAElement.getABS(), ex);
                }
            }
            try {
                insertBatch();
            } catch (SQLException ex) {
                throw new SAXException("Could not insert sql batch", ex);
            }
            if (nodeCountProgress++ % readFileSizeEachNode == 0) {
                // Update Progress
                try {
                    progress.setStep((int) (((double) fc.position() / fileSize) * 100));
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }

        private void insertBatch() throws SQLException {
            instPreparedStmtBatchSize = insertBatch(instPreparedStmt, instPreparedStmtBatchSize);
            trajPreparedStmtBatchSize = insertBatch(trajPreparedStmt, trajPreparedStmtBatchSize);
        }

        private int insertBatch(PreparedStatement st, int batchSize, int maxBatchSize) throws SQLException {
            if (batchSize >= maxBatchSize) {
                st.executeBatch();
                return 0;
            } else {
                return batchSize;
            }
        }

        private int insertBatch(PreparedStatement st, int batchSize) throws SQLException {
            return insertBatch(st, batchSize, BATCH_SIZE);
        }


    }

    /**
     * A class to manage the traj element properties.
     *
     * @author Pierre Aumond
     */
    class TrajSYMUVIAElement {

        private double abs
        private double acc
        private long id
        private double dst
        private double ord
        private String type
        private double vit


        /**
         * Constructor
         * @param abs Latitude value
         * @param acc Longitude value
         * @param id Longitude value
         * @param dst Longitude value
         * @param ord Longitude value
         * @param type Longitude value
         * @param vit Longitude value
         */
        TrajSYMUVIAElement(double abs, double acc, double dst, long id, double ord, String type, double vit) {
            this.abs = abs
            this.acc = acc
            this.id = id
            this.dst = dst
            this.ord = ord
            this.type = type
            this.vit = vit
        }
        /**
         * The id of the element
         *
         * @return
         */
        long getID() {
            return id
        }

        /**
         * Set an id to the element
         *
         * @param id
         */

        double getABS() {
            return abs
        }

        double getACC() {
            return acc
        }

        double getDST() {
            return dst
        }

        double getORD() {
            return ord
        }

        double getVIT() {
            return vit
        }

        /**
         * Type
         *
         * @return
         */
        String getType() {
            return type
        }

        void setType(String type) {
            this.type = type
        }


    }


/**
 * Class to create the tables to import symuvia data
 *
 * An SYMUVIA file is stored in 1 table.
 *
 * (1) table_prefix + _all :  table that contains all
 *
 * @author Pierre Aumond
 */
    class SYMUVIATablesFactory {

        //Suffix table names
        public static final String TRAJ = "_traj"
        public static final String INST = "_inst"


        private SYMUVIATablesFactory() {

        }

        /**
         * Create the global table that will be used to import SYMUVIA nodes
         * @param connection
         * @param instTableName
         * @param isH2
         * @return
         * @throws SQLException
         */
        static PreparedStatement createInstTable(Connection connection, String instTableName) throws SQLException {
            Statement stmt = connection.createStatement()
            StringBuilder sb = new StringBuilder("CREATE TABLE ")
            sb.append(instTableName)
            sb.append("(val DOUBLE PRECISION);")
            stmt.execute(sb.toString())
            stmt.close()
            return connection.prepareStatement("INSERT INTO " + instTableName + " VALUES (?);")
        }


        /**
         * Create the global table that will be used to import SYMUVIA nodes
         * @param connection
         * @param trajTableName
         * @param isH2
         * @return
         * @throws SQLException
         */
        static PreparedStatement createTrajTable(Connection connection, String trajTableName) throws SQLException {
            Statement stmt = connection.createStatement()
            StringBuilder sb = new StringBuilder("CREATE TABLE ")
            sb.append(trajTableName + "_XML")
            sb.append("(inst DOUBLE PRECISION,"
                    + "abs DOUBLE PRECISION,"
                    + "acc DOUBLE PRECISION,"
                    + "dst DOUBLE PRECISION,"
                    + "id INTEGER,"
                    + "ord DOUBLE PRECISION,"
                    + "type VARCHAR,"
                    + "vit DOUBLE PRECISION);")
            stmt.execute(sb.toString())
            stmt.close()
            return connection.prepareStatement("INSERT INTO " + trajTableName + "_XML" + " VALUES (?,?,?,?,?,?,?,?);")


        }


        /**
         * Drop the existing SYMUVIA tables used to store the imported SYMUVIA data
         *
         * @param connection
         * @param isH2
         * @param tablePrefix
         * @throws SQLException
         */
        static void dropSYMUVIATables(Connection connection, boolean isH2, String tablePrefix) throws SQLException {
            TableLocation requestedTable = TableLocation.parse(tablePrefix, DBUtils.getDBType(connection))
            String symuviaTableName = requestedTable.getTable()
            String[] omsTables = String[INST, TRAJ]
            StringBuilder sb = new StringBuilder("drop table if exists ")
            String omsTableSuffix = omsTables[0]
            String symuviaTable = TableUtilities.caseIdentifier(requestedTable, symuviaTableName + omsTableSuffix, DBUtils.getDBType(connection))
            sb.append(symuviaTable)
            for (int i = 1; i < omsTables.length; i++) {
                omsTableSuffix = omsTables[i]
                symuviaTable = TableUtilities.caseIdentifier(requestedTable, symuviaTableName + omsTableSuffix, DBUtils.getDBType(connection))
                sb.append(",").append(symuviaTable)
            }
            Statement stmt = connection.createStatement()
            stmt.execute(sb.toString())
            stmt.close()
        }
    }
}
