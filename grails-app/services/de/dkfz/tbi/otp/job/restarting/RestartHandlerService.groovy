package de.dkfz.tbi.otp.job.restarting

import de.dkfz.tbi.otp.job.plan.JobErrorDefinition
import de.dkfz.tbi.otp.job.processing.Job
import de.dkfz.tbi.otp.job.processing.ProcessingStep

class RestartHandlerService {

    RestartCheckerService restartCheckerService

    RestartParseService restartParseService

    RestartActionService restartActionService


    void handleRestart(Job job) {
        ProcessingStep step = job.processingStep

        job.log.debug("Starting auto-restart handler.")

        if (restartCheckerService.isJobAlreadyRestarted(step)) {
            job.log.debug("Stopping, because job has already been restarted.")
            return
        }

        if (restartCheckerService.isWorkflowAlreadyRestarted(step)) {
            job.log.debug("Stopping, because workflow has already been restarted.")
            return
        }

        if (!(restartCheckerService.canJobBeRestarted(job) || restartCheckerService.canWorkflowBeRestarted(step))) {
            job.log.debug("Stopping, because neither job nor workflow are restartable.")
            return
        }

        List<JobErrorDefinition> jobErrorDefinitions = JobErrorDefinition.createCriteria().list {
            jobDefinitions {
                eq('id', step.jobDefinition.id)
            }
            eq('type', JobErrorDefinition.Type.MESSAGE)
        }

        if (jobErrorDefinitions.empty) {
            job.log.debug("Stopping, because no error patterns are defined for the job.")
            return
        }

        JobErrorDefinition.Action action = restartParseService.handleTypeMessage(job, jobErrorDefinitions)

        restartActionService.handleAction(action, job)
    }
}
