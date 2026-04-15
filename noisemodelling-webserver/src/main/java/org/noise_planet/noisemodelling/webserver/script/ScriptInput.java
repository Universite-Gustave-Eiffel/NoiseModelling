package org.noise_planet.noisemodelling.webserver.script;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an input configuration for a script.
 * This class is designed to encapsulate the metadata and properties that define
 * an input to a script, such as its identifier, title, description, type, and
 * whether it is optional.
 */
public class ScriptInput {
    public String id;
    public String title;
    public String description;
    public Class<?> type;
    /**
     * Minimum occurrences of this input. A value of 0 indicates that the input is optional.
     */
    public int minOccurs = 1;
    /**
     * Maximum occurrences of this input.
     */
    public int maxOccurs = 1;
    /**
     * Default value for this input.
     */
    public String defaultValue;

    /**
     * If at least one element, restrict allowed values, only for Strings
     */
    public Set<String> allowedValues = new HashSet<>();
}
