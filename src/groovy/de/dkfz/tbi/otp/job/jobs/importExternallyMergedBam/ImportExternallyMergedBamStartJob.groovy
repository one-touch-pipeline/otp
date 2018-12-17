package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ImportProcess
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl

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
