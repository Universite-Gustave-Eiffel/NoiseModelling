/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver.script;


import groovy.lang.GroovyShell;
import groovy.lang.Script;
import net.opengis.wps10.DataInputsType1;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents the description for a script, with expected inputs and outputs
 */
public class ScriptMetadata {
    // For the synchronous WPS call, release the http connection after this timeout (with a message "long running process..")
    Logger logger = LoggerFactory.getLogger(ScriptMetadata.class);
    public static final int DEFAULT_JOB_EXECUTION_TIMEOUT_SECONDS = 60;
    final public String id;
    final public String title;
    final public String description;
    final public Path path;
    final public int executionTimeoutSeconds;

    final public Map<String, ScriptInput> inputs = new HashMap<>();
    final public Map<String, ScriptOutput> outputs = new HashMap<>();

    public ScriptMetadata(String group, File file) throws IOException {
        Map metadata = parseGroovyScriptMetadata(file);
        id = group + ":" + file.getName().replace(".groovy", "");
        title = metadata.getOrDefault("title", id).toString();
        description = metadata.getOrDefault("description", "").toString();
        executionTimeoutSeconds = (Integer) metadata.getOrDefault("executionTimeout", DEFAULT_JOB_EXECUTION_TIMEOUT_SECONDS);
        path = file.toPath();

        // Convert metadata inputs into ScriptInput instances
        Object inputsValue = metadata.get("inputs");
        if(inputsValue instanceof Map) {
            for (Map.Entry<String, Object> input : ((Map<String, Object>) inputsValue).entrySet()) {
                ScriptInput si = new ScriptInput();
                si.id = input.getKey().toString();
                if(input.getValue() instanceof Map) {
                    Map<String, Object> inputAttributes = (Map)input.getValue();
                    si.title = inputAttributes.getOrDefault("title", input.getKey()).toString();
                    si.description = inputAttributes.getOrDefault("description", "").toString();
                    Object attributeType = inputAttributes.get("type");
                    if(attributeType instanceof Class) {
                        si.type = (Class<?>)attributeType;
                    }
                    Object minValue = inputAttributes.getOrDefault("min", 1);
                    si.minOccurs = minValue instanceof Integer ? (Integer)minValue : 1;
                    Object maxValue = inputAttributes.getOrDefault("max", 1);
                    si.maxOccurs = maxValue instanceof Integer ? (Integer)maxValue : 1;
                    si.defaultValue = inputAttributes.getOrDefault("default", null);
                    // If minOccurs is 0 but no default value is provided
                    // it means that the input map will not have an entry for this input
                    if (inputAttributes.containsKey("default")) {
                        // We can consider that the input is optional as the default value will be used if no value is provided
                        si.minOccurs  = 0;
                    }
                    Object allowedValues = inputAttributes.getOrDefault("allowedValues", new HashSet<>());
                    if(allowedValues instanceof Collection) {
                        si.allowedValues = new HashSet<>((Collection<String>)allowedValues);
                    }
                }
                inputs.put(si.id, si);
            }
        }

        Object outputsValue = metadata.get("outputs");
        if(outputsValue instanceof Map) {
            for (Map.Entry output : ((Map<String, Object>) outputsValue).entrySet()) {
                ScriptOutput scriptOutput = new ScriptOutput();
                scriptOutput.id = output.getKey().toString();
                if(output.getValue() instanceof Map) {
                    Map<String, Object> outputAttributes = (Map) output.getValue();
                    scriptOutput.title = (String) outputAttributes.getOrDefault("title", "no titles");
                    scriptOutput.description = outputAttributes.getOrDefault("description", "").toString();
                    Object attributeType = outputAttributes.get("type");
                    if(attributeType instanceof Class) {
                        scriptOutput.type = (Class<?>)attributeType;
                    }
                }
                outputs.put(scriptOutput.id, scriptOutput);
            }
        }
    }

    /**
     * Parses metadata from a provided Groovy script file and extracts details such as title,
     * description, inputs, and outputs defined within the script. The method analyzes the script
     * content to populate a metadata map, which includes blocks of inputs and outputs if defined.
     *
     * @param scriptFile the Groovy script file to parse for metadata
     * @return a map containing metadata fields such as "title", "description", "inputs", and "outputs",
     * where "inputs" and "outputs" are themselves maps with their respective properties
     * @throws IOException if an error occurs while reading the script file
     */
    private static Map parseGroovyScriptMetadata(File scriptFile) throws IOException {
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse(scriptFile);
        script.run();
        return script.getBinding().getVariables();
    }


}

