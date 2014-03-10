package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.job.processing.ExecutionState
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.ProcessingStepUpdate
import de.dkfz.tbi.otp.security.User
import java.util.concurrent.locks.ReentrantLock
import org.springframework.beans.factory.DisposableBean
import org.springframework.security.access.prepost.PreAuthorize

/**
 * Service to cleanly shutdown the running application.
 * This service can be used to stop the scheduler and to provide information about the current
 * scheduled shutdown process. E.g. which Jobs are still running.
 * Furthermore the service gets notified when the application finally shuts down and suspends all
 * running but resumable jobs. In case of non-resumable Jobs the service will log a warning
 * message.
 **/
class ShutdownService implements DisposableBean {
    // service is not transactional as the database access has to be locked
    static transactional = false
    /**
     * Dependency Injection of GrailsApplication.
     **/
    def grailsApplication
    /**
     * Dependency Injection of SchedulerService.
     * Required to suspend and resume the scheduler
     **/
    def schedulerService
    /**
     * Dependency Injection of SpringSecurityService
     **/
    def springSecurityService
    // all methods in this service contain critical sections to not start two shutdowns
    private final ReentrantLock lock = new ReentrantLock()

    void destroy() {
        ShutdownInformation.withNewSession { session ->
            if (isShutdownPlanned()) {
                ShutdownInformation info = ShutdownInformation.findBySucceededIsNullAndCanceledIsNull()
                if (!info) {
                    log.error("Shutdown Information is missing")
                    return
                }
                info.succeeded = new Date()
                if (!info.validate()) {
                    info.error("Succeeded date for Shutdown Information could not be stored")
                }
                if (!info.save(flush: true)) {
                    info.error("Succeeded date for Shutdown Information could not be stored")
                }
                // TODO: check that all jobs have really stopped
                List<ProcessingStep> runningJobs = schedulerService.retrieveRunningProcessingSteps()
                runningJobs.each { ProcessingStep step ->
                    boolean jobIsResumable
                    try {
                        jobIsResumable = schedulerService.isJobResumable(step)
                    } catch (final Throwable e) {
                        log.warn("Failed to determine whether ProcessingStep ${step.id} is resumable. Treating it as not resumable.", e)
                        jobIsResumable = false
                    }
                    if (jobIsResumable) {
                        ProcessingStepUpdate update = new ProcessingStepUpdate(
                            date: new Date(),
                            state: ExecutionState.SUSPENDED,
                            previous: step.latestProcessingStepUpdate,
                            processingStep: step
                        )
                        if (!update.save(flush: true)) {
                            log.error("ProcessingStep ${step.id} could not be suspended")
                        }
                        log.info("ProcessingStep ${step.id} has been suspended")
                    } else {
                        log.warn("ProcessingStep ${step.id} is not resumable, but the server is shutting down")
                    }
                }
                log.info("OTP is shutting down")
            } else {
                log.warn("OTP is shutting down without a planned shutdown")
            }
        }
    }

    /**
     * Prepares the Server for clean Shutdown.
     * The scheduler gets stopped, so that no new Processes or ProcessingSteps get started.
     * @param reason The reason why the server is being shut down. This is logged in the database.
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void planShutdown(String reason) {
        lock.lock()
        try {
            if (isShutdownPlanned()) {
                // TODO: throw exception
                return
            }
            ShutdownInformation.withTransaction { status ->
                User user = User.findByUsername(springSecurityService.authentication.principal.username)
                ShutdownInformation info = new ShutdownInformation(initiatedBy: user, initiated: new Date(), reason: reason)
                if (!info.validate()) {
                    println info.errors
                    status.setRollbackOnly()
                    // TODO: throw exception
                }
                if (!info.save(flush: true)) {
                    status.setRollbackOnly()
                    // TODO: throw exception
                }
            }
            schedulerService.suspendScheduler()
        } finally {
            lock.unlock()
        }
    }

    /**
     * Cancels a currently running shutdown process.
     * The scheduler gets started again.
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    void cancelShutdown() {
        lock.lock()
        try {
            if (!isShutdownPlanned()) {
                // TODO: throw Exception
            }
            ShutdownInformation.withTransaction { status ->
                ShutdownInformation info = ShutdownInformation.findBySucceededIsNullAndCanceledIsNull()
                if (!info) {
                    status.setRollbackOnly()
                    // TODO: throw exception
                }
                info.canceledBy = User.findByUsername(springSecurityService.authentication.principal.username)
                info.canceled = new Date()
                if (!info.validate()) {
                    println info.errors
                    status.setRollbackOnly()
                    // TODO: throw exception
                }
                if (!info.save(flush: true)) {
                    status.setRollbackOnly()
                    // TODO: throw exception
                }
            }
            schedulerService.resumeScheduler()
        } finally {
            lock.unlock()
        }
    }

    /**
     * Checks whether a shutdown is currently planned.
     * @return true if there is a running shutdown.
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    boolean isShutdownPlanned() {
        boolean planned = false
        lock.lock()
        try {
            ShutdownInformation.withTransaction {
                ShutdownInformation info = ShutdownInformation.findBySucceededIsNullAndCanceledIsNull()
                if (info) {
                    planned = true
                } else {
                    planned = false
                }
            }
        } finally {
            lock.unlock()
        }
        return planned
    }

    /**
     * Retrieves the shutdown information for the currently planned shutdown if any.
     * @return Currently planned shutdown information or null if there is no planned shutdown.
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    ShutdownInformation getCurrentPlannedShutdown() {
        return ShutdownInformation.findBySucceededIsNullAndCanceledIsNull()
    }

    /**
     * Retrieves a list of all currently running ProcessingSteps.
     * @return The List of currently running processing steps.
     **/
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    List<ProcessingStep> getRunningJobs() {
        return schedulerService.retrieveRunningProcessingSteps()
    }

    /**
     * Calls {@link SchedulerService#isJobResumable(ProcessingStep)}.
     **/
    boolean isJobResumable(ProcessingStep step) {
        return schedulerService.isJobResumable(step)
    }
}
