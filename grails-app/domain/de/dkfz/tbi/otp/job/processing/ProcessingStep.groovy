package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition

/**
 * A ProcessingStep represents one execution of a groovy.de.dkfz.tbi.otp.job.processing.Job for a JobDefinition.
 *
 * The ProcessingStep class is mostly just a logging facility to log which {@link groovy.de.dkfz.tbi.otp.job.processing.Job} got
 * executed with which input parameters and what output parameters are generated. As well it
 * can be used to keep track of the status updates of the running groovy.de.dkfz.tbi.otp.job.processing.Job. These are stored as
 * {@link ProcessingStepUpdate}s and added to this class.
 *
 * Each ProcessingStep belongs to one {@link Process} and was generated from a {@link JobDefinition}.
 * Based on the output parameters the framework will decide which is the next JobDefinition to use
 * to generate a new groovy.de.dkfz.tbi.otp.job.processing.Job and ProcessingStep.
 *
 * @see Process
 * @see ProcessingStepUpdate
 * @see JobDefinition
 * @see groovy.de.dkfz.tbi.otp.job.processing.Job
 * @see Parameter
 **/
public class ProcessingStep implements Serializable {
    /**
     * Input Parameters added to this ProcessingStep.
     */
    Collection<Parameter> input
    /**
     * Output Parameters generated during this ProcessingStep.
     */
    Collection<Parameter> output
    /**
     * The history of state updates of this ProcessingStep
     */
    Collection<ProcessingStepUpdate> updates
    static hasMany = [input: Parameter, output: Parameter, updates: ProcessingStepUpdate]
    /**
     * The JobDefinition this ProcessingStep is generated from.
     **/
    JobDefinition jobDefinition
    /**
     * The name of the groovy.de.dkfz.tbi.otp.job.processing.Job class which is used for this ProcessingStep.
     **/
    String jobClass
    /**
     * The version of the groovy.de.dkfz.tbi.otp.job.processing.Job class which is used for this ProcessingStep.
     **/
    String jobVersion
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

    static constraints = {
        jobDefinition(nullable: false)
        jobClass(nullable: true, empty: false)
        jobVersion(nullable: true, empty: false)
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
            if (val.process != obj.process) {
                return "process"
            }
            if (val.jobDefinition == obj.jobDefinition) {
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
            if (val.process != obj.process) {
                return "process"
            }
            if (val.jobDefinition == obj.jobDefinition) {
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
                if (param.type.usage != ParameterUsage.INPUT) {
                    errors << "invalid.usage"
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
                if (param.type.jobDefinition != obj.jobDefinition) {
                    errors << "invalid.jobDefinition"
                }
                if (param.type.usage != ParameterUsage.OUTPUT && param.type.usage != ParameterUsage.PASSTHROUGH) {
                    errors << "invalid.usage"
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
}
