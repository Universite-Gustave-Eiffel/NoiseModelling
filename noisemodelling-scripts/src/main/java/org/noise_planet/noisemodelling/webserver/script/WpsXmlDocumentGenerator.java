package org.noise_planet.noisemodelling.webserver.script;


import net.opengis.ows11.*;
import net.opengis.wps10.*;
import org.geotools.wps.WPSConfiguration;
import org.geotools.xsd.Encoder;
import org.jspecify.annotations.NonNull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.noise_planet.noisemodelling.webserver.Configuration;
import org.noise_planet.noisemodelling.webserver.NoiseModellingServer;
import org.noise_planet.noisemodelling.webserver.OwsController;
import org.noise_planet.noisemodelling.webserver.database.DatabaseManagement;
import org.noise_planet.noisemodelling.webserver.utilities.Logging;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A utility class for generating WPS (Web Processing Service) XML documents.
 * This class provides methods to construct a DescribeProcess XML for a specific script by
 * defining its inputs, outputs, and other metadata.
 */
public class WpsXmlDocumentGenerator {

    private final static Wps10Factory wpsf = Wps10Factory.eINSTANCE;
    private final static Ows11Factory owsf = Ows11Factory.eINSTANCE;
    private final static Map<Class<?>, String> javaClassToXsdType;
    static {
        javaClassToXsdType = new HashMap<>();
        javaClassToXsdType.put(String.class, "xs:string");
        javaClassToXsdType.put(Boolean.class, "xs:boolean");
        javaClassToXsdType.put(Integer.class, "xs:int");
        javaClassToXsdType.put(Double.class, "xs:double");
    }

    /**
     * Shortcut method to generate wps xml generator string
     * @param value String
     * @return Instance
     */
    private static LanguageStringType languageString(String value) {
        LanguageStringType languageStringType = owsf.createLanguageStringType();
        languageStringType.setValue(value);
        return languageStringType;
    }

    /**
     * Parameter identifier
     * @param value String
     * @return Instance
     */
    private static CodeType codetype(String value) {
        CodeType codeType = owsf.createCodeType();
        codeType.setValue(value);
        return codeType;
    }

    /**
     * Creates and initializes a {@link DomainMetadataType} instance with the specified name.
     * It can be a parameter type (String, number.)
     * @param name String
     * @return Instance
     */
    private static DomainMetadataType domainMetadataType(String name) {
        DomainMetadataType domainMetadataType = owsf.createDomainMetadataType();
        domainMetadataType.setValue(name);
        return domainMetadataType;
    }

    public static ValueType valueType(String value) {
        ValueType valueType = owsf.createValueType();
        valueType.setValue(value);
        return valueType;
    }

    public static void dataInputs(DataInputsType inputs, ScriptInput scriptInput) {
        InputDescriptionType input = wpsf.createInputDescriptionType();
        inputs.getInput().add(input);
        input.setIdentifier(codetype(scriptInput.id));
        input.setTitle(languageString(scriptInput.title));
        input.setAbstract(languageString(scriptInput.description));
        input.setMaxOccurs(scriptInput.maxOccurs < 0 ? BigInteger.valueOf(Long.MAX_VALUE) : BigInteger.valueOf(scriptInput.maxOccurs));
        input.setMinOccurs(BigInteger.valueOf(scriptInput.minOccurs));
        LiteralInputType literalInputType = wpsf.createLiteralInputType();
        input.setLiteralData(literalInputType);
        if (scriptInput.type.equals(Boolean.class)) {
            // Special handling for boolean input
            literalInputType.setDataType(domainMetadataType("xs:boolean"));
            literalInputType.setAllowedValues(owsf.createAllowedValuesType());
            literalInputType.getAllowedValues().getValue().add(valueType("true"));
            literalInputType.getAllowedValues().getValue().add(valueType("false"));
        } else {
            // Set default value
            if(scriptInput.defaultValue != null) {
                literalInputType.setDefaultValue(scriptInput.defaultValue.toString());
            }
            // Generate allowed values
            if(!scriptInput.allowedValues.isEmpty()) {
                literalInputType.setAllowedValues(owsf.createAllowedValuesType());
                for (String allowedValue : scriptInput.allowedValues) {
                    literalInputType.getAllowedValues().getValue().add(valueType(allowedValue));
                }
            } else {
                // If there is restricted values we must not set the data type
                literalInputType.setDataType(domainMetadataType(javaClassToXsdType.getOrDefault(scriptInput.type, "xs:string")));
            }
        }
    }

