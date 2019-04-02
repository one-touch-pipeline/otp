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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class ChromosomeQualityAssessmentMergedServiceIntegrationSpec extends Specification {

    ChromosomeQualityAssessmentMergedService chromosomeQualityAssessmentMergedService

    List<String> chromosomes

    List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses

    List<ChromosomeQualityAssessmentMerged> chromosomeQualityAssessmentMergedList

    void setupData() {
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

        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedX1 = DomainFactory.createChromosomeQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentMergedPass1, chromosomeName: Chromosomes.CHR_X.alias, referenceLength: 0)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedY1 = DomainFactory.createChromosomeQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentMergedPass1, chromosomeName: Chromosomes.CHR_Y.alias, referenceLength: 0)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedX2 = DomainFactory.createChromosomeQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentMergedPass2, chromosomeName: Chromosomes.CHR_X.alias, referenceLength: 0)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessmentMergedY2 = DomainFactory.createChromosomeQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentMergedPass2, chromosomeName: Chromosomes.CHR_Y.alias, referenceLength: 0)
        chromosomeQualityAssessmentMergedList = [
                chromosomeQualityAssessmentMergedX1,
                chromosomeQualityAssessmentMergedY1,
                chromosomeQualityAssessmentMergedX2,
                chromosomeQualityAssessmentMergedY2,
        ]
    }

    void "test qualityAssessmentMergedForSpecificChromosomes"() {
        given:
        setupData()

        List<ChromosomeQualityAssessmentMerged> expected = chromosomeQualityAssessmentMergedList
        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, qualityAssessmentMergedPasses)

        expect:
        expected as Set == result as Set
    }

    void "test qualityAssessmentMergedForSpecificChromosomes with empty chromosome list"() {
        given:
        setupData()

        List<ChromosomeQualityAssessmentMerged> expected = []
        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes([], qualityAssessmentMergedPasses)

        expect:
        expected == result
    }

    void "test qualityAssessmentMergedForSpecificChromosomes with empty qualityAssessmentMergedPasses list"() {
        given:
        setupData()

        List<ChromosomeQualityAssessmentMerged> expected = []
        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, [])

        expect:
        expected == result
    }

    void "test qualityAssessmentMergedForSpecificChromosomes when chromosome list is null"() {
        given:
        setupData()

        when:
        chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(null, qualityAssessmentMergedPasses)

        then:
        thrown(IllegalArgumentException)
    }

    void "test qualityAssessmentMergedForSpecificChromosomes when qualityAssessmentMergedPasses list is null"() {
        given:
        setupData()

        when:
        chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, null)

        then:
        thrown(IllegalArgumentException)
    }

    void "test qualityAssessmentMergedForSpecificChromosomes when no qa available"() {
        given:
        setupData()

        chromosomeQualityAssessmentMergedList*.delete([flush: true])

        List<ChromosomeQualityAssessmentMerged> expected = []
        List<ChromosomeQualityAssessmentMerged> result = chromosomeQualityAssessmentMergedService.qualityAssessmentMergedForSpecificChromosomes(chromosomes, qualityAssessmentMergedPasses)

        expect:
        expected == result
    }
}
