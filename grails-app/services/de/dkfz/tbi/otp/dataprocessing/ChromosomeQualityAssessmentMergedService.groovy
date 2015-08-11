package de.dkfz.tbi.otp.dataprocessing

import org.springframework.util.Assert
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry

class ChromosomeQualityAssessmentMergedService {

    ChromosomeQualityAssessmentMerged qualityAssessmentMergedForSpecificChromosome(ReferenceGenomeEntry referenceGenomeEntry, Long qualityAssessmentMergedPassId) {
        // FIXME: Silently returns null objects. If this is intentional, document it. Also no tests available.
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessment = ChromosomeQualityAssessmentMerged.createCriteria().get {
            eq("chromosomeName", referenceGenomeEntry.name)
            qualityAssessmentMergedPass {
                eq("id", qualityAssessmentMergedPassId)
            }
        }
        return chromosomeQualityAssessment
    }



    List<AbstractQualityAssessment> qualityAssessmentMergedForSpecificChromosomes(List<Chromosomes> chromosomes, List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses) {
        Assert.notNull(chromosomes, 'Parameter "chromosomes" may not be null')
        Assert.notNull(qualityAssessmentMergedPasses, 'Parameter "qualityAssessmentMergedPasses" may not be null')

        qualityAssessmentMergedPasses*.processedMergedBamFile.each { AbstractMergedBamFile abstractMergedBamFile ->
            assert ( abstractMergedBamFile instanceof RoddyBamFile || abstractMergedBamFile instanceof ProcessedMergedBamFile )
        }

        List<QualityAssessmentMergedPass> roddyFilePasses = qualityAssessmentMergedPasses.findAll { it.processedMergedBamFile instanceof RoddyBamFile }
        List<QualityAssessmentMergedPass> bamFilePasses =  qualityAssessmentMergedPasses.findAll { it.processedMergedBamFile instanceof ProcessedMergedBamFile }

        if (chromosomes) {
            List<AbstractQualityAssessment> roddyQAPerChromosome = []
            List<AbstractQualityAssessment> bamFileQAPerChromosome = []

            if (bamFilePasses) {
                bamFileQAPerChromosome = ChromosomeQualityAssessmentMerged.createCriteria().list {
                    'in'("chromosomeName", chromosomes*.alias)
                    'in'('qualityAssessmentMergedPass', bamFilePasses)
                }
            }

            if (roddyFilePasses) {
                roddyQAPerChromosome = RoddyMergedBamQa.createCriteria().list {
                    'in'("chromosome", chromosomes*.alias)
                    'in'('qualityAssessmentMergedPass', roddyFilePasses)
                }
            }
            return bamFileQAPerChromosome + roddyQAPerChromosome
        }
        return []
    }

}
