/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.decider.analysis

import groovy.transform.TupleConstructor
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaWorkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.*
import de.dkfz.tbi.otp.workflow.analysis.runyapsa.RunYapsaWorkflow
import de.dkfz.tbi.otp.workflowExecution.AnalysisArtefactService
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

class RunYapsaDeciderSpec extends AbstractAnalysisDeciderSpec<RunYapsaInstance> {

    @Override
    Class[] getDomainClassesToMock() {
        return super.domainClassesToMock + [
                RunYapsaInstance,
                RoddySnvCallingInstance,
                IndelCallingInstance,
        ]
    }

    void setup() {
        decider = new RunYapsaDecider([
                runYapsaWorkFileService: Mock(RunYapsaWorkFileService) {
                    0 * _
                    _ * constructInstanceName(_) >> "instance"
                },
        ])
    }

    @Override
    void setupDataForCreateWorkflowRunsAndOutputArtefacts(CreatePairVariant variant, Map<String, ?> adaptation) {
        super.setupDataForCreateWorkflowRunsAndOutputArtefacts(variant, adaptation)
        SamplePair samplePair = factory.createSamplePair([
                mergingWorkPackage1: bamFileDisease.workPackage,
                mergingWorkPackage2: bamFileControl.workPackage,
        ])
        if (variant != CreateVariantInvalidRunYapsa.NO_SNV) {
            AnalysisAnalysisArtefactData<RoddySnvCallingInstance> analysisArtefactDataSnv = createAnalysisAnalysisArtefactData(
                    SnvDomainFactory.INSTANCE.createInstance([
                            processingState   : AnalysisProcessingStates.FINISHED,
                            samplePair        : samplePair,
                            sampleType1BamFile: bamFileDisease,
                            sampleType2BamFile: bamFileControl,
                            workflowArtefact  : createWorkflowArtefact(),
                    ]))
            additionalDataList.dependingAnalysisDataList[RunYapsaWorkflow.SNV_INPUT] = [analysisArtefactDataSnv]
        }
        if (variant != CreateVariantInvalidRunYapsa.NO_INDEL) {
            AnalysisAnalysisArtefactData<IndelCallingInstance> analysisArtefactDataIndel = createAnalysisAnalysisArtefactData(
                    IndelDomainFactory.INSTANCE.createInstance([
                            processingState   : AnalysisProcessingStates.FINISHED,
                            samplePair        : samplePair,
                            sampleType1BamFile: bamFileDisease,
                            sampleType2BamFile: bamFileControl,
                            workflowArtefact  : createWorkflowArtefact(),
                    ]))
            additionalDataList.dependingAnalysisDataList[RunYapsaWorkflow.INDEL_INPUT] = [analysisArtefactDataIndel]
        }
    }

    void "getWorkflowName, should return RunYapsaWorkflow.WORKFLOW"() {
        expect:
        decider.workflowName == RunYapsaWorkflow.WORKFLOW
    }

    void "getInstanceClass, should return RunYapsaInstance"() {
        expect:
        decider.instanceClass == RunYapsaInstance
    }

    void "getDependingAnalysisInstanceClass, should return map with snv and indel"() {
        expect:
        decider.dependingAnalysisInstanceClass == [
                (RunYapsaWorkflow.SNV_INPUT)  : RoddySnvCallingInstance,
                (RunYapsaWorkflow.INDEL_INPUT): IndelCallingInstance,
        ]
    }

    void "getArtefactType, should return ArtefactType.RUN_YAPSA"() {
        expect:
        decider.artefactType == ArtefactType.RUN_YAPSA
    }

    void "getPipelineName, should return Pipeline.Name.RUN_YAPSA"() {
        expect:
        decider.pipelineName == Pipeline.Name.RUN_YAPSA
    }

    @Override
    protected RunYapsaDomainFactory getFactory() {
        return RunYapsaDomainFactory.INSTANCE
    }

    void "fetchAdditionalArtefacts"() {
        given:
        RoddyBamFile bamFile1 = createBamFile()
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1], [], [:])

        RoddyBamFile bamFile2 = createBamFile()
        AnalysisBamFileArtefactData artefactData2 = createAnalysisBamFileArtefactData(bamFile2)
        AnalysisAnalysisArtefactData<RunYapsaInstance> analysisArtefactData = createAnalysisAnalysisArtefactData(createAnalysisInstance())
        AnalysisAnalysisArtefactData<RoddySnvCallingInstance> analysisArtefactDataSnv = createAnalysisAnalysisArtefactData(
                SnvDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysisArtefactData.artefact))
        AnalysisAnalysisArtefactData<IndelCallingInstance> analysisArtefactDataIndel = createAnalysisAnalysisArtefactData(
                IndelDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysisArtefactData.artefact))

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([bamFile1]) >> [artefactData2]
            1 * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], decider.instanceClass) >> [analysisArtefactData]
            1 * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], RoddySnvCallingInstance) >> [analysisArtefactDataSnv]
            1 * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], IndelCallingInstance) >> [analysisArtefactDataIndel]
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == [artefactData2]
        dataList2.alreadyRunAnalysisDataList == [analysisArtefactData]
        dataList2.dependingAnalysisDataList == [
                (RunYapsaWorkflow.SNV_INPUT)  : [analysisArtefactDataSnv],
                (RunYapsaWorkflow.INDEL_INPUT): [analysisArtefactDataIndel],
        ]
    }

    void "fetchAdditionalArtefacts, if input is empty, then return object with empty list"() {
        given:
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([], [], [:])

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([]) >> []
            1 * fetchRelatedAnalysisArtefactsForBamFiles([], decider.instanceClass) >> []
            1 * fetchRelatedAnalysisArtefactsForBamFiles([], RoddySnvCallingInstance) >> []
            1 * fetchRelatedAnalysisArtefactsForBamFiles([], IndelCallingInstance) >> []
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == []
        dataList2.alreadyRunAnalysisDataList == []
        dataList2.dependingAnalysisDataList == [
                (RunYapsaWorkflow.SNV_INPUT)  : [],
                (RunYapsaWorkflow.INDEL_INPUT): [],
        ]
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #variant, then do not create an analysis and create a warning"() {
        given:
        setupDataForCreateWorkflowRunsAndOutputArtefacts(variant, [:])

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(
                projectSeqTypeGroup, baseDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion, [:])

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.size() == 1
        deciderResult.warnings.first().contains(variant.message)

        where:
        variant << CreateVariantInvalidRunYapsa.values()
    }

    /**
     * Defines invalid variants for the unroll
     */
    @TupleConstructor
    protected enum CreateVariantInvalidRunYapsa implements AbstractAnalysisDeciderSpec.CreatePairVariant {
        NO_SNV("since depending analysis SNV_INPUT is not available"),
        NO_INDEL("since depending analysis INDEL_INPUT is not available")

        String message
    }
}
