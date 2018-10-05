package de.dkfz.tbi.otp.job.jobs.fileSystemConsistency

import de.dkfz.tbi.otp.fileSystemConsistency.*
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus.Status
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

/**
 * Job that verifies the consistency status of DataFiles and saves only the not consistent ones in the database.
 */
@Component
@Scope("prototype")
@UseJobLog
class CheckDataFileStatusJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ConsistencyService consistencyService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Override
    void execute() throws Exception {
        ConsistencyCheck consistencyCheck = ConsistencyCheck.get(Long.parseLong(getProcessParameterValue()))
        List<DataFile> dataFiles = dataFilesWithConsistentStatus()
        long numberOfInconsistencies = 0
        dataFiles.each { DataFile dataFile ->
            Status status = consistencyService.checkStatus(dataFile)
            if (status != Status.CONSISTENT) {
                ConsistencyStatus consistencyStatus = new ConsistencyStatus(
                    consistencyCheck: consistencyCheck,
                    dataFile: dataFile,
                    status: status
                )
                consistencyStatus.save(flush: true)
                numberOfInconsistencies++
            }
        }
        log.info("A total of " + numberOfInconsistencies + " inconsistencies were found.")
        succeed()
    }

    /**
     * Returns a list of DataFiles that are not listed at @see{ConsistencyStatus} for inconsistencies
     * @return A list of DataFiles without inconsistencies
     */
    private List<DataFile> dataFilesWithConsistentStatus() {
        List<Long> dataFileIdsWithInconsistencies = ConsistencyStatus.findAllByResolvedDateIsNull().collect {
            it.dataFileId
        }
        // if the list is empty, the query throws a nullPointerException
        if (dataFileIdsWithInconsistencies.size() > 0) {
            return DataFile.findAllByIdNotInList(dataFileIdsWithInconsistencies)
        }
        return DataFile.list()
    }
}
