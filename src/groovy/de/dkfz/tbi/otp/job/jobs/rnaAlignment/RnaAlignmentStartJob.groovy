package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.AbstractRoddyAlignmentStartJob
import de.dkfz.tbi.otp.ngsdata.*

@Component('rnaAlignmentStartJob')
@Scope('singleton')
class RnaAlignmentStartJob extends AbstractRoddyAlignmentStartJob {
    @Override
    List<SeqType> getSeqTypes() {
        return SeqTypeService.getRnaAlignableSeqTypes()
    }

    /**
     * For RNA alignment the incremental merging is not implemented yet, therefore null is returned.
     */
    @Override
    RoddyBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {
        return null
    }

    @Override
    AbstractMergedBamFile reallyCreateBamFile(MergingWorkPackage mergingWorkPackage, int identifier, Set<SeqTrack> seqTracks, ConfigPerProjectAndSeqType config, AbstractMergedBamFile baseBamFile = null) {
        new RnaRoddyBamFile (
                workPackage: mergingWorkPackage,
                identifier: identifier,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${identifier}",
                seqTracks: seqTracks,
                config: config,
        )
    }
}
