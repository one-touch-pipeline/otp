package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.util.Environment
import org.joda.time.DateTime
import org.joda.time.Duration

class DomainFactory {

    private DomainFactory() {
    }

    static final String DEFAULT_MD5_SUM = '123456789abcdef123456789abcdef00'
    static final String DEFAULT_TAB_FILE_NAME = 'DefaultTabFileName.tab'
    static final String DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY = 'exec_123456_123456789_test_test'
    static final long DEFAULT_FILE_SIZE = 123456
    static final String TEST_CONFIG_VERSION = 'v1_0'
    static final Map PROCESSED_BAM_FILE_PROPERTIES = [
            fileSize: 123456789,
            md5sum: DEFAULT_MD5_SUM,
            fileOperationStatus: FileOperationStatus.PROCESSED,
    ].asImmutable()

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
                webHost           : 'test.host.invalid',
                host              : 'test.host.invalid',
                port              : -1,
                unixUser          : '!fakeuser',
                timeout           : -1,
                pbsOptions        : '',
                cluster           : Cluster.DKFZ,
        ], realmProperties)
    }

    static Workflow createPanCanWorkflow() {
        return Workflow.buildLazy(name: Workflow.Name.PANCAN_ALIGNMENT, type: Workflow.Type.ALIGNMENT)
    }

    static Workflow createDefaultOtpWorkflow() {
        return Workflow.buildLazy(name: Workflow.Name.DEFAULT_OTP, type: Workflow.Type.ALIGNMENT)
    }

    static Workflow returnOrCreateAnyWorkflow() {
        return (CollectionUtils.atMostOneElement(Workflow.list(max: 1)) ?: createPanCanWorkflow())
    }

    public static MergingSet createMergingSet(final MergingWorkPackage mergingWorkPackage) {
        return MergingSet.build(
                mergingWorkPackage: mergingWorkPackage,
                identifier: MergingSet.nextIdentifier(mergingWorkPackage),
        )
    }

    public static MergingPass createMergingPass(final MergingSet mergingSet) {
        return new MergingPass(
                mergingSet: mergingSet,
                identifier: MergingPass.nextIdentifier(mergingSet),
        ).save(flush: true)
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

        SeqTrack seqTrack = buildSeqTrackWithDataFile(mergingWorkPackage)

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
            SeqType seqType = SeqType.buildLazy(name: SeqTypeNames.WHOLE_GENOME.seqTypeName, alias: "WGS", libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
            Workflow workflow = createPanCanWorkflow()
            workPackage = MergingWorkPackage.build(
                    workflow: workflow,
                    seqType: seqType,
                    libraryPreparationKit: LibraryPreparationKit.buildLazy(name: 'libraryPreparationKit'),
                    statSizeFileName: DEFAULT_TAB_FILE_NAME,
            )
            ReferenceGenomeProjectSeqType.build(
                    referenceGenome: workPackage.referenceGenome,
                    project: workPackage.project,
                    seqType: workPackage.seqType,
                    statSizeFileName: DEFAULT_TAB_FILE_NAME,
            )
        }
        SeqTrack seqTrack = DomainFactory.buildSeqTrackWithDataFile(workPackage)
        RoddyBamFile bamFile = RoddyBamFile.build([
                numberOfMergedLanes: 1,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks: [seqTrack],
                workPackage: workPackage,
                identifier: RoddyBamFile.nextIdentifier(workPackage),
                config: DomainFactory.createRoddyWorkflowConfig(
                        workflow: workPackage.workflow,
                        project: workPackage.project,
                ),
                md5sum: DEFAULT_MD5_SUM,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                fileSize: 10000,
                roddyVersion: ProcessingOption.build(),
                ] + bamFileProperties)
        assert bamFile.save(flush: true) // build-test-data does not flush, only saves
        return bamFile
    }

    public static createRoddyBamFile(RoddyBamFile baseBamFile, Map bamFileProperties = [:]) {
        RoddyBamFile bamFile = RoddyBamFile.build([
                baseBamFile: baseBamFile,
                config: bamFileProperties.config ?: DomainFactory.createRoddyWorkflowConfig(workflow: baseBamFile.config.workflow),
                workPackage: baseBamFile.workPackage,
                identifier: baseBamFile.identifier + 1,
                numberOfMergedLanes: baseBamFile.numberOfMergedLanes + 1,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks: bamFileProperties.seqTracks ?: [DomainFactory.buildSeqTrackWithDataFile(baseBamFile.workPackage)],
                md5sum: DEFAULT_MD5_SUM,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                fileSize: 10000,
                ] + bamFileProperties
        )
        bamFile.save(flush: true)
        return bamFile
    }

    /**
     * Creates a {@link MergingWorkPackage} with the same properties as the specified one but a different
     * {@link SampleType}.
     */
    public static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base) {
        return createMergingWorkPackage(base, SampleType.build())
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
        MergingWorkPackage mergingWorkPackage1 = TestData.createMergingWorkPackage(
                sample: createSample(),
                seqType: createSeqType(),
                seqPlatformGroup: SeqPlatformGroup.build()
        )
        assert mergingWorkPackage1.save(flush: true, failOnError: true)
        SampleTypePerProject sampleTypePerProject = SampleTypePerProject.build(
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

    static SamplePair createSamplePair(MergingWorkPackage mergingWorkPackage1, MergingWorkPackage mergingWorkPackage2, Map properties = [:]) {
        SamplePair samplePair = SamplePair.createInstance([
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ] + properties)
        return samplePair.save(failOnError: true)
    }

    static SamplePair createDisease(MergingWorkPackage controlMwp) {
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(controlMwp)
        SampleTypePerProject.build(project: controlMwp.project, sampleType: diseaseMwp.sampleType, category: SampleType.Category.DISEASE)
        SamplePair samplePair = createSamplePair(diseaseMwp, controlMwp)
        return samplePair
    }

    public static SnvCallingInstance createSnvInstanceWithRoddyBamFiles(Map properties = [:]) {
        Workflow workflow = createPanCanWorkflow()

        MergingWorkPackage controlWorkPackage = MergingWorkPackage.build(
                workflow: workflow,
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
        )
        SamplePair samplePair = createDisease(controlWorkPackage)
        MergingWorkPackage diseaseWorkPackage = samplePair.mergingWorkPackage1

        RoddyBamFile disease = createRoddyBamFile(workPackage: diseaseWorkPackage)
        RoddyBamFile control = createRoddyBamFile(workPackage: controlWorkPackage)

        ExternalScript externalScript = ExternalScript.buildLazy()

        SnvConfig snvConfig = SnvConfig.buildLazy(
                seqType: samplePair.seqType,
                externalScriptVersion: externalScript.scriptVersion
        )

        SnvCallingInstance snvCallingInstance = SnvCallingInstance.build( [
                samplePair: samplePair,
                sampleType1BamFile: disease,
                sampleType2BamFile: control,
                config: snvConfig,
                latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(disease, control),
        ] + properties)
        return snvCallingInstance
    }

    public static SnvJobResult createSnvJobResultWithRoddyBamFiles(Map properties = [:]) {
        Map map = [
                step: SnvCallingStep.CALLING,
                processingState: SnvProcessingStates.FINISHED,
                md5sum: DEFAULT_MD5_SUM,
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

        SnvJobResult snvJobResult = SnvJobResult.build(map)

        return snvJobResult
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
        ], seqPlatformProperties)
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
                currentFormat: RunSegment.DataFormat.FILES_IN_DIRECTORY,
                initialFormat: RunSegment.DataFormat.FILES_IN_DIRECTORY,
                filesStatus  : RunSegment.FilesStatus.FILES_CORRECT,
                run          : { createRun() },
                dataPath     : { TestCase.getUniqueNonExistentPath().path },
                mdPath       : { TestCase.getUniqueNonExistentPath().path },
        ], runSegmentProperties)
    }

    public static Project createProject(Map projectProperties = [:]) {
        return createDomainObject(Project, [
                name                    : 'project_' + (counter++),
                dirName                 : 'projectDirName_' + (counter++),
                realmName               : 'realmName_' + (counter++),
                alignmentDeciderBeanName: 'DUMMY_BEAN_NAME'
        ], projectProperties)
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
        ], sampleTypeProperties)
    }

    public static Sample createSample(Map sampleProperties = [:]) {
        return createDomainObject(Sample, [
                individual: { createIndividual() },
                sampleType: { createSampleType() },
        ], sampleProperties)
    }

    public static SeqType createSeqType(Map seqTypeProperties = [:]) {
        return createDomainObject(SeqType, [
                name         : 'seqTypeName_' + (counter++),
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                dirName      : 'seqTypeDirName_' + (counter++),
        ], seqTypeProperties)
    }

    public static SoftwareTool createSoftwareTool(Map softwareToolProperties = [:]) {
        return createDomainObject(SoftwareTool, [
                programName: 'softwareToolProgramName_' + (counter++),
                type       : SoftwareTool.Type.ALIGNMENT,
        ], softwareToolProperties)
    }

    public static SeqTrack createSeqTrack(Map seqTrackProperties = [:]) {
        return createDomainObject(SeqTrack, [
                laneId         : 'laneId_' + counter++,
                seqType        : { createSeqType() },
                sample         : { createSample() },
                pipelineVersion: { createSoftwareTool() },
                run            : { createRun() },
                seqPlatform    : { createSeqPlatform() },
        ], seqTrackProperties)
    }


    static RoddyWorkflowConfig createRoddyWorkflowConfig(Map properties = [:], boolean saveAndValidate = true) {
        Workflow workflow = properties.containsKey('workflow') ? properties.workflow : createPanCanWorkflow()
        String pluginVersion = properties.containsKey('pluginVersion') ? properties.pluginVersion : "pluginVersion:1.1.${counter++}"
        String configVersion = properties.containsKey('configVersion') ? properties.configVersion : "v1_${counter++}"

        return createDomainObject(RoddyWorkflowConfig, [
                workflow: workflow,
                configFilePath: {"${TestCase.uniqueNonExistentPath}/${workflow.name.name()}_${pluginVersion.substring(pluginVersion.indexOf(':') + 1)}_${configVersion}.xml"},
                pluginVersion: pluginVersion,
                configVersion: configVersion,
                project: {createProject()},
                dateCreated: {new Date()},
                lastUpdated: {new Date()},
        ], properties, saveAndValidate)
    }

    public static SeqTrack buildSeqTrackWithDataFile(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:]) {
        Map map = [
                sample: mergingWorkPackage.sample,
                seqType: mergingWorkPackage.seqType,
                kitInfoReliability:  mergingWorkPackage.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
                libraryPreparationKit: mergingWorkPackage.libraryPreparationKit,
                seqPlatform: SeqPlatform.build(seqPlatformGroup: mergingWorkPackage.seqPlatformGroup),
        ] + seqTrackProperties
        SeqTrack seqTrack
        if (mergingWorkPackage.seqType.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED) {
            seqTrack = buildSeqTrackWithTwoDataFiles(map)
        } else {
            seqTrack = buildSeqTrackWithDataFile(map)
        }
        assert mergingWorkPackage.satisfiesCriteria(seqTrack)
        return seqTrack
    }

    public static SeqTrack buildSeqTrackWithDataFile(Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        SeqTrack seqTrack
        if (seqTrackProperties.seqType?.name == SeqTypeNames.EXOME.seqTypeName) {
            seqTrack = ExomeSeqTrack.build(seqTrackProperties)
        } else {
            seqTrack = SeqTrack.build(seqTrackProperties)
        }
        buildSequenceDataFile(dataFileProperties + [seqTrack: seqTrack])
        return seqTrack
    }

    public static SeqTrack buildSeqTrackWithTwoDataFiles(Map seqTrackProperties = [:], Map dataFileProperties1 = [:], Map dataFileProperties2 = [:]) {
        Map defaultMap1 = [
                fileName: 'DataFileFileName_R1.gz',
                vbpFileName: 'DataFileFileName_R1.gz',
                readNumber: 1,
        ]
        Map defaultMap2 = [
                fileName: 'DataFileFileName_R2.gz',
                vbpFileName: 'DataFileFileName_R2.gz',
                readNumber: 2,
        ]
        SeqTrack seqTrack = buildSeqTrackWithDataFile(seqTrackProperties, defaultMap1 + dataFileProperties1)
        buildSequenceDataFile(defaultMap2 + dataFileProperties2 + [seqTrack: seqTrack])
        return seqTrack
    }

    public static DataFile buildSequenceDataFile(final Map properties = [:]) {
        DataFile dataFile = DataFile.build([
                fileType: FileType.buildLazy(type: Type.SEQUENCE),
                dateCreated: new Date(),  // In unit tests Grails (sometimes) does not automagically set dateCreated.
                used: true,
        ] + properties)
        if (!dataFile.run) {
            dataFile.run = dataFile.seqTrack?.run
            dataFile.save(flush: true, failOnError: true)
        }
        if (!dataFile.project) {
            dataFile.project = dataFile.seqTrack?.project
            dataFile.save(flush: true, failOnError: true)
        }
        return dataFile
    }

    public static SnvCallingInstance createSnvCallingInstance(Map properties) {
        if (!properties.containsKey('latestDataFileCreationDate')) {
            properties += [latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(
                    properties.sampleType1BamFile, properties.sampleType2BamFile)]
        }
        return new SnvCallingInstance(properties)
    }

    public static ProcessingStep createAndSaveProcessingStep(String jobClass = "de.dkfz.tbi.otp.test.job.jobs.NonExistentDummyJob") {
        final JobExecutionPlan jep = new JobExecutionPlan(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0, startJobBean: "DontCare")
        assert jep.save()
        final JobDefinition jobDefinition = new JobDefinition(name: "DontCare", bean: "DontCare", plan: jep)
        assert jobDefinition.save()
        final Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "DontCare", startJobVersion: "1")
        assert process.save()
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

    public static ProcessParameter createProcessParameter(final Process process, final Object parameterValue) {
        return new ProcessParameter(
                process: process,
                className: parameterValue.class.name,
                value: parameterValue.id.toString(),
        )
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
        ] + myProps)
    }

    static SeqType createWholeGenomeSeqType() {
        SeqType.buildLazy(
                name: SeqTypeNames.WHOLE_GENOME.seqTypeName,
                alias: "WGS",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED
        )
    }

    static SeqType createExomeSeqType() {
        SeqType.buildLazy(
                name: SeqTypeNames.EXOME.seqTypeName,
                alias:  SeqTypeNames.EXOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED
        )
    }

    static List<SeqType> createAlignableSeqTypes() {
        [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
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

    static void createRoddyProcessingOptions(File basePath) {

        ProcessingOption processingOptionPath = new ProcessingOption(
            name: "roddyPath",
            type: "",
            project: null,
            value: "${basePath}/tbi_cluster/11.4/x86_64/otp/Roddy/",
            comment: "Path to the roddy.sh on the current cluster (***REMOVED***cluster 11.4)",
        )
        assert processingOptionPath.save(flush: true)

        assert new ProcessingOption(
                name: ExecuteRoddyCommandService.CORRECT_PERMISSION_SCRIPT_NAME,
                type: "",
                project: null,
                value: "${basePath}/correctPathPermissionsOtherUnixUserRemoteWrapper.sh",
                comment: "some comment",
        ).save(flush: true)


        assert new ProcessingOption(
                name: ExecuteRoddyCommandService.CORRECT_GROUP_SCRIPT_NAME,
                type: "",
                project: null,
                value: "${basePath}/correctGroupOtherUnixUserRemoteWrapper.sh",
                comment: "some comment",
        ).save(flush: true)

        assert new ProcessingOption(
                name: ExecuteRoddyCommandService.DELETE_CONTENT_OF_OTHERUNIXUSER_DIRECTORIES_SCRIPT,
                type: "",
                project: null,
                value:  "${basePath}/deleteContentOfRoddyDirectoriesRemoteWrapper.sh",
                comment: "some comment",
        ).save(flush: true)

        ProcessingOption processingOptionVersion = new ProcessingOption(
            name: "roddyVersion",
            type: "",
            project: null,
            value: "2.1.28",
            comment: "Roddy version which is used currently to process Roddy-Pipelines"
        )
        assert processingOptionVersion.save(flush: true)

        ProcessingOption processingOptionBaseConfigsPath = new ProcessingOption(
                name: "roddyBaseConfigsPath",
                type: "",
                project: null,
                value: "${basePath}/tbi_cluster/11.4/x86_64/otp/RoddyBaseConfigs/",
                comment: "Path to the baseConfig-files which are needed to execute Roddy",
        )
        assert processingOptionBaseConfigsPath.save(flush: true)

        ProcessingOption processingOptionApplicationIni = new ProcessingOption(
                name: "roddyApplicationIni",
                type: "",
                project: null,
                value: "${basePath}/tbi_cluster/11.4/x86_64/otp/RoddyBaseConfigs/applicationProperties.ini",
                comment: "Path to the application.ini which is needed to execute Roddy"
        )
        assert processingOptionApplicationIni.save(flush: true)
    }


    static ProcessedMergedBamFile createIncrementalMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        MergingSet mergingSet = createMergingSet(processedMergedBamFile.workPackage)
        MergingPass mergingPass = createMergingPass(mergingSet)

        ProcessedMergedBamFile secondBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum: TestConstants.TEST_MD5SUM,
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

}
