/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.dataprocessing

import grails.test.spock.IntegrationSpec

import de.dkfz.tbi.otp.ngsdata.DomainFactory

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

        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = DomainFactory.createQualityAssessmentMergedPass()
        QualityAssessmentMergedPass qualityAssessmentMergedPass2 = DomainFactory.createQualityAssessmentMergedPass()
        qualityAssessmentMergedPasses = [
                qualityAssessmentMergedPass1,
                qualityAssessmentMergedPass2,
        ]

        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedX1 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass1, chromosomeName: Chromosomes.CHR_X.alias, referenceLength: 0)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedY1 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass1, chromosomeName: Chromosomes.CHR_Y.alias, referenceLength: 0)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedX2 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass2, chromosomeName: Chromosomes.CHR_X.alias, referenceLength: 0)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedY2 = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentMergedPass2, chromosomeName: Chromosomes.CHR_Y.alias, referenceLength: 0)
        chromosomeQualityAssessmentMergedList = [
                chromosomeQualityAssessmentMergedX1,
                chromosomeQualityAssessmentMergedY1,
                chromosomeQualityAssessmentMergedX2,
                chromosomeQualityAssessmentMergedY2,
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
