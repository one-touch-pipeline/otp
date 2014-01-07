package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*

@Component("transferMergedBamFileStartJob")
@Scope("singleton")
class TransferMergedBamFileStartJob extends AbstractStartJobImpl {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay = 10000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        ProcessedMergedBamFile file = processedMergedBamFileService.mergedBamFileWithFinishedQA()
        if (file) {
            log.debug 'Starting to transfer merged BAM file ' + file
            processedMergedBamFileService.updateFileOperationStatus(file, AbstractBamFile.FileOperationStatus.INPROGRESS)
            createProcess(new ProcessParameter(value: file.id.toString(), className: file.class.name))
        }
    }

    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "TransferMergedBamFileWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }
}
