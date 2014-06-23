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
     * When the file is copied its checksum is stored in this property.
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

    SeqType getSeqType() {
        return mergingPass.seqType
    }

    MergingSet getMergingSet() {
        return mergingPass.mergingSet
    }

    @Override
    public String toString() {
        MergingWorkPackage mergingWorkPackage = mergingPass.mergingSet.mergingWorkPackage
        return "pass: ${mergingPass.identifier} " + (mergingPass.latestPass ? "(latest) " : "") +
                "id: ${id} " +
                "set: ${mergingPass.mergingSet.identifier} " + (mergingPass.mergingSet.latestSet ? "(latest) " : "") +
                "<br>sample: ${mergingWorkPackage.sample} " +
                "seqType: ${mergingWorkPackage.seqType} " +
                "project: ${mergingWorkPackage.project}"
    }

    static mapping = {
        mergingPass index: "abstract_bam_file_merging_pass_idx"
    }
}
