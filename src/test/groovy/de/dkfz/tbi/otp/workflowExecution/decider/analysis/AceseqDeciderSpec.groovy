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
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.AceseqDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflowExecution.AnalysisArtefactService
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

class AceseqDeciderSpec extends AbstractAnalysisDeciderSpec<AceseqInstance> {

    @Override
    Class[] getDomainClassesToMock() {
        return super.domainClassesToMock + [
                AceseqInstance,
                SophiaInstance,
        ]
    }

    void setup() {
        decider = new AceseqDecider([
                aceseqWorkFileService: Mock(AceseqWorkFileService) {
                    0 * _
                    _ * constructInstanceName(_) >> "instance"
                },
        ])
    }

    @Override
    void setupDataForCreateWorkflowRunsAndOutputArtefacts(CreatePairVariant variant, Map<String, ?> adaptation) {
        super.setupDataForCreateWorkflowRunsAndOutputArtefacts(variant, adaptation)

        if (variant != CreateVariantInvalidAceseq.NO_SOPHIA) {
            AnalysisAnalysisArtefactData<SophiaInstance> analysisArtefactDataSophia = createAnalysisAnalysisArtefactData(
                    SophiaDomainFactory.INSTANCE.createInstance([
                            processingState   : AnalysisProcessingStates.FINISHED,
                            samplePair        : SophiaDomainFactory.INSTANCE.createSamplePair([
                                    mergingWorkPackage1: bamFileDisease.workPackage,
                                    mergingWorkPackage2: bamFileControl.workPackage,
                            ]),
                            sampleType1BamFile: bamFileDisease,
                            sampleType2BamFile: bamFileControl,
                            workflowArtefact  : createWorkflowArtefact(),
                    ]))
            additionalDataList.dependingAnalysisDataList[AceseqWorkflow.SOPHIA_INPUT] = [analysisArtefactDataSophia]
        }
    }

    void "getWorkflowName, should return AceseqWorkflow.WORKFLOW"() {
        expect:
        decider.workflowName == AceseqWorkflow.WORKFLOW
    }

    void "getInstanceClass, should return AceseqInstance"() {
        expect:
        decider.instanceClass == AceseqInstance
    }

    void "getDependingAnalysisInstanceClass, should return map with sophia"() {
        expect:
        decider.dependingAnalysisInstanceClass == [
                (AceseqWorkflow.SOPHIA_INPUT): SophiaInstance,
        ]
    }

    void "getArtefactType, should return ArtefactType.ACESEQ"() {
        expect:
        decider.artefactType == ArtefactType.ACESEQ
    }

    void "getPipelineName, should return Pipeline.Name.RODDY_ACESEQ"() {
        expect:
        decider.pipelineName == Pipeline.Name.RODDY_ACESEQ
    }

    @Override
    protected AceseqDomainFactory getFactory() {
        return AceseqDomainFactory.INSTANCE
    }

    void "fetchAdditionalArtefacts"() {
        given:
        RoddyBamFile bamFile1 = createBamFile()
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1], [], [:])

        RoddyBamFile bamFile2 = createBamFile()
        AnalysisBamFileArtefactData artefactData2 = createAnalysisBamFileArtefactData(bamFile2)
        AnalysisAnalysisArtefactData<AceseqInstance> analysisArtefactData = createAnalysisAnalysisArtefactData(createAnalysisInstance())
        AnalysisAnalysisArtefactData<SophiaInstance> analysisArtefactDataSophia = createAnalysisAnalysisArtefactData(
                SophiaDomainFactory.INSTANCE.createInstanceWithSameSamplePair(analysisArtefactData.artefact))

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([bamFile1]) >> [artefactData2]
            1 * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], decider.instanceClass) >> [analysisArtefactData]
            1 * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], SophiaInstance) >> [analysisArtefactDataSophia]
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == [artefactData2]
        dataList2.alreadyRunAnalysisDataList == [analysisArtefactData]
        dataList2.dependingAnalysisDataList == [(AceseqWorkflow.SOPHIA_INPUT): [analysisArtefactDataSophia]]
    }

    void "fetchAdditionalArtefacts, if input is empty, then return object with empty list"() {
        given:
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([], [], [:])

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([]) >> []
            1 * fetchRelatedAnalysisArtefactsForBamFiles([], decider.instanceClass) >> []
            1 * fetchRelatedAnalysisArtefactsForBamFiles([], SophiaInstance) >> []
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == []
        dataList2.alreadyRunAnalysisDataList == []
        dataList2.dependingAnalysisDataList == [(AceseqWorkflow.SOPHIA_INPUT): []]
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
        variant << CreateVariantInvalidAceseq.values()
    }

    /**
     * Defines invalid variants for the unroll
     */
    @TupleConstructor
    protected enum CreateVariantInvalidAceseq implements AbstractAnalysisDeciderSpec.CreatePairVariant {
        NO_SOPHIA("since depending analysis SOPHIA_INPUT is not available")

        String message
    }
}
