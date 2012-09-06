package de.dkfz.tbi.otp.job.jobs.fastqcSummary


import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component("fastqcSummaryStartJob")
@Scope("singleton")
class FastqcSummaryStartJob extends AbstractStartJobImpl{

    @Autowired
    SeqTrackService seqTrackService

    final int MAX_RUNNING = 1

    @Scheduled(fixedRate=10000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        Run run = seqTrackService.getRunReadyForFastqcSummary()
        if (run) {
            log.debug "Creating fastqc summary process for run ${run}"
            createProcess(new ProcessParameter(value: run.id.toString(), className: run.class.name))
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
