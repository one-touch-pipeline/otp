package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(ProcessingThresholds)
@Mock([Project, ProjectCategory, SeqType, SampleType])
class ProcessingThresholdsUnitTests {

    Double coverage = 30.00

    Integer numberOfLanes = 3

    @Test
    void testSaveNoProject() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.project = DomainFactory.createProject()
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNoSeqType() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.seqType = new SeqType()
        assertTrue(processingThresholds.validate())

    }

    @Test
    void testSaveNoSampleType() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        seqType: new SeqType(),
                        project: DomainFactory.createProject(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.sampleType = new SampleType()
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNoCoverageAndNumberOfLanes() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        )
        assertFalse(processingThresholds.validate())

        processingThresholds.coverage = coverage
        processingThresholds.numberOfLanes = numberOfLanes
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNoCoverage() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        )
        assertTrue(processingThresholds.validate())
    }


    @Test
    void testSaveCoverageBelowZero() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: -30.00,
                        )
        assertFalse(processingThresholds.validate())
    }

    @Test
    void testSaveNoNumberOfLanes() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        numberOfLanes: numberOfLanes,
                        )
        assertTrue(processingThresholds.validate())
    }

    @Test
    void testSaveNumberOfLanesBelowZero() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        numberOfLanes: -3,
                        )
        assertFalse(processingThresholds.validate())
    }

    @Test
    void testSaveAllCorrect() {
        ProcessingThresholds processingThresholds = new ProcessingThresholds(
                        project: DomainFactory.createProject(),
                        seqType: new SeqType(),
                        sampleType: new SampleType(),
                        coverage: coverage,
                        numberOfLanes: numberOfLanes,
                        )
        assertTrue(processingThresholds.validate())
    }
}
