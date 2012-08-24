package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsdata.*
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

    final int MAX_RUNNING = 8

    @Scheduled(fixedRate=10000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        SeqTrack seqTrack = seqTrackService.getSeqTrackReadyForFastqcProcessing()
        if (seqTrack) {
            println "Creating fastqc process for seTrack ${seqTrack}"
            seqTrackService.setFastqcInProgress(seqTrack)
            createProcess(new ProcessParameter(value: seqTrack.id.toString(), className: seqTrack.class.name))
        }
    }

    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        return (n < MAX_RUNNING)
    }
}