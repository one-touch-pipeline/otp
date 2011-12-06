package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan;

/**
 * The ProcessParameterType is the generic description for {@link ProcessParameter}s of a {@link Process}.
 *
 * It contains a name which is unique for a Process, a human readable description
 * and whether it references a domain class or not. The actual values are ProcessParameter
 * which combines a ProcessParameterType with a value.
 *
 * @see ProcessParameter
 * @see JobExecutionPlan
 */
class ProcessParameterType {
    /**
     * The name for this processParameter
     */
    String name
    /**
     * A description for the processParameter
     */
    String description
    /**
     * The JobExecutionPlan for which this process parameter type has been created
     */
    JobExecutionPlan plan

    static constraints = {
        name(nullable: false, blank: false, unique: 'plan')
        description(nullable: true)
        plan(nullable: false)
    }
}
