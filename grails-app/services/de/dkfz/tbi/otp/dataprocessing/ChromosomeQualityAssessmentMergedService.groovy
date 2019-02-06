package de.dkfz.tbi.otp.dataprocessing

import org.springframework.util.Assert

class ChromosomeQualityAssessmentMergedService {

    List<AbstractQualityAssessment> qualityAssessmentMergedForSpecificChromosomes(
            List<String> chromosomes, List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses) {
        Assert.notNull(chromosomes, 'Parameter "chromosomes" may not be null')
        Assert.notNull(qualityAssessmentMergedPasses, 'Parameter "qualityAssessmentMergedPasses" may not be null')

        qualityAssessmentMergedPasses*.abstractMergedBamFile.each { AbstractMergedBamFile abstractMergedBamFile ->
            assert (abstractMergedBamFile instanceof RoddyBamFile || abstractMergedBamFile instanceof ProcessedMergedBamFile)
        }

        List<QualityAssessmentMergedPass> roddyFilePasses = qualityAssessmentMergedPasses.findAll {
            it.abstractMergedBamFile instanceof RoddyBamFile
        }
        List<QualityAssessmentMergedPass> bamFilePasses = qualityAssessmentMergedPasses.findAll {
            it.abstractMergedBamFile instanceof ProcessedMergedBamFile
        }

        if (chromosomes) {
            List<AbstractQualityAssessment> roddyQAPerChromosome = []
            List<AbstractQualityAssessment> bamFileQAPerChromosome = []

            if (bamFilePasses) {
                bamFileQAPerChromosome = ChromosomeQualityAssessmentMerged.createCriteria().list {
                    'in'("chromosomeName", chromosomes)
                    'in'('qualityAssessmentMergedPass', bamFilePasses)
                }
            }

            if (roddyFilePasses) {
                roddyQAPerChromosome = RoddyMergedBamQa.createCriteria().list {
                    'in'("chromosome", chromosomes)
                    'in'('qualityAssessmentMergedPass', roddyFilePasses)
                }
            }
            return bamFileQAPerChromosome + roddyQAPerChromosome
        }
        return []
    }
}
