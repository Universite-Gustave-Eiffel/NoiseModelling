/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import net.opengis.ows11.ExceptionReportType;
import net.opengis.ows11.ExceptionType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.*;
import org.apache.log4j.*;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.geotools.ows.v1_1.OWS;
import org.geotools.ows.v1_1.OWSConfiguration;
import org.geotools.wps.WPSConfiguration;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.noise_planet.noisemodelling.webserver.script.*;
import org.noise_planet.noisemodelling.webserver.secure.JWTProvider;
import org.noise_planet.noisemodelling.webserver.secure.JavalinJWT;
import org.noise_planet.noisemodelling.webserver.secure.User;
import org.noise_planet.noisemodelling.webserver.utilities.Logging;
import org.noise_planet.noisemodelling.webserver.utilities.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.zaxxer.hikari.HikariDataSource;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The OwsController class handles requests for OGC Web Services (OWS), including
 * WPS (Web Processing Service), WFS (Web Feature Service), and WCS (Web Coverage Service).
 * It provides functionalities for GET and POST requests, depending on the OWS service and
 * operation type.
 */
public class OwsController {
    public static final int CORE_POOL_SIZE = 5;
    public static final int MAXIMUM_POOL_SIZE = 5;
    public static final long KEEP_ALIVE_TIME = 0L;
    public static final int MAXIMUM_LINES_TO_FETCH = 1_000;
    private static final int DEFAULT_ABORT_JOB_DELAY = 5;
    private static final long MAX_UPLOAD_SIZE = 500L * 1024 * 1024; // 500 MB
    private static final int MAX_UPLOAD_TIMEOUT = 10;
    private final Logger logger = LoggerFactory.getLogger(OwsController.class);
    private final JWTProvider<User> provider;
    private Map<Integer, DataSource> userDataSources = Collections.synchronizedMap(new HashMap<Integer, DataSource>());
    private Map<WsContext, WriterAppender> websocketLoggers = Collections.synchronizedMap(new HashMap<>());
    Configuration configuration;
    DataSource serverDataSource;

    /**
     * Handle threads
     */
    final JobExecutorService jobExecutorService = new JobExecutorService(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS);

    /**
     * Collection of {@link ScriptMetadata} objects representing the
     * scripts available for the Web Processing Service (WPS). Each script is wrapped
     * in a {@link ScriptMetadata}, which encapsulates its metadata, inputs, outputs,
     * and logic to facilitate execution.
     * <p>
     * This list serves as a central repository of scripts that can be dynamically
     * reloaded and used to process WPS requests. It plays a crucial role in
     * handling WPS operations by mapping process identifiers to their corresponding
     * Groovy script implementations.
     */
    Map<String, ScriptMetadata> wpsScripts;

    /**
     * An instance of WpsScriptWrapper used to manage the execution of WPS (Web Processing Service) scripts.
     * This wrapper facilitates the interaction between the application and the underlying scripting engine
     * to execute processes defined in scripts. It is responsible for script execution and handling inputs
     * and outputs for WPS processes.
     */
    WpsScriptWrapper wpsScriptWrapper;


    /**
     * Constructs an instance of the OwsController class. This constructor initializes the
     * WPS scripts by loading and grouping them using the WpsScriptWrapper utility. The scripts
     * are classified based on their directory structure and wrapped into appropriate script
     * wrappers for further processing.
     *
     * @throws IOException if an error occurs while loading or processing the script files.
     */
    public OwsController(DataSource serverDataSource, JWTProvider<User> provider, Configuration configuration) throws IOException {
        wpsScriptWrapper = new WpsScriptWrapper(Path.of(configuration.scriptPath));
        Map<String, List<File>> groupedScripts = wpsScriptWrapper.loadScripts();
        wpsScripts = WpsScriptWrapper.buildScriptWrappers(groupedScripts);
        this.provider = provider;
        this.configuration = configuration;
        this.serverDataSource = serverDataSource;
    }

    /**
     * Reloads the WPS (Web Processing Service) scripts by reloading them from the file system
     * and rebuilding the corresponding script wrappers.
     * <p>
     * This method uses {@link WpsScriptWrapper#loadScripts()} to scan and organize script files into
     * groups. The results are then processed by {@link WpsScriptWrapper#buildScriptWrappers(Map)} to
     * create a new set of script wrappers, which replace the existing ones.
     *
     * @throws IOException if an error occurs while loading or rebuilding the scripts.
     */
    public void reloadScripts() throws IOException {
        Map<String, List<File>> groupedScripts = wpsScriptWrapper.loadScripts();
        wpsScripts = WpsScriptWrapper.buildScriptWrappers(groupedScripts);
    }

