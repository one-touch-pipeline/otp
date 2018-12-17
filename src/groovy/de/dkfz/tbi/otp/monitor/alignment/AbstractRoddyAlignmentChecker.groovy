package de.dkfz.tbi.otp.monitor.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.monitor.PipelinesChecker
import de.dkfz.tbi.otp.ngsdata.*

abstract class AbstractRoddyAlignmentChecker extends PipelinesChecker<SeqTrack> {

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
        seqTracks = seqTracks.unique()

        output.showWorkflow(getWorkflowName())

        List<SeqType> supportedSeqTypes = getSeqTypes()

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

            List<SeqTrack> noConfig = seqTracksWithoutCorrespondingRoddyAlignmentConfig(alignableSeqTracks)
            output.showUniqueList(HEADER_NO_CONFIG, noConfig, { "${it.project}  ${it.seqType}" })

            List<SeqTrack> seqTracksWithConfig = alignableSeqTracks - noConfig

            List<SeqTrack> filteredSeqTracks = filter(seqTracksWithConfig, output)

            Map mergingWorkPackageMap = mergingWorkPackageForSeqTracks(filteredSeqTracks)
            output.showList(HEADER_NO_MERGING_WORK_PACKAGE, mergingWorkPackageMap.seqTracksWithoutMergingWorkpackage)

            List<MergingWorkPackage> mergingWorkPackages = mergingWorkPackageMap.mergingWorkPackages ?: []

            Map<Boolean, Collection<MergingWorkPackage>> mergingWorkPackagesByNeedsProcessing =
                    mergingWorkPackages.groupBy {
                        it.needsProcessing
                    }

            List mergingWorkPackageNeedsProcessing = mergingWorkPackagesByNeedsProcessing[true] ?: []

            List<RoddyBamFile> alreadyRunningBamFiles = roddyBamFileForMergingWorkPackage(mergingWorkPackageNeedsProcessing, false, false)
            output.showList(HEADER_OLD_INSTANCE_RUNNING, alreadyRunningBamFiles)

            List<MergingWorkPackage> waiting = mergingWorkPackageNeedsProcessing - alreadyRunningBamFiles*.mergingWorkPackage
            output.showWaiting(waiting)

            List mergingWorkPackageNotNeedProcessing = mergingWorkPackagesByNeedsProcessing[false] ?: []

            List<RoddyBamFile> roddyBamFiles = roddyBamFileForMergingWorkPackage(mergingWorkPackageNotNeedProcessing, true, true)

            List<MergingWorkPackage> mergingWorkPackagesWithoutBamFile = mergingWorkPackageNotNeedProcessing - roddyBamFiles*.mergingWorkPackage
            output.showList(HEADER_MWP_WITHOUT_BAM, mergingWorkPackagesWithoutBamFile)

            Map<Boolean, RoddyBamFile> roddyBamFilesByWithdrawn = roddyBamFiles.groupBy {
                it.withdrawn
            }
            output.showList(HEADER_MWP_WITH_WITHDRAWN_BAM, roddyBamFilesByWithdrawn[true]*.mergingWorkPackage)

            List<RoddyBamFile> notWithdrawnRoddyBamFiles = (roddyBamFilesByWithdrawn[false] ?: []) + alreadyRunningBamFiles

            Map<AbstractMergedBamFile.FileOperationStatus, Collection<RoddyBamFile>> roddyBamFileByFileOperationStatus =
                    notWithdrawnRoddyBamFiles.groupBy { it.fileOperationStatus }

