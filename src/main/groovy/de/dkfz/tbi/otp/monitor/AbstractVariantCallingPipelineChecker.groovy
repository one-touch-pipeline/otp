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
package de.dkfz.tbi.otp.monitor

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils

abstract class AbstractVariantCallingPipelineChecker extends PipelinesChecker<SamplePair> {


    static final String HEADER_NO_CONFIG = 'For the following project seqtype combination no config is defined'
    static final String HEADER_DISABLED_SAMPLE_PAIR = 'The following samplePairs are disabled for processing'
    static final String HEADER_TOO_LITTLE_COVERAGE_FOR_ANALYSIS = 'The following samplePairs have not enough coverage to run this analysis'
    static final String HEADER_OLD_INSTANCE_RUNNING = 'old instance running'
    static final String HEADER_WITHDRAWN_ANALYSIS_RUNNING = 'The following Analysis are withdrawn and running'
    static final String HEADER_WITHDRAWN_ANALYSIS_FINISHED = 'The following Analysis are withdrawn and finished'


    static final String PROBLEMS_NO_BAM_FILE = "bam file does not exist"
    static final String PROBLEMS_MISSING_SEQ_TRACKS = "bam file misses the following seqtracks"
    static final String PROBLEMS_UNEXPECTED_SEQ_TRACKS = "bam file contains unexpected seqtracks"

    @Override
    List handle(List<SamplePair> samplePairsInput, MonitorOutputCollector output) {
        if (!samplePairsInput) {
            return []
        }
        List<SamplePair> samplePairs = samplePairsInput

        output.showWorkflow(getWorkflowName())

        List<SeqType> supportedSeqTypes = getSeqTypes()

        Map samplePairSupportedSeqType = samplePairs.groupBy {
            supportedSeqTypes.contains(it.seqType)
        }

        if (samplePairSupportedSeqType[false]) {
            output.showUniqueNotSupportedSeqTypes(samplePairSupportedSeqType[false], { SamplePair samplePair ->
                "${samplePair.seqType.displayNameWithLibraryLayout}"
            })
        }

        List<SamplePair> samplePairsWithSupportedSeqTypes = samplePairSupportedSeqType[true] ?: []

        List<SamplePair> noConfig = samplePairWithoutCorrespondingConfigForPipelineAndSeqTypeAndProject(samplePairsWithSupportedSeqTypes)
        output.showUniqueList(HEADER_NO_CONFIG, noConfig, { SamplePair samplePair ->
            "${samplePair.project} ${samplePair.seqType.name} ${samplePair.seqType.libraryLayout}"
        })

        List<SamplePair> samplePairsWithConfig = samplePairsWithSupportedSeqTypes - noConfig

        Map processingStateMap = samplePairsWithConfig.groupBy {
            it[getProcessingStateMember()]
        }

        output.showList(HEADER_DISABLED_SAMPLE_PAIR, processingStateMap[SamplePair.ProcessingStatus.DISABLED])

        List needsProcessing = processingStateMap[SamplePair.ProcessingStatus.NEEDS_PROCESSING]
        if (needsProcessing) {
            List<BamFilePairAnalysis> alreadyRunning = analysisAlreadyRunningForSamplePairAndPipeline(needsProcessing)
            output.showRunningWithHeader(HEADER_OLD_INSTANCE_RUNNING, getWorkflowName(), alreadyRunning)

            List<SamplePair> waiting = needsProcessing - alreadyRunning*.samplePair

            List<ToLittleCoverageSamplePair> toLittleCoverageSamplePairs = toLittleCoverageForAnalysis(waiting)
            output.showList(HEADER_TOO_LITTLE_COVERAGE_FOR_ANALYSIS, toLittleCoverageSamplePairs)

            waiting = waiting - toLittleCoverageSamplePairs*.samplePair
            output.showWaiting(waiting, displayWaitingWithInfos)
        }

        List noProcessingNeeded = processingStateMap[SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED]
        if (noProcessingNeeded) {
            List notTriggered = samplePairsWithoutAnalysis(noProcessingNeeded)
            output.showNotTriggered(notTriggered)

            List<BamFilePairAnalysis> analysis = lastAnalysisForSamplePair(noProcessingNeeded)

            Map<AnalysisProcessingStates, Map<Boolean, BamFilePairAnalysis>> analysisStateMap = analysis.groupBy(
                    [
                            { BamFilePairAnalysis it ->
                                it.processingState
                            },
                            { BamFilePairAnalysis it ->
                                it.isWithdrawn()
                            },
                    ]
            )

            output.showList(HEADER_WITHDRAWN_ANALYSIS_RUNNING, analysisStateMap[AnalysisProcessingStates.IN_PROGRESS]?.get(true))

            displayRunning(analysisStateMap[AnalysisProcessingStates.IN_PROGRESS]?.get(false), output)

            output.showList(HEADER_WITHDRAWN_ANALYSIS_FINISHED, analysisStateMap[AnalysisProcessingStates.FINISHED]?.get(true))

            output.showFinished(analysisStateMap[AnalysisProcessingStates.FINISHED]?.get(false))
            return analysisStateMap[AnalysisProcessingStates.FINISHED]?.get(false) ?: []
        }
        return []
    }

