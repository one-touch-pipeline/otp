/*
 * Copyright 2011-2019 The OTP authors
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

import grails.plugin.springsecurity.acl.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaConfig
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.domainFactory.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryInstance
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
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils

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
        return findOrCreateDomainObject(Role, [
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
        createPipeline(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
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

    static MergingSet createMergingSet(Map properties = [:]) {
        MergingWorkPackage mergingWorkPackage = properties.mergingWorkPackage ?: createMergingWorkPackage([pipeline: createDefaultOtpPipeline(), seqType: createSeqType()])
        return createDomainObject(MergingSet, [
                mergingWorkPackage: mergingWorkPackage,
                identifier        : MergingSet.nextIdentifier(mergingWorkPackage),
        ], properties)
    }

    static MergingSet createMergingSet(final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        return createDomainObject(MergingSet, [
                mergingWorkPackage: mergingWorkPackage,
                identifier        : MergingSet.nextIdentifier(mergingWorkPackage),
        ], properties)
    }

    static MergingSetAssignment createMergingSetAssignment(Map properties = [:]) {
        return createDomainObject(MergingSetAssignment, [
                mergingSet: { createMergingSet() },
                bamFile   : { createProcessedBamFile() },
        ], properties)
    }

    static QualityAssessmentMergedPass createQualityAssessmentMergedPass(Map properties = [:]) {
        return createDomainObject(QualityAssessmentMergedPass, [
                abstractMergedBamFile: { createProcessedMergedBamFile() }
        ], properties)
    }

    @Deprecated
    static Map<String, ?> getDefaultValuesForAbstractQualityAssessment() {
        return proxyCellRanger.defaultValuesForAbstractQualityAssessment
    }

    static OverallQualityAssessmentMerged createOverallQualityAssessmentMerged(Map properties = [:]) {
        return createDomainObject(OverallQualityAssessmentMerged, defaultValuesForAbstractQualityAssessment + [
                qualityAssessmentMergedPass: { createQualityAssessmentMergedPass() },
        ], properties)
    }

    static ChromosomeQualityAssessmentMerged createChromosomeQualityAssessmentMerged(Map properties = [:]) {
        return createDomainObject(ChromosomeQualityAssessmentMerged, defaultValuesForAbstractQualityAssessment + [
                chromosomeName             : "chromosomeName_${counter++}",
                qualityAssessmentMergedPass: { createQualityAssessmentMergedPass() },
        ], properties)
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

    static ProcessingOption createProcessingOptionForOtrsTicketPrefix(String prefix = "Prefix ${counter++}") {
        createProcessingOptionLazy(
                name: OptionName.TICKET_SYSTEM_NUMBER_PREFIX,
                type: null,
                project: null,
                value: prefix,
        )
    }

    static ProcessingOption createProcessingOptionForTicketSystemEmail(String ticketSystemEmail = HelperUtils.randomEmail) {
        createProcessingOptionLazy(
                name: OptionName.EMAIL_TICKET_SYSTEM,
                type: null,
                project: null,
                value: ticketSystemEmail,
        )
    }

    static ProcessingOption createProcessingOptionForEmailSenderSalutation(String message = "the service team${counter++}") {
        createProcessingOptionLazy(
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

    static void createProcessingOptionForInitRoddyModule() {
        createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_LOAD_MODULE_LOADER, '')
        createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_JAVA, '')
        createProcessingOptionLazy(ProcessingOption.OptionName.COMMAND_ACTIVATION_GROOVY, '')
    }

    static MergingPass createMergingPass(Map properties = [:]) {
        MergingSet mergingSet = properties.mergingSet ?: createMergingSet()
        return createDomainObject(MergingPass, [
                mergingSet: mergingSet,
                identifier: MergingPass.nextIdentifier(mergingSet),
        ], properties)
    }

    static MergingPass createMergingPass(final MergingSet mergingSet) {
        return createDomainObject(MergingPass, [
                mergingSet: mergingSet,
                identifier: MergingPass.nextIdentifier(mergingSet),
        ], [:])
    }

    static Map getRandomProcessedBamFileProperties() {
        return [
                fileSize           : ++counter,
                md5sum             : HelperUtils.randomMd5sum,
                fileOperationStatus: FileOperationStatus.PROCESSED,
        ]
    }

    static createComment(Map properties = [:]) {
        return createDomainObject(Comment, [
                comment         : "comment ${counter++}",
                author          : "author ${counter++}",
                modificationDate: { new Date() },
        ], properties)
    }

    @Deprecated
    static createIlseSubmission(Map properties = [:], boolean saveAndValidate = true) {
        return proxyCore.createIlseSubmission(properties, saveAndValidate)
    }

    static AlignmentPass createAlignmentPass(Map properties = [:]) {
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

    static AlignmentLog createAlignmentLog(Map properties = [:]) {
        return createDomainObject(AlignmentLog, [
                qcState        : AlignmentLog.QCState.NON,
                executedBy     : AlignmentLog.Execution.UNKNOWN,
                alignmentParams: { createAlignmentParams() },
                seqTrack       : { createSeqTrack() },
        ], properties)
    }

    static AlignmentParams createAlignmentParams(Map properties = [:]) {
        return createDomainObject(AlignmentParams, [
                pipeline: createSoftwareTool(),
                genome  : "genome_${counter++}",
                params  : "params_${counter++}",
        ], properties)
    }

    static QualityAssessmentPass createQualityAssessmentPass(Map properties = [:]) {
        return createDomainObject(QualityAssessmentPass, [
                identifier      : counter++,
                processedBamFile: { createProcessedBamFile() },
        ], properties)
    }

    static Map qaJarQualityAssessmentProperties = [
            duplicateR1                     : 0L,
            duplicateR2                     : 0L,
            properPairStrandConflict        : 0L,
            referenceAgreement              : 0L,
            referenceAgreementStrandConflict: 0L,
            mappedQualityLongR1             : 0L,
            mappedQualityLongR2             : 0L,
            mappedLowQualityR1              : 0L,
            mappedLowQualityR2              : 0L,
            mappedShortR1                   : 0L,
            mappedShortR2                   : 0L,
            notMappedR1                     : 0L,
            notMappedR2                     : 0L,
            endReadAberration               : 0L,
            insertSizeMean                  : 0.0,
            insertSizeRMS                   : 0.0,
            percentIncorrectPEorientation   : 0.0,
            percentReadPairsMapToDiffChrom  : 0.0,
    ]

    static ChromosomeQualityAssessment createChromosomeQualityAssessment(Map properties = [:]) {
        return createDomainObject(ChromosomeQualityAssessment, defaultValuesForAbstractQualityAssessment + qaJarQualityAssessmentProperties + [
                chromosomeName       : "chromosomeName_${counter++}",
                qualityAssessmentPass: { createQualityAssessmentPass() },
        ], properties)
    }

    static OverallQualityAssessment createOverallQualityAssessment(Map properties = [:]) {
        return createDomainObject(OverallQualityAssessment, defaultValuesForAbstractQualityAssessment + qaJarQualityAssessmentProperties + [
                qualityAssessmentPass: { createQualityAssessmentPass() },
        ], properties)
    }

    static PicardMarkDuplicatesMetrics createPicardMarkDuplicatesMetrics(Map properties = [:]) {
        return createDomainObject(PicardMarkDuplicatesMetrics, [
                metricsClass                : "metricsClass_${counter++}",
                library                     : "library_${counter++}",
                unpaired_reads_examined     : 0,
                read_pairs_examined         : 0,
                unmapped_reads              : 0,
                unpaired_read_duplicates    : 0,
                read_pair_duplicates        : 0,
                read_pair_optical_duplicates: 0,
                percent_duplication         : 0.0,
                estimated_library_size      : 0,
        ], properties)
    }

    static MergingWorkPackage findOrSaveMergingWorkPackage(SeqTrack seqTrack, ReferenceGenome referenceGenome = null, Pipeline pipeline = null) {
        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        if (referenceGenome == null || pipeline == null) {
            MergingWorkPackage workPackage = atMostOneElement(MergingWorkPackage.findAllWhere(
                    sample: seqTrack.sample,
                    seqType: seqTrack.seqType,
                    seqPlatformGroup: seqTrack.seqPlatformGroup
            ))
            if (workPackage != null) {
                assert workPackage.libraryPreparationKit == seqTrack.libraryPreparationKit
                return workPackage
            }
        }

        MergingWorkPackage mergingWorkPackage

        Map<String, Object> mwpProperties = [
                sample               : seqTrack.sample,
                seqType              : seqTrack.seqType,
                seqPlatformGroup     : seqTrack.seqPlatformGroup,
                libraryPreparationKit: seqTrack.libraryPreparationKit,
                referenceGenome      : referenceGenome ?: createReferenceGenomeLazy(),
                pipeline             : pipeline ?: createDefaultOtpPipeline(),
        ]

        if (referenceGenome && pipeline) {
            mergingWorkPackage = atMostOneElement(MergingWorkPackage.findAllWhere(mwpProperties))
        }
        return mergingWorkPackage ?: createMergingWorkPackage(mwpProperties)
    }

    static ReferenceGenome createReferenceGenomeLazy() {
        return ReferenceGenome.find { true } ?: createReferenceGenome()
    }

    static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:], boolean saveAndValidate = true) {
        MergingSet mergingSet = createMergingSet(mergingWorkPackage)
        return createProcessedMergedBamFileWithoutProcessedBamFile(mergingSet, properties, saveAndValidate)
    }

    static ProcessedMergedBamFile createProcessedMergedBamFile(MergingWorkPackage mergingWorkPackage, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingWorkPackage, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

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

    static ProcessedMergedBamFile createProcessedMergedBamFile(Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingSet mergingSet, Map properties = [:], boolean saveAndValidate = true) {
        return createProcessedMergedBamFileWithoutProcessedBamFile(properties + [
                mergingPass: createMergingPass([
                        mergingSet: mergingSet,
                        identifier: MergingPass.nextIdentifier(mergingSet),
                ]),
        ], saveAndValidate)
    }

    static ProcessedMergedBamFile createProcessedMergedBamFile(MergingSet mergingSet, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingSet, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    static ProcessedMergedBamFile createProcessedMergedBamFileWithoutProcessedBamFile(MergingPass mergingPass, Map properties = [:], boolean saveAndValidate = true) {
        return createProcessedMergedBamFileWithoutProcessedBamFile([
                mergingPass: mergingPass,
        ] + properties, saveAndValidate)
    }

    static ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass, Map properties = [:], boolean saveAndValidate = true) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile(mergingPass, properties, saveAndValidate)
        assignNewProcessedBamFile(processedMergedBamFile)
        return processedMergedBamFile
    }

    static ProcessedMergedBamFile createFinishedProcessedMergedBamFile(Map properties = [:]) {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFileWithoutProcessedBamFile([
                fileOperationStatus: FileOperationStatus.PROCESSED,
                md5sum             : HelperUtils.randomMd5sum,
                fileSize           : counter++,
        ] + properties)
        MergingWorkPackage mergingWorkPackage = processedMergedBamFile.mergingWorkPackage
        mergingWorkPackage.bamFileInProjectFolder = processedMergedBamFile
        assert mergingWorkPackage.save(flush: true)
        return processedMergedBamFile
    }

    static ProcessedBamFile assignNewProcessedBamFile(final ProcessedMergedBamFile processedMergedBamFile) {
        final ProcessedBamFile bamFile = assignNewProcessedBamFile(processedMergedBamFile.mergingSet)
        processedMergedBamFile.numberOfMergedLanes++
        return bamFile
    }

    static ProcessedBamFile assignNewProcessedBamFile(final MergingSet mergingSet) {
        final ProcessedBamFile bamFile = createProcessedBamFile(mergingSet.mergingWorkPackage)
        createMergingSetAssignment([
                mergingSet: mergingSet,
                bamFile   : bamFile,
        ])
        return bamFile
    }

    static ProcessedSaiFile createProcessedSaiFile(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ProcessedSaiFile, [
                fileExists        : true,
                dateCreated       : { new Date() },
                dateFromFileSystem: { new Date() },
                fileSize          : { counter++ },
                alignmentPass     : { createAlignmentPass() },
                dataFile          : { createDataFile() },
        ], properties, saveAndValidate)
    }

    static ProcessedBamFile createProcessedBamFile(Map properties = [:], boolean saveAndValidate = true) {
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

    static ProcessedBamFile createProcessedBamFile(
            final MergingWorkPackage mergingWorkPackage, Map properties = [:]) {
        SeqTrack seqTrack = createSeqTrackWithDataFiles(mergingWorkPackage)
        mergingWorkPackage.seqTracks.add(seqTrack)
        mergingWorkPackage.save(flush: true)

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

    /**
     * @deprecated Use the {@link de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory#createBamFile} method from the {@link de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory} trait instead.
     */
    @Deprecated
    static <T> T createRoddyBamFile(Map properties = [:]) {
        return proxyRoddy.createBamFile(properties)
    }

    static createRoddyBamFile(RoddyBamFile baseBamFile, Map bamFileProperties = [:]) {
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
    static createRoddyMergedBamQa(Map properties = [:]) {
        return createDomainObject(RoddyMergedBamQa, defaultValuesForAbstractQualityAssessment + roddyQualityAssessmentProperties, properties)
    }

    /**
     * Because RoddyMergedBamQa defines a unique constraint with 'class', the instance can only be created in integration tests.
     */
    static createRoddyMergedBamQa(RoddyBamFile roddyBamFile, Map properties = [:]) {
        return createRoddyMergedBamQa([
                qualityAssessmentMergedPass: createQualityAssessmentMergedPass(
                        abstractMergedBamFile: roddyBamFile
                ),
                referenceLength            : 1,
        ] + properties)
    }

    static Map roddyQualityAssessmentProperties = [
            qualityAssessmentMergedPass  : {
                createQualityAssessmentMergedPass(
                        abstractMergedBamFile: createRoddyBamFile()
                )
            },
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
            AbstractMergedBamFile bamFile, SampleTypePerProject.Category category = SampleTypePerProject.Category.DISEASE) {
        return createSampleTypePerProjectForMergingWorkPackage(bamFile.mergingWorkPackage, category)
    }

    static SamplePair createSamplePair(AbstractMergingWorkPackage mergingWorkPackage1, AbstractMergingWorkPackage mergingWorkPackage2, Map properties = [:]) {
        return createDomainObject(SamplePair, [
                mergingWorkPackage1: mergingWorkPackage1,
                mergingWorkPackage2: mergingWorkPackage2,
        ], properties)
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
        MergingWorkPackage tumorMwp = createMergingWorkPackage(
                seqType: createWholeGenomeSeqType(),
                pipeline: createDefaultOtpPipeline(),
                referenceGenome: createReferenceGenome(name: 'hs37d5')
        )
        ProcessedMergedBamFile bamFileTumor = createProcessedMergedBamFile(tumorMwp, randomProcessedBamFileProperties + [coverage: 30.0])

        ProcessedMergedBamFile bamFileControl = createProcessedMergedBamFile(
                createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
                randomProcessedBamFileProperties + [coverage: 30.0])

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

    static SamplePair createSamplePairWithExternalProcessedMergedBamFiles(boolean initPipelines = false, Map bamFileProperties = [:]) {
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
            ExternallyProcessedMergedBamFile bamFile = createExternallyProcessedMergedBamFile(
                    randomProcessedBamFileProperties + [
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
     * creates an instance of ExternalProcessedMergedBamFileQualityAssessment
     * with the needed quality control values for sophia
     */
    static ExternalProcessedMergedBamFileQualityAssessment createExternalProcessedMergedBamFileQualityAssessment(Map properties = [:], AbstractMergedBamFile mbf) {
        return createDomainObject(ExternalProcessedMergedBamFileQualityAssessment, [
                properlyPaired             : 1919,
                pairedInSequencing         : 2120,
                insertSizeMedian           : 406,
                insertSizeCV               : 23,
                qualityAssessmentMergedPass: {
                    createDomainObject(QualityAssessmentMergedPass, [
                            abstractMergedBamFile: mbf,
                    ], [:])
                },
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
        AbstractMergedBamFile diseaseBamFile = properties.sampleType1BamFile
        AbstractMergedBamFile controlBamFile = properties.sampleType2BamFile

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

    static AceseqQc createAceseqQcWithExistingAceseqInstance(AceseqInstance aceseqInstance) {
        createAceseqQc([:], [:], [:], aceseqInstance)
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

    /**
     * @Deprecated use {@link FastqcDomainFactory#createFastqcProcessedFile()} instead
     */
    @Deprecated
    static FastqcProcessedFile createFastqcProcessedFile(Map properties = [:]) {
        return FastqcDomainFactoryInstance.INSTANCE.createFastqcProcessedFile(properties)
    }

    static Map<String, ?> baseMergingWorkPackageProperties(Map properties) {
        [
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

    @Deprecated
    static createFileType(Map properties = [:]) {
        return proxyCore.createFileType(properties)
    }

    @Deprecated
    static DataFile createDataFile(Map properties = [:], boolean saveAndValidate = true) {
        return proxyCore.createDataFile(properties, saveAndValidate)
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

    static SeqTrack createSeqTrackWithDataFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        Map map = getMergingProperties(mergingWorkPackage) + [
                kitInfoReliability: mergingWorkPackage.libraryPreparationKit ? InformationReliability.KNOWN : InformationReliability.UNKNOWN_UNVERIFIED,
        ] + seqTrackProperties
        SeqTrack seqTrack
        if (mergingWorkPackage.seqType.libraryLayout == SequencingReadType.PAIRED) {
            seqTrack = createSeqTrackWithTwoDataFiles(map, dataFileProperties, dataFileProperties)
        } else {
            seqTrack = createSeqTrackWithOneDataFile(map, dataFileProperties)
        }

        createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        mergingWorkPackage.addToSeqTracks(seqTrack)
        mergingWorkPackage.save(flush: true)

        assert mergingWorkPackage.satisfiesCriteria(seqTrack)
        return seqTrack
    }

    @Deprecated
    static SeqTrack createSeqTrackWithOneDataFile(Map seqTrackProperties = [:], Map dataFileProperties = [:]) {
        return proxyCore.createSeqTrackWithOneDataFile(seqTrackProperties, dataFileProperties)
    }

    static SeqTrack createSeqTrackWithTwoDataFiles(MergingWorkPackage mergingWorkPackage, Map seqTrackProperties = [:], Map dataFileProperties1 = [:], Map dataFileProperties2 = [:]) {
        return createSeqTrackWithTwoDataFiles(getMergingProperties(mergingWorkPackage) + seqTrackProperties, dataFileProperties1, dataFileProperties2)
    }

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
        SeqTrack seqTrack = createSeqTrackWithOneDataFile([seqType: createSeqType(libraryLayout: SequencingReadType.PAIRED)] + seqTrackProperties, defaultMap1 + dataFileProperties1)
        createSequenceDataFile(defaultMap2 + dataFileProperties2 + [seqTrack: seqTrack])
        return seqTrack
    }

    @Deprecated
    static DataFile createSequenceDataFile(final Map properties = [:]) {
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
        createDomainObject(ClusterJob, [
                processingStep: processingStep,
                realm         : clusterJobIdentifier.realm,
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
        findOrCreateDomainObject(SeqType, [:], [
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
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME, 'WGS', 'whole_genome_sequencing', 'WGS', libraryLayout)
    }

    static SeqType createExomeSeqType(SequencingReadType libraryLayout = SequencingReadType.PAIRED) {
        createSeqTypeLazy(SeqTypeNames.EXOME, 'EXOME', 'exon_sequencing', 'WES', libraryLayout, false, false, true)
    }

    static SeqType createWholeGenomeBisulfiteSeqType() {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE, 'WGBS', 'whole_genome_bisulfite_sequencing', 'WGBS')
    }

    static SeqType createWholeGenomeBisulfiteTagmentationSeqType() {
        createSeqTypeLazy(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION, 'WGBS_TAG', 'whole_genome_bisulfite_tagmentation_sequencing', 'WGBSTAG')
    }

    static SeqType createChipSeqType(SequencingReadType libraryLayout = SequencingReadType.PAIRED) {
        createSeqTypeLazy(SeqTypeNames.CHIP_SEQ, 'ChIP', 'chip_seq_sequencing', "CHIPSEQ", libraryLayout, false, true)
    }

    static SeqType createRnaPairedSeqType() {
        createSeqTypeLazy(SeqTypeNames.RNA, 'RNA', 'rna_sequencing', "RNA")
    }

    static SeqType createRnaSingleSeqType() {
        createSeqTypeLazy(SeqTypeNames.RNA, 'RNA', 'rna_sequencing', "RNA", SequencingReadType.SINGLE)
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

    static List<SeqType> createCellRangerAlignableSeqTypes() {
        [
                proxyCellRanger.createSeqType(),
        ].flatten()
    }

    static List<SeqType> createAllAlignableSeqTypes() {
        [
                createDefaultOtpAlignableSeqTypes(),
                createRoddyAlignableSeqTypes(),
                createCellRangerAlignableSeqTypes(),
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
                value   : "value_${counter++}",
                dataFile: { createDataFile() },
                key     : { createMetaDataKey() },
        ], properties)
    }

    static MetaDataEntry createMetaDataKeyAndEntry(DataFile dataFile, String key, String value) {
        MetaDataKey metaDataKey = createMetaDataKeyLazy(name: key)

        return createMetaDataEntry(
                value: value,
                dataFile: dataFile,
                key: metaDataKey,
        )
    }

    static MetaDataEntry createMetaDataKeyAndEntry(DataFile dataFile, MetaDataColumn key, String value) {
        return createMetaDataKeyAndEntry(dataFile, key.name(), value)
    }

    static MetaDataFile createMetaDataFile(Map properties = [:]) {
        return createDomainObject(MetaDataFile, [
                fileName           : "MetaDataFileName_${counter++}",
                filePath           : TestCase.uniqueNonExistentPath.path,
                fastqImportInstance: { createFastqImportInstance() },
        ], properties)
    }

    @Deprecated
    static OtrsTicket createOtrsTicket(Map properties = [:]) {
        return proxyCore.createOtrsTicket(properties)
    }

    @Deprecated
    static OtrsTicket createOtrsTicketWithEndDatesAndNotificationSent(Map properties = [:]) {
        return proxyCore.createOtrsTicketWithEndDatesAndNotificationSent(properties)
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

    @Deprecated
    static ExternallyProcessedMergedBamFile createExternallyProcessedMergedBamFile(Map properties = [:]) {
        return ExternalBamFactoryInstance.INSTANCE.createBamFile(properties)
    }

    @Deprecated
    static ExternallyProcessedMergedBamFile createFinishedExternallyProcessedMergedBamFile(Map properties = [:]) {
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

    static void changeSeqType(RoddyBamFile bamFile, SeqType seqType, SequencingReadType libraryName = null) {
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

    static void createQaFileOnFileSystem(File qaFile, long chromosome8QcBasesMapped) {
        createQaFileOnFileSystem(qaFile, [chromosome8QcBasesMapped: chromosome8QcBasesMapped])
    }

    static void createQaFileOnFileSystem(File qaFile, Map properties = [:]) {
        Map map = [
                chromosome8QcBasesMapped     : '1866013',
                percentageMatesOnDifferentChr: '1.551',
        ] + properties

        qaFile.parentFile.mkdirs()
        // the values are from the documentation on the Wiki: https://wiki.local/NGS/OTP-Roddy+Interface#HTheQCData
        qaFile <<
                """
{
  "8": {
    "genomeWithoutNCoverageQcBases": 0.011,
    "referenceLength": 14636402211,
    "chromosome": 8,
    "qcBasesMapped": ${map.chromosome8QcBasesMapped}
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
    "percentageMatesOnDifferentChr": ${map.percentageMatesOnDifferentChr},
    "chromosome": "all",
    "withItselfAndMateMapped": 4192891,
    "qcBasesMapped": ${map.chromosome8QcBasesMapped},
    "duplicates": 8051,
    "insertSizeCV": 231,
    "referenceLength": 30956774121,
    "properlyPaired": 3847661
  },
  "7": {
    "referenceLength": 1591386631,
    "genomeWithoutNCoverageQcBases": 0.011,
    "qcBasesMapped": ${map.chromosome8QcBasesMapped},
    "chromosome": 7
  }
}
"""
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

    static void createAclObjects(Object domainObject, Map properties = [:]) {
        AclObjectIdentity aclObjectIdentity = createDomainObject(AclObjectIdentity, [objectId: domainObject.id, aclClass: {
            createDomainObject(AclClass, [className: domainObject.class.name], [:])
        }], [:])
        createDomainObject(AclEntry, [aclObjectIdentity: aclObjectIdentity, sid: {
            createDomainObject(AclSid, [sid: Role.ROLE_ADMIN], properties)
        }], [:])
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
                seqType              : createSeqType(),
        ], properties, saveAndValidate)
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
                seqTypeName            : "seqTypeName${counter++}",
                seqTypeDisplayName     : "seqTypeDisplayName${counter}",
                dirName                : "dirName${counter++}",
                libraryLayout          : SequencingReadType.PAIRED,
                sampleTypeName         : "sample-type-name${counter}",
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
                mockFullName       : "mockFullName_${counter++}",
                mockPid            : "mockPid_${counter++}",
                name               : "name_${counter++}",
                pid                : "pid_${counter++}",
                projectName        : "projectName_${counter++}",
                projectDirName     : "projectDirName_${counter++}",
                qualityEncoding    : SeqTrack.QualityEncoding.UNKNOWN,
                realmName          : "realmName_${counter++}",
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
