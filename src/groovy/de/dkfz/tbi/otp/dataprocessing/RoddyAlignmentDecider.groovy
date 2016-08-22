package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

abstract class RoddyAlignmentDecider extends AbstractAlignmentDecider {

    // See integration test for explanation in which cases workpackages needs processing
    @Override
    void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign) {
        RoddyBamFile latestValidBamFile = getLatestBamFileWhichHasBeenOrCouldBeCopied(workPackage)

        def setNeedsProcessing = {
            workPackage.needsProcessing = true
            assert workPackage.save(failOnError: true)
            seqTrack.log("Will align{0} for ${workPackage}.")
        }

        if (!latestValidBamFile ||
                !latestValidBamFile.getContainedSeqTracks().contains(seqTrack) ||
                latestValidBamFile.withdrawn) {
            setNeedsProcessing()
        } else {
            if(forceRealign) {
                seqTrack.log("Will not align{0} for ${workPackage} " +
                        "because the latest bam file already contains the seqtrack. " +
                        "(You can only realign if you set the latest bam file (ID ${latestValidBamFile.id}) to withdrawn).")
            } else {
                seqTrack.log("Will not align{0} for ${workPackage} " +
                        "because the latest bam file already contains the seqtrack.")
            }
        }
    }

    /** this method returns the latest bam file for the work packages which is transferred or not withdrawn */
    private static RoddyBamFile getLatestBamFileWhichHasBeenOrCouldBeCopied(MergingWorkPackage workPackage) {
        List<RoddyBamFile> bamFiles = RoddyBamFile.findAllByWorkPackage(workPackage, [sort: "identifier", order: "desc"])

        RoddyBamFile latestValidBamFile = bamFiles.find {
                    !it.withdrawn ||
                    it.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.INPROGRESS ||
                    it.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        }
        return latestValidBamFile
    }

    @Override
    boolean canPipelineAlign(SeqTrack seqTrack) {
        boolean canAlign = SeqType.getPanCanAlignableSeqTypes().contains(seqTrack.seqType)
        if (canAlign && (RoddyWorkflowConfig.getLatestForProject(seqTrack.project, seqTrack.seqType, pipeline) == null)) {
            seqTrack.log("RoddyWorkflowConfig is missing for ${seqTrack.project} ${seqTrack.seqType} ${pipeline.name}.")
            return false
        }
        return canAlign
    }
}
