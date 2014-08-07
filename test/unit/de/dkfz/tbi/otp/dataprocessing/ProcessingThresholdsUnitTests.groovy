package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(ProcessingThresholds)
@Mock([Project, SeqType, SampleType])
class ProcessingThresholdsUnitTests {

    Double coverage = 30.00

    Integer numberOfLanes = 3

    void testSaveNoProject() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.project = new Project()
        assertTrue(processingThresholds.validate())
    }

    void testSaveNoSeqType() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: new Project(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.seqType = new SeqType()
        assertTrue(processingThresholds.validate())

    }

    void testSaveNoSampleType() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        seqType: new SeqType(),
                        project: new Project(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.sampleType = new SampleType()
        assertTrue(processingThresholds.validate())
    }

    void testSaveNoCoverageAndNumberOfLanes() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: new Project(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.coverage = coverage
        processingThresholds.numberOfLanes = numberOfLanes
        assertTrue(processingThresholds.validate())
    }

    void testSaveNoCoverage() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: new Project(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        )
        assertTrue(processingThresholds.validate())
    }


    void testSaveCoverageBelowZero() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: new Project(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: -30.00,
                        )
        assertFalse(processingThresholds.validate())
    }

    void testSaveNoNumberOfLanes() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: new Project(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        numberOfLanes: numberOfLanes,
                        )
        assertTrue(processingThresholds.validate())
    }

    void testSaveNumberOfLanesBelowZero() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: new Project(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        numberOfLanes: -3,
                        )
        assertFalse(processingThresholds.validate())
    }

    void testSaveAllCorrect() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: new Project(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertTrue(processingThresholds.validate())
    }
}
