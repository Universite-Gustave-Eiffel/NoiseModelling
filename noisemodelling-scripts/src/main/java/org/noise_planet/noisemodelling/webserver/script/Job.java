/**
 * NoiseModelling is a library capable of producing noise maps. It can be freely used either for research and education, as well as by experts in a professional use.
 * <p>
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 * <p>
 * Official webpage : http://noise-planet.org/noisemodelling.html
 * Contact: contact@noise-planet.org
 */

package org.noise_planet.noisemodelling.webserver.script;

import groovy.lang.GroovyShell;
import groovy.lang.MetaMethod;
import groovy.lang.Script;
import org.h2gis.api.ProgressVisitor;
import org.jetbrains.annotations.NotNull;
import org.noise_planet.noisemodelling.webserver.Configuration;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.noise_planet.noisemodelling.pathfinder.utils.profiler.RootProgressVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Manage the execution of a Groovy Script
 */
public class Job<T> implements Callable<T> {
    private static final Logger logger = LoggerFactory.getLogger(Job.class);
    /** NoiseModelling DataBase for the user */
    public static final int WPS_RESPONSE_LOGGING_LENGTH_LIMIT = 2000;
    protected DataSource userDataSource;
    protected DataSource serverDataSource;
    protected boolean isRunning = false;
    protected int userId;
    protected int jobId;
    protected Configuration configuration;
    protected Future<T> future;
    protected ProgressVisitor progressVisitor;
    protected ExecutionPlan executionPlan;
    protected Exception jobException = null;

    public Job(int userId, ExecutionPlan executionPlan,
               DataSource serverDataSource, DataSource userDataSource, Configuration configuration) throws SQLException {
        this.userId = userId;
        this.executionPlan = executionPlan;
        this.configuration = configuration;
        this.userDataSource = userDataSource;
        this.serverDataSource = serverDataSource;
        progressVisitor = new RootProgressVisitor(1, false, 0);
        try (Connection connection = serverDataSource.getConnection()) {
            this.jobId = DatabaseManagement.createJob(connection, userId, executionPlan.scriptMetadata.id);
            progressVisitor.addPropertyChangeListener("PROGRESS" , new ProgressionTracker(serverDataSource, jobId));
        }
    }

    void setJobState(JobStates newState) {
        try (Connection connection = serverDataSource.getConnection()) {
            DatabaseManagement.setJobState(connection, jobId, newState.name());
        } catch (SQLException | SecurityException ex) {
            logger.error(ex.getLocalizedMessage(), ex);
        }
    }


    void setJobProgression(int progression) {
        try (Connection connection = serverDataSource.getConnection()) {
            DatabaseManagement.setJobProgression(connection, jobId, progression);
        } catch (SQLException | SecurityException ex) {
            logger.error(ex.getLocalizedMessage(), ex);
        }
    }

    /**
     * Get the user id of the job
     * @return User id
     */
    public int getUserId() {
        return userId;
    }

    void onJobEnd() throws SQLException {
        try (Connection connection = serverDataSource.getConnection()) {
            DatabaseManagement.setJobEndTime(connection, jobId);
        } catch (SQLException | SecurityException ex) {
            logger.error(ex.getLocalizedMessage(), ex);
        }
    }

    public Future<T> getFuture() {
        return future;
    }

    public void setFuture(Future<T> future) {
        this.future = future;
    }

    @Override
    public T call() throws Exception {
        // Change the Thread name to match the logging filter to the logging messages of this job
        Thread.currentThread().setName(getThreadName(jobId));
        isRunning = true;
        setJobState(JobStates.RUNNING);
        // Follow the execution plan by executing instances of ExecutionPlan on inputs
        ExecutionPlan currentPlan = executionPlan;
        // The currentPlan is executing because the output of the currentPlan
        // is the input (parentPlanInputName) of the parent plan
        Stack<ExecutionPlan> parentPlan = new Stack<>();
        Stack<String> parentPlanInputName = new Stack<>();
        T returnData = null;
        try {
            while (currentPlan != null) {
                // Check inputs of the current plan to find the next plan to execute
                // If one of the input is an ExecutionPlan and not a literal value, it is the next plan to execute
                boolean recheck = false;
                for (Map.Entry<String, Object> input : currentPlan.inputs.entrySet()) {
                    if (input.getValue() instanceof ExecutionPlan) {
                        parentPlan.push(currentPlan);
                        parentPlanInputName.push(input.getKey());
                        currentPlan = (ExecutionPlan) input.getValue();
                        recheck = true;
                        break;
                    }
                }
                if (recheck) {
                    // The current plan has changed, we need to recheck the inputs
                    continue;
                }
                Object ret = runScript(currentPlan, progressVisitor, userDataSource);
                @SuppressWarnings("unchecked") T castedReturn = (T) ret;
                returnData = castedReturn;
                if(!parentPlan.isEmpty()) {
                    // Update the value of the parent plan input
                    if(currentPlan.chainedOutputKey.isEmpty() || !(currentPlan.outputs instanceof Map
                            && ((Map) currentPlan.outputs).containsKey(currentPlan.chainedOutputKey))) {
                        parentPlan.peek().inputs.put(parentPlanInputName.pop(), currentPlan.outputs);
                    } else {
                        Map<String, Object> outputs = (Map<String, Object>) currentPlan.outputs;
                        parentPlan.peek().inputs.put(parentPlanInputName.pop(), outputs.get(currentPlan.chainedOutputKey));
                    }
                }
                currentPlan = parentPlan.isEmpty() ? null : parentPlan.pop();
            }
            setJobState(JobStates.COMPLETED);
            setJobProgression(100);
        } catch (Exception ex) {
            setJobState(JobStates.FAILED);
            logger.error("Job failed", ex);
            jobException = ex;
            throw new RuntimeException(ex);
        } finally {
            isRunning = false;
            onJobEnd();
        }
        return returnData;
    }

