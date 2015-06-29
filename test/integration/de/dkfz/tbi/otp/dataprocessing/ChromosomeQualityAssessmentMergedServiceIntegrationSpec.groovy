package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.TestFor
import grails.test.spock.IntegrationSpec
import org.junit.Before
import org.junit.Test

class ChromosomeQualityAssessmentMergedServiceIntegrationSpec extends IntegrationSpec {

    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService

    List<String> chromosomes

    List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses

    List<ChromosomeQualityAssessmentMerged> chromosomeQualityAssessmentMergedList



    void setup() {
        chromosomeQualityAssessmentMergedService = new ChromosomeQualityAssessmentMergedService()

        chromosomes = [
            Chromosomes.CHR_X.getAlias(),
            Chromosomes.CHR_Y.getAlias()
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

        expect:
        expected as Set == result as Set
    }


    void testQualityAssessmentMergedForSpecificChromosomes_emptyChromosomesList() {
        List<ChromosomeQualityAssessmentMerged> expected = []
        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes([], qualityAssessmentMergedPasses)

        expect:
        expected == result
    }


    void testQualityAssessmentMergedForSpecificChromosomes_emptyQualityAssessmentMergedPassesList() {
        List<ChromosomeQualityAssessmentMerged> expected = []
        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, [])

        expect:
        expected == result
    }


    void testQualityAssessmentMergedForSpecificChromosomes_ChromosomesListIsNull() {
        when:
        chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(null, qualityAssessmentMergedPasses)

        then:
        thrown(IllegalArgumentException)
    }


    void testQualityAssessmentMergedForSpecificChromosomes_QualityAssessmentMergedPassesListIsNull() {
        when:
        chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, null)

        then:
        thrown(IllegalArgumentException)
    }


    void testQualityAssessmentMergedForSpecificChromosomes_NoQaAvailable() {
        chromosomeQualityAssessmentMergedList*.delete([flush: true])

        List<ChromosomeQualityAssessmentMerged> expected = []
        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, qualityAssessmentMergedPasses)

        expect:
        expected == result
    }
}
