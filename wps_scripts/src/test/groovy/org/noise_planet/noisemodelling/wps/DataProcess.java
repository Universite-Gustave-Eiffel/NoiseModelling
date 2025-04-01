
package org.noise_planet.noisemodelling.wps;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

/**
 * The {@code DataProcess} class provides methods for processing CSV files containing device data.
 * It reads the data from input CSV files, performs transformations on timestamps,
 * and writes the updated data to output CSV files.
 */

public class DataProcess {

    /**
     * The entry point of the program that processes all CSV files in the input directory.
     * It reads each CSV file, processes the data, and writes the output to the output directory.
     *
     * @param args command-line arguments
     * @throws IOException if an I/O error occurs while reading or writing files
     */
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        // Input and output directories
        String inputDir = "./wps_scripts/devices-data";
        String outputDir = "./wps_scripts/devices-data_cleaned";

        // Create output directory if it doesn't exist
        Files.createDirectories(Paths.get(outputDir));

        // List all CSV files in the input directory
        List<Path> fileList = Files.list(Paths.get(inputDir))
                .filter(path -> path.toString().endsWith(".csv"))
                .collect(Collectors.toList());

        // Process each CSV file
        for (Path inputFile : fileList) {
            processCsvFile(inputFile.toString(), outputDir + "/" + inputFile.getFileName().toString());
        }

    }

    /**
     * Processes the CSV file by reading its data, transforming the timestamps, and writing the updated data to a new CSV file.
     *
     * @param inputFile the path to the input CSV file
     * @param outputFile the path to the output CSV file where transformed data will be written
     * @throws IOException if an I/O error occurs while reading or writing the CSV files
     */
    private static void processCsvFile(String inputFile, String outputFile) throws IOException {
        // Read the CSV file
        try (CSVReader reader = new CSVReader(new FileReader(inputFile));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {

            List<String[]> data = reader.readAll(); // Read the entire file into memory
            String[] header = data.get(0); // The first line is the header
            List<String[]> rows = data.subList(1, data.size()); // The remaining lines are the data


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"); // current date format
            List<String[]> updatedRows = new ArrayList<>();

            // Transform the data
            LocalDateTime previousTimestamp = null;
            for (String[] row : rows) {
                // Convert the timestamp column to LocalDateTime
                LocalDateTime timestamp = LocalDateTime.parse(row[13], formatter);

                // Round to the nearest quarter-hour
                timestamp = roundToNearestQuarter(timestamp);

                // Ensure a 15-minute difference between rows
                if (previousTimestamp != null && !timestamp.isAfter(previousTimestamp.plusMinutes(15))) {
                    timestamp = previousTimestamp.plusMinutes(15);
                }
                previousTimestamp = timestamp;

                // Update the epoch column
                long epoch = timestamp.toEpochSecond(ZoneOffset.UTC);

                // Convert timestamp to string format
                row[13] = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // Update the epoch column (assuming it's in the second column)
                row[1] = String.valueOf(epoch);

                // Add the transformed row to the list
                updatedRows.add(row);
            }

            // Write the header and updated data to the output file
            writer.writeNext(header);
            writer.writeAll(updatedRows);

            System.out.println("File processed successfully: " + outputFile);
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Rounds the given {@link LocalDateTime} object to the nearest quarter-hour.
     * If the minutes value is between a multiple of 15, it will round up or down
     * to the nearest quarter-hour.
     *
     * @param time the {@link LocalDateTime} to be rounded
     * @return a new {@link LocalDateTime} rounded to the nearest quarter-hour
     */
    public static LocalDateTime roundToNearestQuarter(LocalDateTime time) {
        int minutes = time.getMinute();
        int roundedMinutes = ((minutes + 7) / 15) * 15; // Round to the nearest multiple of 15
        if (roundedMinutes >= 60) {
            return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        } else {
            return time.withMinute(roundedMinutes).withSecond(0).withNano(0);
        }
    }
}
