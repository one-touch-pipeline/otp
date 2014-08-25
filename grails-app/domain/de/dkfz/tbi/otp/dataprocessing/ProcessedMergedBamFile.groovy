package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.ngsdata.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * Represents a merged bam file stored on the file system
 * and produced by the merging process identified by the
 * given {@link MergingPass}
 *
 *
 */
class ProcessedMergedBamFile extends AbstractFileSystemBamFile {

    static transients = ["abstractBamFileService"]
    AbstractBamFileService abstractBamFileService

    static belongsTo = [
        mergingPass: MergingPass
    ]

    FileOperationStatus fileOperationStatus = FileOperationStatus.DECLARED

    /**
     * Holds the number of lanes which were merged in this ProcessedMergedBamFile
     */
    // Has to be from Type Integer so that it can be nullable
    Integer numberOfMergedLanes

    static constraints = {
        md5sum nullable: true, validator: { val, obj ->
            return (!val || (val && obj.fileOperationStatus == FileOperationStatus.PROCESSED))
        }
        fileOperationStatus validator: { val, obj ->
            return ((val != FileOperationStatus.PROCESSED && obj.md5sum == null) || (val == FileOperationStatus.PROCESSED && obj.md5sum != null))
        }
        mergingPass nullable: false, unique: true
        numberOfMergedLanes nullable: true
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

    SampleType getSampleType() {
        return mergingPass.sampleType
    }

    SeqType getSeqType() {
        return mergingPass.seqType
    }

    MergingSet getMergingSet() {
        return mergingPass.mergingSet
    }

    MergingWorkPackage getMergingWorkPackage() {
        return mergingPass.mergingWorkPackage
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

    static mapping = { mergingPass index: "abstract_bam_file_merging_pass_idx" }

    @Override
    /** Beware: This method calls a method of a service */
    Set<SeqTrack> getContainedSeqTracks() {
        List<ProcessedBamFile> processedBamFiles = abstractBamFileService.findAllByProcessedMergedBamFile(this)
        return processedBamFiles*.alignmentPass*.seqTrack as Set<SeqTrack>
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        OverallQualityAssessmentMerged.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'processedMergedBamFile', this
            }
            order 'id', 'desc'
            maxResults 1
        }
    }
}
