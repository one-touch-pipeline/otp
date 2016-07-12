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
        Project project = Project.build(processingPriority: ProcessingPriority.NORMAL_PRIORITY)
        Run run = Run.build()
        DataFile datafile = DomainFactory.createDataFile(run: run)
        assert ProcessingPriority.NORMAL_PRIORITY == run.processingPriority

        datafile.project = project
        datafile.save(flush: true, failOnError: true)
        assert ProcessingPriority.NORMAL_PRIORITY == run.processingPriority

        Project projectFasttrack = Project.build(processingPriority: ProcessingPriority.FAST_TRACK_PRIORITY)
        DataFile datafileFasttrack = DomainFactory.createDataFile(project: projectFasttrack, run: run)

        assert ProcessingPriority.FAST_TRACK_PRIORITY == run.processingPriority
    }
}
