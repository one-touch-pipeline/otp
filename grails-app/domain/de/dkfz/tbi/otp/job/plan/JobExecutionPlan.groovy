package de.dkfz.tbi.otp.job.plan

import de.dkfz.tbi.otp.job.processing.ProcessParameter


class JobExecutionPlan {
    /**
     * A descriptive name for this Job Execution Plan
     */
    String name
    /**
     * The previous version of this Job Execution Plan
     */
    JobExecutionPlan previousPlan
    /**
     * The version Number for this version of the Plan
     */
    int planVersion
    /**
     * Whether the Plan is obsoleted, that is there is a Job Execution Plan
     * which has this Plan as its previousPlan.
     */
    boolean obsoleted
    /**
     * The Job Definition for the first Job to be run when a Process gets created.
     */
    JobDefinition firstJob
    /**
     * The JobDefinition describing the StartJob
     */
    StartJobDefinition startJob
    /**
     * Whether this Job Execution Plan is currently enabled or disabled. If it is disabled
     * no new Job will be started for this Plan.
     */
    boolean enabled
    /**
     * Number of Processes which finished successful for this JobExecutionPlan
     */
    int finishedSuccessful = 0
    /**
     * The parameter which is static for the process
     */
    ProcessParameter processParameter
    /**
     * The number of Processes can be limited to a certain values if needed.
     * If the value is negative there is no limit.
     * The default value is -1, indicating that there is no limitation.
     */
    int numberOfAllowedProcesses = -1

    static constraints = {
        name(nullable: false, blank: false, unique: 'planVersion')
        planVersion(min: 0)
        previousPlan(nullable: true, validator: { JobExecutionPlan value, JobExecutionPlan current ->
            if (value) {
                return current.planVersion > 0
            } else {
                return current.planVersion == 0
            }
        })
        // firstJob needs to be nullable as JobDefinition has a dependency on JobExecutionPlan and this circle could not be solved in the database
        firstJob(nullable: true)
        // firstJob needs to be nullable as JobDefinition has a dependency on JobExecutionPlan and this circle could not be solved in the database
        startJob(nullable: true)
        processParameter(nullable: true, blank: true)
        numberOfAllowedProcesses(nullable: true, blank: true)
    }
}
