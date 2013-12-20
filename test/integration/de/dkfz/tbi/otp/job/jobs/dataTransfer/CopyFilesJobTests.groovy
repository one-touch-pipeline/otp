package de.dkfz.tbi.otp.job.jobs.dataTransfer

import org.junit.*

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileServiceTests
import de.dkfz.tbi.otp.ngsdata.Run

class CopyFilesJobTests extends GroovyTestCase {

    CopyFilesJob job = new CopyFilesJob()

    @Test
    void testProcessedMergedBamFilesForRun() {

        shouldFail(IllegalArgumentException.class, { job.processedMergedBamFilesForRun(null)})

        final AbstractBamFileServiceTests testData = new AbstractBamFileServiceTests()

        testData.setUp()
        assertEquals(0, job.processedMergedBamFilesForRun(testData.run).size())

        testData.createProcessedMergedBamFile()
        assertEquals(1, job.processedMergedBamFilesForRun(testData.run).size())

        final Run run2 = testData.createRun("run2")
        assertNotNull(run2.save(flush: true))
        assertEquals(0, job.processedMergedBamFilesForRun(run2).size())

        testData.createProcessedMergedBamFile()
        assertEquals(2, job.processedMergedBamFilesForRun(testData.run).size())
    }
}
