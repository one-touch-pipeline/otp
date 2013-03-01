package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

class ProcessParameterService {

    List<JobExecutionPlan> getAllJobExecutionPlansBySeqTrackAndClass(String seqTrackId, String className) {
        List<ProcessParameter> processParameters = ProcessParameter.findAllByValueAndClassName(seqTrackId, className)
        List<JobExecutionPlan> jobExecutionPlans = []
        processParameters.each { ProcessParameter processParameter ->
            jobExecutionPlans << processParameter.process.jobExecutionPlan
        }
        return jobExecutionPlans
    }
}