    /**
     * Handles GET requests for the OWS (Web Services) endpoint. Based on the "service" query parameter,
     * it routes the request to the appropriate WPS, WFS, or WCS service handler. If the service type is unknown,
     * responds with HTTP 400 (Bad Request). Handles exceptions and responds with HTTP 500 (Internal Server Error)
     * in case of server-side errors.
     *
     * @param ctx the context of the current HTTP request, providing access to request parameters,
     *            response handling, and the ability to set content type and status codes
     */
    public void handleGet(Context ctx) {
        ctx.contentType("text/xml; charset=UTF-8");
        String service = ctx.queryParam("service");

        try {
            if ("WPS".equalsIgnoreCase(service)) {
                handleWPSGet(ctx);
            } else if ("WFS".equalsIgnoreCase(service)) {
                handleWFSGet(ctx);
            } else if ("WCS".equalsIgnoreCase(service)) {
                handleWCSGet(ctx);
            } else {
                ctx.status(400).result("Unknown service");
            }
        } catch (Exception e) {
            logger.error("Error handling OWS GET request", e);
            try {
                returnExceptionDocument(ctx, e);
            } catch (IOException ex) {
                logger.error("Error generating error document", ex);
            }
        }
    }


    public void returnExceptionDocument(Context ctx, Exception ex) throws IOException {
        ExceptionReportType report = WpsXmlDocumentGenerator.generateExceptionDocument(ex);

        ProcessFailedType failedType = Wps10Factory.eINSTANCE.createProcessFailedType();
        failedType.setExceptionReport(report);

        Encoder encoder = new Encoder(new OWSConfiguration());
        encoder.setIndenting(true);
        encoder.setIndentSize(2);

        // used to throw an exception here
        ctx.status(500).result(encoder.encodeAsString(report, OWS.ExceptionReport));
    }



    /**
     * Handles HTTP GET requests for the WPS (Web Processing Service) by managing
     * the "GetCapabilities" and "DescribeProcess" operations. Based on the "request"
     * query parameter, this method retrieves WPS capabilities or details of a specific
     * process. Responds with appropriate XML content or error messages in case of invalid
     * requests or missing parameters.
     *
     * @param ctx the context of the current HTTP request, providing access to
     *            query parameters, response handling, and the ability to set
     *            content type and status codes
     */
    protected void handleWPSGet(Context ctx) throws IOException {
        String request = ctx.queryParam("request");
        ctx.contentType("text/xml; charset=UTF-8");

        if ("GetCapabilities".equalsIgnoreCase(request)) {
            String xml = WpsXmlDocumentGenerator.generateCapabilitiesXML(wpsScripts);
            ctx.result(xml);
        } else if ("DescribeProcess".equalsIgnoreCase(request)) {
            String identifier = ctx.queryParam("identifier");
            if (identifier == null || identifier.isEmpty()) {
                ctx.status(400).result("<ows:Exception>Missing identifier parameter</ows:Exception>");
                return;
            }

            ScriptMetadata target = wpsScripts.get(identifier);

            if (target != null) {
                ctx.result(WpsXmlDocumentGenerator.generateDescribeProcessXML(target));
            } else {
                ctx.status(404).result("<ows:Exception>Process not found: " + identifier + "</ows:Exception>");
            }

        } else {
            ctx.status(400).result("<ows:Exception>Unknown WPS request: " + request + "</ows:Exception>");
        }
    }

    /**
     * Handles WFS (Web Feature Service) GET requests for the OWS (Web Services) endpoint.
     * Depending on the value of the "request" query parameter, this method determines the desired operation.
     * If the request is "GetCapabilities", the corresponding XML file is read and returned in the response.
     * For unknown or unsupported requests, it returns an HTTP 400 (Bad Request) status.
     *
     * @param ctx the context of the current HTTP request, providing access to query parameters,
     *            request and response handling, and allowing for status and body configuration
     * @throws Exception if an error occurs while reading or responding with the requested resource
     */
    protected void handleWFSGet(Context ctx) throws Exception {
        String request = ctx.queryParam("request");
        if ("GetCapabilities".equalsIgnoreCase(request)) {
            try (InputStream xmlStream = getClass().getResourceAsStream("static/xmlFiles/wfs.xml")){
                if (xmlStream != null) {
                    ctx.result(xmlStream.readAllBytes());
                }
            }
        } else {
            ctx.status(400).result("Unknown WFS request");
        }
    }

