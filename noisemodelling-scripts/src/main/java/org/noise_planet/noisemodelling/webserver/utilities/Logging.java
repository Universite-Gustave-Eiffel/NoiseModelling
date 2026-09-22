/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.webserver.utilities;

import org.apache.log4j.Appender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.jetbrains.annotations.NotNull;
import groovy.sql.Sql;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions related to logging features
 */
public class Logging {

    public static final String DEFAULT_LOG_FORMAT = "[%t][%c{1}] %-5p %d{dd MMM HH:mm:ss} - %m%n";
    public static final Pattern LOG_PATTERN =
            Pattern.compile("^\\[(?<thread>.+?)\\]\\[(?<logger>[^\\]]+)\\]");
    public static final String LINE_SEPARATOR = System.lineSeparator();


    public static void initConsoleLogging() {
        // Reset everything to clear hidden configs from JARs
        org.apache.log4j.LogManager.resetConfiguration();

        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        rootLogger.setLevel(org.apache.log4j.Level.INFO);

        // Create the Console Appender
        org.apache.log4j.ConsoleAppender console = new org.apache.log4j.ConsoleAppender();
        console.setName("stdout");
        console.setLayout(new org.apache.log4j.PatternLayout(DEFAULT_LOG_FORMAT));
        console.setThreshold(org.apache.log4j.Level.INFO);
        console.activateOptions();
        rootLogger.addAppender(console);
    }

    public static void configureLoggerFromWorkingDirectory(String workingDirectory, String loggingFileName, boolean verbose) {
        final org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();

        // Check if there is a log4j configuration file in the working directory
        File log4jConfigFile = new File(workingDirectory, "log4j.properties");
        if (log4jConfigFile.exists()) {
            // Replace our current configuration with the one from the file
            org.apache.log4j.PropertyConfigurator.configure(log4jConfigFile.getAbsolutePath());
            if(verbose) {
                rootLogger.info("Logger initialized successfully from configuration file: " + log4jConfigFile.getAbsolutePath());
            }
        } else {
            if(verbose) {
                System.out.println("No log4j.properties found in working directory." + " Initializing default logger " +
                        "configuration.");
                System.out.println("You can place a log4j.properties file in the working directory to customize " +
                        "logging behavior.");
            }
            try {
                // Create rolling file appender
                RollingFileAppender rollingAppender = createRollingFileAppender(workingDirectory, loggingFileName);

                if (rollingAppender.getLayout() == null) {
                    rollingAppender.setLayout(new org.apache.log4j.PatternLayout(DEFAULT_LOG_FORMAT));
                }
                rollingAppender.setImmediateFlush(true);

                rollingAppender.setThreshold(org.apache.log4j.Level.TRACE);

                // init stream
                rollingAppender.activateOptions();

                // Configure root logger
                rootLogger.addAppender(rollingAppender);

                rootLogger.info("Logger initialized successfully at: " + new File(workingDirectory, loggingFileName).getAbsolutePath());

            } catch (Exception e) {
                System.err.println("Failed to configure logger: " + e.getMessage());
            }
        }

        if(verbose) {
            System.out.println("--- LOGGING DIAGNOSTIC ---");
            System.out.println("Root Logger Class: " + rootLogger.getClass().getName());
            System.out.println("Root Logger Level: " + rootLogger.getLevel());

            Enumeration appenders = rootLogger.getAllAppenders();
            if (!appenders.hasMoreElements()) {
                System.out.println("!!! ERROR: No appenders attached to Root Logger !!!");
            } else {
                while (appenders.hasMoreElements()) {
                    Appender app = (Appender) appenders.nextElement();
                    System.out.println("Appender: " + app.getName() + " [" + app.getClass().getSimpleName() + "]");
                }
            }
            System.out.println("--------------------------");
        }
    }

    public static void clearAppenders() {
        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        // Close file appenders to release file locks
        Enumeration appenders = rootLogger.getAllAppenders();
        while (appenders.hasMoreElements()) {
            Appender app = (Appender) appenders.nextElement();
            if (app instanceof RollingFileAppender) {
                app.close();
            }
        }
        // Remove all appenders to reset logger state
        rootLogger.removeAllAppenders();
    }

    @NotNull
    public static RollingFileAppender createRollingFileAppender(String workingDirectory, String loggingFileName) {
        RollingFileAppender rollingAppender = new RollingFileAppender();

        // Configure appender properties
        rollingAppender.setName("rollingFile");
        rollingAppender.setFile(new File(workingDirectory, loggingFileName).getPath());
        rollingAppender.setAppend(true);
        rollingAppender.setMaxBackupIndex(5);
        rollingAppender.setMaximumFileSize(10_000_000);

        // Create and set a pattern layout
        PatternLayout layout = new PatternLayout(DEFAULT_LOG_FORMAT);
        rollingAppender.setLayout(layout);
        return rollingAppender;
    }

