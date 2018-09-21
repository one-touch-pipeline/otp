package de.dkfz.tbi.otp.job.jobs.importExternallyMergedBam

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

@Component("importExternallyMergedBamStartJob")
@Scope("singleton")
class ImportExternallyMergedBamStartJob extends AbstractStartJobImpl{

    @Scheduled(fixedDelay=60000l)
    @Override
    void execute() {
        doWithPersistenceInterceptor {
            short minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
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
