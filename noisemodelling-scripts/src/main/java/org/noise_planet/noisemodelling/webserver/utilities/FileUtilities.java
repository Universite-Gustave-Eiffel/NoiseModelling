/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.utilities;

import com.fasterxml.jackson.core.*;
import org.h2gis.api.ProgressVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileUtilities {



    /**
     * Merges multiple .sql.gz files into a single .sql.gz file.
     * The first file's create table instruction is preserved; later files only contribute data
     * starting from the first "INSERT INTO" line.
     *
     * @param inputFiles List of paths to .sql.gz files
     * @param outputFile The destination .sql.gz file
     * @throws IOException If an I/O error occurs
     */
    public static void mergeSqlFiles(List<String> inputFiles, File outputFile, ProgressVisitor progressVisitor) throws IOException {
        // Use a large buffer for bulk copying
        char[] buffer = new char[32768]; // 32KB buffer

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             OutputStreamWriter osw = new OutputStreamWriter(gzos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {
            ProgressVisitor subProgress = progressVisitor.subProcess(inputFiles.size());
            for (int fileIndex = 0; fileIndex < inputFiles.size(); fileIndex++) {
                File inputFile = new File(inputFiles.get(fileIndex));
                if (!inputFile.exists()) continue;

                try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(inputFile));
                     InputStreamReader isr = new InputStreamReader(gzis, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(isr)) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Logic to identify the starting line
                        boolean isMatch = line.startsWith("INSERT INTO") ||
                                (fileIndex == 0 && line.startsWith("CREATE CACHED TABLE"));

                        if (isMatch) {
                            // 1. Write the first matching line
                            writer.write(line);
                            writer.write('\n'); // Standardize on Unix newline for SQL

                            // 2. FAST BULK TRANSFER:
                            // Stop reading line-by-line and copy the remaining stream in chunks.
                            int charsRead;
                            while ((charsRead = reader.read(buffer)) != -1) {
                                writer.write(buffer, 0, charsRead);
                            }

                            // Exit the line-by-line loop and proceed to the next file
                            break;
                        }
                    }
                }
                // Flush the writer after each file to keep the GZIP compression stream moving
                writer.flush();
                subProgress.endStep();
            }
        }
    }

    /**
     * Merge GeoJSON files
     * @param inputFiles Input files to merge
     * @param outputFile Merged output file destination
     * @throws IOException If an I/O error occurs
     */
    public static void mergeGeoJSONFiles(List<String> inputFiles, File outputFile) throws IOException {
        JsonFactory factory = new JsonFactory();

        // Merges multiple GeoJSON files into a single FeatureCollection preserving first CRS
        try (FileOutputStream os = new FileOutputStream(outputFile);
             JsonGenerator gen = factory.createGenerator(os, JsonEncoding.UTF8)) {

            gen.writeStartObject();
            gen.writeStringField("type", "FeatureCollection");

            boolean featuresArrayStarted = false;

            for (int i = 0; i < inputFiles.size(); i++) {
                String fileName = inputFiles.get(i);
                File f = new File(fileName);

                if (!f.exists()) {
                    continue;
                }

                // try-with-resources for the parser of each file
                try (JsonParser parser = factory.createParser(f)) {
                    while (parser.nextToken() != null) {
                        String fieldName = parser.currentName();

                        // 1. Capture CRS only from the FIRST file
                        if (i == 0 && "crs".equals(fieldName)) {
                            gen.writeFieldName("crs");
                            parser.nextToken(); // Move to the start of the CRS object
                            gen.copyCurrentStructure(parser);
                        }

                        // 2. Handle the "features" array
                        else if ("features".equals(fieldName) && parser.currentToken() == JsonToken.START_ARRAY) {
                            // If this is the first file we are processing, open the global features array
                            if (!featuresArrayStarted) {
                                gen.writeArrayFieldStart("features");
                                featuresArrayStarted = true;
                            }

                            // Stream every feature object from the current file into the output
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                gen.copyCurrentStructure(parser);
                            }
                            // Stop parsing this file once we finish its features array
                            break;
                        }
                    }
                }
            }

            // Close the features array and the root object
            if (featuresArrayStarted) {
                gen.writeEndArray();
            } else {
                // Fallback if no features were ever found
                gen.writeArrayFieldStart("features");
                gen.writeEndArray();
            }

            gen.writeEndObject();
        }
    }


    /**
     * Collects library information from ClassLoader manifests.
     */
    public static List<LibraryInfo> collectLibraryIdentifiers() {
        List<LibraryInfo> libraries = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault());

        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");

            while (resources.hasMoreElements()) {
                try (var inputStream = resources.nextElement().openStream()) {
                    Manifest manifest = new Manifest(inputStream);
                    Attributes attributes = manifest.getMainAttributes();

                    String bundleName = attributes.getValue("Bundle-Name");
                    if (bundleName != null) {
                        String version = attributes.getValue("Bundle-Version");
                        String commit = attributes.getValue("Implementation-Build");
                        String lastModRaw = attributes.getValue("Bnd-LastModified");

                        String formattedDate = null;
                        if (lastModRaw != null) {
                            try {
                                formattedDate = sdf.format(new Date(Long.parseLong(lastModRaw)));
                            } catch (NumberFormatException ignored) {}
                        }

                        libraries.add(new LibraryInfo(bundleName, formattedDate, version, commit,
                                lastModRaw == null ? 0 : Long.parseLong(lastModRaw)));
                    }
                } catch (IOException e) {
                    // Log internally or skip individual failed manifest reads
                }
            }
        } catch (IOException e) {
            // Error finding resources
        }

        // Sort by name
        libraries.sort(Comparator.comparing(LibraryInfo::getName));
        return libraries;
    }
}
