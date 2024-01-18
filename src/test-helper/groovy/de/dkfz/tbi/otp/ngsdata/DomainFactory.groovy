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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.*
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryInstance
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactoryInstance
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactoryInstance
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectRequestUser
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.exceptions.NotSupportedException

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.ZonedDateTime

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@SuppressWarnings('EmptyClass')
class DomainFactoryProxyCore implements DomainFactoryCore {
}

@SuppressWarnings('EmptyClass')
class DomainFactoryProxyRoddy implements IsRoddy {
}

@SuppressWarnings('EmptyClass')
class DomainFactoryProxyCellRanger implements CellRangerFactory {
}

class DomainFactory {

    private DomainFactory() {
    }

    // These objects are used to access the methods, of the traits they inherit, in a static context
    static DomainFactoryProxyCore proxyCore = new DomainFactoryProxyCore()
    static DomainFactoryProxyRoddy proxyRoddy = new DomainFactoryProxyRoddy()
    static DomainFactoryProxyCellRanger proxyCellRanger = new DomainFactoryProxyCellRanger()

    static final String DEFAULT_TAB_FILE_NAME = 'DefaultTabFileName.tab'
    static final String DEFAULT_CHROMOSOME_LENGTH_FILE_NAME = 'DefaultChromosomeLengthFileName.tsv'
    static final String DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY = 'exec_123456_123456789_test_test'
    static final String TEST_CONFIG_VERSION = 'v1_0'

    /**
     * Counter to create unique names.
     */
    static int counter = 0

    private
    static <T> T createDomainObject(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
        T domain = domainClass.newInstance()
        defaultProperties.each { String key, def value ->
            if (!parameterProperties.containsKey(key)) {
                if (value instanceof Closure) {
                    domain[key] = value()
                } else {
                    domain[key] = value
                }
            }
        }
        parameterProperties.each { String key, def value ->
            domain[key] = value
        }
        if (saveAndValidate) {
            assert domain.save(flush: true)
        }
        return domain
    }

    /**
     * returns a DomainObject of the desired class, matching all requiredSearchProperties,
     * either by re-using a pre-exiting one, or creating a new one.
     *
     * If no (or empty) search properties are provided, the first object from the DB is returned.
     *
     * @param requiredSearchProperties the properties ABSOLUTELY required by the test
     * @param defaultCreationProperties if no matching pre-existing object was found, use these parameters, combined
     * with the requiredSearchProperties, to create a new one (additional properties to make the creation pass validation)
     */
    private
    static <T> T findOrCreateDomainObject(Class<T> domainClass, Map defaultCreationProperties, Map requiredSearchProperties, boolean saveAndValidate = true) {
        assert requiredSearchProperties: 'Required search properties are missing.'

        return atMostOneElement(domainClass.findAllWhere(requiredSearchProperties)) ?: createDomainObject(domainClass, defaultCreationProperties, requiredSearchProperties, saveAndValidate)
    }

    static <T> T createDomainWithImportAlias(Class<T> domainClass, Map parameterProperties) {
        Map defaultProperties = [
                name       : 'metadataName' + (counter++),
                importAlias: [],
        ]
        return createDomainObject(domainClass, defaultProperties, parameterProperties)
    }

    static Role createRoleLazy(Map properties = [:]) {
        return findOrCreateDomainObject(Role, [
                authority: "ROLE_${counter++}",
        ], properties)
    }

    static Role createRoleAdminLazy() {
        return createRoleLazy([
                authority: Role.ROLE_ADMIN,
        ])
    }

    @Deprecated
    static User createUser(Map properties = [:]) {
        return UserDomainFactoryInstance.INSTANCE.createUser(properties)
    }

    /**
     * creates a Pipeline, Name and Type have to be given e.g. Pipeline.Name.PANCAN_ALIGNMENT Pipeline.Type.ALIGNMENT
     * @param ENUM name of the Pipeline
     * @param ENUM type of the Pipeline
     * @return returns a created Pipeline due to given Params
     */
    static Pipeline createPipeline(Pipeline.Name name, Pipeline.Type type) {
        return findOrCreateDomainObject(Pipeline, [:], [
                name: name,
                type: type,
        ])
    }

