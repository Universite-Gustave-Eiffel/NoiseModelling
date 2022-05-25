/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
package org.noisemodelling.runner;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.log4j.PropertyConfigurator;
import org.h2.util.OsgiDataSourceFactory;
import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.utilities.wrapper.ConnectionWrapper;
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.service.jdbc.DataSourceFactory;


public class Main {
    public static final int SECONDS_BETWEEN_PROGRESSION_PRINT = 5;


    public static DataSource createDataSource(String user, String password, String dbDirectory, String dbName, boolean debug) throws SQLException {
        // Create H2 memory DataSource
        org.h2.Driver driver = org.h2.Driver.load();
        OsgiDataSourceFactory dataSourceFactory = new OsgiDataSourceFactory(driver);
        Properties properties = new Properties();
        String databasePath = "jdbc:h2:" + new File(dbDirectory, dbName).getAbsolutePath();
        properties.setProperty(DataSourceFactory.JDBC_URL, databasePath);
        properties.setProperty(DataSourceFactory.JDBC_USER, user);
        properties.setProperty(DataSourceFactory.JDBC_PASSWORD, password);
        if(debug) {
            properties.setProperty("TRACE_LEVEL_FILE", "3"); // enable debug
        }
        DataSource dataSource = dataSourceFactory.createDataSource(properties);
        // Init spatial ext
        try (Connection connection = dataSource.getConnection()) {
            H2GISFunctions.load(connection);
        }
        return dataSource;

    }

    public static void printBuildIdentifiers(Logger logger) {
        try {
            String columnFormat = "%-35.35s %-35.35s %-20.20s %-30.30s";
            String[] columns = new String[] {"name", "last-modified", "version", "commit"};
            Enumeration<URL> resources = Main.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append( "Loaded libraries:\n");
            stringBuilder.append(String.format(Locale.ROOT, columnFormat,
                    (Object[]) columns));
            stringBuilder.append( "\n");
            Map<String, ArrayList<String>> rows = new HashMap<>();
            for (String column : columns) {
                rows.put(column, new ArrayList<>());
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    "EEE, d MMM yyyy HH:mm:ss Z", Locale.getDefault());
            int nbRows = 0;
            while (resources.hasMoreElements()) {
                try {
                    Manifest manifest = new Manifest(resources.nextElement().openStream());
                    Attributes attributes = manifest.getMainAttributes();
                    String bundleName = attributes.getValue("Bundle-Name");
                    String bundleVersion = attributes.getValue("Bundle-Version");
                    String gitCommitId = attributes.getValue("Implementation-Build");
                    String lastModifier = attributes.getValue("Bnd-LastModified");
                    if(bundleName != null) {
                        nbRows++;
                        rows.get(columns[0]).add(bundleName);
                        if(lastModifier != null) {
                            long lastModifiedLong = Long.parseLong(lastModifier);
                            rows.get(columns[1]).add(simpleDateFormat.format(new Date(lastModifiedLong)));
                        } else {
                            rows.get(columns[1]).add(" - ");
                        }
                        rows.get(columns[2]).add(bundleVersion != null ? bundleVersion : " - ");
                        rows.get(columns[3]).add(gitCommitId != null ? gitCommitId : " - ");
                    }
                } catch (IOException ex) {
                    logger.error(ex.getLocalizedMessage(), ex);
                }
            }
            for(int idRow = 0; idRow < nbRows; idRow++) {
                String[] rowValues = new String[columns.length];
                for (int idColumn = 0; idColumn < columns.length; idColumn++) {
                    String column = columns[idColumn];
                    rowValues[idColumn] = rows.get(column).get(idRow);
                }
                stringBuilder.append(String.format(Locale.ROOT, columnFormat,
                        (Object[]) rowValues));
                stringBuilder.append("\n");
            }
            logger.info(stringBuilder.toString());
        } catch (IOException ex) {
            logger.error("Error while accessing resources", ex);
        }
    }


    public static void main(String... args) throws Exception {
        PropertyConfigurator.configure(Main.class.getResource("log4j.properties"));

        Logger logger = LoggerFactory.getLogger("org.noise_planet");
        try {
            // Read parameters
            String workingDir = "";
            String scriptPath = "";
            String databaseName = "h2gisdb";
            Map<String, String> customParameters = new HashMap<>();
            boolean printVersion = false;
            for (int i = 0; args != null && i < args.length; i++) {
                String a = args[i];
                if(a == null) {
                    continue;
                }
                if (a.startsWith("-w")) {
                    workingDir = a.substring(2);
                    if(!(new File(workingDir).exists())) {
                        logger.error(workingDir + " folder does not exists");
                        workingDir = "";
                    }
                } else if (a.startsWith("-s")) {
                    scriptPath = a.substring(2);
                    if(!(new File(scriptPath).exists())) {
                        logger.error(scriptPath + " script does not exists");
                        scriptPath = "";
                    }
                } else if (a.startsWith("-d")) {
                    databaseName = a.substring(2);
                } else if (a.startsWith("-v")) {
                    printVersion = true;
                } else if(a.contains("=")){
                    String key = a.substring(0, a.indexOf("="));
                    String value = a.substring(a.indexOf("=") + 1);
                    customParameters.put(key, value);
                }
            }
            if (workingDir.isEmpty() || scriptPath.isEmpty()) {
                logger.info("Command line arguments :");
                for (String arg : args) {
                    logger.info("Got argument [" + arg + "]");
                }
                String help = "script_runner -wWORKSPACE -sSCRIPT_PATH\n" +
                        "DESCRIPTION\n" +
                        "-wWORKSPACE path where the database is located\n" +
                        "-sSCRIPT_PATH path and file name of the script\n" +
                        "-dFILENAME database name (default to h2gisdb)\n" +
                        "-v print version of all libraries\n" +
                        "customParameter=thevalue Custom arguments for the groovy script\n";
                throw new IllegalArgumentException(help);
            }

            if(printVersion) {
                printBuildIdentifiers(logger);
            }

            // Open database
            DataSource ds = createDataSource("", "", new File(workingDir).getAbsolutePath(), databaseName, false);

            RootProgressVisitor progressVisitor = new RootProgressVisitor(1, true,
                    SECONDS_BETWEEN_PROGRESSION_PRINT);

            try (Connection connection = new ConnectionWrapper(ds.getConnection())) {
                GroovyShell shell = new GroovyShell();
                Script receiversGrid= shell.parse(new File(scriptPath));
                Map<String, Object> inputs = new HashMap<>();
                inputs.putAll(customParameters);
                inputs.put("progressVisitor", progressVisitor);
                Object result = receiversGrid.invokeMethod("exec", new Object[] {connection, inputs});
                if(result != null) {
                    logger.info(result.toString());
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
}