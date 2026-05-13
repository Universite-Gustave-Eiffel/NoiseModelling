/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education,
 * as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE
 * provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.webserver.script;

import org.h2.security.SHA256;
import org.noise_planet.noisemodelling.webserver.utilities.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.locationtech.jts.io.WKBWriter.bytesToHex;
import static org.locationtech.jts.io.WKBWriter.toHex;

/**
 * Parses Groovy WPS scripts and generates RST documentation files for each script,
 * as well as the main Functions.rst index.
 * <p>
 * Bound to the Maven {@code package} phase via the exec-maven-plugin in
 * noisemodelling-scripts/pom.xml.
 */
public class GenerateFunctionsDocs {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateFunctionsDocs.class);

    private static final String AUTO_GENERATED_HEADER =
            ".. DO NOT UPDATE THIS FILE!!\n" +
            ".. This document has been automatically generated with " +
            "noisemodelling-scripts/src/main/java/org/noise_planet/noisemodelling/webserver/script/GenerateFunctionsDocs.java\n\n";

    // Compiled patterns — reused across all calls
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("<img\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern WPS_IMAGE_SRC_PATTERN = Pattern.compile("src=\"wps_images/([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMG_ALT_PATTERN = Pattern.compile("alt=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_ENTITY_PATTERN = Pattern.compile("&#x([0-9a-fA-F]+);");
    private static final Pattern DEC_ENTITY_PATTERN = Pattern.compile("&#([0-9]+);");
    private static final Pattern DEFAULT_VALUE_PATTERN = Pattern.compile("(?i)Default value\\s*:[^\n<]*");

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * @param args args[0] = path to Docs directory (default: "../Docs"),
     *             args[1] = path to groovy scripts root directory
     *             (default: "src/main/groovy/org/noise_planet/noisemodelling/scripts")
     */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        Logging.initConsoleLogging();

        String docsDir = "../Docs";
        String scriptsDir = "src/main/groovy/org/noise_planet/noisemodelling/scripts";
        if (args.length > 0) {
            docsDir = args[0];
        }
        if (args.length > 1) {
            scriptsDir = args[1];
        }

        File docsDirFile = new File(docsDir).getCanonicalFile();
        File scriptsDirFile = new File(scriptsDir).getAbsoluteFile();

        if (!docsDirFile.exists()) {
            LOGGER.error("Docs directory does not exist: {}", docsDirFile);
            return;
        }
        if (!scriptsDirFile.exists()) {
            LOGGER.error("Scripts directory does not exist: {}", scriptsDirFile);
            return;
        }

        // Derive wps_images source directory (resources sibling of the groovy source tree)
        File wpsImagesDir = new File("src/main/resources/org/noise_planet/noisemodelling/webserver/static/wpsbuilder/wps_images");
        if (!wpsImagesDir.exists()) {
            LOGGER.warn("wps_images directory not found at {}, images will not be copied", wpsImagesDir);
            wpsImagesDir = null;
        }

        // Discover and parse all scripts using GroovyShell-based ScriptMetadata

        Map<String, ScriptMetadata> metadataMap = new HashMap<>();
        WpsScriptWrapper.walkUri(metadataMap, LOGGER, scriptsDirFile.toURI(), scriptsDir);

        // Ordered map: category name -> list of script base names (sorted)
        Map<String, List<String>> categories = new LinkedHashMap<>();

        File functionsDocDir = new File(docsDirFile, "functions");
        functionsDocDir.mkdirs();

        for (ScriptMetadata scriptMetadata : metadataMap.values()) {
            String categoryName = scriptMetadata.group;

            File categoryDocDir = new File(functionsDocDir, categoryName);
            categoryDocDir.mkdirs();

            // Copy referenced wps_images to the category doc directory so
            // RST .. figure:: directives resolve correctly
            if (wpsImagesDir != null) {
                copyReferencedImages(scriptMetadata, wpsImagesDir, categoryDocDir);
            }
            String scriptFileName = new File(scriptMetadata.path).getName();
            String scriptBaseName = scriptFileName.substring(0, scriptFileName.length() - ".groovy".length());
            String rstContent = generateFunctionRst(scriptBaseName, scriptMetadata);
            File rstFile = new File(categoryDocDir, scriptBaseName + ".rst");

            categories.merge(categoryName, new ArrayList<>(List.of(scriptBaseName)), (existing, newOnes) -> {
                existing.addAll(newOnes);
                existing.sort(String::compareTo);
                return existing;
            });

            // Check if content is different before writing to avoid unnecessary file updates (which would trigger Sphinx rebuilds)
            if (rstFile.exists()) {
                String existingContent = Files.readString(rstFile.toPath(), StandardCharsets.UTF_8);
                if (existingContent.equals(rstContent)) {
                    LOGGER.trace("No changes for {}, skipping write", docsDirFile.toPath().relativize(rstFile.toPath()));
                    continue;
                }
            }
            try (FileWriter fw = new FileWriter(rstFile, StandardCharsets.UTF_8)) {
                fw.write(rstContent);
            }
            LOGGER.info("Written: {}", rstFile.getAbsolutePath());

        }

        // Write main Functions.rst
        File functionsRst = new File(docsDirFile, "Functions.rst");
        try (FileWriter fw = new FileWriter(functionsRst, StandardCharsets.UTF_8)) {
            fw.write(generateFunctionsIndex(categories));
        }
        LOGGER.info("Functions index written to {}", functionsRst.getCanonicalPath());
    }

    /**
     * Scan all description fields of the given metadata for {@code src="wps_images/..."} references
     * and copy the matching images from {@code wpsImagesDir} to {@code targetDir}.
     */
    private static void copyReferencedImages(ScriptMetadata metadata, File wpsImagesDir, File targetDir) throws IOException, NoSuchAlgorithmException {
        List<String> allDescriptions = new ArrayList<>();
        allDescriptions.add(metadata.description);
        for (ScriptInput input : metadata.inputs.values()) {
            if (input.description != null) {
                allDescriptions.add(input.description);
            }
        }
        for (ScriptOutput output : metadata.outputs.values()) {
            if (output.description != null) {
                allDescriptions.add(output.description);
            }
        }

        for (String desc : allDescriptions) {
            if (desc == null || desc.isEmpty()) {
                continue;
            }
            Matcher m = WPS_IMAGE_SRC_PATTERN.matcher(desc);
            while (m.find()) {
                String imgName = m.group(1);
                File imgSrc = new File(wpsImagesDir, imgName);
                File imgDst = new File(targetDir, imgName);
                if (imgSrc.exists()) {
                    // Check if destination file already exists with the same content to avoid unnecessary writes
                    if (imgDst.exists()) {
                        // Using sha256 hash comparison to avoid loading entire files into memory for large images
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        String srcHash = toHex(digest.digest(Files.readAllBytes(imgSrc.toPath())));
                        String dstHash = toHex(digest.digest(Files.readAllBytes(imgDst.toPath())));
                        boolean sameContent = srcHash.equals(dstHash);
                        if (sameContent) {
                            LOGGER.trace("Image {} already exists in {}, skipping copy", imgName, targetDir);
                            continue;
                        }
                    }
                    Files.copy(imgSrc.toPath(), imgDst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    LOGGER.warn("Image not found in wps_images: {}", imgName);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // HTML cleaner
    // -------------------------------------------------------------------------

    /**
     * Strip HTML tags and decode HTML entities to produce plain RST text.
     * <ul>
     *   <li>{@code <b>name</b>} tags are stripped; text content is kept as plain text</li>
     *   <li>{@code <img src="wps_images/X.png">} → RST {@code .. figure::} directive</li>
     *   <li>{@code <br>} / {@code <hr>} / list tags → newlines and {@code *} bullets</li>
     * </ul>
     */
    static String cleanHtml(String html) {
        if (html == null || html.isEmpty()) return "";

        String s = html;

        // Step 1: Strip <b>/<strong> tags (keep their text content as plain text)
        s = s.replaceAll("(?i)</?(?:b|strong)>", "");

        // Step 2: Replace block-level tags with newlines / bullets
        s = s.replaceAll("(?i)<hr[^>]*>", "\n");
        s = s.replaceAll("(?i)<br[^>]*/?>", "\n");
        s = s.replaceAll("(?i)</li>", "\n");
        s = s.replaceAll("(?i)<li[^>]*>", "\n* ");
        s = s.replaceAll("(?i)<ul[^>]*>", "\n");
        s = s.replaceAll("(?i)</ul>", "\n");
        s = s.replaceAll("(?i)<ol[^>]*>", "\n");
        s = s.replaceAll("(?i)</ol>", "\n");
        s = s.replaceAll("(?i)<p[^>]*>", "\n");
        s = s.replaceAll("(?i)</p>", "\n");

        // Step 2.5: Convert <img src="wps_images/X.png" alt="ALT" ...> to RST figure directive.
        // The image file must be copied alongside the RST (handled in main()).
        Matcher imgM = IMG_TAG_PATTERN.matcher(s);
        StringBuilder sbImg = new StringBuilder();
        while (imgM.find()) {
            String attrs = imgM.group(1);
            Matcher srcM = WPS_IMAGE_SRC_PATTERN.matcher(attrs);
            if (srcM.find()) {
                String filename = srcM.group(1);
                Matcher altM = IMG_ALT_PATTERN.matcher(attrs);
                String alt = altM.find() ? altM.group(1) : filename;
                String figure = "\n\n.. figure:: " + filename + "\n   :align: center\n   :alt: " + alt + "\n\n";
                imgM.appendReplacement(sbImg, Matcher.quoteReplacement(figure));
            } else {
                imgM.appendReplacement(sbImg, "");
            }
        }
        imgM.appendTail(sbImg);
        s = sbImg.toString();

        // Step 3: Strip remaining tags
        s = s.replaceAll("<[^>]+>", "");

        // Step 4: Decode named entities
        // Use concatenation to prevent editor formatters from corrupting HTML entity strings
        String _amp = "&" + "amp;";
        String _lt = "&" + "lt;";
        String _gt = "&" + "gt;";
        String _quot = "&" + "quot;";
        String _apos = "&" + "apos;";
        String _nbsp = "&" + "nbsp;";
        String _le = "&" + "le;";
        String _ge = "&" + "ge;";
        String _deg = "&" + "deg;";
        s = s.replace(_amp, "&");
        s = s.replace(_lt, "<");
        s = s.replace(_gt, ">");
        s = s.replace(_quot, "\"");
        s = s.replace(_apos, "'");
        s = s.replace(_nbsp, " ");
        s = s.replace(_le, "≤");
        s = s.replace(_ge, "≥");
        s = s.replace(_deg, "°");
        s = decodeNumericEntities(s);

        // Step 5: Remove "Default value: ..." sentences from the description body.
        // They will be displayed separately as "Default: ``...``" by appendParameterRst.
        s = DEFAULT_VALUE_PATTERN.matcher(s).replaceAll("");

        // Step 6: Normalize whitespace.
        // Strip leading+trailing spaces per line, EXCEPT for RST directive option lines
        // (indented lines starting with ':') which must keep their leading spaces.
        String[] lines = s.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int consecutiveBlanks = 0;
        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.isEmpty()) {
                consecutiveBlanks++;
                if (consecutiveBlanks <= 1) {
                    sb.append("\n");
                }
            } else {
                consecutiveBlanks = 0;
                // Preserve indentation for RST directive option lines (e.g. "   :align: center")
                String lineToAppend = (stripped.startsWith(":") && line.startsWith(" "))
                        ? line.stripTrailing()
                        : stripped;
                sb.append(lineToAppend).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /** Decode &#NNNN; (decimal) and &#xHHHH; (hex) numeric HTML entities to Unicode. */
    private static String decodeNumericEntities(String s) {
        // Hex entities
        Matcher mh = HEX_ENTITY_PATTERN.matcher(s);
        StringBuilder sbHex = new StringBuilder();
        while (mh.find()) {
            int cp = Integer.parseInt(mh.group(1), 16);
            mh.appendReplacement(sbHex, new String(Character.toChars(cp)));
        }
        mh.appendTail(sbHex);
        s = sbHex.toString();

        // Decimal entities
        Matcher md = DEC_ENTITY_PATTERN.matcher(s);
        StringBuilder sbDec = new StringBuilder();
        while (md.find()) {
            int cp = Integer.parseInt(md.group(1));
            md.appendReplacement(sbDec, new String(Character.toChars(cp)));
        }
        md.appendTail(sbDec);
        return sbDec.toString();
    }

    // -------------------------------------------------------------------------
    // RST generation
    // -------------------------------------------------------------------------

    /** Generate per-function RST content from a {@link ScriptMetadata} instance. */
    static String generateFunctionRst(String scriptName, ScriptMetadata metadata) {
        StringBuilder sb = new StringBuilder();

        sb.append(AUTO_GENERATED_HEADER);

        // Title
        String heading = scriptName.replace("_", " ");
        sb.append(heading).append("\n");
        sb.append("=".repeat(heading.length())).append("\n\n");

        // Compute cleaned title once for reuse
        String cleanTitle = metadata.title.isEmpty() ? "" : cleanHtml(metadata.title);

        // Script title as subtitle if different
        if (!cleanTitle.isEmpty() && !cleanTitle.equalsIgnoreCase(heading)) {
            sb.append(cleanTitle).append("\n\n");
        }

        // Overview
        if (!metadata.description.isEmpty()) {
            String desc = cleanHtml(metadata.description);
            // Avoid duplication: if the description starts with the (cleaned) title, remove the prefix
            if (!cleanTitle.isEmpty() && desc.startsWith(cleanTitle)) {
                desc = desc.substring(cleanTitle.length()).stripLeading();
                // Also strip leading punctuation like ". "
                if (desc.startsWith(".")) {
                    desc = desc.substring(1).stripLeading();
                }
            }
            sb.append("Overview\n");
            sb.append("--------\n\n");
            sb.append(desc).append("\n\n");
        }

        // Split inputs into mandatory (minOccurs > 0) and optional (minOccurs == 0)
        List<ScriptInput> mandatoryInputs = new ArrayList<>();
        List<ScriptInput> optionalInputs = new ArrayList<>();
        for (ScriptInput input : metadata.inputs.values()) {
            if (input.minOccurs == 0) {
                optionalInputs.add(input);
            } else {
                mandatoryInputs.add(input);
            }
        }
        // Sort each group by parameter id for a stable, predictable output
        Comparator<ScriptInput> byId = Comparator.comparing(i -> i.id);
        mandatoryInputs.sort(byId);
        optionalInputs.sort(byId);

        // Arguments section — only if there are inputs
        if (!mandatoryInputs.isEmpty() || !optionalInputs.isEmpty()) {
            sb.append("Arguments\n");
            sb.append("---------\n\n");

            if (!mandatoryInputs.isEmpty()) {
                sb.append("Mandatory inputs\n");
                sb.append("~~~~~~~~~~~~~~~~\n\n");
                for (ScriptInput p : mandatoryInputs) {
                    appendInputRst(sb, p);
                }
            }

            if (!optionalInputs.isEmpty()) {
                sb.append("Optional inputs\n");
                sb.append("~~~~~~~~~~~~~~~\n\n");
                for (ScriptInput p : optionalInputs) {
                    appendInputRst(sb, p);
                }
            }
        }

        // Output
        if (!metadata.outputs.isEmpty()) {
            List<ScriptOutput> outputs = new ArrayList<>(metadata.outputs.values());
            outputs.sort(Comparator.comparing(o -> o.id));
            sb.append("Output\n");
            sb.append("------\n\n");
            for (ScriptOutput p : outputs) {
                appendOutputRst(sb, p);
            }
        }

        return sb.toString();
    }

    private static void appendInputRst(StringBuilder sb, ScriptInput p) {
        sb.append("``").append(p.id).append("``");
        if (p.title != null && !p.title.isEmpty() && !p.title.equals(p.id)) {
            sb.append(" — *").append(p.title).append("*");
        }
        sb.append("\n");

        String desc = cleanHtml(p.description);
        if (!desc.isEmpty()) {
            for (String line : desc.split("\n", -1)) {
                sb.append("   ").append(line).append("\n");
            }
        }

        if (p.type != null) {
            sb.append("\n   Type: ``").append(p.type.getSimpleName()).append("``\n");
        }
        if (p.defaultValue != null) {
            sb.append("\n   Default: ``").append(p.defaultValue).append("``\n");
        }
        if (!p.allowedValues.isEmpty()) {
            sb.append("\n   Allowed values: ``");
            boolean first = true;
            for (String v : p.allowedValues) {
                if (!first) sb.append("``, ``");
                sb.append(v);
                first = false;
            }
            sb.append("``\n");
        }
        sb.append("\n");
    }

    private static void appendOutputRst(StringBuilder sb, ScriptOutput p) {
        sb.append("``").append(p.id).append("``");
        if (p.title != null && !p.title.isEmpty() && !p.title.equals(p.id)) {
            sb.append(" — *").append(p.title).append("*");
        }
        sb.append("\n");

        String desc = cleanHtml(p.description);
        if (!desc.isEmpty()) {
            for (String line : desc.split("\n", -1)) {
                sb.append("   ").append(line).append("\n");
            }
        }

        if (p.type != null) {
            sb.append("\n   Type: ``").append(p.type.getSimpleName()).append("``\n");
        }
        sb.append("\n");
    }

    /** Generate the main Functions.rst index. */
    static String generateFunctionsIndex(Map<String, List<String>> categories) {
        StringBuilder sb = new StringBuilder();

        sb.append(AUTO_GENERATED_HEADER);

        sb.append("List of functions\n");
        sb.append("^^^^^^^^^^^^^^^^^\n\n");
        sb.append("Below is a list of all the functions that can be run in NoiseModelling.\n");
        sb.append("These functions, written as ``.groovy`` scripts, are available in the ``/scripts/`` folder.\n\n");

        for (String category : categories.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new))) {
            List<String> scripts = categories.get(category);

            // Category header — replace underscores with spaces for readability
            String categoryTitle = category.replace("_", " ");
            sb.append(categoryTitle).append("\n");
            sb.append("~".repeat(Math.max(categoryTitle.length(), 5))).append("\n\n");

            sb.append(".. toctree::\n");
            sb.append("    :maxdepth: 1\n\n");
            for (String scriptName : scripts) {
                sb.append("    functions/").append(category).append("/").append(scriptName).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}