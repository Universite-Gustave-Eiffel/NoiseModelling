package org.noise_planet.noisemodelling.wps.DataAssimilation

import com.opencsv.CSVReader
import com.opencsv.exceptions.CsvException
import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import groovy.transform.CompileStatic
import org.geotools.jdbc.JDBCDataStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

title = 'Preparation of Sensor data'
description = 'Extraction of sensor data for a given period and creation of sql tables '

inputs = [
        startDate: [
                name: 'Start Time Stamp',
                title: 'Start Time Stamp',
                description: 'the start timestamp to extract the dataset in format "%Y-%m-%d %H:%M:%S" ',
                type: String.class
        ],
        endDate: [
                name: 'End Time Stamp',
                title: 'End Time Stamp',
                description: 'the end timestamp to extract the dataset in format "%Y-%m-%d %H:%M:%S" ',
                type: String.class
        ],
        trainingRatio: [
                name: 'Training data percentage',
                title: 'Training data percentage',
                description: 'Training data as a percentage of total data ',
                type: Float.class
        ],
        workingFolder: [
                name: 'Input folder path',
                title: 'Working directory path with input files',
                description: 'Folder containing csv files "device_mapping_sf", the osm file and the folder "devices_data"',
                type: String.class
        ],
        targetSRID : [
                name       : 'Target projection identifier',
                title      : 'Target projection identifier',
                description: '&#127757; Target projection identifier (also called SRID) of your table.<br>' +
                        'It should be an <a href="https://epsg.io/" target="_blank">EPSG</a> code, an integer with 4 or 5 digits (ex: <a href="https://epsg.io/3857" target="_blank">3857</a> is Web Mercator projection).<br><br>' +
                        '&#x1F6A8; The target SRID must be in <b>metric</b> coordinates example 2056 for Geneva.',
                type       : Integer.class
        ]
]

outputs = [
        result: [
                name: 'Sql tables output',
                title: 'Sql tables output',
                description: 'Sql tables "SENSORS_MEASUREMENTS", "SENSORS_LOCATION","SENSORS", "SENSORS_MEASUREMENTS_TRAINING"',
                type: Sql.class
        ]
]


//@CompileStatic
static def exec(Connection connection,input){

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start Preparation of Sensor dataset ')

    Sql sql = new Sql(connection)
    String folderPath = input['workingFolder']
    if (!folderPath.endsWith("/")) folderPath += "/"

    String deviceFolder = folderPath + "devices_data"
    LocalDateTime dayStart = parseTimestamp(input['startDate'] as String)
    LocalDateTime dayEnd = parseTimestamp(input['endDate'] as String)
    //String sensorCsv = folderPath + "device_mapping_sf.csv"
    Float trainingRatio = input['trainingRatio'] as Float//: 0.8,
    Integer targetSRID = input['targetSRID'] as Integer

    List<Map<String, String>> selectedData = allMeasurements(dayStart, dayEnd,deviceFolder)
    measurementTable(connection,selectedData)

    sql.execute("ALTER TABLE SENSORS_LOCATION ALTER COLUMN The_GEOM " +
            "TYPE geometry(PointZ, "+targetSRID+") " +
            "USING ST_SetSRID(ST_Force3D(THE_GEOM), "+targetSRID+")")

    // Create the RECEIVERS table with unique sensor data from measurement (SENSORS_MEASUREMENTS_TRAINING: training data) table.
    sql.execute("ALTER TABLE SENSORS_LOCATION ADD PK INT AUTO_INCREMENT PRIMARY KEY;")


    sql.execute("ALTER TABLE SENSORS_MEASUREMENTS ADD COLUMN THE_GEOM GEOMETRY(PointZ,"+targetSRID+")")
    sql.execute("ALTER TABLE SENSORS_MEASUREMENTS ADD COLUMN IDRECEIVER INTEGER")

    sql.execute("UPDATE SENSORS_MEASUREMENTS sm " +
            "SET THE_GEOM = (select ST_Transform(s.The_GEOM, "+targetSRID+" )"+
            " FROM SENSORS_LOCATION s " +
            " WHERE sm.deveui = s.deveui );")

    sql.execute("UPDATE SENSORS_MEASUREMENTS sm " +
            "SET IDRECEIVER = (select PK"+
            " FROM SENSORS_LOCATION s " +
            " WHERE sm.deveui = s.deveui);")

    extractObservationData(connection,trainingRatio)

    sql.execute("ALTER TABLE SENSORS_MEASUREMENTS_TRAINING ALTER COLUMN THE_GEOM TYPE geometry(PointZ, "+targetSRID+") " +
            "USING ST_SetSRID(ST_Force3D(THE_GEOM), "+targetSRID+")")

    String inputTable = "SENSORS_MEASUREMENTS_TRAINING"
    sql.execute("ALTER TABLE "+inputTable+" ALTER COLUMN T SET DATA TYPE INTEGER")
    sql.execute("ALTER TABLE "+inputTable+" ALTER COLUMN IDRECEIVER SET DATA TYPE INTEGER")
    sql.execute("ALTER TABLE "+inputTable+" ALTER COLUMN TEMP SET DATA TYPE FLOAT")
    sql.execute("ALTER TABLE "+inputTable+" ALTER COLUMN LEQA SET DATA TYPE FLOAT")

    logger.info('End Preparation of Sensor dataset ')


}

