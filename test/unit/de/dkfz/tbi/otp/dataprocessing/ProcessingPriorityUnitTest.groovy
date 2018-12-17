package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.Mock
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*

@Build([
        DataFile,
        ProcessedMergedBamFile,
        QualityAssessmentPass,
])
@Mock([
        QualityAssessmentMergedPass,
        MergingCriteria,
])
class ProcessingPriorityUnitTest {

    private void assertPriority(def domainObject) {
        assert ProcessingPriority.NORMAL.priority == domainObject.processingPriority

        domainObject.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        assert ProcessingPriority.FAST_TRACK.priority == domainObject.processingPriority
    }


    @Test
    void testGetProcessingPriorityForAlignmentPass() {
        assertPriority(DomainFactory.createAlignmentPass())
    }

    @Test
    void testGetProcessingPriorityForSeqtrack() {
        assertPriority(DomainFactory.createSeqTrack())
    }

    @Test
    void testGetProcessingPriorityForMergingPass() {
        assertPriority(MergingPass.build())
    }

    @Test
    void testGetProcessingPriorityForQualityAssessmentPass() {
        assertPriority(QualityAssessmentPass.build())
    }

    @Test
    void testGetProcessingPriorityForQualityAssessmentMergedPass() {
        assertPriority(new QualityAssessmentMergedPass(
                abstractMergedBamFile: DomainFactory.createProcessedMergedBamFile(),
        ))
    }

    @Test
    void testGetProcessingPriorityForProcessedBamFile() {
        assertPriority(ProcessedBamFile.build())
    }

    @Test
    void testGetProcessingPriorityForProcessedMergedBamFile() {
        assertPriority(DomainFactory.createProcessedMergedBamFile())
    }

    @Test
    void testGetProcessingPriority() {
        Run run = Run.build()
        DataFile datafile = DomainFactory.createDataFile(run: run)
        datafile.project.processingPriority = ProcessingPriority.NORMAL.priority
        assert ProcessingPriority.NORMAL.priority == run.processingPriority

        DataFile datafileFasttrack = DomainFactory.createDataFile(run: run)
        datafileFasttrack.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        assert ProcessingPriority.FAST_TRACK.priority == run.processingPriority
    }
}
