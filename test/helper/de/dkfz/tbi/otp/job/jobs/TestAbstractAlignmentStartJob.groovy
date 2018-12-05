package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.jobs.alignment.AbstractAlignmentStartJob
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component('testAbstractAlignmentStartJob')
@Scope('singleton')
class TestAbstractAlignmentStartJob extends AbstractAlignmentStartJob {

    JobExecutionPlan jep

    @Override
    JobExecutionPlan getJobExecutionPlan() {
        return jep
    }

    @Override
    List<SeqType> getSeqTypes() {
        return [SeqTypeService.getWholeGenomePairedSeqType()]
    }

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