    static Pipeline createPanCanPipeline() {
        return createPipeline(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createRoddyRnaPipeline() {
        return createPipeline(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createRnaPipeline() {
        return createPipeline(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    @Deprecated
    static Pipeline createDefaultOtpPipeline() {
        return createPipeline(Pipeline.Name.DEFAULT_OTP, Pipeline.Type.ALIGNMENT)
    }

    @Deprecated
    static Pipeline createOtpSnvPipelineLazy() {
        return createPipeline(Pipeline.Name.OTP_SNV, Pipeline.Type.SNV)
    }

    static Pipeline createRoddySnvPipelineLazy() {
        return createPipeline(Pipeline.Name.RODDY_SNV, Pipeline.Type.SNV)
    }

    static Pipeline createIndelPipelineLazy() {
        return createPipeline(Pipeline.Name.RODDY_INDEL, Pipeline.Type.INDEL)
    }

    static Pipeline createAceseqPipelineLazy() {
        return createPipeline(Pipeline.Name.RODDY_ACESEQ, Pipeline.Type.ACESEQ)
    }

    static Pipeline createSophiaPipelineLazy() {
        return createPipeline(Pipeline.Name.RODDY_SOPHIA, Pipeline.Type.SOPHIA)
    }

    static Pipeline createExternallyProcessedPipelineLazy() {
        return createPipeline(Pipeline.Name.EXTERNALLY_PROCESSED, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createRunYapsaPipelineLazy() {
        return createPipeline(Pipeline.Name.RUN_YAPSA, Pipeline.Type.MUTATIONAL_SIGNATURE)
    }

    static Pipeline returnOrCreateAnyPipeline() {
        return (atMostOneElement(Pipeline.list(max: 1)) ?: createPanCanPipeline())
    }

    @Deprecated
    static Map<String, ?> getDefaultValuesForAbstractQualityAssessment() {
        return proxyCellRanger.defaultValuesForAbstractQualityAssessment
    }

    static JobExecutionPlan createJobExecutionPlan(Map properties = [:]) {
        return createDomainObject(JobExecutionPlan, [
                name       : "planName_${counter++}",
                planVersion: 0,
                obsoleted  : false,
        ], properties)
    }

    static Process createProcess(Map properties = [:]) {
        return createDomainObject(Process, [
                started         : new Date(),
                startJobClass   : "startJobClass",
                jobExecutionPlan: { createJobExecutionPlan() },
        ], properties)
    }

    static JobDefinition createJobDefinition(Map properties = [:]) {
        return createDomainObject(JobDefinition, [
                plan: { createJobExecutionPlan() },
                name: "name_${counter++}",
                bean: 'beanName',
        ], properties)
    }

    static ProcessParameter createProcessParameter(Map properties = [:]) {
        return createDomainObject(ProcessParameter, [
                process  : { createProcessingStepUpdate().process },
                value    : "${counter++}",
                className: "${counter++}",
        ], properties)
    }

    static ProcessingStep createProcessingStep(Map properties = [:]) {
        JobExecutionPlan jobExecutionPlan = properties.jobDefinition?.plan ?: properties.process?.jobExecutionPlan ?: createJobExecutionPlan()
        return createDomainObject(ProcessingStep, [
                jobDefinition: { createJobDefinition(plan: jobExecutionPlan) },
                jobClass     : 'someClass',
                process      : { createProcess(jobExecutionPlan: jobExecutionPlan) },
        ], properties)
    }

    static RestartedProcessingStep createRestartedProcessingStep(Map properties = [:]) {
        ProcessingStep original = properties.original ?: createProcessingStep()
        ProcessingStep next = createDomainObject(RestartedProcessingStep, [
                jobDefinition: original.jobDefinition,
                jobClass     : 'someClass',
                process      : original.process,
                original     : original,
        ], properties)
        if (!original.latestProcessingStepUpdate) {
            createProcessingStepUpdate(original, ExecutionState.CREATED)
        }
        createProcessingStepUpdate(original, ExecutionState.RESTARTED)
        if (original.previous) {
            original.previous.next = next
            original.previous.save(flush: true)
        }
        return next
    }

    static ProcessingStepUpdate createProcessingStepUpdate(Map properties = [:]) {
        if (!properties.containsKey('processingStep') && properties.containsKey('state') && properties.state != ExecutionState.CREATED) {
            properties.processingStep = createProcessingStepUpdate().processingStep
        }
        return createDomainObject(ProcessingStepUpdate, [
                processingStep: { createProcessingStep() },
                state         : {
                    properties.processingStep?.latestProcessingStepUpdate ? ExecutionState.STARTED : ExecutionState.CREATED
                },
                previous      : { properties.processingStep?.latestProcessingStepUpdate },
                date          : new Date(),
        ], properties)
    }

    static ProcessingError createProcessingError(Map properties = [:]) {
        return createDomainObject(ProcessingError, [
                errorMessage: "errorMessage_${counter++}",
        ], properties)
    }

    @Deprecated
    static ClusterJob createClusterJob(Map properties = [:]) {
        return WorkflowSystemDomainFactoryInstance.INSTANCE.createClusterJob([
                oldSystem: true,
        ] + properties)
    }

    @Deprecated
    static ProcessingOption createProcessingOptionLazy(Map properties = [:]) {
        return proxyCore.findOrCreateProcessingOption(properties)
    }

    @Deprecated
    static ProcessingOption createProcessingOptionLazy(OptionName optionName, String value, String type = null) {
        return proxyCore.findOrCreateProcessingOption(optionName, value, type)
    }

    static JobErrorDefinition createJobErrorDefinition(Map properties = [:]) {
        return createDomainObject(JobErrorDefinition, [
                type           : JobErrorDefinition.Type.MESSAGE,
                action         : JobErrorDefinition.Action.STOP,
                errorExpression: /.*/,
                jobDefinitions : {
                    (1..3).collect {
                        createJobDefinition()
                    }
                },
        ], properties)
    }

    static ProcessingOption createProcessingOptionForTicketPrefix(String prefix = "Prefix ${counter++}") {
        return createProcessingOptionLazy(
                name: OptionName.TICKET_SYSTEM_NUMBER_PREFIX,
                type: null,
                project: null,
                value: prefix,
        )
    }

    static ProcessingOption createProcessingOptionForTicketSystemEmail(String ticketSystemEmail = HelperUtils.randomEmail) {
        return createProcessingOptionLazy(
                name: OptionName.EMAIL_TICKET_SYSTEM,
                type: null,
                project: null,
                value: ticketSystemEmail,
        )
    }

    static ProcessingOption createProcessingOptionForEmailSenderSalutation(String message = "the service team${counter++}") {
        return createProcessingOptionLazy(
                name: OptionName.HELP_DESK_TEAM_NAME,
                type: null,
                project: null,
                value: message,
        )
    }

    static ProcessingOption createProcessingOptionBasePathReferenceGenome(String fileName = TestCase.uniqueNonExistentPath.path) {
        return createProcessingOptionLazy(
                name: OptionName.BASE_PATH_REFERENCE_GENOME,
                type: null,
                project: null,
                value: fileName,
        )
    }

    static Map getRandomBamFileProperties() {
        return [
                fileSize           : ++counter,
                md5sum             : HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
        ]
    }

    static Comment createComment(Map properties = [:]) {
        return createDomainObject(Comment, [
                comment         : "comment ${counter++}",
                author          : "author ${counter++}",
                modificationDate: { new Date() },
        ], properties)
    }

    @Deprecated
    static IlseSubmission createIlseSubmission(Map properties = [:], boolean saveAndValidate = true) {
        return proxyCore.createIlseSubmission(properties, saveAndValidate)
    }

    static ReferenceGenome createReferenceGenomeLazy() {
        return ReferenceGenome.find { true } ?: createReferenceGenome()
    }

    /**
     * @deprecated Use the {@link RoddyPanCancerFactory#createBamFile} method from the {@link RoddyPanCancerFactory} trait instead.
     */
    @Deprecated
    static <T> T createRoddyBamFile(Map properties = [:]) {
        return proxyRoddy.createBamFile(properties)
    }

    /**
     * Because RoddyMergedBamQa defines a unique constraint with 'class', the instance can only be created in integration tests.
     */
    static RoddyMergedBamQa createRoddyMergedBamQa(Map properties = [:]) {
        return createDomainObject(RoddyMergedBamQa, defaultValuesForAbstractQualityAssessment + roddyQualityAssessmentProperties, properties)
    }

    /**
     * Because RoddyMergedBamQa defines a unique constraint with 'class', the instance can only be created in integration tests.
     */
    static RoddyMergedBamQa createRoddyMergedBamQa(RoddyBamFile roddyBamFile, Map properties = [:]) {
        return createRoddyMergedBamQa([
                abstractBamFile: roddyBamFile,
                referenceLength: 1,
        ] + properties)
    }

    static Map roddyQualityAssessmentProperties = [
            abstractBamFile              : { createRoddyBamFile() },
            chromosome                   : RoddyQualityAssessment.ALL,
            insertSizeCV                 : 0,
            percentageMatesOnDifferentChr: 0,
            genomeWithoutNCoverageQcBases: 0,
    ]

    static RoddyLibraryQa createRoddyLibraryQa(Map properties = [:]) {
        return createDomainObject(RoddyLibraryQa, defaultValuesForAbstractQualityAssessment + roddyQualityAssessmentProperties + [
                libraryDirectoryName: "libraryDirectoryName_${counter++}",
        ], properties)
    }

    static RoddySingleLaneQa createRoddySingleLaneQa(Map properties = [:]) {
        return createDomainObject(RoddySingleLaneQa, defaultValuesForAbstractQualityAssessment + roddyQualityAssessmentProperties + [
                seqTrack: { createSeqTrack() },
        ], properties)
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties as the specified one but a different
     * {@link SampleType}.
     */
    static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base) {
        return createMergingWorkPackage(base, createSampleType())
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties as the specified one but a different
     * {@link SampleType}.
     */
    static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base, SampleType sampleType) {
        Sample sample = createSample(
                individual: base.individual,
                sampleType: sampleType,
        )
        return createMergingWorkPackage(base, [
                sample               : sample,
                referenceGenome      : base.referenceGenome,
                libraryPreparationKit: base.libraryPreparationKit,
        ])
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties as the specified one but a different
     * {@link Sample}.
     */
    static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base, Sample sample) {
        return createMergingWorkPackage(base, [sample: sample])
    }

    /**
     * Creates a {@link MergingWorkPackage} with the properties of the base MergingWorkPackage,
     * but properties can be overwritten in the properties map.
     */
    static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base, Map properties) {
        List<String> mergingProperties = [
                "sample",
                "seqType",
                "seqPlatformGroup",
                'referenceGenome',
                'statSizeFileName',
                'pipeline',
        ]
        if (!base.seqType.isWgbs()) {
            mergingProperties.add("libraryPreparationKit")
        }
        if (base.seqType.hasAntibodyTarget) {
            mergingProperties.add("antibodyTarget")
        }

        MergingWorkPackage mwp = new MergingWorkPackage((mergingProperties + []).collectEntries {
            [it, base."${it}"]
        } + properties)
        assert mwp.save(flush: true)
        return mwp
    }

    static SamplePair createSamplePair(Map properties = [:]) {
        AbstractMergingWorkPackage mergingWorkPackage1 = properties.mergingWorkPackage1 ?:
                properties.mergingWorkPackage2 ? createMergingWorkPackage(properties.mergingWorkPackage2) :
                        createMergingWorkPackage()
        createSampleTypePerProjectLazy(
                sampleType: mergingWorkPackage1.sampleType,
                project: mergingWorkPackage1.project,
                category: SampleTypePerProject.Category.DISEASE,
        )
        return createSamplePair(mergingWorkPackage1, properties)
    }

    static SamplePair createSamplePairPanCan(Map properties = [:]) {
        properties.mergingWorkPackage1 = properties.mergingWorkPackage1 ?:
                properties.mergingWorkPackage2 ? createMergingWorkPackage(properties.mergingWorkPackage2) :
                        createMergingWorkPackage([
                                pipeline: createPanCanPipeline(),
                        ])
        return createSamplePair(properties)
    }

    static SamplePair createSamplePair(AbstractMergingWorkPackage mergingWorkPackage1, Map properties = [:]) {
        return createSamplePair(
                mergingWorkPackage1,
                properties.mergingWorkPackage2 ?: createMergingWorkPackage(mergingWorkPackage1),
                properties)
    }

    private static final Map SAMPLE_TYPE_MAP = [
            project   : { createProject() },
            sampleType: { createSampleType() },
            category  : SampleTypePerProject.Category.DISEASE,
    ].asImmutable()

    static SampleTypePerProject createSampleTypePerProject(Map properties = [:]) {
        return createDomainObject(SampleTypePerProject, SAMPLE_TYPE_MAP, properties)
    }

    static SampleTypePerProject createSampleTypePerProjectLazy(Map properties = [:]) {
        return findOrCreateDomainObject(SampleTypePerProject, SAMPLE_TYPE_MAP, properties)
    }

    static SampleTypePerProject createSampleTypePerProjectForMergingWorkPackage(
            AbstractMergingWorkPackage mergingWorkPackage, SampleTypePerProject.Category category = SampleTypePerProject.Category.DISEASE) {
        return createSampleTypePerProject([
                project   : mergingWorkPackage.project,
                sampleType: mergingWorkPackage.sampleType,
                category  : category,
        ])
    }

    static SampleTypePerProject createSampleTypePerProjectForBamFile(
            AbstractBamFile bamFile, SampleTypePerProject.Category category = SampleTypePerProject.Category.DISEASE) {
        return createSampleTypePerProjectForMergingWorkPackage(bamFile.mergingWorkPackage, category)
    }

    static SamplePair createSamplePair(AbstractMergingWorkPackage mergingWorkPackage1, AbstractMergingWorkPackage mergingWorkPackage2, Map properties = [:]) {
        return createDomainObject(SamplePair, [
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ], properties)
    }

    static Map createProcessableSamplePair(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, [coverage: 30] + bamFile1Properties, [coverage: 30] + bamFile2Properties)

        SamplePair samplePair = map.samplePair
        AbstractBamFile bamFile1 = map.sampleType1BamFile
        AbstractBamFile bamFile2 = map.sampleType2BamFile
        bamFile1.mergingWorkPackage.bamFileInProjectFolder = bamFile1
        bamFile2.mergingWorkPackage.bamFileInProjectFolder = bamFile2

        RoddyWorkflowConfig roddyConfig = createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: createRoddySnvPipelineLazy()
        )

        createProcessingThresholdsForBamFile(bamFile1, [numberOfLanes: null])
        createProcessingThresholdsForBamFile(bamFile2, [numberOfLanes: null])

        return [
                samplePair : samplePair,
                bamFile1   : bamFile1,
                bamFile2   : bamFile2,
                roddyConfig: roddyConfig,
        ]
    }

    static SamplePair createSamplePairWithBamFiles() {
        MergingWorkPackage tumorMwp = createMergingWorkPackage(
                seqType: createWholeGenomeSeqType(),
                pipeline: createPanCanPipeline(),
                referenceGenome: createReferenceGenome(name: 'hs37d5')
        )
        AbstractBamFile bamFileTumor = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile(
                randomBamFileProperties + [coverage: 30.0], tumorMwp, RoddyBamFile)

        AbstractBamFile bamFileControl = AlignmentPipelineFactory.RoddyPanCancerFactoryInstance.INSTANCE.createRoddyBamFile(
                randomBamFileProperties + [coverage: 30.0], createMergingWorkPackage(bamFileTumor.mergingWorkPackage), RoddyBamFile)

        bamFileTumor.mergingWorkPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        createSampleTypePerProject(
                project: bamFileTumor.project,
                sampleType: bamFileTumor.sampleType,
                category: SampleTypePerProject.Category.DISEASE,
        )

        createSampleTypePerProject(
                project: bamFileControl.project,
                sampleType: bamFileControl.sampleType,
                category: SampleTypePerProject.Category.CONTROL,
        )

        SamplePair samplePair = createSamplePair(bamFileTumor.mergingWorkPackage, bamFileControl.mergingWorkPackage)

        createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: createSophiaPipelineLazy()
        )

        createProcessingThresholdsForBamFile(bamFileTumor, [numberOfLanes: null])
        createProcessingThresholdsForBamFile(bamFileControl, [numberOfLanes: null])

        createProcessingOptionLazy(
                name: OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                type: null,
                project: null,
                value: 'hs37d5, hs37d5_PhiX, hs37d5_GRCm38mm_PhiX, hs37d5+mouse, hs37d5_GRCm38mm',
        )

        return samplePair
    }

    static SamplePair createSamplePairWithExternallyProcessedBamFiles(boolean initPipelines = false, Map bamFileProperties = [:]) {
        ExternalMergingWorkPackage tumorMwp = createExternalMergingWorkPackage(
                seqType: createWholeGenomeSeqType(),
                pipeline: createExternallyProcessedPipelineLazy(),
        )

        ExternalMergingWorkPackage controlMwp = createExternalMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: createSample(
                        individual: tumorMwp.individual
                )
        )

        [
                tumorMwp,
                controlMwp,
        ].each {
            ExternallyProcessedBamFile bamFile = createExternallyProcessedBamFile(
                    randomBamFileProperties + [
                            workPackage      : it,
                            coverage         : 30.0,
                            insertSizeFile   : 'insertSize.txt',
                            maximumReadLength: 101,
                    ] + bamFileProperties,
            )
            bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
            assert bamFile.mergingWorkPackage.save(flush: true)
        }

        createSampleTypePerProjectForMergingWorkPackage(tumorMwp, SampleTypePerProject.Category.DISEASE)
        createSampleTypePerProjectForMergingWorkPackage(controlMwp, SampleTypePerProject.Category.CONTROL)

        SamplePair samplePair = createSamplePair(tumorMwp, controlMwp)

        if (initPipelines) {
            initAnalysisForSamplePair(samplePair)
        }

        return samplePair
    }

    /**
     * creates an instance of ExternallyProcessedBamFileQualityAssessment
     * with the needed quality control values for sophia
     */
    static ExternallyProcessedBamFileQualityAssessment createExternallyProcessedBamFileQualityAssessment(Map properties = [:], AbstractBamFile mbf) {
        return createDomainObject(ExternallyProcessedBamFileQualityAssessment, [
                properlyPaired    : 1919,
                pairedInSequencing: 2120,
                insertSizeMedian  : 406,
                insertSizeCV      : 23,
                abstractBamFile   : mbf,
        ], properties)
    }

    /**
     * create necessary initialising for the analysis pipelines for the sample Pair.
     *
     * This contains <ul>
     * <le> ProcessingThresholds </le>
     * <le> RoddyWorkflowConfig for the different roddy analysis pipelines</le>
     * <le> ProcessingOption with the allowed   processing options for the
     *
     * create processing thresholds for the merging workpa, the
     */
    static void initAnalysisForSamplePair(SamplePair samplePair) {
        [
                samplePair.mergingWorkPackage1,
                samplePair.mergingWorkPackage2,
        ].each {
            createProcessingThresholdsForMergingWorkPackage(it, [numberOfLanes: null, coverage: 10])
        }

        [
                createRoddySnvPipelineLazy(),
                createIndelPipelineLazy(),
                createSophiaPipelineLazy(),
                createAceseqPipelineLazy(),
        ].each {
            createRoddyWorkflowConfig(
                    seqType: samplePair.seqType,
                    project: samplePair.project,
                    pipeline: it
            )
        }
        createReferenceGenomeAndAnalysisProcessingOptions("${samplePair.mergingWorkPackage1.referenceGenome.name}, ${samplePair.mergingWorkPackage2.referenceGenome.name}")
    }

    static List<ProcessingOption> createReferenceGenomeAndAnalysisProcessingOptions(String value = null) {
        return [
                OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
                OptionName.PIPELINE_RUNYAPSA_REFERENCE_GENOME,
        ].collect {
            createProcessingOptionLazy(
                    name: it,
                    type: null,
                    project: null,
                    value: value ?: createReferenceGenome().name,
            )
        }
    }

    static SamplePair createDisease(AbstractMergingWorkPackage controlMwp) {
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(controlMwp)
        createSampleTypePerProject(project: controlMwp.project, sampleType: diseaseMwp.sampleType, category: SampleTypePerProject.Category.DISEASE)
        SamplePair samplePair = createSamplePair(diseaseMwp, controlMwp)
        return samplePair
    }

    @Deprecated
    static SnvConfig createSnvConfig(Map properties = [:]) {
        return createDomainObject(SnvConfig, [
                configuration : "configuration_${counter++}",
                programVersion: "1.0",
                seqType       : { createSeqType() },
                project       : { createProject() },
                pipeline      : { createOtpSnvPipelineLazy() },
        ], properties)
    }

    private
    static Map createAnalysisInstanceWithRoddyBamFilesMapHelper(Map properties, Map bamFile1Properties, Map bamFile2Properties) {
        Pipeline pipeline = createPanCanPipeline()

        SamplePair samplePair = properties.samplePair
        AbstractBamFile diseaseBamFile = properties.sampleType1BamFile
        AbstractBamFile controlBamFile = properties.sampleType2BamFile

        AbstractMergingWorkPackage diseaseWorkPackage = diseaseBamFile?.mergingWorkPackage
        AbstractMergingWorkPackage controlWorkPackage = controlBamFile?.mergingWorkPackage

        Collection<SeqTrack> diseaseSeqTracks = bamFile1Properties.seqTracks ?: []
        Collection<SeqTrack> controlSeqTracks = bamFile2Properties.seqTracks ?: []

        SeqType seqType = CollectionUtils.atMostOneElement([
                samplePair?.seqType,
                diseaseWorkPackage?.seqType,
                controlWorkPackage?.seqType,
                diseaseSeqTracks*.seqType,
                controlSeqTracks*.seqType,
        ].findAll().flatten().unique(), "All sources have to contain the same seqType") ?: createWholeGenomeSeqType()

        Sample diseaseSample = CollectionUtils.atMostOneElement([
                samplePair?.mergingWorkPackage1?.sample,
                diseaseWorkPackage?.sample,
                diseaseSeqTracks*.sample,
        ].findAll().flatten().unique(), "All disease sources have to contain the same sample")

        Sample controlSample = CollectionUtils.atMostOneElement([
                samplePair?.mergingWorkPackage2?.sample,
                controlWorkPackage?.sample,
                controlSeqTracks*.sample,
        ].findAll().flatten().unique(), "All control sources have to contain the same sample")

        if (samplePair) {
            if (diseaseWorkPackage) {
                assert samplePair.mergingWorkPackage1 == diseaseWorkPackage
            } else {
                diseaseWorkPackage = samplePair.mergingWorkPackage1
            }
            if (controlWorkPackage) {
                assert samplePair.mergingWorkPackage2 == controlWorkPackage
            } else {
                controlWorkPackage = samplePair.mergingWorkPackage2
            }
        } else {
            if (!controlWorkPackage) {
                Sample sample = controlSample ?:
                        createSample([
                                individual: diseaseWorkPackage?.individual ?: diseaseSample?.individual ?: createIndividual(),
                        ])
                controlWorkPackage = createMergingWorkPackage([
                        pipeline        : pipeline,
                        statSizeFileName: DEFAULT_TAB_FILE_NAME,
                        seqType         : seqType,
                        sample          : sample,
                ])
            }
            if (!diseaseWorkPackage) {
                Sample sample = diseaseSample ?:
                        createSample([
                                individual: controlWorkPackage.individual,
                        ])
                diseaseWorkPackage = createMergingWorkPackage(
                        pipeline: pipeline,
                        statSizeFileName: DEFAULT_TAB_FILE_NAME,
                        seqType: seqType,
                        sample: sample,
                )
            }
            createSampleTypePerProjectLazy([
                    project   : diseaseWorkPackage.project,
                    sampleType: diseaseWorkPackage.sampleType,
                    category  : SampleTypePerProject.Category.DISEASE,
            ])
            samplePair = createSamplePair(diseaseWorkPackage, controlWorkPackage)
        }

        diseaseBamFile = diseaseBamFile ?: createRoddyBamFile([workPackage: diseaseWorkPackage] +
                (controlBamFile ? [config: controlBamFile.config] : [:]) +
                bamFile1Properties
        )
        controlBamFile = controlBamFile ?: createRoddyBamFile([
                workPackage: controlWorkPackage,
                config     : diseaseBamFile.config,
        ] + bamFile2Properties)

        return [
                instanceName      : "instance-${counter++}",
                samplePair        : samplePair,
                sampleType1BamFile: diseaseBamFile,
                sampleType2BamFile: controlBamFile,
        ]
    }

    @Deprecated
    static SnvCallingInstance createSnvInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map.config = properties.config ?: createSnvConfig(
                project: samplePair.project,
                seqType: samplePair.seqType,
        )
        return createDomainObject(SnvCallingInstance, map, properties)
    }

    static RoddySnvCallingInstance createRoddySnvInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map += [
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                config                      : createRoddyWorkflowConfigLazy([
                        project : samplePair.project,
                        seqType : samplePair.seqType,
                        pipeline: createRoddySnvPipelineLazy()
                ]),
        ]
        return createDomainObject(RoddySnvCallingInstance, map, properties)
    }

    static IndelCallingInstance createIndelCallingInstanceWithSameSamplePair(BamFilePairAnalysis instance) {
        return createDomainObject(IndelCallingInstance, [
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                config            : createRoddyWorkflowConfigLazy(pipeline: createIndelPipelineLazy()),
                instanceName      : "instance-${counter++}",
                samplePair        : instance.samplePair,
        ], [:])
    }

    static IndelCallingInstance createIndelCallingInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map += [
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                config                      : createRoddyWorkflowConfigLazy(
                        project: samplePair.project,
                        seqType: samplePair.seqType,
                        pipeline: createIndelPipelineLazy()
                ),
        ]
        return createDomainObject(IndelCallingInstance, map, properties)
    }

