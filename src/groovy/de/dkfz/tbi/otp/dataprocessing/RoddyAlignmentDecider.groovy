package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog

/**
 * An {@link AlignmentDecider} which decides whether the conditions for the Roddy alignment are satisfied.
 */
@Component
@Scope("singleton")
class RoddyAlignmentDecider extends AbstractAlignmentDecider {

    @Override
    Workflow.Name getWorkflowName() {
        return Workflow.Name.RODDY
    }

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

        RoddyBamFile latestValidBamFile = bamFiles.find { !it.withdrawn || it.md5sum }
        return latestValidBamFile
    }
}
