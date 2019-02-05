package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

@Component("importExternallyMergedBamStartJob")
@Scope("singleton")
class ImportExternallyMergedBamStartJob extends AbstractStartJobImpl {

    @Scheduled(fixedDelay = 60000L)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            ProcessingPriority minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority.priority > ProcessingPriority.MAXIMUM.priority) {
                return
            }

            ImportProcess.withTransaction {
                ImportProcess importProcess = ImportProcess.findByState(ImportProcess.State.NOT_STARTED)
                if (importProcess) {
                    importProcess.updateState(ImportProcess.State.STARTED)
                    assert importProcess.save(flush: true)
                    createProcess(importProcess)
                    log.debug "Creating process for import ${importProcess}"
                }
            }
        }
    }
}
