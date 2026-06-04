/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver.script;

import net.opengis.ows11.CodeType;
import net.opengis.ows11.DomainMetadataType;
import net.opengis.ows11.LanguageStringType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.*;
import org.apache.commons.text.StringEscapeUtils;
import org.geotools.wps.WPSConfiguration;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * The `WpsScriptWrapper` class provides functionalities to manage, organize, and process
 * Groovy scripts for use in a Web Processing Service (WPS) environment. It includes methods
 * for loading scripts, parsing their metadata, grouping them into categories, and generating
 * WPS-compliant XML documents.
 *
 * The class relies on the directory structure of Groovy script files to organize them into
 * groups, and it provides mechanisms for extracting script information, such as inputs,
 * outputs, descriptions, and other metadata. These capabilities facilitate the integration
 * of scripts into a WPS framework by generating necessary XML representations.
 */
public class WpsScriptWrapper {

    private Logger logger = LoggerFactory.getLogger(WpsScriptWrapper.class);

    /**
     * The root directory where Groovy script files are stored and managed.
     * This variable represents the base directory from which scripts are loaded,
     * grouped, and processed within the WpsScriptWrapper class.
     */
    private Path scriptsRoot;

    /**
     * Default constructor for the WpsScriptWrapper class.
     *
     * This constructor initializes the WpsScriptWrapper instance by setting the
     * `scriptsRoot` field to point to the default directory containing Groovy
     * script files. The directory is resolved relative to the current working
     * directory of the application and is expected to exist at:
     * "noisemodelling-scripts/src/main/groovy/org/noise_planet/noisemodelling/scripts".
     */
    public WpsScriptWrapper(Path scriptDir) {
        this.scriptsRoot = scriptDir;
    }


    /**
     * Scans a predefined directory structure containing Groovy scripts and organizes them into groups.
     * <p>
     * This method traverses the directory structure rooted at the `scriptsRoot` location recursively.
     * It identifies Groovy script files (files ending with the `.groovy` extension), extracts their names
     * (excluding the file extension), and groups them into categories based on the directory structure.
     * Each group corresponds to a directory path relative to the root directory.
     * <p>
     * If the root directory does not exist or contains no valid files, an empty map is returned.
     *
     * @return a map where the keys are group names (relative directory paths) and the values are lists
     *         of script metadata
     */
    public static Map<String, ScriptMetadata> scanScriptsGrouped(ClassLoader loader, String scriptDirectoryName) throws IOException {
        Map<String, ScriptMetadata> grouped = new TreeMap<>();
        Logger logger = LoggerFactory.getLogger(WpsScriptWrapper.class.getName());
        File baseDir = new File(scriptDirectoryName).getAbsoluteFile();
        logger.info("Scanning scripts in directory: " + baseDir);
        if (!baseDir.exists()) {
            logger.warn("Directory does not exist {}, will try to use ClassLoader resources package instead..", baseDir);
            // The location may be stored into the jar not the local file system
            try {
                URL resourceUrl = loader.getResource(scriptDirectoryName);
                if (resourceUrl == null) {
                    throw new IOException("Can't find scripts in Jar files using this URL " + loader.getResource(scriptDirectoryName));
                }
                URI resourcesScriptUri = resourceUrl.toURI();
                walkUri(grouped, logger, resourcesScriptUri, scriptDirectoryName);
            } catch (URISyntaxException | IOException | IllegalArgumentException e) {
                logger.warn("Can't find scripts in Jar files using this URL {}.", loader.getResource(scriptDirectoryName), e);
                return new TreeMap<>();
            }
        } else {
            walkUri(grouped, logger, baseDir.toURI(), scriptDirectoryName);
        }
        int scriptCount = grouped.size();
        if(scriptCount == 0) {
            logger.warn("No scripts found in directory/package: {}", scriptDirectoryName);
        } else {
            logger.info("Found {} scripts in directory/package: {}", scriptCount, scriptDirectoryName);
        }
        return grouped;
    }

    /**
     * Walks through the contents of a given URI, which can point to either a directory in the file system or a location within a JAR file, to find and process Groovy script files.
     * @param grouped a map to store the metadata of found scripts, where the key is a unique identifier for each script and the value is a ScriptMetadata object containing details about the script
     * @param logger a Logger instance for logging information and warnings during the script discovery process
     * @param resourcesScriptUri a URI pointing to the location to be scanned for Groovy script files; this can be a file system path or a JAR file path
     * @param scriptDirectoryName the name of the script directory being scanned, used for logging purposes and to ensure correct path handling within JAR files
     * @throws IOException if an I/O error occurs while accessing the URI or reading script files, or if the URI is malformed and cannot be processed correctly
     */

    public static void walkUri(Map<String, ScriptMetadata> grouped, Logger logger, URI resourcesScriptUri, String scriptDirectoryName) throws IOException {
        String scheme = resourcesScriptUri.getScheme();

        if ("jar".equals(scheme)) {
            // --- JAR LOGIC script folder not found, use the Jar file system to read the scripts ---
            String uriStr = resourcesScriptUri.toString();
            String[] parts = uriStr.split("!/");
            URI jarUri = URI.create(parts[0]);
            String absoluteScriptPath = scriptDirectoryName.startsWith("/") ? scriptDirectoryName : "/" + scriptDirectoryName;

            FileSystem fileSystem = null;
            try {
                try {
                    fileSystem = FileSystems.getFileSystem(jarUri);
                } catch (FileSystemNotFoundException e) {
                    fileSystem = FileSystems.newFileSystem(jarUri, Collections.emptyMap());
                }
                Path basePath = fileSystem.getPath(absoluteScriptPath);
                scanPath(grouped, logger, basePath, resourcesScriptUri);
            } finally {
                // Keep open if scripts are read lazily, otherwise close.
                if (fileSystem != null) { fileSystem.close(); }
            }
        } else {
            // Regular file system logic
            // Paths.get(URI) handles "file:/Users/..." correctly
            Path basePath = Paths.get(resourcesScriptUri);
            scanPath(grouped, logger, basePath, resourcesScriptUri);
        }
    }

    /**
     * Shared logic to walk a Path (regardless of FileSystem type) and extract metadata.
     */
    private static void scanPath(Map<String, ScriptMetadata> grouped, Logger logger, Path basePath, URI originUri) throws IOException {
        if (!Files.exists(basePath)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(basePath)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".groovy") && !p.endsWith("package-info.groovy"))
                    .forEach(p -> {
                        try {
                            // Relativize identifies the folder structure inside "scripts"
                            Path relativePath = basePath.relativize(p);

                            // Get parent folder name as the "group"
                            Path parentPath = relativePath.getParent();
                            String group = (parentPath != null) ? parentPath.toString() : "";

                            // Normalize path separators for the Metadata object (always use /)
                            group = group.replace("\\", "/");

                            ScriptMetadata script = new ScriptMetadata(group, p.toUri(), originUri);
                            grouped.put(script.id, script);

                            logger.debug("Loaded script: {} in group: {}", relativePath, group);
                        } catch (Exception e) {
                            logger.warn("Error while loading script metadata for file: {}", p, e);
                        }
                    });
        }
    }
}


