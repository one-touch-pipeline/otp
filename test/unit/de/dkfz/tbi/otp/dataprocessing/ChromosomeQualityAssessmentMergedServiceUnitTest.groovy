package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*


@TestFor(ChromosomeQualityAssessmentMergedService)
@Build([
    ChromosomeQualityAssessmentMerged,
    ProcessedMergedBamFile,
])
class ChromosomeQualityAssessmentMergedServiceUnitTest {

    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService

    List<String> chromosomes

    List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses

    List<ChromosomeQualityAssessmentMerged> chromosomeQualityAssessmentMergedList



    void setUp() {
        chromosomeQualityAssessmentMergedService = new ChromosomeQualityAssessmentMergedService()

        chromosomes = [
            Chromosomes.CHR_X,
            Chromosomes.CHR_Y
        ]

        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = QualityAssessmentMergedPass.build()
        QualityAssessmentMergedPass qualityAssessmentMergedPass2 = QualityAssessmentMergedPass.build()
        qualityAssessmentMergedPasses = [
            qualityAssessmentMergedPass1,
            qualityAssessmentMergedPass2
        ]

        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedX1 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass1, chromosomeName: Chromosomes.CHR_X.alias)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedY1 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass1, chromosomeName: Chromosomes.CHR_Y.alias)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedX2 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass2, chromosomeName: Chromosomes.CHR_X.alias)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedY2 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass2, chromosomeName: Chromosomes.CHR_Y.alias)
        chromosomeQualityAssessmentMergedList = [
            chromosomeQualityAssessmentMergedX1,
            chromosomeQualityAssessmentMergedY1,
            chromosomeQualityAssessmentMergedX2,
            chromosomeQualityAssessmentMergedY2
        ]
    }



    void testQualityAssessmentMergedForSpecificChromosomes() {
        List<ChromosomeQualityAssessmentMerged> expected = chromosomeQualityAssessmentMergedList

        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, qualityAssessmentMergedPasses)
        assert expected == result
    }


    void testQualityAssessmentMergedForSpecificChromosomes_emptyChromosomesList() {
        List<ChromosomeQualityAssessmentMerged> expected = []

        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes([], qualityAssessmentMergedPasses)
        assert expected == result
    }


    void testQualityAssessmentMergedForSpecificChromosomes_emptyQualityAssessmentMergedPassesList() {
        List<ChromosomeQualityAssessmentMerged> expected = []

        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, [])
        assert expected == result
    }


    void testQualityAssessmentMergedForSpecificChromosomes_ChromosomesListIsNull() {
        List<ChromosomeQualityAssessmentMerged> expected = []

        shouldFail(IllegalArgumentException) {
            chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(null, qualityAssessmentMergedPasses)
        }
    }


    void testQualityAssessmentMergedForSpecificChromosomes_QualityAssessmentMergedPassesListIsNull() {
        List<ChromosomeQualityAssessmentMerged> expected = []

        shouldFail(IllegalArgumentException) {
            chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, null)
        }
    }


    void testQualityAssessmentMergedForSpecificChromosomes_NoQaAvailable() {
        chromosomeQualityAssessmentMergedList*.delete()
        List<ChromosomeQualityAssessmentMerged> expected = []

        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, qualityAssessmentMergedPasses)
        assert expected == result
    }

}
