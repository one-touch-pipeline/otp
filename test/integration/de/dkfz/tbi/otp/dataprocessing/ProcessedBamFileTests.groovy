package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.dataprocessing.AbstractBamFileServiceTests.*

import de.dkfz.tbi.otp.ngsdata.TestData
import org.junit.*

class ProcessedBamFileTests {

    TestData data = new TestData()

    @Before
    void setUp() {
        data.createObjects()
    }

    @Test
    void test_getOverallQualityAssessment_WhenOnePassExists_ShouldReturnThis() {

        final Long ARBITRARY_IDENTIFIER = 42

        def processedBamFile = createProcessedBamFile()
        def oqa = createOverallQualityAssessment(processedBamFile, ARBITRARY_IDENTIFIER)

        assert processedBamFile.overallQualityAssessment == oqa
    }

    @Test
    void test_getOverallQualityAssessment_WhenMultiplePassesExists_ShouldReturnLatest() {

        final Long IDENTIFIER_FORMER = 100
        final Long IDENTIFIER_LATER = 200

        assert IDENTIFIER_FORMER < IDENTIFIER_LATER

        def processedBamFile = createProcessedBamFile()
        def oqaFormer = createOverallQualityAssessment(processedBamFile, IDENTIFIER_FORMER)
        def oqaLater = createOverallQualityAssessment(processedBamFile, IDENTIFIER_LATER)

        assert processedBamFile.overallQualityAssessment == oqaLater
        assert processedBamFile.overallQualityAssessment != oqaFormer
    }

    private ProcessedBamFile createProcessedBamFile() {

        AlignmentPass alignmentPass = data.createAlignmentPass()
        alignmentPass.save([flush: true])

        ProcessedBamFile processedBamFile = new ProcessedBamFile([
                type                   : AbstractBamFile.BamType.SORTED,
                withdrawn              : false,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
                alignmentPass          : alignmentPass,
        ])
        assert processedBamFile.save([flush: true])

        return processedBamFile
    }

    private static OverallQualityAssessment createOverallQualityAssessment(ProcessedBamFile processedBamFile, Long identifier) {

        assert processedBamFile: 'processedBamFile must not be null'

        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass([
                processedBamFile: processedBamFile,
                identifier      : QualityAssessmentPass.nextIdentifier(processedBamFile)
        ])
        assert qualityAssessmentPass.save([flush: true])

        OverallQualityAssessment overallQualityAssessment = new OverallQualityAssessment(
                ARBITRARY_QA_VALUES + [
                id                   : identifier,
                qualityAssessmentPass: qualityAssessmentPass,
        ])
        assert overallQualityAssessment.save([flush: true])

        return overallQualityAssessment
    }
}
