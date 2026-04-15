/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Manage webserver configuration
 */
public class Configuration {
    public static final int DEFAULT_PORT = 8000;
    public static final String DEFAULT_APPLICATION_URL = "noisemodelling";
    public static final String DEFAULT_APPLICATION_PROXY_URL = "http://localhost";
    public static final String NOISE_MODELLING_WEB_SERVER = "NoiseModelling Web Server";
    /** Application context url */
    String applicationRootUrl = DEFAULT_APPLICATION_URL;
    /** Proxy url of the application */
    String applicationProxyBaseUrl = DEFAULT_APPLICATION_PROXY_URL;
    String scriptPath = "scripts";
    final boolean unsecure;
    boolean skipOpenBrowser = false;
    String workingDirectory = Path.of(System.getProperty("user.home"), ".noisemodelling").toString();
    // secureBase is the h2 database that store web application critical data
    // it is not associated with any noisemodelling data
    String secureBaseEncryptionSecret = "";
    String secureBaseAdminUser = "sa";
    String secureBaseAdminPassword = "sa";
    int port = DEFAULT_PORT;
    Map<String, Object> customConfiguration = new HashMap<String, Object>();


    public Configuration(boolean unsecure) {
        this.unsecure = unsecure;
    }

    /**
     * Creates a Configuration object from command-line arguments (backward compatible entry point).
     * Delegates to {@link #createConfigurationFromCommandLine(String[], Options)} using default options.
     *
     * @param args command-line arguments
     * @return Configuration with properties set according to command-line arguments
     * @throws IllegalArgumentException if required options are missing or invalid
     */
    public static Configuration createConfigurationFromArguments(String[] args) throws IllegalArgumentException {
        return createConfigurationFromCommandLine(args, buildOptions());
    }

    /**
     * Build the CLI Options definition used for both CLI and JSON parsing.
     * Required flags defined here are also enforced when parsing from JSON.
     */
    public static Options buildOptions() {
        Options options = new Options();

        Option workingDirOption = new Option("w", "working-dir", true,
                "Path were the application have writing rights to store sessions data");
        workingDirOption.setArgName("folder path");
        options.addOption(workingDirOption);

        Option helpOption = new Option("h", "help", false, "Show this help message");
        options.addOption(helpOption);

        Option scriptPathOption = new Option("s", "script", true, "Path and file name of the script");
        scriptPathOption.setArgName("script path");
        options.addOption(scriptPathOption);

        Option unsecureOption = new Option("u", "unsecure", false, "Disable TOTP, visitors can run any process");
        options.addOption(unsecureOption);

        Option secureBaseEncryptionSecret = new Option("e", "encryption-secret", true,
                "If provided will encrypt the webserver h2 database with this secret");
        options.addOption(secureBaseEncryptionSecret);

        Option applicationRootUrlOption = new Option("r", "root-url", true, "Custom root URL for the web application (default " + DEFAULT_APPLICATION_URL+ " )");
        options.addOption(applicationRootUrlOption);

        Option portOption = new Option("p", "port", true, "Server http serve port (default " + DEFAULT_PORT+ " )");
        portOption.setType(Integer.class);
        options.addOption(portOption);

        Option browserNotOpenOption = new Option("b", "browser-skip", false, "Disable open the browser page on startup");
        options.addOption(browserNotOpenOption);

        Option applicationProxyBaseUrlOption = new Option("l", "proxy-base-url", true, "Custom root URL for the web application (ex: http://myservice.org)");
        options.addOption(applicationProxyBaseUrlOption);

        return options;
    }

