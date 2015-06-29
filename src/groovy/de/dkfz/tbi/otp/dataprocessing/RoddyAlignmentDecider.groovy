package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog

abstract class RoddyAlignmentDecider extends AbstractAlignmentDecider {

    // See integration test for explanation in which cases workpackages needs processing
    @Override
    void prepareForAlignment(MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign) {
        RoddyBamFile latestValidBamFile = getLatestBamFileWhichHasBeenOrCouldBeCopied(workPackage)

        def setNeedsProcessing = { String reason ->
            workPackage.needsProcessing = true
            assert workPackage.save(failOnError: true)
            threadLog?.info("Will align ${seqTrack} for ${workPackage} because ${reason}.")
        }

        if (!latestValidBamFile) {
            setNeedsProcessing("no valid bam file for this work package exists yet")
        } else if(!latestValidBamFile.getContainedSeqTracks().contains(seqTrack)) {
            setNeedsProcessing("it's not contained in latest bam file.")
        } else if(latestValidBamFile.withdrawn) {
            setNeedsProcessing("latest bam file is withdrawn.")
        } else {
            if(forceRealign) {
                threadLog?.info("Will not align ${seqTrack} for ${workPackage} " +
                        "because the latest bam file already contains the seqtrack. " +
                        "(You can only realign if you set the latest bam file (ID ${latestValidBamFile.id}) to withdrawn).")
            } else {
                threadLog?.info("Will not align ${seqTrack} for ${workPackage} " +
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
    boolean canWorkflowAlign(SeqTrack seqTrack) {
        return SeqType.findAllByNameAndLibraryLayout(
                SeqTypeNames.WHOLE_GENOME.seqTypeName, SeqType.LIBRARYLAYOUT_PAIRED
        )*.id.contains(seqTrack.seqType.id)
    }
}
