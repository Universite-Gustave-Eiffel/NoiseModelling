/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */


package org.noise_planet.noisemodelling.webserver;

import com.atlassian.onetime.core.TOTP;
import com.atlassian.onetime.core.TOTPGenerator;
import com.atlassian.onetime.model.EmailAddress;
import com.atlassian.onetime.model.Issuer;
import com.atlassian.onetime.model.TOTPSecret;
import com.atlassian.onetime.service.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.util.NaiveRateLimit;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.noise_planet.noisemodelling.webserver.secure.*;
import org.noise_planet.noisemodelling.webserver.utilities.FileUtilities;
import org.noise_planet.noisemodelling.webserver.utilities.LibraryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.stream.Collectors;

/**
 * Handle users management
 * Adapted from tutorial material from
 * <a href="https://github.com/javalin/javalin-samples/tree/main/javalin6/javalin-auth-example">javalin-auth-example</a>
 * Do not add SQL queries in this class
 */
public class UserController {
    public static final String ROLE_FIELD_PREPEND = "ROLE_";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DataSource serverDataSource;
    private final JWTProvider<User> provider;
    private final TOTPService totpService;
    private final Configuration configuration;

    private final ScheduledExecutorService memoryStatsScheduler = Executors.newScheduledThreadPool(1);
    private final Set<WsContext> memoryWebSocketContexts = Collections.synchronizedSet(new HashSet<>());

    public UserController(DataSource serverDataSource, JWTProvider<User> provider, Configuration configuration) {
        this.serverDataSource = serverDataSource;
        this.provider = provider;
        this.configuration = configuration;
        TOTPGenerator totpGenerator = new TOTPGenerator();
        TOTPConfiguration totpConfiguration = new TOTPConfiguration();
        totpService = new DefaultTOTPService(totpGenerator, totpConfiguration
        );
    }

    public void login(Context ctx ) {
        List<String> messages = readMessagesArg(ctx);
        ctx.render("login.html", Map.of("messages", messages));
    }

    public void index(Context ctx ) throws SQLException {
        Map<String, Object> data = new HashMap<>();
        // not a restricted area so we do not have access to the user in the attributes
        User user = Auth.getUserFromContext(ctx, serverDataSource, provider);
        if(user != null) {
            data.put("user", user);
        }
        ctx.render("index.html", data);
    }

    public void logout(Context ctx ) {
        JavalinJWT.addTokenToCookie(ctx, "");
        // redirect the user to the page
        ctx.render("blank", Map.of(
                "redirectUrl", ctx.contextPath().isEmpty() ? "/" : ctx.contextPath(),
                "message", "Logout successful, you will be redirected to the application"));
    }

    public void doLogin(Context ctx ) {
        // brute force protection
        NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.MINUTES);
        String totpCode = Optional.ofNullable(ctx.formParam("TOTP_CODE")).orElse("");
        String email = Optional.ofNullable(ctx.formParam("EMAIL")).orElse("");

        try(Connection connection = serverDataSource.getConnection()) {
            String totpSecret = DatabaseManagement.getTotpSecretByUserEmail(connection, email);
            if(totpSecret.isEmpty()) {
                ctx.attribute("messages", "Invalid email or TOTP code");
                login(ctx);
            } else {
                TOTPVerificationResult result = totpService.verify(new TOTP(totpCode),
                        TOTPSecret.Companion.fromBase32EncodedString(totpSecret));
                if (result.isSuccess()) {
                    // Fetch user
                    int userId = DatabaseManagement.getUserIdByUserEmail(connection, email);
                    User user = DatabaseManagement.getUser(connection, userId);
                    String token = provider.generateToken(user);
                    // Register the JWT token in cookie
                    JavalinJWT.addTokenToCookie(ctx, token);
                    logger.info("User {} has logged into the system", email);
                    // redirect the user to the page
                    ctx.render("blank", Map.of(
                            "redirectUrl", ctx.contextPath().isEmpty() ? "/" : ctx.contextPath(),
                            "message", "Login successful, you will be redirected to the application"));
                } else {
                    ctx.attribute("messages", "Invalid email or TOTP code");
                    logger.info("Visitor with email {} failed to login {}", email, ctx.attribute("messages"));
                    login(ctx);
                }
            }
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }


