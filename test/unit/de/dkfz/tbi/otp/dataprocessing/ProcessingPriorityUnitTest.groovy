package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.TestData
import grails.buildtestdata.mixin.Build
import org.junit.Test

@Build([
    QualityAssessmentPass,
    QualityAssessmentMergedPass,
    DataFile,
    ])
class ProcessingPriorityUnitTest {

    private void assertPriority(def domainObject) {
        assert ProcessingPriority.NORMAL_PRIORITY == domainObject.processingPriority

        domainObject.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert ProcessingPriority.FAST_TRACK_PRIORITY == domainObject.processingPriority
    }


    @Test
    void testGetProcessingPriorityForAlignmentPass() {
        assertPriority(TestData.createAndSaveAlignmentPass())
    }

    @Test
    void testGetProcessingPriorityForSeqtrack() {
        assertPriority(SeqTrack.build())
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
        assertPriority(QualityAssessmentMergedPass.build())
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
        Project project = Project.build(processingPriority: ProcessingPriority.NORMAL_PRIORITY)
        Run run = Run.build()
        DataFile datafile = DataFile.build(project: project, run: run)

        assert ProcessingPriority.NORMAL_PRIORITY == run.processingPriority

        Project projectFasttrack = Project.build(processingPriority: ProcessingPriority.FAST_TRACK_PRIORITY)
        DataFile datafileFasttrack = DataFile.build(project: projectFasttrack, run: run)

        assert ProcessingPriority.FAST_TRACK_PRIORITY == run.processingPriority
    }
}
