package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

@Component("transferMergedBamFileStartJob")
@Scope("singleton")
class TransferMergedBamFileStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Scheduled(fixedDelay = 10000L)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            ProcessingPriority minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority.priority > ProcessingPriority.MAXIMUM.priority) {
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
    }

    @Override
    Process restart(Process process) {
        assert process

        ProcessedMergedBamFile failedInstance = (ProcessedMergedBamFile)process.getProcessParameterObject()

        ProcessedMergedBamFile.withTransaction {
            failedInstance.updateFileOperationStatus(AbstractMergedBamFile.FileOperationStatus.INPROGRESS)
            return createProcess(failedInstance)
        }
    }
}
