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
    static belongsTo = [process: Process]
    static hasMany = [input: Parameter, output: Parameter]
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
}