    static SophiaInstance createSophiaInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map += [
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                config                      : createRoddyWorkflowConfigLazy(
                        project: samplePair.project,
                        seqType: samplePair.seqType,
                        pipeline: createSophiaPipelineLazy()
                ),
        ]
        return createDomainObject(SophiaInstance, map, properties)
    }

    static SophiaInstance createSophiaInstance(SamplePair samplePair, Map properties = [:]) {
        return createDomainObject(SophiaInstance, [
                samplePair        : samplePair,
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                instanceName      : "instance-${counter++}",
                config            : createRoddyWorkflowConfig([pipeline: createSophiaPipelineLazy()]),
        ], properties)
    }

    static SophiaInstance createSophiaInstanceWithSameSamplePair(BamFilePairAnalysis instance) {
        return createDomainObject(SophiaInstance, [
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                config            : createRoddyWorkflowConfigLazy(pipeline: createSophiaPipelineLazy()),
                instanceName      : "instance-${counter++}",
                samplePair        : instance.samplePair,
        ], [:])
    }

    static AceseqInstance createAceseqInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map += [
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                config                      : createRoddyWorkflowConfigLazy(
                        project: samplePair.project,
                        seqType: samplePair.seqType,
                        pipeline: createAceseqPipelineLazy()
                ),
        ]
        return createDomainObject(AceseqInstance, map, properties)
    }

    static AceseqInstance createAceseqInstanceWithSameSamplePair(BamFilePairAnalysis instance, Map properties = [:]) {
        return createDomainObject(AceseqInstance, [
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                config            : createRoddyWorkflowConfigLazy(pipeline: createAceseqPipelineLazy()),
                instanceName      : "instance-${counter++}",
                samplePair        : instance.samplePair,
        ], properties)
    }

    static AceseqQc createAceseqQcWithExistingAceseqInstance(AceseqInstance aceseqInstance) {
        return createAceseqQc([:], [:], [:], aceseqInstance)
    }

    static AceseqQc createAceseqQc(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:], AceseqInstance aceseqInstance = null) {
        return createDomainObject(AceseqQc, [
                aceseqInstance  : aceseqInstance ?: createAceseqInstanceWithRoddyBamFiles(properties, bamFile1Properties, bamFile2Properties),
                number          : 1,
                tcc             : 1,
                ploidyFactor    : '1.0',
                ploidy          : 1,
                goodnessOfFit   : 1,
                gender          : 'M',
                solutionPossible: 1,
        ], properties)
    }

    static RunYapsaInstance createRunYapsaInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map += [
                config: createRunYapsaConfigLazy(
                        project: samplePair.project,
                        seqType: samplePair.seqType,
                ),
        ]
        return createDomainObject(RunYapsaInstance, map, properties)
    }

    static RunYapsaInstance createRunYapsaInstanceWithSameSamplePair(BamFilePairAnalysis instance, Map properties = [:]) {
        return createDomainObject(RunYapsaInstance, [
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                config            : createRunYapsaConfigLazy(pipeline: createRunYapsaPipelineLazy(), project: instance.project, seqType: instance.seqType),
                instanceName      : "instance-${counter++}",
                samplePair        : instance.samplePair,
        ], properties)
    }

    @Deprecated
    static AntibodyTarget createAntibodyTarget(Map properties = [:]) {
        return proxyCore.createAntibodyTarget(properties)
    }

    @Deprecated
    static SeqCenter createSeqCenter(Map seqCenterProperties = [:]) {
        return proxyCore.createSeqCenter(seqCenterProperties)
    }

    @Deprecated
    static SeqPlatform createSeqPlatformWithSeqPlatformGroup(Map seqPlatformProperties = [:]) {
        return proxyCore.createSeqPlatformWithSeqPlatformGroup(seqPlatformProperties)
    }

    @Deprecated
    static SeqPlatform createSeqPlatform(Map seqPlatformProperties = [:]) {
        return proxyCore.createSeqPlatform(seqPlatformProperties)
    }

    @Deprecated
    static SeqPlatformModelLabel createSeqPlatformModelLabel(Map properties = [:]) {
        return proxyCore.createSeqPlatformModelLabel(properties)
    }

    @Deprecated
    static SequencingKitLabel createSequencingKitLabel(Map properties = [:]) {
        return proxyCore.createSequencingKitLabel(properties)
    }

    @Deprecated
    static SeqPlatformGroup createSeqPlatformGroup(Map properties = [:]) {
        return proxyCore.createSeqPlatformGroup(properties)
    }

    @Deprecated
    static SeqPlatformGroup createSeqPlatformGroupWithMergingCriteria(Map properties = [:]) {
        return proxyCore.createSeqPlatformGroupWithMergingCriteria(properties)
    }

    @Deprecated
    static Run createRun(Map runProperties = [:]) {
        return proxyCore.createRun(runProperties)
    }

    @Deprecated
    static FastqImportInstance createFastqImportInstance(Map fastqImportInstanceProperties = [:]) {
        return proxyCore.createFastqImportInstance(fastqImportInstanceProperties)
    }

    @Deprecated
    static Project createProject(Map projectProperties = [:], boolean saveAndValidate = true) {
        return proxyCore.createProject(projectProperties, saveAndValidate)
    }

    @Deprecated
    static UserProjectRole createUserProjectRole(Map properties = [:]) {
        return UserDomainFactoryInstance.INSTANCE.createUserProjectRole(properties)
    }

    @Deprecated
    static ProjectRole createProjectRole(Map properties = [:]) {
        return UserDomainFactoryInstance.INSTANCE.createProjectRole(properties)
    }

    @Deprecated
    static ProjectRole createOrGetAuthorityProjectRole(Map properties = [:]) {
        return UserDomainFactoryInstance.INSTANCE.createOrGetAuthorityProjectRole(properties)
    }

    @Deprecated
    static Individual createIndividual(Map individualProperties = [:]) {
        return proxyCore.createIndividual(individualProperties)
    }

    @Deprecated
    static SampleType createSampleType(Map sampleTypeProperties = [:]) {
        return proxyCore.createSampleType(sampleTypeProperties)
    }

    @Deprecated
    static Sample createSample(Map sampleProperties = [:], boolean saveAndValidate = true) {
        return proxyCore.createSample(sampleProperties, saveAndValidate)
    }

    static SampleIdentifier createSampleIdentifier(Map properties = [:]) {
        return createDomainObject(SampleIdentifier, [
                sample: { createSample() },
                name  : 'sampleIdentifierName_' + (counter++),
        ], properties)
    }

    @Deprecated
    static SeqType createSeqType(Map seqTypeProperties = [:], boolean saveAndValidate = true) {
        return proxyCore.createSeqType(seqTypeProperties, saveAndValidate)
    }

    static SeqType createSeqTypePaired(Map seqTypeProperties = [:], boolean saveAndValidate = true) {
        return createSeqType([libraryLayout: SequencingReadType.PAIRED] + seqTypeProperties, saveAndValidate)
    }

    @Deprecated
    static SoftwareTool createSoftwareTool(Map softwareToolProperties = [:]) {
        return proxyCore.createSoftwareTool(softwareToolProperties)
    }

    @Deprecated
    static SoftwareToolIdentifier createSoftwareToolIdentifier(Map properties = [:]) {
        return proxyCore.createSoftwareToolIdentifier(properties)
    }

    @Deprecated
    static LibraryPreparationKit createLibraryPreparationKit(Map properties = [:]) {
        return proxyCore.createLibraryPreparationKit(properties)
    }

    @Deprecated
    static ReferenceGenome createReferenceGenome(Map properties = [:], boolean saveAndValidate = true) {
        return proxyCore.createReferenceGenome(properties, saveAndValidate)
    }

    static ReferenceGenome createAceseqReferenceGenome() {
        return createReferenceGenome(
                speciesWithStrain: [TaxonomyFactoryInstance.INSTANCE.findOrCreateHumanSpecies()] as Set,
                species: [] as Set,
                gcContentFile: "gcContentFile.file",
                geneticMapFile: "/geneticMapFile.file",
                geneticMapFileX: "/geneticMapFileX.file",
                knownHaplotypesFile: "/knownHaplotypesFile.file",
                knownHaplotypesFileX: "/knownHaplotypesFileX.file",
                knownHaplotypesLegendFile: "/knownHaplotypesLegendFile.file",
                knownHaplotypesLegendFileX: "/knownHaplotypesLegendFileX.file",
                mappabilityFile: "/mappabilityFile.file",
                replicationTimeFile: "/replicationTimeFile.file",
        )
    }

    static ReferenceGenomeEntry createReferenceGenomeEntry(Map properties = [:]) {
        return createDomainObject(ReferenceGenomeEntry, [
                referenceGenome: { createReferenceGenome() },
                classification : ReferenceGenomeEntry.Classification.CHROMOSOME,
                name           : "name${counter++}",
                alias          : "alias${counter++}",
        ], properties)
    }

    static List<ReferenceGenomeEntry> createReferenceGenomeEntries(ReferenceGenome referenceGenome = createReferenceGenome(), Collection<String> chromosomeNames) {
        return chromosomeNames.collect {
            createReferenceGenomeEntry([
                    name           : it,
                    alias          : it,
                    referenceGenome: referenceGenome,
                    classification : ReferenceGenomeEntry.Classification.CHROMOSOME,
            ])
        }
    }

    @Deprecated
    static ReferenceGenomeIndex createReferenceGenomeIndex(Map properties = [:], boolean saveAndValidate = true) {
        return proxyCore.createReferenceGenomeIndex(properties, saveAndValidate)
    }

    @Deprecated
    static ToolName createToolName(Map properties = [:], boolean saveAndValidate = true) {
        return proxyCore.createToolName(properties, saveAndValidate)
    }

    static GeneModel createGeneModel(Map properties = [:]) {
        return createDomainObject(GeneModel, [
                referenceGenome: { createReferenceGenome() },
                path           : "path_${counter++}",
                fileName       : "fileName.gtf",
                excludeFileName: "excludeFileName.gtf",
                dexSeqFileName : "dexSeqFileName.gtf",
                gcFileName     : "gcFileName.gtf",
        ], properties)
    }

    static ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ReferenceGenomeProjectSeqType, [
                project        : { createProject() },
                seqType        : { createSeqType() },
                referenceGenome: { createReferenceGenome() },
        ], properties, saveAndValidate)
    }

    static ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqTypeLazy(Map properties = [:]) {
        return findOrCreateDomainObject(ReferenceGenomeProjectSeqType, [
                project        : { createProject() },
                seqType        : { createSeqType() },
                referenceGenome: { createReferenceGenome() },
        ], properties)
    }

    static BedFile createBedFile(Map properties = [:]) {
        return createDomainObject(BedFile, [
                fileName             : "fileName_${counter++}",
                referenceGenome      : { createReferenceGenome() },
                libraryPreparationKit: { createLibraryPreparationKit() },
                targetSize           : 1,
        ], properties)
    }

    @Deprecated
    static SeqTrack createSeqTrack(Map properties = [:]) {
        return proxyCore.createSeqTrack(properties)
    }

    @Deprecated
    static SeqTrack createExomeSeqTrack(Map properties = [:]) {
        return proxyCore.createExomeSeqTrack(properties)
    }

    @Deprecated
    static SeqTrack createChipSeqSeqTrack(Map properties = [:]) {
        return proxyCore.createChipSeqSeqTrack(properties)
    }

    static Map<String, ?> baseMergingWorkPackageProperties(Map properties) {
        return [
                libraryPreparationKit: { properties.seqType?.isWgbs() ? null : createLibraryPreparationKit() },
                sample               : { createSample() },
                seqPlatformGroup     : { createSeqPlatformGroup() },
                referenceGenome      : { createReferenceGenome() },
                antibodyTarget       : { properties.seqType?.hasAntibodyTarget ? createAntibodyTarget() : null },
        ]
    }

    @Deprecated
    static MergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true) {
        return proxyRoddy.createMergingWorkPackage(properties, saveAndValidate)
    }

    @Deprecated
    static ExternalMergingWorkPackage createExternalMergingWorkPackage(Map properties = [:]) {
        return ExternalBamFactoryInstance.INSTANCE.createMergingWorkPackage(properties)
    }

    static <E extends AbstractMergingWorkPackage> E createMergingWorkPackage(Class<E> clazz, Map properties = [:]) {
        switch (clazz) {
            case MergingWorkPackage:
                return createMergingWorkPackage(properties)
            case ExternalMergingWorkPackage:
                return createExternalMergingWorkPackage(properties)
            default:
                throw new NotSupportedException("Unknown subclass of AbstractMergingWorkPackage: ${clazz}")
        }
    }

    static AbstractMergingWorkPackage createMergingWorkPackageForPipeline(Pipeline.Name pipelineName, Map properties = [:]) {
        switch (pipelineName) {
            case Pipeline.Name.PANCAN_ALIGNMENT:
                return createMergingWorkPackage([
                        pipeline: createPanCanPipeline()
                ] + properties)
            case Pipeline.Name.DEFAULT_OTP:
                return createMergingWorkPackage([
                        pipeline: createDefaultOtpPipeline()
                ] + properties)
            case Pipeline.Name.EXTERNALLY_PROCESSED:
                return createExternalMergingWorkPackage(properties)
            default:
                throw new NotSupportedException("Unknown alignment pipeline: ${pipelineName}")
        }
    }

    @Deprecated
    static FileType createFileType(Map properties = [:]) {
        return proxyCore.createFileType(properties)
    }

    @Deprecated
    static RawSequenceFile createFastqFile(Map properties = [:], boolean saveAndValidate = true) {
        return proxyCore.createFastqFile(properties, saveAndValidate)
    }

    static private Map createRoddyWorkflowConfigMapHelper(Map properties = [:]) {
        Pipeline pipeline = properties.containsKey('pipeline') ? properties.pipeline : createPanCanPipeline()
        SeqType seqType = properties.containsKey('seqType') ? properties.seqType : createWholeGenomeSeqType()
        String programVersion = properties.containsKey('programVersion') ? properties.programVersion : "programVersion:1.1.${counter++}"
        String configVersion = properties.containsKey('configVersion') ? properties.configVersion : "v1_${counter++}"
        return [
                pipeline             : pipeline,
                seqType              : seqType,
                configFilePath       : {
                    "${TestCase.uniqueNonExistentPath}/${pipeline.name.name()}_${seqType.roddyName}_${seqType.libraryLayout}_${programVersion.substring(programVersion.indexOf(':') + 1)}_${configVersion}.xml"
                },
                programVersion       : programVersion,
                configVersion        : configVersion,
                project              : { properties.individual?.project ?: createProject() },
                dateCreated          : { new Date() },
                lastUpdated          : { new Date() },
                adapterTrimmingNeeded: { seqType.isWgbs() || seqType.isRna() || seqType.isChipSeq() },
                nameUsedInConfig     : RoddyWorkflowConfig.getNameUsedInConfig(pipeline.name, seqType, programVersion, configVersion),
                md5sum               : HelperUtils.randomMd5sum,
        ]
    }

    static RoddyWorkflowConfig createRoddyWorkflowConfig(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(RoddyWorkflowConfig, createRoddyWorkflowConfigMapHelper(properties), properties, saveAndValidate)
    }

    static RoddyWorkflowConfig createRoddyWorkflowConfigLazy(Map properties = [:], boolean saveAndValidate = true) {
        return findOrCreateDomainObject(RoddyWorkflowConfig, createRoddyWorkflowConfigMapHelper(properties), properties, saveAndValidate)
    }

    static private Map createRunYapsaConfigMapHelper(Map properties = [:]) {
        return [
                pipeline      : createRunYapsaPipelineLazy(),
                seqType       : { properties.seqType ?: createSeqType() },
                project       : { properties.project ?: createProject() },
                programVersion: "programmVersion${counter++}",
                dateCreated   : { new Date() },
                lastUpdated   : { new Date() },
        ]
    }

    static RunYapsaConfig createRunYapsaConfig(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(RunYapsaConfig, createRunYapsaConfigMapHelper(properties), properties, saveAndValidate)
    }

    static RunYapsaConfig createRunYapsaConfigLazy(Map properties = [:], boolean saveAndValidate = true) {
        return findOrCreateDomainObject(RunYapsaConfig, createRunYapsaConfigMapHelper(properties), properties, saveAndValidate)
    }

    static SeqTrack createSeqTrack(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:]) {
        return createSeqTrack(getMergingProperties(mergingWorkPackage) + seqTrackProperties)
    }

    static Map getMergingProperties(MergingWorkPackage mergingWorkPackage) {
        SeqPlatform seqPlatform = mergingWorkPackage?.seqPlatformGroup?.seqPlatforms?.sort()?.find() ?:
                createSeqPlatformWithSeqPlatformGroup(seqPlatformGroups: [mergingWorkPackage.seqPlatformGroup])

        Map properties = [
                sample               : mergingWorkPackage.sample,
                seqType              : mergingWorkPackage.seqType,
                libraryPreparationKit: mergingWorkPackage.libraryPreparationKit,
                run                  : createRun(seqPlatform: seqPlatform),
        ]
        if (mergingWorkPackage.seqType.hasAntibodyTarget) {
            properties += [
                    antibodyTarget: mergingWorkPackage.antibodyTarget,
            ]
        }
        return properties
    }

    static SeqTrack createSeqTrackWithFastqFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map fastqFileProperties = [:]) {
        Map map = getMergingProperties(mergingWorkPackage) + [
                kitInfoReliability: mergingWorkPackage.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ] + seqTrackProperties
        SeqTrack seqTrack
        if (mergingWorkPackage.seqType.libraryLayout == SequencingReadType.PAIRED) {
            seqTrack = createSeqTrackWithTwoFastqFiles(map, fastqFileProperties, fastqFileProperties)
        } else {
            seqTrack = createSeqTrackWithOneFastqFile(map, fastqFileProperties)
        }

        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        mergingWorkPackage.addToSeqTracks(seqTrack)
        mergingWorkPackage.save(flush: true)

        assert mergingWorkPackage.satisfiesCriteria(seqTrack)
        return seqTrack
    }

    @Deprecated
    static SeqTrack createSeqTrackWithOneFastqFile(Map seqTrackProperties = [:], Map fastqFileProperties = [:]) {
        return proxyCore.createSeqTrackWithOneFastqFile(seqTrackProperties, fastqFileProperties)
    }

    static SeqTrack createSeqTrackWithTwoFastqFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map fastqFileProperties1 = [:], Map fastqFileProperties2 = [:]) {
        return createSeqTrackWithTwoFastqFiles(getMergingProperties(mergingWorkPackage) + seqTrackProperties, fastqFileProperties1, fastqFileProperties2)
    }

    static SeqTrack createSeqTrackWithTwoFastqFiles(Map seqTrackProperties = [:], Map fastqFileProperties1 = [:], Map fastqFileProperties2 = [:]) {
        FileType fileType = createFileType()
        Map defaultMap1 = [
                fileName   : 'DataFileFileName_R1.gz',
                vbpFileName: 'DataFileFileName_R1.gz',
                fileType   : fileType,
                mateNumber : 1,
        ]
        Map defaultMap2 = [
                fileName   : 'DataFileFileName_R2.gz',
                vbpFileName: 'DataFileFileName_R2.gz',
                fileType   : fileType,
                mateNumber : 2,
        ]
        SeqTrack seqTrack = createSeqTrackWithOneFastqFile([seqType: createSeqType(libraryLayout: SequencingReadType.PAIRED)] + seqTrackProperties, defaultMap1 + fastqFileProperties1)
        createSequenceDataFile(defaultMap2 + fastqFileProperties2 + [seqTrack: seqTrack])
        return seqTrack
    }

    @Deprecated
    static RawSequenceFile createSequenceDataFile(final Map properties = [:]) {
        return proxyCore.createSequenceDataFile(properties)
    }

    static RoddySnvCallingInstance createRoddySnvCallingInstance(Map properties = [:]) {
        return createDomainObject(RoddySnvCallingInstance, [
                processingState: AnalysisProcessingStates.IN_PROGRESS,
                config         : properties.config ?:
                        properties.samplePair ?
                                createRoddyWorkflowConfig(
                                        project: properties.samplePair.project,
                                        seqType: properties.samplePair.seqType,
                                )
                                : createRoddyWorkflowConfig(),
                instanceName   : "instance-${counter++}",
        ], properties)
    }

    static RoddySnvCallingInstance createRoddySnvCallingInstance(SamplePair samplePair, Map properties = [:]) {
        return createDomainObject(RoddySnvCallingInstance, [
                samplePair        : samplePair,
                processingState   : AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                instanceName      : "instance-${counter++}",
                config            : createRoddyWorkflowConfig([pipeline: createRoddySnvPipelineLazy()]),
        ], properties)
    }

    @Deprecated
    static ProcessingStep createAndSaveProcessingStep(ProcessParameterObject processParameterObject = null) {
        return createAndSaveProcessingStep("de.dkfz.tbi.otp.test.job.jobs.NonExistentDummyJob", processParameterObject)
    }

    @Deprecated
    static ProcessingStep createAndSaveProcessingStep(String jobClass, ProcessParameterObject processParameterObject = null) {
        final JobExecutionPlan jep = new JobExecutionPlan(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0, startJobBean: "DontCare")
        assert jep.save(flush: true)
        final JobDefinition jobDefinition = new JobDefinition(name: "DontCare", bean: "DontCare", plan: jep)
        assert jobDefinition.save(flush: true)
        final Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "DontCare")
        assert process.save(flush: true)
        if (processParameterObject != null) {
            createProcessParameter(process, processParameterObject)
        }
        final ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process, jobClass: jobClass)
        assert step.save(flush: true)
        final ProcessingStepUpdate update = createProcessingStepUpdate(step, ExecutionState.CREATED)
        assert update.save(flush: true)
        return step
    }

    static RestartedProcessingStep createAndSaveRestartedProcessingStep(ProcessingStep step = null) {
        ProcessingStep originalStep = step ?: createAndSaveProcessingStep()
        final RestartedProcessingStep restartedProcessingStep = RestartedProcessingStep.create(originalStep)
        assert restartedProcessingStep.save(flush: true)
        return restartedProcessingStep
    }

    static ProcessingStepUpdate createProcessingStepUpdate(final ProcessingStep step, final ExecutionState state) {
        return createProcessingStepUpdate([
                date          : new Date(),
                state         : state,
                previous      : step.latestProcessingStepUpdate,
                processingStep: step,
        ])
    }

    static ProcessingStep createProcessingStepWithUpdates(ProcessingStep processingStep = createProcessingStep()) {
        ProcessingStepUpdate last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.CREATED)
        last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.STARTED, previous: last)
        last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.FINISHED, previous: last)
        createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.SUCCESS, previous: last)
        return processingStep
    }

    static ProcessParameter createProcessParameter(final Process process, final ProcessParameterObject parameterValue, Map properties = [:]) {
        return createDomainObject(ProcessParameter, [
                process  : process,
                className: parameterValue.class.name,
                value    : parameterValue.id.toString(),
        ], properties)
    }

    static ProcessParameter createProcessParameter(final Process process, final String className, final String value) {
        return createDomainObject(ProcessParameter, [
                process  : process,
                className: className,
                value    : value,
        ], [:])
    }

    static Parameter createParameter(Map properties = [:]) {
        return createDomainObject(Parameter, [
                type: { createParameterType() },
        ], properties)
    }

    static ParameterType createParameterType(Map properties = [:]) {
        return createDomainObject(ParameterType, [
                name          : "parameterTypeName_${counter++}",
                jobDefinition : { createJobDefinition() },
                parameterUsage: ParameterUsage.INPUT,
        ], properties)
    }

    static ClusterJob createClusterJob(
            final ProcessingStep processingStep, final ClusterJobIdentifier clusterJobIdentifier,
            final Map myProps = [
                    clusterJobName   : "testName_${processingStep.nonQualifiedJobClass}",
                    jobClass         : processingStep.nonQualifiedJobClass,
                    queued           : ZonedDateTime.now(),
                    requestedWalltime: Duration.ofMinutes(5),
                    requestedCores   : 10,
                    requestedMemory  : 1000,
            ]) {
        return createDomainObject(ClusterJob, [
                processingStep: processingStep,
                clusterJobId  : clusterJobIdentifier.clusterJobId,
                userName      : "userName_${counter++}",
        ], myProps)
    }

    /**
     * @deprecated can be removed as soon as there is a 'SeqTypeDomainFactory' or another system how to handle the
     * creation of the different seqTypes.
     */
    @Deprecated
    private static SeqType createSeqTypeLazy(SeqTypeNames seqTypeNames, String displayName, String dirName,
                                             String roddyName = null, SequencingReadType libraryLayout = SequencingReadType.PAIRED,
                                             boolean singleCell = false, boolean hasAntibodyTarget = false, boolean needsBedFile = false) {
        return findOrCreateDomainObject(SeqType, [:], [
                name             : seqTypeNames.seqTypeName,
                displayName      : displayName,
                dirName          : dirName,
                roddyName        : roddyName,
                libraryLayout    : libraryLayout,
                singleCell       : singleCell,
                hasAntibodyTarget: hasAntibodyTarget,
                needsBedFile     : needsBedFile,
        ]).refresh()
    }

    static SeqType createWholeGenomeSeqType(SequencingReadType libraryLayout = SequencingReadType.PAIRED) {
        return createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME, 'WGS', 'whole_genome_sequencing', 'WGS', libraryLayout)
    }

    static SeqType createExomeSeqType(SequencingReadType libraryLayout = SequencingReadType.PAIRED) {
        return createSeqTypeLazy(SeqTypeNames.EXOME, 'EXOME', 'exon_sequencing', 'WES', libraryLayout, false, false, true)
    }

    static SeqType createWholeGenomeBisulfiteSeqType() {
        return createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE, 'WGBS', 'whole_genome_bisulfite_sequencing', 'WGBS')
    }

    static SeqType createWholeGenomeBisulfiteTagmentationSeqType() {
        return createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION, 'WGBS_TAG', 'whole_genome_bisulfite_tagmentation_sequencing', 'WGBSTAG')
    }

    static SeqType createChipSeqType(SequencingReadType libraryLayout = SequencingReadType.PAIRED) {
        return createSeqTypeLazy(SeqTypeNames.CHIP_SEQ, 'ChIP', 'chip_seq_sequencing', "CHIPSEQ", libraryLayout, false, true)
    }

    static SeqType createRnaPairedSeqType() {
        return createSeqTypeLazy(SeqTypeNames.RNA, 'RNA', 'rna_sequencing', "RNA")
    }

    static SeqType createRnaSingleSeqType() {
        return createSeqTypeLazy(SeqTypeNames.RNA, 'RNA', 'rna_sequencing', "RNA", SequencingReadType.SINGLE)
    }

    static List<SeqType> createDefaultOtpAlignableSeqTypes() {
        return [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createPanCanAlignableSeqTypes() {
        return [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
                createWholeGenomeBisulfiteSeqType(),
                createWholeGenomeBisulfiteTagmentationSeqType(),
                createChipSeqType(),
        ]
    }

    static List<SeqType> createRnaAlignableSeqTypes() {
        return [
                createRnaPairedSeqType(),
                createRnaSingleSeqType(),
        ]
    }

    static List<SeqType> createRoddyAlignableSeqTypes() {
        return [
                createPanCanAlignableSeqTypes(),
                createRnaAlignableSeqTypes(),
        ].flatten() as List<SeqType>
    }

    static List<SeqType> createCellRangerAlignableSeqTypes() {
        return [
                proxyCellRanger.createSeqType(),
        ].flatten() as List<SeqType>
    }

    static List<SeqType> createAllAlignableSeqTypes() {
        return [
                createDefaultOtpAlignableSeqTypes(),
                createRoddyAlignableSeqTypes(),
                createCellRangerAlignableSeqTypes(),
        ].flatten().unique() as List<SeqType>
    }

    static List<SeqType> createSnvSeqTypes() {
        return [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createIndelSeqTypes() {
        return [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createSophiaSeqTypes() {
        return [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createAceseqSeqTypes() {
        return [
                createWholeGenomeSeqType(),
        ]
    }

    static List<SeqType> createRunYapsaSeqTypes() {
        return [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createAllAnalysableSeqTypes() {
        return [
                createSnvSeqTypes(),
                createIndelSeqTypes(),
                createSophiaSeqTypes(),
                createAceseqSeqTypes(),
        ].flatten().unique() as List<SeqType>
    }

    static MetaDataKey createMetaDataKeyLazy(Map properties = [:]) {
        return findOrCreateDomainObject(MetaDataKey, [
                name: "name_${counter++}",
        ], properties)
    }

    static MetaDataKey createMetaDataKey(Map properties = [:]) {
        return createDomainObject(MetaDataKey, [
                name: "name_${counter++}",
        ], properties)
    }

    static MetaDataEntry createMetaDataEntry(Map properties = [:]) {
        return createDomainObject(MetaDataEntry, [
                value       : "value_${counter++}",
                sequenceFile: { createFastqFile() },
                key         : { createMetaDataKey() },
        ], properties)
    }

    static MetaDataEntry createMetaDataKeyAndEntry(RawSequenceFile sequenceFile, String key, String value) {
        MetaDataKey metaDataKey = createMetaDataKeyLazy(name: key)

        return createMetaDataEntry(
                value: value,
                sequenceFile: sequenceFile,
                key: metaDataKey,
        )
    }

    static MetaDataEntry createMetaDataKeyAndEntry(RawSequenceFile sequenceFile, MetaDataColumn key, String value) {
        return createMetaDataKeyAndEntry(sequenceFile, key.name(), value)
    }

    static MetaDataFile createMetaDataFile(Map properties = [:]) {
        return createDomainObject(MetaDataFile, [
                fileNameSource     : "MetaDataFileName_${counter++}",
                filePathSource     : TestCase.uniqueNonExistentPath.path,
                filePathTarget     : TestCase.uniqueNonExistentPath.path,
                fastqImportInstance: { createFastqImportInstance() },
        ], properties)
    }

    @Deprecated
    static Ticket createTicket(Map properties = [:]) {
        return proxyCore.createTicket(properties)
    }

    @Deprecated
    static Ticket createTicketWithEndDatesAndNotificationSent(Map properties = [:]) {
        return proxyCore.createTicketWithEndDatesAndNotificationSent(properties)
    }

    static TumorEntity createTumorEntity(Map properties = [:]) {
        return createDomainObject(TumorEntity, [
                name: "AML/ALL",
        ], properties)
    }

    static void createRoddyProcessingOptions(File basePath = TestCase.uniqueNonExistentPath) {
        createProcessingOptionLazy(
                OptionName.RODDY_PATH,
                "${basePath}/roddy",
        )
        createProcessingOptionLazy(
                OptionName.RODDY_BASE_CONFIGS_PATH,
                "${basePath}/roddyBaseConfigs",
        )
        createProcessingOptionLazy(
                OptionName.RODDY_APPLICATION_INI,
                "${basePath}/roddyBaseConfigs/applicationProperties.ini",
        )
        createProcessingOptionLazy(
                OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH,
                "${basePath}/roddyBaseConfigs/featureToggles.ini",
        )
        createProcessingOptionLazy(
                OptionName.RODDY_SHARED_FILES_BASE_DIRECTORY,
                "/shared",
        )
    }

    @Deprecated
    static ExternallyProcessedBamFile createExternallyProcessedBamFile(Map properties = [:]) {
        return ExternalBamFactoryInstance.INSTANCE.createBamFile(properties)
    }

    @Deprecated
    static ExternallyProcessedBamFile createFinishedExternallyProcessedBamFile(Map properties = [:]) {
        return ExternalBamFactoryInstance.INSTANCE.createFinishedBamFile(properties)
    }

    static ProcessingThresholds createProcessingThresholds(Map properties = [:]) {
        return createDomainObject(ProcessingThresholds, [
                project      : { createProject() },
                seqType      : { createSeqType() },
                sampleType   : { createSampleType() },
                coverage     : 30.0,
                numberOfLanes: 3,
        ], properties)
    }

    static ProcessingThresholds createProcessingThresholdsForMergingWorkPackage(AbstractMergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return createProcessingThresholds([
                project   : mergingWorkPackage.project,
                seqType   : mergingWorkPackage.seqType,
                sampleType: mergingWorkPackage.sampleType,
        ] + properties)
    }

    static ProcessingThresholds createProcessingThresholdsForSeqTrack(SeqTrack seqTrack, Map properties = [:]) {
        return createProcessingThresholds([
                project   : seqTrack.project,
                seqType   : seqTrack.seqType,
                sampleType: seqTrack.sampleType,
        ] + properties)
    }

    static ProcessingThresholds createProcessingThresholdsForBamFile(AbstractBamFile bamFile, Map properties = [:]) {
        return createProcessingThresholdsForMergingWorkPackage(bamFile.mergingWorkPackage, properties)
    }

    static void createAceseqQaFileOnFileSystem(Path qaFile) {
        Files.createDirectories(qaFile.parent)
        qaFile << """\
{
    "1":{
        "gender":"male",
        "solutionPossible":"3",
        "tcc":"0.5",
        "goodnessOfFit":"0.904231625835189",
        "ploidyFactor":"2.27",
        "ploidy":"2",
    },
    "2":{
        "gender":"female",
        "solutionPossible":"4",
        "tcc":"0.7",
        "goodnessOfFit":"0.12345",
        "ploidyFactor":"1.27",
        "ploidy":"5",
    }
}
"""
    }

    static void createSophiaQcFileOnFileSystem(Path qcFile) {
        Files.createDirectories(qcFile.parent)
        qcFile << """\
{
  "all": {
    "controlMassiveInvPrefilteringLevel": 0,
    "tumorMassiveInvFilteringLevel": 0,
    "rnaContaminatedGenesMoreThanTwoIntron": "PRKRA;ACTG2;TYRO3;COL18A1;",
    "rnaContaminatedGenesCount": 4,
    "rnaDecontaminationApplied": false
  }
}
"""
    }

    static void createIndelQcFileOnFileSystem(Path qcFile) {
        Files.createDirectories(qcFile.parent)
        qcFile << """
{
  "all": {
    "file": "${qcFile}",
    "numIndels":23,
    "numIns":24,
    "numDels":25,
    "numSize1_3":26,
    "numSize4_10":27,
    "numSize11plus":28,
    "numInsSize1_3":29,
    "numInsSize4_10":30,
    "numInsSize11plus":31,
    "numDelsSize1_3":32,
    "numDelsSize4_10":33,
    "numDelsSize11plus":34,
    "percentIns":35.0,
    "percentDels":36.0,
    "percentSize1_3":37.0,
    "percentSize4_10":38.0,
    "percentSize11plus":39.0,
    "percentInsSize1_3":40.0,
    "percentInsSize4_10":41.0,
    "percentInsSize11plus":42.0,
    "percentDelsSize1_3":43.0,
    "percentDelsSize4_10":44.0,
    "percentDelsSize11plus":45.0,
    }
}
"""
    }

    static void createIndelSampleSwapDetectionFileOnFileSystem(Path qcFile, Individual individual) {
        Files.createDirectories(qcFile.parent)
        qcFile << """
{
    "somaticSmallVarsInTumorCommonInGnomADPer":1,
    "somaticSmallVarsInControlCommonInGnomad":2,
    "tindaSomaticAfterRescue":3,
    "somaticSmallVarsInControlInBiasPer":4,
    "somaticSmallVarsInTumorPass":5,
    "pid": ${individual.pid},
    "somaticSmallVarsInControlPass":6,
    "somaticSmallVarsInControlPassPer":7,
    "tindaSomaticAfterRescueMedianAlleleFreqInControl":8.0,
    "somaticSmallVarsInTumorInBiasPer":9.0,
    "somaticSmallVarsInControlCommonInGnomadPer":10,
    "somaticSmallVarsInTumorInBias":11,
    "somaticSmallVarsInControlCommonInGnomasPer":12,
    "germlineSNVsHeterozygousInBothRare":13,
    "germlineSmallVarsHeterozygousInBothRare":14,
    "tindaGermlineRareAfterRescue":15,
    "somaticSmallVarsInTumorCommonInGnomad":16,
    "somaticSmallVarsInControlInBias":17,
    "somaticSmallVarsInControl":18,
    "somaticSmallVarsInTumor":19,
    "germlineSNVsHeterozygousInBoth":20,
    "somaticSmallVarsInTumorPassPer":21.9,
    "somaticSmallVarsInTumorCommonInGnomadPer":22,
    "germlineSmallVarsInBothRare":23,
}
"""
    }

    @Deprecated
    static MergingCriteria createMergingCriteria(Map properties = [:]) {
        return proxyCore.createMergingCriteria(properties)
    }

    @Deprecated
    static MergingCriteria createMergingCriteriaLazy(Map properties) {
        return proxyCore.createMergingCriteriaLazy(properties)
    }

    static QcThreshold createQcThreshold(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(QcThreshold, [
                qcProperty1          : "estimatedNumberOfCells",
                warningThresholdUpper: counter++,
                errorThresholdUpper  : counter++,
                compare              : QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass              : CellRangerQualityAssessment.name,
                seqType              : { createSeqType() },
        ], properties, saveAndValidate)
    }

    static AggregateSequences createAggregateSequences(Map properties = [:]) {
        return createDomainObject(AggregateSequences, [
                seqTypeId              : counter++,
                seqPlatformId          : counter++,
                seqPlatformModelLabelId: counter++,
                sequencingKitLabelId   : counter++,
                sampleId               : counter++,
                seqCenterId            : counter++,
                sampleTypeId           : counter++,
                individualId           : counter++,
                projectId              : counter++,
                laneCount              : counter++,
                sum_N_BasePairs        : counter++,
                sum_N_BasePairsGb      : counter++,
                seqPlatformName        : "seqPlatformName${counter++}",
                seqTypeName            : "seqTypeName${counter++}",
                seqTypeDisplayName     : "seqTypeDisplayName${counter}",
                dirName                : "dirName${counter++}",
                libraryLayout          : SequencingReadType.PAIRED,
                sampleTypeName         : "sample-type-name${counter}",
                pid                    : "pid${counter++}",
                type                   : Individual.Type.REAL,
                projectName            : "projectName${counter++}",
                projectDirName         : "projectDirName${counter++}",
                seqCenterName          : "seqCenterName${counter++}",
                seqCenterDirName       : "seqCenterDirName${counter++}",
        ], properties)
    }

    @Deprecated
    static ProjectRequestUser createProjectRequestUser(Map properties = [:]) {
        return createDomainObject(ProjectRequestUser, [
                user         : { createUser() },
                projectRoles : { [createProjectRole()] },
                accessToFiles: true,
                manageUsers  : true,
        ], properties)
    }

    static Sequence createSequence(Map properties = [:]) {
        return createDomainObject(Sequence, [
                antibodyTarget     : "antibodyTarget_${counter++}",
                dirName            : "dirName__${counter++}",
                fastqcState        : SeqTrack.DataProcessingState.UNKNOWN,
                ilseId             : counter++,
                laneId             : "laneId_${counter++}",
                libraryLayout      : "libraryLayout_${counter++}",
                libraryName        : "libraryName_${counter++}",
                name               : "name_${counter++}",
                pid                : "pid_${counter++}",
                projectName        : "projectName_${counter++}",
                projectDirName     : "projectDirName_${counter++}",
                qualityEncoding    : SeqTrack.QualityEncoding.UNKNOWN,
                sampleTypeName     : "sampleTypeName_${counter++}",
                seqCenterDirName   : "seqCenterDirName_${counter++}",
                seqCenterName      : "seqCenterName_${counter++}",
                seqPlatformName    : "seqPlatformName_${counter++}",
                seqTypeDisplayName : "seqTypeDisplayName_${counter++}",
                seqTypeName        : "seqTypeName_${counter++}",
                singleCellWellLabel: "singleCellWellLabel_${counter++}",
                type               : Individual.Type.UNDEFINED,
        ], properties)
    }
}