    /**
     * Retrieves the execution plan associated with the job.
     * The execution plan encapsulates the metadata, inputs, outputs,
     * and other details required for script execution within the job.
     *
     * @return The execution plan instance linked to this job.
     */
    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }

    /**
     * Retrieves the exception associated with the job, if any.
     *
     * @return The exception that occurred during the job execution, or null if no exception was thrown.
     */
    public Exception getJobException() {
        return jobException;
    }


    /**
     * Executes a Groovy script defined in the given execution plan. The script must have an `exec` method
     * with one of the following signatures:
     * - `exec(Connection connection, Map input)`
     * - `exec(DataSource dataSource, Map input)`
     * - `exec(Connection connection, Map input, ProgressVisitor progressVisitor)`
     * - `exec(DataSource dataSource, Map input, ProgressVisitor progressVisitor)`
     * <p>
     * This method handles script execution by dynamically invoking the appropriate `exec` method
     * and passing in the required arguments, which may include a database connection, input parameters,
     * and a progress visitor. The outputs of the script, if any, are stored in the execution plan.
     *
     * @param currentPlan The execution plan that contains the script metadata, input parameters, and stores outputs.
     * @param progressVisitor An instance of `ProgressVisitor` used for reporting progress during script execution.
     * @param userDataSource The data source to provide a database connection for the script
     *
     * @return The result produced by the executed script, or null if no result is returned.
     *
     * @throws IOException If an I/O error occurs during script execution.
     * @throws SQLException If a database access error occurs.
     * @throws RuntimeException If the `exec` method's argument types are invalid or other runtime exceptions occur.
     */
    public static Object runScript(ExecutionPlan currentPlan, ProgressVisitor progressVisitor, DataSource userDataSource) throws IOException, SQLException {
        Object returnData = null;
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse(currentPlan.scriptMetadata.path.toFile());
        // Check expected arguments
        List<MetaMethod> methods = script.getMetaClass().getMethods();
        MetaMethod execMetaMethod = null;
        // Take the exec method with the most arguments, in case there are multiple exec methods,
        // we want to use the one with the most arguments to provide more features to the script author
        for (MetaMethod method : methods) {
            if (method.getName().equals("exec")) {
                if (execMetaMethod == null || method.getNativeParameterTypes().length > execMetaMethod.getNativeParameterTypes().length) {
                    execMetaMethod = method;
                }
            }
        }
        boolean useConnection = true; //first argument is a connection input
        boolean useProgressVisitor = false; // third argument is a ProgressVisitor
        if (execMetaMethod != null) {
            // 2. Access the native parameter types
            Class[] parameterTypes = execMetaMethod.getNativeParameterTypes();
            Class<?> firstArgClass = parameterTypes[0];
            if (firstArgClass.equals(DataSource.class)) {
                useConnection = false;
            } else if (!firstArgClass.equals(Object.class) && !firstArgClass.equals(Connection.class)) {
                throw new RuntimeException("Invalid first argument type for exec method in " + currentPlan.scriptMetadata.id);
            }
            if (parameterTypes.length >= 3 && parameterTypes[2].equals(ProgressVisitor.class)) {
                useProgressVisitor = true;
            }
            // Exec method signature can be:
            // def exec(Connection connection, Map input)
            // def exec(DataSource dataSource, Map input)
            // def exec(Connection connection, Map input, ProgressVisitor progressVisitor)
            // def exec(DataSource dataSource, Map input, ProgressVisitor progressVisitor)
            Object[] args = new Object[useProgressVisitor ? 3 : 2];
            args[1] = currentPlan.inputs;
            if (useProgressVisitor) {
                args[2] = progressVisitor;
            }
            Object ret;
            logger.info("Executing script {}", currentPlan.scriptMetadata.id);
            if (useConnection) {
                // Open the connection to the database
                try (Connection connection = userDataSource.getConnection()) {
                    args[0] = connection;
                    ret = execMetaMethod.invoke(script, args);
                }
            } else {
                args[0] = userDataSource;
                ret = execMetaMethod.invoke(script, args);
            }
            if (ret != null) {
                // Unchecked cast is unavoidable due to type erasure with generics
                // The script author is responsible for returning the correct type
                currentPlan.outputs = ret;
                returnData = ret;
            }
        }
        String outputString = currentPlan.outputs != null ? currentPlan.outputs.toString() : "null";
        logger.info("Script {} executed with result {}",
                currentPlan.scriptMetadata.id,
                outputString.length() > WPS_RESPONSE_LOGGING_LENGTH_LIMIT ?
                        outputString.substring(0, WPS_RESPONSE_LOGGING_LENGTH_LIMIT) +
                                "... [TRUNCATED]" : outputString);
        return returnData;
    }

    public void cancel() {
        progressVisitor.cancel();
    }

    public boolean isRunning() {
        return isRunning;
    }

    @NotNull
    public static String getThreadName(int jobId) {
        return String.format("JOB_%d", jobId);
    }

    /**
     * @return Job id
     */
    public int getId() {
        return jobId;
    }

    public BigInteger getProgression() {
        return BigInteger.valueOf(Math.round(progressVisitor.getProgression()) * 100);
    }
}
