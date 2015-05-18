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
                createProcess(roddyBamFile)
            }
        }
    }

    static List<MergingWorkPackage> findProcessableMergingWorkPackages(short minPriority) {
        return MergingWorkPackage.findAll(
                'FROM MergingWorkPackage mwp ' +
                'WHERE needsProcessing = true ' +
                'AND NOT EXISTS ( ' +
                    'FROM RoddyBamFile ' +
                    'WHERE workPackage = mwp ' +
                    'AND fileOperationStatus <> :processed ' +
                    'AND withdrawn = false ' +
                ') ' +
                'AND sample.individual.project.processingPriority >= :minPriority ' +
                'ORDER BY sample.individual.project.processingPriority DESC',
                [
                        processed: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                        minPriority: minPriority,
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
    static RoddyBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {
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
        RoddyBamFile roddyBamFile = new RoddyBamFile(
                workPackage: mergingWorkPackage,
                identifier: RoddyBamFile.nextIdentifier(mergingWorkPackage),
                baseBamFile: baseBamFile,
                seqTracks: SeqTrack.getAll(mergingWorkPackage.findMergeableSeqTracks()*.id - baseBamFile?.containedSeqTracks*.id) as Set,
                config: exactlyOneElement(RoddyWorkflowConfig.findAllWhere(
                        project: mergingWorkPackage.project,
                        workflow: mergingWorkPackage.workflow,
                        obsoleteDate: null,
                )),
                roddyVersion: CollectionUtils.exactlyOneElement(ProcessingOption.findAllByNameAndDateObsoleted("roddyVersion", null))
        )
        roddyBamFile.numberOfMergedLanes = roddyBamFile.containedSeqTracks.size()
        assert roddyBamFile.save(flush: true, failOnError: true)
        return roddyBamFile
    }
}


