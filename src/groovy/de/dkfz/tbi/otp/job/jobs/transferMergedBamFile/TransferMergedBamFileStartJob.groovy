package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
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

        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }

        ProcessedMergedBamFile.withTransaction {
            ProcessedMergedBamFile file = processedMergedBamFileService.mergedBamFileWithFinishedQA(minPriority)
            if (file) {
                log.debug 'Starting to transfer merged BAM file ' + file
                file.updateFileOperationStatus(AbstractMergedBamFile.FileOperationStatus.INPROGRESS)
                assert file.save(flush: true)
                createProcess(file)
            }
        }
    }


    @Override
    protected String getJobExecutionPlanName() {
        return "transferMergedBamFileWorkflow"
    }
}
