package de.dkfz.tbi.otp.job.processing

import org.springframework.context.ApplicationEvent

/**
 * An ApplicationEvent notifying interested parties about a change
 * in a JobExecutionPlan which requires re-fetching the JobExecutionPlan
 * from database.
 */
class JobExecutionPlanChangedEvent extends ApplicationEvent {
    /**
     * The id of the JobExecutionPlan which got changed.
     */
    Long planId

    JobExecutionPlanChangedEvent(Object source, Long planId) {
        super(source)
        this.planId = planId
    }

}
