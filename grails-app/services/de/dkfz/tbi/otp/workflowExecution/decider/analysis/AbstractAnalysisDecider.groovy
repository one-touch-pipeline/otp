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
     * Returns the map of additional analysis with role name and analysis this workflow depends on.
     */
    abstract Map<String, Class<? extends BamFilePairAnalysis>> getDependingAnalysisInstanceClass()

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
                [],
                [:]
        )
    }

    @Override
    protected AnalysisArtefactDataList fetchAdditionalArtefacts(AnalysisArtefactDataList inputArtefactDataList) {
        List<AbstractBamFile> bamFiles = inputArtefactDataList.bamFileDataList*.artefact
        List<AnalysisBamFileArtefactData> dataBamFiles =
                analysisArtefactService.fetchRelatedBamFilesArtefactsForBamFiles(bamFiles)

        List<AnalysisAnalysisArtefactData<BamFilePairAnalysis>> analysisData =
                analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles(bamFiles, instanceClass)

        Map<String, List<AnalysisAnalysisArtefactData<BamFilePairAnalysis>>> dependingAnalysisData =
                dependingAnalysisInstanceClass.collectEntries {
                    [(it.key): analysisArtefactService.fetchRelatedAnalysisArtefactsForBamFiles(bamFiles, it.value)]
                }

        return new AnalysisArtefactDataList(dataBamFiles, analysisData, dependingAnalysisData)
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
            new AnalysisArtefactDataList([], [], [:].withDefault { [] })
        }
        inputArtefactDataList.bamFileDataList.each {
            map[createAnalysisDeciderGroup(it)].bamFileDataList << it
        }
        inputArtefactDataList.alreadyRunAnalysisDataList.each {
            map[createAnalysisDeciderGroup(it)].alreadyRunAnalysisDataList << it
        }
        dependingAnalysisInstanceClass.each { String role, Class<?> dependingAnalysis ->
            inputArtefactDataList.dependingAnalysisDataList.getOrDefault(role, []).each {
                map[createAnalysisDeciderGroup(it)].dependingAnalysisDataList[role] << it
            }
        }
        return map
    }

    protected BaseDeciderGroup createAnalysisDeciderGroup(AnalysisBamFileArtefactData data) {
        return new BaseDeciderGroup(data.individual, data.seqType)
    }

    protected BaseDeciderGroup createAnalysisDeciderGroup(AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis> data) {
        return new BaseDeciderGroup(data.individual, data.seqType)
    }

    @Override
    protected DeciderResult createWorkflowRunsAndOutputArtefacts(ProjectSeqTypeGroup projectSeqTypeGroup, BaseDeciderGroup group,
                                                                 AnalysisArtefactDataList givenArtefacts, AnalysisArtefactDataList additionalArtefacts,
                                                                 AnalysisAdditionalData analysisAdditionalData, WorkflowVersion workflowVersion,
                                                                 Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = [:]) {
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
                dependingAnalysisInstanceClass.collectEntries {
                    Collection<? extends AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> list = []
                    list.addAll(givenArtefacts.dependingAnalysisDataList.getOrDefault(it.key, []))
                    list.addAll(additionalArtefacts.dependingAnalysisDataList.getOrDefault(it.key, []))
                    [(it.key): list]
                },
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
                            workflowVersion, deciderResult, deciderAction)
                }
            }
        }
        return deciderResult
    }

    // codenarc thinks, the method would return a boolean and therefore report the return statements
    @SuppressWarnings(["ParameterCount", "AbcMetric", "BooleanMethodReturnsNull"])
    void createSingleAnalysis(BaseDeciderGroup group, AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData,
                              AnalysisArtefactDataList allArtefacts, AnalysisAdditionalData analysisAdditionalData,
                              WorkflowVersion workflowVersion, DeciderResult deciderResult,
                              Map<Class<? extends Decider>, DeciderCreateWorkflowAction> deciderAction = [:]) {

        Collection<AnalysisAnalysisArtefactData> existingAnalysisData = findExistingAnalysis(allArtefacts.alreadyRunAnalysisDataList, diseaseData, controlData)

        if (existingAnalysisData) {
            DeciderCreateWorkflowAction action = deciderAction[getClass()]
            switch (action) {
                case DeciderCreateWorkflowAction.CREATE_ALWAYS:
                    deciderResult.warnings << "recreate ${group}, since action is CREATE_ALWAYS".toString()
                    break
                case DeciderCreateWorkflowAction.CREATE_MISSING_AND_NEWER:
                    // Check if any existing analysis have the same workflow version
                    boolean sameVersionExists = existingAnalysisData.any { it.version == workflowVersion.workflowVersion }
                    if (sameVersionExists) {
                        deciderResult.warnings << ("skip ${group}, since existing ${artefactType} analysis with the same version was found, " +
                                "and action is CREATE_MISSING_AND_NEWER").toString()
                        return
                    }
                    deciderResult.warnings << ("recreate ${group}, since the existing ${artefactType} analysis has a different version, " +
                            "and action is CREATE_MISSING_AND_NEWER").toString()
                    break
                case DeciderCreateWorkflowAction.CREATE_MISSING:
                default:
                    deciderResult.warnings << "skip ${group}, since existing ${artefactType} analysis was found and action is CREATE_MISSING".toString()
                    return
            }
        }

        Map<String, AnalysisAnalysisArtefactData> additionalAnalysis = dependingAnalysisInstanceClass.collectEntries {
            Collection<? extends AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> analysis = allArtefacts.dependingAnalysisDataList?.get(it.key)
            Collection<? extends AnalysisAnalysisArtefactData<? extends BamFilePairAnalysis>> existingAnalysis = analysis ?
                    findExistingAnalysis(analysis, diseaseData, controlData) : null
            [(it.key): existingAnalysis ? existingAnalysis.sort { it.artefact.id }.last() : null]
        }

        List<String> missingDependencies = additionalAnalysis.findAll { !it.value }*.key
        if (missingDependencies) {
            deciderResult.warnings << ("skip ${group} ${diseaseData.sampleType.displayName} ${controlData.sampleType.displayName}, " +
                    "since depending analysis ${missingDependencies.join(' and ')} ${missingDependencies.size() == 1 ? 'is' : 'are'} not available").toString()
            return
        }

        if (diseaseData.seqPlatformGroup && controlData.seqPlatformGroup && diseaseData.seqPlatformGroup != controlData.seqPlatformGroup) {
            deciderResult.warnings << ("skip ${group} ${diseaseData.sampleType.displayName} ${controlData.sampleType.displayName}, " +
                    "since they use different seqPlatformGroups: ${diseaseData.seqPlatformGroup} and ${controlData.seqPlatformGroup}").toString()
            return
        }

        // reference genomes in both disease and control bam files should be in the list of allowed reference genomes of the workflow version
        List<GString> warnings = [diseaseData, controlData].collect { AnalysisBamFileArtefactData analysisBamFileArtefactData ->
            analysisBamFileArtefactData.referenceGenome in workflowVersion.allowedReferenceGenomes ? null :
                    "skip ${group} ${analysisBamFileArtefactData.sampleType.displayName}, " +
                            "since the reference genome ${analysisBamFileArtefactData.referenceGenome} is not supported for the current workflow"
        }.findAll { it }

        if (warnings) {
            warnings.each {
                deciderResult.warnings << it.toString()
            }
            return
        }

        AnalysisGroup analysisGroup = new AnalysisGroup(diseaseData.mergingWorkPackage, controlData.mergingWorkPackage)
        SamplePair samplePair = analysisAdditionalData.samplePairMap[analysisGroup] ?: new SamplePair(
                mergingWorkPackage1: diseaseData.mergingWorkPackage,
                mergingWorkPackage2: controlData.mergingWorkPackage,
        ).save(flush: true, deepValidate: false)

        WorkflowRun run = createWorkflowRun(workflowVersion, diseaseData, controlData, additionalAnalysis)

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
        run.save(flush: true, deepValidate: false)

        deciderResult.infos << "--> create analysis ${analysis}".toString()
        deciderResult.newArtefacts << workflowOutputArtefact
    }

    private WorkflowRun createWorkflowRun(WorkflowVersion workflowVersion, AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData,
                                          Map<String, AnalysisAnalysisArtefactData> additionalAnalysis) {
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

        Map<String, ArtefactData<? extends Artefact>> inputArtefact = [
                (AbstractAnalysisWorkflow.INPUT_TUMOR_BAM)  : diseaseData,
                (AbstractAnalysisWorkflow.INPUT_CONTROL_BAM): controlData,
        ]
        inputArtefact.putAll(additionalAnalysis)

        inputArtefact.each {
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

    private Collection<AnalysisAnalysisArtefactData> findExistingAnalysis(Collection<AnalysisAnalysisArtefactData<BamFilePairAnalysis>> analysisData,
                                                                          AnalysisBamFileArtefactData diseaseData, AnalysisBamFileArtefactData controlData) {
        return analysisData.findAll {
            BamFilePairAnalysis analysis = it.artefact
            analysis.sampleType1BamFile == diseaseData.artefact && analysis.sampleType2BamFile == controlData.artefact
        } as Collection<AnalysisAnalysisArtefactData>
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