            String workflowName = getWorkflowName()
            output.showRunningWithHeader(HEADER_RUNNING_DECLARED, workflowName, roddyBamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.DECLARED])
            output.showRunningWithHeader(HEADER_RUNNING_NEEDS_PROCESSING, workflowName, roddyBamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING])
            output.showRunningWithHeader(HEADER_RUNNING_IN_PROGRESS, workflowName, roddyBamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.INPROGRESS])

            output.showFinished(roddyBamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.PROCESSED])

            return roddyBamFileByFileOperationStatus[AbstractMergedBamFile.FileOperationStatus.PROCESSED]
        }
        return []
    }


    List<SeqTrack> seqTracksWithoutCorrespondingRoddyAlignmentConfig(List<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return []
        }
        return SamplePair.executeQuery("""
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
                                ConfigPerProjectAndSeqType config
                            where
                                config.project = seqTrack.sample.individual.project
                                and config.seqType = seqTrack.seqType
                                and config.pipeline.type = '${Pipeline.Type.ALIGNMENT}'
                                and config.pipeline.name = :pipeLineName
                                and config.obsoleteDate is null
                        )
                """, [
                seqTracks   : seqTracks,
                pipeLineName: getPipeLineName(),
        ])
    }


    Map mergingWorkPackageForSeqTracks(List<SeqTrack> seqTracks) {
        if (!seqTracks) {
            return [
                    seqTracksWithoutMergingWorkpackage: [],
                    mergingWorkPackages               : [],
            ]
        }
        List list = MergingWorkPackage.executeQuery("""
                    select
                        mergingWorkPackage,
                        seqTrack
                    from
                        MergingWorkPackage mergingWorkPackage,
                        SeqTrack seqTrack
                    where
                        seqTrack in (:seqTracks)
                        and seqTrack.sample = mergingWorkPackage.sample
                        and seqTrack.seqType = mergingWorkPackage.seqType
                        and mergingWorkPackage.pipeline.type = '${Pipeline.Type.ALIGNMENT}'
                        and mergingWorkPackage.pipeline.name = :pipeLineName
                        and (
                            seqTrack.libraryPreparationKit = mergingWorkPackage.libraryPreparationKit
                            or (
                                seqTrack.libraryPreparationKit is null
                                and mergingWorkPackage.libraryPreparationKit is null
                            )
                            or seqTrack.seqType in (:seqTypesCanHaveDifferentLibraryPreperationKit)
                        )
                """, [
                seqTracks                                    : seqTracks,
                pipeLineName                                 : getPipeLineName(),
                seqTypesCanHaveDifferentLibraryPreperationKit: SeqTypeService.getSeqTypesIgnoringLibraryPreparationKitForMerging(),
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


    List<RoddyBamFile> roddyBamFileForMergingWorkPackage(List<MergingWorkPackage> mergingWorkPackages, boolean showFinished, boolean showWithdrawn) {
        if (!mergingWorkPackages) {
            return []
        }

        String filterFinished = showFinished ? '' :
                "and roddyBamFile.fileOperationStatus != '${AbstractMergedBamFile.FileOperationStatus.PROCESSED}'"
        String filterWithdrawnFinished = showWithdrawn ? '' :
                "and roddyBamFile.withdrawn = false"

        return RoddyBamFile.executeQuery("""
                    select
                        roddyBamFile
                    from
                        RoddyBamFile roddyBamFile
                    where
                        roddyBamFile.workPackage in (:mergingWorkPackage)
                        ${filterFinished}
                        ${filterWithdrawnFinished}
                        and roddyBamFile.config.pipeline.type = '${Pipeline.Type.ALIGNMENT}'
                        and config.pipeline.name = :pipeLineName
                        and roddyBamFile.id = (
                            select
                                max(bamFile.id)
                            from
                                RoddyBamFile bamFile
                            where
                                bamFile.workPackage = roddyBamFile.workPackage
                        )
                """, [
                mergingWorkPackage: mergingWorkPackages,
                pipeLineName      : getPipeLineName(),
        ])
    }


    abstract String getWorkflowName()

    abstract Pipeline.Name getPipeLineName()

    abstract List<SeqType> getSeqTypes()

    /**
     * Subclass can override this method to do additional filtering
     */
    @SuppressWarnings("UnusedMethodParameter")
    List<SeqTrack> filter(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        return seqTracks
    }

}
