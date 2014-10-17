package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static org.junit.Assert.*
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvCallingJobTests extends GroovyTestCase{

    public static final String SOME_INSTANCE_NAME = "2014-08-25_15h32"
    public static final String OTHER_INSTANCE_NAME = "2014-09-01_15h32"

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    ExecutionService executionService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    SchedulerService schedulerService

    File testDirectory
    Realm realm_processing
    Project project
    SeqType seqType
    Individual individual
    TestData testData
    SnvCallingInstance snvCallingInstance
    SnvCallingInstance snvCallingInstance2
    ExternalScript externalScript_Calling
    SnvJobResult snvJobResult
    SnvCallingJob snvCallingJob
    SnvCallingInstanceTestData snvCallingTestData

    final String CONFIGURATION ="""
RUN_CALLING=1
RUN_SNV_ANNOTATION=1
RUN_SNV_DEEPANNOTATION=1
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

    final String PBS_ID = "123456"

    @Before
    void setUp() {
        testDirectory = TestCase.createEmptyTestDirectory()
        testData = new TestData()
        testData.createObjects()
        snvCallingTestData = new SnvCallingInstanceTestData()
        realm_processing = testData.realm
        realm_processing.stagingRootPath = "${testDirectory}/staging"
        assert realm_processing.save()

        Realm realm_management = DomainFactory.createRealmDataManagementDKFZ([
            rootPath: "${testDirectory}/root",
            processingRootPath: "${testDirectory}/processing",
            stagingRootPath: null
        ])
        assert realm_management.save()
        project = testData.project
        individual = testData.individual
        seqType = testData.seqType

        ProcessedMergedBamFile processedMergedBamFile1 = createProcessedMergedBamFile("1")
        assert processedMergedBamFile1.save()
        ProcessedMergedBamFile processedMergedBamFile2 = createProcessedMergedBamFile("2")
        assert processedMergedBamFile2.save()

        SnvConfig snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION)
        assert snvConfig.save()

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: processedMergedBamFile1.sampleType,
                sampleType2: processedMergedBamFile2.sampleType,
                seqType: seqType)
        assert sampleTypeCombinationPerIndividual.save()

        snvCallingInstance = new SnvCallingInstance(
                instanceName: SOME_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                sampleTypeCombination: sampleTypeCombinationPerIndividual)
        assert snvCallingInstance.save()

        snvCallingInstance2 = new SnvCallingInstance(
                instanceName: OTHER_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                sampleTypeCombination: sampleTypeCombinationPerIndividual)
        assert snvCallingInstance2.save()

        externalScript_Calling = new ExternalScript(
                scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                filePath: "/tmp/scriptLocation/calling.sh",
                author: "otptest",
                )
        assert externalScript_Calling.save()

        ExternalScript externalScript_Joining = new ExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                filePath: "/tmp/scriptLocation/joining.sh",
                author: "otptest",
                )
        assert externalScript_Joining.save()

        snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                externalScript: externalScript_Calling
                )
        assert snvJobResult.save()

        snvCallingJob = applicationContext.getBean('snvCallingJob',
            DomainFactory.createAndSaveProcessingStep(SnvCallingJob.toString()), [])
        snvCallingJob.log = log

        ParameterType typeRealm = new ParameterType(
                name: REALM,
                className: "${SnvCallingJob.class}",
                jobDefinition: snvCallingJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeRealm.save()

        ParameterType typeScript = new ParameterType(
                name: SCRIPT,
                className: "${SnvCallingJob.class}",
                jobDefinition: snvCallingJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeScript.save()
    }

    @After
    void tearDown() {
        testData = null
        snvCallingTestData = null
        individual = null
        project = null
        seqType = null
        snvCallingInstance = null
        realm_processing = null
        snvCallingInstance2 = null
        externalScript_Calling = null
        snvJobResult = null
        createClusterScriptService.metaClass = null
        snvCallingJob.executionHelperService.executionService.metaClass = null
        LsdfFilesService.metaClass = null
        assert testDirectory.deleteDir()
    }


    @Test
    void testGetStep() {
        assertEquals(SnvCallingStep.CALLING, snvCallingJob.getStep())
    }

    @Test
    void testMaybeSubmitWithSnvCallingInput_ConfiguredNotToRun_ResultsExist() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=1
RUN_SNV_DEEPANNOTATION=1
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} XY)
"""

        snvCallingJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult }
        snvCallingJob.log = log
        assertEquals(NextAction.SUCCEED, snvCallingJob.maybeSubmit(snvCallingInstance2))
    }

    @Test
    void testMaybeSubmitWithSnvCallingInput_ConfiguredNotToRun_NoResultsExist() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=1
