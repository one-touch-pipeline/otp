package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.*
import de.dkfz.tbi.otp.job.jobs.snvcalling.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm.Cluster
import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.acl.*
import grails.util.*
import org.joda.time.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class DomainFactory {

    private DomainFactory() {
    }

    /**
     * @deprecated Use {@link HelperUtils#getRandomMd5sum()} instead.
     */
    @Deprecated
    static final String DEFAULT_MD5_SUM = HelperUtils.randomMd5sum
    static final String DEFAULT_TAB_FILE_NAME = 'DefaultTabFileName.tab'
    static final String DEFAULT_CHROMOSOME_LENGTH_FILE_NAME = 'DefaultChromosomeLengthFileName.tsv'
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
            domainObject = atMostOneElement(domainClass.list(limit: 1))
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
                jobScheduler      : Realm.JobScheduler.PBS,
                host              : 'test.host.invalid',
                port              : -1,
                unixUser          : '!fakeuser',
                roddyUser         : '!fakeroddyuser',
                timeout           : -1,
                defaultJobSubmissionOptions : '{}',
                cluster           : Cluster.DKFZ,
        ], realmProperties)
    }

    /**
     * creates a Pipeline, Name and Type have to be given e.g. Pipeline.Name.PANCAN_ALIGNMENT Pipeline.Type.ALIGNMENT
     * @param ENUM name of the Pipeline
     * @param ENUM type of the Pipeline
     * @return returns a created Pipeline due to given Params
     */
    static Pipeline createPipeline(Pipeline.Name name, Pipeline.Type type) {
        return createDomainObjectLazy(Pipeline, [:], [
                name: name,
                type: type,
        ])
    }

    static Pipeline createPanCanPipeline() {
        createPipeline(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createRoddyRnaPipeline() {
        createPipeline(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createRnaPipeline() {
        createPipeline(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createDefaultOtpPipeline() {
        createPipeline( Pipeline.Name.DEFAULT_OTP, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createOtpSnvPipelineLazy() {
        createPipeline( Pipeline.Name.OTP_SNV, Pipeline.Type.SNV)
    }

    static Pipeline createRoddySnvPipelineLazy() {
        createPipeline( Pipeline.Name.RODDY_SNV, Pipeline.Type.SNV)
    }

    static Pipeline createIndelPipelineLazy() {
        createPipeline( Pipeline.Name.RODDY_INDEL, Pipeline.Type.INDEL)
    }

    static Pipeline createAceseqPipelineLazy() {
        createPipeline( Pipeline.Name.RODDY_ACESEQ, Pipeline.Type.ACESEQ)
    }

    static Pipeline createSophiaPipelineLazy() {
        createPipeline( Pipeline.Name.RODDY_SOPHIA, Pipeline.Type.SOPHIA)
    }

    static Pipeline createExternallyProcessedPipelineLazy() {
        createPipeline( Pipeline.Name.EXTERNALLY_PROCESSED, Pipeline.Type.ALIGNMENT)
    }


    static Pipeline returnOrCreateAnyPipeline() {
        return (atMostOneElement(Pipeline.list(max: 1)) ?: createPanCanPipeline())
    }

    public static MergingSet createMergingSet(Map properties = [:]) {
        MergingWorkPackage mergingWorkPackage = properties.mergingWorkPackage ?: createMergingWorkPackage([:])
        return createDomainObject(MergingSet, [
                mergingWorkPackage: mergingWorkPackage,
                identifier: MergingSet.nextIdentifier(mergingWorkPackage),
        ], properties)
    }

    public static MergingSet createMergingSet(final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return createDomainObject(MergingSet, [
                mergingWorkPackage: mergingWorkPackage,
                identifier: MergingSet.nextIdentifier(mergingWorkPackage),
        ], properties)
    }

    public static MergingSetAssignment createMergingSetAssignment(Map properties = [:]) {
        return createDomainObject(MergingSetAssignment, [
                mergingSet: { createMergingSet() },
                bamFile   : { createProcessedBamFile() },
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
            referenceLength: 1,
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
                process: { createProcessingStepUpdate().process },
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

    public static RestartedProcessingStep createRestartedProcessingStep(Map properties = [:]) {
        ProcessingStep original = properties.original ?: createProcessingStep()
        return createDomainObject(RestartedProcessingStep, [
                jobDefinition: original.jobDefinition,
                jobClass: 'someClass',
                jobVersion: '0',
                process: original.process,
                original:original
        ], properties)
    }

    public static ProcessingStepUpdate createProcessingStepUpdate(Map properties = [:]) {
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

    public static void createProcessingOptionForOtrsTicketPrefix(String prefix = "Prefix ${counter++}"){
        ProcessingOptionService option = new ProcessingOptionService()
        option.createOrUpdate(
                TrackingService.TICKET_NUMBER_PREFIX,
                null,
                null,
                prefix,
                "comment to the number prefix"
        )
    }

    public static void createProcessingOptionForStatisticRecipient(String recipientEmail = "email${counter++}@example.example"){
        createProcessingOption(
                name: JobMailService.PROCESSING_OPTION_EMAIL_RECIPIENT,
                type: JobMailService.PROCESSING_OPTION_STATISTIC_EMAIL_RECIPIENT,
                project: null,
                value: recipientEmail,
        )
    }


    public static ProcessingOption createProcessingOptionBasePathReferenceGenome(String fileName = TestCase.uniqueNonExistentPath.path) {
        return createProcessingOption(
                name: ReferenceGenomeService.REFERENCE_GENOME_BASE_PATH,
                type: null,
                project: null,
                value: fileName,
        )
    }

    public static MergingPass createMergingPass(Map properties = [:]) {
        MergingSet mergingSet = properties.mergingSet ?: createMergingSet()
        return createDomainObject(MergingPass, [
                mergingSet: mergingSet,
                identifier: MergingPass.nextIdentifier(mergingSet),
        ], properties)
    }

    public static MergingPass createMergingPass(final MergingSet mergingSet) {
        return createDomainObject(MergingPass, [
                mergingSet: mergingSet,
                identifier: MergingPass.nextIdentifier(mergingSet),
        ], [:])
    }

    public static Map getRandomProcessedBamFileProperties() {
        return [
                fileSize           : ++counter,
                md5sum             : HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
        ]
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

    public static AlignmentPass createAlignmentPass(Map properties = [:]) {
        final SeqTrack seqTrack = properties.get('seqTrack') ?: createSeqTrack([:])
        final MergingWorkPackage workPackage = findOrSaveMergingWorkPackage(
                seqTrack,
                properties.get('referenceGenome'),
                properties.get('pipeline')
        )
        properties.remove("referenceGenome")
        properties.remove("description")
        final AlignmentPass alignmentPass = createDomainObject(AlignmentPass, [
                identifier: AlignmentPass.nextIdentifier(seqTrack),
                seqTrack: seqTrack,
                workPackage: workPackage,
                alignmentState: AlignmentPass.AlignmentState.FINISHED,
        ], properties)
        return alignmentPass
    }

    public static MergingWorkPackage findOrSaveMergingWorkPackage(SeqTrack seqTrack, ReferenceGenome referenceGenome = null, Pipeline pipeline = null) {
        if (referenceGenome == null || pipeline == null) {
            MergingWorkPackage workPackage = MergingWorkPackage.findWhere(
                    sample: seqTrack.sample,
                    seqType: seqTrack.seqType,
            )
            if (workPackage != null) {
                assert workPackage.seqPlatformGroup == seqTrack.seqPlatform.seqPlatformGroup
                assert workPackage.libraryPreparationKit == seqTrack.libraryPreparationKit
                return workPackage
            }
        }

        final MergingWorkPackage mergingWorkPackage = MergingWorkPackage.findOrSaveWhere(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatform.seqPlatformGroup,
                referenceGenome: referenceGenome ?: createReferenceGenomeLazy(),
                libraryPreparationKit: seqTrack.libraryPreparationKit,
                pipeline: pipeline ?: createDefaultOtpPipeline(),
        )
        return mergingWorkPackage
    }


    public static ReferenceGenome createReferenceGenomeLazy() {
        return ReferenceGenome.find{true} ?: createReferenceGenome()
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:], boolean saveAndValidate = true) {
        MergingSet mergingSet = createMergingSet(mergingWorkPackage)
        return createProcessedMergedBamFileWithoutProcessedBamFile(mergingSet, properties, saveAndValidate)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingWorkPackage, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(Map properties = [:], boolean saveAndValidate = true) {
        MergingPass mergingPass = properties.mergingPass ?: properties.workPackage ?
                createMergingPass(createMergingSet([mergingWorkPackage: properties.workPackage])) :
                createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createDomainObject(ProcessedMergedBamFile, [
                mergingPass        : mergingPass,
                workPackage        : mergingPass.mergingWorkPackage,
                type               : AbstractBamFile.BamType.MDUP,
                numberOfMergedLanes: 1,
        ], properties, saveAndValidate)
        return processedMergedBamFile
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingSet mergingSet, Map properties = [:], boolean saveAndValidate = true) {
        return createProcessedMergedBamFileWithoutProcessedBamFile(properties + [
                mergingPass: createMergingPass([
                        mergingSet: mergingSet,
                        identifier: MergingPass.nextIdentifier(mergingSet),
                ]),
        ], saveAndValidate)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingSet mergingSet, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingSet, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingPass mergingPass, Map properties = [:], boolean saveAndValidate = true) {
        return createProcessedMergedBamFileWithoutProcessedBamFile([
                mergingPass: mergingPass
        ] + properties, saveAndValidate)
    }

    public static ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public static ProcessedBamFile assignNewProcessedBamFile(final ProcessedMergedBamFile processedMergedBamFile) {
        final ProcessedBamFile bamFile = assignNewProcessedBamFile(processedMergedBamFile.mergingSet)
        processedMergedBamFile.numberOfMergedLanes++
        return bamFile
    }

    public static ProcessedBamFile assignNewProcessedBamFile(final MergingSet mergingSet) {
        final ProcessedBamFile bamFile = createProcessedBamFile(mergingSet.mergingWorkPackage)
        createMergingSetAssignment([
                mergingSet: mergingSet,
                bamFile: bamFile
        ])
        return bamFile
    }

    public static ProcessedBamFile createProcessedBamFile(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ProcessedBamFile, [
                alignmentPass     : { createAlignmentPass() },
                md5sum            : { HelperUtils.randomMd5sum },
                fileExists        : true,
                dateFromFileSystem: { new Date() },
                fileSize          : { counter++ },
                type              : AbstractBamFile.BamType.SORTED,
                hasIndexFile      : true,
                hasCoveragePlot   : true,
                hasInsertSizePlot : true,
                withdrawn         : false,
                coverage          : { counter++ },
                coverageWithN     : { counter++ },
                status            : AbstractBamFile.State.DECLARED,
        ], properties, saveAndValidate)
    }

    public static ProcessedBamFile createProcessedBamFile(final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        SeqTrack seqTrack = createSeqTrackWithDataFiles(mergingWorkPackage)

        final ProcessedBamFile bamFile = createProcessedBamFile([
                alignmentPass: createAlignmentPass([
                        seqTrack: seqTrack,
                        workPackage: mergingWorkPackage,
                        referenceGenome: mergingWorkPackage.referenceGenome,
                ]),
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status: AbstractBamFile.State.PROCESSED,
        ] + properties)

        return bamFile
    }

    public static <T> T createRoddyBamFile(Map bamFileProperties = [:], Class<T> clazz = RoddyBamFile) {
        MergingWorkPackage workPackage = bamFileProperties.workPackage
        if (!workPackage) {
            SeqType seqType = (clazz == RnaRoddyBamFile) ? createRnaSeqType() : createWholeGenomeSeqType()
            Pipeline pipeline = (clazz == RnaRoddyBamFile) ? createRoddyRnaPipeline() : createPanCanPipeline()
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
        Collection<SeqTrack> seqTracks = bamFileProperties.seqTracks ?: [createSeqTrackWithDataFiles(workPackage)]
        T bamFile = createDomainObject(clazz, [
                numberOfMergedLanes: seqTracks.size(),
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks: seqTracks as Set,
                workPackage: workPackage,
                identifier: RoddyBamFile.nextIdentifier(workPackage),
                config: { createRoddyWorkflowConfigLazy(
                        pipeline: workPackage.pipeline,
                        project: workPackage.project,
                        seqType: workPackage.seqType,
                        adapterTrimmingNeeded: workPackage.seqType.isRna() || workPackage.seqType.isWgbs(),
                )},
                md5sum: {
                    (!bamFileProperties.containsKey('fileOperationStatus') || bamFileProperties.fileOperationStatus == FileOperationStatus.PROCESSED) ? HelperUtils.randomMd5sum : null
                },
                fileOperationStatus: FileOperationStatus.PROCESSED,
                fileSize: 10000,
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ], bamFileProperties)
        return bamFile
    }

    public static RnaRoddyBamFile createRnaRoddyBamFile(Map bamFileProperties = [:]) {
        createRoddyBamFile(bamFileProperties, RnaRoddyBamFile)
    }

    public static createRoddyBamFile(RoddyBamFile baseBamFile, Map bamFileProperties = [:]) {
        RoddyBamFile bamFile = createDomainObject(RoddyBamFile, [
                baseBamFile: baseBamFile,
                config: baseBamFile.config,
                workPackage: baseBamFile.workPackage,
                identifier: baseBamFile.identifier + 1,
                numberOfMergedLanes: baseBamFile.numberOfMergedLanes + 1,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks: bamFileProperties.seqTracks ?: [createSeqTrackWithDataFiles(baseBamFile.workPackage)],
                md5sum: HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
                fileSize: 10000,
        ], bamFileProperties)
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
                ),
                referenceLength: 1,
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
        return createMergingWorkPackage(base, [
                sample:  sample,
                referenceGenome: base.referenceGenome,
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
     * Creates a {@link MergingWorkPackage} with the same properties except for the specified ones.
     */
    static MergingWorkPackage createMergingWorkPackage(MergingWorkPackage base, Map properties) {
        MergingWorkPackage mwp = new MergingWorkPackage((MergingWorkPackage.seqTrackPropertyNames +
                MergingWorkPackage.processingParameterNames).collectEntries{[it, base."${it}"]} + properties)
        assert mwp.save(failOnError: true)
        return mwp
    }

    static SamplePair createSamplePair(Map properties = [:]) {
        MergingWorkPackage mergingWorkPackage1 = properties.mergingWorkPackage1 ?:
                properties.mergingWorkPackage2 ? createMergingWorkPackage(properties.mergingWorkPackage2) :
                        createMergingWorkPackage()
        createSampleTypePerProjectLazy(
                sampleType: mergingWorkPackage1.sampleType,
                project: mergingWorkPackage1.project,
                category: SampleType.Category.DISEASE,
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


    static SamplePair createSamplePair(MergingWorkPackage mergingWorkPackage1, Map properties = [:]) {
        return createSamplePair(
                mergingWorkPackage1,
                createMergingWorkPackage(mergingWorkPackage1),
                properties)
    }

    private static final Map sampleTypeMap = [
            project   : { createProject() },
            sampleType: { createSampleType() },
            category  : SampleType.Category.DISEASE,
    ].asImmutable()

    static SampleTypePerProject createSampleTypePerProject(Map properties = [:]) {
        return createDomainObject(SampleTypePerProject, sampleTypeMap, properties)
    }

    static SampleTypePerProject createSampleTypePerProjectLazy(Map properties = [:]) {
        return createDomainObjectLazy(SampleTypePerProject, sampleTypeMap, properties)
    }

    static SampleTypePerProject createSampleTypePerProjectForMergingWorkPackage(MergingWorkPackage mergingWorkPackage, SampleType.Category category = SampleType.Category.DISEASE) {
        return createSampleTypePerProject([
                project   : mergingWorkPackage.project,
                sampleType: mergingWorkPackage.sampleType,
                category  : category,
        ])
    }

    static SampleTypePerProject createSampleTypePerProjectForBamFile(AbstractMergedBamFile bamFile, SampleType.Category category =  SampleType.Category.DISEASE) {
        return createSampleTypePerProjectForMergingWorkPackage(bamFile.mergingWorkPackage, category)
    }

    static SamplePair createSamplePair(MergingWorkPackage mergingWorkPackage1, MergingWorkPackage mergingWorkPackage2, Map properties = [:]) {
        SamplePair samplePair = SamplePair.createInstance([
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ] + properties)
        return samplePair.save(failOnError: true)
    }

    static def createProcessableSamplePair(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        def map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, [coverage: 30] + bamFile1Properties, [coverage: 30] + bamFile2Properties)

        SamplePair samplePair = map.samplePair
        AbstractMergedBamFile bamFile1 = map.sampleType1BamFile
        AbstractMergedBamFile bamFile2 = map.sampleType2BamFile
        bamFile1.mergingWorkPackage.bamFileInProjectFolder = bamFile1
        bamFile2.mergingWorkPackage.bamFileInProjectFolder = bamFile2

        ExternalScript script = createExternalScript(
                scriptIdentifier: "SnvCallingStep.CALLING",
        )

        SnvConfig snvConfig = createSnvConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                externalScriptVersion: script.scriptVersion
        )

        ExternalScript joinScript = createExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                scriptVersion: snvConfig.externalScriptVersion,
                filePath: "/tmp/scriptLocation_${counter++}/joining.sh",
        )


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
                snvConfig  : snvConfig,
                script     : script,
                joinScript : joinScript,
                roddyConfig: roddyConfig
        ]
    }

    static SamplePair createSamplePairWithProcessedMergedBamFiles() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: createWholeGenomeSeqType(),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
                referenceGenome: createReferenceGenome(name: 'hs37d5')
        )
        ProcessedMergedBamFile bamFileTumor = DomainFactory.createProcessedMergedBamFile(tumorMwp, getRandomProcessedBamFileProperties() + [coverage: 30.0])

        ProcessedMergedBamFile bamFileControl = DomainFactory.createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
                getRandomProcessedBamFileProperties() + [coverage: 30.0])

        bamFileTumor.mergingWorkPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        createSampleTypePerProject(
                project: bamFileTumor.project,
                sampleType: bamFileTumor.sampleType,
                category: SampleType.Category.DISEASE,
        )

        createSampleTypePerProject(
                project: bamFileControl.project,
                sampleType: bamFileControl.sampleType,
                category: SampleType.Category.CONTROL,
        )

        SamplePair samplePair = DomainFactory.createSamplePair(bamFileTumor.mergingWorkPackage, bamFileControl.mergingWorkPackage)

        createRoddyWorkflowConfig(
                seqType: samplePair.seqType,
                project: samplePair.project,
                pipeline: createSophiaPipelineLazy()
        )

        createProcessingThresholdsForBamFile(bamFileTumor, [numberOfLanes: null])
        createProcessingThresholdsForBamFile(bamFileControl, [numberOfLanes: null])

        createProcessingOption(
                name: SophiaService.PROCESSING_OPTION_REFERENCE_KEY,
                type: null,
                project: null,
                value: 'hs37d5, hs37d5_PhiX, hs37d5_GRCm38mm_PhiX, hs37d5+mouse, hs37d5_GRCm38mm',
                comment: "Name of reference genomes for sophia",
        )


        return samplePair

    }

    static SamplePair createDisease(MergingWorkPackage controlMwp) {
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(controlMwp)
        createSampleTypePerProject(project: controlMwp.project, sampleType: diseaseMwp.sampleType, category: SampleType.Category.DISEASE)
        SamplePair samplePair = createSamplePair(diseaseMwp, controlMwp)
        return samplePair
    }

    public static SnvConfig createSnvConfig(Map properties = [:]) {
        return createDomainObject(SnvConfig, [
                configuration: "configuration_${counter++}",
                externalScriptVersion: { createExternalScript().scriptVersion },
                seqType: { createSeqType() },
                project: { createProject() },
                pipeline: { createOtpSnvPipelineLazy() },
        ], properties)
    }


    private static Map createAnalysisInstanceWithRoddyBamFilesMapHelper(Map properties, Map bamFile1Properties, Map bamFile2Properties) {
        Pipeline pipeline = createPanCanPipeline()

        MergingWorkPackage controlWorkPackage = properties.samplePair?.mergingWorkPackage2 ?: createMergingWorkPackage(
                pipeline: pipeline,
                statSizeFileName: DEFAULT_TAB_FILE_NAME,
                seqType: createWholeGenomeSeqType(),
        )
        SamplePair samplePair = properties.samplePair ?: createDisease(controlWorkPackage)
        MergingWorkPackage diseaseWorkPackage = samplePair.mergingWorkPackage1

        RoddyBamFile disease = createRoddyBamFile([workPackage: diseaseWorkPackage] + bamFile1Properties)
        RoddyBamFile control = createRoddyBamFile([workPackage: controlWorkPackage, config: disease.config] + bamFile2Properties)

        return [
                instanceName: "instance-${counter++}",
                samplePair: samplePair,
                sampleType1BamFile: disease,
                sampleType2BamFile: control,
                latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(disease, control),
        ]
    }

    public static SnvCallingInstance createSnvInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map.config = properties.config ?: createSnvConfig(
                project: samplePair.project,
                seqType: samplePair.seqType,
        )
        return createDomainObject(SnvCallingInstance, map, properties)
    }

    public static RoddySnvCallingInstance createRoddySnvInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        Map map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, bamFile1Properties, bamFile2Properties)
        SamplePair samplePair = map.samplePair
        map += [
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                config                      : createRoddyWorkflowConfigLazy([
                        project: samplePair.project,
                        seqType: samplePair.seqType,
                        pipeline: createRoddySnvPipelineLazy()
                ]),
        ]
        return createDomainObject(RoddySnvCallingInstance, map, properties)
    }

    public static IndelCallingInstance createIndelCallingInstance(Map properties) {
        if (!properties.containsKey('latestDataFileCreationDate')) {
            properties += [latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(
                    properties.sampleType1BamFile, properties.sampleType2BamFile)]
        }
        return new IndelCallingInstance(properties)
    }

    public static IndelCallingInstance createIndelCallingInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
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

    public static AceseqInstance createAceseqInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
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

    public static SophiaInstance createSophiaInstanceWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
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

    public static SophiaInstance createSophiaInstance(SamplePair samplePair, Map properties = [:]) {
        return createDomainObject(SophiaInstance, [
                samplePair: samplePair,
                processingState: AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(samplePair.mergingWorkPackage1.bamFileInProjectFolder, samplePair.mergingWorkPackage2.bamFileInProjectFolder),
                instanceName: "instance-${counter++}",
                config: createRoddyWorkflowConfig([pipeline: createSophiaPipelineLazy()]),
        ], properties)
    }

    public static SophiaQc createSophiaQc(Map properties) {
        return createDomainObject(SophiaQc, [

                sophiaInstance: { createSophiaInstanceWithRoddyBamFiles() },
                controlMassiveInvPrefilteringLevel: 1,
                tumorMassiveInvFilteringLevel: 1,
                rnaContaminatedGenesMoreThanTwoIntron: "arbitraryGeneName",
                rnaContaminatedGenesCount: 1,
                rnaDecontaminationApplied: true,

        ], properties)
    }

    public static AceseqInstance createAceseqInstance(Map properties) {
        if (!properties.containsKey('latestDataFileCreationDate')) {
            properties += [latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(
                    properties.sampleType1BamFile, properties.sampleType2BamFile)]
        }
        return createDomainObject(AceseqInstance, [:], properties)
    }

    public static AceseqInstance createAceseqInstanceWithSameSamplePair(BamFilePairAnalysis instance) {
        return createAceseqInstance([
                processingState: AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                config: createRoddyWorkflowConfigLazy(pipeline: createAceseqPipelineLazy()),
                instanceName: "2017-03-17_10h44",
                samplePair: instance.samplePair,
        ])
    }

    public static AceseqQc createAceseqQcWithExistingAceseqInstance(AceseqInstance aceseqInstance){
        createAceseqQc([:], [:], [:], aceseqInstance)
    }

    public static AceseqQc createAceseqQc( Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:], AceseqInstance aceseqInstance=null) {
        aceseqInstance= aceseqInstance?:createAceseqInstanceWithRoddyBamFiles(properties, bamFile1Properties, bamFile2Properties)
        return createDomainObject(AceseqQc, [
                aceseqInstance: aceseqInstance,
                number: 1,
                tcc: 1,
                ploidyFactor: '1.0',
                ploidy:1,
                goodnessOfFit:1,
                gender:'M',
                solutionPossible: 1,
        ],properties)
    }

    public static SnvJobResult createSnvJobResultWithRoddyBamFiles(Map properties = [:]) {
        Map map = [
                step: SnvCallingStep.CALLING,
                processingState: AnalysisProcessingStates.FINISHED,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: DEFAULT_FILE_SIZE,
        ] + properties

        if (map.step == SnvCallingStep.CALLING) {
            if (!map.snvCallingInstance) {
                map.snvCallingInstance = createSnvInstanceWithRoddyBamFiles()
            }
            if (!map.chromosomeJoinExternalScript) {
                map.chromosomeJoinExternalScript = createExternalScript([
                        scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                        scriptVersion: map.snvCallingInstance.config.externalScriptVersion,
                ])
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
            map.externalScript = createExternalScript([
                    scriptIdentifier: map.step.externalScriptIdentifier,
                    scriptVersion: map.snvCallingInstance.config.externalScriptVersion,
            ])
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
                importMode: RunSegment.ImportMode.AUTOMATIC
        ], runSegmentProperties)
    }

    public static Project createProject(Map projectProperties = [:]) {
        return createDomainObject(Project, [
                name                    : 'project_' + (counter++),
                dirName                 : 'projectDirName_' + (counter++),
                realmName               : 'realmName_' + (counter++),
                alignmentDeciderBeanName: 'DUMMY_BEAN_NAME',
        ], projectProperties)
    }

    public static ProjectCategory createProjectCategory(Map projectProperties = [:]) {
        return createDomainObject(ProjectCategory, [
                name: 'projectCategory_' + (counter++),
        ], projectProperties)
    }

    public static ProjectContactPerson createProjectContactPerson(Map projectContactPersonProperties = [:]) {
        return createDomainObject(ProjectContactPerson, [
                project          : { createProject() },
                contactPersonRole: { createContactPersonRole() },
        ], projectContactPersonProperties)
    }

    public static ContactPersonRole createContactPersonRole(Map contactPersonRoleProperties = [:]) {
        return createDomainObject(ContactPersonRole, [
                name: 'roleName',
        ], contactPersonRoleProperties)
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
                name: 'sampleTypeName-' + (counter++),
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

    public static SeqType createSeqTypePaired(Map seqTypeProperties = [:], boolean saveAndValidate = true) {
        return createSeqType([libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED] + seqTypeProperties, saveAndValidate)
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

    static ReferenceGenome createReferenceGenome(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ReferenceGenome, [
                name                        : "name${counter++}",
                path                        : HelperUtils.uniqueString,
                fileNamePrefix              : "prefix_${counter++}",
                length                      : 1,
                lengthWithoutN              : 1,
                lengthRefChromosomes        : 1,
                lengthRefChromosomesWithoutN: 1,
                chromosomePrefix            : "",
                chromosomeSuffix            : "",

        ], properties, saveAndValidate)
    }

    static ReferenceGenome createAceseqReferenceGenome() {
        return createReferenceGenome(
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

    static ReferenceGenomeIndex createReferenceGenomeIndex(Map properties = [:]) {
        return createDomainObject(ReferenceGenomeIndex, [
                referenceGenome: { createReferenceGenome() },
                toolName: { createToolName() },
                path: "path_${counter++}",
                indexToolVersion: "indexToolVersion_${counter++}",
        ], properties)
    }

    static ToolName createToolName(Map properties = [:]) {
        return createDomainObject(ToolName, [
                name: "GENOME_STAR_INDEX_${counter++}",
                type: ToolName.Type.RNA,
                path: "path_${counter++}",
        ], properties)
    }

    static GeneModel createGeneModel(Map properties = [:]) {
        return createDomainObject(GeneModel, [
                referenceGenome: { createReferenceGenome() },
                path: "path_${counter++}",
                fileName: "fileName.gtf",
                excludeFileName: "excludeFileName.gtf",
                dexSeqFileName: "dexSeqFileName.gtf",
                gcFileName: "gcFileName.gtf",
        ], properties)
    }

    static ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Map properties = [:]) {
        return createDomainObject(ReferenceGenomeProjectSeqType, [
                project        : { createProject() },
                seqType        : { createSeqType() },
                referenceGenome: { createReferenceGenome() },
        ], properties)
    }

    static ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqTypeLazy(Map properties = [:]) {
        return createDomainObjectLazy(ReferenceGenomeProjectSeqType, [
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
                normalizedLibraryName: SeqTrack.normalizeLibraryName(properties.libraryName),
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
                libraryPreparationKit: { properties.seqType?.isWgbs() ? null : createLibraryPreparationKit() },
                sample:                { createSample() },
                seqType:               { createSeqType() },
                seqPlatformGroup:      { createSeqPlatformGroup() },
                referenceGenome:       { createReferenceGenome() },
                statSizeFileName:      { properties.pipeline?.name == Pipeline.Name.PANCAN_ALIGNMENT ?
                        "statSizeFileName_${counter++}.tab" : null },
                pipeline:              { createDefaultOtpPipeline() },
        ], properties)
    }

    public static ExternalMergingWorkPackage createExternalMergingWorkPackage(Map properties = [:]) {
        return createDomainObject(ExternalMergingWorkPackage, [
                sample:               { createSample() },
                seqType:              { createSeqType() },
                referenceGenome:      { createReferenceGenome() },
                pipeline:             { createExternallyProcessedPipelineLazy() },
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

    static private Map createRoddyWorkflowConfigMapHelper(Map properties = [:]) {
        Pipeline pipeline = properties.containsKey('pipeline') ? properties.pipeline : createPanCanPipeline()
        SeqType seqType =  properties.containsKey('seqType') ? properties.seqType : createWholeGenomeSeqType()
        String pluginVersion = properties.containsKey('pluginVersion') ? properties.pluginVersion : "pluginVersion:1.1.${counter++}"
        String configVersion = properties.containsKey('configVersion') ? properties.configVersion : "v1_${counter++}"
        return [
                pipeline: pipeline,
                seqType: seqType,
                configFilePath: {"${TestCase.uniqueNonExistentPath}/${pipeline.name.name()}_${seqType.roddyName}_${pluginVersion.substring(pluginVersion.indexOf(':') + 1)}_${configVersion}.xml"},
                pluginVersion: pluginVersion,
                configVersion: configVersion,
                project: { properties.individual?.project ?: createProject() },
                dateCreated: {new Date()},
                lastUpdated: {new Date()},
                adapterTrimmingNeeded: {seqType.isWgbs() || seqType.isRna()},
        ]
    }

    static RoddyWorkflowConfig createRoddyWorkflowConfig(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(RoddyWorkflowConfig, createRoddyWorkflowConfigMapHelper(properties), properties, saveAndValidate)
    }

    static RoddyWorkflowConfig createRoddyWorkflowConfigLazy(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObjectLazy(RoddyWorkflowConfig, createRoddyWorkflowConfigMapHelper(properties), properties, saveAndValidate)
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

    public static SeqTrack createSeqTrackWithTwoDataFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map dataFileProperties1 = [:], Map dataFileProperties2 = [:]) {
        return createSeqTrackWithTwoDataFiles(getMergingProperties(mergingWorkPackage) + seqTrackProperties, dataFileProperties1, dataFileProperties2)
    }

    public static SeqTrack createSeqTrackWithTwoDataFiles(Map seqTrackProperties = [:], Map dataFileProperties1 = [:], Map dataFileProperties2 = [:]) {
        FileType fileType = createFileType()
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
                dateCreated: new Date(),  // In unit tests Grails (sometimes) does not automatically set dateCreated.
                used       : true,
                sequenceLength: 100,
        ]
        if (properties.seqTrack) {
            defaultProperties.project = properties.seqTrack.project
            defaultProperties.run = properties.seqTrack.run
        }
        return createDataFile(defaultProperties + properties)
    }

    public static RoddySnvCallingInstance createRoddySnvCallingInstance(Map properties) {
        if (!properties.containsKey('latestDataFileCreationDate')) {
            properties += [latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(
                    properties.sampleType1BamFile, properties.sampleType2BamFile)]
        }
        return createDomainObject(RoddySnvCallingInstance, properties,[:])
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

    public static createIndelInstanceWithSameSamplePair(BamFilePairAnalysis instance) {
        IndelCallingInstance indelInstance =  createIndelCallingInstance([
                processingState: AnalysisProcessingStates.FINISHED,
                sampleType1BamFile: instance.sampleType1BamFile,
                sampleType2BamFile: instance.sampleType2BamFile,
                config: createRoddyWorkflowConfigLazy(pipeline: createIndelPipelineLazy()),
                instanceName: "2017-03-17_10h12",
                samplePair: instance.samplePair,
        ])
        return indelInstance.save(failOnError: true)
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

    public static ProcessingStep createProcessingStepWithUpdates(ProcessingStep processingStep = createProcessingStep()) {
        ProcessingStepUpdate last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.CREATED)
        last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.STARTED, previous: last)
        last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.FINISHED, previous: last)
        createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.SUCCESS, previous: last)
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

    static SeqType createSeqTypeLazy(SeqTypeNames seqTypeNames, String alias, String dirName, String roddyName = null, String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED) {
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

    static SeqType createRnaSeqType() {
        createSeqTypeLazy(SeqTypeNames.RNA, 'RNA', 'rna_sequencing', "RNA")
    }

    static List<SeqType> createDefaultOtpAlignableSeqTypes() {
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

    static List<SeqType> createRoddyAlignableSeqTypes() {
        [
                createPanCanAlignableSeqTypes(),
                createRnaSeqType(),
        ].flatten()
    }

    static List<SeqType> createAllAlignableSeqTypes() {
        [
                createDefaultOtpAlignableSeqTypes(),
                createRoddyAlignableSeqTypes(),
        ].flatten().unique()
    }

    static List<SeqType> createAllAnalysableSeqTypes() {
        [
                createSnvSeqTypes(),
                createIndelSeqTypes(),
                createSophiaSeqTypes(),
                createAceseqSeqTypes(),
        ].flatten().unique()
    }

    static List<SeqType> createSnvSeqTypes() {
        [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createIndelSeqTypes() {
        [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createSophiaSeqTypes() {
        [
                createWholeGenomeSeqType(),
        ]
    }

    static List<SeqType> createAceseqSeqTypes() {
        [
                createWholeGenomeSeqType(),
        ]
    }

    static MetaDataKey createMetaDataKeyLazy(Map properties = [:]) {
        return createDomainObjectLazy(MetaDataKey,[
                name: "name_${counter++}",
        ], properties)
    }

    static MetaDataEntry createMetaDataEntry(Map properties = [:]) {
        return createDomainObject(MetaDataEntry,[
                value: "value_${counter++}",
                dataFile: { createDataFile() },
                key: { createMetaDataKeyLazy() },
                status: MetaDataEntry.Status.VALID,
                source: MetaDataEntry.Source.MDFILE,
        ], properties)
    }


    static MetaDataEntry createMetaDataKeyAndEntry(DataFile dataFile, String key, String value) {
        MetaDataKey metaDataKey = createMetaDataKeyLazy(name: key)

        return createMetaDataEntry(
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

    static TumorEntity createTumorEntity(Map properties = [:]) {
        return createDomainObject(TumorEntity, [
                name: "AML/ALL",
        ], properties)
    }

    static Map<String, String> createOtpAlignmentProcessingOptions(Map properties = [:]) {
        [
                createProcessingOption(
                        name: ProjectOverviewService.BWA_COMMAND,
                        type: null,
                        project: null,
                        value: properties[ProjectOverviewService.BWA_COMMAND] ?: "value ${counter++}",
                        comment: "Some comment ${counter++}",
                ),
                createProcessingOption(
                        name: ProjectOverviewService.BWA_Q_PARAMETER,
                        type: null,
                        project: null,
                        value: properties[ProjectOverviewService.BWA_Q_PARAMETER] ?: "value ${counter++}",
                        comment: "Some comment ${counter++}",
                ),
                createProcessingOption(
                        name: ProjectOverviewService.SAM_TOOLS_COMMAND,
                        type: null,
                        project: null,
                        value: properties[ProjectOverviewService.SAM_TOOLS_COMMAND] ?: "value ${counter++}",
                        comment: "Some comment ${counter++}",
                ),
                createProcessingOption(
                        name: ProjectOverviewService.PICARD_MDUP_COMMAND,
                        type: null,
                        project: null,
                        value: properties[ProjectOverviewService.PICARD_MDUP_COMMAND] ?: "value ${counter++}",
                        comment: "Some comment ${counter++}",
                ),
                createProcessingOption(
                        name: ProjectOverviewService.PICARD_MDUP_OPTIONS,
                        type: null,
                        project: null,
                        value: [ProjectOverviewService.PICARD_MDUP_OPTIONS] ?: "value ${counter++}",
                        comment: "Some comment ${counter++}",
                )
        ].collectEntries { ProcessingOption processingOption ->
            [(processingOption.name): processingOption.value]
        }
    }

    static void createRoddyProcessingOptions(File basePath) {

        ProcessingOption processingOptionPath = new ProcessingOption(
                name: "roddyPath",
                type: null,
                project: null,
                value: "${basePath}/roddy/",
                comment: "Path to the roddy.sh on the current cluster (***REMOVED***cluster 13.1)",
        )
        assert processingOptionPath.save(flush: true)

        ProcessingOption processingOptionBaseConfigsPath = new ProcessingOption(
                name: "roddyBaseConfigsPath",
                type: null,
                project: null,
                value: "${basePath}/roddyBaseConfigs/",
                comment: "Path to the baseConfig-files which are needed to execute Roddy",
        )
        assert processingOptionBaseConfigsPath.save(flush: true)

        ProcessingOption processingOptionApplicationIni = new ProcessingOption(
                name: "roddyApplicationIni",
                type: null,
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

    static IndelQualityControl createIndelQualityControl(Map properties = [:]) {
        return createDomainObject(IndelQualityControl, [
                indelCallingInstance: { createIndelCallingInstance() },
                file: "file",
                numIndels: counter++,
                numIns: counter++,
                numDels: counter++,
                numSize1_3: counter++,
                numSize4_10: counter++,
                numSize11plus: counter++,
                numInsSize1_3: counter++,
                numInsSize4_10: counter++,
                numInsSize11plus: counter++,
                numDelsSize1_3: counter++,
                numDelsSize4_10: counter++,
                numDelsSize11plus: counter++,
                percentIns: counter++ as double,
                percentDels: counter++ as double,
                percentSize1_3: counter++ as double,
                percentSize4_10: counter++ as double,
                percentSize11plus: counter++ as double,
                percentInsSize1_3: counter++ as double,
                percentInsSize4_10: counter++ as double,
                percentInsSize11plus: counter++ as double,
                percentDelsSize1_3: counter++ as double,
                percentDelsSize4_10: counter++ as double,
                percentDelsSize11plus: counter++ as double,
        ], properties)
    }

    static void createNotificationProcessingOptions() {

        createProcessingOption([
                name   : CreateNotificationTextService.BASE_NOTIFICATION_TEMPLATE,
                type   : null,
                project: null,
                value  : '''
base notification
stepInformation: ${stepInformation}
seqCenterComment: ${seqCenterComment}
''',
                comment: '',
        ])

        createProcessingOption([
                name   : CreateNotificationTextService.INSTALLATION_NOTIFICATION_TEMPLATE,
                type   : null,
                project: null,
                value  : '''
data installation finished
runs: ${runs}
paths: ${paths}
samples: ${samples}
links: ${links}
''',
                comment: '',
        ])

        createProcessingOption([
                name   : CreateNotificationTextService.INSTALLATION_FURTHER_PROCESSING_TEMPLATE,
                type   : null,
                project: null,
                value  : '''further processing''',
                comment: '',
        ])

        createProcessingOption([
                name   : CreateNotificationTextService.ALIGNMENT_NOTIFICATION_TEMPLATE,
                type   : null,
                project: null,
                value  : '''
alignment finished
samples: ${samples}
links: ${links}
processingValues: ${processingValues}
paths: ${paths}
''',
                comment: '',
        ])


        createProcessingOption([
                name: CreateNotificationTextService.ALIGNMENT_FURTHER_PROCESSING_TEMPLATE,
                type: null,
                project: null,
                value: '''
run variant calling
variantCallingPipelines: ${variantCallingPipelines}
samplePairsWillProcess: ${samplePairsWillProcess}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.ALIGNMENT_NO_FURTHER_PROCESSING_TEMPLATE,
                type: null,
                project: null,
                value: '''
no variant calling
samplePairsWontProcess: ${samplePairsWontProcess}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.ALIGNMENT_PROCESSING_INFORMATION_TEMPLATE,
                type: null,
                project: null,
                value: '''
alignment information
seqType: ${seqType}
referenceGenome: ${referenceGenome}
alignmentProgram: ${alignmentProgram}
alignmentParameter: ${alignmentParameter}
mergingProgram: ${mergingProgram}
mergingParameter: ${mergingParameter}
samtoolsProgram: ${samtoolsProgram}
''',
                comment: '',
        ])


        createProcessingOption([
                name: CreateNotificationTextService.SNV_NOTIFICATION_TEMPLATE,
                type: null,
                project: null,
                value: '''
snv finished
samplePairsFinished: ${samplePairsFinished}
otpLinks: ${otpLinks}
directories: ${directories}
''',
                comment: '',
        ])


        createProcessingOption([
                name: CreateNotificationTextService.SNV_NOT_PROCESSED_TEMPLATE,
                type: null,
                project: null,
                value: '''
snv not processed
samplePairsNotProcessed: ${samplePairsNotProcessed}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.INDEL_NOTIFICATION_TEMPLATE,
                type: null,
                project: null,
                value: '''
indel finished
samplePairsFinished: ${samplePairsFinished}
otpLinks: ${otpLinks}
directories: ${directories}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.INDEL_NOT_PROCESSED_TEMPLATE,
                type: null,
                project: null,
                value: '''
indel not processed
samplePairsNotProcessed: ${samplePairsNotProcessed}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.ACESEQ_NOTIFICATION_TEMPLATE,
                type: null,
                project: null,
                value: '''
aceseq finished
samplePairsFinished: ${samplePairsFinished}
otpLinks: ${otpLinks}
directories: ${directories}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.ACESEQ_NOT_PROCESSED_TEMPLATE,
                type: null,
                project: null,
                value: '''
aceseq not processed
samplePairsNotProcessed: ${samplePairsNotProcessed}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.SOPHIA_NOTIFICATION_TEMPLATE,
                type: null,
                project: null,
                value: '''
sophia finished
samplePairsFinished: ${samplePairsFinished}
otpLinks: ${otpLinks}
directories: ${directories}
''',
                comment: '',
        ])

        createProcessingOption([
                name: CreateNotificationTextService.SOPHIA_NOT_PROCESSED_TEMPLATE,
                type: null,
                project: null,
                value: '''
sophia not processed
samplePairsNotProcessed: ${samplePairsNotProcessed}
''',
                comment: '',
        ])
    }



    static ProcessedMergedBamFile createIncrementalMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        MergingSet mergingSet = createMergingSet(processedMergedBamFile.workPackage)
        MergingPass mergingPass = createMergingPass(mergingSet)

        ProcessedMergedBamFile secondBamFile = createProcessedMergedBamFile(mergingPass, [
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

    static ExternallyProcessedMergedBamFile createExternallyProcessedMergedBamFile(Map properties = [:]) {
        return createDomainObject(ExternallyProcessedMergedBamFile, [
                fileName: "runName_${counter++}",
                workPackage: { createExternalMergingWorkPackage() },
                numberOfMergedLanes: null,
                importedFrom: "/importFrom_${counter++}"
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

    static ProcessingThresholds createProcessingThresholdsForMergingWorkPackage(MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return createProcessingThresholds([
                project: mergingWorkPackage.project,
                seqType: mergingWorkPackage.seqType,
                sampleType: mergingWorkPackage.sampleType,
        ] + properties)
    }

    static ProcessingThresholds createProcessingThresholdsForBamFile(AbstractBamFile bamFile, Map properties = [:]) {
        createProcessingThresholdsForMergingWorkPackage(bamFile.mergingWorkPackage, properties)
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

    static void createAceseqQaFileOnFileSystem(File qaFile) {
        qaFile.parentFile.mkdirs()
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
""" }


    static void createSophiaQcFileOnFileSystem(File qcFile) {
        qcFile.parentFile.mkdirs()
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


    public static void createAclObjects(Object domainObject, Map properties = [:]) {
        AclObjectIdentity aclObjectIdentity = createDomainObject(AclObjectIdentity, [objectId: domainObject.id, aclClass: {createDomainObject(AclClass, [className: domainObject.class.name], [:])}], [:])
        createDomainObject(AclEntry, [aclObjectIdentity: aclObjectIdentity, sid: {createDomainObject(AclSid, [sid: "ROLE_ADMIN"], properties)}], [:])
    }


    public static MergingCriteria createMergingCriteria(Map properties = [:]) {
        return createDomainObject(MergingCriteria, [
                project: { createProject() },
                seqType: { createSeqType() },
        ], properties )
    }
}
