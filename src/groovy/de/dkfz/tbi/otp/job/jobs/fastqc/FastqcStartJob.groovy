package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.*

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("fastqcStartJob")
@Scope("singleton")
class FastqcStartJob extends AbstractStartJobImpl {

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    ProcessingOptionService optionService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay=10000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        SeqTrack.withTransaction {
            SeqTrack seqTrack = seqTrackService.getSeqTrackReadyForFastqcProcessing()
            if (seqTrack) {
                log.debug "Creating fastqc process for seTrack ${seqTrack}"
                seqTrackService.setFastqcInProgress(seqTrack)
                createProcess(new ProcessParameter(value: seqTrack.id.toString(), className: seqTrack.class.name))
            }
        }
    }

    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "FastqcWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }

    @Override
    protected String getJobExecutionPlanName() {
        return "FastqcWorkflow"
    }
}
