package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.utils.Entity

/**
 * The JobDecision defines a possible outcome of a {@link DecisionJob}.
 * It does not contain any information about the next {@link JobDefinition}
 * to be executed.
 *
 * @see JobDefinition
 * @see DecidingJobDefinition
 * @see DecisionJob
 * @see DecisionMapping
 */
class JobDecision implements Entity {
    /**
     * The JobDefinition this decision belongs to. It is not the definition the decision points to!
     */
    DecidingJobDefinition jobDefinition
    /**
     * The name of the decision. May be used by the Job Implementation to set it's decision.
     */
    String name
    /**
     * A descriptive name for the usage in the interface.
     */
    String description

    static constraints = {
        jobDefinition(nullable: false)
        name(nullable: false, blank: false, unique: 'jobDefinition')
        description(nullable: false, blank: true)
    }
}
