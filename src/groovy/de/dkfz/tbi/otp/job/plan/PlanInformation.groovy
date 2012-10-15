package de.dkfz.tbi.otp.job.plan

/**
 * Class representing a serialized form of the JobExecutionPlan
 * including information about all JobDefinitions, all Parameters,
 * Mappings and so on.
 */
class PlanInformation implements Serializable {

    /**
     * The name of the JobExecutionPlan
     */
    String name
    /**
     * All the Jobs of the JobExecutionPlan, the actual connections between
     * the Jobs are provided by the connections.
     */
    List<JobInformation> jobs = []
    /**
     * All the connections between the Jobs.
     */
    List<JobConnection> connections = []

    /**
     * Creates the PlanInformation for the given JobExecutionPlan
     * @param plan
     * @return
     */
    public static PlanInformation fromPlan(JobExecutionPlan plan) {
        PlanInformation ret = new PlanInformation()
        ret.name = plan.name

        ret.jobs << JobInformation.fromJob(plan.startJob)
        ret.connections << new JobConnection(plan.startJob.id, plan.firstJob.id)
        JobDefinition job = plan.firstJob
        while (job) {
            ret.jobs << JobInformation.fromJob(job)
            if (job.next) {
                ret.connections << new JobConnection(job.id, job.next.id)
            }
            job = job.next
        }
        return ret
    }
}
