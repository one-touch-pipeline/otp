package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import spock.lang.Specification



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
