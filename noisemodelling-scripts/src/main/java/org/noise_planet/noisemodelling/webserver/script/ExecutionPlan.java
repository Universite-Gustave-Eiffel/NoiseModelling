/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */
package org.noise_planet.noisemodelling.webserver.script;

import java.util.Map;

/**
 * Store the inputs and outputs of a job execution.
 * Inputs values can be an instance of ExecutionPlan for a chained process and will be replaced by the outputs of the previous process.
 */
public class ExecutionPlan {
    protected final Map<String, Object> inputs;
    protected Object outputs;
    protected final ScriptMetadata scriptMetadata;
    // If this plan is referenced as an input of another plan,
    // this is the key of one of these plan outputs that will be used as an input of the parent plan
    protected final String chainedOutputKey;

    /**
     * Create a new ExecutionPlan.
     * @param inputs WPS Scripts inputs
     * @param scriptMetadata Metadata of the script
     */
    public ExecutionPlan(Map<String, Object> inputs, ScriptMetadata scriptMetadata) {
        this.inputs = inputs;
        this.outputs = null;
        this.scriptMetadata = scriptMetadata;
        this.chainedOutputKey = "";
    }

    /**
     * Create a new ExecutionPlan for a chained process.
     * @param inputs WPS Scripts inputs
     * @param scriptMetadata Metadata of the script
     * @param chainedOutputKey the name of the output of this plan used as an input of the parent plan
     */
    public ExecutionPlan(Map<String, Object> inputs, ScriptMetadata scriptMetadata, String chainedOutputKey) {
        this.chainedOutputKey = chainedOutputKey;
        this.inputs = inputs;
        this.outputs = null;
        this.scriptMetadata = scriptMetadata;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }
    public Object getOutputs() {
        return outputs;
    }
    public ScriptMetadata getScriptMetadata() {
        return scriptMetadata;
    }
    public void setOutputs(Object outputs) {
        this.outputs = outputs;
    }

    /**
     * @return the name of the output of this plan used as an input of the parent plan
     */
    public String getChainedOutputKey() {
        return chainedOutputKey;
    }

}
