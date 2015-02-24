package de.dkfz.tbi.otp.job.jobs.dataTransfer

import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.ngsdata.*


class CopyFilesJobTests extends GroovyTestCase {

    CopyFilesJob job = new CopyFilesJob()
    TestData testData

    @Test
    void testProcessedMergedBamFilesForRun() {

        shouldFail(IllegalArgumentException.class, { job.processedMergedBamFilesForRun(null)})

        testData = new TestData()
        testData.createObjects()
        Run run1 = testData.run

        assertEquals(0, job.processedMergedBamFilesForRun(run1).size())

        ProcessedMergedBamFile pmbf1 = createMergedBamFileForRun(run1, testData.sample)

        assertEquals(1, job.processedMergedBamFilesForRun(run1).size())

        final Run run2 = testData.createRun("run2")
        assertNotNull(run2.save(flush: true))
        assertEquals(0, job.processedMergedBamFilesForRun(run2).size())

        ProcessedMergedBamFile pmbf2 = createMergedBamFileForRun(run1, testData.sample)
        assertEquals(2, job.processedMergedBamFilesForRun(run1).size())
    }

    private ProcessedMergedBamFile createMergedBamFileForRun(Run run, Sample sample) {
        SeqTrack seqTrack = testData.createSeqTrack([run: run, sample: sample])
        assert seqTrack.save()

        AlignmentPass alignmentPass = testData.createAlignmentPass([seqTrack: seqTrack])
        assert alignmentPass.save()

        ProcessedBamFile processedBamFile = testData.createProcessedBamFile([alignmentPass: alignmentPass])
        assert processedBamFile.save()

        MergingSet mergingSet = testData.createMergingSet([
            identifier: MergingSet.nextIdentifier(alignmentPass.workPackage),
            mergingWorkPackage: alignmentPass.workPackage,
            status: MergingSet.State.NEEDS_PROCESSING
        ])
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        assertNotNull(mergingPass.save([flush: true]))

        ProcessedMergedBamFile processedMergedBamFile = testData.createProcessedMergedBamFile([
            mergingPass: mergingPass,
            fileOperationStatus: FileOperationStatus.PROCESSED
            ])
        assert processedMergedBamFile.save()
    }
}
