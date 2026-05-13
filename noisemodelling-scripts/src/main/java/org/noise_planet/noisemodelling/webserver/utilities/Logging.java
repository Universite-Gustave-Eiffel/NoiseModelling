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
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility functions related to logging features
 */
public class Logging {

    public static final String DEFAULT_LOG_FORMAT = "[%t][%c] %-5p %d{dd MMM HH:mm:ss} - %m%n";
    public static final Pattern LOG_PATTERN =
            Pattern.compile("^\\[(?<thread>.+?)\\]\\[(?<logger>[^\\]]+)\\]");
    public static final String LINE_SEPARATOR = System.lineSeparator();


    public static void initConsoleLogging() {
        // 1. Internal debug to see what's happening
        org.apache.log4j.helpers.LogLog.setInternalDebugging(true);

        // 2. Reset everything to clear hidden configs from JARs
        org.apache.log4j.LogManager.resetConfiguration();

        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        rootLogger.setLevel(org.apache.log4j.Level.INFO);

        // 3. Create the Console Appender
        org.apache.log4j.ConsoleAppender console = new org.apache.log4j.ConsoleAppender();
        console.setName("stdout");
        console.setLayout(new org.apache.log4j.PatternLayout(DEFAULT_LOG_FORMAT));
        console.setThreshold(org.apache.log4j.Level.INFO);
        console.activateOptions();
        rootLogger.addAppender(console);
    }

    public static Appender configureLoggerFromWorkingDirectory(String workingDirectory, String loggingFileName) {
        // Check if there is a log4j configuration file in the working directory
        File log4jConfigFile = new File(workingDirectory, "log4j.properties");
        if (log4jConfigFile.exists()) {
            // Replace our current configuration with the one from the file
            org.apache.log4j.PropertyConfigurator.configure(log4jConfigFile.getAbsolutePath());
            org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
            rootLogger.info("Logger initialized successfully from configuration file: " + log4jConfigFile.getAbsolutePath());
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
            final org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
            rootLogger.addAppender(rollingAppender);

            rootLogger.info("Logger initialized successfully at: " + new File(workingDirectory, loggingFileName).getAbsolutePath());

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

            return rollingAppender;
        } catch (Exception e) {
            System.err.println("Failed to configure logger: " + e.getMessage());
        }
        return null;
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
     * Return lines from the most recent to the old ones
     * @param jobId
     * @param numberOfLines
     * @return
     * @throws IOException
     */
    public static String getAllLines(String jobId, File loggingFile, int numberOfLines) throws IOException {
        List<File> logFiles = new ArrayList<>();
        logFiles.add(loggingFile);
        int logCounter = 1;
        // Add rotating log files
        while(true) {
            File oldLogFile = new File(loggingFile + "." + (logCounter++));
            if(oldLogFile.exists()) {
                logFiles.add(oldLogFile);
            } else {
                break;
            }
        }
        StringBuilder rows = new StringBuilder();
        AtomicInteger fetchLines = new AtomicInteger(0);
        for(File logFile : logFiles) {
            rows.append(getLastLines(logFile,
                    numberOfLines == -1 ? -1 : numberOfLines - fetchLines.get(),
                    String.format("JOB_%s", jobId), fetchLines));
            if(numberOfLines != -1 && fetchLines.get() >= numberOfLines) {
                break;
            }
        }
        return rows.toString();
    }

    /**
     * Equivalent to "tail -n x file" linux command.
     * Retrieve the n last lines from a file but from the most recent to the oldest one.
     * @param logFile
     * @param maximumLinesToFetch
     * @return
     * @throws IOException
     */
    public static String getLastLines(File logFile, int maximumLinesToFetch, String jobId, AtomicInteger fetchedLines) throws IOException {
        AtomicInteger pushedLines = new AtomicInteger();
        StringBuilder sbMatch = new StringBuilder();
        final int buffer = 8192;
        long read = 0;
        String tailCache = "";
        int lastEndOfLine=0;
        try(RandomAccessFile f = new RandomAccessFile(logFile.getAbsoluteFile(), "r")) {
            long fileSize = f.length();
            long lastCursor = fileSize;
            while((maximumLinesToFetch == -1 || pushedLines.get() < maximumLinesToFetch) && read < fileSize) {
                long cursor = Math.max(0, fileSize - read - buffer);
                read += buffer;
                f.seek(cursor);
                byte[] b = new byte[(int)(lastCursor - cursor)];
                f.readFully(b);
                lastCursor = cursor;
                String remainingLines = "";
                if(!tailCache.isEmpty() && lastEndOfLine > 0) {
                    remainingLines = tailCache.substring(0, Math.min(tailCache.length(), lastEndOfLine + 1));
                }
                tailCache = new String(b, StandardCharsets.UTF_8);
                if(!remainingLines.isEmpty()) {
                    tailCache = tailCache + remainingLines;
                }
                int previousHookLocation = tailCache.length();
                // Reverse search of end of line into the string buffer
                lastEndOfLine = tailCache.lastIndexOf(LINE_SEPARATOR);
                while (lastEndOfLine != -1 && (maximumLinesToFetch == -1 || pushedLines.get() < maximumLinesToFetch)) {
                    int nextEndOfLine = tailCache.lastIndexOf(LINE_SEPARATOR, Math.max(0, lastEndOfLine - 1));
                    if(nextEndOfLine <= 0) {
                        break;
                    }
                    String line = tailCache.substring(nextEndOfLine + LINE_SEPARATOR.length(), lastEndOfLine);
                    if(!jobId.isEmpty()) {
                        Matcher matcher = LOG_PATTERN.matcher(line);
                        if (matcher.find()) {
                            String threadName = matcher.group("thread");
                            String loggerName = matcher.group("logger");
                            if((threadName.equals(jobId) || loggerName.equals(jobId)) && lastEndOfLine < previousHookLocation) {
                                // push other lines of this log
                                String logLines = tailCache.substring(nextEndOfLine + LINE_SEPARATOR.length(), previousHookLocation);
                                logLines.lines().forEach(s -> {
                                    pushedLines.getAndIncrement();
                                    sbMatch.append(s);
                                    sbMatch.append(LINE_SEPARATOR);
                                });
                            }
                            previousHookLocation = nextEndOfLine + 1;
                        }
                    } else {
                        sbMatch.append(line);
                        sbMatch.append(LINE_SEPARATOR);
                        pushedLines.getAndIncrement();
                    }
                    lastEndOfLine = nextEndOfLine;
                }
            }
        }
        fetchedLines.addAndGet(pushedLines.get());
        return sbMatch.toString();
    }
}
