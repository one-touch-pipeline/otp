package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.*

/**
 * Represents a merged bam file stored on the file system
 * and produced by the merging process identified by the
 * given {@link MergingPass}
 */
class ProcessedMergedBamFile extends AbstractMergedBamFile implements ProcessParameterObject {

    static belongsTo = [
        mergingPass: MergingPass,
    ]

    static constraints = {
        type validator: { it == BamType.MDUP }
        mergingPass nullable: false, unique: true
        workPackage validator: { val, obj ->
            val.id == obj.mergingSet.mergingWorkPackage.id &&
                    val?.pipeline?.name == Pipeline.Name.DEFAULT_OTP &&
                    MergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }
    }

    MergingSet getMergingSet() {
        return mergingPass.mergingSet
    }

    /**
     * @return <code>true</code>, if this {@link ProcessedMergedBamFile} is from the latest merging
     * @see MergingPass#isLatestPass()
     */
    @Override
    boolean isMostRecentBamFile() {
        return (mergingPass.isLatestPass() && mergingSet.isLatestSet())
    }

    @Override
    String toString() {
        MergingWorkPackage mergingWorkPackage = mergingPass.mergingSet.mergingWorkPackage
        return "PMBF ${id}: " +
        "pass: ${mergingPass.identifier} " + (mergingPass.latestPass ? "(latest) " : "") +
        "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
        "<br>sample: ${mergingWorkPackage.sample} " +
        "seqType: ${mergingWorkPackage.seqType} " +
        "<br>project: ${mergingWorkPackage.project}"
    }

    static mapping = { mergingPass index: "abstract_bam_file_merging_pass_idx" }

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return MergingWorkPackage.get(workPackage.id)
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        final Set<SeqTrack> seqTracks = mergingSet.containedSeqTracks
        if (seqTracks.empty) {
            throw new IllegalStateException("MergingSet ${mergingSet} is empty.")
        }
        return seqTracks
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        OverallQualityAssessmentMerged.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'abstractMergedBamFile', this
            }
            order 'id', 'desc'
            maxResults 1
        }
    }

    String fileNameNoSuffix() {
        String seqTypeName = "${this.seqType.name}_${this.seqType.libraryLayout}"
        return "${this.sampleType.name}_${this.individual.pid}_${seqTypeName}_merged.mdup"
    }

    @Override
    String getBamFileName() {
        String body = this.fileNameNoSuffix()
        return "${body}.bam"
    }

    @Override
    String getBaiFileName() {
        String body = this.fileNameNoSuffix()
        return "${body}.bai"
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(baseDirectory, bamFileName)
    }

    @Override
    AlignmentConfig getAlignmentConfig() {
        throw new MissingPropertyException('AlignmentConfig is not implemented for processed merged BAM files')
    }

    @Override
    File getFinalInsertSizeFile() {
        assert false: 'not available for ProcessedMergedBamFile'
    }

    @Override
    Integer getMaximalReadLength() {
        assert false: 'not used for ProcessedMergedBamFile'
    }

    @Override
    void withdraw() {
        withTransaction {
            super.withdraw()

            withdrawDownstreamBamFiles()
        }
    }
}
