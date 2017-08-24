package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.junit.*

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
        assert ProcessingPriority.NORMAL_PRIORITY == domainObject.processingPriority

        domainObject.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert ProcessingPriority.FAST_TRACK_PRIORITY == domainObject.processingPriority
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
        datafile.project.processingPriority = ProcessingPriority.NORMAL_PRIORITY
        assert ProcessingPriority.NORMAL_PRIORITY == run.processingPriority

        DataFile datafileFasttrack = DomainFactory.createDataFile(run: run)
        datafileFasttrack.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert ProcessingPriority.FAST_TRACK_PRIORITY == run.processingPriority
    }
}