    /**
     * Post method of register page
     * @param ctx Context
     */
    public void doRegister(Context ctx ) {
        String totpCode = ctx.formParam("TOTP_CODE");
        String totpSecret = ctx.formParam("TOTP_SECRET");
        String userToken = ctx.formParam("TOKEN");

        try(Connection connection = serverDataSource.getConnection()) {
            int userIdentifier = DatabaseManagement.getUserByRegisterToken(connection, userToken);
            if (totpCode != null && totpSecret != null && userToken != null && userIdentifier >= 0) {
                User user = DatabaseManagement.getUser(connection, userIdentifier);
                TOTPVerificationResult result = totpService.verify(new TOTP(totpCode),
                        TOTPSecret.Companion.fromBase32EncodedString(totpSecret));
                if(result.isSuccess()) {
                    DatabaseManagement.updateUserTotpToken(connection, user.getIdentifier(), totpSecret);
                    String message = "Account successfully created ! You will be directed to the login page to enter your credentials";
                    // redirect the user to the page
                    ctx.render("blank", Map.of(
                            "redirectUrl", ctx.contextPath() + "/login",
                            "message", message));
                } else {
                    ctx.attribute("messages", "Invalid TOTP code");
                    logger.info("User {} failed to register {}", user.getEmail(), ctx.attribute("messages"));
                    register(ctx);
                }
            } else {
                ctx.attribute("messages", "Invalid register page url, ask your administrator for a new link.");
                logger.info("User with token {} failed to register {}", userToken, ctx.attribute("messages"));
                register(ctx);
            }
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }


    public static String createQRCodeImage(URI totpUri) throws IOException {
        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(totpUri.toString(), BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (WriterException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Format totp secret using block 4 of chars to be more readable for the user
     * @param totpSecret totpSecret string (multiple of 4
     * @return formated totpSecret
     */
    public static String formatTotpSecret(String totpSecret) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < totpSecret.length(); i += 4) {
            formatted.append(totpSecret, i, Math.min(i + 4, totpSecret.length()));
            if (i + 4 < totpSecret.length()) { // Don't add space at the end of string
                formatted.append(' ');
            }
        }
        return formatted.toString();
    }

    public List<String> readMessagesArg(Context ctx) {
        String message = ctx.attributeMap().getOrDefault("messages", "").toString();
        if(message.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(message);
        }
    }

    /**
     * Display register page
     * @param ctx Context
     */
    public void register(Context ctx) {
        // brute force protection
        NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.MINUTES);
        String token = ctx.pathParam("token");
        TOTPSecret totpSecret = RandomSecretProvider.Companion.generateSecret();
        try(Connection connection = serverDataSource.getConnection()) {
            int userIdentifier = DatabaseManagement.getUserByRegisterToken(connection, token);
            if(userIdentifier >= 0) {
                User user = DatabaseManagement.getUser(connection, userIdentifier);
                URI totpUri = totpService.generateTOTPUrl(
                        totpSecret,
                        new EmailAddress(user.getEmail()),
                        new Issuer("NoiseModelling"));
                String qrCodeBytes = createQRCodeImage(totpUri);
                ctx.render("register.html", Map.of(
                        "messages", readMessagesArg(ctx),
                        "token", token,
                        "totpUri", totpUri,
                        "totpSettings", totpUri,
                        "totpSecret", formatTotpSecret(totpSecret.getBase32Encoded()),
                        "qrCodeBytes", qrCodeBytes));
            }  else {
                ctx.render("login.html", Map.of(
                        "messages", List.of("Register/Reset token is no longer valid, ask your administrator for a new link.")));
            }
        } catch (SQLException | IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Render user list HTML page
     * @param ctx web context
     */
    public void about(Context ctx) {
        List<LibraryInfo> libraries = FileUtilities.collectLibraryIdentifiers();
        // Sort by last modified column
        libraries.sort(Comparator.comparingLong(LibraryInfo::getLastModifiedTimeStamp).reversed());
        ctx.render("about", Map.of("libraries", libraries));
    }

    /**
     * Render user list HTML page
     * @param ctx web context
     */
    public void users(Context ctx) {
        try(Connection connection = serverDataSource.getConnection()) {
            String message = "";
            String email = ctx.formParam("USER_EMAIL");
            if (email != null) {
                // Administrator add a new user
                Set<Role> roles = new HashSet<>();
                for (Role role : Role.values()) {
                    boolean selected = ctx.formParam(ROLE_FIELD_PREPEND + role.name()) != null;
                    if (selected) {
                        roles.add(role);
                    }
                }
                try {
                    DatabaseManagement.addUser(connection, email, roles.toArray(new Role[0]));
                    message = "User " + email + " successfully added";
                } catch (SQLException e) {
                    message = "User " + email + " already exists";
                }
            }
            List<Map<String, Object>> table = new ArrayList<>();
            List<User> users = DatabaseManagement.getUsers(connection);
            for(User user : users) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", user.getIdentifier());
                row.put("email", user.getEmail());
                long size = 0;
                File databaseFile = getDatabaseFile(user);
                if(databaseFile.exists()) {
                    size = databaseFile.length();
                }
                row.put("dbSize", FileUtils.byteCountToDisplaySize(size));
                row.put("registerUrl", user.getRegisterUrl(configuration.getWebSiteFullUrl()));
                row.put("groups", user.getRoles().stream().map(Enum::name).collect(Collectors.joining(", ")));
                table.add(row);
            }
            Map<String, Boolean> groups = Map.of(Role.RUNNER.name(), true,
                    Role.ADMINISTRATOR.name(), false);
            ctx.render("users", Map.of("accounts", table, "groups", groups, "message", message));
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }


    /**
     * Render user edit HTML page
     * @param ctx web context
     */
    public void userEdit(Context ctx) {
        try(Connection connection = serverDataSource.getConnection()) {
            List<String> messages = new ArrayList<>();
            int userId = Integer.parseInt(ctx.pathParam("userId"));
            User user = DatabaseManagement.getUser(connection, userId);
            if(user == null) {
                String message = "This user does not exist";
                // redirect the user to the page
                ctx.render("blank", Map.of(
                        "redirectUrl", ctx.contextPath() + "/user_list",
                        "message", message));
                return;
            }
            // Read post data
            boolean delete = ctx.formParam("DELETE_USER") != null;
            boolean deleteDatabase = ctx.formParam("DELETE_DATABASE") != null;
            if(delete) {
                DatabaseManagement.deleteUser(connection, user.getIdentifier());
                messages.add("User " + user.getEmail() + " successfully deleted");
                logger.info("User {} successfully deleted", user.getEmail());
            } else if(deleteDatabase) {
                File databaseFile = getDatabaseFile(user);
                FileUtils.deleteQuietly(databaseFile);
                messages.add("Database for user " + user.getEmail() + " successfully deleted");
                logger.info("Database for user {} successfully deleted", user.getEmail());
            } else {
                String email = ctx.formParam("USER_EMAIL");
                if (email != null) {
                    // Administrator update the user attributes
                    Set<Role> roles = new HashSet<>();
                    for (Role role : Role.values()) {
                        boolean selected = ctx.formParam(ROLE_FIELD_PREPEND + role.name()) != null;
                        if (selected) {
                            roles.add(role);
                        }
                    }
                    if (user.getIdentifier() == 1 && user.isAdministrator()) {
                        // Avoid removing the administrator role of the first account
                        roles.add(Role.ADMINISTRATOR);
                    }
                    // read RESET_TOTP checkbox value
                    boolean resetTotp = ctx.formParam("RESET_TOTP") != null;
                    String token = user.getRegisterToken();
                    if (resetTotp) {
                        token = JWTProviderFactory.generateServerSecretToken();
                    }
                    User updatedUser = new User(user.getIdentifier(), email, roles, token);
                    DatabaseManagement.updateUserAttributes(connection, updatedUser);
                    messages.add("User " + email + " successfully updated");
                    user = updatedUser;
                }
            }
            // Rendering of the form
            Map<String, Object> userFields = new HashMap<>();
            userFields.put("email", user.getEmail());
            Map<String, Boolean> groups = new HashMap<>();
            Set<Role> userRoles = user.getRoles();
            for (Role role : Role.values()) {
                if(role.ordinal() > 0) { // skip anyone
                    groups.put(role.name(), userRoles.contains(role));
                }
            }
            userFields.put("groups", groups);
            userFields.put("messages", messages);
            userFields.put("deletable", user.getIdentifier() > 1);
            ctx.render("user_edit", userFields);
        } catch (SQLException e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new InternalServerErrorResponse();
        }
    }

    /**
     * Handles WebSocket connection for memory stats streaming.
     * @param ctx the WebSocket connect context
     */
    public void memoryStatsStreamOnConnect(WsConnectContext ctx) {
        logger.info("WebSocket connection established for memory stats");
        memoryWebSocketContexts.add(ctx);
        // Start sending memory stats every second
        memoryStatsScheduler.scheduleAtFixedRate(() -> sendMemoryStats(ctx), 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Handles WebSocket disconnection for memory stats streaming.
     * @param wsCloseContext the WebSocket close context
     */
    public void memoryStatsStreamOnClose(WsCloseContext wsCloseContext) {
        logger.info("WebSocket connection closed for memory stats");
        memoryWebSocketContexts.remove(wsCloseContext);
    }

    /**
     * Sends current JVM memory statistics to the connected WebSocket client.
     * @param ctx the WebSocket context
     */
    private void sendMemoryStats(WsContext ctx) {
        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

            long max = heapMemoryUsage.getMax();
            long used = heapMemoryUsage.getUsed();
            long free = max - used;

            // Create JSON-like string
            String memoryStats = String.format("{\"max\": %d, \"used\": %d, \"free\": %d}", max, used, free);

            if (ctx.session.isOpen()) {
                ctx.send(memoryStats);
            }
        } catch (Exception e) {
            logger.error("Error sending memory stats", e);
        }
    }

    /**
     * Retrieves the database file associated with the specified user.
     *
     * @param user The user for whom the database file is being retrieved.
     * @return A File instance representing the user's database file.
     */
    @NotNull
    public File getDatabaseFile(User user) {
        return getDatabaseFile(user.getIdentifier(), configuration.getWorkingDirectory());
    }


    /**
     * Retrieves the database file associated with the specified user.
     *
     * @param userId The identifier of the user for whom the database file is being retrieved.
     * @param workingDirectory The working directory where the database file is located.
     * @return A File instance representing the user's database file.
     */
    public static File getDatabaseFile(int userId, String workingDirectory) {
        return new File(workingDirectory, OwsController.getUserDatabaseName(userId) + ".mv.db");
    }
}
