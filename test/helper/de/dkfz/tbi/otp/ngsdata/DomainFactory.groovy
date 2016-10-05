package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import grails.util.*
import org.joda.time.*

class DomainFactory {

    private DomainFactory() {
    }

    /**
     * @deprecated Use {@link HelperUtils#getRandomMd5sum()} instead.
     */
    @Deprecated
    static final String DEFAULT_MD5_SUM = HelperUtils.randomMd5sum
    static final String DEFAULT_TAB_FILE_NAME = 'DefaultTabFileName.tab'
    static final String DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY = 'exec_123456_123456789_test_test'
    static final long DEFAULT_FILE_SIZE = 123456
    static final String TEST_CONFIG_VERSION = 'v1_0'

    /**
     * Counter to create unique names.
     */
    static int counter = 0

    private static <T> T createDomainObject(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
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
            assert domain.save(flush: true, failOnError: true)
        }
        return domain
    }

    /**
     * Check whether one or more domain objects with the given parameterProperties exists,
     * if yes return an arbitrary one, otherwise create a new one and return it.
     */
    private static <T> T createDomainObjectLazy(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
        T domainObject
        if(parameterProperties) {
            domainObject = domainClass.findWhere(parameterProperties)
        } else {
            domainObject = domainClass.list(limit: 1)?.first()
        }
        if(!domainObject) {
            domainObject = createDomainObject(domainClass, defaultProperties, parameterProperties, saveAndValidate)
        }
        return domainObject
    }

    /**
     * @deprecated Use {@link #createRealmDataManagement()} instead.
     */
    @Deprecated
    public static Realm createRealmDataManagementDKFZ(Map myProps = [:]) {
        createRealmDataManagement(myProps)
    }

    /**
     * @deprecated Use {@link #createRealmDataProcessing()} instead.
     */
    @Deprecated
    public static Realm createRealmDataProcessingDKFZ(Map myProps = [:]) {
        createRealmDataProcessing(myProps)
    }

    static Realm createRealmDataManagement(Map properties = [:]) {
        createRealm([
                operationType:      Realm.OperationType.DATA_MANAGEMENT,
        ] + properties)
    }

    static Realm createRealmDataManagement(File testDirectory, Map properties = [:]) {
        assert testDirectory.isAbsolute()
        createRealmDataManagement([
                rootPath:           new File(testDirectory, 'root').path,
        ] + properties)
    }

    static Realm createRealmDataProcessing(Map properties = [:]) {
        createRealm([
                operationType:      Realm.OperationType.DATA_PROCESSING,
        ] + properties)
    }

    static Realm createRealmDataProcessing(File testDirectory, Map properties = [:]) {
        assert testDirectory.isAbsolute()
        createRealmDataProcessing([
                processingRootPath: new File(testDirectory, 'processing').path,
                stagingRootPath:    new File(testDirectory, 'staging').path,
                loggingRootPath:    new File(testDirectory, 'logging').path,
        ] + properties)
    }

    static Realm createRealm(Map realmProperties = [:]) {
        File fakePath = TestCase.uniqueNonExistentPath
        return createDomainObject(Realm, [
                name              : 'realmName_' + (counter++),
                env               : Environment.current.name,
                rootPath          : { new File(fakePath, 'root').path },
                processingRootPath: { new File(fakePath, 'processing').path },
                loggingRootPath   : { new File(fakePath, 'logging').path },
                programsRootPath  : { new File(fakePath, 'programs').path },
                stagingRootPath   : { new File(fakePath, 'staging').path },
                webHost           : 'test.host.invalid',
                host              : 'test.host.invalid',
                port              : -1,
                unixUser          : '!fakeuser',
                roddyUser         : '!fakeroddyuser',
                timeout           : -1,
                pbsOptions        : '{}',
                cluster           : Cluster.DKFZ,
        ], realmProperties)
    }

    static Pipeline createPanCanPipeline() {
        return createDomainObjectLazy(Pipeline, [:], [
                name: Pipeline.Name.PANCAN_ALIGNMENT,
                type: Pipeline.Type.ALIGNMENT,
        ])
    }

    static Pipeline createDefaultOtpPipeline() {
        return createDomainObjectLazy(Pipeline, [:], [
                name: Pipeline.Name.DEFAULT_OTP,
                type: Pipeline.Type.ALIGNMENT,
        ])
    }

    static Pipeline createOtpSnvPipelineLazy() {
        return createDomainObjectLazy(Pipeline, [:], [
                name: Pipeline.Name.OTP_SNV,
                type: Pipeline.Type.SNV,
        ])
    }

    static Pipeline createRoddySnvPipelineLazy() {
        return createDomainObjectLazy(Pipeline, [:], [
                name: Pipeline.Name.RODDY_SNV,
                type: Pipeline.Type.SNV,
        ])
    }


    static Pipeline returnOrCreateAnyPipeline() {
        return (CollectionUtils.atMostOneElement(Pipeline.list(max: 1)) ?: createPanCanPipeline())
    }

    public static MergingSet createMergingSet(Map properties = [:]) {
        return createMergingSet(createMergingWorkPackage(pipeline: createDefaultOtpPipeline()), properties)
    }

    public static MergingSet createMergingSet(final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return createDomainObject(MergingSet, [
                mergingWorkPackage: mergingWorkPackage,
                identifier: MergingSet.nextIdentifier(mergingWorkPackage),
        ], properties)
    }

    public static QualityAssessmentMergedPass createQualityAssessmentMergedPass(Map properties = [:]) {
        return createDomainObject(QualityAssessmentMergedPass, [
                abstractMergedBamFile: {createProcessedMergedBamFile()}
        ], properties)
    }

    public static Map defaultValuesForAbstractQualityAssessment = [
            qcBasesMapped: 0,
            totalReadCounter: 0,
            qcFailedReads: 0,
            duplicates: 0,
            totalMappedReadCounter: 0,
            pairedInSequencing: 0,
            pairedRead1: 0,
            pairedRead2: 0,
            properlyPaired: 0,
            withItselfAndMateMapped: 0,
            withMateMappedToDifferentChr: 0,
            withMateMappedToDifferentChrMaq: 0,
            singletons: 0,
            insertSizeMedian: 0,
            insertSizeSD: 0,
    ].asImmutable()

    public static OverallQualityAssessmentMerged createOverallQualityAssessmentMerged(Map properties = [:]) {
        return createDomainObject(OverallQualityAssessmentMerged, defaultValuesForAbstractQualityAssessment + [
                qualityAssessmentMergedPass: {createQualityAssessmentMergedPass()},
        ], properties)
    }

    public static ChromosomeQualityAssessmentMerged createChromosomeQualityAssessmentMerged(Map properties = [:]) {
        return createDomainObject(ChromosomeQualityAssessmentMerged, defaultValuesForAbstractQualityAssessment + [
                chromosomeName: "chromosomeName_${counter++}",
                qualityAssessmentMergedPass: {createQualityAssessmentMergedPass()},
        ], properties)
    }

    public static JobExecutionPlan createJobExecutionPlan(Map properties = [:]) {
        return createDomainObject(JobExecutionPlan, [
                name: "planName_${counter++}",
                planVersion: 0,
                obsoleted: false,
        ], properties)
    }

    public static Process createProcess(Map properties = [:]) {
        return createDomainObject(Process, [
                started: new Date(),
                startJobClass: "startJobClass",
                startJobVersion: "startJobVersion",
                jobExecutionPlan: {createJobExecutionPlan()}
        ], properties)
    }

    public static JobDefinition createJobDefinition(Map properties = [:]) {
        return createDomainObject(JobDefinition, [
                plan: {createJobExecutionPlan()},
                name: "name_${counter++}",
                bean: 'beanName',
        ], properties)
    }

    public static ProcessParameter createProcessParameter(Map properties = [:]) {
        return createDomainObject(ProcessParameter, [
                process: { createProcessingStep().process },
                value: "${counter++}",
                className: "${counter++}",
        ], properties)
    }

    public static ProcessingStep createProcessingStep(Map properties = [:]) {
        JobExecutionPlan jobExecutionPlan = properties.jobDefinition?.plan ?: properties.process?.jobExecutionPlan ?: createJobExecutionPlan()
        return createDomainObject(ProcessingStep, [
                jobDefinition: {createJobDefinition(plan: jobExecutionPlan)},
                jobClass: 'someClass',
                jobVersion: '0',
                process: {createProcess(jobExecutionPlan: jobExecutionPlan)},
        ], properties)
    }

    public static ProcessingStepUpdate createProcessingStepUpdate(Map properties = [:]) {
        return createDomainObject(ProcessingStepUpdate, [
                processingStep: {createProcessingStep()},
                state: ExecutionState.CREATED,
                previous: {properties.step ? properties.step.latestProcessingStepUpdate : null },
                date: new Date(),
        ], properties)
    }

    static ProcessingError createProcessingError(Map properties = [:]) {
        return createDomainObject(ProcessingError, [
                errorMessage        : "errorMessage_${counter++}",
                processingStepUpdate: {
                    ProcessingStep step = createProcessingStep()
                    ProcessingStepUpdate update = createProcessingStepUpdate([processingStep: step])
                    createProcessingStepUpdate([
                            state         : ExecutionState.FAILURE,
                            processingStep: step,
                            previous      : update,
                    ])
                },
        ], properties)
    }

    public static ClusterJob createClusterJob(Map properties = [:]) {
        return createDomainObject(ClusterJob, [
                processingStep: {createProcessingStep()},
                realm: {createRealmDataProcessing()},
                clusterJobId: "clusterJobId_${counter++}",
                userName: "userName_${counter++}",
                clusterJobName: "clusterJobName_${counter++}_jobClass",
                jobClass: "jobClass",
                queued: new DateTime(),
        ], properties)
    }

    public static ProcessingOption createProcessingOption(Map properties = [:]) {
        return createDomainObject(ProcessingOption, [
                 name: "processingOptionName_${counter++}",
                 type: "processingOptionType_${counter++}",
                 value:  "processingOptionValue_${counter++}",
                 comment: "processingOptionComment_${counter++}",
        ], properties)
    }

    static JobErrorDefinition createJobErrorDefinition(Map properties = [:]) {
        return createDomainObject(JobErrorDefinition, [
                type : JobErrorDefinition.Type.MESSAGE,
                action: JobErrorDefinition.Action.STOP,
                errorExpression: /.*/,
                jobDefinitions: { (1..3).collect {
                    createJobDefinition()
                }}
        ], properties)
    }

    public static void createProcessingOptionForOtrsTicketPrefix(String prefix){
        ProcessingOptionService option = new ProcessingOptionService()
        option.createOrUpdate(
                TrackingService.TICKET_NUMBER_PREFIX,
                null,
                null,
                prefix,
                "comment to the number prefix"
        )
    }

    public static MergingPass createMergingPass(final MergingSet mergingSet) {
        return new MergingPass(
                mergingSet: mergingSet,
                identifier: MergingPass.nextIdentifier(mergingSet),
        ).save(flush: true)
    }

    public static Map getRandomProcessedBamFileProperties() {
        return [
                fileSize           : ++counter,
                md5sum             : HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
        ]
    }

    public static AdapterFile createAdapterFile(Map properties = [:]) {
        return createDomainObject(AdapterFile, [
                fileName: "fileName_${counter++}",
        ], properties)
    }

    public static createComment(Map properties = [:]) {
        return createDomainObject(Comment, [
                comment: "comment ${counter++}",
                author:  "author ${counter++}",
                modificationDate: {new Date()},
        ], properties)
    }

    public static createIlseSubmission(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(IlseSubmission, [
                ilseNumber: { counter++ % 999000 + 1000 },
                warning: false,
                ], properties, saveAndValidate)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        MergingSet mergingSet = createMergingSet(mergingWorkPackage)
        return createProcessedMergedBamFile(mergingSet, properties)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(Map properties = [:]) {
        return createProcessedMergedBamFile(MergingSet.build(), properties)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingSet mergingSet, Map properties = [:]) {
        assignNewProcessedBamFile(mergingSet)
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.build([
                mergingPass: MergingPass.build(
                        mergingSet: mergingSet,
                        identifier: MergingPass.nextIdentifier(mergingSet),
                ),
                numberOfMergedLanes: 1,
                workPackage: mergingSet.mergingWorkPackage,
        ] + properties)
        return bamFile
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass, Map properties = [:]) {
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile([
                mergingPass: mergingPass,
                workPackage: mergingPass.mergingWorkPackage,
                type: AbstractBamFile.BamType.SORTED,
                numberOfMergedLanes: 1,
        ] + properties)
        // has to be set explicitly to null due strange behavior of GORM (?)
        bamFile.mergingPass.mergingWorkPackage.bamFileInProjectFolder = null
        return bamFile
    }

    public static ProcessedBamFile assignNewProcessedBamFile(final ProcessedMergedBamFile processedMergedBamFile) {
        final ProcessedBamFile bamFile = assignNewProcessedBamFile(processedMergedBamFile.mergingSet)
        processedMergedBamFile.numberOfMergedLanes++
        return bamFile
    }

    public static ProcessedBamFile assignNewProcessedBamFile(final MergingSet mergingSet) {
        final ProcessedBamFile bamFile = createProcessedBamFile(mergingSet.mergingWorkPackage)
        MergingSetAssignment.build(mergingSet: mergingSet, bamFile: bamFile)
        return bamFile
    }

    public static ProcessedBamFile createProcessedBamFile(final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {

        SeqTrack seqTrack = createSeqTrackWithDataFiles(mergingWorkPackage)

        final ProcessedBamFile bamFile = ProcessedBamFile.build([
                alignmentPass: TestData.createAndSaveAlignmentPass(
                        seqTrack: seqTrack,
                        workPackage: mergingWorkPackage,
                        referenceGenome: mergingWorkPackage.referenceGenome,
                ),
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status: AbstractBamFile.State.PROCESSED,
        ] + properties)

        return bamFile
    }

    public static createRoddyBamFile(Map bamFileProperties = [:]) {
        MergingWorkPackage workPackage = bamFileProperties.workPackage
        if (!workPackage) {
            SeqType seqType = createWholeGenomeSeqType()
            Pipeline pipeline = createPanCanPipeline()
            workPackage = createMergingWorkPackage(
                    pipeline: pipeline,
                    seqType: seqType,
            )
            createReferenceGenomeProjectSeqType(
                    referenceGenome: workPackage.referenceGenome,
                    project: workPackage.project,
                    seqType: workPackage.seqType,
                    statSizeFileName: workPackage.statSizeFileName,
            )
        }
        SeqTrack seqTrack = createSeqTrackWithDataFiles(workPackage)
        RoddyBamFile bamFile = createDomainObject(RoddyBamFile, [
                numberOfMergedLanes: 1,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks: [seqTrack],
                workPackage: workPackage,
                identifier: RoddyBamFile.nextIdentifier(workPackage),
                config: { createRoddyWorkflowConfig(
                        pipeline: workPackage.pipeline,
                        project: workPackage.project,
                )},
                md5sum: HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
                fileSize: 10000,
                ], bamFileProperties)
        return bamFile
    }

    public static createRoddyBamFile(RoddyBamFile baseBamFile, Map bamFileProperties = [:]) {
        RoddyBamFile bamFile = RoddyBamFile.build([
                baseBamFile: baseBamFile,
                config: bamFileProperties.config ?: createRoddyWorkflowConfig(pipeline: baseBamFile.config.pipeline),
                workPackage: baseBamFile.workPackage,
                identifier: baseBamFile.identifier + 1,
                numberOfMergedLanes: baseBamFile.numberOfMergedLanes + 1,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks: bamFileProperties.seqTracks ?: [createSeqTrackWithDataFiles(baseBamFile.workPackage)],
                md5sum: HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
                fileSize: 10000,
                ] + bamFileProperties
        )
        bamFile.save(flush: true)
        return bamFile
    }

    /**
     * Because RoddyMergedBamQa defines a unique constraint with 'class', the instance can only be created in integration tests.
     */
    public static createRoddyMergedBamQa(Map properties = [:]) {
        return createDomainObject(RoddyMergedBamQa, defaultValuesForAbstractQualityAssessment + [
                qualityAssessmentMergedPass: { createQualityAssessmentMergedPass(
                        abstractMergedBamFile: createRoddyBamFile()
                ) },
                chromosome: RoddyQualityAssessment.ALL,
                insertSizeCV: 0,
                percentageMatesOnDifferentChr: 0,
                genomeWithoutNCoverageQcBases: 0,
        ], properties)
    }

    /**
     * Because RoddyMergedBamQa defines a unique constraint with 'class', the instance can only be created in integration tests.
     */
    public static createRoddyMergedBamQa(RoddyBamFile roddyBamFile, Map properties = [:]) {
        return createRoddyMergedBamQa([
                qualityAssessmentMergedPass: createQualityAssessmentMergedPass(
                        abstractMergedBamFile: roddyBamFile
                )
        ] + properties)
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties as the specified one but a different
     * {@link SampleType}.
     */
    public static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base) {
        return createMergingWorkPackage(base, createSampleType())
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties as the specified one but a different
     * {@link SampleType}.
     */
    public static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base, SampleType sampleType) {
        Sample sample = new Sample(
                individual: base.individual,
                sampleType: sampleType,
                libraryPreparationKit: base.libraryPreparationKit,
        )
        assert sample.save(failOnError: true)
        return createMergingWorkPackage(base, sample)
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties as the specified one but a different
     * {@link Sample}.
     */
    static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base, Sample sample) {
        return createMergingWorkPackage(base, [sample: sample])
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties except for the specified ones.
     */
    static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base, Map properties) {
        MergingWorkPackage mwp = new MergingWorkPackage((MergingWorkPackage.seqTrackPropertyNames +
                MergingWorkPackage.processingParameterNames).collectEntries{[it, base."${it}"]} + properties)
        assert mwp.save(failOnError: true)
        return mwp
    }

    static SamplePair createSamplePair(Map properties = [:]) {
        MergingWorkPackage mergingWorkPackage1 = createMergingWorkPackage()
        SampleTypePerProject.build(
                sampleType: mergingWorkPackage1.sampleType,
                project: mergingWorkPackage1.project,
                category: SampleType.Category.DISEASE,
        )
        return createSamplePair(mergingWorkPackage1, properties)
    }

    static SamplePair createSamplePair(MergingWorkPackage mergingWorkPackage1, Map properties = [:]) {
        return createSamplePair(
                mergingWorkPackage1,
                createMergingWorkPackage(mergingWorkPackage1),
                properties)
    }

    static SampleTypePerProject createSampleTypePerProject(Map properties = [:]) {
        return createDomainObject(SampleTypePerProject, [
                project   : { createProject() },
                sampleType: { createSampleType() },
                category  : SampleType.Category.DISEASE,
        ], properties)
    }

    static SamplePair createSamplePair(MergingWorkPackage mergingWorkPackage1, MergingWorkPackage mergingWorkPackage2, Map properties = [:]) {
        SamplePair samplePair = SamplePair.createInstance([
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ] + properties)
        return samplePair.save(failOnError: true)
    }

    static SamplePair createDisease(MergingWorkPackage controlMwp) {
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(controlMwp)
        createSampleTypePerProject(project: controlMwp.project, sampleType: diseaseMwp.sampleType, category: SampleType.Category.DISEASE)
        SamplePair samplePair = createSamplePair(diseaseMwp, controlMwp)
        return samplePair
    }

    public static SnvConfig createSnvConfigForSnvCallingInstance(SnvCallingInstance snvCallingInstance) {
        ExternalScript externalScript = ExternalScript.buildLazy (
                scriptVersion: "v1",
                deprecatedDate: null,
        )

        SnvConfig.buildLazy (
                project: snvCallingInstance.project,
                seqType: snvCallingInstance.seqType,
                externalScriptVersion: externalScript.scriptVersion,
                pipeline: DomainFactory.createOtpSnvPipelineLazy(),
        )
    }

    public static SnvConfig createSnvConfig(Map properties = [:]) {
        return createDomainObject(SnvConfig, [
                configuration: "configuration_${counter++}",
                externalScriptVersion: { DomainFactory.createExternalScript().scriptVersion },
                seqType: { createSeqType() },
                project: { createProject() },
                pipeline: { createOtpSnvPipelineLazy() }
        ], properties)
    }

    public static SnvCallingInstance createSnvInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Pipeline pipeline = createPanCanPipeline()

        MergingWorkPackage controlWorkPackage = createMergingWorkPackage(pipeline: pipeline, statSizeFileName: DEFAULT_TAB_FILE_NAME)
        SamplePair samplePair = createDisease(controlWorkPackage)
        MergingWorkPackage diseaseWorkPackage = samplePair.mergingWorkPackage1

        RoddyBamFile disease = createRoddyBamFile([workPackage: diseaseWorkPackage] + bamFile1Properties)
        RoddyBamFile control = createRoddyBamFile([workPackage: controlWorkPackage, config: disease.config] + bamFile2Properties)

        ExternalScript externalScript = DomainFactory.createExternalScript()

        SnvConfig snvConfig = createSnvConfig(
                seqType: samplePair.seqType,
                externalScriptVersion: externalScript.scriptVersion
        )

        SnvCallingInstance snvCallingInstance = createSnvCallingInstance([
                instanceName: "2014-08-25_15h32",
                samplePair: samplePair,
                sampleType1BamFile: disease,
                sampleType2BamFile: control,
                config: snvConfig,
                latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(disease, control),
        ] + properties)
        assert snvCallingInstance.save(flush: true)
        return snvCallingInstance
    }

    public static SnvJobResult createSnvJobResultWithRoddyBamFiles(Map properties = [:]) {
        Map map = [
                step: SnvCallingStep.CALLING,
                processingState: SnvProcessingStates.FINISHED,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: DEFAULT_FILE_SIZE,
        ] + properties

        if (map.step == SnvCallingStep.CALLING) {
            if (!map.snvCallingInstance) {
                map.snvCallingInstance = createSnvInstanceWithRoddyBamFiles()
            }
            if (!map.chromosomeJoinExternalScript) {
                map.chromosomeJoinExternalScript = ExternalScript.buildLazy(
                        scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                        scriptVersion: map.snvCallingInstance.config.externalScriptVersion,
                )
            }
        } else {
            if (!map.inputResult) {
                map.inputResult = createSnvJobResultWithRoddyBamFiles(snvCallingInstance: map.snvCallingInstance)
            }
            if (!map.snvCallingInstance) {
                map.snvCallingInstance = map.inputResult?.snvCallingInstance
            }
        }
        if (!map.externalScript) {
            map.externalScript = ExternalScript.buildLazy(
                    scriptIdentifier: map.step.externalScriptIdentifier,
                    scriptVersion: map.snvCallingInstance.config.externalScriptVersion,
            )
        }

        return createSnvJobResult(map)
    }


    public static SnvJobResult createSnvJobResult(Map properties) {
        return createDomainObject(SnvJobResult, [
                step: SnvCallingStep.CALLING,
                externalScript: { createExternalScript() },
                snvCallingInstance: { createSnvCallingInstance() }
        ], properties)
    }


    public static ExternalScript createExternalScript(Map properties = [:]) {
        return createDomainObject(ExternalScript, [
            scriptIdentifier: "scriptIdentifier_${counter++}",
            scriptVersion: "scriptVersion_${counter++}",
            filePath: TestCase.uniqueNonExistentPath,
            author: "author_${counter++}",
        ], properties)
    }


    public static AntibodyTarget createAntibodyTarget(Map properties = [:]) {
        return createDomainObject(AntibodyTarget, [
                name: 'antibodyTargetName_' + (counter++),
        ], properties)
    }

    public static SeqCenter createSeqCenter(Map seqCenterProperties = [:]) {
        return createDomainObject(SeqCenter, [
                name   : 'seqCenterName_' + (counter++),
                dirName: 'seqCenterDirName_' + (counter++),
        ], seqCenterProperties)
    }

    public static SeqPlatform createSeqPlatform(Map seqPlatformProperties = [:]) {
        return createDomainObject(SeqPlatform, [
                name: 'seqPlatform_' + (counter++),
                seqPlatformGroup: { createSeqPlatformGroup() },
        ], seqPlatformProperties)
    }

    public static SeqPlatformModelLabel createSeqPlatformModelLabel(Map properties = [:]) {
        return createDomainObject(SeqPlatformModelLabel, [
                name: 'seqPlatformModelLabel_' + (counter++),
        ], properties)
    }

    public static SequencingKitLabel createSequencingKitLabel(Map properties = [:]) {
        return createDomainObject(SequencingKitLabel, [
                name: 'SequencingKitLabel_' + (counter++),
        ], properties)
    }

    public static SeqPlatformGroup createSeqPlatformGroup(Map properties = [:]) {
        return createDomainObject(SeqPlatformGroup, [
                name: 'seqPlatformGroup_' + (counter++),
        ], properties)
    }

    public static Run createRun(Map runProperties = [:]) {
        return createDomainObject(Run, [
                name       : 'runName_' + (counter++),
                seqCenter  : { createSeqCenter() },
                seqPlatform: { createSeqPlatform() },
        ], runProperties)
    }

    public static RunSegment createRunSegment(Map runSegmentProperties = [:]) {
        return createDomainObject(RunSegment, [
                :
        ], runSegmentProperties)
    }

    public static Project createProject(Map projectProperties = [:]) {
        return createDomainObject(Project, [
                name                    : 'project_' + (counter++),
                dirName                 : 'projectDirName_' + (counter++),
                realmName               : 'realmName_' + (counter++),
                alignmentDeciderBeanName: 'DUMMY_BEAN_NAME',
                category                : { createProjectCategory() },
        ], projectProperties)
    }

    public static ProjectCategory createProjectCategory(Map projectProperties = [:]) {
        return createDomainObject(ProjectCategory, [
                name: 'projectCategory_' + (counter++),
        ], projectProperties)
    }

    public static Project createProjectWithRealms(Map projectProperties = [:]) {
        Project project = createProject(projectProperties)
        createRealmDataManagement(name: project.realmName)
        createRealmDataProcessing(name: project.realmName)
        return project
    }

    public static Individual createIndividual(Map individualProperties = [:]) {
        return createDomainObject(Individual, [
                pid         : 'pid_' + (counter++),
                mockPid     : 'mockPid_' + (counter++),
                mockFullName: 'mockFullName_' + (counter++),
                type        : Individual.Type.REAL,
                project     : { createProject() },
        ], individualProperties)
    }

    public static SampleType createSampleType(Map sampleTypeProperties = [:]) {
        return createDomainObject(SampleType, [
                name: 'sampleTypeName_' + (counter++),
                specificReferenceGenome: SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ], sampleTypeProperties)
    }

    public static Sample createSample(Map sampleProperties = [:]) {
        return createDomainObject(Sample, [
                individual: { createIndividual() },
                sampleType: { createSampleType() },
        ], sampleProperties)
    }

    public static SampleIdentifier createSampleIdentifier(Map properties = [:]) {
        return createDomainObject(SampleIdentifier, [
                sample: { createSample() },
                name: 'sampleIdentifierName_' + (counter++),
        ], properties)
    }

    public static SeqType createSeqType(Map seqTypeProperties = [:], boolean saveAndValidate = true) {
        String defaultName = 'seqTypeName_' + (counter++)
        return createDomainObject(SeqType, [
                name         : defaultName,
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                dirName      : 'seqTypeDirName_' + (counter++),
                displayName  : seqTypeProperties.get('alias') ?: seqTypeProperties.get('name') ?: defaultName,
        ], seqTypeProperties, saveAndValidate)
    }

    public static SoftwareTool createSoftwareTool(Map softwareToolProperties = [:]) {
        return createDomainObject(SoftwareTool, [
                programName: 'softwareToolProgramName_' + (counter++),
                type       : SoftwareTool.Type.ALIGNMENT,
        ], softwareToolProperties)
    }


    public static SoftwareToolIdentifier createSoftwareToolIdentifier(Map properties = [:]) {
        return createDomainObject(SoftwareToolIdentifier, [
                name: 'softwareToolIdentifier_' + (counter++),
                softwareTool: { createSoftwareTool() },
        ], properties)
    }


    static LibraryPreparationKit createLibraryPreparationKit(Map properties = [:]) {
        return createDomainObject(LibraryPreparationKit, [
                name: "name_${counter++}",
                shortDisplayName: "name_${counter++}",
        ], properties)
    }

    static ReferenceGenome createReferenceGenome(Map properties = [:]) {
        return createDomainObject(ReferenceGenome, [
                name                        : "name${counter++}",
                path                        : HelperUtils.uniqueString,
                fileNamePrefix              : "prefix_${counter++}",
                length                      : 1,
                lengthWithoutN              : 1,
                lengthRefChromosomes        : 1,
                lengthRefChromosomesWithoutN: 1,
        ], properties)
    }

    static ReferenceGenomeEntry createReferenceGenomeEntry(Map properties = [:]) {
        return createDomainObject(ReferenceGenomeEntry, [
                referenceGenome: { createReferenceGenome() },
                classification: ReferenceGenomeEntry.Classification.CHROMOSOME,
                name: "name${counter++}",
                alias: "alias${counter++}",
        ], properties)
    }

    static List<ReferenceGenomeEntry> createReferenceGenomeEntries(ReferenceGenome referenceGenome = createReferenceGenome(), Collection<String> chromosomeNames) {
        return chromosomeNames.collect {
            createReferenceGenomeEntry([
                    name: it,
                    alias: it,
                    referenceGenome: referenceGenome,
                    classification: ReferenceGenomeEntry.Classification.CHROMOSOME,
            ])
        }
    }


    static ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Map properties = [:]) {
        return createDomainObject(ReferenceGenomeProjectSeqType, [
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

    static Map seqTrackProperties() {
        return [
                laneId         : 'laneId_' + counter++,
                sample         : { createSample() },
                pipelineVersion: { createSoftwareTool() },
                run            : { createRun() },
        ]
    }

    public static SeqTrack createSeqTrack(Map properties = [:]) {
        return createDomainObject(SeqTrack, seqTrackProperties() + [
                seqType        : { createSeqType() },
                kitInfoReliability: properties.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ], properties)
    }

    public static ExomeSeqTrack createExomeSeqTrack(Map exomSeqTrackProperties = [:]) {
        return createDomainObject(ExomeSeqTrack, seqTrackProperties() + [
                seqType        : { createExomeSeqType() },
        ], exomSeqTrackProperties)
    }

    public static ChipSeqSeqTrack createChipSeqSeqTrack(Map properties = [:]) {
        return createDomainObject(ChipSeqSeqTrack, seqTrackProperties() + [
                seqType        : { createChipSeqType() },
                antibodyTarget : { createAntibodyTarget() },
        ], properties)
    }

    public static FastqcProcessedFile createFastqcProcessedFile(Map properties = [:]) {
        return createDomainObject(FastqcProcessedFile, [
                dataFile: { createDataFile() },
        ], properties)
    }

    public static MergingWorkPackage createMergingWorkPackage(Map properties = [:]) {
        return createDomainObject(MergingWorkPackage, [
                libraryPreparationKit: { properties.get('seqType')?.isWgbs() ? null : createLibraryPreparationKit() },
                sample:                { createSample() },
                seqType:               { createSeqType() },
                seqPlatformGroup:      { createSeqPlatformGroup() },
                referenceGenome:       { createReferenceGenome() },
                statSizeFileName:      { properties.get('pipeline')?.name == Pipeline.Name.PANCAN_ALIGNMENT ?
                                            "statSizeFileName_${counter++}.tab" : null },
                pipeline:              { createDefaultOtpPipeline() },
        ], properties)
    }

    static createFileType(Map properties = [:]) {
        return createDomainObject(FileType, [
                type: FileType.Type.SEQUENCE,
                vbpPath: "sequence_${counter++}",
        ], properties)
    }

    static DataFile createDataFile(Map properties = [:], boolean saveAndValidate = true) {
        SeqTrack seqTrack
        if (properties.containsKey('seqTrack')) {
            seqTrack = properties.seqTrack
        } else {
            seqTrack = createSeqTrack(properties.containsKey('run') ? [run: properties.run] : [:])
        }
        return createDomainObject(DataFile, [
                seqTrack: seqTrack,
                project: seqTrack?.project,
                run: seqTrack?.run,
                runSegment: { createRunSegment() },
                fileName: "DataFileFileName_${counter}_R1.gz",
                vbpFileName: "VbpDataFileFileName_${counter}_R1.gz",
                pathName: "path_${counter}",
                initialDirectory: TestCase.getUniqueNonExistentPath().path,
                md5sum: {HelperUtils.getRandomMd5sum()},
                dateExecuted: new Date(),
                dateFileSystem: new Date(),
                dateCreated: new Date(),
                fileWithdrawn: false,
                fileType: {createFileType()},
                used: true,
                fileExists: true,
                fileLinked: true,
                fileSize: counter++,
                mateNumber: 1,
        ], properties, saveAndValidate)
    }

    static RoddyWorkflowConfig createRoddyWorkflowConfig(Map properties = [:], boolean saveAndValidate = true) {
        Pipeline pipeline = properties.containsKey('pipeline') ? properties.pipeline : createPanCanPipeline()
        SeqType seqType =  properties.containsKey('seqType') ? properties.seqType : createWholeGenomeSeqType()
        String pluginVersion = properties.containsKey('pluginVersion') ? properties.pluginVersion : "pluginVersion:1.1.${counter++}"
        String configVersion = properties.containsKey('configVersion') ? properties.configVersion : "v1_${counter++}"

        return createDomainObject(RoddyWorkflowConfig, [
                pipeline: pipeline,
                seqType: seqType,
                configFilePath: {"${TestCase.uniqueNonExistentPath}/${pipeline.name.name()}_${seqType.roddyName}_${pluginVersion.substring(pluginVersion.indexOf(':') + 1)}_${configVersion}.xml"},
                pluginVersion: pluginVersion,
                configVersion: configVersion,
                project: { properties.individual?.project ?: createProject() },
                dateCreated: {new Date()},
                lastUpdated: {new Date()},
        ], properties, saveAndValidate)
    }

    public static SeqTrack createSeqTrack(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:]) {
        return createSeqTrack(getMergingProperties(mergingWorkPackage) + seqTrackProperties)
    }

    public static Map getMergingProperties(MergingWorkPackage mergingWorkPackage) {
        return [
                sample: mergingWorkPackage.sample,
                seqType: mergingWorkPackage.seqType,
                libraryPreparationKit: mergingWorkPackage.libraryPreparationKit,
                run: createRun(seqPlatform: createSeqPlatform(seqPlatformGroup: mergingWorkPackage.seqPlatformGroup)),
        ]
    }

    public static SeqTrack createSeqTrackWithDataFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        Map map = getMergingProperties(mergingWorkPackage) + [
                kitInfoReliability:  mergingWorkPackage.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ] + seqTrackProperties
        SeqTrack seqTrack
        if (mergingWorkPackage.seqType.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED) {
            seqTrack = createSeqTrackWithTwoDataFiles(map, dataFileProperties, dataFileProperties)
        } else {
            seqTrack = createSeqTrackWithOneDataFile(map, dataFileProperties)
        }
        assert mergingWorkPackage.satisfiesCriteria(seqTrack)
        return seqTrack
    }

    public static SeqTrack createSeqTrackWithOneDataFile(Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        SeqTrack seqTrack
        if (seqTrackProperties.seqType?.name == SeqTypeNames.EXOME.seqTypeName) {
            seqTrack = createExomeSeqTrack(seqTrackProperties)
        } else {
            seqTrack = createSeqTrack(seqTrackProperties)
        }
        createSequenceDataFile(dataFileProperties + [seqTrack: seqTrack])
        return seqTrack
    }

    public static SeqTrack createSeqTrackWithTwoDataFiles(Map seqTrackProperties = [:], Map dataFileProperties1 = [:], Map dataFileProperties2 = [:]) {
        FileType fileType = DomainFactory.createFileType()
        Map defaultMap1 = [
                fileName: 'DataFileFileName_R1.gz',
                vbpFileName: 'DataFileFileName_R1.gz',
                fileType: fileType,
                mateNumber: 1,
        ]
        Map defaultMap2 = [
                fileName: 'DataFileFileName_R2.gz',
                vbpFileName: 'DataFileFileName_R2.gz',
                fileType: fileType,
                mateNumber: 2,
        ]
        SeqTrack seqTrack = createSeqTrackWithOneDataFile([seqType: createSeqType(libraryLayout: LibraryLayout.PAIRED)] + seqTrackProperties, defaultMap1 + dataFileProperties1)
        createSequenceDataFile(defaultMap2 + dataFileProperties2 + [seqTrack: seqTrack])
        return seqTrack
    }

    public static DataFile createSequenceDataFile(final Map properties = [:]) {
        Map defaultProperties = [
                dateCreated: new Date(),  // In unit tests Grails (sometimes) does not automagically set dateCreated.
                used       : true,
        ]
        if (properties.seqTrack) {
            defaultProperties.project = properties.seqTrack.project
            defaultProperties.run = properties.seqTrack.run
        }
        return createDataFile(defaultProperties + properties)
    }

    public static SnvCallingInstance createSnvCallingInstance(Map properties) {
        if (!properties.containsKey('latestDataFileCreationDate')) {
            properties += [latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(
                    properties.sampleType1BamFile, properties.sampleType2BamFile)]
        }
        return new SnvCallingInstance(properties)
    }

    public static SnvCallingInstance createSnvCallingInstanceBasedOnPreviousSnvCallingInstance(SnvCallingInstance snvCallingInstance, Map properties = [:]) {
        return createSnvCallingInstance([
                instanceName: "instance_${counter++}",
                samplePair: snvCallingInstance.samplePair,
                sampleType1BamFile: snvCallingInstance.sampleType1BamFile,
                sampleType2BamFile: snvCallingInstance.sampleType2BamFile,
                config: snvCallingInstance.config,
        ] + properties).save(flush: true)
    }

    public static ProcessingStep createAndSaveProcessingStep(ProcessParameterObject processParameterObject = null) {
        return createAndSaveProcessingStep("de.dkfz.tbi.otp.test.job.jobs.NonExistentDummyJob", processParameterObject)
    }

    public static ProcessingStep createAndSaveProcessingStep(String jobClass, ProcessParameterObject processParameterObject = null) {
        final JobExecutionPlan jep = new JobExecutionPlan(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0, startJobBean: "DontCare")
        assert jep.save()
        final JobDefinition jobDefinition = new JobDefinition(name: "DontCare", bean: "DontCare", plan: jep)
        assert jobDefinition.save()
        final Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "DontCare", startJobVersion: "1")
        assert process.save()
        if (processParameterObject != null) {
            createProcessParameter(process, processParameterObject)
        }
        final ProcessingStep step = new ProcessingStep(jobDefinition: jobDefinition, process: process, jobClass: jobClass)
        assert step.save()
        final ProcessingStepUpdate update = createProcessingStepUpdate(step, ExecutionState.CREATED)
        assert update.save(flush: true)
        return step
    }

    public static RestartedProcessingStep createAndSaveRestartedProcessingStep(ProcessingStep step = null) {
        ProcessingStep originalStep = step ?: createAndSaveProcessingStep()
        final RestartedProcessingStep restartedProcessingStep = RestartedProcessingStep.create(originalStep)
        assert restartedProcessingStep.save(flush: true)
        return restartedProcessingStep
    }

    public static ProcessingStepUpdate createProcessingStepUpdate(final ProcessingStep step, final ExecutionState state) {
        return new ProcessingStepUpdate(
                date: new Date(),
                state: state,
                previous: step.latestProcessingStepUpdate,
                processingStep: step,
        )
    }

    public static ProcessingStep createProcessingStepWithUpdates(ProcessingStep processingStep = DomainFactory.createProcessingStep()) {
        ProcessingStepUpdate last = DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.CREATED)
        last = DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.STARTED, previous: last)
        last = DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.FINISHED, previous: last)
        DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.SUCCESS, previous: last)
        return processingStep
    }

    public static ProcessParameter createProcessParameter(final Process process, final ProcessParameterObject parameterValue, Map properties = [:]) {
        return createDomainObject(ProcessParameter, [
                process: process,
                className: parameterValue.class.name,
                value: parameterValue.id.toString(),
        ], properties)
    }

    public static ProcessParameter createProcessParameter(final Process process, final String className, final String value) {
        return new ProcessParameter(
                process: process,
                className: className,
                value: value,
        )
    }

    public static ClusterJob createClusterJob(
            final ProcessingStep processingStep, final ClusterJobIdentifier clusterJobIdentifier,
            final Map myProps = [
                    clusterJobName: "testName_${processingStep.nonQualifiedJobClass}",
                    jobClass: processingStep.nonQualifiedJobClass,
                    queued: new DateTime(),
                    requestedWalltime: Duration.standardMinutes(5),
                    requestedCores: 10,
                    requestedMemory: 1000,
            ]) {
        return new ClusterJob([
                processingStep: processingStep,
                realm: clusterJobIdentifier.realm,
                clusterJobId: clusterJobIdentifier.clusterJobId,
                userName: clusterJobIdentifier.userName,
        ] + myProps)
    }

    static SeqType createSeqTypeLazy(SeqTypeNames seqTypeNames, String alias, String dirName, String roddyName = '', String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED) {
        createDomainObjectLazy(SeqType, [:], [
                name: seqTypeNames.seqTypeName,
                alias: alias,
                roddyName: roddyName,
                dirName: dirName,
                libraryLayout: libraryLayout,
        ]).refresh()
    }

    static SeqType createWholeGenomeSeqType() {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME, 'WGS', 'whole_genome_sequencing', 'WGS')
    }

    static SeqType createExomeSeqType() {
        createSeqTypeLazy(SeqTypeNames.EXOME, 'EXOME', 'exon_sequencing', 'WES')
    }

    static SeqType createWholeGenomeBisulfiteSeqType() {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE, 'WGBS', 'whole_genome_bisulfite_sequencing', 'WGBS')
    }

    static SeqType createWholeGenomeBisulfiteTagmentationSeqType() {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION, 'WGBS_TAG', 'whole_genome_bisulfite_tagmentation_sequencing', 'WGBSTAG')
    }

    static SeqType createChipSeqType() {
        createSeqTypeLazy(SeqTypeNames.CHIP_SEQ, 'ChIP', 'chip_seq_sequencing', "CHIPSEQ")
    }

    static List<SeqType> createAlignableSeqTypes() {
        [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createPanCanAlignableSeqTypes() {
        [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
                createWholeGenomeBisulfiteSeqType(),
                createWholeGenomeBisulfiteTagmentationSeqType(),
        ]
    }


    static MetaDataEntry createMetaDataKeyAndEntry(DataFile dataFile, String key, String value) {
        MetaDataKey metaDataKey = MetaDataKey.buildLazy(name: key)

        return MetaDataEntry.build(
            value: value,
            dataFile: dataFile,
            key: metaDataKey,
            status: MetaDataEntry.Status.VALID,
            source: MetaDataEntry.Source.MDFILE,
            )
    }

    static MetaDataEntry createMetaDataKeyAndEntry(DataFile dataFile, MetaDataColumn key, String value) {
        return createMetaDataKeyAndEntry(dataFile, key.name(), value)
    }

    static MetaDataFile createMetaDataFile(Map properties = [:]) {
        return createDomainObject(MetaDataFile,[
                fileName: "MetaDataFileName_${counter++}",
                filePath: TestCase.getUniqueNonExistentPath().path,
                runSegment: { createRunSegment() },
        ], properties)
    }

    static OtrsTicket createOtrsTicket(Map properties = [:]) {
        return createDomainObject(OtrsTicket,[
                ticketNumber: "20000101"+String.format("%08d", counter++),
        ], properties)
    }

    static void createRoddyProcessingOptions(File basePath) {

        ProcessingOption processingOptionPath = new ProcessingOption(
            name: "roddyPath",
            type: "",
            project: null,
            value: "${basePath}/roddy/",
            comment: "Path to the roddy.sh on the current cluster (***REMOVED***cluster 13.1)",
        )
        assert processingOptionPath.save(flush: true)

        ProcessingOption processingOptionBaseConfigsPath = new ProcessingOption(
                name: "roddyBaseConfigsPath",
                type: "",
                project: null,
                value: "${basePath}/roddyBaseConfigs/",
                comment: "Path to the baseConfig-files which are needed to execute Roddy",
        )
        assert processingOptionBaseConfigsPath.save(flush: true)

        ProcessingOption processingOptionApplicationIni = new ProcessingOption(
                name: "roddyApplicationIni",
                type: "",
                project: null,
                value: "${basePath}/roddyBaseConfigs/applicationProperties.ini",
                comment: "Path to the application.ini which is needed to execute Roddy"
        )
        assert processingOptionApplicationIni.save(flush: true)

        ProcessingOption featureTogglesConfigPath = new ProcessingOption(
                name: ExecuteRoddyCommandService.FEATURE_TOGGLES_CONFIG_PATH,
                type: null,
                project: null,
                value: "${basePath}/roddyBaseConfigs/featureToggles.ini",
                comment: "Path to featureToggles.ini which contains feature toggles for Roddy",
        )
        assert featureTogglesConfigPath.save(flush: true)
    }


    static ProcessedMergedBamFile createIncrementalMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        MergingSet mergingSet = createMergingSet(processedMergedBamFile.workPackage)
        MergingPass mergingPass = createMergingPass(mergingSet)

        ProcessedMergedBamFile secondBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: 1000,
        ])
        assert secondBamFile.save(flush: true)

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                mergingSet: mergingSet,
                bamFile: processedMergedBamFile
        )
        assert mergingSetAssignment.save(flush: true)
        return secondBamFile
    }

    static FastqSet createFastqSet(Map properties = [:]) {
        return createDomainObject(FastqSet, [
                seqTracks: { [createSeqTrack()] }
        ], properties)
    }

    static ExternallyProcessedMergedBamFile createExternallyProcessedMergedBamFile(Map properties = [:]) {
        return createDomainObject(ExternallyProcessedMergedBamFile, [
                fileName: 'runName_' + (counter++),
                source  : "SOURCE",
                fastqSet: { createFastqSet() },
                referenceGenome: { createReferenceGenome() },
                type: AbstractBamFile.BamType.MDUP
        ], properties)
    }

    static ProcessingThresholds createProcessingThresholds(Map properties = [:]) {
        return createDomainObject(ProcessingThresholds, [
                project: {createProject()},
                seqType: {createSeqType()},
                sampleType: {createSampleType()},
                coverage: 30.0,
                numberOfLanes: 3,
        ], properties)
    }

    static ProcessingThresholds createProcessingThresholdsForBamFile(AbstractBamFile bamFile, Map properties = [:]) {
        return createDomainObject(ProcessingThresholds, [
                project: bamFile.project,
                seqType: bamFile.seqType,
                sampleType: bamFile.sampleType,
                coverage: 30.0,
                numberOfLanes: 3
        ], properties)
    }

    static void changeSeqType(RoddyBamFile bamFile, SeqType seqType, String libraryName = null) {

        bamFile.mergingWorkPackage.seqType = seqType
        if (seqType.isWgbs()) {
            bamFile.mergingWorkPackage.libraryPreparationKit = null
        }
        assert bamFile.mergingWorkPackage.save(flush: true)

        bamFile.seqTracks.each {
            it.seqType = seqType
            it.libraryName = libraryName
            it.normalizedLibraryName = SeqTrack.normalizeLibraryName(it.libraryName)
            assert it.save(flush: true)
        }
    }

    static void createQaFileOnFileSystem(File qaFile, long chromosome8QcBasesMapped = 1866013) {
        qaFile.parentFile.mkdirs()
        // the values are from the documentation on the Wiki: https://wiki.local/NGS/OTP-Roddy+Interface#HTheQCData
        qaFile <<
                """
{
  "8": {
    "genomeWithoutNCoverageQcBases": 0.011,
    "referenceLength": 14636402211,
    "chromosome": 8,
    "qcBasesMapped": ${chromosome8QcBasesMapped}
  },
  "all": {
    "pairedRead1": 2091461,
    "pairedInSequencing": 4213091,
    "withMateMappedToDifferentChr": 336351,
    "qcFailedReads": 10,
    "totalReadCounter": 4213091,
    "totalMappedReadCounter": 4203691,
    "genomeWithoutNCoverageQcBases": 0.011,
    "singletons": 10801,
    "withMateMappedToDifferentChrMaq": 61611,
    "insertSizeMedian": 3991,
    "insertSizeSD": 931,
    "pairedRead2": 2121631,
    "percentageMatesOnDifferentChr": 1.551,
    "chromosome": "all",
    "withItselfAndMateMapped": 4192891,
    "qcBasesMapped": ${chromosome8QcBasesMapped},
    "duplicates": 8051,
    "insertSizeCV": 231,
    "referenceLength": 30956774121,
    "properlyPaired": 3847661
  },
  "7": {
    "referenceLength": 1591386631,
    "genomeWithoutNCoverageQcBases": 0.011,
    "qcBasesMapped": ${chromosome8QcBasesMapped},
    "chromosome": 7
  }
}
"""
    }

}
