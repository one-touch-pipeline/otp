/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.decider.*

/**
 * The AbstractAnalysisDecider is an abstract class that provides a blueprint for creating specific analysis deciders in the application.
 * It extends the AbstractWorkflowDecider class and is parameterized with a type A that extends BamFilePairAnalysis.
 */
@Transactional
@Slf4j
abstract class AbstractAnalysisDecider<A extends BamFilePairAnalysis>
        extends AbstractWorkflowDecider<AnalysisArtefactDataList, BaseDeciderGroup, AnalysisAdditionalData> {

    @Autowired
    AnalysisArtefactService analysisArtefactService

    @Autowired
    ConfigService configService

    @Autowired
    PipelineService pipelineService

    @Autowired
    WorkflowArtefactService workflowArtefactService

    @Autowired
    WorkflowRunService workflowRunService

    /**
     * returns the workFileService for the analysis
     */
    abstract AbstractAnalysisWorkFileService<A> getWorkFileService()

    /**
     * Returns the name of the workflow that is used for the analysis.
     */
    abstract String getWorkflowName()

    /**
     * Returns the class of the analysis that is created by the decider.
     */
    abstract Class<A> getInstanceClass()

    /**
     * Returns the artefact type of the analysis that is created by the decider.
     */
    abstract ArtefactType getArtefactType()

    /**
     * Returns the name of the pipeline that is used for the analysis.
     */
    abstract Pipeline.Name getPipelineName()

    /**
     * Creates an analysis without flushing it to the database.
     */
    abstract BamFilePairAnalysis createAnalysisWithoutFlush(Map properties)

    @Override
    final protected Workflow getWorkflow() {
        return workflowService.getExactlyOneWorkflow(workflowName)
    }

    @Override
    final protected Set<ArtefactType> getSupportedInputArtefactTypes() {
        return [ArtefactType.BAM] as Set
    }

    @Override
    protected AnalysisArtefactDataList fetchInputArtefacts(Collection<WorkflowArtefact> inputArtefacts, Set<SeqType> seqTypes) {
        return new AnalysisArtefactDataList(
                analysisArtefactService.fetchBamFileArtefacts(inputArtefacts, seqTypes),
                []
        )
    }

    @Override
    protected AnalysisArtefactDataList fetchAdditionalArtefacts(AnalysisArtefactDataList inputArtefactDataList) {
        List<AbstractBamFile> bamFiles = inputArtefactDataList.bamFileDataList*.artefact
        List<AnalysisBamFileArtefactData> dataBamFiles =
                analysisArtefactService.fetchRelatedBamFilesArtefactsForBamFiles(bamFiles)

        List<AnalysisAnalysisArtefactData<BamFilePairAnalysis>> analysisData =
                analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles(bamFiles, instanceClass)

        return new AnalysisArtefactDataList(dataBamFiles, analysisData)
    }

    @Override
    protected AnalysisAdditionalData fetchAdditionalData(AnalysisArtefactDataList inputArtefactDataList, Workflow workflow) {
        List<AbstractBamFile> bamFiles = inputArtefactDataList.bamFileDataList*.artefact
        if (!bamFiles) {
            return new AnalysisAdditionalData([:], [:], null)
        }
        return new AnalysisAdditionalData(
                analysisArtefactService.fetchSamplePairs(bamFiles),
                analysisArtefactService.fetchCategoryPerSampleTypeAndProject(bamFiles),
                pipelineService.findByPipelineName(pipelineName),
        )
    }

    @Override
    protected List<WorkflowVersionSelector> fetchWorkflowVersionSelector(AnalysisArtefactDataList inputArtefactDataList, Workflow workflow) {
        List<AbstractBamFile> bamFiles = inputArtefactDataList.bamFileDataList*.artefact
        return analysisArtefactService.fetchWorkflowVersionSelectorForBamFiles(workflow, bamFiles)
    }

    @Override
    protected Map<BaseDeciderGroup, AnalysisArtefactDataList> groupData(AnalysisArtefactDataList inputArtefactDataList,
                                                                        AnalysisAdditionalData additionalData,
                                                                        Map<String, String> userParams) {

        Map<BaseDeciderGroup, AnalysisArtefactDataList> map = [:].withDefault {
            new AnalysisArtefactDataList([], [])
        }
        inputArtefactDataList.bamFileDataList.each {
            map[createAnalysisDeciderGroup(it)].bamFileDataList << it
        }
        inputArtefactDataList.alreadyRunAnalysisDataList.each {
            map[createAnalysisDeciderGroup(it)].alreadyRunAnalysisDataList << it
        }
        return map
    }

    protected BaseDeciderGroup createAnalysisDeciderGroup(AnalysisBamFileArtefactData data) {
        return new BaseDeciderGroup(data.individual, data.seqType)
    }

    protected BaseDeciderGroup createAnalysisDeciderGroup(AnalysisAnalysisArtefactData<?> data) {
        return new BaseDeciderGroup(data.individual, data.seqType)
    }

    @Override
    protected DeciderResult createWorkflowRunsAndOutputArtefacts(ProjectSeqTypeGroup projectSeqTypeGroup, BaseDeciderGroup group,
                                                                 AnalysisArtefactDataList givenArtefacts, AnalysisArtefactDataList additionalArtefacts,
                                                                 AnalysisAdditionalData analysisAdditionalData, WorkflowVersion workflowVersion) {
        DeciderResult deciderResult = new DeciderResult()
        deciderResult.infos << "process group ${group}".toString()

        Map<SampleTypePerProject.Category, Map<SampleType, List<AnalysisBamFileArtefactData>>> givenBamFilesMap =
                groupAndFilter(givenArtefacts, analysisAdditionalData, deciderResult)

        if (!givenBamFilesMap) {
            deciderResult.warnings << """skip ${group}, since no BAM files with category DISEASE or CONTROL""".toString()
            return deciderResult
        }

        Set<AnalysisBamFileArtefactData> bamFileDataSet = givenArtefacts.bamFileDataList as Set

        AnalysisArtefactDataList allArtefacts = new AnalysisArtefactDataList(
                givenArtefacts.bamFileDataList + additionalArtefacts.bamFileDataList,
                givenArtefacts.alreadyRunAnalysisDataList + additionalArtefacts.alreadyRunAnalysisDataList,
        )
        Map<SampleTypePerProject.Category, Map<SampleType, List<AnalysisBamFileArtefactData>>> allDataGrouped =
                groupByCategoryAndSampleType(allArtefacts, analysisAdditionalData)

        Map<SampleType, List<AnalysisBamFileArtefactData>> diseaseData = allDataGrouped[SampleTypePerProject.Category.DISEASE]
        Map<SampleType, List<AnalysisBamFileArtefactData>> controlData = allDataGrouped[SampleTypePerProject.Category.CONTROL]

        if (!diseaseData || !controlData) {
            deciderResult.warnings << """skip ${group}, since no sample pairs available""".toString()
            return deciderResult
        }

        // generating the maps ensue one bamFile per sample type
        diseaseData.each { SampleType sampleTypeDisease, List<AnalysisBamFileArtefactData> bamFileDiseaseList ->
            controlData.each { SampleType sampleTypeControl, List<AnalysisBamFileArtefactData> bamFileControlList ->
                if (bamFileDataSet.contains(bamFileDiseaseList.first()) || bamFileDataSet.contains(bamFileControlList.first())) {
                    createSingleAnalysis(group, bamFileDiseaseList.first(), bamFileControlList.first(), allArtefacts, analysisAdditionalData,
                            workflowVersion, deciderResult)
                }
            }
        }
        return deciderResult
    }

    @SuppressWarnings("ParameterCount")
    private void createSingleAnalysis(BaseDeciderGroup group, AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData,
                                      AnalysisArtefactDataList allArtefacts, AnalysisAdditionalData analysisAdditionalData,
                                      WorkflowVersion workflowVersion, DeciderResult deciderResult) {

        BamFilePairAnalysis existingAnalysis = findExistingAnalysis(allArtefacts, diseaseData, controlData)

        if (existingAnalysis) {
            deciderResult.warnings << ("skip ${group} ${diseaseData.sampleType.displayName} ${controlData.sampleType.displayName}, " +
                    "since existing analysis ${workflowName} for the same bam file pair exist").toString()
            return
        }

        if (diseaseData.seqPlatformGroup && controlData.seqPlatformGroup && diseaseData.seqPlatformGroup != controlData.seqPlatformGroup) {
            deciderResult.warnings << ("skip ${group} ${diseaseData.sampleType.displayName} ${controlData.sampleType.displayName}, " +
                    "since they use different seqPlatformGroups: ${diseaseData.seqPlatformGroup} and ${controlData.seqPlatformGroup}").toString()
            return
        }

        AnalysisGroup analysisGroup = new AnalysisGroup(diseaseData.mergingWorkPackage, controlData.mergingWorkPackage)
        SamplePair samplePair = analysisAdditionalData.samplePairMap[analysisGroup] ?: new SamplePair(
                mergingWorkPackage1: diseaseData.mergingWorkPackage,
                mergingWorkPackage2: controlData.mergingWorkPackage,
        ).save(flush: true, deepValidate: false)

        WorkflowRun run = createWorkflowRun(workflowVersion, diseaseData, controlData)

        WorkflowArtefact workflowOutputArtefact = workflowArtefactService.buildWorkflowArtefact(new WorkflowArtefactValues(
                run,
                AbstractAnalysisWorkflow.ANALYSIS_OUTPUT,
                artefactType,
                createDisplayName(diseaseData, controlData),
        ))

        BamFilePairAnalysis analysis = createAnalysisWithoutFlush([
                workflowArtefact  : workflowOutputArtefact,
                samplePair        : samplePair,
                sampleType1BamFile: diseaseData.artefact,
                sampleType2BamFile: controlData.artefact,
                instanceName      : workFileService.constructInstanceName(workflowVersion),
        ])

        deciderResult.infos << "--> create analysis ${analysis}".toString()
        deciderResult.newArtefacts << workflowOutputArtefact
    }

    private WorkflowRun createWorkflowRun(WorkflowVersion workflowVersion, AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData) {
        List<String> displayName = createDisplayName(diseaseData, controlData)
        String shortName = createShortDisplayName(diseaseData, controlData)

        WorkflowRun run = workflowRunService.buildWorkflowRun(
                workflowVersion.workflow,
                diseaseData.project.processingPriority,
                "",
                diseaseData.project,
                displayName,
                shortName,
                workflowVersion,
        )

        [
                (AbstractAnalysisWorkflow.INPUT_TUMOR_BAM)  : diseaseData,
                (AbstractAnalysisWorkflow.INPUT_CONTROL_BAM): controlData,
        ].each {
            new WorkflowRunInputArtefact(
                    workflowRun: run,
                    role: it.key,
                    workflowArtefact: it.value.workflowArtefact,
            ).save(flush: false, deepValidate: false)
        }

        return run
    }

    private String createShortDisplayName(AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData) {
        return ("${workflowName}: ${diseaseData.individual.pid} ${diseaseData.sampleType.displayName} ${controlData.sampleType.displayName} " +
                "${diseaseData.seqType.displayNameWithLibraryLayout}").toString()
    }

    private List<String> createDisplayName(AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData) {
        return [
                "project: ${diseaseData.project.name}",
                "individual: ${diseaseData.individual.displayName}",
                "seqType: ${diseaseData.seqType.displayNameWithLibraryLayout}",
                "sampleType1: ${diseaseData.sampleType.displayName}",
                "sampleType2: ${controlData.sampleType.displayName}",
        ]*.toString()
    }

    private BamFilePairAnalysis findExistingAnalysis(AnalysisArtefactDataList allArtefacts,
                                                     AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData) {
        return allArtefacts.alreadyRunAnalysisDataList.find {
            BamFilePairAnalysis analysis = it.artefact
            analysis.sampleType1BamFile == diseaseData.artefact && analysis.sampleType2BamFile == controlData.artefact
        }?.artefact
    }

    private Map<SampleTypePerProject.Category, Map<SampleType, List<AnalysisBamFileArtefactData>>> groupAndFilter(
            AnalysisArtefactDataList artefactsDataList, AnalysisAdditionalData additionalData, DeciderResult deciderResult) {
        Map<SampleTypePerProject.Category, Map<SampleType, List<AnalysisBamFileArtefactData>>> groupedData =
                groupByCategoryAndSampleType(artefactsDataList, additionalData)
        createWarningForSomeCategoriesAndRemoveThem(groupedData, deciderResult)
        groupedData.values().each {
            it.values().each {
                assert it.size() == 1: "Multiple bam not yet supported: ${it}"
            }
        }

        return groupedData
    }

    private Map<SampleTypePerProject.Category, Map<SampleType, List<AnalysisBamFileArtefactData>>> groupByCategoryAndSampleType(
            AnalysisArtefactDataList artefactsDataList, AnalysisAdditionalData additionalData) {
        return artefactsDataList.bamFileDataList.groupBy([
                { AnalysisBamFileArtefactData data ->
                    additionalData.categoryMap[new ProjectSampleTypeGroup(data.project, data.sampleType)]
                },
                { AnalysisBamFileArtefactData data ->
                    data.sampleType
                },
        ])
    }

    private void createWarningForSomeCategoriesAndRemoveThem(
            Map<SampleTypePerProject.Category, Map<SampleType, List<AnalysisBamFileArtefactData>>> groupedData,
            DeciderResult deciderResult) {
        [
                null,
                SampleTypePerProject.Category.UNDEFINED,
                SampleTypePerProject.Category.IGNORED,
        ].each {
            createWarningForCategoryAndRemoveIt(it, groupedData, deciderResult)
        }
    }

    private void createWarningForCategoryAndRemoveIt(
            SampleTypePerProject.Category category, Map<SampleTypePerProject.Category, Map<SampleType, List<AnalysisBamFileArtefactData>>> groupedData,
            DeciderResult deciderResult) {
        Map<SampleType, List<AnalysisBamFileArtefactData>> perCategory = groupedData[category]
        if (perCategory) {
            perCategory.each {
                String categoryString = category ? "category '${category}'" : "no category"
                deciderResult.warnings << "exclude bamFile ${it.value.artefact}, since ${categoryString} defined".toString()
            }
            groupedData.remove(category)
        }
    }
}