static def run(input) {

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

// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

/**
 * Extracts sensor data from the sensor measurements files for a given period dayStart -> dayEnd
 *
 * @param dayStart: Starting date period
 * @param dayEnd: Ending date period
 * @param deviceFolder: Path to the folder containing all the measurements of sensors
 * @return a Map containing all sensors data
 */
static def allMeasurements(LocalDateTime dayStart, LocalDateTime dayEnd, String deviceFolder){

    List<Map<String, String>> allData = []
    Files.walk(Paths.get(deviceFolder))
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".csv") }
            .forEach { path ->
                try {
                    allData.addAll(readCsv(path))
                } catch (Exception e) {
                    e.printStackTrace()
                }
            }

    List<Map<String, String>> validData = allData.findAll { it.containsKey("timestamp") && !it.timestamp.isEmpty() }
    List<Map<String, String>> selectedData = validData.findAll {
        LocalDateTime ts = parseTimestamp(it.timestamp)
        !ts.isBefore(dayStart) && !ts.isAfter(dayEnd)
    }

    return selectedData
   // if (dataSelected == null ) print("null selected data")

    //selectedData.clear()
    //selectedData.addAll(dataSelected)
}



/**
 * Extracts sensor data from the sensor measurements files for a given period dayStart -> dayEnd
 *
 * @param connection: The database connection used for executing queries.
 * @param selectedData Map containing all sensors
 */
static def measurementTable(Connection connection,List<Map<String, String>> selectedData) {
    Sql sql = new Sql(connection)
    sql.execute("""DROP TABLE IF EXISTS SENSORS_MEASUREMENTS;""")
    sql.execute("CREATE TABLE SENSORS_MEASUREMENTS (" +
            "    deveui VARCHAR(255)," +
            "    epoch VARCHAR(255)," +
            "    Leq FLOAT," +
            "    Temp FLOAT" +
            ")")

    String tableName = "SENSORS_MEASUREMENTS"

    List<String> columns = ["deveui", "epoch", "Leq", "Temp"]

    selectedData.each { row ->
        StringBuilder sb = new StringBuilder()
        sb.append("INSERT INTO ").append(tableName).append(" (")
                .append(columns.join(", ")).append(") VALUES (")

        columns.eachWithIndex { col, i ->
            String value = row.getOrDefault(col, "")
            if (col == "Leq" || col == "Temp") {
                sb.append(value)
            } else {
                sb.append("'").append(value.replace("'", "''")).append("'")
            }
            if (i < columns.size() - 1) sb.append(", ")
        }

        sb.append(")")
        sql.execute(sb.toString())
    }
}

/**
 * Converts a timestamp to LocalDateTime.
 *
 * @param timestamp Timestamp in milliseconds
 * @return Corresponding date and time
 */
static def parseTimestamp(String timestamp) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return LocalDateTime.parse(timestamp, formatter)
}


/**
 * Reads all the CSV file and converts it into a list of maps.
 *
 * @param filePath Path to the CSV file
 * @return List of data as a map
 * @throws IOException, CsvException In case of read errors
 */
static def readCsv(Path path) {
    List<Map<String, String>> data = new ArrayList<>()
    try {
        CSVReader reader = new CSVReader(new FileReader(path.toFile()))
        List<String[]> rows = reader.readAll()
        String[] headers = rows.get(0)
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i)
            Map<String, String> map = new HashMap<>()
            for (int j = 0; j < headers.length; j++) {
                map.put(headers[j], row[j])
            }
            map.put("file_name", path.getFileName().toString())
            data.add(map)
        }
    }catch (IOException | CsvException e) {
        e.printStackTrace()
    }
    return data
}



/**
 * Extracts training observation sensor data from an input CSV one hour file and writes filtered training dataset to an output CSV file.
 *
 * @param connection  : The database connection used for executing queries.
 * @param ratio : the percentage of the training data
 */
static def extractObservationData(Connection connection,Float ratio) {
    Sql sql = new Sql(connection)

    List<Map<String, Object>> measureRows = sql.rows("SELECT * FROM SENSORS_MEASUREMENTS")

    Map<String, Integer> idReceiverMap = [:]
    AtomicInteger idCounter = new AtomicInteger(1)

    Set<String> uniqueSensors = measureRows*.deveui.toSet() as Set<String>

    uniqueSensors.each { sensor ->
        idReceiverMap[sensor] = idCounter.getAndIncrement()
    }

    int keepSize = (int) (idReceiverMap.size() * ratio)
    Map<String, Integer> resultMap = idReceiverMap.entrySet().toList().subList(0, keepSize).collectEntries {
        [(it.key): it.value]
    }
    sql.execute("""DROP TABLE IF EXISTS SENSORS_MEASUREMENTS_TRAINING;""")
    sql.execute("""
        CREATE TABLE IF NOT EXISTS SENSORS_MEASUREMENTS_TRAINING (
            SENSORS VARCHAR(255),
            THE_GEOM VARCHAR(255),
            IDRECEIVER INTEGER,
            T INTEGER,
            LEQA FLOAT,
            TEMP FLOAT
        )
    """)

    measureRows.each { row ->
        String sensor = row.deveui
        if (resultMap.containsKey(sensor)) {
            sql.execute("INSERT INTO SENSORS_MEASUREMENTS_TRAINING (SENSORS, THE_GEOM, IDRECEIVER, T, LEQA, TEMP) " +
                    " VALUES ('${sensor}', '${row.The_GEOM}', ${row.IDRECEIVER}, '${row.epoch}', ${row.Leq}, ${row.Temp})")
        }
    }



}
