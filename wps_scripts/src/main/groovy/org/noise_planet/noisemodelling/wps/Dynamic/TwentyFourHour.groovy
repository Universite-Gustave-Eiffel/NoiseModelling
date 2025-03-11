package org.noise_planet.noisemodelling.wps.Dynamic


import java.nio.file.*;
import java.time.*;
import java.util.stream.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

/**
 * TwentyFourHour class for filtering and processing CSV files
 * containing sensor data over a 24-hour period.
 */

class TwentyFourHour {

    /**
     * Main method executing the filtering and processing of CSV data.
     *
     * @param args Command line arguments
     * @throws Exception In case of CSV file read/write errors
     */

     static void main(String[] args) throws Exception {
        String folderPath = "/home/maguettte/IdeaProjects/Orbiwise/DataORBIWISE/devices-data/devices-data_cleaned"
        String outputFilePath = "/home/maguettte/IdeaProjects/Orbiwise/DataORBIWISE/devices-data/24_hour_updt.csv"

        // Read all CSV files
        List<Map<String, String>> allData = new ArrayList<>()
        try {
            Stream<Path> paths = Files.walk(Paths.get(folderPath))
            paths.filter({ path -> Files.isRegularFile(path) })
                    .filter({ path -> path.toString().endsWith(".csv") })
                    .forEach({ filePath ->
                        try {
                            allData.addAll(readCsv(filePath))
                        } catch (IOException | CsvException e) {
                            e.printStackTrace()
                        }
                    });
        }catch (IOException | CsvException e) {
            e.printStackTrace()
        }

        // Convert timestamps and filter valid data
        List<Map<String, String>> validData = allData.stream()
                .filter({ data -> data.containsKey("timestamp") && !data.get("timestamp").isEmpty() })
                .collect(Collectors.toList())

        // Summarize start and end times per sensor
        Map<String, Map<String, String>> sensorSummary = summarizeSensorTimes(validData)

        // Print sensor activity
        /*sensorSummary.forEach({ deveui, times ->
            System.out.println("Sensor: " + deveui);
            System.out.println("Start Time: " + times.get("start_time"))
            System.out.println("End Time: " + times.get("end_time"))
        })*/

        // Find the global overlapping time window
        LocalDateTime globalStart = getMaxStartTime(sensorSummary)
        //LocalDateTime globalEnd = getMinEndTime(sensorSummary)

        // Define a 24-hour window within the overlapping period
        LocalDateTime dayStart = globalStart.plusMonths(1)
        LocalDateTime dayEnd = dayStart.plusHours(24) // 24-hour interval and can be changed to 1 hour, 2 hour,...

        // Filter data for the 24-hour interval
        List<Map<String, String>> df24Hours = validData.stream()
                .filter({ data ->
                    LocalDateTime timestamp = parseTimestamp(data.get("timestamp"))
                    return !timestamp.isBefore(dayStart) && !timestamp.isAfter(dayEnd)
                })
                .collect(Collectors.toList())

        // Save the filtered 24-hour data to CSV
        writeCsv(outputFilePath, df24Hours)
    }

    /**
     * Reads all the CSV file and converts it into a list of maps.
     *
     * @param filePath Path to the CSV file
     * @return List of data as a map
     * @throws IOException, CsvException In case of read errors
     */
     static List<Map<String, String>> readCsv(Path filePath) throws IOException, CsvException {
        List<Map<String, String>> data = new ArrayList<>()
        try {
            CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))
            List<String[]> rows = reader.readAll()
            String[] headers = rows.get(0)
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i)
                Map<String, String> map = new HashMap<>()
                for (int j = 0; j < headers.length; j++) {
                    map.put(headers[j], row[j])
                }
                map.put("file_name", filePath.getFileName().toString())
                data.add(map)
            }
        }catch (IOException | CsvException e) {
            e.printStackTrace()
        }
        return data
    }

    /**
     * Summarizes the start and end times for each sensor.
     *
     * @param validData List of filtered data
     * @return A map containing sensor summaries
     */
     static Map<String, Map<String, String>> summarizeSensorTimes(List<Map<String, String>> validData) {
        Map<String, Map<String, String>> sensorSummary = new HashMap<>()
        for (Map<String, String> data : validData) {
            String deveui = data.get("deveui")
            String timestamp = data.get("timestamp")
            LocalDateTime time = parseTimestamp(timestamp)

            if (!sensorSummary.containsKey(deveui)) {
                sensorSummary.put(deveui, new HashMap<>())
                sensorSummary.get(deveui).put("start_time", timestamp)
                sensorSummary.get(deveui).put("end_time", timestamp)
            } else {
                LocalDateTime startTime = parseTimestamp(sensorSummary.get(deveui).get("start_time"))
                LocalDateTime endTime = parseTimestamp(sensorSummary.get(deveui).get("end_time"))
                if (time.isBefore(startTime)) {
                    sensorSummary.get(deveui).put("start_time", timestamp)
                }
                if (time.isAfter(endTime)) {
                    sensorSummary.get(deveui).put("end_time", timestamp)
                }
            }
        }
        return sensorSummary
    }

    /**
     * Finds the maximum start time of measurements among all sensors.
     *
     * @param sensorSummary Sensor summary
     * @return The latest start time
     */
     static LocalDateTime getMaxStartTime(Map<String, Map<String, String>> sensorSummary) {
        return sensorSummary.values().stream()
                .map({ times -> parseTimestamp(times.get("start_time")) })
                .max({ time1, time2 -> time1.compareTo(time2) })
                .orElseThrow({ -> new IllegalArgumentException("No start times found") })
    }

    /**
     * Finds the minimum end time among all sensors.
     *
     * @param sensorSummary Sensor summary
     * @return The earliest end time
     */
     static LocalDateTime getMinEndTime(Map<String, Map<String, String>> sensorSummary) {
        return sensorSummary.values().stream()
                .map({ times -> parseTimestamp(times.get("end_time")) })
                .min({ time1, time2 -> time1.compareTo(time2) })
                .orElseThrow({ -> new IllegalArgumentException("No end times found") })
    }

    /**
     * Converts a timestamp to LocalDateTime.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Corresponding date and time
     */
     static LocalDateTime parseTimestamp(String timestamp) {
        long epochMilli = Long.parseLong(timestamp)
        return Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    /**
     * Writes filtered data to a CSV file.
     *
     * @param outputFilePath Output file path
     * @param data Filtered data to write
     * @throws IOException In case of write errors
     */
     static void writeCsv(String outputFilePath, List<Map<String, String>> data) throws IOException {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath))
            if (!data.isEmpty()) {
                // Write header
                Set<String> headers = data.get(0).keySet()
                writer.writeNext(headers.toArray(new String[0]))

                // Write data rows
                for (Map<String, String> row : data) {
                    writer.writeNext(row.values().toArray(new String[0]))
                }
            }
        }catch (IOException | CsvException e) {
            e.printStackTrace()
        }

    }

}
