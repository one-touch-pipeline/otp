package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFileService
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import static de.dkfz.tbi.TestCase.removeMetaClass

class BamFileValidationJobTests {

    @Autowired
    ProcessedBamFileService processedBamFileService

    BamFileValidationJob bamFileValidationJob = new BamFileValidationJob()

    @After
    void tearDown() {
        removeMetaClass(ProcessedBamFileService, processedBamFileService)
    }

    @Test
    void testValidateNumberOfReads_SameResult() {
        ProcessedBamFile processedBamFile = ProcessedBamFile.build()

        processedBamFileService.metaClass.getAlignmentReadLength = { ProcessedBamFile processedBamFile1 -> 1234 }
        processedBamFileService.metaClass.getFastQCReadLength = { ProcessedBamFile processedBamFile1 -> 1234 }

        bamFileValidationJob.validateNumberOfReads(processedBamFile)
    }

    @Test
    void testValidateNumberOfReads_DifferentResult() {
        ProcessedBamFile processedBamFile = ProcessedBamFile.build()

        processedBamFileService.metaClass.getAlignmentReadLength = { ProcessedBamFile processedBamFile1 -> 1234 }
        processedBamFileService.metaClass.getFastQCReadLength = { ProcessedBamFile processedBamFile1 -> 4321 }

        shouldFail(AssertionError, { bamFileValidationJob.validateNumberOfReads(processedBamFile) })
    }
}
