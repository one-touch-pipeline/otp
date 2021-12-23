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
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*

@Rollback
@Integration
class OverallQualityAssessmentMergedServiceIntegrationSpec extends Specification {

    OverallQualityAssessmentMergedService overallQualityAssessmentMergedService

    void "test findChromosomeLengthForQualityAssessmentMerged case one element should return one element"() {
        given:
        OverallQualityAssessmentMerged overallQualityAssessmentMerged = DomainFactory.createOverallQualityAssessmentMerged()
        DomainFactory.createChromosomeQualityAssessmentMerged(
                qualityAssessmentMergedPass: overallQualityAssessmentMerged.qualityAssessmentMergedPass,
                chromosomeName: Chromosomes.CHR_X.name(),
        )
        DomainFactory.createReferenceGenomeEntry(
                name: Chromosomes.CHR_X.name(),
                alias: Chromosomes.CHR_X.alias,
                lengthWithoutN: 100,
                classification: ReferenceGenomeEntry.Classification.CHROMOSOME,
                referenceGenome: overallQualityAssessmentMerged.referenceGenome
        )

        when:
        List<ReferenceGenomeEntry> result = overallQualityAssessmentMergedService.findChromosomeLengthForQualityAssessmentMerged([Chromosomes.CHR_X.alias, Chromosomes.CHR_Y.name()], [overallQualityAssessmentMerged])

        then:
        1 == result.size()
        Chromosomes.CHR_X.alias == result[0].alias
        100 == result[0].lengthWithoutN
        overallQualityAssessmentMerged.referenceGenome.id == result[0].referenceGenomeId
    }

    void "test findChromosomeLengthForQualityAssessmentMerged case many objects should return correct subset"() {
        given:
        List<Chromosomes> chromosomesListUsed = [Chromosomes.CHR_X, Chromosomes.CHR_Y]
        List<Chromosomes> chromosomesListUnused = [Chromosomes.CHR_1, Chromosomes.CHR_2]

        Set<ReferenceGenomeEntry> chromosomeLengthForReferenceGenomes = []
        List<OverallQualityAssessmentMerged> overallQualityAssessmentMergedList = []

        ReferenceGenome referenceGenome1 = createReferenceGenomeWithEntries(chromosomesListUsed, chromosomesListUnused, 1000, chromosomeLengthForReferenceGenomes)
        ReferenceGenome referenceGenome2 = createReferenceGenomeWithEntries(chromosomesListUsed, chromosomesListUnused, 2000, chromosomeLengthForReferenceGenomes)
        ReferenceGenome referenceGenome3 = createReferenceGenomeWithEntries(chromosomesListUsed, chromosomesListUnused, 3000, [])

        createOverallQualityAssessmentMerged(referenceGenome1)
        createOverallQualityAssessmentMerged(referenceGenome2)
        createOverallQualityAssessmentMerged(referenceGenome3)
        createOverallQualityAssessmentMerged(referenceGenome3)

        overallQualityAssessmentMergedList << createOverallQualityAssessmentMerged(referenceGenome1)
        overallQualityAssessmentMergedList << createOverallQualityAssessmentMerged(referenceGenome2)
        overallQualityAssessmentMergedList << createOverallQualityAssessmentMerged(referenceGenome2)

        when:
        Set<ReferenceGenomeEntry> result = overallQualityAssessmentMergedService.findChromosomeLengthForQualityAssessmentMerged(chromosomesListUsed*.alias, overallQualityAssessmentMergedList) as Set

        then:
        chromosomeLengthForReferenceGenomes == result
    }

    private OverallQualityAssessmentMerged createOverallQualityAssessmentMerged(ReferenceGenome referenceGenome) {
        DomainFactory.createOverallQualityAssessmentMerged([
                qualityAssessmentMergedPass: DomainFactory.createQualityAssessmentMergedPass([
                        abstractMergedBamFile: DomainFactory.createProcessedMergedBamFile(
                                DomainFactory.createMergingWorkPackage([
                                        referenceGenome: referenceGenome,
                                        pipeline: DomainFactory.createDefaultOtpPipeline(),
                                ])
                        )
                ])
        ])
    }

    private ReferenceGenome createReferenceGenomeWithEntries(List<Chromosomes> usedChromosomes, List<Chromosomes> unusedChromosomes, Long lengthWithoutNOffset, Collection<ReferenceGenomeEntry> chromosomeLengthForReferenceGenomes) {
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        [usedChromosomes, unusedChromosomes].flatten().eachWithIndex { Chromosomes chromosomes, int i ->
            ReferenceGenomeEntry referenceGenomeEntry = DomainFactory.createReferenceGenomeEntry(
                    name: chromosomes.name(),
                    alias: chromosomes.alias,
                    lengthWithoutN: lengthWithoutNOffset + 100 * i,
                    classification: ReferenceGenomeEntry.Classification.CHROMOSOME,
                    referenceGenome: referenceGenome
            )
            if (usedChromosomes.contains(chromosomes)) {
                chromosomeLengthForReferenceGenomes << referenceGenomeEntry
            }
        }
        return referenceGenome
    }
}