    public static void processOutputs(ProcessOutputsType outputs, ScriptOutput scriptOutput) {
        OutputDescriptionType output = wpsf.createOutputDescriptionType();
        outputs.getOutput().add(output);
        output.setIdentifier(codetype(scriptOutput.id));
        output.setTitle(languageString(scriptOutput.title));
        output.setAbstract(languageString(scriptOutput.description));
        if(!scriptOutput.type.equals(Geometry.class)) {
            output.setLiteralOutput(wpsf.createLiteralOutputType());
            if(scriptOutput.type.equals(Boolean.class)) {
                output.getLiteralOutput().setDataType(domainMetadataType("xs:boolean"));
            } else {
                output.getLiteralOutput().setDataType(domainMetadataType(javaClassToXsdType.getOrDefault(scriptOutput.type, "xs:string")));
            }
        } else {
            // Geometry output is converted to WKT
            SupportedComplexDataType complex = wpsf.createSupportedComplexDataType();
            output.setComplexOutput(complex);
            complex.setSupported(wpsf.createComplexDataCombinationsType());
            ComplexDataDescriptionType ddt = wpsf.createComplexDataDescriptionType();
            ddt.setMimeType("application/wkt");
            complex.getSupported().getFormat().add(ddt);
            ComplexDataDescriptionType def = wpsf.createComplexDataDescriptionType();
            def.setMimeType(ddt.getMimeType());
            complex.setDefault(wpsf.createComplexDataCombinationType());
            complex.getDefault().setFormat(def);
        }
    }

    /**
     * Generates a WPS DescribeProcess XML for a specific Groovy script.
     *
     * @param wrapper the ScriptWrapper representing the script
     * @return XML string for WPS DescribeProcess
     */
    public static String generateDescribeProcessXML(ScriptMetadata wrapper) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ProcessDescriptionsType processDescriptionsType = wpsf.createProcessDescriptionsType();
        processDescriptionsType.setLang("en");
        processDescriptionsType.setService("WPS");

        ProcessDescriptionType processDescriptionType = wpsf.createProcessDescriptionType();
        processDescriptionsType.getProcessDescription().add(processDescriptionType);
        processDescriptionType.setIdentifier(codetype(wrapper.id));
        processDescriptionType.setTitle(languageString(wrapper.title));
        processDescriptionType.setAbstract(languageString(wrapper.description));
        processDescriptionType.setProcessVersion("1.0.0");
        processDescriptionType.setStoreSupported(true); // complex data output(s) from this process can be requested to be stored by the WPS server
        processDescriptionType.setStatusSupported(true); // support for Asynchronous WPS
        DataInputsType dataInputsType = wpsf.createDataInputsType();
        processDescriptionType.setDataInputs(dataInputsType);
        for (ScriptInput input : wrapper.inputs.values()) {
            dataInputs(dataInputsType, input);
        }
        processDescriptionType.setProcessOutputs(wpsf.createProcessOutputsType());
        for(ScriptOutput output : wrapper.outputs.values()) {
            processOutputs(processDescriptionType.getProcessOutputs(), output);
        }