    /**
     * Handles a Get request for the Web Coverage Service (WCS). The method processes
     * the incoming HTTP request by examining the "request" query parameter. If the
     * query parameter is "GetCapabilities", it responds with the contents of a WCS
     * capabilities XML file. If the request is not recognized, it responds with a
     * 400 HTTP status and an error message.
     *
     * @param ctx the context of the HTTP request, providing access to query parameters,
     *            response handling, status codes, and other request-related information
     * @throws Exception if an I/O error occurs while attempting to read the WCS capabilities
     *                   XML file or while processing the request
     */
    protected void handleWCSGet(Context ctx) throws Exception {
        String request = ctx.queryParam("request");
        if ("GetCapabilities".equalsIgnoreCase(request)) {
            try (InputStream xmlStream = getClass().getResourceAsStream("static/xmlFiles/wcs.xml")) {
                if (xmlStream != null) {
                    ctx.result(xmlStream.readAllBytes());
                }
            }
        } else {
            ctx.status(400).result("Unknown WCS request");
        }
    }

    /**
     * OGC compliant WPS, Build a ExecuteResponse according to the state of a Job
     * @param ctx the context of the current HTTP request, providing access to query parameters,
     * request and response handling, and allowing for status and body configuration
     */
    public void handleJobExecuteStatus(Context ctx) {
        try(Connection connection = serverDataSource.getConnection()) {
            User user = ctx.attribute("user");
            int jobId = Integer.parseInt(ctx.pathParam("job_id"));
            Map<String, Object> jobData = DatabaseManagement.getJob(connection, jobId);
            if(hasUnauthorizedJobAccess(ctx, user, jobData)) {
                return;
            }
            Job<?> job = jobExecutorService.getJob(jobId);
            ctx.contentType("text/xml; charset=UTF-8");
            ctx.result(WpsXmlDocumentGenerator.generateExecuteResponseDocument(job, jobData, configuration));
        } catch (SQLException | IOException | ParseException | DatatypeConfigurationException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Render job list HTML page
     * @param ctx web context
     */
    public void jobList(Context ctx) {
        try(Connection connection = serverDataSource.getConnection()) {
            int userIdFilter = -1;
            User user = ctx.attribute("user");
            if(user != null && !user.isAdministrator()) {
                userIdFilter = user.getIdentifier();
            }
            ctx.render("job_list", Map.of("jobs", DatabaseManagement.getJobs(connection, userIdFilter)));
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }


    /**
     * Parses an InputStream to extract and return an ExecuteType object.
     *
     * @param inputStream the input stream containing the request to be parsed
     * @return an ExecuteType object if parsing is successful and the input corresponds to ExecuteType;
     *         otherwise, returns null
     * @throws IOException if an I/O error occurs during parsing
     * @throws ParserConfigurationException if a configuration error occurs while setting up the parser
     * @throws SAXException if an error occurs during XML parsing
     */
    public static ExecuteType parseExecuteRequest(InputStream inputStream)
            throws IOException, ParserConfigurationException, SAXException {
        Parser parser = new Parser(new WPSConfiguration());
        Object parsed = parser.parse(inputStream);
        if (!(parsed instanceof ExecuteType)) {
            return null;
        }
        return (ExecuteType) parsed;
    }

    /**
     * Parses the given input stream to extract an ExecuteResponseType object.
     *
     * @param inputStream the input stream containing the data to be parsed
     * @return an ExecuteResponseType instance if parsing is successful and the parsed object
     *         is of the expected type, otherwise returns null
     * @throws IOException if an I/O error occurs while reading the input stream
     * @throws ParserConfigurationException if a parser configuration error occurs
     * @throws SAXException if an XML parsing error occurs
     */
    public static ExecuteResponseType parseExecuteResponse(InputStream inputStream)
            throws IOException, ParserConfigurationException, SAXException {
        Parser parser = new Parser(new WPSConfiguration());
        Object parsed = parser.parse(inputStream);
        if (!(parsed instanceof ExecuteResponseType)) {
            return null;
        }
        return (ExecuteResponseType) parsed;
    }

    /**
     * Parse the XML of the query to generate the execution plan
     * @param wpsXmlBody XML of the query
     * @return Execution plan
     * @throws IOException if an I/O error occurs while reading the XML
     * @throws ParserConfigurationException if a configuration error occurs while parsing the XML
     * @throws SAXException if a SAX error occurs while parsing the XML
     */
    public static ExecutionPlan generateExecutionPlanFromWPS(InputStream wpsXmlBody, Map<String, ScriptMetadata> wpsScripts)
            throws IOException, ParserConfigurationException, SAXException {
        ExecuteType execute = parseExecuteRequest(wpsXmlBody);
        if(execute == null) {
            throw new IOException("Invalid WPS request");
        }
        return generateExecutionPlanFroExecuteType(execute, wpsScripts);
    }

    public static ExecutionPlan generateExecutionPlanFroExecuteType(ExecuteType execute, Map<String, ScriptMetadata> wpsScripts)
            throws IOException, ParserConfigurationException, SAXException {
        return extractExecuteQuery(execute, wpsScripts);
    }

    /**
     * Extracts input data from the provided {@code ExecuteType} object and returns the execution plan.
     * It processes the data inputs defined in the {@code ExecuteType}
     * object to construct this mapping.
     *
     * @param execute the {@code ExecuteType} object that contains the data inputs to extract.
     * @param wpsScripts the map of WPS scripts to their metadata.
     * @return The execution plan requested by the query
     */
    public static ExecutionPlan extractExecuteQuery(ExecuteType execute, Map<String, ScriptMetadata> wpsScripts) throws IOException {
        String processId = execute.getIdentifier().getValue();

        // Fetch the script name to execute
        ScriptMetadata scriptMetadata = wpsScripts.get(processId);

        if (scriptMetadata == null) {
            throw new IOException("Process name not found: " + processId);
        }

        DataInputsType1 dataInputs = execute.getDataInputs();
        Map<String, Object> queryInputs = new HashMap<>();
        if (dataInputs != null && dataInputs.getInput() != null) {
            for (Object inputObj : dataInputs.getInput()) {
                if (inputObj instanceof InputType) {
                    InputType input = (InputType) inputObj;
                    try {
                        String inputId = input.getIdentifier().getValue();
                        if(input.getData() != null && input.getData().getLiteralData() != null) {
                            // Simple literal data type as input
                            Object inputContent = input.getData().getLiteralData().getValue();
                            if (scriptMetadata.inputs.containsKey(inputId)) {
                                ScriptInput scriptInput = scriptMetadata.inputs.get(inputId);
                                // found expected input, try to cast to expect type if not null
                                Class<?> expectedInputType = scriptInput.type;
                                String typeName = expectedInputType.getName();
                                if (typeName.equals(Long.class.getName())) {
                                    inputContent = Long.parseLong(input.getData().getLiteralData().getValue());
                                } else if (typeName.equals(Integer.class.getName())) {
                                    inputContent = Integer.parseInt(input.getData().getLiteralData().getValue());
                                } else if (typeName.equals(Float.class.getName())) {
                                    inputContent = Float.parseFloat(input.getData().getLiteralData().getValue());
                                } else if (typeName.equals(Double.class.getName())) {
                                    inputContent = Double.parseDouble(input.getData().getLiteralData().getValue());
                                } else if (typeName.equals(Boolean.class.getName())) {
                                    inputContent = Boolean.parseBoolean(input.getData().getLiteralData().getValue());
                                } else if (typeName.equals(org.locationtech.jts.geom.Geometry.class.getName())) {
                                    inputContent =
                                            new org.locationtech.jts.io.WKTReader().read(input.getData().getLiteralData().getValue());
                                }
                                queryInputs.put(inputId, inputContent);
                            } else {
                                Logger logger = LoggerFactory.getLogger(ExecutionPlan.class);
                                logger.warn("Input '{}' not found in metadata, ignore this argument", inputId);
                            }
                        } else if(input.getReference() != null) {
                            // Chained Process as input
                            InputReferenceType reference = input.getReference();
                            if(reference.getBody() instanceof ExecuteType) {
                                ExecutionPlan childExecutionPlan =
                                        extractExecuteQuery((ExecuteType) reference.getBody(), wpsScripts);
                                queryInputs.put(inputId, childExecutionPlan);
                            }
                        }
                    } catch (Exception ex) {
                        Logger logger = LoggerFactory.getLogger(ExecutionPlan.class);
                        logger.info("Warning, ignore input as there was an exception while converting input '{}'", input.getIdentifier().getValue(), ex);
                    }
                }
            }
        }
        // Expected output
        OutputDefinitionType outputDefinitionType = execute.getResponseForm().getRawDataOutput();
        if(outputDefinitionType != null) {
            String outputIdentifier = outputDefinitionType.getIdentifier().getValue();
            if(scriptMetadata.outputs.containsKey(outputIdentifier)) {
                return new ExecutionPlan(queryInputs, scriptMetadata, outputIdentifier);
            }
        }
        return new ExecutionPlan(queryInputs, scriptMetadata);
    }
    /**
     * Handles an HTTP POST request for a Web Processing Service (WPS) operation.
     * This method parses the request body, validates the WPS Execute Request, identifies
     * the target script to execute based on its process identifier, and executes the script.
     * The result of the script execution is returned as a JSON response.
     * Responds with appropriate HTTP status codes for invalid requests, missing scripts,
     * and internal server errors.
     *
     * @param ctx the context of the HTTP request, providing access to the request body,
     *            response handling, and the ability to set status codes and send JSON responses
     */
    public void handleWPSPost(Context ctx) {
        try {
            int userId = JavalinJWT.getUserIdentifierFromContext(ctx, provider);

            ExecuteType execute = parseExecuteRequest(new ByteArrayInputStream(ctx.bodyAsBytes()));
            if(execute == null) {
                throw new IOException("Invalid WPS request");
            }
            ExecutionPlan executionPlan = generateExecutionPlanFroExecuteType(execute, wpsScripts);
            int jobUserId = userId > 0 ? userId : 1; // user may not be logged in
            DataSource userDataSource = fetchUserDataSource(jobUserId);
            Job<Object> job = new Job<>(jobUserId, executionPlan, serverDataSource,
                    userDataSource , configuration);
            Future<Object> result = jobExecutorService.submitJob(job);
            try {
                Object jobResult = result.get(executionPlan.getScriptMetadata().executionTimeoutSeconds, TimeUnit.SECONDS);
                processResult(ctx, jobResult);
            } catch (TimeoutException e) {
                // Long process, switch to asynchronous call
                if(execute.getResponseForm().getResponseDocument() == null) {
                    String url = ctx.contextPath() + "/job_logs/" + job.getId();
                    ctx.result(String.format(
                            "Long running process, <a href=\"%s\" target=\"_blank\">please look at the job (id: %d)</a> output logs",
                            url,
                            job.getId()));
                } else {
                    // Request want a standard OGC ExecuteResponse document
                    Map<String, Object> jobData;
                    try (Connection connection = serverDataSource.getConnection()) {
                        jobData = DatabaseManagement.getJob(connection, job.getId());
                    }
                    ctx.contentType("text/xml; charset=UTF-8");
                    ctx.result(WpsXmlDocumentGenerator.generateExecuteResponseDocument(job, jobData, configuration));
                }
            }
        } catch (Exception e) {
            logger.error("Error executing WPS {}", ctx.body(), e);
            // If error occurred inside the future, unwrap the ExecutionException
            String stackTrace = Logging.formatThrowableAsHtml(e);
            String html = MessageFormat.format("<html><head>    <style>        body '{' font-family: Arial; margin: " +
                    "20px; '}'        .section '{' margin-bottom: 20px; '}'        .title '{' font-size: 20px; " +
                    "font-weight: bold; margin-bottom: 5px; color:#b30000; '}'        .box '{' border: 1px solid " +
                    "#ccc; padding: 10px; background:#fafafa; '}'        .error '{' color: #b30000; font-weight: " +
                    "bold; '}'    </style></head><body>    <div class=''section''>        <div class=''title''>Error:" +
                    " </div>        <div class=''box''><span class=''error''>{0}</span></div>    </div>    <div " +
                    "class=''section''>        <div class=''title''>Inputs Data</div>        <div " +
                    "class=''box''>{1}</div>    </div></body></html>", stackTrace, StringUtilities.escapeHtml(ctx.body()));

            ctx.contentType("text/html; charset=UTF-8");
            ctx.result(html);
        }
    }

    /**
     * Format the WPS output for the rendering of the WPS Builder website
     * @param context
     * @param result
     */
    public static void processResult(Context context, Object result) {
        if(result instanceof Map<?, ?> && ((Map<?, ?>) result).size() == 1) {
            processResult(context, ((Map<?, ?>) result).values().iterator().next());
        } else {
            if(result instanceof Geometry) {
                // Convert Geometry to WKT then encode it in base64
                WKTWriter wktWriter = new WKTWriter(2);
                String wkt = wktWriter.write((Geometry) result);
                context.contentType("application/octet-stream");
                context.result(wkt.getBytes());
            } else {
                context.result(result != null ? result.toString() : "");
            }
        }
    }

    /**
     * The datasource instance contain the hikari connection pool so we must keep it between transactions
     * @param userId User identifier
     * @return
     * @throws SQLException
     */
    public DataSource fetchUserDataSource(int userId) throws SQLException {
        DataSource dataSource = userDataSources.get(userId);
        if (dataSource == null) {
            dataSource = DatabaseManagement.createH2DataSource(
                configuration.getWorkingDirectory(), getUserDatabaseName(userId),
                "sa",
                "sa",
                "",
                true);
            userDataSources.put(userId, dataSource);
        }
        return dataSource;
    }

    @NotNull
    public static String getUserDatabaseName(int userId) {
        return String.format("user_%03d", userId);
    }


    /**
     * Checks if the given user has unauthorized access to the specified job data.
     * If the user is not an administrator and the job data's user ID does not match
     * the user's identifier, the method renders an "unauthorized access" message and
     * redirects the user to the jobs page.
     *
     * @param ctx the context of the current HTTP request, providing access to request parameters,
     *            response handling, and other request-related features
     * @param user the user requesting access to the job; may be null if no user is authenticated
     * @param jobData a map containing job data, including the key "userId" which identifies the
     *                owner of the job
     * @return true if the user has unauthorized access to the job and the method handles it by
     *         rendering a response; false if the access is authorized
     */
    private boolean hasUnauthorizedJobAccess(@NotNull Context ctx, User user,
                                             Map<String, Object> jobData) {
        if (user != null && !user.isAdministrator() && Integer.valueOf(user.getIdentifier()) != jobData.get("userId")) {
            ctx.render("blank", Map.of(
                    "redirectUrl", ctx.contextPath() + "/jobs",
                    "message", "Unauthorized access, this job does not belong to you"));
            return true;
        }
        return false;
    }

    /**
     * Retrieves and displays the logs of a specific job based on the job ID.
     * This method retrieves the job data from the database, checks access permissions,
     * and fetches the corresponding log entries, rendering them in the response context.
     *
     * @param ctx the context of the current request, containing job-related parameters,
     *            request attributes, and response handling methods.
     */
    public void jobLogs(@NotNull Context ctx) {
        try (Connection connection = serverDataSource.getConnection()) {
            User user = ctx.attribute("user");
            try {
                int jobId = Integer.parseInt(ctx.pathParam("job_id"));
                Map<String, Object> jobData = DatabaseManagement.getJob(connection, jobId);
                if(hasUnauthorizedJobAccess(ctx, user, jobData)) {
                    return;
                }
                // Parse the current server logs
                // we could store the logs into the database when the job complete or failed, maybe another time.
                String lastLines = Logging.getLastLines(new File(configuration.workingDirectory,
                        NoiseModellingServer.LOGGING_FILE_NAME), MAXIMUM_LINES_TO_FETCH, Job.getThreadName(jobId), new AtomicInteger());
                ctx.render("job_logs", Map.of("jobId", jobId, "rows", lastLines));
            } catch (NumberFormatException ex) {
                logger.error("Invalid job id {}", ctx.body(), ex);
                ctx.render("blank", Map.of(
                        "redirectUrl", ctx.contextPath() + "/jobs",
                        "message", "Wrong job id parameter"));
            }
        } catch (SQLException | IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Deletes a job based on the job ID provided in the request context.
     * Ensures user authorization before deleting the job and updates the job list
     * upon successful deletion. Handles errors for invalid job IDs and database issues.
     *
     * @param ctx The context of the request containing user information and
     *            job ID path parameter.
     */
    public void jobDelete(@NotNull Context ctx) {
        try (Connection connection = serverDataSource.getConnection()) {
            User user = ctx.attribute("user");
            try {
                int jobId = Integer.parseInt(ctx.pathParam("job_id"));
                Map<String, Object> jobData = DatabaseManagement.getJob(connection, jobId);
                if(hasUnauthorizedJobAccess(ctx, user, jobData)) {
                    return;
                }
                DatabaseManagement.deleteJob(connection, jobId);
                jobList(ctx);
            } catch (NumberFormatException ex) {
                logger.error("Invalid job id {}", ctx.body(), ex);
                ctx.render("blank", Map.of(
                        "redirectUrl", ctx.contextPath() + "/jobs",
                        "message", "Wrong job id parameter"));
            }
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Deletes a job based on the job ID provided in the request context.
     * Ensures user authorization before deleting the job and updates the job list
     * upon successful deletion. Handles errors for invalid job IDs and database issues.
     *
     * @param ctx The context of the request containing user information and
     *            job ID path parameter.
     */
    public void jobDeleteAll(@NotNull Context ctx) {
        try (Connection connection = serverDataSource.getConnection()) {
            User user = ctx.attribute("user");
            if(user != null) {
                try {
                    DatabaseManagement.deleteAllFinalizedJobs(connection, user.getIdentifier());
                    jobList(ctx);
                } catch (NumberFormatException ex) {
                    logger.error("Invalid job id {}", ctx.body(), ex);
                    ctx.render("blank",
                            Map.of("redirectUrl", ctx.contextPath() + "/jobs", "message", "Wrong job id parameter"));
                }
            }
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Cancels the job specified by the job ID in the request context.
     * The method validates user access to the job, retrieves the job data,
     * and triggers the cancellation of the job with a default delay.
     *
     * @param ctx the context of the HTTP request, containing information such as path parameters,
     *            user attributes, and other relevant data required for the operation
     */
    public void jobCancel(@NotNull Context ctx) {
        try (Connection connection = serverDataSource.getConnection()) {
            User user = ctx.attribute("user");
            try {
                int jobId = Integer.parseInt(ctx.pathParam("job_id"));
                Map<String, Object> jobData = DatabaseManagement.getJob(connection, jobId);
                if(hasUnauthorizedJobAccess(ctx, user, jobData)) {
                    return;
                }
                if(!jobExecutorService.cancelJob(jobId, DEFAULT_ABORT_JOB_DELAY)) {
                    // Can't find the job, set it in error to be able to remove it
                    DatabaseManagement.setJobState(connection, jobId, JobStates.FAILED.name());
                }
                jobList(ctx);
            } catch (NumberFormatException ex) {
                logger.error("Invalid job id {}", ctx.body(), ex);
                ctx.render("blank", Map.of(
                        "redirectUrl", ctx.contextPath() + "/jobs",
                        "message", "Wrong job id parameter"));
            }
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Establishes a WebSocket stream to send logs associated with a specific job to the client upon connection.
     * The method retrieves job details, validates user access, and creates a custom log appender that captures logs
     * for the specified job thread. Logs are filtered and streamed through the WebSocket connection.
     *
     * @param ctx the WebSocket connection context that contains the connection details and user session data.
     */
    public void jobLogsStreamOnConnect(WsConnectContext ctx) {
        try (Connection connection = serverDataSource.getConnection()) {
            User user = ctx.attribute("user");
            int jobId = Integer.parseInt(ctx.pathParam("job_id"));
            Map<String, Object> jobData = DatabaseManagement.getJob(connection, jobId);
            if(hasUnauthorizedJobAccess(ctx.getUpgradeCtx$javalin(), user, jobData)) {
                return;
            }
            logger.info("WebSocket connection established for job {}", jobId);
            String threadName = Job.getThreadName(jobId);

            // Create a custom appender that sends logs to WebSocket
            WriterAppender wsAppender = getWriterAppender(ctx, jobId);

            websocketLoggers.put(ctx, wsAppender);

            // Filter to only capture logs from this job's thread
            wsAppender.addFilter(new Filter() {
                @Override
                public int decide(LoggingEvent event) {
                    if (event.getThreadName().equals(threadName)) {
                        return Filter.ACCEPT;
                    }
                    return Filter.DENY;
                }
            });

            wsAppender.activateOptions();
            org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
            rootLogger.addAppender(wsAppender);

        } catch (NumberFormatException ex) {
            logger.error("Invalid job id in WebSocket connection", ex);
            ctx.closeSession();
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            ctx.closeSession();
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Creates and configures a WriterAppender that sends log messages through a WebSocket connection.
     *
     * @param ctx the WebSocket context, which contains the session information and allows sending messages
     * @param jobId the identifier for the job, used to assign a unique name to the appender
     * @return a configured WriterAppender instance that writes log messages to the WebSocket session
     */
    @NotNull
    private static WriterAppender getWriterAppender(WsConnectContext ctx, int jobId) {
        Layout layout = new PatternLayout(Logging.DEFAULT_LOG_FORMAT);
        WriterAppender wsAppender = new WriterAppender();
        wsAppender.setLayout(layout);
        wsAppender.setWriter(new java.io.Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                String message = new String(cbuf, off, len);
                if(ctx.session.isOpen()) {
                    ctx.send(message);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        wsAppender.setName("WebSocketAppender-" + jobId);
        wsAppender.setLayout(layout);
        return wsAppender;
    }

    /**
     * Handles the closure of a job log stream associated with a WebSocket context.
     * This method removes the corresponding appender from the logger and cleans up
     * the association within the internal tracking map.
     *
     * @param wsCloseContext the WebSocket close context representing the closed connection
     */
    public void jobLogsStreamOnClose(WsCloseContext wsCloseContext) {
        WriterAppender appender = websocketLoggers.get(wsCloseContext);
        if(appender != null) {
            org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
            rootLogger.removeAppender(appender);
            websocketLoggers.remove(wsCloseContext);
            logger.info("Removed WebSocket appender {} for job logs", wsCloseContext);
        } else {
            logger.info("Could not find WebSocket appender for job logs");
        }
    }

    public void closeDataBaseDataSources() {
        userDataSources.forEach((userId, dataSource) -> {
            if(dataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) dataSource).close();
                } catch (Exception e) {
                    logger.error("Error closing database datasource for user {}", userId, e);
                }
            }
        });
    }

    /**
     * Handles the export of the user's H2 database as a zip file.
     * Uses H2's BACKUP TO command to create a safe binary backup.
     * The backup is streamed to the client as application/octet-stream.
     *
     * @param ctx the Javalin HTTP context
     */
    public void handleDatabaseExport(Context ctx) {
        Path tempDir = null;
        try {
            User user = ctx.attribute("user");
            int userId = user.getIdentifier();
            File targetDbFile = UserController.getDatabaseFile(userId, configuration.getWorkingDirectory());
            // Check if the database size is inferior than MAX_UPLOAD_SIZE
            long dbSize = Files.size(targetDbFile.toPath());
            if(dbSize > MAX_UPLOAD_SIZE) {
                String message = "Database size is too big, max size is " + MAX_UPLOAD_SIZE + " bytes";
                logger.error(message);
                ctx.status(HttpStatus.CONTENT_TOO_LARGE).result(message);
                return;
            }
            DataSource dataSource = fetchUserDataSource(userId);
            tempDir = Files.createTempDirectory("nm_db_export_");
            File backupFile = tempDir.resolve("database.zip").toFile();
            // Add a timeout for the execution of the dump
            try (Connection connection = dataSource.getConnection()) {
                Statement st = connection.createStatement();
                st.setQueryTimeout(MAX_UPLOAD_TIMEOUT);
                st.execute("BACKUP TO '" + backupFile.getAbsolutePath().replace("'", "''") + "'");
                st.setQueryTimeout(0);
            }
            byte[] data = Files.readAllBytes(backupFile.toPath());
            ctx.contentType("application/octet-stream");
            ctx.header("Content-Disposition", "attachment; filename=\"database.zip\"");
            ctx.result(data);
        } catch (Exception e) {
            logger.error("Error exporting database", e);
            ctx.status(500).result("Error exporting database: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try (Stream<Path> paths = Files.walk(tempDir)) {
                    paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Handles the import of an H2 database backup.
     * Closes the existing user connection pool, replaces the database file
     * with the uploaded backup, and lets the pool reconnect on next access.
     * No SQL is executed from the uploaded file — only file replacement.
     *
     * @param ctx the Javalin HTTP context
     */
    public void handleDatabaseImport(Context ctx) {
        Path tempDir = null;
        try {
            User user = ctx.attribute("user");
            int userId = user.getIdentifier();
            var uploadedFile = ctx.uploadedFile("database");
            if (uploadedFile == null) {
                ctx.status(400).result("No database file uploaded");
                return;
            }
            if (uploadedFile.size() > MAX_UPLOAD_SIZE) {
                ctx.status(413).result("Upload too large (max 500 MB)");
                return;
            }
            // Close existing connection pool for this user
            DataSource existingDS = userDataSources.remove(userId);
            if (existingDS instanceof HikariDataSource) {
                ((HikariDataSource) existingDS).close();
            }
            // Extract the H2 backup zip to a temp directory and find the .mv.db file
            File targetDbFile = UserController.getDatabaseFile(userId, configuration.getWorkingDirectory());
            tempDir = Files.createTempDirectory("nm_db_import_");
            File tempZip = tempDir.resolve("uploaded.zip").toFile();
            // Save uploaded file
            try (InputStream is = uploadedFile.content();
                 OutputStream os = new FileOutputStream(tempZip)) {
                is.transferTo(os);
            }
            // Extract .mv.db from the H2 backup zip
            boolean foundDb = false;
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    // Zip Slip protection: use only the file name, ignore directory components
                    String entryName = Path.of(entry.getName()).getFileName().toString();
                    if (entryName.endsWith(".mv.db")) {
                        try (OutputStream os = new FileOutputStream(targetDbFile)) {
                            zis.transferTo(os);
                        }
                        foundDb = true;
                        break;
                    }
                }
            }
            if (!foundDb) {
                ctx.status(400).result("Invalid database backup: no .mv.db file found in zip");
                return;
            }
            ctx.result("Database imported successfully");
        } catch (Exception e) {
            logger.error("Error importing database", e);
            ctx.status(500).result("Error importing database: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try (Stream<Path> paths = Files.walk(tempDir)) {
                    paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Shuts down the server by stopping the job executor service.
     */
    public void shutdown() {
        jobExecutorService.shutdown();
    }
}