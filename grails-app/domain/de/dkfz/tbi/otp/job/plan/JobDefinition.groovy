package de.dkfz.tbi.otp.job.plan

class JobDefinition {
    static belongsTo = [plan: JobExecutionPlan]
    /**
     * The JobExecutionPlan this JobDefinition belongs to.
     */
    JobExecutionPlan plan
    /**
     * The previous JobDefinition in the execution plan before this one.
     * For the first JobDefinition this is null.
     */
    JobDefinition previous
    /**
     * The next JobDefinition in the execution plan after this one.
     * For the last JobDefinition this is null.
     */
    JobDefinition next
    /**
     * A descriptive name for this JobDefinition
     */
    String name
    /**
     * The name of the Spring Bean which implements the Job to be executed for this JobDefinition
     */
    String bean

    static constraints = {
        name(nullable: false, blank: false, unique: 'plan')
        bean(nullable: false, blank: false)
        previous(nullable: true)
        next(nullable: true)
        plan(nullable: false)
    }
}
