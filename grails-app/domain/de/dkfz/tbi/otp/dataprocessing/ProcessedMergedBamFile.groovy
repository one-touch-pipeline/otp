package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Represents a merged bam file stored on the file system
 * and produced by the merging process identified by the
 * given {@link MergingPass}
 *
 *
 */
class ProcessedMergedBamFile extends AbstractFileSystemBamFile {

    /**
     * Checksum to verify success of copying.
     * When the file - and all other files handled by the transfer workflow - are copied, its checksum is stored in this property.
     * Otherwise it is null.
     */
    String md5sum

    /** Additional digest, may be used in the future (to verify xz compression) */
    String sha256sum

    static belongsTo = [
        mergingPass: MergingPass
    ]

    static constraints = {
        md5sum nullable: true , validator: { val, obj ->
            return (!val || (val && obj.fileOperationStatus == FileOperationStatus.PROCESSED))
        }
        fileOperationStatus validator: { val, obj ->
            return ((val != FileOperationStatus.PROCESSED && obj.md5sum == null) || (val == FileOperationStatus.PROCESSED && obj.md5sum != null))
        }
        sha256sum(nullable: true)
    }

    Project getProject() {
        return mergingPass.project
    }

    Individual getIndividual() {
        return mergingPass.individual
    }

    Sample getSample() {
        return mergingPass.sample
    }

    SeqType getSeqType() {
        return mergingPass.seqType
    }

    MergingSet getMergingSet() {
        return mergingPass.mergingSet
    }

    /**
     * @return <code>true</code>, if this {@link ProcessedMergedBamFile} is from the latest merging
     * @see MergingPass#isLatestPass()
     */
    public boolean isMostRecentBamFile() {
        return (mergingPass.isLatestPass() && mergingSet.isLatestSet())
    }


    @Override
    public String toString() {
        MergingWorkPackage mergingWorkPackage = mergingPass.mergingSet.mergingWorkPackage
        return "id: ${id} " +
        "pass: ${mergingPass.identifier} " + (mergingPass.latestPass ? "(latest) " : "") +
        "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
        "<br>sample: ${mergingWorkPackage.sample} " +
        "seqType: ${mergingWorkPackage.seqType} " +
        "<br>project: ${mergingWorkPackage.project}"
    }

    static mapping = {
        mergingPass index: "abstract_bam_file_merging_pass_idx"
    }
}
