/*
 * Copyright 2011-2026 The OTP authors
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

import grails.testing.gorm.DataTest
import groovy.transform.TupleConstructor
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.AbstractAnalysisDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.*

abstract class AbstractAnalysisDeciderSpec<T extends BamFilePairAnalysis> extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    protected AbstractAnalysisDecider decider

    protected Workflow workflow
    protected WorkflowApiVersion workflowApiVersion
    protected WorkflowVersion workflowVersionOld
    protected WorkflowVersion workflowVersion
    protected Pipeline pipeline
    protected RoddyBamFile bamFileDisease
    protected RoddyBamFile bamFileControl
    protected AnalysisArtefactDataList dataList
    protected AnalysisArtefactDataList additionalDataList
    protected AnalysisAdditionalData additionalData
    protected ProjectSeqTypeGroup projectSeqTypeGroup
    protected BaseDeciderGroup baseDeciderGroup

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                SampleTypePerProject,
                WorkflowRunInputArtefact,
                WorkflowVersionSelector,
        ]
    }

    protected abstract AbstractAnalysisDomainFactory<T> getFactory()

    void "getWorkflow"() {
        given:
        Workflow workflow = factory.findOrCreateWorkflow()

        decider.workflowService = Mock(WorkflowService) {
            0 * _
            1 * getExactlyOneWorkflow(workflow.name) >> workflow
        }

        expect:
        decider.workflow == workflow
    }

    void "getSupportedInputArtefactTypes"() {
        expect:
        decider.supportedInputArtefactTypes == [ArtefactType.BAM] as Set
    }

    void "fetchInputArtefacts"() {
        given:
        Collection<WorkflowArtefact> inputArtefacts = [createWorkflowArtefact()]
        Set<SeqType> seqTypes = [createSeqType()] as Set
        RoddyBamFile bamFile = createBamFile()

        AnalysisBamFileArtefactData artefactData = createAnalysisBamFileArtefactData(bamFile)

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchBamFileArtefacts(inputArtefacts, seqTypes) >> [artefactData]
        }

        when:
        AnalysisArtefactDataList dataList = decider.fetchInputArtefacts(inputArtefacts, seqTypes)

        then:
        dataList.bamFileDataList == [artefactData]
        dataList.alreadyRunAnalysisDataList == []
    }

    void "fetchInputArtefacts, if input is empty, then return object with empty list"() {
        given:
        Set<SeqType> seqTypes = [createSeqType()] as Set

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchBamFileArtefacts([], seqTypes) >> []
        }

        when:
        AnalysisArtefactDataList dataList = decider.fetchInputArtefacts([], seqTypes)

        then:
        dataList.bamFileDataList == []
        dataList.alreadyRunAnalysisDataList == []
    }

    void "fetchAdditionalArtefacts, returns related BAM and analyses artefact data"() {
        given:
        RoddyBamFile bamFile1 = createBamFile()
        RoddyBamFile bamFile2 = createBamFile([
                workPackage: createMergingWorkPackage([
                        sample : createSample([
                                individual: bamFile1.individual,
                        ]),
                        seqType: bamFile1.seqType,
                ]),
        ])

        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        createAnalysisBamFileArtefactData(bamFile2)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1], [], [:])

        AnalysisBamFileArtefactData artefactDataAdditional1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisBamFileArtefactData artefactDataAdditional2 = createAnalysisBamFileArtefactData(bamFile2)

        T analysis1 = factory.createInstanceWithRoddyBamFiles([
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2,
        ])
        AnalysisAnalysisArtefactData<T> analysisArtefactData1 = createAnalysisAnalysisArtefactData(analysis1)

        Map<String, List<AnalysisAnalysisArtefactData<BamFilePairAnalysis>>> dependingAnalysisData = [:]
        decider.dependingAnalysisInstanceClasses.each { String role, List<Class<?>> dependingAnalysis ->
            dependingAnalysisData[role] = []
        }

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([bamFile1]) >> [artefactDataAdditional1, artefactDataAdditional2]
            1 * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], decider.instanceClasses) >> [analysisArtefactData1]
            decider.dependingAnalysisInstanceClasses.size() * fetchRelatedAnalysisArtefactsForBamFiles([bamFile1], _) >> []
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == [artefactDataAdditional1, artefactDataAdditional2]
        dataList2.alreadyRunAnalysisDataList == [analysisArtefactData1]
        dataList2.dependingAnalysisDataList == dependingAnalysisData
    }

    void "fetchAdditionalArtefacts, if input is empty, then return object with empty list"() {
        given:
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([], [], [:])

        Map<String, List<AnalysisAnalysisArtefactData<BamFilePairAnalysis>>> dependingAnalysisData = [:]
        decider.dependingAnalysisInstanceClasses.each { String role, List<Class<?>> dependingAnalysis ->
            dependingAnalysisData[role] = []
        }

        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchRelatedBamFilesArtefactsForBamFiles([]) >> []
            1 * fetchRelatedAnalysisArtefactsForBamFiles([], decider.instanceClasses) >> []
            decider.dependingAnalysisInstanceClasses.size() * fetchRelatedAnalysisArtefactsForBamFiles([], _) >> []
        }

        when:
        AnalysisArtefactDataList dataList2 = decider.fetchAdditionalArtefacts(dataList)

        then:
        dataList2.bamFileDataList == []
        dataList2.alreadyRunAnalysisDataList == []
        dataList2.dependingAnalysisDataList == dependingAnalysisData
    }

    void "fetchAdditionalData"() {
        given:
        Workflow workflow = createWorkflow(name: decider.workflowName)
        RoddyBamFile bamFile1 = createBamFile()
        RoddyBamFile bamFile2 = createBamFile([
                workPackage: createMergingWorkPackage([
                        sample : createSample([
                                individual: bamFile1.individual,
                        ]),
                        seqType: bamFile1.seqType,
                ]),
        ])
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisBamFileArtefactData artefactData2 = createAnalysisBamFileArtefactData(bamFile2)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1, artefactData2], [], [:])
        AnalysisArtefactDataList dataList2 = new AnalysisArtefactDataList([], [], [:])

        Pipeline pipeline = findOrCreateAnalysisPipeline()

        and: 'objects for fetchSamplePairs'
        SamplePair samplePair = factory.createSamplePair([
                mergingWorkPackage1: bamFile1.workPackage,
                mergingWorkPackage2: bamFile2.workPackage,
        ])
        AnalysisGroup analysisGroup = new AnalysisGroup(bamFile1.workPackage, bamFile2.workPackage)

        Map<AnalysisGroup, SamplePair> samplePairMap = [
                (analysisGroup): samplePair,
        ]

        and: 'objects for fetchCategoryPerSampleTypeAndProject'
        ProjectSampleTypeGroup projectSampleTypeGroup1 = new ProjectSampleTypeGroup(bamFile1.project, bamFile1.sampleType)
        ProjectSampleTypeGroup projectSampleTypeGroup2 = new ProjectSampleTypeGroup(bamFile2.project, bamFile2.sampleType)
        Map<ProjectSampleTypeGroup, SampleTypePerProject.Category> categoryMap = [
                (projectSampleTypeGroup1): SampleTypePerProject.Category.DISEASE,
                (projectSampleTypeGroup2): SampleTypePerProject.Category.CONTROL,
        ]

        and: 'mocked services'
        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchSamplePairs([bamFile1, bamFile2]) >> samplePairMap
            1 * fetchCategoryPerSampleTypeAndProject([bamFile1, bamFile2]) >> categoryMap
        }
        decider.pipelineService = Mock(PipelineService) {
            0 * _
            1 * findByPipelineName(_) >> pipeline
        }

        when:
        AnalysisAdditionalData analysisAdditionalData = decider.fetchAdditionalData(dataList, dataList2, workflow)

        then:
        analysisAdditionalData.samplePairMap == samplePairMap
        analysisAdditionalData.categoryMap == categoryMap
        analysisAdditionalData.pipeline == pipeline
    }

    void "fetchAdditionalData, if input is empty, then return object with empty maps"() {
        given:
        Workflow workflow = createWorkflow(name: decider.workflowName)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([], [], [:])
        AnalysisArtefactDataList dataList2 = new AnalysisArtefactDataList([], [], [:])

        and: 'mocked services'
        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
        }
        decider.pipelineService = Mock(PipelineService) {
            0 * _
        }

        when:
        AnalysisAdditionalData analysisAdditionalData = decider.fetchAdditionalData(dataList, dataList2, workflow)

        then:
        analysisAdditionalData.samplePairMap == [:]
        analysisAdditionalData.categoryMap == [:]
        analysisAdditionalData.pipeline == null
    }

    void "fetchAdditionalData, include also category of additional bam files"() {
        given:
        Workflow workflow = createWorkflow(name: decider.workflowName)
        RoddyBamFile bamFile1 = createBamFile()
        RoddyBamFile bamFile2 = createBamFile([
                workPackage: createMergingWorkPackage([
                        sample : createSample([
                                individual: bamFile1.individual,
                        ]),
                        seqType: bamFile1.seqType,
                ]),
        ])
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisBamFileArtefactData artefactData2 = createAnalysisBamFileArtefactData(bamFile2)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1], [], [:])
        AnalysisArtefactDataList dataList2 = new AnalysisArtefactDataList([artefactData2], [], [:])

        Pipeline pipeline = findOrCreateAnalysisPipeline()

        and: 'objects for fetchSamplePairs'
        SamplePair samplePair = factory.createSamplePair([
                mergingWorkPackage1: bamFile1.workPackage,
                mergingWorkPackage2: bamFile2.workPackage,
        ])
        AnalysisGroup analysisGroup = new AnalysisGroup(bamFile1.workPackage, bamFile2.workPackage)

        Map<AnalysisGroup, SamplePair> samplePairMap = [
                (analysisGroup): samplePair,
        ]

        and: 'objects for fetchCategoryPerSampleTypeAndProject'
        ProjectSampleTypeGroup projectSampleTypeGroup1 = new ProjectSampleTypeGroup(bamFile1.project, bamFile1.sampleType)
        ProjectSampleTypeGroup projectSampleTypeGroup2 = new ProjectSampleTypeGroup(bamFile2.project, bamFile2.sampleType)
        Map<ProjectSampleTypeGroup, SampleTypePerProject.Category> categoryMap = [
                (projectSampleTypeGroup1): SampleTypePerProject.Category.DISEASE,
                (projectSampleTypeGroup2): SampleTypePerProject.Category.CONTROL,
        ]

        and: 'mocked services'
        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchSamplePairs([bamFile1, bamFile2]) >> samplePairMap
            1 * fetchCategoryPerSampleTypeAndProject([bamFile1, bamFile2]) >> categoryMap
        }
        decider.pipelineService = Mock(PipelineService) {
            0 * _
            1 * findByPipelineName(_) >> pipeline
        }

        when:
        AnalysisAdditionalData analysisAdditionalData = decider.fetchAdditionalData(dataList, dataList2, workflow)

        then:
        analysisAdditionalData.samplePairMap == samplePairMap
        analysisAdditionalData.categoryMap == categoryMap
        analysisAdditionalData.pipeline == pipeline
    }

    void "fetchWorkflowVersionSelector"() {
        given:
        Workflow workflow = createWorkflow(name: decider.workflowName)

        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector()

        and: 'input objects'
        RoddyBamFile bamFile1 = createBamFile()
        AnalysisBamFileArtefactData artefactData = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData], [], [:])

        and: 'mocked services'
        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchWorkflowVersionSelectorForBamFiles(workflow, [bamFile1]) >> [workflowVersionSelector]
        }

        when:
        List<WorkflowVersionSelector> workflowVersionSelectorList = decider.fetchWorkflowVersionSelector(dataList, workflow)

        then:
        workflowVersionSelectorList == [workflowVersionSelector]
    }

    void "fetchWorkflowVersionSelector, if input is empty, then return object with empty list"() {
        given:
        Workflow workflow = createWorkflow(name: decider.workflowName)

        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([], [], [:])

        and: 'mocked services'
        decider.analysisArtefactService = Mock(AnalysisArtefactService) {
            0 * _
            1 * fetchWorkflowVersionSelectorForBamFiles(workflow, []) >> []
        }

        when:
        List<WorkflowVersionSelector> workflowVersionSelectorList = decider.fetchWorkflowVersionSelector(dataList, workflow)

        then:
        workflowVersionSelectorList == []
    }

    void "groupData, one BamFile"() {
        given:
        RoddyBamFile bamFile1 = createBamFile()

        and: 'AnalysisArtefactDataList'
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1], [], [:])

        and: 'AnalysisAdditionalData'
        AnalysisAdditionalData additionalData = new AnalysisAdditionalData([:], [:], findOrCreatePipeline())

        and: 'useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        BaseDeciderGroup deciderGroup = createAnalysisDeciderGroup(bamFile1)

        when:
        Map<BaseDeciderGroup, AnalysisArtefactDataList> analysisArtefactDataListMap = decider.groupData(dataList, additionalData, userParams)

        then:
        analysisArtefactDataListMap.size() == 1
        analysisArtefactDataListMap.keySet().first() == deciderGroup
        analysisArtefactDataListMap.values().first() == dataList
    }

    void "groupData, two BamFiles with same referenced data"() {
        given:
        RoddyBamFile bamFile1 = createBamFile()
        RoddyBamFile bamFile2 = createBamFile([
                workPackage: createMergingWorkPackage([
                        sample : createSample([
                                individual: bamFile1.individual,
                        ]),
                        seqType: bamFile1.seqType,
                ]),
        ])

        and: 'AnalysisArtefactDataList'
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisBamFileArtefactData artefactData2 = createAnalysisBamFileArtefactData(bamFile2)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactData1, artefactData2], [], [:])

        and: 'AnalysisAdditionalData'
        AnalysisAdditionalData additionalData = new AnalysisAdditionalData([:], [:], findOrCreatePipeline())

        and: 'useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        BaseDeciderGroup deciderGroup = createAnalysisDeciderGroup(bamFile1)

        when:
        Map<BaseDeciderGroup, AnalysisArtefactDataList> analysisArtefactDataListMap = decider.groupData(dataList, additionalData, userParams)

        then:
        analysisArtefactDataListMap.size() == 1
        analysisArtefactDataListMap.keySet().first() == deciderGroup
        analysisArtefactDataListMap.values().first() == dataList
    }

    void "groupData, two BamFiles without shared references"() {
        given: 'first seqTrack'
        RoddyBamFile bamFile1 = createBamFile()
        RoddyBamFile bamFile2 = createBamFile()

        and: 'AnalysisArtefactDataList'
        AnalysisBamFileArtefactData artefactData1 = createAnalysisBamFileArtefactData(bamFile1)
        AnalysisBamFileArtefactData artefactData2 = createAnalysisBamFileArtefactData(bamFile2)
        AnalysisArtefactDataList dataList1 = new AnalysisArtefactDataList([artefactData1], [], [:])
        AnalysisArtefactDataList dataList2 = new AnalysisArtefactDataList([artefactData2], [], [:])

        and: 'AnalysisArtefactDataList together'
        AnalysisArtefactDataList dataListTogether = new AnalysisArtefactDataList([artefactData1, artefactData2], [], [:])

        and: 'AnalysisAdditionalData'
        AnalysisAdditionalData additionalData = new AnalysisAdditionalData([:], [:], findOrCreatePipeline())

        and: 'useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        BaseDeciderGroup deciderGroup1 = createAnalysisDeciderGroup(bamFile1)
        BaseDeciderGroup deciderGroup2 = createAnalysisDeciderGroup(bamFile2)

        when:
        Map<BaseDeciderGroup, AnalysisArtefactDataList> analysisArtefactDataListMap = decider.groupData(dataListTogether, additionalData, userParams)

        then:
        analysisArtefactDataListMap.size() == 2
        TestCase.assertContainSame(analysisArtefactDataListMap.keySet(), [deciderGroup1, deciderGroup2])
        analysisArtefactDataListMap[deciderGroup1] == dataList1
        analysisArtefactDataListMap[deciderGroup2] == dataList2
    }

    void "groupData all data combination"() {
        given: 'data to group'
        List<Project> projects = (1..2).collect {
            createProject()
        }
        List<SeqType> seqTypes = (1..2).collect {
            createSeqTypePaired()
        }
        List<SampleType> sampleTypes = (1..2).collect {
            createSampleType()
        }

        and: 'create combination'
        List<Individual> individuals = projects.collectMany { Project project ->
            (1..2).collect {
                createIndividual([
                        project: project,
                ])
            }
        }
        List<Sample> samples = individuals.collectMany { Individual individual ->
            sampleTypes.collect { SampleType sampleType ->
                createSample([
                        individual: individual,
                        sampleType: sampleType,
                ])
            }
        }
        List<RoddyBamFile> bamFiles = samples.collectMany { Sample sample ->
            seqTypes.collectMany { SeqType seqType ->
                MergingWorkPackage workPackage = createMergingWorkPackage([
                        sample : sample,
                        seqType: seqType,
                ])
                (1..2).collect {
                    createBamFile([
                            workPackage: workPackage,
                    ])
                }
            }
        }

        and: 'input objects'
        List<AnalysisBamFileArtefactData> bamFileData = bamFiles.collect { RoddyBamFile bamFile ->
            createAnalysisBamFileArtefactData(bamFile)
        }
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList(bamFileData, [], [:])

        and: 'additional data'
        AnalysisAdditionalData additionalData = new AnalysisAdditionalData([:], [:], findOrCreatePipeline())

        and: 'set useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        Map<BaseDeciderGroup, List<AnalysisBamFileArtefactData>> bamFileDeciderGroups = bamFileData.groupBy {
            createAnalysisDeciderGroup(it.artefact)
        }

        when:
        Map<BaseDeciderGroup, AnalysisArtefactDataList> analysisArtefactDataListMap = decider.groupData(dataList, additionalData, userParams)

        then:
        TestCase.assertContainSame(analysisArtefactDataListMap.keySet(), bamFileDeciderGroups.keySet())
        analysisArtefactDataListMap.each { BaseDeciderGroup group, AnalysisArtefactDataList list ->
            assert list.bamFileDataList == bamFileDeciderGroups[group]
        }
    }

    void "groupData, one BamFile with Analysis"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        T analysis = factory.createInstanceWithRoddyBamFiles()

        and: 'AnalysisArtefactDataList'
        AnalysisBamFileArtefactData artefactDataBamFile = createAnalysisBamFileArtefactData(bamFile)
        AnalysisAnalysisArtefactData<T> artefactDataAnalysis = createAnalysisAnalysisArtefactData(analysis)
        AnalysisArtefactDataList dataList = new AnalysisArtefactDataList([artefactDataBamFile], [artefactDataAnalysis], [:])
        AnalysisArtefactDataList dataListBam = new AnalysisArtefactDataList([artefactDataBamFile], [], [:])
        AnalysisArtefactDataList dataListAnalysis = new AnalysisArtefactDataList([], [artefactDataAnalysis], [:])

        and: 'AnalysisAdditionalData'
        AnalysisAdditionalData additionalData = new AnalysisAdditionalData([:], [:], findOrCreatePipeline())

        and: 'useParams'
        Map<String, String> userParams = [:]

        and: 'expected'
        BaseDeciderGroup deciderGroup1 = createAnalysisDeciderGroup(bamFile)
        BaseDeciderGroup deciderGroup2 = createAnalysisDeciderGroup(analysis)

        when:
        Map<BaseDeciderGroup, AnalysisArtefactDataList> analysisArtefactDataListMap = decider.groupData(dataList, additionalData, userParams)

        then:
        analysisArtefactDataListMap.size() == 2
        analysisArtefactDataListMap.containsKey(deciderGroup1)
        analysisArtefactDataListMap.containsKey(deciderGroup2)
        analysisArtefactDataListMap[deciderGroup1] == dataListBam
        analysisArtefactDataListMap[deciderGroup2] == dataListAnalysis
    }

    void "createAnalysisDeciderGroup, when input is a bam file, then return correct BaseDeciderGroup"() {
        given:
        RoddyBamFile bamFile = createBamFile()

        AnalysisBamFileArtefactData data = createAnalysisBamFileArtefactData(bamFile)

        when:
        BaseDeciderGroup group = decider.createAnalysisDeciderGroup(data)

        then:
        group.individual == bamFile.individual
        group.seqType == bamFile.seqType
    }

    void "createAnalysisDeciderGroup, when input is a analysis, then return correct BaseDeciderGroup"() {
        given:
        T t = factory.createInstanceWithRoddyBamFiles()
        AnalysisAnalysisArtefactData<T> data = createAnalysisAnalysisArtefactData(t)

        when:
        BaseDeciderGroup group = decider.createAnalysisDeciderGroup(data)

        then:
        group.individual == t.individual
        group.seqType == t.seqType
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #variant, then create new analysis"() {
        given:
        setupDataForCreateWorkflowRunsAndOutputArtefacts(variant, [:])
        int countAnalysis = BamFilePairAnalysis.count()

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(
                projectSeqTypeGroup, baseDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion, [:])

        then:
        deciderResult.newArtefacts.size() == 1
        deciderResult.warnings.empty == variant.message.empty
        if (deciderResult.warnings) {
            assert deciderResult.warnings.first().contains(variant.message)
        }

        WorkflowArtefact workflowArtefact = deciderResult.newArtefacts[0]
        workflowArtefact.artefactType == decider.artefactType
        workflowArtefact.outputRole == AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

        workflowArtefact.displayName.contains(bamFileDisease.individual.pid)
        workflowArtefact.displayName.contains(bamFileDisease.sampleType.name)
        workflowArtefact.displayName.contains(bamFileControl.sampleType.name)
        workflowArtefact.displayName.contains(bamFileDisease.seqType.displayNameWithLibraryLayout)

        WorkflowRun workflowRun = workflowArtefact.producedBy
        workflowRun.workflowVersion == workflowVersion
        workflowRun.workflow == workflow

        workflowRun.displayName.contains(bamFileDisease.project.name)
        workflowRun.displayName.contains(bamFileDisease.individual.pid)
        workflowRun.displayName.contains(bamFileDisease.sampleType.name)
        workflowRun.displayName.contains(bamFileControl.sampleType.name)
        workflowRun.displayName.contains(bamFileDisease.seqType.displayNameWithLibraryLayout)

        workflowRun.shortDisplayName.contains(bamFileDisease.individual.pid)
        workflowRun.shortDisplayName.contains(bamFileDisease.sampleType.name)
        workflowRun.shortDisplayName.contains(bamFileControl.sampleType.name)
        workflowRun.shortDisplayName.contains(bamFileDisease.seqType.displayNameWithLibraryLayout)

        BamFilePairAnalysis.count() == 1 + countAnalysis
        BamFilePairAnalysis analysis = BamFilePairAnalysis.last()
        analysis instanceof T
        analysis.workflowArtefact == workflowArtefact
        analysis.sampleType1BamFile == bamFileDisease
        analysis.sampleType2BamFile == bamFileControl

        where:
        variant << CreateVariantValidPair.values()
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #variant, then do not create an analysis and create a warning"() {
        given:
        setupDataForCreateWorkflowRunsAndOutputArtefacts(variant, [:])

        // simulate that the reference genome is not supported
        if (variant == CreateVariantInvalid.REF_GENOME_NOT_SUPPORTED) {
            workflowVersion.allowedReferenceGenomes -= bamFileDisease.mergingWorkPackage.referenceGenome
        }

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(
                projectSeqTypeGroup, baseDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion, [:])

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.size() == 1
        deciderResult.warnings.first().contains(variant.message)

        where:
        variant << CreateVariantInvalid.values()
    }

    void "createWorkflowRunsAndOutputArtefacts, when both disease and control have wrong ref genomes, then do not create an analysis and create warnings"() {
        given:
        setupDataForCreateWorkflowRunsAndOutputArtefacts(CreateVariantInvalid.REF_GENOME_NOT_SUPPORTED, [:])

        // both disease and control samples have reference genomes which are not supported (not in the allowed list)
        workflowVersion.allowedReferenceGenomes -= [
                bamFileDisease.mergingWorkPackage.referenceGenome,
                bamFileControl.mergingWorkPackage.referenceGenome,
        ]

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(
                projectSeqTypeGroup, baseDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion, [:])

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.size() == 2
        deciderResult.warnings.first().contains(CreateVariantInvalid.REF_GENOME_NOT_SUPPORTED.message)
        deciderResult.warnings.last().contains(CreateVariantInvalid.REF_GENOME_NOT_SUPPORTED.message)
    }

    void "createAnalysisWithoutFlush"() {
        given:
        SamplePair samplePair = factory.createSamplePair()
        RoddyBamFile sampleType1BamFile = createBamFile([workPackage: samplePair.mergingWorkPackage1])
        RoddyBamFile sampleType2BamFile = createBamFile([workPackage: samplePair.mergingWorkPackage2])
        Map properties = [
                samplePair        : samplePair,
                sampleType1BamFile: sampleType1BamFile,
                sampleType2BamFile: sampleType2BamFile,
                instanceName      : "instanceName",
        ]

        when:
        BamFilePairAnalysis result = decider.createAnalysisWithoutFlush(properties)

        then:
        result instanceof T
        result.id
    }

    void "createSingleAnalysis, when valid disease and control data provided, then create analysis successfully"() {
        given:
        setupDataForCreateWorkflowRunsAndOutputArtefacts(CreateVariantValidPair.BOTH_GIVEN, [:])

        AnalysisBamFileArtefactData diseaseData = createAnalysisBamFileArtefactData(bamFileDisease)
        AnalysisBamFileArtefactData controlData = createAnalysisBamFileArtefactData(bamFileControl)

        AnalysisArtefactDataList allArtefacts = new AnalysisArtefactDataList([diseaseData, controlData], [], additionalDataList.dependingAnalysisDataList ?: [:])
        DeciderResult deciderResult = new DeciderResult()

        int initialAnalysisCount = BamFilePairAnalysis.count()

        when:
        decider.createSingleAnalysis(baseDeciderGroup, diseaseData, controlData, allArtefacts,
                additionalData, workflowVersion, deciderResult, [:])

        then:
        BamFilePairAnalysis.count() == initialAnalysisCount + 1

        BamFilePairAnalysis analysis = BamFilePairAnalysis.last()
        analysis.sampleType1BamFile == bamFileDisease
        analysis.sampleType2BamFile == bamFileControl
        analysis.workflowArtefact

        deciderResult.newArtefacts.size() == 1
        deciderResult.newArtefacts[0] == analysis.workflowArtefact
        deciderResult.infos.any { it.contains("create analysis") }
    }

    @Unroll
    void "createWorkflowRunsAndOutputArtefacts, when #deciderCreateWorkflowAction, existsAndSameVersion=#sameVersion, analysisCreated=#existingAnalysis, then correct warning is added"() {
        given:
        Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = [
                (AceseqDecider)  : deciderCreateWorkflowAction,
                (SnvDecider)     : deciderCreateWorkflowAction,
                (SophiaDecider)  : deciderCreateWorkflowAction,
                (IndelDecider)   : deciderCreateWorkflowAction,
                (RunYapsaDecider): deciderCreateWorkflowAction,
        ]

        and:
        setupDataForCreateWorkflowRunsAndOutputArtefacts(createVariant, [sameVersion: sameVersion, existingAnalysis: existingAnalysis])

        when:
        DeciderResult deciderResult = decider.createWorkflowRunsAndOutputArtefacts(
                projectSeqTypeGroup, baseDeciderGroup,
                dataList, additionalDataList, additionalData, workflowVersion, deciderAction)

        then:
        !deciderResult.newArtefacts.empty == newArtefactCreated
        (deciderResult?.warnings?.size() > 0 && deciderResult?.warnings?.first()?.contains(expectedWarningOrInfo)) ||
                (deciderResult?.infos?.size() > 0 && deciderResult?.infos?.any { it.contains(expectedWarningOrInfo) })

        where:
        deciderCreateWorkflowAction                          | createVariant                               | sameVersion | existingAnalysis || expectedWarningOrInfo                                                              | newArtefactCreated
        DeciderCreateWorkflowAction.CREATE_ALWAYS            | CreateVariantInvalid.EXISTING_ANALYSIS      | true        | true             || "action is CREATE_ALWAYS"                                                          | true
        DeciderCreateWorkflowAction.CREATE_ALWAYS            | CreateVariantInvalid.EXISTING_ANALYSIS      | false       | true             || "action is CREATE_ALWAYS"                                                          | true
        DeciderCreateWorkflowAction.CREATE_ALWAYS            | CreateVariantValidPair.SAMPLE_PAIR_NO_EXIST | false       | false            || "create analysis"                                                                  | true
        DeciderCreateWorkflowAction.CREATE_MISSING_AND_NEWER | CreateVariantInvalid.EXISTING_ANALYSIS      | true        | true             || "analysis with the same version was found, and action is CREATE_MISSING_AND_NEWER" | false
        DeciderCreateWorkflowAction.CREATE_MISSING_AND_NEWER | CreateVariantInvalid.EXISTING_ANALYSIS      | false       | true             || "analysis has a different version, and action is CREATE_MISSING_AND_NEWER"         | true
        DeciderCreateWorkflowAction.CREATE_MISSING_AND_NEWER | CreateVariantValidPair.SAMPLE_PAIR_NO_EXIST | false       | false            || "create analysis"                                                                  | true
        DeciderCreateWorkflowAction.CREATE_MISSING           | CreateVariantInvalid.EXISTING_ANALYSIS      | true        | true             || "analysis was found and action is CREATE_MISSING"                                  | false
        DeciderCreateWorkflowAction.CREATE_MISSING           | CreateVariantInvalid.EXISTING_ANALYSIS      | false       | true             || "analysis was found and action is CREATE_MISSING"                                  | false
        DeciderCreateWorkflowAction.CREATE_MISSING           | CreateVariantValidPair.SAMPLE_PAIR_NO_EXIST | false       | false            || "create analysis"                                                                  | true
    }

    protected BaseDeciderGroup createAnalysisDeciderGroup(AbstractBamFile bamFile) {
        return new BaseDeciderGroup(
                bamFile.individual,
                bamFile.seqType
        )
    }

    protected BaseDeciderGroup createAnalysisDeciderGroup(T analysis) {
        return new BaseDeciderGroup(
                analysis.individual,
                analysis.seqType
        )
    }

    protected AnalysisBamFileArtefactData createAnalysisBamFileArtefactData(AbstractBamFile bamFile) {
        AbstractMergingWorkPackage mergingWorkPackage = bamFile.mergingWorkPackage
        SeqPlatformGroup seqPlatformGroup = mergingWorkPackage.class.isAssignableFrom(MergingWorkPackage) ?
                ((MergingWorkPackage) mergingWorkPackage).seqPlatformGroup : null
        return new AnalysisBamFileArtefactData(
                bamFile.workflowArtefact,
                bamFile,
                bamFile.config?.programVersion ?: bamFile.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion,
                bamFile.project,
                bamFile.seqType,
                bamFile.individual,
                bamFile.sampleType,
                bamFile.sample,
                mergingWorkPackage,
                mergingWorkPackage.referenceGenome,
                seqPlatformGroup
        )
    }

    protected <T extends BamFilePairAnalysis> AnalysisAnalysisArtefactData<T> createAnalysisAnalysisArtefactData(T analysis) {
        return new AnalysisAnalysisArtefactData(
                analysis.workflowArtefact,
                analysis,
                analysis.sampleType1BamFile?.config?.programVersion ?: analysis.workflowArtefact?.producedBy?.workflowVersion?.workflowVersion,
                analysis.project,
                analysis.seqType,
                analysis.samplePair,
                analysis.individual,
                analysis.sampleType1BamFile.sampleType,
                analysis.sampleType2BamFile.sampleType,
                analysis.sampleType1BamFile.sample,
                analysis.sampleType2BamFile.sample,
                analysis.sampleType1BamFile,
                analysis.sampleType2BamFile,
        )
    }

    void setupDataForCreateWorkflowRunsAndOutputArtefacts(CreatePairVariant variant, Map<String, ?> adaptation) {
        workflow = createWorkflow(name: decider.workflowName)
        pipeline = findOrCreateAnalysisPipeline()

        // artefact data
        bamFileDisease = createBamFile([
                workflowArtefact: createWorkflowArtefact(),
                config          : null,
        ])
        bamFileControl = createCorrespondingBamFile(bamFileDisease, [
                workflowArtefact: createWorkflowArtefact(),
                config          : null,
        ])

        workflowApiVersion = createWorkflowApiVersion([workflow: workflow])
        workflowVersionOld = createWorkflowVersion([apiVersion: createWorkflowApiVersion(workflow: workflow)])
        workflowVersion = createWorkflowVersion([
                apiVersion             : workflowApiVersion,
                allowedReferenceGenomes: [
                        bamFileDisease.mergingWorkPackage.referenceGenome,
                        bamFileControl.mergingWorkPackage.referenceGenome,
                        createReferenceGenome(),
                ],
        ])

        if (variant == CreateVariantInvalid.DIFFERENT_SEQ_PLATFORM_GROUP) {
            bamFileControl.mergingWorkPackage.with {
                it.seqPlatformGroup = createSeqPlatformGroup()
                it.save(flush: true)
            }
        }
        RoddyBamFile bamFileAdditional = createCorrespondingBamFile(bamFileDisease, [
                workflowArtefact: createWorkflowArtefact(),
        ])
        AnalysisBamFileArtefactData artefactDataDisease = createAnalysisBamFileArtefactData(bamFileDisease)
        AnalysisBamFileArtefactData artefactDataControl = createAnalysisBamFileArtefactData(bamFileControl)
        AnalysisBamFileArtefactData artefactDataAdditional = createAnalysisBamFileArtefactData(bamFileAdditional)

        dataList = new AnalysisArtefactDataList([], [], [:])

        // additional artefact data
        additionalDataList = new AnalysisArtefactDataList([], [], [:])

        // additional data: sample pair
        Map<AnalysisGroup, SamplePair> samplePairMap = [:]

        // additional data: fetchCategoryPerSampleTypeAndProject
        ProjectSampleTypeGroup projectSampleTypeGroup1 = new ProjectSampleTypeGroup(bamFileDisease.project, bamFileDisease.sampleType)
        ProjectSampleTypeGroup projectSampleTypeGroup2 = new ProjectSampleTypeGroup(bamFileControl.project, bamFileControl.sampleType)
        ProjectSampleTypeGroup projectSampleTypeGroup3 = new ProjectSampleTypeGroup(bamFileAdditional.project, bamFileAdditional.sampleType)
        Map<ProjectSampleTypeGroup, SampleTypePerProject.Category> categoryMap = [
                (projectSampleTypeGroup1): variant == CreateVariantInvalid.NO_DISEASE ? SampleTypePerProject.Category.CONTROL : SampleTypePerProject.Category.DISEASE,
                (projectSampleTypeGroup2): variant == CreateVariantInvalid.NO_CONTROL ? SampleTypePerProject.Category.DISEASE : SampleTypePerProject.Category.CONTROL,
        ]

        // additional data:
        additionalData = new AnalysisAdditionalData(samplePairMap, categoryMap, pipeline)

        // others
        projectSeqTypeGroup = new ProjectSeqTypeGroup(bamFileDisease.project, bamFileDisease.seqType)
        baseDeciderGroup = createAnalysisDeciderGroup(bamFileDisease)

        switch (variant) {
            case CreateVariantInvalid.NO_DISEASE_CONTROL:
                additionalDataList.bamFileDataList << artefactDataDisease << artefactDataControl
                break
            case CreateVariantValidPair.DISEASES_GIVEN:
                dataList.bamFileDataList << artefactDataDisease
                additionalDataList.bamFileDataList << artefactDataControl
                break
            case CreateVariantValidPair.CONTROL_GIVEN:
                dataList.bamFileDataList << artefactDataControl
                additionalDataList.bamFileDataList << artefactDataDisease
                break
            case CreateVariantValidPair.CATEGORY_IGNORED:
                categoryMap[projectSampleTypeGroup3] = SampleTypePerProject.Category.IGNORED
                dataList.bamFileDataList << artefactDataDisease << artefactDataControl << artefactDataAdditional
                break
            case CreateVariantValidPair.CATEGORY_UNDEFINED:
                categoryMap[projectSampleTypeGroup3] = SampleTypePerProject.Category.UNDEFINED
                dataList.bamFileDataList << artefactDataDisease << artefactDataControl << artefactDataAdditional
                break
            case CreateVariantValidPair.CATEGORY_NONE:
                dataList.bamFileDataList << artefactDataDisease << artefactDataControl << artefactDataAdditional
                break
            case CreateVariantValidPair.BOTH_GIVEN:
            default:
                dataList.bamFileDataList << artefactDataDisease << artefactDataControl
                break
        }

        if (variant != CreateVariantValidPair.SAMPLE_PAIR_NO_EXIST) {
            SamplePair samplePair = factory.createSamplePair([
                    mergingWorkPackage1: bamFileDisease.workPackage,
                    mergingWorkPackage2: bamFileControl.workPackage,
            ])
            AnalysisGroup analysisGroup = new AnalysisGroup(bamFileDisease.workPackage, bamFileControl.workPackage)
            samplePairMap[analysisGroup] = samplePair

            switch (variant) {
                case CreateVariantInvalid.EXISTING_ANALYSIS:
                    if (adaptation.existingAnalysis) {
                        // create existing analysis with proper workflow version
                        T existingAnalysis = factory.createInstance([
                                samplePair        : samplePair,
                                sampleType1BamFile: bamFileDisease,
                                sampleType2BamFile: bamFileControl,
                                workflowArtefact  : createWorkflowArtefact([
                                        artefactType: decider.artefactType,
                                        producedBy  : createWorkflowRun([
                                                workflowVersion: adaptation.sameVersion ? workflowVersion : workflowVersionOld,
                                                project        : bamFileDisease.project,
                                        ])
                                ]),
                        ])
                        additionalDataList.alreadyRunAnalysisDataList << createAnalysisAnalysisArtefactData(existingAnalysis)
                    } else {
                        additionalDataList.alreadyRunAnalysisDataList << createAnalysisAnalysisArtefactData(factory.createInstance([
                                samplePair        : samplePair,
                                sampleType1BamFile: bamFileDisease,
                                sampleType2BamFile: bamFileControl,
                        ]))
                    }
                    break
                case CreateVariantValidPair.ANALYSIS_WITH_SAME_CONTROL:
                    additionalDataList.alreadyRunAnalysisDataList << createAnalysisAnalysisArtefactData(factory.createInstanceWithRoddyBamFiles([
                            samplePair        : samplePair,
                            sampleType2BamFile: bamFileControl,
                    ]))
                    break
                case CreateVariantValidPair.ANALYSIS_WITH_SAME_DISEASE:
                    additionalDataList.alreadyRunAnalysisDataList << createAnalysisAnalysisArtefactData(factory.createInstanceWithRoddyBamFiles([
                            samplePair        : samplePair,
                            sampleType1BamFile: bamFileDisease,
                    ]))
                    break
                case CreateVariantValidPair.INDEPENDENT_ANALYSIS:
                    additionalDataList.alreadyRunAnalysisDataList << createAnalysisAnalysisArtefactData(factory.createInstanceWithRoddyBamFiles([
                            samplePair: samplePair,
                    ]))
                    break
            }
        }

        // services
        TestConfigService configService = new TestConfigService()
        configService.fixClockTo()

        decider.workflowRunService = new WorkflowRunService()
        decider.workflowArtefactService = new WorkflowArtefactService()
        decider.configService = new TestConfigService()
    }

    protected T createAnalysisInstance(Map properties = [:]) {
        return factory.createInstanceWithRoddyBamFiles(properties)
    }

    protected Pipeline findOrCreateAnalysisPipeline() {
        return findOrCreatePipeline(decider.pipelineName, decider.pipelineName.type)
    }

    /**
     * Helper to define the variants for the unroll
     */
    static protected interface CreatePairVariant {
        abstract String getMessage()
    }

    /**
     * Defines valid variants for the unroll
     */
    @TupleConstructor
    protected enum CreateVariantValidPair implements CreatePairVariant {
        BOTH_GIVEN(''),
        DISEASES_GIVEN(''),
        CONTROL_GIVEN(''),
        SAMPLE_PAIR_NO_EXIST(''),
        ANALYSIS_WITH_SAME_DISEASE(''),
        ANALYSIS_WITH_SAME_CONTROL(''),
        INDEPENDENT_ANALYSIS(''),
        CATEGORY_UNDEFINED("since category 'UNDEFINED' defined"),
        CATEGORY_IGNORED("since category 'IGNORED' defined"),
        CATEGORY_NONE("since no category defined")

        String message
    }

    /**
     * Defines invalid variants for the unroll
     */
    @TupleConstructor
    protected enum CreateVariantInvalid implements CreatePairVariant {
        NO_DISEASE_CONTROL("since no BAM files with category DISEASE or CONTROL"),
        NO_DISEASE("since no sample pairs available"),
        NO_CONTROL("since no sample pairs available"),
        DIFFERENT_SEQ_PLATFORM_GROUP("since they use different seqPlatformGroups"),
        EXISTING_ANALYSIS("analysis"),
        REF_GENOME_NOT_SUPPORTED("since the reference genome")

        String message
    }
}
