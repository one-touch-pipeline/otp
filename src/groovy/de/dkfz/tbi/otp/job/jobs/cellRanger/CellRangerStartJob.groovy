package de.dkfz.tbi.otp.job.jobs.cellRanger

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.jobs.alignment.AbstractAlignmentStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

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
                        "programVersion_${mergingWorkPackage.config.programVersion.replace("/", "-")}",
                ].join('_'),
                seqTracks: seqTracks,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
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
