package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.SeqTrack

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



    void testGetProcessingPriorityForAlignmentPass() {
        assertPriority(AlignmentPass.build())
    }

    void testGetProcessingPriorityForSeqtrack() {
        assertPriority(SeqTrack.build())
    }

    void testGetProcessingPriorityForMergingPass() {
        assertPriority(MergingPass.build())
    }

    void testGetProcessingPriorityForQualityAssessmentPass() {
        assertPriority(QualityAssessmentPass.build())
    }

    void testGetProcessingPriorityForQualityAssessmentMergedPass() {
        assertPriority(QualityAssessmentMergedPass.build())
    }

    void testGetProcessingPriorityForProcessedBamFile() {
        assertPriority(ProcessedBamFile.build())
    }

    void testGetProcessingPriorityForProcessedMergedBamFile() {
        assertPriority(ProcessedMergedBamFile.build())
    }


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
