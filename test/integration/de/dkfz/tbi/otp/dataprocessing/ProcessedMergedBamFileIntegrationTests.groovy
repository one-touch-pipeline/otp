package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.dataprocessing.AbstractBamFileServiceTests.*

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Test
import static org.junit.Assert.*

class ProcessedMergedBamFileIntegrationTests {

    @Test
    void testToString() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()
                         //PMBF 2: pass: 0 (latest) set: 0 (latest) <br>sample: mockPid sampleTypeName_3 seqType: null seqTypelibraryLayout_8 <br>project: projectName_1
        String expected = /PMBF ${bamFile.id}: pass: 0 \(latest\) set: 0 \(latest\) <br>sample: mockPid sampleTypeName_\d+ seqType: null seqTypelibraryLayout_\d+ <br>project: projectName_\d+/
        String actual = bamFile.toString()
        assertTrue("Expected string matching '" + expected + "'. Got: " + actual, actual.matches(expected))
    }

    @Test
    void test_getOverallQualityAssessment_WhenOnePassExists_ShouldReturnThis() {

        final Long ARBITRARY_IDENTIFIER = 42

        def processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        def oqa = createOverallQualityAssessment(processedMergedBamFile, ARBITRARY_IDENTIFIER)

        assert processedMergedBamFile.overallQualityAssessment == oqa
    }

    @Test
    void test_getOverallQualityAssessment_WhenMultiplePassesExists_ShouldReturnLatest() {

        final Long IDENTIFIER_FORMER = 100
        final Long IDENTIFIER_LATER = 200

        assert IDENTIFIER_FORMER < IDENTIFIER_LATER

        def processedMergedBamFile = DomainFactory.createProcessedMergedBamFile()
        def oqaFormer = createOverallQualityAssessment(processedMergedBamFile, IDENTIFIER_FORMER)
        def oqaLater = createOverallQualityAssessment(processedMergedBamFile, IDENTIFIER_LATER)

        assert processedMergedBamFile.overallQualityAssessment == oqaLater
        assert processedMergedBamFile.overallQualityAssessment != oqaFormer
    }

    private static OverallQualityAssessmentMerged createOverallQualityAssessment(ProcessedMergedBamFile processedMergedBamFile, Long identifier) {

        assert processedMergedBamFile: 'processedMergedBamFile must not be null'

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass([
                abstractMergedBamFile: processedMergedBamFile,
                identifier: QualityAssessmentMergedPass.nextIdentifier(processedMergedBamFile),
        ])
        assert qualityAssessmentMergedPass.save([flush: true])

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged(
                ARBITRARY_QA_VALUES + [
                id                   : identifier,
                qualityAssessmentMergedPass: qualityAssessmentMergedPass,
        ])
        assert overallQualityAssessmentMerged.save([flush: true])

        return overallQualityAssessmentMerged
    }
}
