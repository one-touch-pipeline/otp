package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.RunSegment
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import org.springframework.scheduling.annotation.Scheduled

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

abstract class RoddyAlignmentStartJob extends AbstractStartJobImpl {

    @Override
    @Scheduled(fixedDelay = 10000l)
    void execute() {
        startRoddyAlignment()
    }

    protected void startRoddyAlignment() {
        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }

        RoddyBamFile.withTransaction {
            MergingWorkPackage mergingWorkPackage = findProcessableMergingWorkPackages(minPriority).find { !isDataInstallationWFInProgress(it) }
            if (mergingWorkPackage) {
                mergingWorkPackage.needsProcessing = false
                assert mergingWorkPackage.save(failOnError: true)
                RoddyBamFile roddyBamFile = createRoddyBamFile(mergingWorkPackage, findUsableBaseBamFile(mergingWorkPackage))
                trackingService.setStartedForSeqTracks(roddyBamFile.containedSeqTracks, OtrsTicket.ProcessingStep.ALIGNMENT)
                createProcess(roddyBamFile)
            }
        }
    }

    abstract List<SeqType> getSeqTypes()

    List<MergingWorkPackage> findProcessableMergingWorkPackages(short minPriority) {
        return MergingWorkPackage.findAll(
                'FROM MergingWorkPackage mwp ' +
                'WHERE needsProcessing = true ' +
                'AND seqType IN (:seqTypes)' +
                'AND NOT EXISTS ( ' +
                    'FROM RoddyBamFile ' +
                    'WHERE workPackage = mwp ' +
                    'AND fileOperationStatus <> :processed ' +
                    'AND withdrawn = false ' +
                ') ' +
                'AND sample.individual.project.processingPriority >= :minPriority ' +
                'ORDER BY sample.individual.project.processingPriority DESC, mwp.id ASC',
                [
                        processed: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                        minPriority: minPriority,
                        seqTypes: seqTypes,
                ]
        )
    }

    static boolean isDataInstallationWFInProgress(MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        return RunSegment.createCriteria().get {
            ne('filesStatus', RunSegment.FilesStatus.FILES_CORRECT)
            run {
                'in'('id', mergingWorkPackage.findMergeableSeqTracks()*.run*.id)
            }
            maxResults(1)
        }
    }

    static RoddyBamFile findBamFileInProjectFolder(MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        // Find the latest BAM file which moving to the final destination has been initiated for, regardless of whether
        // the moving was successful or not.
        RoddyBamFile bamFile = RoddyBamFile.find(
                'FROM RoddyBamFile ' +
                'WHERE fileOperationStatus IN (:inprogress, :processed) ' +
                'AND workPackage = :mergingWorkPackage ' +
                'ORDER BY identifier DESC',
                [
                        mergingWorkPackage: mergingWorkPackage,
                        inprogress: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                        processed: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                ],
        )
        assert bamFile?.id == mergingWorkPackage.bamFileInProjectFolder?.id
        if (bamFile && bamFile.fileOperationStatus != AbstractMergedBamFile.FileOperationStatus.PROCESSED) {
            // If we get here, moving of bamFile to the final destination has been initiated, but has not been reported
            // to have finished successfully. So we do not know what currently is on the file system.
            return null
        }
        return bamFile
    }

    /**
     * Returns the {@link RoddyBamFile} which
     * <ul>
     *     <li>is {@link AbstractMergedBamFile.FileOperationStatus#PROCESSED}</li>
     *     <li>has not been overwritten by a later {@link RoddyBamFile}</li>
     *     <li>is not withdrawn</li>
     * </ul>
     */
    RoddyBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {

        // TODO OTP-2192: incremental merging is currently not supported by Roddy
        return null

        assert mergingWorkPackage
        RoddyBamFile bamFile = findBamFileInProjectFolder(mergingWorkPackage)
        if (!bamFile || bamFile.withdrawn) {
            return null
        } else {
            return bamFile
        }
    }

    static RoddyBamFile createRoddyBamFile(MergingWorkPackage mergingWorkPackage, RoddyBamFile baseBamFile) {
        assert mergingWorkPackage
        RoddyBamFile previousRoddyBamFile = mergingWorkPackage.bamFileInProjectFolder
        List<Long> mergableSeqtracks =  mergingWorkPackage.findMergeableSeqTracks()*.id
        List<Long> containedSeqTracks = baseBamFile?.containedSeqTracks*.id
        Set<SeqTrack> seqTracks = SeqTrack.getAll(mergableSeqtracks - containedSeqTracks) as Set

        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForIndividual(mergingWorkPackage.individual, mergingWorkPackage.seqType, mergingWorkPackage.pipeline)
        assert config: "Could not find one RoddyWorkflowConfig for ${mergingWorkPackage.project}, ${mergingWorkPackage.seqType} and ${mergingWorkPackage.pipeline}"

        int identifier = RoddyBamFile.nextIdentifier(mergingWorkPackage)
        RoddyBamFile roddyBamFile = new RoddyBamFile(
                workPackage: mergingWorkPackage,
                identifier: identifier,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${identifier}",
                baseBamFile: baseBamFile,
                seqTracks: seqTracks,
                config: config,
        )
        // has to be set explicitly to old value due strange behavior of GORM (?)
        mergingWorkPackage.bamFileInProjectFolder = previousRoddyBamFile
        roddyBamFile.numberOfMergedLanes = roddyBamFile.containedSeqTracks.size()
        assert roddyBamFile.save(flush: true, failOnError: true)
        assert !roddyBamFile.isOldStructureUsed()
        return roddyBamFile
    }
}


