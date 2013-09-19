package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry

class ChromosomeQualityAssessmentService {

    ChromosomeQualityAssessment qualityAssessmentForSpecificChromosome(ReferenceGenomeEntry referenceGenomeEntry, Long qualityAssessmentPassId) {
        // FIXME: Silently returns null objects. If this is intentional, document it. Also no tests available.
        ChromosomeQualityAssessment chromosomeQualityAssessment = ChromosomeQualityAssessment.createCriteria().get {
            eq("chromosomeName", referenceGenomeEntry.name)
            qualityAssessmentPass {
                eq("id", qualityAssessmentPassId)
            }
        }
        return chromosomeQualityAssessment
    }
}
