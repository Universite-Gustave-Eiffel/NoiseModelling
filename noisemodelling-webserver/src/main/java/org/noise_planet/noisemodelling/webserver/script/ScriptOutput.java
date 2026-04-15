package org.noise_planet.noisemodelling.webserver.script;

/**
 * Represents an output configuration for a script.
 * This class is designed to encapsulate the metadata that defines
 * an output of a script, such as its identifier and title.
 */
public class ScriptOutput {
    public String id;
    public String title;
    public String description;
    public Class<?> type;
}
