package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.acl.*
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
            assert domain.save(flush: true, failOnError: true)
        }
        return domain
    }

    /**
     * Check whether one or more domain objects with the given parameterProperties exists,
     * if yes return an arbitrary one, otherwise create a new one and return it.
     */
    private
    static <T> T createDomainObjectLazy(Class<T> domainClass, Map defaultProperties, Map parameterProperties, boolean saveAndValidate = true) {
        T domainObject
        if (parameterProperties) {
            domainObject = domainClass.findWhere(parameterProperties)
        } else {
            domainObject = atMostOneElement(domainClass.list(limit: 1))
        }
        if (!domainObject) {
            domainObject = createDomainObject(domainClass, defaultProperties, parameterProperties, saveAndValidate)
        }
        return domainObject
    }

    static <T> T createDomainWithImportAlias(Class<T> domainClass, Map parameterProperties) {
        Map defaultProperties = [
                name       : 'metadataName' + (counter++),
                importAlias: [],
        ]
        return createDomainObject(domainClass, defaultProperties, parameterProperties)
    }

    static Realm createRealm(Map realmProperties = [:]) {
        return createDomainObject(Realm, [
                name                       : 'realmName_' + (counter++),
                jobScheduler               : Realm.JobScheduler.PBS,
                host                       : 'test.host.invalid',
                port                       : -1,
                timeout                    : -1,
                defaultJobSubmissionOptions: '',
        ], realmProperties)
    }

    static Realm createDefaultRealmWithProcessingOption(Map realmProperties = [:]) {
        Realm realm = createRealm(realmProperties)
        createProcessingOptionLazy([
                name   : OptionName.REALM_DEFAULT_VALUE,
                type   : null,
                project: null,
                value  : realm.name,
        ])
        return realm
    }

    static Role createRoleLazy(Map properties = [:]) {
        return createDomainObjectLazy(Role, [
                authority: "ROLE_${counter++}",
        ], properties)
    }

    static Role createRoleOperatorLazy() {
        return createRoleLazy([
                authority: Role.ROLE_OPERATOR,
        ])
    }

    static Role createRoleAdminLazy() {
        return createRoleLazy([
                authority: Role.ROLE_ADMIN,
        ])
    }

    static Role createRoleSwitchUserLazy() {
        return createRoleLazy([
                authority: Role.ROLE_SWITCH_USER,
        ])
    }

    static User createUser(Map properties = [:]) {
        return createDomainObject(User, [
                username: "user_${counter++}",
                password: "password_${counter++}",
                email   : "user${counter++}@dummy.de",
                realName: "realName_${counter++}",
                enabled : true,
        ], properties)
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

    @Deprecated
    static Pipeline createDefaultOtpPipeline() {
        createPipeline(Pipeline.Name.DEFAULT_OTP, Pipeline.Type.ALIGNMENT)
    }

    @Deprecated
    static Pipeline createOtpSnvPipelineLazy() {
        createPipeline(Pipeline.Name.OTP_SNV, Pipeline.Type.SNV)
    }

    static Pipeline createRoddySnvPipelineLazy() {
        createPipeline(Pipeline.Name.RODDY_SNV, Pipeline.Type.SNV)
    }

    static Pipeline createIndelPipelineLazy() {
        createPipeline(Pipeline.Name.RODDY_INDEL, Pipeline.Type.INDEL)
    }

    static Pipeline createAceseqPipelineLazy() {
        createPipeline(Pipeline.Name.RODDY_ACESEQ, Pipeline.Type.ACESEQ)
    }

    static Pipeline createSophiaPipelineLazy() {
        createPipeline(Pipeline.Name.RODDY_SOPHIA, Pipeline.Type.SOPHIA)
    }

    static Pipeline createExternallyProcessedPipelineLazy() {
        createPipeline(Pipeline.Name.EXTERNALLY_PROCESSED, Pipeline.Type.ALIGNMENT)
    }

    static Pipeline createRunYapsaPipelineLazy() {
        createPipeline(Pipeline.Name.RUN_YAPSA, Pipeline.Type.MUTATIONAL_SIGNATURE)
    }


    static Pipeline returnOrCreateAnyPipeline() {
        return (atMostOneElement(Pipeline.list(max: 1)) ?: createPanCanPipeline())
    }

    public static MergingSet createMergingSet(Map properties = [:]) {
        MergingWorkPackage mergingWorkPackage = properties.mergingWorkPackage ?: createMergingWorkPackage([pipeline: createDefaultOtpPipeline()])
        return createDomainObject(MergingSet, [
                mergingWorkPackage: mergingWorkPackage,
                identifier        : MergingSet.nextIdentifier(mergingWorkPackage),
        ], properties)
    }

    public static MergingSet createMergingSet(final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return createDomainObject(MergingSet, [
                mergingWorkPackage: mergingWorkPackage,
                identifier        : MergingSet.nextIdentifier(mergingWorkPackage),
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
                abstractMergedBamFile: { createProcessedMergedBamFile() }
        ], properties)
    }

    public static Map defaultValuesForAbstractQualityAssessment = [
            qcBasesMapped                  : 0,
            totalReadCounter               : 0,
            qcFailedReads                  : 0,
            duplicates                     : 0,
            totalMappedReadCounter         : 0,
            pairedInSequencing             : 0,
            pairedRead1                    : 0,
            pairedRead2                    : 0,
            properlyPaired                 : 0,
            withItselfAndMateMapped        : 0,
            withMateMappedToDifferentChr   : 0,
            withMateMappedToDifferentChrMaq: 0,
            singletons                     : 0,
            insertSizeMedian               : 0,
            insertSizeSD                   : 0,
            referenceLength                : 1,
    ].asImmutable()

    public static OverallQualityAssessmentMerged createOverallQualityAssessmentMerged(Map properties = [:]) {
        return createDomainObject(OverallQualityAssessmentMerged, defaultValuesForAbstractQualityAssessment + [
                qualityAssessmentMergedPass: { createQualityAssessmentMergedPass() },
        ], properties)
    }

    public static ChromosomeQualityAssessmentMerged createChromosomeQualityAssessmentMerged(Map properties = [:]) {
        return createDomainObject(ChromosomeQualityAssessmentMerged, defaultValuesForAbstractQualityAssessment + [
                chromosomeName             : "chromosomeName_${counter++}",
                qualityAssessmentMergedPass: { createQualityAssessmentMergedPass() },
        ], properties)
    }

    public static JobExecutionPlan createJobExecutionPlan(Map properties = [:]) {
        return createDomainObject(JobExecutionPlan, [
                name       : "planName_${counter++}",
                planVersion: 0,
                obsoleted  : false,
        ], properties)
    }

    public static Process createProcess(Map properties = [:]) {
        return createDomainObject(Process, [
                started         : new Date(),
                startJobClass   : "startJobClass",
                jobExecutionPlan: { createJobExecutionPlan() }
        ], properties)
    }

    public static JobDefinition createJobDefinition(Map properties = [:]) {
        return createDomainObject(JobDefinition, [
                plan: { createJobExecutionPlan() },
                name: "name_${counter++}",
                bean: 'beanName',
        ], properties)
    }

    public static ProcessParameter createProcessParameter(Map properties = [:]) {
        return createDomainObject(ProcessParameter, [
                process  : { createProcessingStepUpdate().process },
                value    : "${counter++}",
                className: "${counter++}",
        ], properties)
    }

    public static ProcessingStep createProcessingStep(Map properties = [:]) {
        JobExecutionPlan jobExecutionPlan = properties.jobDefinition?.plan ?: properties.process?.jobExecutionPlan ?: createJobExecutionPlan()
        return createDomainObject(ProcessingStep, [
                jobDefinition: { createJobDefinition(plan: jobExecutionPlan) },
                jobClass     : 'someClass',
                process      : { createProcess(jobExecutionPlan: jobExecutionPlan) },
        ], properties)
    }

    public static RestartedProcessingStep createRestartedProcessingStep(Map properties = [:]) {
        ProcessingStep original = properties.original ?: createProcessingStep()
        return createDomainObject(RestartedProcessingStep, [
                jobDefinition: original.jobDefinition,
                jobClass     : 'someClass',
                process      : original.process,
                original     : original,
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
                processingStep: { createProcessingStep() },
                realm         : { createRealm() },
                clusterJobId  : "clusterJobId_${counter++}",
                userName      : "userName_${counter++}",
                clusterJobName: "clusterJobName_${counter++}_jobClass",
                jobClass      : "jobClass",
                queued        : new DateTime(),
        ], properties)
    }

    public static ProcessingOption createProcessingOptionLazy(Map properties = [:]) {
        ProcessingOption processingOption = createDomainObjectLazy(ProcessingOption,
                [value: properties.containsKey("value") ? properties['value'] : "processingOptionValue_${counter++}"],
                properties.findAll { it.key != "value" },
        )
        if (properties.containsKey("value")) {
            processingOption.value = properties.value
        }
        processingOption.save(flush: true)
        return processingOption
    }

    static ProcessingOption createProcessingOptionLazy(OptionName optionName, String value, String type = null) {
        return createProcessingOptionLazy([
                name: optionName,
                value: value,
                type: type,
        ])
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
                }
        ], properties)
    }

    static void createProcessingOptionForOtrsTicketPrefix(String prefix = "Prefix ${counter++}") {
        createProcessingOptionLazy(
                 name: OptionName.TICKET_SYSTEM_NUMBER_PREFIX,
                 type: null,
                 project: null,
                 value: prefix,
        )
    }

    static void createProcessingOptionForNotificationRecipient(String recipientEmail = HelperUtils.randomEmail) {
        createProcessingOptionLazy(
                name: OptionName.EMAIL_RECIPIENT_NOTIFICATION,
                type: null,
                project: null,
                value: recipientEmail,
        )
    }

    public
    static void createProcessingOptionForErrorRecipient(String recipientEmail = HelperUtils.randomEmail) {
        createProcessingOptionLazy(
                name: OptionName.EMAIL_RECIPIENT_ERRORS,
                type: null,
                project: null,
                value: recipientEmail,
        )
    }

    static void createProcessingOptionForEmailSenderSalutation(String message = "the service team${counter++}") {
        createProcessingOptionLazy(
                name: OptionName.EMAIL_SENDER_SALUTATION,
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

    static void createProcessingOptionForInitRoddyModule() {
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, '')
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, '')
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, '')
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
                comment         : "comment ${counter++}",
                author          : "author ${counter++}",
                modificationDate: { new Date() },
        ], properties)
    }

    public static createIlseSubmission(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(IlseSubmission, [
                ilseNumber: { counter++ % 999000 + 1000 },
                warning   : false,
        ], properties, saveAndValidate)
    }

    public static AlignmentPass createAlignmentPass(Map properties = [:]) {
        final SeqTrack seqTrack = properties.get('seqTrack') ?: createSeqTrack([:])

        if (!seqTrack.seqPlatform.seqPlatformGroups) {
            seqTrack.seqPlatform.addToSeqPlatformGroups(createSeqPlatformGroup())
            seqTrack.seqPlatform.save(flush: true)
        }

        final MergingWorkPackage workPackage = findOrSaveMergingWorkPackage(
                seqTrack,
                properties.get('referenceGenome'),
                properties.get('pipeline')
        )
        properties.remove("referenceGenome")
        properties.remove("description")
        final AlignmentPass alignmentPass = createDomainObject(AlignmentPass, [
                identifier    : AlignmentPass.nextIdentifier(seqTrack),
                seqTrack      : seqTrack,
                workPackage   : workPackage,
                alignmentState: AlignmentPass.AlignmentState.FINISHED,
        ], properties)
        return alignmentPass
    }

    public
    static MergingWorkPackage findOrSaveMergingWorkPackage(SeqTrack seqTrack, ReferenceGenome referenceGenome = null, Pipeline pipeline = null) {
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        if (referenceGenome == null || pipeline == null) {
            MergingWorkPackage workPackage = MergingWorkPackage.findWhere(
                    sample: seqTrack.sample,
                    seqType: seqTrack.seqType,
                    seqPlatformGroup: seqTrack.seqPlatformGroup
            )
            if (workPackage != null) {
                assert workPackage.libraryPreparationKit == seqTrack.libraryPreparationKit
                return workPackage
            }
        }

        final MergingWorkPackage mergingWorkPackage = MergingWorkPackage.findOrSaveWhere(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: referenceGenome ?: createReferenceGenomeLazy(),
                libraryPreparationKit: seqTrack.libraryPreparationKit,
                pipeline: pipeline ?: createDefaultOtpPipeline(),
        )
        return mergingWorkPackage
    }


    public static ReferenceGenome createReferenceGenomeLazy() {
        return ReferenceGenome.find { true } ?: createReferenceGenome()
    }

    public
    static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:], boolean saveAndValidate = true) {
        MergingSet mergingSet = createMergingSet(mergingWorkPackage)
        return createProcessedMergedBamFileWithoutProcessedBamFile(mergingSet, properties, saveAndValidate)
    }

    public
    static ProcessedMergedBamFile createProcessedMergedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingWorkPackage, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public
    static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(Map properties = [:], boolean saveAndValidate = true) {
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

    public
    static ProcessedMergedBamFile createProcessedMergedBamFile(Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public
    static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingSet mergingSet, Map properties = [:], boolean saveAndValidate = true) {
        return createProcessedMergedBamFileWithoutProcessedBamFile(properties + [
                mergingPass: createMergingPass([
                        mergingSet: mergingSet,
                        identifier: MergingPass.nextIdentifier(mergingSet),
                ]),
        ], saveAndValidate)
    }

    public
    static ProcessedMergedBamFile createProcessedMergedBamFile(MergingSet mergingSet, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingSet, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public
    static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingPass mergingPass, Map properties = [:], boolean saveAndValidate = true) {
        return createProcessedMergedBamFileWithoutProcessedBamFile([
                mergingPass: mergingPass,
        ] + properties, saveAndValidate)
    }

    public
    static ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    public
    static ProcessedMergedBamFile createFinishedProcessedMergedBamFile(Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile([
                fileOperationStatus: FileOperationStatus.PROCESSED,
                md5sum             : HelperUtils.randomMd5sum,
                fileSize           : counter++ ,
        ] + properties)
        MergingWorkPackage mergingWorkPackage = processedMergedBamFile.mergingWorkPackage
        mergingWorkPackage.bamFileInProjectFolder = processedMergedBamFile
        assert mergingWorkPackage.save(flush: true)
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
                bamFile   : bamFile,
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

    public static ProcessedBamFile createProcessedBamFile(
            final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        SeqTrack seqTrack = createSeqTrackWithDataFiles(mergingWorkPackage)
        mergingWorkPackage.seqTracks.add(seqTrack)
        mergingWorkPackage.save(flush: true, failOnError: true)

        final ProcessedBamFile bamFile = createProcessedBamFile([
                alignmentPass          : createAlignmentPass([
                        seqTrack       : seqTrack,
                        workPackage    : mergingWorkPackage,
                        referenceGenome: mergingWorkPackage.referenceGenome,
                ]),
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
        ] + properties)

        return bamFile
    }

    public static <T> T createRoddyBamFile(Map bamFileProperties = [:], Class<T> clazz = RoddyBamFile) {
        MergingWorkPackage workPackage = bamFileProperties.workPackage
        if (!workPackage) {
            SeqType seqType = (clazz == RnaRoddyBamFile) ? createRnaPairedSeqType() : createWholeGenomeSeqType()
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
        workPackage.seqTracks = seqTracks
        workPackage.save(flush: true, failOnError: true)
        T bamFile = createDomainObject(clazz, [
                numberOfMergedLanes         : seqTracks.size(),
                workDirectoryName           : "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks                   : seqTracks as Set,
                workPackage                 : workPackage,
                identifier                  : RoddyBamFile.nextIdentifier(workPackage),
                config                      : {
                    createRoddyWorkflowConfigLazy(
                            pipeline: workPackage.pipeline,
                            project: workPackage.project,
                            seqType: workPackage.seqType,
                            adapterTrimmingNeeded: workPackage.seqType.isRna() || workPackage.seqType.isWgbs() || workPackage.seqType.isChipSeq(),
                    )
                },
                md5sum                      : {
                    (!bamFileProperties.containsKey('fileOperationStatus') || bamFileProperties.fileOperationStatus == FileOperationStatus.PROCESSED) ? HelperUtils.randomMd5sum : null
                },
                fileOperationStatus         : FileOperationStatus.PROCESSED,
                fileSize                    : 10000,
                roddyExecutionDirectoryNames: [DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                comment                     : {
                    [
                            AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED,
                            AbstractMergedBamFile.QcTrafficLightStatus.REJECTED,
                    ].contains(bamFileProperties.qcTrafficLightStatus) ? createComment() : null
                }
        ], bamFileProperties)
        return bamFile
    }

    public static RnaRoddyBamFile createRnaRoddyBamFile(Map bamFileProperties = [:]) {
        createRoddyBamFile(bamFileProperties, RnaRoddyBamFile)
    }

    public static createRoddyBamFile(RoddyBamFile baseBamFile, Map bamFileProperties = [:]) {
        RoddyBamFile bamFile = createDomainObject(RoddyBamFile, [
                baseBamFile        : baseBamFile,
                config             : baseBamFile.config,
                workPackage        : baseBamFile.workPackage,
                identifier         : baseBamFile.identifier + 1,
                numberOfMergedLanes: baseBamFile.numberOfMergedLanes + 1,
                workDirectoryName  : "${RoddyBamFile.WORK_DIR_PREFIX}_${counter++}",
                seqTracks          : bamFileProperties.seqTracks ?: [createSeqTrackWithDataFiles(baseBamFile.workPackage)],
                md5sum             : HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
                fileSize           : 10000,
        ], bamFileProperties)
        return bamFile
    }

    /**
     * Because RoddyMergedBamQa defines a unique constraint with 'class', the instance can only be created in integration tests.
     */
    public static createRoddyMergedBamQa(Map properties = [:]) {
        return createDomainObject(RoddyMergedBamQa, defaultValuesForAbstractQualityAssessment + [
                qualityAssessmentMergedPass  : {
                    createQualityAssessmentMergedPass(
                            abstractMergedBamFile: createRoddyBamFile()
                    )
                },
                chromosome                   : RoddyQualityAssessment.ALL,
                insertSizeCV                 : 0,
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
                referenceLength            : 1,
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
                sample         : sample,
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
        if (base.seqType.isChipSeq()) {
            mergingProperties.add("antibodyTarget")
        }

        MergingWorkPackage mwp = new MergingWorkPackage((mergingProperties + []).collectEntries {
            [it, base."${it}"]
        } + properties)
        assert mwp.save(failOnError: true)
        return mwp
    }

    static SamplePair createSamplePair(Map properties = [:]) {
        AbstractMergingWorkPackage mergingWorkPackage1 = properties.mergingWorkPackage1 ?:
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


    static SamplePair createSamplePair(AbstractMergingWorkPackage mergingWorkPackage1, Map properties = [:]) {
        return createSamplePair(
                mergingWorkPackage1,
                properties.mergingWorkPackage2 ?: createMergingWorkPackage(mergingWorkPackage1),
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

    static SampleTypePerProject createSampleTypePerProjectForMergingWorkPackage(AbstractMergingWorkPackage mergingWorkPackage, SampleType.Category category = SampleType.Category.DISEASE) {
        return createSampleTypePerProject([
                project   : mergingWorkPackage.project,
                sampleType: mergingWorkPackage.sampleType,
                category  : category,
        ])
    }

    static SampleTypePerProject createSampleTypePerProjectForBamFile(AbstractMergedBamFile bamFile, SampleType.Category category = SampleType.Category.DISEASE) {
        return createSampleTypePerProjectForMergingWorkPackage(bamFile.mergingWorkPackage, category)
    }

    static SamplePair createSamplePair(AbstractMergingWorkPackage mergingWorkPackage1, AbstractMergingWorkPackage mergingWorkPackage2, Map properties = [:]) {
        SamplePair samplePair = SamplePair.createInstance([
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ] + properties)
        return samplePair.save(failOnError: true)
    }

    static
    def createProcessableSamplePair(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:]) {
        def map = createAnalysisInstanceWithRoddyBamFilesMapHelper(properties, [coverage: 30] + bamFile1Properties, [coverage: 30] + bamFile2Properties)

        SamplePair samplePair = map.samplePair
        AbstractMergedBamFile bamFile1 = map.sampleType1BamFile
        AbstractMergedBamFile bamFile2 = map.sampleType2BamFile
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

        createProcessingOptionLazy(
                name: OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                type: null,
                project: null,
                value: 'hs37d5, hs37d5_PhiX, hs37d5_GRCm38mm_PhiX, hs37d5+mouse, hs37d5_GRCm38mm',
        )

        return samplePair
    }

    static SamplePair createSamplePairWithExternalProcessedMergedBamFiles(boolean initPipelines = false, Map bamFileProperties = [:]) {
        ExternalMergingWorkPackage tumorMwp = DomainFactory.createExternalMergingWorkPackage(
                seqType: createWholeGenomeSeqType(),
                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
        )

        ExternalMergingWorkPackage controlMwp = DomainFactory.createExternalMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: DomainFactory.createSample(
                        individual: tumorMwp.getIndividual()
                )
        )

        [
                tumorMwp,
                controlMwp,
        ].each {
            ExternallyProcessedMergedBamFile bamFile = DomainFactory.createExternallyProcessedMergedBamFile(
                    getRandomProcessedBamFileProperties() + [
                            workPackage      : it,
                            coverage         : 30.0,
                            insertSizeFile   : 'insertSize.txt',
                            maximumReadLength: 101,
                    ] + bamFileProperties,
            )
            bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
            assert bamFile.mergingWorkPackage.save(flush: true)
        }

        createSampleTypePerProjectForMergingWorkPackage(tumorMwp, SampleType.Category.DISEASE)
        createSampleTypePerProjectForMergingWorkPackage(controlMwp, SampleType.Category.CONTROL)

        SamplePair samplePair = DomainFactory.createSamplePair(tumorMwp, controlMwp)

        if (initPipelines) {
            initAnalysisForSamplePair(samplePair)
        }

        return samplePair
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
     *
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

        [
                OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME,
        ].each {
            createProcessingOptionLazy(
                    name: it,
                    type: null,
                    project: null,
                    value: "${samplePair.mergingWorkPackage1.referenceGenome.name}, ${samplePair.mergingWorkPackage2.referenceGenome.name}",
            )
        }
    }

    static SamplePair createDisease(AbstractMergingWorkPackage controlMwp) {
        MergingWorkPackage diseaseMwp = createMergingWorkPackage(controlMwp)
        createSampleTypePerProject(project: controlMwp.project, sampleType: diseaseMwp.sampleType, category: SampleType.Category.DISEASE)
        SamplePair samplePair = createSamplePair(diseaseMwp, controlMwp)
        return samplePair
    }

    @Deprecated
    public static SnvConfig createSnvConfig(Map properties = [:]) {
        return createDomainObject(SnvConfig, [
                configuration        : "configuration_${counter++}",
                externalScriptVersion: "1.0",
                seqType              : { createSeqType() },
                project              : { createProject() },
                pipeline             : { createOtpSnvPipelineLazy() },
        ], properties)
    }


    private
    static Map createAnalysisInstanceWithRoddyBamFilesMapHelper(Map properties, Map bamFile1Properties, Map bamFile2Properties) {
        Pipeline pipeline = createPanCanPipeline()

        SamplePair samplePair = properties.samplePair
        AbstractMergedBamFile diseaseBamFile = properties.sampleType1BamFile
        AbstractMergedBamFile controlBamFile = properties.sampleType2BamFile

        AbstractMergingWorkPackage diseaseWorkPackage = diseaseBamFile?.mergingWorkPackage
        AbstractMergingWorkPackage controlWorkPackage = controlBamFile?.mergingWorkPackage

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
                controlWorkPackage = createMergingWorkPackage([
                        pipeline        : pipeline,
                        statSizeFileName: DEFAULT_TAB_FILE_NAME,
                        seqType         : diseaseWorkPackage?.seqType ?: createWholeGenomeSeqType(),
                        sample          : createSample([
                                individual: diseaseWorkPackage?.sample?.individual ?: createIndividual(),
                        ])
                ])
            }
            if (!diseaseWorkPackage) {
                diseaseWorkPackage = createMergingWorkPackage(
                        pipeline: pipeline,
                        statSizeFileName: DEFAULT_TAB_FILE_NAME,
                        seqType: controlWorkPackage.seqType,
                        sample: createSample([
                                individual: controlWorkPackage.sample.individual,
                        ])
                )
            }
            createSampleTypePerProjectLazy([
                    project   : diseaseWorkPackage.project,
                    sampleType: diseaseWorkPackage.sampleType,
                    category  : SampleType.Category.DISEASE,
            ])
            samplePair = createSamplePair(diseaseWorkPackage, controlWorkPackage)
        }

        if (!diseaseBamFile) {
            diseaseBamFile = createRoddyBamFile(
                    [ workPackage: diseaseWorkPackage ] +
                    (controlBamFile ? [ config: controlBamFile.config ] : [:]) +
                    bamFile1Properties
            )
        }
        if (!controlBamFile) {
            controlBamFile = createRoddyBamFile([
                    workPackage: controlWorkPackage,
                    config     : diseaseBamFile.config,
            ] + bamFile2Properties)
        }

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

    static SophiaQc createSophiaQc(Map properties = [:], boolean createAndValidate = true) {
        return createDomainObject(SophiaQc, [

                sophiaInstance                       : { createSophiaInstanceWithRoddyBamFiles() },
                controlMassiveInvPrefilteringLevel   : 1,
                tumorMassiveInvFilteringLevel        : 1,
                rnaContaminatedGenesMoreThanTwoIntron: "arbitraryGeneName",
                rnaContaminatedGenesCount            : 1,
                rnaDecontaminationApplied            : true,
        ], properties, createAndValidate)
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


    public static AceseqQc createAceseqQcWithExistingAceseqInstance(AceseqInstance aceseqInstance) {
        createAceseqQc([:], [:], [:], aceseqInstance)
    }

    public
    static AceseqQc createAceseqQc(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:], AceseqInstance aceseqInstance = null) {
        aceseqInstance = aceseqInstance ?: createAceseqInstanceWithRoddyBamFiles(properties, bamFile1Properties, bamFile2Properties)
        return createDomainObject(AceseqQc, [
                aceseqInstance  : aceseqInstance,
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


    public static AntibodyTarget createAntibodyTarget(Map properties = [:]) {
        return createDomainObject(AntibodyTarget, [
                name       : 'antibodyTargetName_' + (counter++),
                importAlias: [],
        ], properties)
    }

    public static SeqCenter createSeqCenter(Map seqCenterProperties = [:]) {
        return createDomainObject(SeqCenter, [
                name   : 'seqCenterName_' + (counter++),
                dirName: 'seqCenterDirName_' + (counter++),
        ], seqCenterProperties)
    }

    public static SeqPlatform createSeqPlatformWithSeqPlatformGroup(Map seqPlatformProperties = [:]) {
        Set<SeqPlatformGroup> spg = seqPlatformProperties.seqPlatformGroups as Set ?: [createSeqPlatformGroup()] as Set

        SeqPlatform sp = createSeqPlatform([seqPlatformGroups: spg] + seqPlatformProperties)

        sp.seqPlatformGroups.each {
            sp.addToSeqPlatformGroups(it)
        }
        sp.save(flush: true)

        return sp
    }

    static SeqPlatform createSeqPlatform(Map seqPlatformProperties = [:]) {
        return createDomainObject(SeqPlatform, [
                name                 : 'seqPlatform_' + (counter++),
                seqPlatformModelLabel: { createSeqPlatformModelLabel() },
        ], seqPlatformProperties)
    }

    public static SeqPlatformModelLabel createSeqPlatformModelLabel(Map properties = [:]) {
        return createDomainObject(SeqPlatformModelLabel, [
                name       : 'seqPlatformModelLabel_' + (counter++),
                importAlias: [],
        ], properties)
    }

    public static SequencingKitLabel createSequencingKitLabel(Map properties = [:]) {
        return createDomainObject(SequencingKitLabel, [
                name       : 'SequencingKitLabel_' + (counter++),
                importAlias: [],
        ], properties)
    }

    public static SeqPlatformGroup createSeqPlatformGroup(Map properties = [:]) {
        return createDomainObject(SeqPlatformGroup, [:], properties)
    }

    static SeqPlatformGroup createSeqPlatformGroupWithMergingCriteria(Map properties = [:]) {
        return createDomainObject(SeqPlatformGroup, [
                mergingCriteria: createMergingCriteriaLazy(
                        seqPlatformGroup: MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC
                )
        ], properties)

    }

    public static Run createRun(Map runProperties = [:]) {
        return createDomainObject(Run, [
                name       : 'runName_' + (counter++),
                seqCenter  : { createSeqCenter() },
                seqPlatform: { createSeqPlatformWithSeqPlatformGroup() },
        ], runProperties)
    }

    public static RunSegment createRunSegment(Map runSegmentProperties = [:]) {
        return createDomainObject(RunSegment, [
                importMode: RunSegment.ImportMode.AUTOMATIC,
        ], runSegmentProperties)
    }

    public static Project createProject(Map projectProperties = [:]) {
        return createDomainObject(Project, [
                name                    : 'project_' + (counter++),
                phabricatorAlias        : 'phabricatorAlias_' + (counter++),
                dirName                 : 'projectDirName_' + (counter++),
                realm                   : { createRealm() },
                alignmentDeciderBeanName: 'DUMMY_BEAN_NAME',
        ], projectProperties)
    }

    public static ProjectCategory createProjectCategory(Map projectProperties = [:]) {
        return createDomainObject(ProjectCategory, [
                name: 'projectCategory_' + (counter++),
        ], projectProperties)
    }

    public static UserProjectRole createUserProjectRole(Map userProjectRoleProperties = [:]) {
        return createDomainObject(UserProjectRole, [
                user       : { createUser() },
                project    : { createProject() },
                projectRole: { createProjectRole() },
                manageUsers: false,
        ], userProjectRoleProperties)
    }

    public static ProjectRole createProjectRole(Map projectRoleProperties = [:]) {
        return createDomainObject(ProjectRole, [
                name                  : 'roleName_' + (counter++),
                accessToOtp           : false,
                accessToFiles         : false,
                manageUsersAndDelegate: false,
        ], projectRoleProperties)
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
                name                   : 'sampleTypeName-' + (counter++),
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
                name  : 'sampleIdentifierName_' + (counter++),
        ], properties)
    }

    public static SeqType createSeqType(Map seqTypeProperties = [:], boolean saveAndValidate = true) {
        String defaultName = 'seqTypeName_' + (counter++)
        return createDomainObject(SeqType, [
                name         : defaultName,
                libraryLayout: SeqType.LIBRARYLAYOUT_SINGLE,
                dirName      : 'seqTypeDirName_' + (counter++),
                displayName  : seqTypeProperties.get('displayName') ?: seqTypeProperties.get('name') ?: defaultName,
                importAlias  : [],
                singleCell   : false,
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
                name        : 'softwareToolIdentifier_' + (counter++),
                softwareTool: { createSoftwareTool() },
        ], properties)
    }


    static LibraryPreparationKit createLibraryPreparationKit(Map properties = [:]) {
        return createDomainObject(LibraryPreparationKit, [
                name            : "library-preperation-kit-name_${counter++}",
                shortDisplayName: "library-preperation-kit-short-name_${counter++}",
                importAlias     : [],
        ], properties)
    }

    static ReferenceGenome createReferenceGenome(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ReferenceGenome, [
                name                        : "referencegenome_name${counter++}",
                path                        : HelperUtils.uniqueString,
                fileNamePrefix              : "referencegenome-prefix_${counter++}",
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

    static ReferenceGenomeIndex createReferenceGenomeIndex(Map properties = [:]) {
        return createDomainObject(ReferenceGenomeIndex, [
                referenceGenome : { createReferenceGenome() },
                toolName        : { createToolName() },
                path            : "path_${counter++}",
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
                path           : "path_${counter++}",
                fileName       : "fileName.gtf",
                excludeFileName: "excludeFileName.gtf",
                dexSeqFileName : "dexSeqFileName.gtf",
                gcFileName     : "gcFileName.gtf",
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

    static Map seqTrackProperties(Map properties = [:]) {
        return [
                laneId               : 'laneId_' + counter++,
                sample               : { createSample() },
                pipelineVersion      : { createSoftwareTool() },
                run                  : { properties.run ?: createRun() },
                kitInfoReliability   : properties.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
                normalizedLibraryName: SeqTrack.normalizeLibraryName(properties.libraryName),
        ]
    }

    public static SeqTrack createSeqTrack(Map properties = [:]) {
        if (properties.seqType?.isChipSeq()) {
            return createChipSeqSeqTrack(properties)
        }
        if (properties.seqType?.isExome()) {
            return createExomeSeqTrack(properties)
        }
        return createDomainObject(SeqTrack, seqTrackProperties(properties) + [
                seqType: { createSeqType() },
        ], properties)
    }

    public static ExomeSeqTrack createExomeSeqTrack(Map properties = [:]) {
        return createDomainObject(ExomeSeqTrack, seqTrackProperties(properties) + [
                seqType: { createExomeSeqType() },
        ], properties)
    }

    public static ChipSeqSeqTrack createChipSeqSeqTrack(Map properties = [:]) {
        return createDomainObject(ChipSeqSeqTrack, seqTrackProperties(properties) + [
                seqType       : { createChipSeqType() },
                antibodyTarget: { createAntibodyTarget() },
        ], properties)
    }

    public static FastqcProcessedFile createFastqcProcessedFile(Map properties = [:]) {
        return createDomainObject(FastqcProcessedFile, [
                dataFile: { createDataFile() },
        ], properties)
    }

    static MergingWorkPackage createMergingWorkPackage(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(MergingWorkPackage, [
                libraryPreparationKit: { properties.seqType?.isWgbs() ? null : createLibraryPreparationKit() },
                sample               : { createSample() },
                seqType              : { createSeqType() },
                seqPlatformGroup     : { createSeqPlatformGroup() },
                referenceGenome      : { createReferenceGenome() },
                statSizeFileName     : {
                    properties.pipeline?.name == Pipeline.Name.PANCAN_ALIGNMENT || properties.pipeline == null ?
                            "statSizeFileName_${counter++}.tab" : null
                },
                pipeline             : { createPanCanPipeline() },
                antibodyTarget       : { properties.seqType?.isChipSeq() ? createAntibodyTarget() : null },
        ], properties, saveAndValidate)
    }

    static ExternalMergingWorkPackage createExternalMergingWorkPackage(Map properties = [:]) {
        return createDomainObject(ExternalMergingWorkPackage, [
                sample         : { createSample() },
                seqType        : { createSeqType() },
                referenceGenome: { createReferenceGenome() },
                pipeline       : { createExternallyProcessedPipelineLazy() },
        ], properties)
    }

    static <E extends AbstractMergingWorkPackage> E createMergingWorkPackage(Class<E> clazz, Map properties = [:]) {
        switch (clazz) {
            case MergingWorkPackage:
                return DomainFactory.createMergingWorkPackage(properties)
            case ExternalMergingWorkPackage:
                return DomainFactory.createExternalMergingWorkPackage(properties)
            default:
                throw new RuntimeException("Unknown subclass of AbstractMergingWorkPackage: ${clazz}")
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
                throw new RuntimeException("Unknown alignment pipeline: ${pipelineName}")
        }
    }


    static createFileType(Map properties = [:]) {
        return createDomainObject(FileType, [
                type   : FileType.Type.SEQUENCE,
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
                seqTrack        : seqTrack,
                project         : seqTrack?.project,
                run             : seqTrack?.run,
                runSegment      : { createRunSegment() },
                fileName        : "DataFileFileName_${counter}_R1.gz",
                vbpFileName     : "VbpDataFileFileName_${counter}_R1.gz",
                pathName        : "path_${counter}",
                initialDirectory: TestCase.getUniqueNonExistentPath().path,
                md5sum          : { HelperUtils.getRandomMd5sum() },
                dateExecuted    : new Date(),
                dateFileSystem  : new Date(),
                dateCreated     : new Date(),
                dateLastChecked : new Date(),
                fileWithdrawn   : false,
                fileType        : { createFileType() },
                used            : true,
                fileExists      : true,
                fileLinked      : true,
                fileSize        : counter++,
                mateNumber      : 1,
        ], properties, saveAndValidate)
    }

    static private Map createRoddyWorkflowConfigMapHelper(Map properties = [:]) {
        Pipeline pipeline = properties.containsKey('pipeline') ? properties.pipeline : createPanCanPipeline()
        SeqType seqType = properties.containsKey('seqType') ? properties.seqType : createWholeGenomeSeqType()
        String pluginVersion = properties.containsKey('pluginVersion') ? properties.pluginVersion : "pluginVersion:1.1.${counter++}"
        String configVersion = properties.containsKey('configVersion') ? properties.configVersion : "v1_${counter++}"
        return [
                pipeline             : pipeline,
                seqType              : seqType,
                configFilePath       : {
                    "${TestCase.uniqueNonExistentPath}/${pipeline.name.name()}_${seqType.roddyName}_${seqType.libraryLayout}_${pluginVersion.substring(pluginVersion.indexOf(':') + 1)}_${configVersion}.xml"
                },
                pluginVersion        : pluginVersion,
                configVersion        : configVersion,
                project              : { properties.individual?.project ?: createProject() },
                dateCreated          : { new Date() },
                lastUpdated          : { new Date() },
                adapterTrimmingNeeded: { seqType.isWgbs() || seqType.isRna() || seqType.isChipSeq() },
                nameUsedInConfig     : RoddyWorkflowConfig.getNameUsedInConfig(pipeline.name, seqType, pluginVersion, configVersion)
        ]
    }

    static RoddyWorkflowConfig createRoddyWorkflowConfig(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(RoddyWorkflowConfig, createRoddyWorkflowConfigMapHelper(properties), properties, saveAndValidate)
    }

    static RoddyWorkflowConfig createRoddyWorkflowConfigLazy(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObjectLazy(RoddyWorkflowConfig, createRoddyWorkflowConfigMapHelper(properties), properties, saveAndValidate)
    }

    static private Map createRunYapsaConfigMapHelper(properties) {
        return [
                pipeline      : createRunYapsaPipelineLazy(),
                seqType       : { properties.seqType ?: createSeqType() },
                project       : { properties.project ?: createProject() },
                programVersion: "programmVersion${counter++}",
                dateCreated          : { new Date() },
                lastUpdated          : { new Date() },
        ]
    }

    static RunYapsaConfig createRunYapsaConfig(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(RunYapsaConfig, createRunYapsaConfigMapHelper(properties), properties, saveAndValidate)
    }

    static RunYapsaConfig createRunYapsaConfigLazy(Map properties = [:], boolean saveAndValidate = true) {
        createDomainObjectLazy(RunYapsaConfig, createRunYapsaConfigMapHelper(properties), properties, saveAndValidate)
    }

    public static SeqTrack createSeqTrack(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:]) {
        return createSeqTrack(getMergingProperties(mergingWorkPackage) + seqTrackProperties)
    }

    public static Map getMergingProperties(MergingWorkPackage mergingWorkPackage) {
        SeqPlatform seqPlatform = mergingWorkPackage?.seqPlatformGroup?.seqPlatforms?.sort()?.first()
        if (!seqPlatform) {
            seqPlatform = createSeqPlatformWithSeqPlatformGroup(seqPlatformGroups: [mergingWorkPackage.seqPlatformGroup])
        }

        Map properties = [
                sample               : mergingWorkPackage.sample,
                seqType              : mergingWorkPackage.seqType,
                libraryPreparationKit: mergingWorkPackage.libraryPreparationKit,
                run                  : createRun(seqPlatform: seqPlatform),
        ]
        if (mergingWorkPackage.seqType.isChipSeq()) {
            properties += [
                    antibodyTarget: mergingWorkPackage.antibodyTarget,
            ]
        }
        return properties
    }

    public
    static SeqTrack createSeqTrackWithDataFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        Map map = getMergingProperties(mergingWorkPackage) + [
                kitInfoReliability: mergingWorkPackage.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ] + seqTrackProperties
        SeqTrack seqTrack
        if (mergingWorkPackage.seqType.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED) {
            seqTrack = createSeqTrackWithTwoDataFiles(map, dataFileProperties, dataFileProperties)
        } else {
            seqTrack = createSeqTrackWithOneDataFile(map, dataFileProperties)
        }

        mergingWorkPackage.addToSeqTracks(seqTrack)
        mergingWorkPackage.save(flush: true, failOnError: true)

        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        assert mergingWorkPackage.satisfiesCriteria(seqTrack)
        return seqTrack
    }

    public static SeqTrack createSeqTrackWithOneDataFile(Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        SeqTrack seqTrack
        if (seqTrackProperties.seqType?.name == SeqTypeNames.EXOME.seqTypeName) {
            seqTrack = createExomeSeqTrack(seqTrackProperties)
        } else if (seqTrackProperties.seqType?.name == SeqTypeNames.CHIP_SEQ.seqTypeName) {
            seqTrack = createChipSeqSeqTrack(seqTrackProperties)
        } else {
            seqTrack = createSeqTrack(seqTrackProperties)
        }
        createSequenceDataFile(dataFileProperties + [seqTrack: seqTrack])
        return seqTrack
    }

    public
    static SeqTrack createSeqTrackWithTwoDataFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map dataFileProperties1 = [:], Map dataFileProperties2 = [:]) {
        return createSeqTrackWithTwoDataFiles(getMergingProperties(mergingWorkPackage) + seqTrackProperties, dataFileProperties1, dataFileProperties2)
    }

    public
    static SeqTrack createSeqTrackWithTwoDataFiles(Map seqTrackProperties = [:], Map dataFileProperties1 = [:], Map dataFileProperties2 = [:]) {
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
        SeqTrack seqTrack = createSeqTrackWithOneDataFile([seqType: createSeqType(libraryLayout: LibraryLayout.PAIRED)] + seqTrackProperties, defaultMap1 + dataFileProperties1)
        createSequenceDataFile(defaultMap2 + dataFileProperties2 + [seqTrack: seqTrack])
        return seqTrack
    }

    public static DataFile createSequenceDataFile(final Map properties = [:]) {
        Map defaultProperties = [
                dateCreated   : new Date(),  // In unit tests Grails (sometimes) does not automatically set dateCreated.
                used          : true,
                sequenceLength: 100,
        ]
        if (properties.seqTrack) {
            defaultProperties.project = properties.seqTrack.project
            defaultProperties.run = properties.seqTrack.run
        }
        return createDataFile(defaultProperties + properties)
    }


    public static RoddySnvCallingInstance createRoddySnvCallingInstance(Map properties = [:]) {
        return createDomainObject(RoddySnvCallingInstance, [
                processingState: AnalysisProcessingStates.IN_PROGRESS,
                config: properties.config ?:
                        properties.samplePair ?
                            createRoddyWorkflowConfig(
                                    project: properties.samplePair.project,
                                    seqType: properties.samplePair.seqType,
                            )
                            : createRoddyWorkflowConfig(),
                instanceName: "instance-${counter++}",
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

    public static ProcessingStep createAndSaveProcessingStep(ProcessParameterObject processParameterObject = null) {
        return createAndSaveProcessingStep("de.dkfz.tbi.otp.test.job.jobs.NonExistentDummyJob", processParameterObject)
    }

    public
    static ProcessingStep createAndSaveProcessingStep(String jobClass, ProcessParameterObject processParameterObject = null) {
        final JobExecutionPlan jep = new JobExecutionPlan(name: "DontCare" + sprintf('%016X', new Random().nextLong()), planVersion: 0, startJobBean: "DontCare")
        assert jep.save()
        final JobDefinition jobDefinition = new JobDefinition(name: "DontCare", bean: "DontCare", plan: jep)
        assert jobDefinition.save()
        final Process process = new Process(jobExecutionPlan: jep, started: new Date(), startJobClass: "DontCare")
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

    public static ProcessingStepUpdate createProcessingStepUpdate(
            final ProcessingStep step, final ExecutionState state) {
        return createProcessingStepUpdate([
                date          : new Date(),
                state         : state,
                previous      : step.latestProcessingStepUpdate,
                processingStep: step,
        ])
    }

    public
    static ProcessingStep createProcessingStepWithUpdates(ProcessingStep processingStep = createProcessingStep()) {
        ProcessingStepUpdate last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.CREATED)
        last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.STARTED, previous: last)
        last = createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.FINISHED, previous: last)
        createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.SUCCESS, previous: last)
        return processingStep
    }

    public static ProcessParameter createProcessParameter(
            final Process process, final ProcessParameterObject parameterValue, Map properties = [:]) {
        return createDomainObject(ProcessParameter, [
                process  : process,
                className: parameterValue.class.name,
                value    : parameterValue.id.toString(),
        ], properties)
    }

    public static ProcessParameter createProcessParameter(
            final Process process, final String className, final String value) {
        return createDomainObject(ProcessParameter, [
                process  : process,
                className: className,
                value    : value,
        ], [:])
    }

    public static ClusterJob createClusterJob(
            final ProcessingStep processingStep, final ClusterJobIdentifier clusterJobIdentifier,
            final Map myProps = [
                    clusterJobName   : "testName_${processingStep.nonQualifiedJobClass}",
                    jobClass         : processingStep.nonQualifiedJobClass,
                    queued           : new DateTime(),
                    requestedWalltime: Duration.standardMinutes(5),
                    requestedCores   : 10,
                    requestedMemory  : 1000,
            ]) {
        createDomainObject(ClusterJob, [
                processingStep: processingStep,
                realm         : clusterJobIdentifier.realm,
                clusterJobId  : clusterJobIdentifier.clusterJobId,
                userName      : clusterJobIdentifier.userName,
        ], myProps)
    }

    static SeqType createSeqTypeLazy(SeqTypeNames seqTypeNames, String displayName, String dirName, String roddyName = null, String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED) {
        createDomainObjectLazy(SeqType, [:], [
                name         : seqTypeNames.seqTypeName,
                displayName  : displayName,
                roddyName    : roddyName,
                dirName      : dirName,
                libraryLayout: libraryLayout,
        ]).refresh()
    }

    static SeqType createWholeGenomeSeqType(String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED) {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME, 'WGS', 'whole_genome_sequencing', 'WGS', libraryLayout)
    }

    static SeqType createExomeSeqType(String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED) {
        createSeqTypeLazy(SeqTypeNames.EXOME, 'EXOME', 'exon_sequencing', 'WES', libraryLayout)
    }

    static SeqType createWholeGenomeBisulfiteSeqType() {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE, 'WGBS', 'whole_genome_bisulfite_sequencing', 'WGBS')
    }

    static SeqType createWholeGenomeBisulfiteTagmentationSeqType() {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION, 'WGBS_TAG', 'whole_genome_bisulfite_tagmentation_sequencing', 'WGBSTAG')
    }

    static SeqType createChipSeqType(String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED) {
        createSeqTypeLazy(SeqTypeNames.CHIP_SEQ, 'ChIP', 'chip_seq_sequencing', "CHIPSEQ", libraryLayout)
    }

    static SeqType createRnaPairedSeqType() {
        createSeqTypeLazy(SeqTypeNames.RNA, 'RNA', 'rna_sequencing', "RNA")
    }

    static SeqType createRnaSingleSeqType() {
        createSeqTypeLazy(SeqTypeNames.RNA, 'RNA', 'rna_sequencing', "RNA", LibraryLayout.SINGLE.name())
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
                createChipSeqType(),
        ]
    }

    static List<SeqType> createRnaAlignableSeqTypes() {
        [
                createRnaPairedSeqType(),
                createRnaSingleSeqType(),
        ]
    }

    static List<SeqType> createRoddyAlignableSeqTypes() {
        [
                createPanCanAlignableSeqTypes(),
                createRnaAlignableSeqTypes(),
        ].flatten()
    }

    static List<SeqType> createAllAlignableSeqTypes() {
        [
                createDefaultOtpAlignableSeqTypes(),
                createRoddyAlignableSeqTypes(),
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
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createAceseqSeqTypes() {
        [
                createWholeGenomeSeqType(),
        ]
    }

    static List<SeqType> createRunYapsaSeqTypes() {
        [
                createWholeGenomeSeqType(),
                createExomeSeqType(),
        ]
    }

    static List<SeqType> createAllAnalysableSeqTypes() {
        [
                createSnvSeqTypes(),
                createIndelSeqTypes(),
                createSophiaSeqTypes(),
                createAceseqSeqTypes(),
        ].flatten().unique()
    }

    static MetaDataKey createMetaDataKeyLazy(Map properties = [:]) {
        return createDomainObjectLazy(MetaDataKey, [
                name: "name_${counter++}",
        ], properties)
    }

    static MetaDataEntry createMetaDataEntry(Map properties = [:]) {
        return createDomainObject(MetaDataEntry, [
                value   : "value_${counter++}",
                dataFile: { createDataFile() },
                key     : { createMetaDataKeyLazy() },
                status  : MetaDataEntry.Status.VALID,
                source  : MetaDataEntry.Source.MDFILE,
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
        return createDomainObject(MetaDataFile, [
                fileName  : "MetaDataFileName_${counter++}",
                filePath  : TestCase.getUniqueNonExistentPath().path,
                runSegment: { createRunSegment() },
        ], properties)
    }

    static OtrsTicket createOtrsTicket(Map properties = [:]) {
        return createDomainObject(OtrsTicket, [
                ticketNumber: "20000101" + String.format("%08d", counter++),
        ], properties)
    }

    static TumorEntity createTumorEntity(Map properties = [:]) {
        return createDomainObject(TumorEntity, [
                name: "AML/ALL",
        ], properties)
    }

    static void createRoddyProcessingOptions(File basePath = TestCase.uniqueNonExistentPath) {

        ProcessingOption processingOptionPath = new ProcessingOption(
                name: OptionName.RODDY_PATH,
                type: null,
                project: null,
                value: "${basePath}/roddy",
        )
        assert processingOptionPath.save(flush: true)

        ProcessingOption processingOptionBaseConfigsPath = new ProcessingOption(
                name: OptionName.RODDY_BASE_CONFIGS_PATH,
                type: null,
                project: null,
                value: "${basePath}/roddyBaseConfigs",
        )
        assert processingOptionBaseConfigsPath.save(flush: true)

        ProcessingOption processingOptionApplicationIni = new ProcessingOption(
                name: OptionName.RODDY_APPLICATION_INI,
                type: null,
                project: null,
                value: "${basePath}/roddyBaseConfigs/applicationProperties.ini",
        )
        assert processingOptionApplicationIni.save(flush: true)

        ProcessingOption featureTogglesConfigPath = new ProcessingOption(
                name: OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH,
                type: null,
                project: null,
                value: "${basePath}/roddyBaseConfigs/featureToggles.ini",
        )
        assert featureTogglesConfigPath.save(flush: true)
    }

    static IndelQualityControl createIndelQualityControl(Map properties = [:]) {
        return createDomainObject(IndelQualityControl, [
                indelCallingInstance : { createIndelCallingInstanceWithRoddyBamFiles() },
                file                 : "file",
                numIndels            : counter++,
                numIns               : counter++,
                numDels              : counter++,
                numSize1_3           : counter++,
                numSize4_10          : counter++,
                numSize11plus        : counter++,
                numInsSize1_3        : counter++,
                numInsSize4_10       : counter++,
                numInsSize11plus     : counter++,
                numDelsSize1_3       : counter++,
                numDelsSize4_10      : counter++,
                numDelsSize11plus    : counter++,
                percentIns           : counter++ as double,
                percentDels          : counter++ as double,
                percentSize1_3       : counter++ as double,
                percentSize4_10      : counter++ as double,
                percentSize11plus    : counter++ as double,
                percentInsSize1_3    : counter++ as double,
                percentInsSize4_10   : counter++ as double,
                percentInsSize11plus : counter++ as double,
                percentDelsSize1_3   : counter++ as double,
                percentDelsSize4_10  : counter++ as double,
                percentDelsSize11plus: counter++ as double,
        ], properties)
    }

    static IndelSampleSwapDetection createIndelSampleSwapDetection(Map properties = [:]) {
        IndelCallingInstance indelCallingInstance = properties.get("indelCallingInstance") ?: createIndelCallingInstanceWithRoddyBamFiles()
        return createDomainObject(IndelSampleSwapDetection, [
                indelCallingInstance                            : indelCallingInstance,
                somaticSmallVarsInTumorCommonInGnomADPer        : counter++,
                somaticSmallVarsInControlCommonInGnomad         : counter++,
                tindaSomaticAfterRescue                         : counter++,
                somaticSmallVarsInControlInBiasPer              : counter++,
                somaticSmallVarsInTumorPass                     : counter++,
                pid                                             : indelCallingInstance.individual.pid,
                somaticSmallVarsInControlPass                   : counter++,
                somaticSmallVarsInControlPassPer                : counter++,
                tindaSomaticAfterRescueMedianAlleleFreqInControl: counter++ as double,
                somaticSmallVarsInTumorInBiasPer                : counter++ as double,
                somaticSmallVarsInControlCommonInGnomadPer      : counter++,
                somaticSmallVarsInTumorInBias                   : counter++,
                somaticSmallVarsInControlCommonInGnomasPer      : counter++,
                germlineSNVsHeterozygousInBothRare              : counter++,
                germlineSmallVarsHeterozygousInBothRare         : counter++,
                tindaGermlineRareAfterRescue                    : counter++,
                somaticSmallVarsInTumorCommonInGnomad           : counter++,
                somaticSmallVarsInControlInBias                 : counter++,
                somaticSmallVarsInControl                       : counter++,
                somaticSmallVarsInTumor                         : counter++,
                germlineSNVsHeterozygousInBoth                  : counter++,
                somaticSmallVarsInTumorPassPer                  : counter++ as double,
                somaticSmallVarsInTumorCommonInGnomadPer        : counter++,
        ], properties)
    }

    static ProcessedMergedBamFile createIncrementalMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        MergingSet mergingSet = createMergingSet(processedMergedBamFile.workPackage)
        MergingPass mergingPass = createMergingPass(mergingSet)

        ProcessedMergedBamFile secondBamFile = createProcessedMergedBamFile(mergingPass, [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum             : HelperUtils.randomMd5sum,
                fileSize           : 1000,
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
                fileName           : "bamfile_${counter++}.bam",
                workPackage        : { createExternalMergingWorkPackage() },
                numberOfMergedLanes: null,
                importedFrom       : "/importFrom_${counter++}",
                furtherFiles       : [],
        ], properties)
    }

    static ExternallyProcessedMergedBamFile createFinishedExternallyProcessedMergedBamFile(Map properties = [:]) {
        ExternallyProcessedMergedBamFile externallyProcessedMergedBamFile = createExternallyProcessedMergedBamFile([
                fileOperationStatus: FileOperationStatus.PROCESSED,
                md5sum             : HelperUtils.randomMd5sum,
                fileSize           : counter++,
        ] + properties)
        ExternalMergingWorkPackage externalMergingWorkPackage = externallyProcessedMergedBamFile.mergingWorkPackage
        externalMergingWorkPackage.bamFileInProjectFolder = externallyProcessedMergedBamFile
        assert externalMergingWorkPackage.save(flush: true)
        return externallyProcessedMergedBamFile
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
"""
    }


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

    static void createIndelQcFileOnFileSystem(File qcFile) {
        qcFile.parentFile.mkdirs()
        qcFile << """
{
  "all": {
    "file": "${qcFile.path}",
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


    static void createIndelSampleSwapDetectionFileOnFileSystem(File qcFile, Individual individual) {
        qcFile.parentFile.mkdirs()
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
}
"""

    }


    static void createAclObjects(Object domainObject, Map properties = [:]) {
        AclObjectIdentity aclObjectIdentity = createDomainObject(AclObjectIdentity, [objectId: domainObject.id, aclClass: {
            createDomainObject(AclClass, [className: domainObject.class.name], [:])
        }], [:])
        createDomainObject(AclEntry, [aclObjectIdentity: aclObjectIdentity, sid: {
            createDomainObject(AclSid, [sid: Role.ROLE_ADMIN], properties)
        }], [:])
    }


    static MergingCriteria createMergingCriteria(Map properties = [:]) {
        return createDomainObject(MergingCriteria, [
                project: { createProject() },
                seqType: { createSeqType() },
        ], properties)
    }


    static MergingCriteria createMergingCriteriaLazy(Map properties = [:]) {
        if ((properties.get("seqType") as SeqType)?.isWgbs()) {
            properties.get("libPrepKit") ?: properties.put("libPrepKit", false)
        }

        return createDomainObjectLazy(MergingCriteria, [
                project: { createProject() },
                seqType: { createSeqType() },
        ], properties)
    }

    static QcThreshold createQcThreshold(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(QcThreshold, [
                qcProperty1          : "controlMassiveInvPrefilteringLevel",
                warningThresholdUpper: counter++,
                errorThresholdUpper  : counter++,
                compare              : QcThreshold.ThresholdStrategy.ABSOLUTE_LIMITS,
                qcClass              : SophiaQc.name,
                seqType              : createSeqType(),
        ], properties, saveAndValidate)
    }

    static Document createDocument(Map properties = [:]) {
        return createDomainObject(Document, [
                content: HelperUtils.getUniqueString().bytes,
                type   : Document.Type.PDF,
        ], properties)
    }

    static AggregateSequences createAggregateSequences(Map properties = [:]) {
        createDomainObject(AggregateSequences, [
                seqTypeId              : counter++,
                seqPlatformId          : counter++,
                seqPlatformModelLabelId: counter++,
                sequencingKitLabelId   : counter++,
                sampleId               : counter++,
                seqCenterId            : counter++,
                sampleTypeId           : counter++,
                individualId           : counter++,
                projectId              : counter++,
                realmId                : counter++,
                laneCount              : counter++,
                sum_N_BasePairs        : counter++,
                sum_N_BasePairsGb      : counter++,
                seqPlatformName        : "seqPlatformName${counter++}",
                seqTypeName            : "{seqTypeName${counter++}",
                seqTypeDisplayName     : "seqTypeDisplayName${counter}",
                dirName                : "dirName${counter++}",
                libraryLayout          : "libraryLayout${counter++}",
                sampleTypeName         : "sampleTypeName${counter}",
                pid                    : "pid${counter++}",
                mockPid                : "mockPid${counter++}",
                mockFullName           : "mockFullName${counter++}",
                type                   : Individual.Type.REAL,
                projectName            : "projectName${counter++}",
                projectDirName         : "projectDirName${counter++}",
                realmName              : "realmName${counter++}",
                seqCenterName          : "seqCenterName${counter++}",
                seqCenterDirName       : "seqCenterDirName${counter++}",
        ], properties)
    }
}
