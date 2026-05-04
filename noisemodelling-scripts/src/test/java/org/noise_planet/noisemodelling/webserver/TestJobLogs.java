/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.webserver;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.noise_planet.noisemodelling.webserver.utilities.Logging;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TestJobLogs {

    /**
     * Check if the most recent logging rows have been fetched by this method
     * @throws IOException
     */
    @Test
    public void testFilter() throws IOException, URISyntaxException {
        AtomicInteger fetchedLines = new AtomicInteger(0);
        URL resourceFile = TestJobLogs.class.getResource("test.log");
        assertNotNull(resourceFile);
        String lastLines = Logging.getLastLines(
                new File(resourceFile.toURI()),
                -1, "JOB_16", fetchedLines);
        assertEquals(7, fetchedLines.get());
        assertLinesMatch(Arrays.asList("[JOB_16][JOB_16] ERROR 2026-02-24 12:06:11 - Command java -v",
                " exit-status: 127",
                "[sshd-SshClient[1e3d79ba]-nio2-thread-1][JOB_16] ERROR 2026-02-24 12:06:11 - bash: line 1: java: command not found",
                "[JOB_16][JOB_16] INFO  2026-02-24 12:06:11 - Successfully connected to the server localhost",
                "[sshd-SshClient[1e3d79ba]-nio2-thread-4][JOB_16] WARN  2026-02-24 12:06:11 - No server key configured. Trusting the server automatically (not recommended for production).",
                "[JOB_16][org.apache.sshd.client.config.hosts.DefaultConfigFileHostEntryResolver] INFO  2026-02-24 12:06:11 - resolveEffectiveResolver(testuser@localhost:2222) loaded 4 entries from /Users/user/.ssh/config",
                "[JOB_16][org.apache.sshd.common.io.DefaultIoServiceFactoryFactory] INFO  2026-02-24 12:06:11 - No detected/configured IoServiceFactoryFactory; using Nio2ServiceFactoryFactory"), Arrays.asList(lastLines.split("\\r?\\n|\\r")));
    }

    /**
     * Check if the most recent logging rows have been fetched by this method
     * @throws IOException
     */
    @Test
    public void testFilterLimited() throws IOException, URISyntaxException {
        AtomicInteger fetchedLines = new AtomicInteger(0);
        URL resourceFile = TestJobLogs.class.getResource("test.log");
        assertNotNull(resourceFile);
        String lastLines = Logging.getLastLines(
                new File(resourceFile.toURI()),
                2, "JOB_16", fetchedLines);
        assertEquals(2, fetchedLines.get());
        assertLinesMatch(Arrays.asList("[JOB_16][JOB_16] ERROR 2026-02-24 12:06:11 - Command java -v"
                , " exit-status: 127"), Arrays.asList(lastLines.split("\\r?\\n|\\r")));
    }


    /**
     * Check of other job logs not included
     *
     * @throws IOException
     */
    @Test
    public void testFilterExceptionOtherJob() throws IOException, URISyntaxException {
        AtomicInteger fetchedLines = new AtomicInteger(0);
        URL resourceFile = TestJobLogs.class.getResource("exception_test.log");
        assertNotNull(resourceFile);
        String lastLines = Logging.getLastLines(new File(resourceFile.toURI()), -1, "JOB_9", fetchedLines);
        assertEquals(3, fetchedLines.get());
        assertEquals("[JOB_9][JOB_9] INFO  15 déc. 16:23:25 - End : Display database\n" + "[JOB_9][JOB_9] INFO  15 déc. 16:23:25 - " +
                "inputs {_progression=org.noise_planet.noisemodelling.pathfinder.utils.profiler" +
                ".RootProgressVisitor@34f93c74, _configuration=org.noise_planet.noisemodelling.webserver" +
                ".Configuration@30cb6bcc, showColumns=true}\n" + "[JOB_9][JOB_9] INFO  15 déc. 16:23:25 - Start : Display " +
                "database\n", lastLines);
    }

    /**
     * Check of job logs contain exception traceback
     *
     * @throws IOException
     */
    @Test
    public void testFilterExceptionJobTraceback() throws IOException, URISyntaxException {
        AtomicInteger fetchedLines = new AtomicInteger(0);
        URL resourceFile = TestJobLogs.class.getResource("exception_test.log");
        assertNotNull(resourceFile);
        String lastLines = Logging.getLastLines(new File(resourceFile.toURI()), -1, "JOB_8", fetchedLines);
        assertEquals(74, fetchedLines.get());
        assertEquals("[JOB_8][JOB_8] ERROR 15 déc. 15:47:09 - Error executing WPS <p0:Execute xmlns:p0=\"http://www.opengis" + ".net/wps/1.0.0\" service=\"WPS\" version=\"1.0.0\"><p1:Identifier xmlns:p1=\"http://www.opengis" + ".net/ows/1.1\">Database_Manager:Add_Primary_Key</p1:Identifier><p0:DataInputs><p0:Input><p1" + ":Identifier xmlns:p1=\"http://www.opengis.net/ows/1" + ".1\">pkName</p1:Identifier><p0:Data><p0:LiteralData>ID</p0:LiteralData></p0:Data></p0:Input><p0" + ":Input><p1:Identifier xmlns:p1=\"http://www.opengis.net/ows/1" + ".1\">tableName</p1:Identifier><p0:Data><p0:LiteralData>test</p0:LiteralData></p0:Data></p0:Input" + "></p0:DataInputs><p0:ResponseForm><p0:RawDataOutput><p1:Identifier xmlns:p1=\"http://www.opengis" + ".net/ows/1.1\">result</p1:Identifier></p0:RawDataOutput></p0:ResponseForm></p0:Execute>\n" + "java" + ".util.concurrent.ExecutionException: java.lang.RuntimeException: org.codehaus.groovy.runtime" + ".InvokerInvocationException: java.sql.SQLException: Table TEST not found.\n" + "\tat java.base/java" + ".util.concurrent.FutureTask.report(FutureTask.java:122)\n" + "\tat java.base/java.util.concurrent" + ".FutureTask.get(FutureTask.java:205)\n" + "\tat org.noise_planet.noisemodelling.webserver.OwsController" + ".handleWPSPost(OwsController.java:381)\n" + "\tat io.javalin.router.Endpoint.handle(Endpoint.kt:52)" + "\n" + "\tat io.javalin.router.ParsedEndpoint.handle(ParsedEndpoint.kt:15)\n" + "\tat io.javalin.http" + ".servlet.DefaultTasks.HTTP$lambda$11$lambda$9$lambda$8(DefaultTasks.kt:55)\n" + "\tat io.javalin" + ".http.servlet.JavalinServlet.handleTask(JavalinServlet.kt:99)\n" + "\tat io.javalin.http.servlet" + ".JavalinServlet.handleSync(JavalinServlet.kt:64)\n" + "\tat io.javalin.http.servlet.JavalinServlet" + ".handle(JavalinServlet.kt:50)\n" + "\tat io.javalin.http.servlet.JavalinServlet.service" + "(JavalinServlet.kt:30)\n" + "\tat jakarta.servlet.http.HttpServlet.service(HttpServlet.java:587)\n" + "\tat io.javalin.jetty.JavalinJettyServlet.service(JavalinJettyServlet.kt:52)\n" + "\tat jakarta.servlet.http.HttpServlet.service(HttpServlet.java:587)\n" + "\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:764)\n" + "\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:529)\n" + "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:221)\n" + "\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:1580)\n" + "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextHandle(ScopedHandler.java:221)\n" + "\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1381)\n" + "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:176)\n" + "\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:484)\n" + "\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:1553)\n" + "\tat org.eclipse.jetty.server.handler.ScopedHandler.nextScope(ScopedHandler.java:174)\n" + "\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1303)\n" + "\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:129)\n" + "\tat org.eclipse.jetty.server.handler.StatisticsHandler.handle(StatisticsHandler.java:173)\n" + "\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:122)\n" + "\tat org.eclipse.jetty.server.Server.handle(Server.java:563)\n" + "\tat org.eclipse.jetty.server.HttpChannel$RequestDispatchable.dispatch(HttpChannel.java:1598)\n" + "\tat org.eclipse.jetty.server.HttpChannel.dispatch(HttpChannel.java:753)\n" + "\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:501)\n" + "\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:287)\n" + "\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:314)\n" + "\tat org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:100)\n" + "\tat org.eclipse.jetty.io.SelectableChannelEndPoint$1.run(SelectableChannelEndPoint.java:53)\n" + "\tat org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.runTask(AdaptiveExecutionStrategy.java:421)\n" + "\tat org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.consumeTask(AdaptiveExecutionStrategy.java:390)\n" + "\tat org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.tryProduce(AdaptiveExecutionStrategy.java:277)\n" + "\tat org.eclipse.jetty.util.thread.strategy.AdaptiveExecutionStrategy.run(AdaptiveExecutionStrategy.java:199)\n" + "\tat org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread.run(ReservedThreadExecutor.java:411)\n" + "\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:969)\n" + "\tat org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.doRunJob(QueuedThreadPool.java:1194)\n" + "\tat org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:1149)\n" + "\tat java.base/java.lang.Thread.run(Thread.java:829)\n" + "Caused by: java.lang.RuntimeException: org.codehaus.groovy.runtime.InvokerInvocationException: java.sql.SQLException: Table TEST not found.\n" + "\tat org.noise_planet.noisemodelling.webserver.script.Job.call(Job.java:118)\n" + "\tat java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)\n" + "\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)\n" + "\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)\n" + "\t... 1 more\n" + "Caused by: org.codehaus.groovy.runtime.InvokerInvocationException: java.sql.SQLException: Table TEST not found.\n" + "\tat org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:343)\n" + "\tat groovy.lang.MetaMethod.doMethodInvoke(MetaMethod.java:274)\n" + "\tat groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1240)\n" + "\tat groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1013)\n" + "\tat groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:798)\n" + "\tat groovy.lang.GroovyObject.invokeMethod(GroovyObject.java:39)\n" + "\tat groovy.lang.Script.invokeMethod(Script.java:101)\n" + "\tat org.noise_planet.noisemodelling.webserver.script.Job.call(Job.java:112)\n" + "\t... 4 more\n" + "Caused by: java.sql.SQLException: Table TEST not found.\n" + "\tat org.h2gis.utilities.JDBCUtilities.getIntegerPrimaryKey(JDBCUtilities.java:446)\n" + "\tat org.codehaus.groovy.vmplugin.v8.IndyInterface.fromCache(IndyInterface.java:344)\n" + "\tat org.noise_planet.noisemodelling.scripts.Database_Manager.Add_Primary_Key.exec(Add_Primary_Key.groovy:84)\n" + "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" + "\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" + "\tat java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" + "\tat java.base/java.lang.reflect.Method.invoke(Method.java:566)\n" + "\tat org.codehaus.groovy.reflection.CachedMethod.invoke(CachedMethod.java:338)\n" + "\t... 11 more\n" + "[JOB_8][JOB_8] INFO  15 déc. 15:47:09 - Start : Add primary key column or constraint\n" + "[JOB_8][JOB_8] INFO  15 déc. 15:47:09 - inputs {pkName=ID, _progression=org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor@16c4f9e7, _configuration=org.noise_planet.noisemodelling.webserver.Configuration@30cb6bcc, tableName=test}\n", lastLines);
    }
}