package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

/**
 * The Process represents one execution of a JobExecutionPlan.
 *
 * A Process is triggered by a {@link groovy.de.dkfz.tbi.otp.job.processing.StartJob} and generated from a {@link JobExecutionPlan}. It
 * groups all the {@link groovy.de.dkfz.tbi.otp.job.processing.Job}s run for the various {@link JobDefinition}s. For each run groovy.de.dkfz.tbi.otp.job.processing.Job a
 * {@link ProcessingStep} is added to this Process.
 *
 * For each JobExecutionPlan there can be many Processes representing the various executions for
 * different values (e.g. different file name).
 *
 * @see Process
 * @see ProcessingStep
 * @see JobExecutionPlan
 * @see JobDefinition
 * @see groovy.de.dkfz.tbi.otp.job.processing.Job
 * @see groovy.de.dkfz.tbi.otp.job.processing.StartJob
 **/
public class Process implements Serializable {
    /**
     * The Date when this process was started.
     **/
    Date started
    /**
     * The name of the StartJob class which triggered this Process.
     **/
    String startJobClass
    /**
     * The version of the StartJob class which triggered this Process.
     **/
    String startJobVersion
    /**
     * The JobExecutionPlan this Process belongs to
     */
    JobExecutionPlan jobExecutionPlan
    /**
     * Whether there are Jobs still running (false) for this process or not (true).
     **/
    boolean finished = false


    static mapping = {
        finished index: 'finished_idx'
        jobExecutionPlan index: 'job_execution_plan_idx'
    }


    static constraints = {
        jobExecutionPlan(nullable: false)
        started(nullable: false)
        startJobClass(nullable: false, blank: false)
        startJobVersion(nullable: false, blank: false)
    }
}
