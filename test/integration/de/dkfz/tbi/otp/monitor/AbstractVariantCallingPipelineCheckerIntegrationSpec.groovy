package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

abstract class AbstractVariantCallingPipelineCheckerIntegrationSpec extends Specification {


    abstract AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker()

    abstract Pipeline createPipeLine()

    abstract BamFilePairAnalysis createAnalysis(Map properties)

    BamFilePairAnalysis createAnalysis() {
        createAnalysis([:])
    }

    ConfigPerProject createConfig(SamplePair samplePair, Map properties = [:]) {
        DomainFactory.createRoddyWorkflowConfig([
                pipeline: createPipeLine(),
                seqType : samplePair.seqType,
                project : samplePair.project,
        ] + properties)
    }

    //for checking with data of other PipeLine
    abstract Pipeline createPipeLineForCrosschecking()

    //for checking with analysis of other PipeLine
    abstract BamFilePairAnalysis createAnalysisForCrosschecking(Map properties)

    BamFilePairAnalysis createAnalysisForCrosschecking() {
        createAnalysisForCrosschecking([:])
    }

    SamplePair createSamplePair(Map properties = [:]) {
        return DomainFactory.createSamplePairPanCan([
                mergingWorkPackage1: DomainFactory.createMergingWorkPackage([
                        seqType: SeqType.wholeGenomePairedSeqType,
                        pipeline: DomainFactory.createPanCanPipeline(),
                ])
        ] + properties)
    }


    void "samplePairWithoutCorrespondingConfigForPipelineAndSeqTypeAndProject, when some sample pairs have a config and some not some not, return project and seqtype of sample pairs without config"() {
        given:
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        SamplePair samplePair1 = DomainFactory.createSamplePair()

        SamplePair samplePair2 = DomainFactory.createSamplePair()
        createConfig(samplePair2)

        SamplePair samplePair3 = DomainFactory.createSamplePair()
        createConfig(samplePair3, [project: DomainFactory.createProject()])

        SamplePair samplePair4 = DomainFactory.createSamplePair()
        createConfig(samplePair3, [seqType: DomainFactory.createSeqType()])

        SamplePair samplePair5 = DomainFactory.createSamplePair()
        createConfig(samplePair5)

        SamplePair samplePair6 = DomainFactory.createSamplePair()
        createConfig(samplePair6, [
                obsoleteDate: new Date(),
        ])

        SamplePair samplePair7 = DomainFactory.createSamplePair()
        DomainFactory.createRoddyWorkflowConfig([
                pipeline: createPipeLineForCrosschecking(),
                seqType : samplePair7.seqType,
                project : samplePair7.project,
        ])

        List<SamplePair> samplePairs = [
                samplePair1,
                samplePair2,
                samplePair3,
                samplePair4,
                samplePair5,
                samplePair6,
                samplePair7,
        ]

        List<String> expected = [
                samplePair1,
                samplePair3,
                samplePair4,
                samplePair6,
                samplePair7,
        ]

        when:
        List returnValue = pipelineChecker.samplePairWithoutCorrespondingConfigForPipelineAndSeqTypeAndProject(samplePairs)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }


    void "analysisAlreadyRunningForSamplePairAndPipeline, when some sample pairs have running analysis and some not, return the running analysis"() {
        given:
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        BamFilePairAnalysis analysis1 = createAnalysis([processingState: AnalysisProcessingStates.FINISHED])
        BamFilePairAnalysis analysis2 = createAnalysis([processingState: AnalysisProcessingStates.IN_PROGRESS])
        analysis2.withdrawn = true
        analysis2.save(flush: true)
        BamFilePairAnalysis analysis3 = createAnalysis([processingState: AnalysisProcessingStates.FINISHED])
        analysis3.withdrawn = true
        analysis3.save(flush: true)
        BamFilePairAnalysis analysis4 = createAnalysis([processingState: AnalysisProcessingStates.IN_PROGRESS])

        BamFilePairAnalysis analysis5 = createAnalysisForCrosschecking([processingState: AnalysisProcessingStates.IN_PROGRESS])

        SamplePair samplePair = DomainFactory.createSamplePair()

        List<SamplePair> samplePairs = [
                samplePair,
                analysis1.samplePair,
                analysis2.samplePair,
                analysis3.samplePair,
                analysis4.samplePair,
                analysis5.samplePair,
        ]
        List<BamFilePairAnalysis> expected = [
                analysis4,
        ]

        when:
        List returnValue = pipelineChecker.analysisAlreadyRunningForSamplePairAndPipeline(samplePairs)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }


