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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.codehaus.groovy.runtime.MethodClosure
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*

@Rollback
@Integration
class VariantCallingPipelinesCheckerIntegrationSpec extends Specification {

    SamplePair createSamplePair(Map properties = [:]) {
        return DomainFactory.createSamplePairPanCan([
                mergingWorkPackage1: DomainFactory.createMergingWorkPackage([
                        seqType: SeqTypeService.wholeGenomePairedSeqType,
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
        DomainFactory.createSampleTypePerProjectForBamFile(bpa.sampleType2BamFile, SampleTypePerProject.Category.CONTROL)
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
        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(finishedSamplePair.mergingWorkPackage2, SampleTypePerProject.Category.CONTROL)
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
