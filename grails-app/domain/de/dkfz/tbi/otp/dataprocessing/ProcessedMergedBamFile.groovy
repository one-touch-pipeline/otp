package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * Represents a merged bam file stored on the file system
 * and produced by the merging process identified by the
 * given {@link MergingPass}
 *
 *
 */
class ProcessedMergedBamFile extends AbstractMergedBamFile implements ProcessParameterObject {

    static belongsTo = [
        mergingPass: MergingPass
    ]

    static constraints = {
        mergingPass nullable: false, unique: true
        workPackage validator: { val, obj ->
            val.id == obj.mergingSet.mergingWorkPackage.id &&
            val?.workflow?.name == Workflow.Name.DEFAULT_OTP
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
    public boolean isMostRecentBamFile() {
        return (mergingPass.isLatestPass() && mergingSet.isLatestSet())
    }

    @Override
    public String toString() {
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
    public String fileNameNoSuffix() {
        String seqTypeName = "${this.seqType.name}_${this.seqType.libraryLayout}"
        return "${this.sampleType.name}_${this.individual.pid}_${seqTypeName}_merged.mdup"
    }

    @Override
    public String getBamFileName() {
        String body = this.fileNameNoSuffix()
        return "${body}.bam"
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(baseDirectory, bamFileName)
    }

    void withdraw() {
        withTransaction {
            super.withdrawCorrespondingSnvResults()

            withdrawDownstreamBamFiles()

            LogThreadLocal.threadLog.info "Execute WithdrawnFilesRename.groovy script afterwards"
            LogThreadLocal.threadLog.info "Withdrawing ${this}"
            withdrawn = true
            assert save(flush: true)
        }
    }

}
