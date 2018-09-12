package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ProcessParameterService

class SeqTrackController {

    SeqTrackService seqTrackService
    ProcessParameterService processParameterService

    Map show() {
        if (!params.id) {
            params.id = "0"
        }
        SeqTrack seqTrack = seqTrackService.getSeqTrack(params.id)
        if (!seqTrack) {
            response.sendError(404)
            return
        }
        List<JobExecutionPlan> jobExecutionPlans = processParameterService.getAllJobExecutionPlansBySeqTrackAndClass(seqTrack.id as String, SeqTrack.class.name)

        return [
            seqTrack: seqTrack,
            jobExecutionPlans: jobExecutionPlans,
        ]
    }

}
