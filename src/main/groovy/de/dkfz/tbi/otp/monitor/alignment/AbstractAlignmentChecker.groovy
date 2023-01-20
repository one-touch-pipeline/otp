/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.monitor.alignment

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.monitor.PipelinesChecker
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.workflowExecution.Workflow

@CompileDynamic
abstract class AbstractAlignmentChecker extends PipelinesChecker<SeqTrack> {

    static final String HEADER_NO_CONFIG =
            'For the following project seqtype combination no config is defined'

    static final String HEADER_NO_MERGING_WORK_PACKAGE =
            'For the following SeqTracks no corresponding mergingWorkPackage could be found'

    static final String HEADER_OLD_INSTANCE_RUNNING =
            'Old instance running'

    static final String HEADER_MWP_WITHOUT_BAM =
            'The following MergingWorkPackages marked as processed, but have no corresponding bam files'

    static final String HEADER_MWP_WITH_WITHDRAWN_BAM =
            'The following MergingWorkPackages have an withdrawn bam files'

    static final String HEADER_RUNNING_DECLARED =
            'running (declared)'

    static final String HEADER_RUNNING_NEEDS_PROCESSING =
            'running (needs_processing)'

    static final String HEADER_RUNNING_IN_PROGRESS =
            'running (in_progress)'

