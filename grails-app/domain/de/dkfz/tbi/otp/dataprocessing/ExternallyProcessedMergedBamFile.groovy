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

    /** source of the file, eg. workflow or import name; used to construct the path of the file */
    String source
    String fileName

    ReferenceGenome referenceGenome

    @Override
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
    MergingWorkPackage getMergingWorkPackage() {
        throw new UnsupportedOperationException()  // We might return a real MergingWorkPackage here in the future.
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return fastqSet.seqTracks
    }

    @Override
    public AbstractQualityAssessment getOverallQualityAssessment() {
        throw new MissingPropertyException('Quality assessment is not implemented for externally imported BAM files')
    }


    public OtpPath getFilePath() {
        String relative = MergedAlignmentDataFileService.buildRelativePath(seqType, sample)
        return new OtpPath(project, relative, "nonOTP",
                "${source}_${referenceGenome}", fileName)
    }


    static constraints = {
        referenceGenome nullable: false
        source blank: false
        fileName blank: false
    }

    static mapping = {
        fastqSet index: "abstract_bam_file_fastq_set_idx"
    }
}
