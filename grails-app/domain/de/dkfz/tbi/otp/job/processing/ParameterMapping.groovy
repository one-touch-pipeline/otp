package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition

/**
 * The ParameterMapping describes the mapping of an output {@link Parameter}
 * to the input Parameter of the next {@link JobDefinition}.
 *
 *
 */
class ParameterMapping {
    /**
     * The ParameterType of the output Parameter of the previous Job
     */
    ParameterType from
    /**
     * The ParameterType of the input Parameter of the next Job
     */
    ParameterType to
    /**
     * The Next Job to which this mapping belongs to.
     */
    JobDefinition job

    static constraints = {
        from(nullable: false, unique: 'to', validator: { ParameterType value, ParameterMapping mapping ->
            if (value.jobDefinition == mapping.to?.jobDefinition) {
                return "jobDefinition"
            }
            if (value.usage != ParameterUsage.OUTPUT && value.usage != ParameterUsage.PASSTHROUGH) {
                return "usage"
            }
        })
        to(nullable: false, unique: 'from', validator: { ParameterType value, ParameterMapping mapping ->
            if (value.jobDefinition == mapping.from?.jobDefinition) {
                return "jobDefinition"
            }
            if (value.usage != ParameterUsage.INPUT && value.usage != ParameterUsage.PASSTHROUGH) {
                return "usage"
            }
        })
        job(nullable: false, validator: { JobDefinition value, ParameterMapping mapping ->
            return value == mapping.to?.jobDefinition
        })
    }
}
