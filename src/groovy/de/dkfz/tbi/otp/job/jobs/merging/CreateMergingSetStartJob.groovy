package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.*

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired


class CreateMergingSetStartJob extends AbstractStartJobImpl {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    ProcessedBamFileService processedBamFileService

    final int MAX_RUNNING = 1 // must not be more then one

    @Scheduled(fixedRate=60000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        ProcessedBamFile bamFile2Merge = processedBamFileService.getFreeProcessedBamFile()
        if (bamFile2Merge) {
            processedBamFileService.blockForMerging(bamFile2Merge)
            createProcess(new ProcessParameter(value: bamFile2Merge.id.toString(), className: ProcessedBamFile.class.name))
            println "CreateMergingSetStartJob started for: ${bamFile2Merge.toString()}"
        }
    }

    // TODO: think to make it in superclass, to be DRY
    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "CreateMergingSetWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }
}