    List<SamplePair> samplePairWithoutCorrespondingConfigForPipelineAndSeqTypeAndProject(List<SamplePair> samplePairs) {
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
                                config
                            from
                                ConfigPerProjectAndSeqType config
                            where
                                config.project = samplePair.mergingWorkPackage1.sample.individual.project
                                and config.seqType = samplePair.mergingWorkPackage1.seqType
                                and config.pipeline.type = :pipelineType
                                and config.obsoleteDate is null
                        )
                """, [
                samplePairs : samplePairs,
                pipelineType: getPipeline().type,
        ])
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
                """.toString(), [
                samplePairs : samplePairs,
                pipelineType: getPipeline().type,
        ])
    }

    List<ToLittleCoverageSamplePair> toLittleCoverageForAnalysis(List<SamplePair> samplePairs) {
        if (!samplePairs) {
            return []
        }
        Double minCoverage = ProcessingOptionService.findOptionSafe(ProcessingOption.OptionName.PIPELINE_MIN_COVERAGE, pipeline.type.toString(), null) as Double
        if (minCoverage == null) {
            return []
        }

        def connectBamFile = { String number ->
            return """(
                        bamFile${number}.workPackage = samplePair.mergingWorkPackage${number}
                        and bamFile${number}.id = (
                            select
                                max(bamFile.id)
                            from
                                AbstractMergedBamFile bamFile
                            where
                                bamFile.workPackage = bamFile${number}.workPackage
                        )
                    )""".toString()
        }

        return SamplePair.executeQuery("""
                select
                    new ${ToLittleCoverageSamplePair.class.getName()} (
                        samplePair,
                        bamFile1.coverage,
                        bamFile2.coverage,
                        ${minCoverage}
                    )
                from
                    SamplePair samplePair,
                    AbstractMergedBamFile bamFile1,
                    AbstractMergedBamFile bamFile2
                where
                    samplePair in (:samplePair)
                    and ${connectBamFile('1')}
                    and ${connectBamFile('2')}
                    and (
                        bamFile1.coverage < ${minCoverage}
                        or bamFile2.coverage < ${minCoverage}
                    )
            """.toString(), [
                samplePair: samplePairs,
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
                """.toString(), [
                samplePairs : samplePairs,
                pipelineType: getPipeline().type,
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
                        and analysis.config.pipeline.type = :pipelineType
                """.toString(), [
                samplePairs : samplePairs,
                pipelineType: getPipeline().type,
        ])
    }


    abstract String getWorkflowName()

    abstract String getProcessingStateMember()

    abstract Pipeline getPipeline()

    List<SeqType> getSeqTypes() {
        return pipeline.getSeqTypes()
    }

    abstract Class<? extends BamFilePairAnalysis> getBamFilePairAnalysisClass()

    void displayRunning(List<BamFilePairAnalysis> bamFilePairAnalysis, MonitorOutputCollector output) {
        output.showRunning(getWorkflowName(), bamFilePairAnalysis)
    }

    private Closure displayWaitingWithInfos = { SamplePair samplePair ->
        List<String> ret = []
        [
                disease: samplePair.mergingWorkPackage1,
                control: samplePair.mergingWorkPackage2,
        ].each { String key, AbstractMergingWorkPackage mergingWorkPackage ->
            AbstractMergedBamFile bamFile = AbstractMergedBamFile.findByWorkPackage(mergingWorkPackage, [sort: 'id', order: 'desc'])
            if (bamFile == null) {
                ret << "${key} ${PROBLEMS_NO_BAM_FILE}"
            } else if (!(bamFile instanceof ExternallyProcessedMergedBamFile)) {
                Set<SeqTrack> containedSeqTracks = bamFile.getContainedSeqTracks()
                Set<SeqTrack> availableSeqTracks = bamFile.workPackage.seqTracks
                if (!CollectionUtils.containSame(containedSeqTracks*.id, availableSeqTracks*.id)) {
                    Set<SeqTrack> missingSeqTracks = availableSeqTracks.findAll {
                        !containedSeqTracks*.id.contains(it.id)
                    }
                    Set<SeqTrack> additionalSeqTrack = containedSeqTracks.findAll {
                        !availableSeqTracks*.id.contains(it.id)
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


    @TupleConstructor
    @EqualsAndHashCode
    static class ToLittleCoverageSamplePair {
        SamplePair samplePair
        Double coverageBamFile1
        Double coverageBamFile2
        Double coverage

        @Override
        String toString() {
            List<String> reasonsForBlocking = []
            [
                    disease: coverageBamFile1,
                    control: coverageBamFile2,
            ].each { String key, Double coverageBamFile ->
                if (coverageBamFile < coverage) {
                    reasonsForBlocking << "${key} ${coverageBamFile} of ${coverage}"
                }
            }
            return "${samplePair} (${reasonsForBlocking.join(', ')})"
        }
    }
}
