package de.dkfz.tbi.otp.job.jobs.cellRanger

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.jobs.alignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component('cellRangerAlignmentStartJob')
@Scope('singleton')
class CellRangerStartJob extends AbstractAlignmentStartJob implements RestartableStartJob {

    @Override
    List<SeqType> getSeqTypes() {
        return SeqTypeService.cellRangerAlignableSeqTypes
    }

    @Override
    AbstractMergedBamFile reallyCreateBamFile(
            MergingWorkPackage mergingWorkPackage,
            int identifier,
            Set<SeqTrack> seqTracks,
            ConfigPerProjectAndSeqType config,
            AbstractMergedBamFile baseBamFile = null) {
        new SingleCellBamFile(
                workPackage: mergingWorkPackage,
                identifier: identifier,
                workDirectoryName: [
                        "${mergingWorkPackage.referenceGenome.name}",
                        "expectedCells_${mergingWorkPackage.expectedCells}",
                        "forcedCells_${mergingWorkPackage.enforcedCells ?: '-'}",
                        "programVersion_${mergingWorkPackage.config.programVersion}",
                ].join('_'),
                seqTracks: seqTracks,
        )
    }

    @Override
    ConfigPerProjectAndSeqType getConfig(MergingWorkPackage mergingWorkPackage) {
        CellRangerConfig config = CollectionUtils.<CellRangerConfig> atMostOneElement(
                CellRangerConfig.findAllByProjectAndPipelineAndSeqTypeAndObsoleteDate(
                        mergingWorkPackage.project, mergingWorkPackage.pipeline, mergingWorkPackage.seqType, null))
        assert config: "Could not find one CellRangerConfig for ${mergingWorkPackage.project}, ${mergingWorkPackage.seqType} " +
                "and ${mergingWorkPackage.pipeline}"
        return config
    }
}
