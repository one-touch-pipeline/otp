package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Represents a merged bam file stored on the file system
 * which was processed externally (not in OTP)
 *
 */
class ExternallyProcessedMergedBamFile extends AbstractFileSystemBamFile {

    static belongsTo = [
        fastqSet: FastqSet
    ]

    Project getProject() {
        return fastqSet.project
    }

    Individual getIndividual() {
        return fastqSet.individual
    }

    Sample getSample() {
        return fastqSet.sample
    }

    @Override
    SeqType getSeqType() {
        return fastqSet.seqType
    }

    @Override
    public String toString() {
        return "id: ${id} (external) " +
                "<br>sample: ${sample} seqType: ${seqType} " +
                "<br>project: ${project}"
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return fastqSet.seqTracks
    }

    @Override
    public AbstractQualityAssessment getOverallQualityAssessment() {
        throw new MissingPropertyException('Quality assessment is not implemented for externally imported BAM files')
    }

    static mapping = {
        fastqSet index: "abstract_bam_file_fastq_set_idx"
    }
}
