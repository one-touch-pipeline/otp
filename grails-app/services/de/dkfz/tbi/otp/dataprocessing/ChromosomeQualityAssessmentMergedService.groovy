package de.dkfz.tbi.otp.dataprocessing

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
}
