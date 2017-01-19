package org.janelia.jacs2.service.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.ProcessingLocation;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public abstract class AbstractExeBasedServiceProcessor<R> extends AbstractServiceProcessor<R> {

    protected static final String DY_LIBRARY_PATH_VARNAME = "LD_LIBRARY_PATH";


    private final String executablesBaseDir;
    private final Instance<ExternalProcessRunner> serviceRunners;

    public AbstractExeBasedServiceProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                            ServiceComputationFactory computationFactory,
                                            JacsServiceDataPersistence jacsServiceDataPersistence,
                                            @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                            @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                                            @Any Instance<ExternalProcessRunner> serviceRunners,
                                            Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.serviceRunners = serviceRunners;
        this.executablesBaseDir = executablesBaseDir;
    }

    @Override
    protected ServiceComputation<R> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return invokeExternalProcess(jacsServiceData)
                .thenApply(r -> {
                    R result = collectResult(preProcessingResult, jacsServiceData);
                    setResult(result, jacsServiceData);
                    return result;
                });
    }

    protected abstract R collectResult(Object preProcessingResult, JacsServiceData jacsServiceData);

    protected abstract List<String> prepareCmdArgs(JacsServiceData jacsServiceData);

    protected abstract Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData);

    protected Optional<String> getEnvVar(String varName) {
        return Optional.ofNullable(System.getenv(varName));
    }

    protected String getFullExecutableName(String... execPathComponents) {
        Path cmdPath;
        if (StringUtils.isNotBlank(executablesBaseDir)) {
            cmdPath = Paths.get(executablesBaseDir, execPathComponents);
        } else {
            cmdPath = Paths.get("", execPathComponents);
        }
        return cmdPath.toString();
    }

    protected String getUpdatedEnvValue(String varName, String addedValue) {
        Preconditions.checkArgument(StringUtils.isNotBlank(addedValue), "Cannot update environment variable " + varName + " with a null or empty value");
        Optional<String> currentValue = getEnvVar(varName);
        if (currentValue.isPresent()) {
            return currentValue.get().endsWith(":")  ? currentValue.get() + addedValue : currentValue.get() + ":" + addedValue;
        } else {
            return addedValue;
        }
    }

    protected String outputStreamHandler(InputStream outStream) {
        return streamHandler(outStream, s -> {if (StringUtils.isNotBlank(s)) logger.debug(s);});
    }

    protected String errStreamHandler(InputStream outStream) {
        return streamHandler(outStream, s -> {if (StringUtils.isNotBlank(s)) logger.error(s);});
    }

    private String streamHandler(InputStream outStream, Consumer<String> logWriter) {
        BufferedReader outputReader = new BufferedReader(new InputStreamReader(outStream));
        String error = null;
        for (; ; ) {
            try {
                String l = outputReader.readLine();
                if (l == null) break;
                logWriter.accept(l);
                if (checkForErrors(l)) {
                    error = l;
                }
            } catch (IOException re) {
                logger.error("Error reading process output", re);
                return "Error reading process output";
            }
        }
        return error;
    }

    protected boolean checkForErrors(String l) {
        if (StringUtils.isNotBlank(l) && l.matches("(?i:.*(error|exception).*)")) {
            logger.error(l);
            return true;
        } else {
            return false;
        }
    }

    protected ServiceComputation<Void> invokeExternalProcess(JacsServiceData jacsServiceData) {
        List<String> args = prepareCmdArgs(jacsServiceData);
        Map<String, String> env = prepareEnvironment(jacsServiceData);
        return computationFactory.<Void>newComputation()
                .supply(() -> {
                    getProcessRunner(jacsServiceData.getProcessingLocation()).runCmd(
                            jacsServiceData.getServiceCmd(),
                            args,
                            env,
                            getWorkingDirectory(jacsServiceData).toString(),
                            this::outputStreamHandler,
                            this::errStreamHandler,
                            jacsServiceData);
                    return null;
                });
    }

    private ExternalProcessRunner getProcessRunner(ProcessingLocation processingLocation) {
        ProcessingLocation location = processingLocation == null ? ProcessingLocation.LOCAL : processingLocation;
        for (ExternalProcessRunner serviceRunner : serviceRunners) {
            if (serviceRunner.getClass().isAnnotationPresent(location.getProcessingAnnotationClass())) {
                return serviceRunner;
            }
        }
        throw new IllegalArgumentException("Unsupported runner: " + processingLocation);
    }

}