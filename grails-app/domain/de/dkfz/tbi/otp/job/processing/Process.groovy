package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * The Process represents one execution of a {@link JobExecutionPlan}.
 *
 * A Process is triggered by a {@link StartJob} and generated from a {@link JobExecutionPlan}. It
 * groups all the {@link Job}s run for the various {@link JobDefinition}s. For each run of a {@link Job} a
 * {@link ProcessingStep} is added to this Process.
 *
 * For each JobExecutionPlan there can be many Processes representing the various executions for
 * different values (e.g. different file name).
 *
 * @see ProcessingStep
 * @see JobExecutionPlan
 * @see JobDefinition
 * @see Job
 * @see StartJob
 **/
public class Process implements Serializable, Commentable, Entity {
    /**
     * The Date when this process was started.
     **/
    Date started
    /**
     * The name of the StartJob class which triggered this Process.
     **/
    String startJobClass
    /**
     * The JobExecutionPlan this Process belongs to
     */
    JobExecutionPlan jobExecutionPlan
    /**
     * Whether there are Jobs still running (false) for this process or not (true).
     **/
    boolean finished = false

    boolean operatorIsAwareOfFailure = false

    /**
     * The value is set if this Process has been created in response to the failure of the referenced Process.
     */
    Process restarted

    Comment comment

    static mapping = {
        finished index: 'finished_idx'
        jobExecutionPlan index: 'job_execution_plan_idx'
    }


    static constraints = {
        jobExecutionPlan(nullable: false)
        started(nullable: false)
        startJobClass(nullable: false, blank: false)
        restarted(nullable: true, unique: true)
        comment(nullable: true)
    }

    ProcessParameterObject getProcessParameterObject() {
        return atMostOneElement(ProcessParameter.findAllByProcess(this))?.toObject()
    }

    @Override
    Project getProject() {
        return getProcessParameterObject()?.project
    }
}
