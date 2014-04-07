package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Each execution of the Quality Assessment Workflow on the particular data file is represented as QualityAssessmentPass.
 */
class QualityAssessmentPass {

    int identifier
    String description

    static constraints = {
        description(nullable: true)
    }

    public String toString() {
        return "pass: ${identifier} on ${processedBamFile.alignmentPass}"
    }

    static belongsTo = [
        processedBamFile: ProcessedBamFile
    ]

    AlignmentPass getAlignmentPass() {
        return processedBamFile.alignmentPass
    }

    Project getProject() {
        return processedBamFile.project
    }

    SeqTrack getSeqTrack() {
        return processedBamFile.seqTrack
    }

    Sample getSample() {
        return processedBamFile.sample
    }

    SeqType getSeqType() {
        return processedBamFile.seqType
    }
}
