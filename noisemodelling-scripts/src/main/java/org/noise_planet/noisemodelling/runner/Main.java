/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and
 * education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE
 * provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.runner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.h2gis.utilities.dbtypes.DBTypes;
import org.h2gis.utilities.dbtypes.DBUtils;
import org.noise_planet.noisemodelling.webserver.NoiseModellingServer;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.noise_planet.noisemodelling.webserver.script.ExecutionPlan;
import org.noise_planet.noisemodelling.webserver.script.Job;
import org.noise_planet.noisemodelling.webserver.script.ScriptMetadata;
import org.noise_planet.noisemodelling.webserver.utilities.FileUtilities;
import org.noise_planet.noisemodelling.webserver.utilities.LibraryInfo;
import org.noise_planet.noisemodelling.webserver.utilities.Logging;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.noise_planet.noisemodelling.webserver.utilities.PgPassUtilities;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.sql.Statement;
import java.util.*;


public class Main {
    public static final int SECONDS_BETWEEN_PROGRESSION_PRINT = 5;
    public static final int DEFAULT_TERMINAL_WIDTH = 240;

    /**
     * Logs the collected libraries in a tabular format.
     */
    public static void printBuildIdentifiers(Logger logger, List<LibraryInfo> libraries) {
        if (libraries.isEmpty()) {
            logger.info("No library identifiers found.");
            return;
        }

        String columnFormat = "%-35.35s %-35.35s %-20.20s %-30.30s";
        StringBuilder sb = new StringBuilder();
        sb.append("Loaded libraries:\n");
        sb.append(String.format(Locale.ROOT, columnFormat, "Name", "Last-Modified", "Version", "Commit"));
        sb.append("\n");

        for (LibraryInfo lib : libraries) {
            sb.append(String.format(Locale.ROOT, columnFormat,
                    lib.getName(), lib.getLastModified(), lib.getVersion(), lib.getCommit()));
            sb.append("\n");
        }

        logger.info(sb.toString());
    }