    void "samplePairsWithoutAnalysis, when some sample pairs are triggered for analysis and some not, return the not triggered SamplePairs"() {
        given:
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        SamplePair samplePair = DomainFactory.createSamplePair()

        BamFilePairAnalysis analysis1 = createAnalysis()
        BamFilePairAnalysis analysis2 = createAnalysisForCrosschecking()

        List<SamplePair> samplePairs = [
                samplePair,
                analysis1.samplePair,
                analysis2.samplePair,
        ]

        List<SamplePair> expected = [
                samplePair,
                analysis2.samplePair,
        ]

        when:
        List returnValue = pipelineChecker.samplePairsWithoutAnalysis(samplePairs)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }


    void "lastAnalysisForSamplePair, when some sample pairs have multiple analysis, return the last analysis of every SamplePair"() {
        given:
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        SamplePair samplePair = DomainFactory.createSamplePair()
        BamFilePairAnalysis oneAnalysisForSamplePair = createAnalysis()
        BamFilePairAnalysis firstAnalysisSamplePairWithTwoAnalysis = createAnalysis()
        BamFilePairAnalysis secondAnalysisSamplePairWithTwoAnalysis = createAnalysis([samplePair: firstAnalysisSamplePairWithTwoAnalysis.samplePair])
        BamFilePairAnalysis crossCheckingAnalysis = createAnalysisForCrosschecking()
        BamFilePairAnalysis withdrawnAnalysis = createAnalysis()
        withdrawnAnalysis.withdrawn = true //because of constraint in SnvCallingInstance it can not be given in the parameter map
        withdrawnAnalysis.save(flush: true)

        List<SamplePair> samplePairs = [
                samplePair,
                oneAnalysisForSamplePair.samplePair,
                firstAnalysisSamplePairWithTwoAnalysis.samplePair,
                secondAnalysisSamplePairWithTwoAnalysis.samplePair,
                crossCheckingAnalysis.samplePair,
                withdrawnAnalysis.samplePair,
        ]

        List<BamFilePairAnalysis> expected = [
                oneAnalysisForSamplePair,
                secondAnalysisSamplePairWithTwoAnalysis,
                withdrawnAnalysis,
        ]

        when:
        List returnValue = pipelineChecker.lastAnalysisForSamplePair(samplePairs)

        then:
        TestCase.assertContainSame(expected, returnValue)
    }


    void "handle, if no samplePairs given, do nothing"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        when:
        pipelineChecker.handle([], output)

