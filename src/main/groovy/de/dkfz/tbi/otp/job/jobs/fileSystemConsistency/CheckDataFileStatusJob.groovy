/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.jobs.fileSystemConsistency

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.fileSystemConsistency.*
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus.Status
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

/**
 * Job that verifies the consistency status of DataFiles and saves only the not consistent ones in the database.
 */
@Component
@Scope("prototype")
@Slf4j
class CheckDataFileStatusJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ConsistencyService consistencyService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Override
    void execute() throws Exception {
        ConsistencyCheck consistencyCheck = ConsistencyCheck.get(Long.parseLong(processParameterValue))
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
        List<Long> dataFileIdsWithInconsistencies = ConsistencyStatus.findAllByResolvedDateIsNull()*.dataFileId
        // if the list is empty, the query throws a nullPointerException
        if (dataFileIdsWithInconsistencies.size() > 0) {
            return DataFile.findAllByIdNotInList(dataFileIdsWithInconsistencies)
        }
        return DataFile.list()
    }
}
