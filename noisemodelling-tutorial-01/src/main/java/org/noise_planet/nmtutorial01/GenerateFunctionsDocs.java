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

package org.noise_planet.nmtutorial01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Groovy WPS scripts and generates RST documentation files for each script,
 * as well as the main Functions.rst index.
 * <p>
 * Both this class and {@link GenerateReferenceDeviation} are bound to the Maven {@code package} phase
 * via the exec-maven-plugin in noisemodelling-tutorial-01/pom.xml.
 */
public class GenerateFunctionsDocs {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateFunctionsDocs.class);

    private static final String AUTO_GENERATED_HEADER =
            ".. DO NOT UPDATE THIS FILE!!\n" +
            ".. This document has been automatically generated with " +
            "noisemodelling-tutorial-01/src/main/java/org/noise_planet/nmtutorial01/GenerateFunctionsDocs.java\n\n";

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    /**
     * @param args args[0] = path to Docs directory (default: "Docs"),
     *             args[1] = path to groovy scripts root directory
     *             (default: "../noisemodelling-scripts/src/main/groovy/org/noise_planet/noisemodelling/scripts")
     */
    public static void main(String[] args) throws IOException {
        String docsDir = "Docs";
        String scriptsDir = "../noisemodelling-scripts/src/main/groovy/org/noise_planet/noisemodelling/scripts";
        if (args.length > 0) {
            docsDir = args[0];
        }
        if (args.length > 1) {
            scriptsDir = args[1];
        }

        File docsDirFile = new File(docsDir).getAbsoluteFile();
        File scriptsDirFile = new File(scriptsDir).getAbsoluteFile();

        if (!docsDirFile.exists()) {
            LOGGER.error("Docs directory does not exist: {}", docsDirFile);
            return;
        }
        if (!scriptsDirFile.exists()) {
            LOGGER.error("Scripts directory does not exist: {}", scriptsDirFile);
            return;
        }

        // Derive wps_images source directory (in noisemodelling-scripts/src/main/resources)
        File wpsImagesDir = scriptsDirFile
                .getParentFile() // noisemodelling
                .getParentFile() // noise_planet
                .getParentFile() // org
                .getParentFile() // groovy
                .getParentFile() // main
                .toPath()
                .resolve("resources/org/noise_planet/noisemodelling/webserver/static/wpsbuilder/wps_images")
                .toFile();
        if (!wpsImagesDir.exists()) {
            LOGGER.warn("wps_images directory not found at {}, images will not be copied", wpsImagesDir);
            wpsImagesDir = null;
        }

        // Ordered map: category name -> list of script base names (sorted)
        Map<String, List<String>> categories = new LinkedHashMap<>();

        File[] categoryDirs = scriptsDirFile.listFiles(File::isDirectory);
        if (categoryDirs == null) {
            LOGGER.warn("No category directories found in {}", scriptsDirFile);
            return;
        }
        Arrays.sort(categoryDirs, Comparator.comparing(File::getName));

        File functionsDocDir = new File(docsDirFile, "functions");
        functionsDocDir.mkdirs();

        for (File categoryDir : categoryDirs) {
            String categoryName = categoryDir.getName();
            File[] groovyFiles = categoryDir.listFiles(f -> f.getName().endsWith(".groovy"));
            if (groovyFiles == null || groovyFiles.length == 0) {
                continue;
            }
            Arrays.sort(groovyFiles, Comparator.comparing(File::getName));

            List<String> scriptNames = new ArrayList<>();
            File categoryDocDir = new File(functionsDocDir, categoryName);
            categoryDocDir.mkdirs();

            for (File groovyFile : groovyFiles) {
                String scriptBaseName = groovyFile.getName().replace(".groovy", "");
                GroovyScriptInfo info = parseGroovyScript(groovyFile);

                // Copy referenced wps_images to category doc dir so RST .. figure:: directives resolve
                if (wpsImagesDir != null) {
                    String rawContent = new String(Files.readAllBytes(groovyFile.toPath()), StandardCharsets.UTF_8);
                    Matcher imgRef = Pattern.compile("src=\"wps_images/([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(rawContent);
                    while (imgRef.find()) {
                        String imgName = imgRef.group(1);
                        File imgSrc = new File(wpsImagesDir, imgName);
                        File imgDst = new File(categoryDocDir, imgName);
                        if (imgSrc.exists()) {
                            Files.copy(imgSrc.toPath(), imgDst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            LOGGER.warn("Image not found in wps_images: {}", imgName);
                        }
                    } // end while imgRef
                }

                String rstContent = generateFunctionRst(categoryName, scriptBaseName, info);
                File rstFile = new File(categoryDocDir, scriptBaseName + ".rst");
                try (FileWriter fw = new FileWriter(rstFile, StandardCharsets.UTF_8)) {
                    fw.write(rstContent);
                }
                LOGGER.info("Written: {}", rstFile.getAbsolutePath());
                scriptNames.add(scriptBaseName);
            }
            categories.put(categoryName, scriptNames);
        }

        // Write main Functions.rst
        File functionsRst = new File(docsDirFile, "Functions.rst");
        try (FileWriter fw = new FileWriter(functionsRst, StandardCharsets.UTF_8)) {
            fw.write(generateFunctionsIndex(categories));
        }
        LOGGER.info("Functions index written to {}", functionsRst.getAbsolutePath());
    }

    // -------------------------------------------------------------------------
    // Groovy script parser
    // -------------------------------------------------------------------------

    static GroovyScriptInfo parseGroovyScript(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        List<String> lines = Arrays.asList(content.split("\r?\n", -1));

        GroovyScriptInfo info = new GroovyScriptInfo();
        info.title = extractTitle(lines);
        info.description = extractDescription(lines);

        String inputsBlock = extractTopLevelBlock(content, "inputs");
        String outputsBlock = extractTopLevelBlock(content, "outputs");

        List<ParameterInfo> allInputs = parseParameterMap(inputsBlock);
        for (ParameterInfo p : allInputs) {
            if (p.optional) {
                info.optionalInputs.add(p);
            } else {
                info.mandatoryInputs.add(p);
            }
        }
        info.outputs = parseParameterMap(outputsBlock);

        return info;
    }

    /** Extract title = '...' or title = "..." (single line) */
    private static String extractTitle(List<String> lines) {
        Pattern p = Pattern.compile("^\\s*title\\s*=\\s*['\"](.+?)['\"]\\s*$");
        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                return m.group(1).trim();
            }
        }
        return "";
    }

    /**
     * Extract description = '...' + '...' (multi-line Groovy concatenation with +).
     * Accumulates lines as long as the non-comment content ends with '+'.
     * Handles both single-quote and double-quote string literals.
     */
    private static String extractDescription(List<String> lines) {
        boolean inDescription = false;
        StringBuilder sb = new StringBuilder();

        Pattern startPattern = Pattern.compile("^\\s*description\\s*=\\s*(.+)$");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!inDescription) {
                Matcher m = startPattern.matcher(line);
                if (m.matches()) {
                    inDescription = true;
                    String rest = m.group(1).trim();
                    sb.append(extractStringContent(rest));
                    if (!rest.trim().endsWith("+")) {
                        break;
                    }
                }
            } else {
                String trimmed = line.trim();
                sb.append(extractStringContent(trimmed));
                if (!trimmed.endsWith("+")) {
                    break;
                }
            }
        }
        return sb.toString().trim();
    }

    /**
     * From a line fragment that may contain one or more quoted string literals and '+' operators,
     * extract all string content using character-by-character scanning (no regex, no recursion).
     */
    private static String extractStringContent(String fragment) {
        String s = fragment.trim();
        if (s.endsWith("+")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\'' || c == '"') {
                char quote = c;
                i++;
                while (i < s.length()) {
                    char d = s.charAt(i);
                    if (d == '\\') {
                        i++;
                        if (i < s.length()) sb.append(s.charAt(i));
                    } else if (d == quote) {
                        break;
                    } else {
                        sb.append(d);
                    }
                    i++;
                }
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * Extracts the full bracket-delimited block for a top-level Groovy variable assignment like:
     * {@code inputs = [ ... ]} or {@code outputs = [ ... ]}
     * Uses bracket-depth counting on '[' and ']', while tracking whether we are inside a string literal.
     */
    private static String extractTopLevelBlock(String content, String varName) {
        // Find the assignment: varName = [
        Pattern assignPattern = Pattern.compile("(?m)^\\s*" + Pattern.quote(varName) + "\\s*=\\s*\\[");
        Matcher m = assignPattern.matcher(content);
        if (!m.find()) {
            return "";
        }
        int blockStart = m.end() - 1; // position of the opening '['
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        StringBuilder sb = new StringBuilder();
        for (int i = blockStart; i < content.length(); i++) {
            char c = content.charAt(i);
            char prev = i > 0 ? content.charAt(i - 1) : 0;

            if (!inDoubleQuote && c == '\'' && prev != '\\') {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && c == '"' && prev != '\\') {
                inDoubleQuote = !inDoubleQuote;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        sb.append(c);
                        break;
                    }
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Parse the parameter map block (content between the outer brackets).
     * Each top-level entry looks like:
     * <pre>
     *   paramName : [
     *       name : '...',
     *       title : '...',
     *       description: '...' +
     *                    '...',
     *       min: 0, max: 1,
     *       type: String.class
     *   ],
     * </pre>
     */
    private static List<ParameterInfo> parseParameterMap(String block) {
        List<ParameterInfo> params = new ArrayList<>();
        if (block == null || block.isEmpty()) {
            return params;
        }

        // Strip outer brackets
        String inner = block.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);

        // Split into top-level entries using bracket-depth tracking
        List<String> entries = splitTopLevelEntries(inner);

        for (String entry : entries) {
            entry = entry.trim();
            if (entry.isEmpty()) continue;

            // Find parameter name (first identifier before ':')
            int colonIdx = entry.indexOf(':');
            if (colonIdx < 0) continue;
            String paramName = entry.substring(0, colonIdx).trim()
                    .replaceAll("^['\"]|['\"]$", ""); // remove quotes if any

            // Extract the nested map value
            int bracketStart = entry.indexOf('[', colonIdx);
            if (bracketStart < 0) continue;
            String paramBlock = extractNestedBlock(entry, bracketStart);

            ParameterInfo pi = new ParameterInfo();
            pi.name = paramName;
            pi.description = extractFieldValue(paramBlock, "description");
            pi.typeName = normalizeTypeName(extractFieldValue(paramBlock, "type"));
            pi.defaultValue = extractDefaultFromDescription(pi.description);
            pi.optional = isOptional(paramBlock);
            params.add(pi);
        }
        return params;
    }

    /**
     * Split the content of a map literal into its top-level key:value entries,
     * respecting bracket nesting and string literals.
     */
    private static List<String> splitTopLevelEntries(String content) {
        List<String> entries = new ArrayList<>();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int start = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            char prev = i > 0 ? content.charAt(i - 1) : 0;

            if (!inDoubleQuote && c == '\'' && prev != '\\') {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && c == '"' && prev != '\\') {
                inDoubleQuote = !inDoubleQuote;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '[' || c == '(') depth++;
                else if (c == ']' || c == ')') depth--;
                else if (c == ',' && depth == 0) {
                    String entry = content.substring(start, i).trim();
                    // A top-level entry must contain a nested bracket (the param sub-map)
                    // Simple comma-separated items inside a nested map are not top-level entries.
                    // We collect here and filter later.
                    if (!entry.isEmpty()) {
                        entries.add(entry);
                    }
                    start = i + 1;
                }
            }
        }
        String last = content.substring(start).trim();
        if (!last.isEmpty()) {
            entries.add(last);
        }
        return entries;
    }

    /** Extract the nested '[...]' block starting at position startIdx. */
    private static String extractNestedBlock(String content, int startIdx) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder sb = new StringBuilder();

        for (int i = startIdx; i < content.length(); i++) {
            char c = content.charAt(i);
            char prev = i > 0 ? content.charAt(i - 1) : 0;

            if (!inDoubleQuote && c == '\'' && prev != '\\') {
                inSingleQuote = !inSingleQuote;
            } else if (!inSingleQuote && c == '"' && prev != '\\') {
                inDoubleQuote = !inDoubleQuote;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        sb.append(c);
                        break;
                    }
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Extract the value of a named field from a parameter block, handling multi-line
     * Groovy '+' string concatenation. The field name is case-insensitive.
     * Uses line-by-line scanning to avoid StackOverflowError on large descriptions.
     */
    private static String extractFieldValue(String block, String fieldName) {
        String[] lines = block.split("\r?\n", -1);
        StringBuilder result = new StringBuilder();
        boolean collecting = false;

        Pattern fieldStart = Pattern.compile(
                "\\b" + Pattern.quote(fieldName) + "\\s*:\\s*(.*)",
                Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            if (!collecting) {
                Matcher m = fieldStart.matcher(line);
                if (m.find()) {
                    collecting = true;
                    String rest = m.group(1).trim();
                    result.append(extractStringContent(rest));
                    if (!rest.endsWith("+")) {
                        break;
                    }
                }
            } else {
                String trimmed = line.trim();
                result.append(extractStringContent(trimmed));
                if (!trimmed.endsWith("+")) {
                    break;
                }
            }
        }
        return result.toString();
    }

    /**
     * Returns true if the parameter block contains {@code min  :  0} (optional parameter).
     */
    private static boolean isOptional(String block) {
        Pattern p = Pattern.compile("\\bmin\\s*:\\s*0\\b");
        return p.matcher(block).find();
    }

    /**
     * Normalize the Groovy type reference to a clean Java type name.
     * Case-insensitive: handles {@code String.class}, {@code string.class}, {@code INTEGER.class}, etc.
     */
    private static String normalizeTypeName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Remove .class suffix
        String name = raw.replaceAll("(?i)\\.class", "").trim();
        // Map to canonical names
        switch (name.toLowerCase(Locale.ROOT)) {
            case "string":    return "String";
            case "double":    return "Double";
            case "float":     return "Float";
            case "integer":
            case "int":       return "Integer";
            case "long":      return "Long";
            case "boolean":   return "Boolean";
            case "geometry":  return "Geometry";
            case "object":    return "Object";
            default:
                // Return the simple class name (last segment after '.')
                int lastDot = name.lastIndexOf('.');
                return lastDot >= 0 ? name.substring(lastDot + 1) : name;
        }
    }

    /**
     * Attempt to extract a default value from the description text.
     * Looks for the pattern {@code &#128736; Default value: <b>X</b>} (after HTML stripping).
     */
    private static String extractDefaultFromDescription(String htmlDescription) {
        // After HTML strip this looks like: "Default value: X"
        // But we work on the raw HTML here.
        // Match: &#128736; Default value: <b>VALUE</b>
        Pattern p = Pattern.compile("(?i)default\\s*value\\s*:\\s*<b>([^<]+)</b>");
        Matcher m = p.matcher(htmlDescription);
        if (m.find()) {
            return m.group(1).trim();
        }
        // Fallback: "Default value: VALUE" without tags
        Pattern p2 = Pattern.compile("(?i)default\\s*value\\s*:\\s*([\\w.\\-]+)");
        Matcher m2 = p2.matcher(htmlDescription);
        if (m2.find()) {
            return m2.group(1).trim();
        }
        return null;
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
        Pattern imgPat = Pattern.compile("<img\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
        Matcher imgM = imgPat.matcher(s);
        StringBuffer sbImg = new StringBuffer();
        while (imgM.find()) {
            String attrs = imgM.group(1);
            Matcher srcM = Pattern.compile("src=\"wps_images/([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(attrs);
            if (srcM.find()) {
                String filename = srcM.group(1);
                Matcher altM = Pattern.compile("alt=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(attrs);
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
        s = s.replace("&amp;", "&");
        s = s.replace("&lt;", "<");
        s = s.replace("&gt;", ">");
        s = s.replace("&quot;", "\"");
        s = s.replace("&apos;", "'");
        s = s.replace("&nbsp;", " ");
        s = s.replace("&le;", "≤");
        s = s.replace("&ge;", "≥");
        s = s.replace("&deg;", "°");
        s = s.replace("&#176;", "°");
        s = decodeNumericEntities(s);


        // Step 6: Remove "Default value: ..." sentences from the description body.
        // They will be displayed separately as "Default: ``...``" by appendParameterRst.
        // Matches the tool emoji (🛠 = U+1F6E0) optionally, then "Default value: VALUE"
        // where VALUE ends at a sentence boundary (period, </br>, newline, or end of string).
        s = s.replaceAll("(?i)\\S*\\s*Default value\\s*:.*?((?=[<\n])|$)", "");

        // Step 7: Normalize whitespace.
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
        Pattern hex = Pattern.compile("&#x([0-9a-fA-F]+);");
        Matcher mh = hex.matcher(s);
        StringBuffer sbHex = new StringBuffer();
        while (mh.find()) {
            int cp = Integer.parseInt(mh.group(1), 16);
            mh.appendReplacement(sbHex, Matcher.quoteReplacement(new String(Character.toChars(cp))));
        }
        mh.appendTail(sbHex);
        s = sbHex.toString();

        // Decimal entities
        Pattern dec = Pattern.compile("&#([0-9]+);");
        Matcher md = dec.matcher(s);
        StringBuffer sbDec = new StringBuffer();
        while (md.find()) {
            int cp = Integer.parseInt(md.group(1));
            md.appendReplacement(sbDec, Matcher.quoteReplacement(new String(Character.toChars(cp))));
        }
        md.appendTail(sbDec);
        return sbDec.toString();
    }

    // -------------------------------------------------------------------------
    // RST generation
    // -------------------------------------------------------------------------

    /** Generate per-function RST content. */
    static String generateFunctionRst(String category, String scriptName, GroovyScriptInfo info) {
        StringBuilder sb = new StringBuilder();

        sb.append(AUTO_GENERATED_HEADER);

        // Title
        String heading = scriptName.replace("_", " ");
        sb.append(heading).append("\n");
        sb.append("=".repeat(heading.length())).append("\n\n");

        // Script title as subtitle if different
        if (!info.title.isEmpty() && !info.title.equalsIgnoreCase(heading)) {
            sb.append(cleanHtml(info.title)).append("\n\n");
        }

        // Overview
        if (!info.description.isEmpty()) {
            sb.append("Overview\n");
            sb.append("--------\n\n");
            sb.append(cleanHtml(info.description)).append("\n\n");
        }

        // Arguments section — only if there are inputs
        if (!info.mandatoryInputs.isEmpty() || !info.optionalInputs.isEmpty()) {
            sb.append("Arguments\n");
            sb.append("---------\n\n");

            if (!info.mandatoryInputs.isEmpty()) {
                sb.append("Mandatory inputs\n");
                sb.append("~~~~~~~~~~~~~~~~\n\n");
                for (ParameterInfo p : info.mandatoryInputs) {
                    appendParameterRst(sb, p);
                }
            }

            if (!info.optionalInputs.isEmpty()) {
                sb.append("Optional inputs\n");
                sb.append("~~~~~~~~~~~~~~~\n\n");
                for (ParameterInfo p : info.optionalInputs) {
                    appendParameterRst(sb, p);
                }
            }
        }

        // Output
        if (!info.outputs.isEmpty()) {
            sb.append("Output\n");
            sb.append("------\n\n");
            for (ParameterInfo p : info.outputs) {
                appendParameterRst(sb, p);
            }
        }

        // Function signatures
        sb.append("Function Signatures\n");
        sb.append("-------------------\n\n");
        sb.append("The script exposes one entry point:\n\n");
        sb.append("* ``exec(Connection connection, input)``\n");

        return sb.toString();
    }

    private static void appendParameterRst(StringBuilder sb, ParameterInfo p) {
        sb.append("``").append(p.name).append("``\n");

        String desc = cleanHtml(p.description);
        if (!desc.isEmpty()) {
            // Indent description by 3 spaces
            for (String line : desc.split("\n", -1)) {
                sb.append("   ").append(line).append("\n");
            }
        }

        if (!p.typeName.isEmpty()) {
            sb.append("\n   Type: ``").append(p.typeName).append("``\n");
        }
        if (p.defaultValue != null && !p.defaultValue.isEmpty()) {
            sb.append("\n   Default: ``").append(p.defaultValue).append("``\n");
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

        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            String category = entry.getKey();
            List<String> scripts = entry.getValue();

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

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    static class GroovyScriptInfo {
        String title = "";
        String description = "";
        List<ParameterInfo> mandatoryInputs = new ArrayList<>();
        List<ParameterInfo> optionalInputs = new ArrayList<>();
        List<ParameterInfo> outputs = new ArrayList<>();
    }

    static class ParameterInfo {
        String name = "";
        String description = "";
        String typeName = "";
        String defaultValue = null;
        boolean optional = false;
    }
}
