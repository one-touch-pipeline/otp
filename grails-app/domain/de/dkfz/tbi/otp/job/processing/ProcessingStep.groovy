package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.utils.*
import grails.util.*
import org.hibernate.*

/**
 * A ProcessingStep represents one execution of a Job for a JobDefinition.
 *
 * The ProcessingStep class is mostly just a logging facility to log which {@link Job} got
 * executed with which input parameters and what output parameters are generated. As well it
 * can be used to keep track of the status updates of the running Job. These are stored as
 * {@link ProcessingStepUpdate}s and added to this class.
 *
 * Each ProcessingStep belongs to one {@link Process} and was generated from a {@link JobDefinition}.
 * Based on the output parameters the framework will decide which is the next JobDefinition to use
 * to generate a new Job and ProcessingStep.
 *
 * @see Process
 * @see ProcessingStepUpdate
 * @see JobDefinition
 * @see Job
 * @see Parameter
 **/
public class ProcessingStep implements Serializable, Entity {
    /**
     * Input Parameters added to this ProcessingStep.
     */
    Collection<Parameter> input
    /**
     * Output Parameters generated during this ProcessingStep.
     */
    Collection<Parameter> output
    static hasMany = [input: Parameter, output: Parameter]
    /**
     * The JobDefinition this ProcessingStep is generated from.
     **/
    JobDefinition jobDefinition
    /**
     * The name of the Job class which is used for this ProcessingStep.
     **/
    String jobClass
    /**
     * The Process this ProcessingStep belongs to.
     */
    Process process
    /**
     * The previous ProcessingStep. Is null for the first ProcessingStep of a Process.
     */
    ProcessingStep previous
    /**
     * The next ProcessingStep. Is null as long as the processing step has not been set and for the last ProcessingStep
     * of a Process.
     */
    ProcessingStep next


    static mapping = {
        next index: 'next_idx'
        process index: 'process_idx'
    }


    static constraints = {
        jobDefinition(nullable: false)
        jobClass(nullable: true, empty: false)
        process(nullable: false, validator: { Process val, ProcessingStep obj ->
            if (!val) {
                return false
            }
            if (val.jobExecutionPlan?.id != obj.jobDefinition?.plan?.id) {
                return "jobExecutionPlan"
            }
            return true
        })
        previous(nullable: true, validator: { ProcessingStep val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            if (val.process?.id != obj.process?.id) {
                return "process"
            }
            if (val.jobDefinition?.id == obj.jobDefinition?.id) {
                return "jobDefinition"
            }
            if (val == obj.next) {
                return "next"
            }
            return true
        })
        next(nullable: true, validator: { ProcessingStep val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            if (val.process?.id != obj.process?.id) {
                return "process"
            }
            if (val.jobDefinition?.id == obj.jobDefinition?.id) {
                return "jobDefinition"
            }
            if (val == obj.previous) {
                return "previous"
            }
            return true
        })
        input(validator: { Collection<Parameter> val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            List<String> errors = []
            val.each { Parameter param ->
                if (param.type.jobDefinition.id != obj.jobDefinition.id) {
                    errors << "invalid.jobDefinition"
                }
                if (param.type.parameterUsage != ParameterUsage.INPUT) {
                    errors << "invalid.parameterUsage"
                }
                // check for uniqueness of parameter types
                for (Parameter p in obj.input) {
                    if (p == param) {
                        continue
                    }
                    if (p.type == param.type) {
                        errors << "unique.type"
                    }
                }
            }
            if (!errors) {
                return true
            }
            return errors
        })
        output(validator: { Collection<Parameter> val, ProcessingStep obj ->
            if (!val) {
                return true
            }
            List<String> errors = []
            val.each { Parameter param ->
                if (param.type.jobDefinition.id != obj.jobDefinition.id) {
                    errors << "invalid.jobDefinition"
                }
                if (param.type.parameterUsage != ParameterUsage.OUTPUT && param.type.parameterUsage != ParameterUsage.PASSTHROUGH) {
                    errors << "invalid.parameterUsage"
                }
                // check for uniqueness of parameter types
                for (Parameter p in obj.output) {
                    if (p == param) {
                        continue
                    }
                    if (p.type == param.type) {
                        errors << "unique.type"
                    }
                }
            }
            if (!errors) {
                return true
            }
            return errors
        })
    }

    /**
     * @return The latest {@link ProcessingStepUpdate} belonging to this {@link ProcessingStep} or
     * <code>null</code> if this {@link ProcessingStep} has no {@link ProcessingStepUpdate}s.
     */
    public ProcessingStepUpdate getLatestProcessingStepUpdate() {
        return ProcessingStepUpdate.findByProcessingStep(this, [sort: "id", order: "desc"])
    }

    /**
     * @return The first {@link ProcessingStepUpdate} belonging to this {@link ProcessingStep} or
     * <code>null</code> if this {@link ProcessingStep} has no {@link ProcessingStepUpdate}s.
     */
    public ProcessingStepUpdate getFirstProcessingStepUpdate() {
        return ProcessingStepUpdate.findByProcessingStep(this, [sort: "id", order: "asc"])
    }

    /**
     * Convenience method to retrieve the non-qualified class name of the {@link Job} used for this {@link ProcessingStep}.
     *
     * @return the non-qualified class name, or <code>null</code> if {@link #jobClass} is <code>null</code>.
     */
    String getNonQualifiedJobClass() {
        jobClass?.split('\\.')?.last()
    }

    /**
     * Returns the object with the specified ID or throws an exception if no such object exists.
     */
    static ProcessingStep getInstance(final long id) {
        final ProcessingStep instance = ProcessingStep.get(id)
        if (instance == null) {
            throw new RuntimeException("No ProcessingStep with ID ${id} found in database.")
        }
        return instance
    }

    public String getClusterJobName() {
        String env
        switch (Environment.current) {
            case Environment.PRODUCTION:
                env = 'prod'
                break
            case Environment.DEVELOPMENT:
                env = 'devel'
                break
            default:
                env = Environment.current.name.toLowerCase()
        }
        String psId  = this.id
        String psClass = this.getNonQualifiedJobClass()
        String psWorkflow = this.process.jobExecutionPlan.toString()
        String pid = this.getProcessParameterObject()?.individual?.pid
        return [
                'otp',
                env,
                pid,
                psWorkflow,
                psId,
                psClass
        ].findAll().join('_')
    }

    public boolean belongsToMultiJob() {
        Class jobClass = Class.forName(jobClass, true, getClass().getClassLoader())
        return AbstractMultiJob.isAssignableFrom(jobClass)
    }

    ProcessParameterObject getProcessParameterObject() {
        return process.processParameterObject
    }

    public static ProcessingStep findTopMostProcessingStep(ProcessingStep step) {
        if (RestartedProcessingStep.isAssignableFrom(Hibernate.getClass(step))) {
            if (step.original) {
                return findTopMostProcessingStep(step.original)
            }
        }
        return step
    }

    JobExecutionPlan getJobExecutionPlan() {
        return process.jobExecutionPlan
    }
}