RUN_SNV_DEEPANNOTATION=1
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} XY)
"""

        snvCallingJob.metaClass.getProcessParameterObject = { return snvCallingInstance }
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        shouldFail(RuntimeException, { snvCallingJob.maybeSubmit(snvCallingInstance) })
    }

    @Test
    void testMaybeSubmitWithSnvCallingInput() {
        snvJobResult.delete()
        snvCallingJob.metaClass.getProcessParameterObject = { return snvCallingInstance }
        snvCallingJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult ->
            return true
        }
        snvCallingJob.executionHelperService.executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->
            return [PBS_ID]
        }
        schedulerService.startingJobExecutionOnCurrentThread(snvCallingJob)
        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, snvCallingJob.maybeSubmit(snvCallingInstance))
        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(snvCallingJob)
            snvCallingJob.executionHelperService.executionService.metaClass = null
        }
    }

    @Test
    void testValidateWithSnvCallingInput() {
        File configFile = snvCallingTestData.createConfigFileWithContentInFileSystem(
            snvCallingInstance.configFilePath.absoluteStagingPath,
            CONFIGURATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> return true }
        createClusterScriptService.metaClass.createTransferScript = { List<File> sourceLocations, List<File> targetLocations, List<File> linkLocations, boolean move ->
            return "some bash commands to copy the files and link them"
        }
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
        try {
            snvCallingJob.validate(snvCallingInstance)
        } finally {
            configFile.parentFile.deleteDir()
        }
    }

    @Test
    void testWriteConfigFile_InputIsNull() {
        shouldFail( IllegalArgumentException, { snvCallingJob.writeConfigFile(null) })
    }

    @Test
    void testWriteConfigFile_FileExistsAlready() {
        File configFile = snvCallingTestData.createConfigFileWithContentInFileSystem(
            snvCallingInstance.configFilePath.absoluteStagingPath,
            CONFIGURATION)

        assertEquals(configFile, snvCallingJob.writeConfigFile(snvCallingInstance))
        try {
            assert configFile.text == snvCallingInstance.config.configuration
        } finally {
            configFile.parentFile.deleteDir()
        }
    }

    @Test
    void testWriteConfigFile() {
        File file = snvCallingInstance.configFilePath.absoluteStagingPath
        if (file.exists()) {
            file.delete()
        }
        assertEquals(file, snvCallingJob.writeConfigFile(snvCallingInstance))
        try {
            assert file.text == CONFIGURATION
        } finally {
            assert file.delete()
        }
    }

    @Test
    void testCreateAndSaveSnvJobResult() {
        snvJobResult.delete()
        assert SnvJobResult.count() == 0
        snvCallingJob.createAndSaveSnvJobResult(snvCallingInstance, externalScript_Calling)
        final SnvJobResult snvJobResult = exactlyOneElement(SnvJobResult.findAll())
        assert snvJobResult.snvCallingInstance == snvCallingInstance
        assert snvJobResult.processingState == SnvProcessingStates.IN_PROGRESS
        assert snvJobResult.step == SnvCallingStep.CALLING
        assert snvJobResult.externalScript == externalScript_Calling
    }

    @Test
     void testChangeProcessingStateOfJobResult() {
         List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstanceAndStep(snvCallingInstance, SnvCallingStep.CALLING)
         assert results.size == 1
         assert results.first() == snvJobResult
         assert snvJobResult.processingState == SnvProcessingStates.IN_PROGRESS
         snvCallingJob.changeProcessingStateOfJobResult(snvCallingInstance, SnvProcessingStates.FINISHED)
         assert snvJobResult.processingState == SnvProcessingStates.FINISHED
     }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_NoResults_NoPbsOut() {
        /*
         * In this test the method 'addOutputParameter' shall not be called since pbsOutput == false.
         * To make sure that it can be recognized if the method would be called it is overwritten to throw an exception.
         */
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> throw new RuntimeException()}
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        assert shouldFail(RuntimeException, {
            snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance2, false)
        }).contains(SnvCallingStep.CALLING.name())
    }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_NoResults_WithPbsOut() {
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }

        assert shouldFail(RuntimeException, {
            snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance2, false)
        }).contains(SnvCallingStep.CALLING.name())
    }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_WithResults_NoPbsOut() {
        final String errorMessage = "No Pbs output"
        /*
         * In this test the method 'addOutputParameter' shall not be called since pbsOutput == false.
         * To make sure that it can be recognized if the method would be called it is overwritten to throw an exception.
         */
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> throw new RuntimeException(errorMessage) }
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult }

        snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance, false)
    }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_WithResults_WithPbsOut() {
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult }

        snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance, true)
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(String identifier) {
        SampleType sampleType = testData.createSampleType([name: "SampleType"+identifier])
        assert sampleType.save(flush: true)

        Sample sample = testData.createSample([individual: individual, sampleType: sampleType])
        assert sample.save(flush: true)

        SeqTrack seqTrack = testData.createSeqTrack([sample: sample, seqType: seqType])
        assert seqTrack.save(flush: true)

        AlignmentPass alignmentPass = testData.createAlignmentPass([seqTrack: seqTrack])
        assert alignmentPass.save(flush: true)

        ProcessedBamFile processedBamFile = testData.createProcessedBamFile([alignmentPass: alignmentPass,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED])
        assert processedBamFile.save(flush: true)

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage([sample: sample, seqType: seqType])
        assert mergingWorkPackage.save(flush: true)

        MergingSet mergingSet = testData.createMergingSet([mergingWorkPackage: mergingWorkPackage, status: State.PROCESSED])
        assert mergingSet.save(flush: true)

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                mergingSet: mergingSet,
                bamFile: processedBamFile
                )
        assert mergingSetAssignment.save(flush: true)

        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        mergingPass.save(flush: true)

        final String bamFileContent = 'I am a test BAM file. Nice to meet you. :)'

        final ProcessedMergedBamFile bamFile = testData.createProcessedMergedBamFile([mergingPass: mergingPass,
            fileExists: true,
            fileSize: bamFileContent.length(),
            md5sum: '0123456789ABCDEF0123456789ABCDEF',
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
            status: AbstractBamFile.State.PROCESSED,
            fileOperationStatus: FileOperationStatus.PROCESSED])

        final File file = new File(processedMergedBamFileService.destinationDirectory(bamFile), processedMergedBamFileService.fileName(bamFile))
        assert file.path.startsWith(testDirectory.path)
        file.parentFile.mkdirs()
        file.text = bamFileContent

        return bamFile
    }
}
