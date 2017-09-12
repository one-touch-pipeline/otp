package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.*

/**
 * To be extended later on
 * Class to represent the data for the entire set of chromosomes (1 to 22, X, Y and M) as one
 * for merged bam file
 */
class OverallQualityAssessmentMerged extends QaJarQualityAssessment {

    static belongsTo = [
        qualityAssessmentMergedPass: QualityAssessmentMergedPass
    ]

    static constraints = {
        qualityAssessmentMergedPass(validator: {
            ProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(it.abstractMergedBamFile))
        })
    }

    static mapping = {
        qualityAssessmentMergedPass index: "abstract_quality_assessment_quality_assessment_merged_pass_idx"
    }

    Project getProject() {
        return qualityAssessmentMergedPass.project
    }

    Individual getIndividual() {
        return qualityAssessmentMergedPass.individual
    }

    SampleType getSampleType() {
        return qualityAssessmentMergedPass.sampleType
    }

    MergingPass getMergingPass() {
        return qualityAssessmentMergedPass.mergingPass
    }

    MergingSet getMergingSet() {
        return qualityAssessmentMergedPass.mergingSet
    }

    MergingWorkPackage getMergingWorkPackage() {
        return qualityAssessmentMergedPass.mergingWorkPackage
    }

    SeqType getSeqType() {
        return qualityAssessmentMergedPass.seqType
    }

    ProcessedMergedBamFile getProcessedMergedBamFile() {
        return qualityAssessmentMergedPass.abstractMergedBamFile as ProcessedMergedBamFile
    }

    ReferenceGenome getReferenceGenome() {
        return qualityAssessmentMergedPass.referenceGenome
    }

}
