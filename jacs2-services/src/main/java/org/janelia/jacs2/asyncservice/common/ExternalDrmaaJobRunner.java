package org.janelia.jacs2.asyncservice.common;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.asyncservice.qualifier.ClusterJob;
import org.janelia.jacs2.model.jacsservice.JacsServiceEventTypes;
import org.janelia.jacs2.model.jacsservice.JacsServiceState;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@ClusterJob
public class ExternalDrmaaJobRunner extends AbstractExternalProcessRunner {

    private final Session drmaaSession;

    @Inject
    public ExternalDrmaaJobRunner(Session drmaaSession, JacsServiceDataPersistence jacsServiceDataPersistence, Logger logger) {
        super(jacsServiceDataPersistence, logger);
        this.drmaaSession = drmaaSession;
    }

    @Override
    public void runCmds(ExternalCodeBlock externalCode,
                        Map<String, String> env,
                        String workingDirName,
                        ExternalProcessOutputHandler outStreamHandler,
                        ExternalProcessOutputHandler errStreamHandler,
                        JacsServiceData serviceContext) {
        logger.debug("Begin DRMAA job invocation for {}", serviceContext);
        String processingScript = createProcessingScript(externalCode, workingDirName, serviceContext);
        serviceContext.setState(JacsServiceState.RUNNING);
        this.jacsServiceDataPersistence.update(serviceContext);
        JobTemplate jt = null;
        File outputFile = null;
        File errorFile = null;
        try {
            jt = drmaaSession.createJobTemplate();
            jt.setJobName(serviceContext.getName());
            jt.setRemoteCommand(processingScript);
            jt.setArgs(Collections.emptyList());
            File workingDirectory = setJobWorkingDirectory(jt, workingDirName);
            logger.debug("Using working directory {} for {}", workingDirectory, serviceContext);
            jt.setJobEnvironment(env);
            if (StringUtils.isNotBlank(serviceContext.getInputPath())) {
                jt.setInputPath(":" + serviceContext.getInputPath());
            }
            if (StringUtils.isNotBlank(serviceContext.getOutputPath())) {
                outputFile = new File(serviceContext.getOutputPath());
                Files.createParentDirs(outputFile);
                jt.setOutputPath(":" + outputFile.getAbsolutePath());
            }
            if (StringUtils.isNotBlank(serviceContext.getErrorPath())) {
                errorFile = new File(serviceContext.getErrorPath());
                Files.createParentDirs(errorFile);
                jt.setErrorPath(":" + errorFile.getAbsolutePath());
            }
            String nativeSpec = createNativeSpec(serviceContext);
            if (StringUtils.isNotBlank(nativeSpec)) {
                jt.setNativeSpecification(nativeSpec);
            }
            logger.debug("Start {} using {} with content={}; env={}", serviceContext, processingScript, externalCode, env);
            String jobId = drmaaSession.runJob(jt);
            logger.info("Submitted job {} for {}", jobId, serviceContext);
            serviceContext.addEvent(JacsServiceEventTypes.DRMAA_SUBMIT, String.format("Submitted job %s {%s} running: %s", serviceContext.getName(), jobId, processingScript));
            drmaaSession.deleteJobTemplate(jt);
            jt = null;
            if (outputFile == null) {
                outputFile = new File(workingDirectory, serviceContext.getName() + ".o" + jobId);
            }
            if (errorFile == null) {
                errorFile = new File(workingDirectory, serviceContext.getName() + ".e" + jobId);
            }
            JobInfo jobInfo = drmaaSession.wait(jobId, getTimeout(serviceContext));

            if (jobInfo.wasAborted()) {
                serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ABORTED, String.format("Aborted DRMAA job %s {%s}", serviceContext.getName(), jobId));
                logger.error("Job {} for {} never ran", jobId, serviceContext);
                throw new ComputationException(serviceContext, String.format("Job %s never ran", jobId));
            } else if (jobInfo.hasExited()) {
                logger.info("Job {} for {} completed with exist status {}", jobId, serviceContext, jobInfo.getExitStatus());
                ExternalProcessIOHandler processStdoutHandler;
                try (InputStream outputStream = new FileInputStream(outputFile)) {
                    processStdoutHandler = new ExternalProcessIOHandler(outStreamHandler, outputStream);
                    processStdoutHandler.run();
                }
                ExternalProcessIOHandler processStderrHandler;
                try (InputStream errorStream = new FileInputStream(errorFile)) {
                    processStderrHandler = new ExternalProcessIOHandler(errStreamHandler, errorStream);
                    processStderrHandler.run();
                }
                int jobExitStatus = jobInfo.getExitStatus();
                if (jobExitStatus != 0) {
                    serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ERROR, String.format("DRMAA job %s {%s} exit status %d", serviceContext.getName(), jobId, jobExitStatus));
                    throw new ComputationException(serviceContext, String.format("Job %s completed with status %d", jobId, jobInfo.getExitStatus()));
                } else if (processStdoutHandler.getResult() != null) {
                    serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ERROR, String.format("DRMAA job %s {%s} error %s", serviceContext.getName(), jobId, processStdoutHandler.getResult()));
                    throw new ComputationException(serviceContext, "Process error: " + processStdoutHandler.getResult());
                } else if (processStderrHandler.getResult() != null) {
                    serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ERROR, String.format("DRMAA job %s {%s} error %s", serviceContext.getName(), jobId, processStderrHandler.getResult()));
                    throw new ComputationException(serviceContext, "Process error: " + processStderrHandler.getResult());
                }
                serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_COMPLETED, String.format("DRMAA job %s {%s} completed", serviceContext.getName(), jobId));
                deleteProcessingScript(processingScript);
            } else if (jobInfo.hasSignaled()) {
                serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ERROR, String.format("DRMAA job %s {%s} signaled to terminate %s", serviceContext.getName(), jobId, jobInfo.getTerminatingSignal()));
                logger.warn("Job {} for {} terminated due to signal {}", jobId, serviceContext, jobInfo.getTerminatingSignal());
                throw new ComputationException(serviceContext, String.format("Job %s completed with status %s", jobId, jobInfo.getTerminatingSignal()));
            } else {
                serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ERROR, String.format("DRMAA job %s {%s} unknown error", serviceContext.getName(), jobId));
                logger.warn("Job {} for {} finished with unclear conditions", jobId, serviceContext);
                throw new ComputationException(serviceContext, String.format("Job %s completed with unclear conditions", jobId));
            }
        } catch (Exception e) {
            serviceContext.addEvent(JacsServiceEventTypes.DRMAA_JOB_ERROR, String.format("Error creating DRMAA job %s - %s", serviceContext.getName(), e.getMessage()));
            logger.error("Error creating a DRMAA job {} for {}", processingScript, serviceContext, e);
            throw new ComputationException(serviceContext, e);
        } finally {
            this.jacsServiceDataPersistence.update(serviceContext);
            if (jt != null) {
                try {
                    drmaaSession.deleteJobTemplate(jt);
                } catch (DrmaaException e) {
                    logger.error("Error deleting a DRMAA job {} for {}", processingScript, serviceContext, e);
                }
            }
        }
    }

    private File setJobWorkingDirectory(JobTemplate jt, String workingDirName) {
        File workingDirectory;
        if (StringUtils.isNotBlank(workingDirName)) {
            workingDirectory = new File(workingDirName);
        } else {
            workingDirectory = Files.createTempDir();
        }
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }
        if (!workingDirectory.exists()) {
            throw new IllegalStateException("Cannot create working directory " + workingDirectory.getAbsolutePath());
        }
        try {
            jt.setWorkingDirectory(workingDirectory.getAbsolutePath());
        } catch (DrmaaException e) {
            throw new IllegalStateException(e);
        }
        return workingDirectory;
    }

    private String createNativeSpec(JacsServiceData serviceContext) {
        StringBuilder nativeSpecBuilder = new StringBuilder();
        Map<String, String> jobResources = serviceContext.getResources();
        // append accountID
        if (StringUtils.isNotBlank(jobResources.get("gridAccountId"))) {
            nativeSpecBuilder.append("-A ").append(jobResources.get("gridAccountId")).append(' ');
        }
        // append processing environment
        if (StringUtils.isNotBlank(jobResources.get("gridPE"))) {
            nativeSpecBuilder.append("-pe ").append(jobResources.get("gridPE")).append(' ');
        }
        // append grid queue
        if (StringUtils.isNotBlank(jobResources.get("gridQueue"))) {
            nativeSpecBuilder.append("-q ").append(jobResources.get("gridQueue")).append(' ');
        }
        // append grid resource limits - the resource limits must be specified as a comma delimited list of <name>'='<value>, e.g.
        // gridResourceLimits: "short=true,scalityr=1,scalityw=1,haswell=true"
        if (StringUtils.isNotBlank(jobResources.get("gridResourceLimits"))) {
            nativeSpecBuilder.append("-l ").append(jobResources.get("gridResourceLimits")).append(' ');
        }
        return nativeSpecBuilder.toString();
    }

    private long getTimeout(JacsServiceData serviceContext) {
        Long serviceTimeout = serviceContext.getServiceTimeout();
        if (serviceTimeout == null) {
            return Session.TIMEOUT_WAIT_FOREVER;
        } else {
            return serviceTimeout;
        }
    }
}