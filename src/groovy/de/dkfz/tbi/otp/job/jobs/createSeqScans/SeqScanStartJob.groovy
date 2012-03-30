package de.dkfz.tbi.otp.job.jobs.createSeqScans

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.MergingAssignment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("seqScanStartJob")
@Scope("singleton")
class SeqScanStartJob extends AbstractStartJobImpl  {

    final int MAX_RUNNING = 1
    final String name = "seqScanWorkflow"
    final String hql = "FROM SeqTrack as track WHERE track.id not in (SELECT seqTrack.id from MergingAssignment)"

    @Scheduled(fixedRate=1000l)
    void execute() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            //println("${name}: execution plan not set or not active")
            return
        }
        int numberOfRunning = numberOfRunningProcesses()
        if (numberOfRunning >= MAX_RUNNING) {
            //println "${name}: ${numberOfRunning} processes already running"
            return
        }
        int nRuns = Run.countByCompleteAndBlacklisted(false, false)
        if (nRuns > 1) {
            //println "${name}: runs processing running"
            return
        }
        SeqTrack seqTrack = newSeqTrack()
        if (seqTrack == null) {
            return
        }
        createProcess(new ProcessParameter(value: seqTrack.id.toString(), className: seqTrack.class.name))
        println "${name}: job started for seqTrack ${seqTrack}"
    }

   /**
     * returns number of running processes for this execution plan
     * @return
     */
    private int numberOfRunningProcesses() {
        return Process.countByFinishedAndJobExecutionPlan(false, getExecutionPlan())
    }

    private SeqTrack newSeqTrack() {
        return SeqTrack.find(hql)
    }
}