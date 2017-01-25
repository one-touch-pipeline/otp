package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

abstract class AbstractVariantCallingPipelineChecker extends PipelinesChecker<SamplePair> {


    static final String HEADER_DISABLED_SAMPLE_PAIR = 'The following samplePairs are disabled for processing'
    static final String HEADER_NO_CONFIG = 'For the following project seqtype combination no config is defined'
    static final String HEADER_OLD_INSTANCE_RUNNING = 'old instance running'

    static final String PROBLEMS_NO_BAM_FILE = "bam file does not exist"
    static final String PROBLEMS_MISSING_SEQ_TRACKS = "bam file misses the following seqtracks"
    static final String PROBLEMS_UNEXPECTED_SEQ_TRACKS = "bam file contains unexpected  seqtracks"


    List handle(List<SamplePair> samplePairs, MonitorOutputCollector output) {
        if (!samplePairs) {
            return []
        }
        samplePairs = samplePairs.unique()

        output.showWorkflow(getWorkflowName())

        Map processingStateMap = samplePairs.groupBy {
            it[getProcessingStateMember()]
        }

        output.showList(HEADER_DISABLED_SAMPLE_PAIR, processingStateMap[SamplePair.ProcessingStatus.DISABLED])

        List needsProcessing = processingStateMap[SamplePair.ProcessingStatus.NEEDS_PROCESSING]
        if (needsProcessing) {
            List<String> noConfig = samplePairWithoutCorrespondingConfigForPipelineAndSeqTypeAndProject(needsProcessing)
            output.showUniqueList(HEADER_NO_CONFIG, noConfig)

            List<BamFilePairAnalysis> alreadyRunning = analysisAlreadyRunningForSamplePairAndPipeline(needsProcessing)
            output.showList(HEADER_OLD_INSTANCE_RUNNING, alreadyRunning)

            List<SamplePair> waiting = samplePairsWithConfigAndWithoutRunningAnalysis(needsProcessing)
            output.showWaiting(waiting, displayWaitingWithInfos)
        }

        List noProcessingNeeded = processingStateMap[SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED]
        if (noProcessingNeeded) {
            List notTriggered = samplePairsWithoutAnalysis(noProcessingNeeded)
            output.showNotTriggered(notTriggered)

            List<BamFilePairAnalysis> analysis = lastAnalysisForSamplePair(noProcessingNeeded)

            Map analysisStateMap = analysis.groupBy {
                it.processingState
            }

            displayRunning(analysisStateMap[AnalysisProcessingStates.IN_PROGRESS], output)

            output.showFinished(analysisStateMap[AnalysisProcessingStates.FINISHED])
            return analysisStateMap[AnalysisProcessingStates.FINISHED] ?: []
        }
        return []
    }

    List<String> samplePairWithoutCorrespondingConfigForPipelineAndSeqTypeAndProject(List<SamplePair> samplePairs) {
        if (!samplePairs) {
            return []
        }
        return SamplePair.executeQuery("""
                    select
                        samplePair.mergingWorkPackage1.sample.individual.project.name,
                        samplePair.mergingWorkPackage1.seqType.name
                    from
                        SamplePair samplePair
                    where
                        samplePair in (:samplePairs)
                        and not exists (
                            select
                                config
                            from
                                ConfigPerProject config
                            where
                                config.project = samplePair.mergingWorkPackage1.sample.individual.project
                                and config.seqType = samplePair.mergingWorkPackage1.seqType
                                and config.pipeline.type = :pipelineType
                                and config.obsoleteDate is null
                        )
                """, [
                samplePairs : samplePairs,
                pipelineType: getPipelineType(),
        ]).collect {
            it.join(' ')
        }
    }

    List<BamFilePairAnalysis> analysisAlreadyRunningForSamplePairAndPipeline(List<SamplePair> samplePairs) {
        if (!samplePairs) {
            return []
        }
        return SamplePair.executeQuery("""
                    select
                        analysis
                    from
                        ${getBamFilePairAnalysisClass().simpleName} analysis
                    where
                        analysis.samplePair in (:samplePairs)
                        and analysis.processingState = '${AnalysisProcessingStates.IN_PROGRESS}'
                        and analysis.withdrawn = false
                        and analysis.config.pipeline.type = :pipelineType
                """, [
                samplePairs : samplePairs,
                pipelineType: getPipelineType(),
        ])
    }