    /**
     * Build an HTML-friendly stack trace string similar to what SLF4J would print,
     * including the exception type, message, stack frames, causes, and suppressed exceptions.
     */
    public static String formatThrowableAsHtml(Throwable throwable) {
        if (throwable == null) return "";
        StringBuilder sb = new StringBuilder();

        // Detect circular references
        java.util.IdentityHashMap<Throwable, Boolean> seen = new java.util.IdentityHashMap<>();

        Throwable t = throwable;
        String prefix = "";
        while (t != null && !seen.containsKey(t)) {
            seen.put(t, Boolean.TRUE);

            // Exception header (class: message)
            String header = t.getClass().getName();
            String msg = t.getMessage();
            if (msg != null && !msg.isEmpty()) {
                header += ": " + msg;
            }
            sb.append(StringUtilities.escapeHtml(prefix + header)).append("<br>");

            // Stack frames
            for (StackTraceElement el : t.getStackTrace()) {
                sb.append(StringUtilities.escapeHtml(prefix + "\tat " + el)).append("<br>");
            }

            // Suppressed exceptions
            for (Throwable sup : t.getSuppressed()) {
                appendSuppressed(sb, sup, seen, prefix + "\t");
            }

            // Move to cause
            t = t.getCause();
            if (t != null && !seen.containsKey(t)) {
                sb.append(StringUtilities.escapeHtml(prefix + "Caused by: ")).append("<br>");
            }
        }

        return sb.toString();
    }

    public static void appendSuppressed(StringBuilder sb, Throwable sup, java.util.IdentityHashMap<Throwable, Boolean> seen, String prefix) {
        if (sup == null || seen.containsKey(sup)) return;
        seen.put(sup, Boolean.TRUE);

        String header = sup.getClass().getName();
        String msg = sup.getMessage();
        if (msg != null && !msg.isEmpty()) {
            header += ": " + msg;
        }
        sb.append(StringUtilities.escapeHtml(prefix + "Suppressed: " + header)).append("<br>");
        for (StackTraceElement el : sup.getStackTrace()) {
            sb.append(StringUtilities.escapeHtml(prefix + "\tat " + el)).append("<br>");
        }
        for (Throwable nested : sup.getSuppressed()) {
            appendSuppressed(sb, nested, seen, prefix + "\t");
        }
        if (sup.getCause() != null) {
            sb.append(StringUtilities.escapeHtml(prefix + "Caused by: ")).append("<br>");
            appendSuppressed(sb, sup.getCause(), seen, prefix + "\t");
        }
    }

    /**
     * Executes a SQL query and return the result set as an ASCII table.
     *
     * @param sql   An instance of groovy.sql.Sql
     * @param query A String or GString (allows for parameter injection)
     */
    public static String formatSqlQueryResult(Sql sql, Object query) {
        return formatSqlQueryResult(sql, query, 30);
    }

    /**
     * Executes a SQL query and return the result set as an ASCII table.
     *
     * @param sql         An instance of groovy.sql.Sql
     * @param query       A String or GString (allows for parameter injection)
     * @param maxColWidth Maximum width of a column before truncation
     */
    public static String formatSqlQueryResult(Sql sql, Object query, int maxColWidth) {
        List<Map<String, Object>> rawRows = new ArrayList<>();
        try {
            List<?> rows = sql.rows(query.toString());
            for (Object row : rows) {
                if (row instanceof Map) {
                    rawRows.add((Map<String, Object>) row);
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Logging.class).error("Error executing SQL query: {}", query, e);
            return "";
        }

        if (rawRows.isEmpty()) {
            return String.format("Query returned 0 rows.\nSQL: %s", query);
        }

        List<String> columnNames = new ArrayList<>();
        for (Object key : rawRows.get(0).keySet()) {
            columnNames.add(key.toString());
        }

        // 1. Pre-format all data (Truncate strings and Round numbers)
        List<Map<String, String>> formattedRows = new ArrayList<>();
        for (Map<String, Object> row : rawRows) {
            Map<String, String> formattedRow = new LinkedHashMap<>();
            for (String col : columnNames) {
                Object val = row.get(col);
                String formattedVal;

                if (val == null) {
                    formattedVal = "null";
                } else if (val instanceof Number && !(val instanceof Integer || val instanceof Long || val instanceof BigInteger)) {
                    // Round numbers to 2 decimal places
                    formattedVal = String.format(Locale.US, "%.2f", ((Number) val).doubleValue());
                } else {
                    // Truncate long strings (like Geometries)
                    formattedVal = val.toString();
                    if (formattedVal.length() > maxColWidth) {
                        formattedVal = formattedVal.substring(0, maxColWidth - 3) + "...";
                    }
                }
                formattedRow.put(col, formattedVal);
            }
            formattedRows.add(formattedRow);
        }

        // 2. Calculate column widths based on formatted data
        Map<String, Integer> columnWidths = new HashMap<>();
        for (String col : columnNames) {
            int headerLen = col.length();
            int maxDataLen = 0;
            for (Map<String, String> row : formattedRows) {
                String val = row.get(col);
                if (val != null) {
                    maxDataLen = Math.max(maxDataLen, val.length());
                }
            }
            columnWidths.put(col, Math.max(headerLen, maxDataLen));
        }

        // 3. Build the ASCII Table
        StringBuilder lineSeparatorBuilder = new StringBuilder("+");
        for (String col : columnNames) {
            int width = columnWidths.get(col);
            lineSeparatorBuilder.append("-".repeat(Math.max(0, width + 2)));
            lineSeparatorBuilder.append("+");
        }
        String lineSeparator = lineSeparatorBuilder.toString();

        StringBuilder table = new StringBuilder();
        table.append("\nSQL: ").append(query).append("\n");
        table.append(lineSeparator).append("\n|");

        // Header
        for (String col : columnNames) {
            int width = columnWidths.get(col);
            table.append(String.format(" %-" + width + "s |", col));
        }
        table.append("\n").append(lineSeparator).append("\n");

        // Rows
        for (Map<String, String> row : formattedRows) {
            table.append("|");
            for (String col : columnNames) {
                int width = columnWidths.get(col);
                table.append(String.format(" %-" + width + "s |", row.get(col)));
            }
            table.append("\n");
        }
        table.append(lineSeparator);

        return table.toString();
    }

}
