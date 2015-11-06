package de.dkfz.tbi.otp.job.jobs.indexSingleBam

import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Parameter
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("indexSingleBamStartJob")
@Scope("singleton")
class IndexSingleBamStartJob extends AbstractStartJobImpl {

    final int MAX_RUNNING = 1

    @Scheduled(fixedDelay=10000l)
    void execute() {
        if (!getExecutionPlan() || !getExecutionPlan().enabled) {
            return
        }
        int numberOfRunning = numberOfRunningProcesses()
        if (numberOfRunning >= MAX_RUNNING) {
            println "Number of running: ${numberOfRunning}"
            return
        }
        int n = 0;
        List<SeqScan> scans = SeqScan.findAllByNLanes(1)
        for(SeqScan scan in scans) {
            if (numberOfRunning >= MAX_RUNNING) {
                break
            }
            if (!isAligned(scan)) {
                continue
            }
            if (processed(scan)) {
                continue
            }
            // new run to be processed
            createProcess(scan)
            println scan.toString()
            numberOfRunning++
            n++
        }
        if (n>0) {
            println "IndexSingleBamWorkflow: ${n} jobs started"
        }
    }

    private boolean processed(SeqScan scan) {
        List<ProcessParameter> processParameters =
            ProcessParameter.findAllByValue(scan.id.toString())
        for(ProcessParameter parameter in processParameters) {
            if (parameter.process.jobExecutionPlan.id == getExecutionPlan().id) {
                return true
            }
        }
        return false
    }

    private boolean isAligned(SeqScan scan) {
        SeqTrack seqTrack = MergingAssignment.findBySeqScan(scan).seqTrack
        AlignmentLog alignment = AlignmentLog.findBySeqTrack(seqTrack)
        return (boolean) alignment
    }

    /**
     * returns number of running processes for this execution plan
     * @return
     */
    private int numberOfRunningProcesses() {
        return Process.countByFinishedAndJobExecutionPlan(false, getExecutionPlan())
    }

    @Override
    protected String getJobExecutionPlanName() {
        return null
    }
}
