/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.administration

import grails.plugin.springsecurity.SpringSecurityService
import grails.validation.ValidationException
import org.springframework.beans.factory.DisposableBean
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator
import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepService
import de.dkfz.tbi.otp.workflowExecution.WorkflowSystemService

import java.util.concurrent.locks.ReentrantLock

/**
 * Service to cleanly shutdown the running application.
 * This service can be used to stop the scheduler and to provide information about the current
 * scheduled shutdown process. E.g. which Jobs are still running.
 * Furthermore the service gets notified when the application finally shuts down and suspends all
 * running but resumable jobs. In case of non-resumable Jobs the service will log a warning
 * message.
 */
class ShutdownService implements DisposableBean {
    // service is not transactional as the database access has to be locked
    static transactional = false

    SchedulerService schedulerService
    SpringSecurityService springSecurityService
    ProcessService processService
    ConfigService configService
    WorkflowSystemService workflowSystemService
    WorkflowStepService workflowStepService
    LogService logService

    // all methods in this service contain critical sections to not start two shutdowns
    private final ReentrantLock lock = new ReentrantLock()
    private static boolean shutdownSuccessful = false

    @Override
    void destroy() {
        ShutdownInformation info = getCurrentPlannedShutdown()
        if (info) {
            ShutdownInformation.withTransaction {
                info.succeeded = new Date()
                info.save(flush: true)
                suspendResumeableJobs()
                logRunningWorkflowSteps()
                log.info("OTP is shutting down")
                DicomAuditLogger.logActorStop(EventOutcomeIndicator.SUCCESS, info.initiatedBy.username)
            }
        } else {
            log.warn("OTP is shutting down without a planned shutdown")
            DicomAuditLogger.logActorStop(EventOutcomeIndicator.SUCCESS,
                    configService.getDicomInstanceName())
        }
        shutdownSuccessful = true
    }

    /**
     * Write logs about the running workflow steps.
     * Should be called before the shutdown.
     */
    private void logRunningWorkflowSteps() {
        List<WorkflowStep> workflowSteps = workflowStepService.runningWorkflowSteps()

        workflowSteps.each { WorkflowStep step ->
            if (step.workflowRun.jobCanBeRestarted) {
                logService.addSimpleLogEntry(step, "Step will be stopped soon. It can be restarted.")
            } else {
                logService.addSimpleLogEntry(step, "This step is not restartable, but the server will shutdown now.")
            }
        }
    }

    @Deprecated
    private void suspendResumeableJobs() {
        List<ProcessingStep> runningJobs = schedulerService.retrieveRunningProcessingSteps()
        runningJobs.each { ProcessingStep step ->
            if (isJobResumable(step)) {
                suspendProcessingStep(step)
                log.info("ProcessingStep ${step.id} has been suspended")
            } else {
                log.warn("ProcessingStep ${step.id} is not resumable, but the server is shutting down")
            }
        }
    }

    /**
     * Prepares the Server for clean Shutdown.
     * The scheduler gets stopped, so that no new Processes or ProcessingSteps get started.
     * @param reason The reason why the server is being shut down. This is logged in the database.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Errors planShutdown(String reason) {
        lock.lock()
        try {
            ShutdownInformation.withTransaction {
                User user = User.findByUsername(springSecurityService.authentication.principal.username)
                ShutdownInformation info = new ShutdownInformation(initiatedBy: user, initiated: new Date(), reason: reason)
                info.save(flush: true)
                schedulerService.suspendScheduler()
                workflowSystemService.stopWorkflowSystem()
            }
        } catch (ValidationException e) {
            return e.errors
        } finally {
            lock.unlock()
        }
    }

    /**
     * Cancels a currently running shutdown process.
     * The scheduler gets started again.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    Errors cancelShutdown() throws OtpException {
        lock.lock()
        try {
            ShutdownInformation.withTransaction {
                ShutdownInformation info = getCurrentPlannedShutdown()
                if (!info) {
                    throw new OtpException('Canceling Shutdown failed since there is no shutdown in progress')
                }
                info.canceledBy = User.findByUsername(springSecurityService.authentication.principal.username)
                info.canceled = new Date()
                info.save(flush: true)
                schedulerService.resumeScheduler()
                workflowSystemService.startWorkflowSystem()
            }
        } catch (ValidationException e) {
            return e.errors
        } finally {
            lock.unlock()
        }
    }

    /**
     * Retrieves the shutdown information for the currently planned shutdown if any.
     * @return Currently planned shutdown information or null if there is no planned shutdown.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ShutdownInformation getCurrentPlannedShutdown() {
        return ShutdownInformation.findBySucceededIsNullAndCanceledIsNull()
    }

    /**
     * Retrieves a list of all currently running ProcessingSteps.
     * @return The List of currently running processing steps.
     */
    @Deprecated
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<ProcessingStep> getRunningJobs() {
        return schedulerService.retrieveRunningProcessingSteps()
    }

    /**
     * Retrieve a list of all running workflowSteps where the workflowRuns are resumable.
     * @return list of running and restartable workflowSteps
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<WorkflowStep> getRestartableRunningWorkflowSteps() {
        return workflowStepService.runningWorkflowSteps().findAll({
            it.workflowRun.jobCanBeRestarted
        })
    }

    /**
     * Retrieve a list of all running workflowSteps where the workflowRuns are not resumable.
     * @return list of running and not restartable workflowSteps
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<WorkflowStep> getNonRestartableRunningWorkflowSteps() {
        return workflowStepService.runningWorkflowSteps().findAll({
            !it.workflowRun.jobCanBeRestarted
        })
    }

    boolean isShutdownSuccessful() {
        return shutdownSuccessful
    }

    /**
     * Safely determines if the job of that processing step is resumeable.
     */
    @Deprecated
    boolean isJobResumable(ProcessingStep step) {
        try {
            return schedulerService.isJobResumable(step)
        } catch (final Throwable e) {
            log.warn("Failed to determine whether ProcessingStep ${step.id} is resumable. Treating it as not resumable.", e)
            return false
        }
    }

    @Deprecated
    private void suspendProcessingStep(ProcessingStep step) {
        processService.setOperatorIsAwareOfFailure(step.process, false)
        ProcessingStepUpdate update = new ProcessingStepUpdate(
                date: new Date(),
                state: ExecutionState.SUSPENDED,
                previous: step.latestProcessingStepUpdate,
                processingStep: step
        )
        update.save(flush: true)
    }
}
