/*
 * Copyright 2011-2020 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.jobs.alignment

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.scheduling.annotation.Scheduled

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@CompileDynamic
@Slf4j
abstract class AbstractAlignmentStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Override
    @Scheduled(fixedDelay = 60000L)
    void execute() {
        SessionUtils.withTransaction {
            startAlignment()
        }
    }

    protected void startAlignment() {
        int minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority == ProcessingPriority.SUPREMUM) {
            return
        }

        MergingWorkPackage mergingWorkPackage = findProcessableMergingWorkPackages(minPriority).find { !isDataInstallationWFInProgress(it) }
        if (mergingWorkPackage) {
            mergingWorkPackage.needsProcessing = false
            assert mergingWorkPackage.save(flush: true)
            AbstractBamFile bamFile = createBamFile(mergingWorkPackage, findUsableBaseBamFile(mergingWorkPackage))
            notificationCreator.setStartedForSeqTracks(bamFile.containedSeqTracks, OtrsTicket.ProcessingStep.ALIGNMENT)
            createProcess(bamFile)
        }
    }

    @Override
    Process restart(Process process) {
        assert process

        AbstractBamFile failedInstance = (AbstractBamFile) process.processParameterObject

        return AbstractBamFile.withTransaction {
            failedInstance.withdraw()
            MergingWorkPackage mergingWorkPackage = failedInstance.workPackage
            mergingWorkPackage.needsProcessing = false
            AbstractBamFile bamFile = createBamFile(mergingWorkPackage, findUsableBaseBamFile(mergingWorkPackage))

            assert bamFile.save(flush: true)
            return createProcess(bamFile)
        }
    }

    abstract List<SeqType> getSeqTypes()

    List<MergingWorkPackage> findProcessableMergingWorkPackages(int minPriority) {
        return MergingWorkPackage.findAll(
                'FROM MergingWorkPackage mwp ' +
                        'WHERE needsProcessing = true ' +
                        'AND seqType IN (:seqTypes) ' +
                        'AND sample.individual.project.archived = false ' +
                        'AND NOT EXISTS (' +
                        ' FROM AbstractBamFile ' +
                        'WHERE workPackage = mwp ' +
                        'AND fileOperationStatus <> :processed ' +
                        'AND withdrawn = false ' +
                        ')' +
                        ' AND mwp.seqTracks is not empty ' +
                        'AND sample.individual.project.processingPriority.priority >= :minPriority ' +
                        'ORDER BY sample.individual.project.processingPriority.priority DESC, mwp.id ASC',
                [
                        processed  : AbstractBamFile.FileOperationStatus.PROCESSED,
                        minPriority: minPriority,
                        seqTypes   : seqTypes,
                ]
        )
    }

    private static boolean isDataInstallationWFInProgress(MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        return mergingWorkPackage.seqTracks.find {
            it.dataInstallationState != SeqTrack.DataProcessingState.FINISHED
        }
    }

    private static AbstractBamFile findBamFileInProjectFolder(MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        // Find the latest BAM file which moving to the final destination has been initiated for, regardless of whether
        // the moving was successful or not.
        AbstractBamFile bamFile = AbstractBamFile.find(
                'FROM AbstractBamFile ' +
                        'WHERE fileOperationStatus IN (:inprogress, :processed) ' +
                        'AND workPackage = :mergingWorkPackage ' +
                        'ORDER BY identifier DESC',
                [
                        mergingWorkPackage: mergingWorkPackage,
                        inprogress        : AbstractBamFile.FileOperationStatus.INPROGRESS,
                        processed         : AbstractBamFile.FileOperationStatus.PROCESSED,
                ],
        )
        assert bamFile?.id == mergingWorkPackage.bamFileInProjectFolder?.id
        if (bamFile && bamFile.fileOperationStatus != AbstractBamFile.FileOperationStatus.PROCESSED) {
            // If we get here, moving of bamFile to the final destination has been initiated, but has not been reported
            // to have finished successfully. So we do not know what currently is on the file system.
            return null
        }
        return bamFile
    }

    /**
     * Returns the {@link AbstractBamFile} which
     * <ul>
     *     <li>is {@link AbstractBamFile.FileOperationStatus#PROCESSED}</li>
     *     <li>has not been overwritten by a later {@link AbstractBamFile}</li>
     *     <li>is not withdrawn</li>
     * </ul>
     */
    protected AbstractBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        AbstractBamFile bamFile = findBamFileInProjectFolder(mergingWorkPackage)
        if (!bamFile || bamFile.withdrawn) {
            return null
        }
        return bamFile
    }

    AbstractBamFile createBamFile(MergingWorkPackage mergingWorkPackage, AbstractBamFile baseBamFile) {
        assert mergingWorkPackage
        AbstractBamFile previousBamFile = mergingWorkPackage.bamFileInProjectFolder
        List<Long> mergableSeqTracks = mergingWorkPackage.seqTracks*.id
        Set<SeqTrack> seqTracks = SeqTrack.getAll(mergableSeqTracks) as Set

        ConfigPerProjectAndSeqType config = getConfig(mergingWorkPackage)

        int identifier = RoddyBamFile.nextIdentifier(mergingWorkPackage)

        AbstractBamFile bamFile = reallyCreateBamFile(mergingWorkPackage, identifier, seqTracks, config, baseBamFile)
        // has to be set explicitly to old value due strange behavior of GORM (?)
        mergingWorkPackage.bamFileInProjectFolder = previousBamFile
        bamFile.numberOfMergedLanes = bamFile.containedSeqTracks.size()
        assert bamFile.save(flush: true)
        return bamFile
    }

    abstract AbstractBamFile reallyCreateBamFile(MergingWorkPackage mergingWorkPackage, int identifier, Set<SeqTrack> seqTracks,
                                                 ConfigPerProjectAndSeqType config, AbstractBamFile baseBamFile = null)

    abstract ConfigPerProjectAndSeqType getConfig(MergingWorkPackage mergingWorkPackage)
}