        then:
        0 * output._
    }


    void "displayRunning, check that running is called with expected parameters"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AbstractVariantCallingPipelineChecker pipelineChecker = createVariantCallingPipelineChecker()

        SamplePair samplePair = DomainFactory.createSamplePairPanCan([
                (pipelineChecker.getProcessingStateMember()): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        BamFilePairAnalysis runningAnalysis = createAnalysis([
                samplePair: samplePair,
        ])

        createExpectedCall(pipelineChecker.getWorkflowName(), output, runningAnalysis)

        when:
        pipelineChecker.displayRunning([runningAnalysis], output)

        then:
        true
    }

    protected void createExpectedCall(String workFlowName, MonitorOutputCollector output, BamFilePairAnalysis runningAnalysis) {
        1 * output.showRunning(workFlowName, [runningAnalysis])
        0 * output.showRunning(_, _)
    }


    void "handle, if samplePairs given, then return analysis and create output for the others"() {
        given:
        DomainFactory.createAllAnalysableSeqTypes()
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        AbstractVariantCallingPipelineChecker pipelineChecker = Spy(createVariantCallingPipelineChecker().class)

        String processingStateMember = pipelineChecker.getProcessingStateMember()

        //sample pair with needs processing but no config
        SamplePair samplePairWithUnsupportedSeqType = createSamplePair(
                mergingWorkPackage1: DomainFactory.createMergingWorkPackage([
                        seqType: DomainFactory.createSeqType(),
                        pipeline: DomainFactory.createPanCanPipeline(),
                ])
        )

        //sample pair with needs processing but no config
        SamplePair samplePairWithoutConfig = createSamplePair()

        //disabled sample pair
        SamplePair samplePairDisabled = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.DISABLED,
        ])
        createConfig(samplePairDisabled)

        //sample pair with needs processing and no config and running instance
        SamplePair samplePairWithOldRunningInstance = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.NEEDS_PROCESSING,
        ])
        BamFilePairAnalysis oldRunningInstance = createAnalysis([
                samplePair: samplePairWithOldRunningInstance,
        ])

        //sample pair waiting for start
        SamplePair samplePairWaitingForStart = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.NEEDS_PROCESSING,
        ])
        createConfig(samplePairWaitingForStart)

        //sample pair with no processing needed and no instance
        SamplePair samplePairNotTriggered = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        createConfig(samplePairNotTriggered)

        //sample pair with no processing needed and withdrawn analysis
        SamplePair samplePairWithWithdrawnRunningAnalysis = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        BamFilePairAnalysis runningWithdrawnAnalysis = createAnalysis([
                samplePair: samplePairWithWithdrawnRunningAnalysis,
        ])
        runningWithdrawnAnalysis.withdrawn = true
        runningWithdrawnAnalysis.save(flush: true)

        //sample pair with no processing needed and analysis running
        SamplePair samplePairWithRunningAnalysis = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        BamFilePairAnalysis runningAnalysis = createAnalysis([
                samplePair: samplePairWithRunningAnalysis,
        ])

        //sample pair with no processing needed and withrawn analysis finished
        SamplePair samplePairWithFinishedWithdrawnAnalysis = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        BamFilePairAnalysis finishedWithdrawnAnalysis = createAnalysis([
                samplePair     : samplePairWithFinishedWithdrawnAnalysis,
                processingState: AnalysisProcessingStates.FINISHED,
        ])
        finishedWithdrawnAnalysis.withdrawn = true
        finishedWithdrawnAnalysis.save(flush: true)

        //sample pair with no processing needed and analysis finished
        SamplePair samplePairWithFinishedAnalysis = createSamplePair([
                (processingStateMember): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED,
        ])
        BamFilePairAnalysis finishedAnalysis = createAnalysis([
                samplePair     : samplePairWithFinishedAnalysis,
                processingState: AnalysisProcessingStates.FINISHED,
        ])

        List<SamplePair> samplePairs = [
                samplePairDisabled,
                samplePairWithUnsupportedSeqType,
                samplePairWithoutConfig,
                samplePairWithOldRunningInstance,
                samplePairWaitingForStart,
                samplePairNotTriggered,
                samplePairWithWithdrawnRunningAnalysis,
                samplePairWithRunningAnalysis,
                samplePairWithFinishedWithdrawnAnalysis,
                samplePairWithFinishedAnalysis,
        ]
        List<SamplePair> finishedSamplePairs = [
                finishedAnalysis,
        ]

        when:
        List<SamplePair> result = pipelineChecker.handle(samplePairs, output)

        then:
        1 * output.showWorkflow(_)

        finishedSamplePairs == result

        then:
        1 * output.showUniqueNotSupportedSeqTypes([samplePairWithUnsupportedSeqType], _)

        then:
        1 * pipelineChecker.samplePairWithoutCorrespondingConfigForPipelineAndSeqTypeAndProject(_)
        1 * output.showUniqueList(AbstractVariantCallingPipelineChecker.HEADER_NO_CONFIG, [samplePairWithoutConfig], _)

        then:
        1 * output.showList(AbstractVariantCallingPipelineChecker.HEADER_DISABLED_SAMPLE_PAIR, [samplePairDisabled])

        then:
        1 * pipelineChecker.analysisAlreadyRunningForSamplePairAndPipeline(_)
        1 * output.showRunningWithHeader(AbstractVariantCallingPipelineChecker.HEADER_OLD_INSTANCE_RUNNING, _, [oldRunningInstance])

        then:
        1 * output.showWaiting([samplePairWaitingForStart], _)

        then:
        1 * pipelineChecker.samplePairsWithoutAnalysis(_)
        1 * output.showNotTriggered([samplePairNotTriggered])

        then:
        1 * pipelineChecker.lastAnalysisForSamplePair(_)
        1 * output.showList(AbstractVariantCallingPipelineChecker.HEADER_WITHDRAWN_ANALYSIS_RUNNING, [runningWithdrawnAnalysis])

        then:
        1 * pipelineChecker.displayRunning([runningAnalysis], output)
        (1.._) * output.showRunning(_, _)

        then:
        1 * output.showList(AbstractVariantCallingPipelineChecker.HEADER_WITHDRAWN_ANALYSIS_FINISHED, [finishedWithdrawnAnalysis])

        then:
        1 * output.showFinished([finishedAnalysis])

        0 * output._
    }

}
