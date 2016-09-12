package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Test

class AbstractMergedBamFileServiceTests {

    AbstractMergedBamFileService abstractMergedBamFileService

    final String MERGED_BAM_FILES_PATH = "merged-alignment"

    @Test
    void testDestinationDirectory_ProcessedMergedBamFile() {
        ProcessedMergedBamFile mergedBamFile = DomainFactory.createProcessedMergedBamFile()
        Realm realm = Realm.build(name: mergedBamFile.project.realmName)

        String destinationExp = expectedMergedAlignmentPath(mergedBamFile, realm)
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        assert destinationExp == destinationAct
    }

    @Test
    void testDestinationDirectory_RoddyBamFile() {
        RoddyBamFile mergedBamFile = DomainFactory.createRoddyBamFile()
        Realm realm = Realm.build(name: mergedBamFile.project.realmName)

        String destinationExp = expectedMergedAlignmentPath(mergedBamFile, realm)
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        assert destinationExp == destinationAct
    }

    private String expectedMergedAlignmentPath(AbstractMergedBamFile mergedBamFile, Realm realm) {
        String pidPath = "${realm.rootPath}/${mergedBamFile.project.dirName}/sequencing/${mergedBamFile.seqType.dirName}/view-by-pid/${mergedBamFile.individual.pid}"
        return "${pidPath}/${mergedBamFile.sampleType.dirName}/${mergedBamFile.seqType.libraryLayoutDirName}/${MERGED_BAM_FILES_PATH}/"
    }


    @Test
    void testSetSamplePairStatusToNeedProcessing_InputNull_ShouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "The input bam file must not be null") {
            abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(null)
        }
    }


    @Test
    void testSetSamplePairStatusToNeedProcessing_SamplePairInStateNeedsProcessing_NothingTodo() {
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NEEDS_PROCESSING)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair.mergingWorkPackage2)
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)
        assert samplePair.processingStatus == ProcessingStatus.NEEDS_PROCESSING
    }


    @Test
    void testSetSamplePairStatusToNeedProcessing_SamplePairInStateNoProcessingNeeded_ShallSetPairToNeedsProcessing() {
        SamplePair samplePair = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NO_PROCESSING_NEEDED)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair.mergingWorkPackage2)
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)
        assert samplePair.processingStatus == ProcessingStatus.NEEDS_PROCESSING
    }

    @Test
    void testSetSamplePairStatusToNeedProcessing_OtherSamplePairInStateNoProcessingNeeded_NothingTodo() {
        SamplePair samplePair1 = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NO_PROCESSING_NEEDED)
        SamplePair samplePair2 = setSamplePairStatusToNeedProcessing_setup(ProcessingStatus.NEEDS_PROCESSING)
        AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(samplePair2.mergingWorkPackage2)
        abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(bamFile)
        assert samplePair1.processingStatus == ProcessingStatus.NO_PROCESSING_NEEDED
    }

    private SamplePair setSamplePairStatusToNeedProcessing_setup(ProcessingStatus processingStatus) {
        AbstractMergedBamFile bamFile1 = DomainFactory.createRoddyBamFile()
        SamplePair samplePair = DomainFactory.createDisease(bamFile1.workPackage)
        samplePair.processingStatus = processingStatus
        assert samplePair.save(flush: true)
        return samplePair
    }
}