        Encoder encoder = new Encoder(new WPSConfiguration());
        encoder.encode(processDescriptionsType, new QName("http://www.opengis.net/wps/1.0.0", "ProcessDescriptions"), baos);
        return baos.toString();
    }



    /**
     * Generates a WPS GetCapabilities XML document listing all available scripts.
     *
     * @param scripts the list of available ScriptWrapper instances
     * @return XML string for WPS GetCapabilities
     */
    public static String generateCapabilitiesXML(Map<String, ScriptMetadata> scripts) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        WPSCapabilitiesType capabilities = wpsf.createWPSCapabilitiesType();
        capabilities.setService("WPS");
        capabilities.setVersion("1.0.0");

        ServiceIdentificationType serviceIdentification = owsf.createServiceIdentificationType();
        capabilities.setServiceIdentification(serviceIdentification);
        serviceIdentification.getTitle().add(languageString("NoiseModelling WPS"));
        serviceIdentification.getAbstract().add(languageString("WPS service of NoiseModelling"));

        CodeType serviceType = codetype("WPS");
        serviceIdentification.setServiceType(serviceType);
        serviceIdentification.setServiceTypeVersion ("1.0.0");

        ProcessOfferingsType processOfferings = wpsf.createProcessOfferingsType();
        capabilities.setProcessOfferings(processOfferings);

        for (ScriptMetadata script : scripts.values()) {
            ProcessBriefType process = getProcessBriefType(script);
            processOfferings.getProcess().add(process);
        }

        Encoder encoder = new Encoder(new WPSConfiguration());
        encoder.encode(capabilities, new QName("http://www.opengis.net/wps/1.0.0", "Capabilities"), baos);
        return baos.toString();
    }

    private static @NonNull ProcessBriefType getProcessBriefType(ScriptMetadata script) {
        ProcessBriefType process = wpsf.createProcessBriefType();
        process.setProcessVersion("1.0.0");
        process.setIdentifier(codetype(script.id));
        process.setTitle(languageString(script.title));
        process.setAbstract(languageString(script.description));
        return process;
    }

    /**
     * Generates WPS execute response with status by job state
     */
    @SuppressWarnings("unchecked")
    public static String generateExecuteResponseDocument(Job<?> job, Map<String, Object> jobData, Configuration webServerConfiguration)
            throws IOException, DatatypeConfigurationException, SQLException, ParseException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ExecuteResponseType response = wpsf.createExecuteResponseType();
        response.setLang("en");
        response.setService("WPS");
        if(job != null) {
            response.setProcess(getProcessBriefType(job.executionPlan.scriptMetadata));
        }
        int jobId = (int) jobData.get("id");
        response.setStatusLocation(webServerConfiguration.getWebSiteFullUrl() + "/builder/jobs/" + jobId);
        response.setStatus(wpsf.createStatusType());
        String startDate = jobData.get("startDate").toString();
        // Converts timestamp to XMLGregorianCalendar for standardized serialization
        response.getStatus().setCreationTime(DatabaseManagement.dateToXMLGregorianCalendar(startDate));
        // Sets status details by job state
        switch (JobStates.valueOf(jobData.get("status").toString())) {
            case QUEUED:
                response.getStatus().setProcessAccepted(JobStates.QUEUED.name());
                break;
            case RUNNING:
                response.getStatus().setProcessStarted(wpsf.createProcessStartedType());
                response.getStatus().getProcessStarted().setValue(getLastLoggingLines(webServerConfiguration, jobId));
                // Extracts progression percentage for status encoding
                response.getStatus().getProcessStarted().setPercentCompleted(job == null ? BigInteger.valueOf(0) : job.getProgression());
                break;
            case COMPLETED:
                String output = "";
                if(job != null && job.getExecutionPlan().getOutputs() != null) {
                    output = castJobOutputToString(job.getExecutionPlan().getOutputs());
                }
                // Fetch logs output for this job and attach to response (up to a maximum number of lines)
                String lastLines = getLastLoggingLines(webServerConfiguration, jobId);
                response.getStatus().setProcessSucceeded(lastLines);
                // Copy the wps outputs if available
                response.setProcessOutputs(wpsf.createProcessOutputsType1());
                OutputDataType outputDataType = wpsf.createOutputDataType();
                outputDataType.setIdentifier(codetype("result"));
                DataType data = wpsf.createDataType();
                if(job != null) {
                    outputDataType.setTitle(languageString(job.getExecutionPlan().scriptMetadata.title));
                    // If the output is a Geometry it is a complex output, otherwise it is a literal output
                    if(job.getExecutionPlan().getOutputs() != null && job.getExecutionPlan().getOutputs() instanceof Geometry) {
                        ComplexDataType complexDataType = wpsf.createComplexDataType();
                        complexDataType.setMimeType("application/wkt");
                        complexDataType.getData().add(output);
                        data.setComplexData(complexDataType);
                    } else {
                        LiteralDataType literalDataType = wpsf.createLiteralDataType();
                        literalDataType.setValue(output);
                        data.setLiteralData(literalDataType);
                    }
                }
                outputDataType.setData(data);
                response.getProcessOutputs().getOutput().add(outputDataType);
                break;
            case CANCELED:
            case FAILED:
                response.getStatus().setProcessFailed(wpsf.createProcessFailedType());
                // Attaches exception report to failed process status; generates default if absent
                if(job != null && job.getJobException() != null) {
                    response.getStatus().getProcessFailed().setExceptionReport(generateExceptionDocument(job.getJobException()));
                } else {
                    response.getStatus().getProcessFailed().setExceptionReport(owsf.createExceptionReportType());
                }
                break;
        }
        Encoder encoder = new Encoder(new WPSConfiguration());
        encoder.encode(response, new QName("http://www.opengis.net/wps/1.0.0", "ExecuteResponse"), baos);
        return baos.toString();

    }

    public static @NonNull String getLastLoggingLines(Configuration webServerConfiguration, int jobId) throws IOException {
        String lastLines = "";
        File logFile = new File(webServerConfiguration.getWorkingDirectory(),
                NoiseModellingServer.LOGGING_FILE_NAME);
        if(logFile.exists()) {
            lastLines = Logging.getLastLines(logFile, OwsController.MAXIMUM_LINES_TO_FETCH,
                    Job.getThreadName(jobId), new AtomicInteger());
        }
        return lastLines;
    }

    public static String castJobOutputToString(Object output) {
        if(output instanceof Map<?, ?> && ((Map<?, ?>) output).size() == 1) {
            // Take first key if this is a map [result: "my result"}
            castJobOutputToString(((Map<?, ?>) output).values().iterator().next());
        } else if(output instanceof Geometry) {
            // Convert Geometry to WKT
            WKTWriter wktWriter = new WKTWriter(2);
            return wktWriter.write((Geometry) output);
        }
        return output.toString();
    }

    /**
     * Generates an exception report from stack trace details
     * @return Exception report
     */
    public static ExceptionReportType generateExceptionDocument(Exception ex) {
        ExceptionType e = owsf.createExceptionType();
        e.setExceptionCode(ex.getMessage());
        e.setLocator(ex.getClass().getName());
        for (StackTraceElement traceElement : ex.getStackTrace()) {
            e.getExceptionText().add(traceElement.toString());
        }
        ExceptionReportType report = Ows11Factory.eINSTANCE.createExceptionReportType();
        report.setVersion("2.0");
        report.getException().add(e);
        return report;
    }
}
