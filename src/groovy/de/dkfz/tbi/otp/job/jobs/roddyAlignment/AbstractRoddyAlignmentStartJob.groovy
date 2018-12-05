package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.jobs.alignment.*
import de.dkfz.tbi.otp.ngsdata.*

abstract class AbstractRoddyAlignmentStartJob extends AbstractAlignmentStartJob {
    @Override
    AbstractMergedBamFile reallyCreateBamFile(MergingWorkPackage mergingWorkPackage, int identifier, Set<SeqTrack> seqTracks, ConfigPerProjectAndSeqType config, AbstractMergedBamFile baseBamFile) {
        new RoddyBamFile (
                workPackage: mergingWorkPackage,
                identifier: identifier,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${identifier}",
                baseBamFile: baseBamFile,
                seqTracks: seqTracks,
                config: config,
        )
    }

    @Override
    ConfigPerProjectAndSeqType getConfig(MergingWorkPackage mergingWorkPackage) {
        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForIndividual(mergingWorkPackage.individual, mergingWorkPackage.seqType, mergingWorkPackage.pipeline)
        assert config: "Could not find one RoddyWorkflowConfig for ${mergingWorkPackage.project}, ${mergingWorkPackage.seqType} and ${mergingWorkPackage.pipeline}"
        return config
    }
}
