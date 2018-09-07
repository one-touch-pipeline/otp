package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import org.codehaus.groovy.runtime.MethodClosure
import spock.lang.*

class VariantCallingPipelinesCheckerSpec extends Specification {

    SamplePair createSamplePair(Map properties = [:]) {
        return DomainFactory.createSamplePairPanCan([
                mergingWorkPackage1: DomainFactory.createMergingWorkPackage([
                        seqType: SeqType.wholeGenomePairedSeqType,
                        pipeline: DomainFactory.createPanCanPipeline(),
                ])
        ] + properties)
    }

    /**
     * helper to generate sample pairs with only one finished variant calling instance.
     *
     * @return The finished variant calling analysis, with all new SamplePair behind it.
     */
    BamFilePairAnalysis createBpaWithSingleAnalysisFinished( String workflowProcessingStatus, MethodClosure createTestInstance) {
        BamFilePairAnalysis bpa = createTestInstance([
                processingState: AnalysisProcessingStates.FINISHED,
                samplePair     : createSamplePair([
                        (workflowProcessingStatus): SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
                ]),
        ])
        DomainFactory.createSampleTypePerProjectForBamFile(bpa.sampleType2BamFile, SampleType.Category.CONTROL)
        return bpa
    }

    void "handle, when no bamFiles given, then return empty list and create no output"() {
        given:
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        VariantCallingPipelinesChecker checker = new VariantCallingPipelinesChecker()

        when:
        List<SamplePair> result = checker.handle([], output)

        then:
        [] == result
        0 * output._
    }

    void "handle, if bamFiles given, then return SamplePairs with finished all analysis and create output for the others"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        MonitorOutputCollector output = Mock(MonitorOutputCollector)
        VariantCallingPipelinesChecker checker = new VariantCallingPipelinesChecker()

        // Workflow names for which we have Checkers, i.e. that the ProcessingProgressMonitor monitors.
        // use names as defined in JobExecutionPlanDSL
        List<String> workflowNames = [
                'RoddySnvWorkflow',
                'IndelWorkflow',
                'SophiaWorkflow',
                'ACEseqWorkflow',
                'RunYapsaWorkflow',
        ]

        // For each monitored workflow:
        //   - the relevant processingStatus field in SamplePair
        //   - the DomainFactory methods to create test instances for that workflow.
        // samplePair.workflowProcessingStatus fieldname : DomainFactory.&create${Workflow}InstanceWithRoddyBamFiles
        Map<String, Closure<BamFilePairAnalysis>> workflowStatusFieldsAndTheirTestInstanceCreator = [
                snvProcessingStatus:      DomainFactory.&createRoddySnvInstanceWithRoddyBamFiles,
                indelProcessingStatus:    DomainFactory.&createIndelCallingInstanceWithRoddyBamFiles,
                sophiaProcessingStatus:   DomainFactory.&createSophiaInstanceWithRoddyBamFiles,
                aceseqProcessingStatus:   DomainFactory.&createAceseqInstanceWithRoddyBamFiles,
                runYapsaProcessingStatus: DomainFactory.&createRunYapsaInstanceWithRoddyBamFiles,
        ]

        assert workflowNames.size() == workflowStatusFieldsAndTheirTestInstanceCreator.size() : "mismatch between workflow names and the factory-methods map"

        and: 'a sample pair with all analyses completed'
        SamplePair finishedSamplePair = createSamplePair(
                // for all workflows: set processingStatus field to "done/no-processing-needed"
                workflowStatusFieldsAndTheirTestInstanceCreator.keySet().collectEntries {
                    [ (it) : SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED ]
                }
        )
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(finishedSamplePair.mergingWorkPackage2, SampleType.Category.CONTROL)
        // 'run' all the workflows for our all-finished samplePair
        List<BamFilePairAnalysis> analysesOfAllAnalysisFinishedSamplePair = workflowStatusFieldsAndTheirTestInstanceCreator.values()
                .collect { Closure createTestInstance ->
                    createTestInstance([
                            samplePair     : finishedSamplePair,
                            processingState: AnalysisProcessingStates.FINISHED,
                    ])
                }

        and: 'sample pairs with each only one pipeline finished'
        List<BamFilePairAnalysis> analysesOfSingleAnalysisFinishedSamplePairs =
                workflowStatusFieldsAndTheirTestInstanceCreator.collect { String workflowProcessingStatus, MethodClosure createTestInstance ->
                    createBpaWithSingleAnalysisFinished(workflowProcessingStatus, createTestInstance)
                }

        and: 'all bam files have a processing threshold set'
        List<AbstractMergedBamFile> bamFiles = (
                analysesOfAllAnalysisFinishedSamplePair +
                analysesOfSingleAnalysisFinishedSamplePairs
        ).collect {
            [it.sampleType1BamFile, it.sampleType2BamFile]
        }.flatten().each {
            DomainFactory.createProcessingThresholdsForBamFile(it, [coverage: null, numberOfLanes: 1])
        }

        List<SamplePair> expectedSamplePairs = [ finishedSamplePair ]

        when:
        List<SamplePair> result = checker.handle(bamFiles, output)

        then:
        expectedSamplePairs == result
        1 * output.showWorkflow('Sample pairs', false)

        then: 'monitor should have asked each workflow for details'
        workflowNames.each { 1 * output.showWorkflow(it) }

        then: 'and no workflows should have been overlooked by this test'
        0 * output.showWorkflow(_)
        0 * output.showWorkflow(_, _)
    }

}
