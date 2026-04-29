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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
     * Loads Groovy scripts from a predefined directory structure and organizes them into groups.
     *
     * This method scans the available scripts using the `scanScriptsGrouped` method to organize
     * them by groups, then attempts to locate the corresponding script files for each script
     * name in the directory structure. Only valid script files that exist on the file system
     * are included in the resulting map.
     *
     * @return a map where the keys are script group names and the values are lists of
     *         File objects corresponding to the scripts in each group
     */
    public  Map<String, List<File>> loadScripts(){
        return scanScriptsGrouped(getClass().getClassLoader(), scriptsRoot);
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
     *         of script names (without file extensions) belonging to each group
     */
    public static Map<String, List<File>> scanScriptsGrouped(ClassLoader loader, Path scriptDirectory) {
        Map<String, List<File>> grouped = new TreeMap<>();
        File baseDir = scriptDirectory.toFile();
        Logger logger = LoggerFactory.getLogger(WpsScriptWrapper.class.getName());
        logger.info("Scanning scripts in directory: " + scriptDirectory.toAbsolutePath());
        if (!baseDir.exists()) {
            logger.warn("Directory does not exist {}, will try to use ClassLoader resources package instead..", scriptDirectory);
            // The location may be stored into the jar not the local file system
            try {
                URL resourceUrl = loader.getResource(scriptDirectory.toString());
                if (resourceUrl == null) {
                    return grouped;
                }
                baseDir = new File(resourceUrl.toURI());
            } catch (URISyntaxException e) {
                return grouped;
            }
            if (!baseDir.exists()) {
                return grouped;
            }
        }
        scanRecursive(baseDir, "", grouped);
        logger.info("Found {} scripts in directory/package: {}", grouped.values().stream().mapToInt(List::size).sum(), scriptDirectory);
        return grouped;
    }


    /**
     * Finds a Groovy script file based on the specified group and script name.
     *
     * This method builds the path to the desired script file by resolving the group
     * and script name against a predefined root directory. If the file exists, it
     * returns a {@code File} object representing the script; otherwise, it returns null.
     *
     * @param group the name of the group or folder containing the script
     *              (relative to the root directory)
     * @param scriptName the name of the script file (without the ".groovy" extension)
     * @return a {@code File} object representing the script file if it exists,
     *         or null if the file does not exist
     */
    public File findScript(String group, String scriptName) {
        Path path = scriptsRoot.resolve(group).resolve(scriptName + ".groovy");
        return Files.exists(path) ? path.toFile() : null;
    }

    /**
     * Recursively scans a directory for Groovy script files and groups them into categories
     * based on the directory structure. Each group corresponds to a directory path relative
     * to the root directory.
     *
     * @param dir the directory to scan for Groovy script files
     * @param currentGroup the current group name, representing the relative path from the root directory
     * @param grouped a map where keys are group names (relative directory paths) and values are lists
     *        of script files that belong to each group
     */
    private static void scanRecursive(File dir, String currentGroup, Map<String, List<File>> grouped) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String newGroup = currentGroup.isEmpty() ? f.getName() : currentGroup + "/" + f.getName();
                scanRecursive(f, newGroup, grouped);
            } else if (f.getName().endsWith(".groovy")) {
                grouped.computeIfAbsent(currentGroup, k -> new ArrayList<>())
                        .add(f);
            }
        }
    }

    /**
     * Builds a list of {@link ScriptMetadata} objects from the Groovy scripts available
     * in the given directory or JAR.
     *
     * <p>This method reads each Groovy script file, parses its metadata (title,
     * description, inputs, and outputs), and wraps it into a {@link ScriptMetadata}
     * instance. The resulting list can be used to generate WPS Capabilities and
     * DescribeProcess documents.</p>
     *
     * @param scriptFiles a map of grouped script files (group → list of script files)
     * @return a list of {@code ScriptWrapper} instances representing available scripts
     * @throws IOException if a script file cannot be read or parsed
     */
    public static Map<String, ScriptMetadata> buildScriptWrappers(Map<String, List<File>> scriptFiles) throws IOException {
        Map<String, ScriptMetadata> wrappers = new HashMap<>();
        for (Map.Entry<String, List<File>> entry : scriptFiles.entrySet()) {
            String group = entry.getKey();
            for (File file : entry.getValue()) {
                ScriptMetadata wrapper = new ScriptMetadata(group, file);
                wrappers.put(wrapper.id, wrapper);
            }
        }

        return wrappers;
    }


}