    List<SamplePair> samplePairsWithConfigAndWithoutRunningAnalysis(List<SamplePair> samplePairs) {
        if (!samplePairs) {
            return []
        }
        return SamplePair.executeQuery("""
                    select
                        samplePair
                    from
                        SamplePair samplePair
                    where
                        samplePair in (:samplePairs)
                        and exists (
                            select
                                config
                            from
                                ConfigPerProject config
                            where
                                config.project = samplePair.mergingWorkPackage1.sample.individual.project
                                and config.seqType = samplePair.mergingWorkPackage1.seqType
                                and config.pipeline.type = :pipelineType
                                and config.obsoleteDate is null
                        ) and not exists (
                            select
                                analysis
                            from
                                ${getBamFilePairAnalysisClass().simpleName} analysis
                            where
                                analysis.samplePair = samplePair
                                and analysis.processingState = '${AnalysisProcessingStates.IN_PROGRESS}'
                                and analysis.withdrawn = false
                                and analysis.config.pipeline.type = :pipelineType
                        )
                """, [
                samplePairs : samplePairs,
                pipelineType: getPipelineType(),
        ])
    }

    List<SamplePair> samplePairsWithoutAnalysis(List<SamplePair> samplePairs) {
        if (!samplePairs) {
            return []
        }
        return SamplePair.executeQuery("""
                    select
                        samplePair
                    from
                        SamplePair samplePair
                    where
                        samplePair in (:samplePairs)
                        and not exists (
                            select
                                analysis
                            from
                                ${getBamFilePairAnalysisClass().simpleName} analysis
                            where
                                analysis.samplePair = samplePair
                                and analysis.config.pipeline.type = :pipelineType
                        )
                """, [
                samplePairs : samplePairs,
                pipelineType: getPipelineType(),
        ])
    }

    List<BamFilePairAnalysis> lastAnalysisForSamplePair(List<SamplePair> samplePairs) {
        if (!samplePairs) {
            return []
        }
        return SamplePair.executeQuery("""
                    select
                        analysis
                    from
                        ${getBamFilePairAnalysisClass().simpleName} analysis
                    where
                        analysis.id in (
                            select
                                max(analysis.id)
                            from
                                ${getBamFilePairAnalysisClass().simpleName} analysis
                            where
                                analysis.samplePair in (:samplePairs)
                                and analysis.config.pipeline.type = :pipelineType
                            group by
                                analysis.samplePair.id
                        )
                        and analysis.withdrawn = false
                        and analysis.config.pipeline.type = :pipelineType
                """, [
                samplePairs : samplePairs,
                pipelineType: getPipelineType(),
        ])
    }


    abstract String getWorkflowName()

    abstract String getProcessingStateMember()

    abstract Pipeline.Type getPipelineType()

    abstract Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass()

    void displayRunning(List<BamFilePairAnalysis> bamFilePairAnalysis, MonitorOutputCollector output) {
        output.showRunning(getWorkflowName(), bamFilePairAnalysis)
    }

    private Closure displayWaitingWithInfos = { SamplePair samplePair ->
        List<String> ret = []
        [
                disease: samplePair.mergingWorkPackage1,
                control: samplePair.mergingWorkPackage2,
        ].each { String key, MergingWorkPackage mergingWorkPackage ->
            AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(mergingWorkPackage, [sort: 'id', order: 'desc'])
            if (bamFile == null) {
                ret << "${key} ${PROBLEMS_NO_BAM_FILE}"
            } else {
                Set<SeqTrack> containedSeqTracks = bamFile.getContainedSeqTracks()
                Set<SeqTrack> availableSeqTracks = bamFile.workPackage.findMergeableSeqTracks()
                if (!CollectionUtils.containSame(containedSeqTracks*.id, availableSeqTracks*.id)) {
                    Set<SeqTrack> missingSeqTracks = availableSeqTracks.findAll {
                        containedSeqTracks*.id.contains(it.id)
                    }
                    Set<SeqTrack> additionalSeqTrack = containedSeqTracks.findAll {
                        availableSeqTracks*.id.contains(it.id)
                    }
                    if (missingSeqTracks) {
                        ret << "${key} ${PROBLEMS_MISSING_SEQ_TRACKS}: ${missingSeqTracks.collect { "<${it.run} ${it.laneId}>" }.join('; ')}"
                    }
                    if (additionalSeqTrack) {
                        ret << "${key} ${PROBLEMS_UNEXPECTED_SEQ_TRACKS}: ${additionalSeqTrack.collect { "<${it.run} ${it.laneId}>" }.join('; ')}"
                    }
                }
            }
        }
        return "${samplePair} ${ret ? " (${ret.join(', ')})" : ''}"
    }
}