    /**
     * Creates a Configuration object from command-line arguments using the provided Options definition.
     *
     * @param args command-line arguments
     * @param options Apache Commons CLI options definition
     * @return Configuration configured from CLI
     * @throws IllegalArgumentException if parsing fails
     */
    public static Configuration createConfigurationFromCommandLine(String[] args, Options options)
            throws IllegalArgumentException {
        Logger logger = LoggerFactory.getLogger(Configuration.class.getName());

        CommandLineParser commandLineParser = new DefaultParser();
        HelpFormatter helpFormatter = HelpFormatter.builder()
                .setPrintWriter(new PrintWriter(new LoggerWriter(logger))).get();
        // Check if -h or --help argument is present
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
            helpFormatter.printHelp(NOISE_MODELLING_WEB_SERVER, options);
            return null;
        }
        try {
            CommandLine commandLine = commandLineParser.parse(options, args, true);
            Configuration config = new Configuration(commandLine.hasOption("u"));
            // Map options to fields
            if (commandLine.hasOption("s")) {
                config.scriptPath = commandLine.getOptionValue("s");
            }
            if (commandLine.hasOption("w")) {
                config.workingDirectory = commandLine.getOptionValue("w");
            }
            if (commandLine.hasOption("e")) {
                config.secureBaseEncryptionSecret = commandLine.getOptionValue("e");
            }
            if (commandLine.hasOption("r")) {
                config.applicationRootUrl = commandLine.getOptionValue("r");
            }
            if (commandLine.hasOption("p")) {
                config.port = Integer.parseInt(commandLine.getOptionValue("p"));
            }
            if (commandLine.hasOption("b")) {
                config.skipOpenBrowser = true;
            }
            if(commandLine.hasOption("l")) {
                config.applicationProxyBaseUrl = commandLine.getOptionValue("l");
            }
            return config;
        } catch (ParseException ex) {
            helpFormatter.printHelp(NOISE_MODELLING_WEB_SERVER, options);
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /**
     * A Writer implementation that redirects output to an SLF4J Logger instance.
     *
     * This class is useful for redirecting output from a Writer to a logging framework.
     * It buffers the output and logs it when the buffer is flushed or closed.
     */
    public static class LoggerWriter extends Writer {

        /**
         * The SLF4J Logger instance to which output will be redirected.
         */
        private final Logger logger;

        /**
         * A buffer to store the output before it is logged.
         */
        private StringBuilder buffer = new StringBuilder();

        /**
         * Constructs a new LoggerWriter instance that redirects output to the specified Logger.
         *
         * @param logger the SLF4J Logger instance to which output will be redirected
         */
        public LoggerWriter(Logger logger) {
            this.logger = logger;
        }

        /**
         * Writes a portion of an array of characters to the buffer.
         *
         * @param cbuf the array of characters to write
         * @param off the offset from which to start writing
         * @param len the number of characters to write
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            buffer.append(cbuf, off, len);
        }

        /**
         * Flushes the buffer and logs its contents to the Logger.
         *
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void flush() throws IOException {
            if (buffer.length() > 0) {
                logger.info(buffer.toString());
                buffer = new StringBuilder();
            }
        }

        /**
         * Closes the Writer, but does not perform any actual closing operation.
         *
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void close() throws IOException {
            // do nothing
        }
    }

    /**
     * Returns the application root URL.
     *
     * @return the application root URL
     */
    public String getApplicationRootUrl() {
        return applicationRootUrl;
    }

    /**
     * Sets the application root URL.
     *
     * @param applicationRootUrl the application root URL to set
     */
    public void setApplicationRootUrl(String applicationRootUrl) {
        this.applicationRootUrl = applicationRootUrl;
    }

    /**
     * Returns the script path.
     *
     * @return the script path
     */
    public String getScriptPath() {
        return scriptPath;
    }

    /**
     * Sets the script path.
     *
     * @param scriptPath the script path to set
     */
    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    /**
     * Returns whether the server is in unsecure mode.
     *
     * @return true if unsecure mode is enabled, false otherwise
     */
    public boolean isUnsecure() {
        return unsecure;
    }

    /**
     * Returns the working directory path.
     *
     * @return the working directory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the working directory path.
     *
     * @param workingDirectory the working directory to set
     */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Returns the encryption secret for the secure base.
     *
     * @return the secure base encryption secret
     */
    public String getSecureBaseEncryptionSecret() {
        return secureBaseEncryptionSecret;
    }

    /**
     * Sets the encryption secret for the secure base.
     *
     * @param secureBaseEncryptionSecret the encryption secret to set
     */
    public void setSecureBaseEncryptionSecret(String secureBaseEncryptionSecret) {
        this.secureBaseEncryptionSecret = secureBaseEncryptionSecret;
    }

    /**
     * Returns the secure base admin user.
     *
     * @return the secure base admin user
     */
    public String getSecureBaseAdminUser() {
        return secureBaseAdminUser;
    }

    /**
     * Sets the secure base admin user.
     *
     * @param secureBaseAdminUser the admin user to set
     */
    public void setSecureBaseAdminUser(String secureBaseAdminUser) {
        this.secureBaseAdminUser = secureBaseAdminUser;
    }

    /**
     * Returns the secure base admin password.
     *
     * @return the secure base admin password
     */
    public String getSecureBaseAdminPassword() {
        return secureBaseAdminPassword;
    }

    /**
     * Sets the secure base admin password.
     *
     * @param secureBaseAdminPassword the admin password to set
     */
    public void setSecureBaseAdminPassword(String secureBaseAdminPassword) {
        this.secureBaseAdminPassword = secureBaseAdminPassword;
    }

    /**
     * Returns the custom configuration map.
     *
     * @return the custom configuration
     */
    public Map<String, Object> getCustomConfiguration() {
        return customConfiguration;
    }

    /**
     * Sets the custom configuration map.
     *
     * @param customConfiguration the custom configuration to set
     */
    public void setCustomConfiguration(Map<String, Object> customConfiguration) {
        this.customConfiguration = customConfiguration;
    }

    /**
     * @return Server port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return Return the full website url to type in the browser window
     */
    public String getWebSiteFullUrl() {
        return (Objects.equals(applicationProxyBaseUrl, Configuration.DEFAULT_APPLICATION_PROXY_URL)
                ? Configuration.DEFAULT_APPLICATION_PROXY_URL + ":" + port : applicationProxyBaseUrl)
                + (applicationRootUrl.isEmpty() ? "" : "/" + applicationRootUrl);
    }

    /**
     * @param port Server port
     */
    public void setPort(int port) {
        this.port = port;
    }
}