    @Override
    List handle(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        if (!seqTracks) {
            return []
        }
        seqTracks.unique()

        output.showWorkflowOldSystem(workflowName)

        Set<SeqType> supportedSeqTypes = seqTypes

        Map seqTrackMap = seqTracks.groupBy {
            supportedSeqTypes.contains(it.seqType)
        }

        if (seqTrackMap[false]) {
            output.showUniqueNotSupportedSeqTypes(seqTrackMap[false], { SeqTrack seqTrack ->
                "${seqTrack.seqType.displayNameWithLibraryLayout}"
            })
        }

        if (seqTrackMap[true]) {
            List<SeqTrack> alignableSeqTracks = seqTrackMap[true] ?: []

            List<SeqTrack> noConfig = getSeqTracksWithoutCorrespondingAlignmentConfig(alignableSeqTracks)
            output.showUniqueList(HEADER_NO_CONFIG, noConfig, { "${it.project}  ${it.seqType}" })

            List<SeqTrack> seqTracksWithConfig = alignableSeqTracks - noConfig

            List<SeqTrack> seqTracksWithReferenceGenome = filterWithoutReferenceGenome(seqTracksWithConfig, output)

            List<SeqTrack> filteredSeqTracks = filter(seqTracksWithReferenceGenome, output)

            Map mergingWorkPackageMap = mergingWorkPackageForSeqTracks(filteredSeqTracks)
            output.showList(HEADER_NO_MERGING_WORK_PACKAGE, mergingWorkPackageMap.seqTracksWithoutMergingWorkpackage)

            List<MergingWorkPackage> mergingWorkPackages = mergingWorkPackageMap.mergingWorkPackages ?: []

            Map<Boolean, Collection<MergingWorkPackage>> mergingWorkPackagesByNeedsProcessing =
                    mergingWorkPackages.groupBy {
                        it.needsProcessing
                    }

            List mergingWorkPackageNeedsProcessing = mergingWorkPackagesByNeedsProcessing[true] ?: []

            List<AbstractMergedBamFile> alreadyRunningBamFiles = getBamFileForMergingWorkPackage(
                    mergingWorkPackageNeedsProcessing, false, false
            )
            output.showList(HEADER_OLD_INSTANCE_RUNNING, alreadyRunningBamFiles)

            List<MergingWorkPackage> waiting = mergingWorkPackageNeedsProcessing - alreadyRunningBamFiles*.mergingWorkPackage
            output.showWaiting(waiting)

            List mergingWorkPackageNotNeedProcessing = mergingWorkPackagesByNeedsProcessing[false] ?: []

            List<AbstractMergedBamFile> bamFiles = getBamFileForMergingWorkPackage(mergingWorkPackageNotNeedProcessing, true, true)

            List<MergingWorkPackage> mergingWorkPackagesWithoutBamFile = mergingWorkPackageNotNeedProcessing - bamFiles*.mergingWorkPackage
            output.showList(HEADER_MWP_WITHOUT_BAM, mergingWorkPackagesWithoutBamFile)

            Map<Boolean, List<AbstractMergedBamFile>> bamFilesByWithdrawn = bamFiles.groupBy {
                it.withdrawn
            }
            output.showList(HEADER_MWP_WITH_WITHDRAWN_BAM, bamFilesByWithdrawn[true]*.mergingWorkPackage)

            List<AbstractMergedBamFile> notWithdrawnBamFiles = (bamFilesByWithdrawn[false] ?: []) + alreadyRunningBamFiles

            Map<AbstractMergedBamFile.FileOperationStatus, Collection<AbstractMergedBamFile>> bamFileByFileOperationStatus =
                    notWithdrawnBamFiles.groupBy { it.fileOperationStatus }

            output.showRunningWithHeader(
                    HEADER_RUNNING_DECLARED, workflowName, bamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.DECLARED]
            )
            output.showRunningWithHeader(
                    HEADER_RUNNING_NEEDS_PROCESSING, workflowName, bamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING]
            )
            output.showRunningWithHeader(
                    HEADER_RUNNING_IN_PROGRESS, workflowName, bamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.INPROGRESS]
            )

            output.showFinished(bamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.PROCESSED])

            return bamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.PROCESSED]
        }
        return []
    }

    List<SeqTrack> getSeqTracksWithoutCorrespondingAlignmentConfig(List<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return []
        }
        return SamplePair.executeQuery('''
                    select
                        seqTrack
                    from
                        SeqTrack seqTrack
                    where
                        seqTrack in (:seqTracks)
                        and not exists (
                            select
                                config
                            from
                                WorkflowVersionSelector config
                            where
                                config.project = seqTrack.sample.individual.project
                                and config.seqType = seqTrack.seqType
                                and config.workflowVersion.workflow = :workflow
                                and config.deprecationDate is null
                        )
                ''', [
                seqTracks: seqTracks,
                workflow : workflow,
        ])
    }

    Map mergingWorkPackageForSeqTracks(List<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return [
                    seqTracksWithoutMergingWorkpackage: [],
                    mergingWorkPackages               : [],
            ]
        }
        List list = MergingWorkPackage.executeQuery('''
                    select
                        mergingWorkPackage,
                        seqTrack
                    from
                        MergingWorkPackage mergingWorkPackage
                        join mergingWorkPackage.seqTracks seqTrack
                    where
                        seqTrack in (:seqTracks)
                        and mergingWorkPackage.seqType in (:seqTypes)
                ''', [
                seqTracks: seqTracks,
                seqTypes : seqTypes,
        ])

        List seqTracksWithoutMergingWorkpackage = seqTracks - list.collect {
            it[1]
        }

        List<MergingWorkPackage> mergingWorkPackages = list.collect {
            it[0]
        }.unique()

        return [
                seqTracksWithoutMergingWorkpackage: seqTracksWithoutMergingWorkpackage,
                mergingWorkPackages               : mergingWorkPackages,
        ]
    }

    List<AbstractMergedBamFile> getBamFileForMergingWorkPackage(List<MergingWorkPackage> mergingWorkPackages, boolean showFinished, boolean showWithdrawn) {
        if (!mergingWorkPackages) {
            return []
        }

        String filterFinished = showFinished ? '' :
                "and bamFile.fileOperationStatus != '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'"
        String filterWithdrawnFinished = showWithdrawn ? '' :
                "and bamFile.withdrawn = false"

        return AbstractMergedBamFile.executeQuery("""
                    select
                        bamFile
                    from
                        AbstractMergedBamFile bamFile
                    where
                        bamFile.workPackage in (:mergingWorkPackage)
                        ${filterFinished}
                        ${filterWithdrawnFinished}
                        and bamFile.workPackage.config.pipeline.type = '${Pipeline.Type.ALIGNMENT}'
                        and bamFile.workPackage.config.pipeline.name = :pipeLineName
                        and bamFile.id = (
                            select
                                max(bamFile1.id)
                            from
                                AbstractMergedBamFile bamFile1
                            where
                                bamFile1.workPackage = bamFile.workPackage
                        )
                """.toString(), [
                mergingWorkPackage: mergingWorkPackages,
                pipeLineName      : pipeLineName,
        ])
    }

    @Deprecated
    abstract String getWorkflowName()

    @Deprecated
    abstract Pipeline.Name getPipeLineName()

    abstract Workflow getWorkflow()

    Set<SeqType> getSeqTypes() {
        return workflow.supportedSeqTypes
    }

    /**
     * Subclass can override this method to do additional filtering
     */
    @SuppressWarnings("UnusedMethodParameter")
    List<SeqTrack> filter(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        return seqTracks
    }

    /**
     * Subclass can override this method to do additional filtering
     */
    @SuppressWarnings("UnusedMethodParameter")
    List<SeqTrack> filterWithoutReferenceGenome(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        return seqTracks
    }
}