    public static void main(String... args) throws Exception {
        PropertyConfigurator.configure(
                Objects.requireNonNull(NoiseModellingServer.class.getResource("static/log4j.properties")));

        // Arguments parser
        Options options = new Options();
        Option workingDirOption = new Option("w", "working-dir", true, "Path where the database and output logs will be written. It must be an existing folder with write permissions");
        workingDirOption.setRequired(true);
        workingDirOption.setArgName("WORKING_DIR");
        options.addOption(workingDirOption);
        Option scriptPathOption = new Option("s", "script", true, "Path and file name of the script");
        scriptPathOption.setRequired(true);
        scriptPathOption.setArgName("SCRIPT_PATH");
        options.addOption(scriptPathOption);
        Option databaseNameOption = new Option("d", "database-name", true, "Database name (default: h2gisdb)");
        databaseNameOption.setArgName("DATABASE_NAME");
        options.addOption(databaseNameOption);
        Option usernameOption = new Option("u", "username", true, "Database username (default: sa)");
        usernameOption.setArgName("USERNAME");
        options.addOption(usernameOption);
        Option passwordOption = new Option("p", "password", true, "Database password. If a PostGIS host is specified without a password, the password will be fetched from the .pgpass file if it exists (see https://www.postgresql.org/docs/current/libpq-pgpass.html). (default: sa for H2GIS)");
        passwordOption.setArgName("PASSWORD");
        Option portOption = new Option(null, "port", true, "Database port when connecting to PostGIS database (default: 5432)");
        portOption.setArgName("PORT");
        options.addOption(portOption);
        Option databaseHostNameOption = new Option(null, "host", true, "Database host name when connecting to PostGIS database, localhost if it is on your PC.  The database and host name can be used to fetch the credential access from the file .pgpass on your system if it exists (see https://www.postgresql.org/docs/current/libpq-pgpass.html).(default: empty to use embedded H2GIS)");
        databaseHostNameOption.setArgName("HOST");
        options.addOption(databaseHostNameOption);
        options.addOption(passwordOption);
        Option printVersionOption = new Option("v", false, "Print version of all libraries");
        options.addOption(printVersionOption);
        Option shutdownOption = new Option("c", "shutdown", false, "Do not shutdown compact the database at the end " +
                "of the execution");
        options.addOption(shutdownOption);
        Logger logger = LoggerFactory.getLogger("org.noise_planet");

        // Read parameters
        String workingDir = "";
        String scriptPath = "";
        Map<String, Object> customParameters = new HashMap<>();
        // Check if -v option is invoked before parsing using commandLineParser
        for (String arg : args) {
            if (arg.equals("-v") || arg.equals("--version")) {
                List<LibraryInfo> libraryInfoList = FileUtilities.collectLibraryIdentifiers();
                printBuildIdentifiers(logger, libraryInfoList);
                return;
            }
        }

        CommandLineParser commandLineParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(DEFAULT_TERMINAL_WIDTH);
        helpFormatter.setLeftPadding(2);
        helpFormatter.setDescPadding(4);
        CommandLine commandLine;
        try {
            commandLine = commandLineParser.parse(options, args, true);
        } catch (ParseException ex) {
            logger.info(ex.getMessage());
            helpFormatter.printHelp("ScriptRunner", options, true);
            System.exit(1);
            return;
        }
        workingDir = commandLine.getOptionValue(workingDirOption.getOpt());
        scriptPath = commandLine.getOptionValue(scriptPathOption.getOpt());
        boolean shutdown = !commandLine.hasOption(shutdownOption.getOpt());

        try (HikariDataSource ds = createDataSource(commandLine)) {
            // Initialize additional loggers
            Logging.configureFileLogger(workingDir, NoiseModellingServer.LOGGING_FILE_NAME);
            RootProgressVisitor progressVisitor = new RootProgressVisitor(1, true, SECONDS_BETWEEN_PROGRESSION_PRINT);
            try {
                File parentFolder = new File(scriptPath).getParentFile();
                String group = "";
                if(parentFolder != null) {
                    group = parentFolder.getName();
                }
                ScriptMetadata scriptMetadata = new ScriptMetadata(group, new File(scriptPath));
                // Create Command line arguments specification using the Input specification of the WPS process
                scriptMetadata.inputs.forEach((key, scriptInput) -> {
                    StringBuilder description = new StringBuilder(scriptInput.description.replaceAll("<[^>]*>", ""));
                    if(scriptInput.defaultValue != null) {
                        description.append("\nDefault: ").append(scriptInput.defaultValue).append("\n");
                    }
                    if(!scriptInput.allowedValues.isEmpty()) {
                        description.append("\nPossible values:\n");
                        for(Object allowedValue : scriptInput.allowedValues) {
                            description.append(" - ").append(allowedValue.toString()).append("\n");
                        }
                    }
                    Option customOption = new Option(null, key, true, description.toString());
                    customOption.setType(scriptInput.type);
                    if(Boolean.class == scriptInput.type) {
                        customOption.setArgName("true/false");
                    } else {
                        customOption.setArgName(scriptInput.title);
                    }
                    customOption.setRequired(scriptInput.minOccurs > 0);
                    options.addOption(customOption);
                });
                try {
                    commandLine = commandLineParser.parse(options, args);
                    // Read the command lines values to feed an input hash map for the groovy WPS
                    for (Iterator<Option> it = commandLine.iterator(); it.hasNext(); ) {
                        Option option = it.next();
                        if (option.getType() == String.class) {
                            customParameters.put(option.getLongOpt(), option.getValue());
                        } else if (option.getType() == Boolean.class) {
                            customParameters.put(option.getLongOpt(), Boolean.valueOf(option.getValue()));
                        } else if (option.getType() == Integer.class) {
                            customParameters.put(option.getLongOpt(), Integer.valueOf(option.getValue()));
                        } else if (option.getType() == Double.class) {
                            customParameters.put(option.getLongOpt(), NumberFormat.getInstance(Locale.ROOT).parse(option.getValue()).doubleValue());
                        } else {
                            throw new IllegalArgumentException("Unsupported type for option " + option.getOpt());
                        }
                    }
                } catch (ParseException ex) {
                    logger.info(ex.getMessage());
                    helpFormatter.printHelp("NoiseModelling Script Runner", options);
                    System.exit(1);
                    return;
                }
                Map<String, Object> inputs = new HashMap<>(customParameters);
                ExecutionPlan executionPlan = new ExecutionPlan(inputs, scriptMetadata);
                Object result = Job.runScript(executionPlan, progressVisitor, ds);
                if (result != null) {
                    logger.info(result.toString());
                }
                if (shutdown) {
                    // No try with resource as connection will be closed by SHUTDOWN command
                    Connection connection = ds.getConnection();
                    DBTypes dbType = DBUtils.getDBType(connection);
                    if (dbType == DBTypes.H2 || dbType == DBTypes.H2GIS) {
                        try (Statement st = connection.createStatement()) {
                            logger.info("Shutdown compact the database..");
                            st.execute("SHUTDOWN COMPACT");
                            logger.info("done");
                        }
                    }
                }
            } catch (SQLException ex) {
                while (ex != null) {
                    logger.error(ex.getLocalizedMessage(), ex);
                    ex = ex.getNextException();
                }
                System.exit(1);
            }
        } catch (Throwable ex) {
            logger.error(ex.getLocalizedMessage(), ex);
            System.exit(1);
        }
    }

    /**
     * Create a datasource for the database
     * @param commandLine Command line arguments
     * @return
     */
    public static HikariDataSource createDataSource(CommandLine commandLine) throws SQLException {
        String workingDir = commandLine.getOptionValue("working-dir");
        String databaseName = commandLine.getOptionValue("database-name", "h2gisdb");
        String username = commandLine.getOptionValue("username", "sa");
        String password = commandLine.getOptionValue("password");
        String port = commandLine.getOptionValue("port", "5432");
        String host = commandLine.getOptionValue("host", "");
        if(!host.isEmpty()) {
            if (password == null) {
                // Password isn't specified, try to fetch it from the .pgpass file
                PgPassUtilities.PgPassEntry entry = PgPassUtilities.getCredentials(PgPassUtilities.getPgPassFile(), host, port, databaseName, username);
                if (entry != null) {
                    password = entry.password;
                }
            }
            HikariConfig config = new HikariConfig();
            config.setUsername(username);
            config.setPassword(password);
            config.setDataSourceClassName(PGSimpleDataSource.class.getCanonicalName());
            config.addDataSourceProperty("portNumbers", Integer.parseInt(port));
            config.addDataSourceProperty("databaseName", databaseName);
            config.addDataSourceProperty("serverNames", host);
            return new HikariDataSource(config);
        } else {
            if(password == null) {
                password = "sa";
            }
            return DatabaseManagement.createH2DataSource(new File(workingDir).getAbsolutePath(), databaseName, username, password, "", true);
        }
    }
}
