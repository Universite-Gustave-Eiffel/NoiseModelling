/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.javalin.util.JavalinException;
import io.javalin.util.JavalinLogger;
import io.javalin.websocket.WsConfig;
import org.apache.commons.cli.Option;
import org.apache.log4j.PropertyConfigurator;
import org.noise_planet.noisemodelling.VersionUtils;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.noise_planet.noisemodelling.webserver.script.ScriptFileWatchedProcess;
import org.noise_planet.noisemodelling.webserver.secure.*;
import org.noise_planet.noisemodelling.webserver.utilities.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * {@link NoiseModellingServer} is the main class responsible for initializing and running the NoiseModelling web server.
 * It leverages {@link Javalin}(psi_element://io.javalin.Javalin) to serve static content, manage OGC-compliant Web Processing Service (WPS) operations
 * through {@link OwsController}(psi_element://org.noise_planet.noisemodelling.webserver.OwsController), and handle user authentication and management
 * via {@link UserController}(psi_element://org.noise_planet.noisemodelling.webserver.secure.UserController).
 * <p>
 * The server provides functionalities such as:
 * <ul>
 *     <li>Serving static web resources for the application interface.</li>
 *     <li>Exposing OGC WPS endpoints for NoiseModelling computations.</li>
 *     <li>User registration, login, and access control using JWT.</li>
 *     <li>Job management, including logging and cancellation.</li>
 *     <li>Dynamic script reloading by watching for changes in a specified script directory.</li>
 * </ul>
 * This class also handles server shutdown procedures and integrates with a database for persistent storage
 * and user management.
 */
public class NoiseModellingServer {
    public static final String LOGGING_FILE_NAME = "webserver.log";
    protected final Logger logger = LoggerFactory.getLogger(NoiseModellingServer.class);
    protected Javalin app;
    protected Future<?> scriptWatch;
    protected final OwsController owsController;
    protected final Configuration configuration;
    protected final DataSource serverDataSource;
    protected final JWTProvider<User> provider;
    protected final UserController userController;

    public NoiseModellingServer(Configuration configuration) throws IOException, SQLException {
        this.configuration = configuration;
        serverDataSource = DatabaseManagement.createH2DataSource(configuration.workingDirectory, "server",
                configuration.secureBaseAdminUser, configuration.secureBaseAdminPassword,
                configuration.getSecureBaseEncryptionSecret(), false);
        // Initialize server database
        DatabaseManagement.initializeServerDatabaseStructure(serverDataSource, configuration);
        // Initialize an access right system
        provider = JWTProviderFactory.createHMAC512(DatabaseManagement.getJWTSigningKey(serverDataSource));
        userController = new UserController(serverDataSource, provider, configuration);
        owsController  = new OwsController(serverDataSource, provider, configuration);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public DataSource getServerDataSource() {
        return serverDataSource;
    }

    /**
     * Retrieves a user-specific {@link DataSource} instance for database operations.
     * The {@code DataSource} is maintained with a connection pool for reuse between transactions.
     *
     * @param userId the unique identifier of the user for whom the {@code DataSource} is being retrieved.
     * @return the {@code DataSource} instance associated with the specified user.
     * @throws SQLException if an SQL error occurs while retrieving or creating the {@code DataSource}.
     */
    public DataSource getUserDataSource(int userId) throws SQLException {
        return owsController.fetchUserDataSource(userId);
    }

    /**
     * The entry point of the application. This method initializes and starts the server.
     *
     * @param args command-line arguments passed to the application. Not utilized currently.
     */
    public static void main(String[] args) {
        PropertyConfigurator.configure(
                Objects.requireNonNull(NoiseModellingServer.class.getResource("static/log4j.properties")));
        try {
            // Read configuration from command line
            Configuration configuration = Configuration.createConfigurationFromArguments(args);
            if(configuration == null) {
                // Use called with --help or -h argument, just exit after displaying help
                System.exit(0);
            }
            // Initialize additional loggers
            Logging.configureFileLogger(configuration.workingDirectory, LOGGING_FILE_NAME);
            // Create WebServer instance
            NoiseModellingServer noiseModellingServer = new NoiseModellingServer(configuration);
            noiseModellingServer.startServer(!configuration.skipOpenBrowser);
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger("Main");
            logger.error("Can't start NoiseModelling", e);
        }
    }

    /**
     * @return Returns the Javalin application instance.
     */
    public Javalin getJavalinInstance() {
        return app;
    }

    /**
     * Initializes and starts the NoiseModelling server with the specified configuration.
     * The server serves static files, provides endpoints for OGC-compliant operations,
     * and optionally opens a browser pointing to the server's base URL.
     *
     * @param openBrowser indicates whether the default web browser should be opened
     *                    pointing to the server's base URL.
     * @throws IOException if an I/O error occurs during server initialization or script directory resolution.
     */
    public void startServer(boolean openBrowser) throws IOException {
        String rootPath = "/" + configuration.applicationRootUrl;

        configureApp(rootPath);

        scriptWatch = startWatcher(Path.of(configuration.scriptPath), owsController);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Stop watching for script changes
                scriptWatch.cancel(true);
                app.stop();
                // Close all datasource connections
                owsController.closeDataBaseDataSources();
                owsController.shutdown();
                if (serverDataSource instanceof AutoCloseable) {
                    ((AutoCloseable) serverDataSource).close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }));

        app.start(configuration.port);

        if (openBrowser) {
            openBrowser(configuration.getWebSiteFullUrl());
        }
    }

    /**
     * Configure Javalin routes
     * @param rootPath Base url
     */
    protected void configureApp(String rootPath) {
        JavalinLogger.startupInfo = false; // disable startup info
        app = Javalin.create(config -> {
            config.router.contextPath = rootPath;
            config.router.ignoreTrailingSlashes = false;
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.hostedPath = "/builder";
                staticFileConfig.directory = "org/noise_planet/noisemodelling/webserver/static/wpsbuilder/";
                staticFileConfig.roles = configuration.unsecure ? Set.of(Role.ANYONE) : Set.of(Role.RUNNER);
            });
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "org/noise_planet/noisemodelling/webserver/static/root/";
                staticFileConfig.roles = Set.of(Role.ANYONE);
            });
            // Serve documentation if the folder exists
            if(new File("help").exists()) {
                config.staticFiles.add(staticFileConfig -> {
                    staticFileConfig.location = Location.EXTERNAL;
                    staticFileConfig.hostedPath = "/builder/help";
                    staticFileConfig.directory = "help/";
                    staticFileConfig.roles = Set.of(Role.ANYONE);
                });
            } else {
                logger.warn("Documentation folder 'help/' not found, documentation won't be served on the web interface.");
            }
            config.fileRenderer(new JavalinThymeleaf(ThymeleafConfig.buildTemplateConfiguration()));
        });

        app.get("/builder/", this::handleBuilderIndex, configuration.unsecure ? Role.ANYONE : Role.RUNNER);
        app.get("/builder", ctx -> ctx.redirect(ctx.contextPath() + "/builder/"), configuration.unsecure ? Role.ANYONE : Role.RUNNER);

        /*
         * A decode handler which captures the value of a JWT from an
         * authorization header in the form of "Bearer {jwt}". The handler
         * decodes and verifies the JWT then puts the decoded object as
         * a context attribute for future handlers to access directly.
         */
        Handler decodeHandler = JavalinJWT.createCookieDecodeHandler(provider);
        Handler headerDecodeHandler = JavalinJWT.createHeaderDecodeHandler(provider);

        app.before(headerDecodeHandler);
        app.before(decodeHandler);
        app.beforeMatched(new Auth(provider, serverDataSource, configuration)::handleAccess);

        installWpsRoutes();


        installJobsRoutes();
        installUserManagementRoutes();
        installExceptionHandlers();
    }

    protected void handleBuilderIndex(Context ctx) {
        ctx.render("wpsbuilder_index", Map.of("version", "NoiseModelling " + VersionUtils.getVersion()));
    }

    /**
     * Web Processing Service (WPS) API
     */
    protected void installWpsRoutes() {
        app.get("/builder/ows", owsController::handleGet, Role.RUNNER);
        app.post("/builder/ows", owsController::handleWPSPost, Role.RUNNER);
        // Job status using web processing service ExecuteResponseDocument
        app.get("/builder/jobs/{job_id}", owsController::handleJobExecuteStatus, Role.RUNNER);
    }

    protected void installExceptionHandlers() {
        // Exception rendering Handling
        app.error(HttpStatus.UNAUTHORIZED, ctx -> {
            String message = ctx.attributeMap().getOrDefault("messages", "").toString();
            // redirect the user to the login page
            ctx.render("blank", Map.of(
                    "redirectUrl", ctx.contextPath()+"/login",
                    "message", message));
        });
        app.error(HttpStatus.NOT_FOUND, ctx -> {
            logger.info("404 not found on {}", ctx.req().getRequestURI());
            // redirect the user to the login page
            ctx.render("blank", Map.of(
                    "redirectUrl", ctx.contextPath().isEmpty() ? "/" : ctx.contextPath(),
                    "message", "This page does not exists, redirecting to home.."));
        });
    }

    /**
     * Routes for job management and custom WPS Builder operations
     */
    protected void installJobsRoutes() {
        app.get("/job_logs/{job_id}", owsController::jobLogs, Role.RUNNER);
        app.ws("/job_logs_stream/{job_id}", this::manageLogsWebSocket, Role.RUNNER);
        app.post("/jobs/delete/{job_id}", owsController::jobDelete, Role.RUNNER);
        app.post("/jobs/delete_all", owsController::jobDeleteAll, Role.RUNNER);
        app.post("/jobs/cancel/{job_id}", owsController::jobCancel, Role.RUNNER);
        app.get("/jobs", owsController::jobList, Role.RUNNER);
        app.get("/builder/database/export", owsController::handleDatabaseExport, Role.RUNNER);
        app.post("/builder/database/import", owsController::handleDatabaseImport, Role.RUNNER);
    }

    protected void installUserManagementRoutes() {
        app.get("/", userController::index, Role.ANYONE);
        app.get("/login", userController::login, Role.ANYONE);
        app.post("/do_login", userController::doLogin, Role.ANYONE);
        app.get("/register/{token}", userController::register, Role.ANYONE);
        app.post("/do_register/{token}", userController::doRegister, Role.ANYONE);
        app.get("/users", userController::users, Role.ADMINISTRATOR);
        app.post("/users", userController::users, Role.ADMINISTRATOR);
        app.get("/edit_user/{userId}", userController::userEdit,  Role.ADMINISTRATOR);
        app.post("/edit_user/{userId}", userController::userEdit,  Role.ADMINISTRATOR);
        app.get("/logout", userController::logout, Role.ANYONE);
        app.get("/about", userController::about, Role.ADMINISTRATOR);
        app.ws("/memory_stats_stream", this::manageMemoryWebSocket, Role.ADMINISTRATOR);
    }

    /**
     * Opens the default web browser and navigates to the specified URL.
     *
     * @param url the URL to be opened in the default web browser. It must be a properly formatted URI.
     */
    public void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            logger.error("Unable to open the browser : {}", e.getMessage(), e);
        }
    }

    /**
     * Monitors a specified directory and its subdirectories for changes in files.
     * Specifically watches for creation, deletion, and modification events of files with the `.groovy` extension
     * and triggers a script reload using the provided OwsController.
     *
     * @param scriptsDir the root directory to monitor for changes. All subdirectories under this will also be monitored.
     * @param owsController the instance responsible for reloading scripts when a `.groovy` file is changed.
     * @return a Future representing the asynchronous script reload operation.
     */
    protected Future<Boolean> startWatcher(Path scriptsDir, OwsController owsController) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Boolean> task = new ScriptFileWatchedProcess(scriptsDir, owsController);
        return executor.submit(task);
    }

    protected void manageLogsWebSocket(WsConfig ws) {
        ws.onConnect(owsController::jobLogsStreamOnConnect);
        ws.onClose(owsController::jobLogsStreamOnClose);
    }

    protected void manageMemoryWebSocket(WsConfig ws) {
        ws.onConnect(userController::memoryStatsStreamOnConnect);
        ws.onClose(userController::memoryStatsStreamOnClose);
    }
}