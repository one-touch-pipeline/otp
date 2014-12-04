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



    List<ChromosomeQualityAssessmentMerged> qualityAssessmentMergedForSpecificChromosomes(List<Chromosomes> chromosomes, List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses) {
        Assert.notNull(chromosomes, 'Parameter "chromosomes" may not be null')
        Assert.notNull(qualityAssessmentMergedPasses, 'Parameter "qualityAssessmentMergedPasses" may not be null')

        if (chromosomes && qualityAssessmentMergedPasses) {
            List<ChromosomeQualityAssessmentMerged> chromosomeQualityAssessments = ChromosomeQualityAssessmentMerged.createCriteria().list {
                'in'("chromosomeName", chromosomes*.alias)
                'in'('qualityAssessmentMergedPass', qualityAssessmentMergedPasses)
            }
            return chromosomeQualityAssessments
        }
        return []
    }

}
