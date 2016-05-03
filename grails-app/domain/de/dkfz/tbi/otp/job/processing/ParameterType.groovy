package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.utils.Entity

/**
 * The ParameterType is the generic description for {@link Parameter}s of a {@link JobDefinition}.
 *
 * It contains a name which is unique for a JobDefinition, a human readable description
 * and whether it references a domain class or not. The actual values are Parameter
 * which combines a ParameterType with a value.
 *
 * The ParameterType allows to easily map the output Parameters from one JobDefinition to
 * the input Parameters of the next Job.
 *
 * @see Parameter
 * @see JobDefinition
 */
class ParameterType implements Serializable, Entity {
    /**
     * The name for this parameter
     */
    String name
    /**
     * A description for the parameter
     */
    String description
    /**
     * If not null it references the Domain Class the Parameter has as instance
     */
    String className
    /**
     * The JobDefinition for which this parameter type has been created
     */
    JobDefinition jobDefinition
    /**
     * The kind for which the Job is used
     */
    ParameterUsage parameterUsage

    static constraints = {
        name(nullable: false, blank: false, unique: ['jobDefinition', 'parameterUsage'])
        description(nullable: true)
        className(nullable: true)
        parameterUsage(nullable: false)
    }
}
