package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LinkFileUtils
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.TestCase.removeMetaClass
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.utils.CreateSNVFileHelper.createMD5SUMFile
import static de.dkfz.tbi.otp.utils.CreateSNVFileHelper.createResultFile
import static org.junit.Assert.assertEquals

class SnvDeepAnnotationJobTests extends GroovyTestCase {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ExecutionService executionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    LinkFileUtils linkFileUtils

    File testDirectory
    SnvDeepAnnotationJob snvDeepAnnotationJob
    SnvCallingInstance snvCallingInstance1
    SnvCallingInstance snvCallingInstance2
    SnvJobResult snvJobResult_Annotation
    SnvJobResult snvJobResult_DeepAnnotation
    ProcessedMergedBamFile processedMergedBamFile1
    SnvCallingInstanceTestData testData

    public static final String SOME_INSTANCE_NAME = "2014-09-01_15h32"
    public static final String OTHER_INSTANCE_NAME = "2014-09-23_13h37"

    final String CONFIGURATION = """
RUN_CALLING=1
RUN_SNV_ANNOTATION=1
RUN_SNV_DEEPANNOTATION=1
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

    final String PBS_ID = "123456"

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        tmpDir.create()

        testDirectory = tmpDir.newFolder("/otp-test")
        if(!testDirectory.exists()) {
            assert testDirectory.mkdirs()
        }

        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects(testDirectory)

        processedMergedBamFile1 = testData.bamFileTumor
        ProcessedMergedBamFile processedMergedBamFile2 = testData.bamFileControl

        testData.createSnvConfig(CONFIGURATION)

        snvCallingInstance1 = testData.createSnvCallingInstance([
            sampleType1BamFile: processedMergedBamFile1,
            sampleType2BamFile: processedMergedBamFile2,
            instanceName      : SOME_INSTANCE_NAME,
        ])
        assert snvCallingInstance1.save()

        snvCallingInstance2 = testData.createSnvCallingInstance([
            sampleType1BamFile: processedMergedBamFile1,
            sampleType2BamFile: processedMergedBamFile2,
            instanceName      : OTHER_INSTANCE_NAME,
        ])
        assert snvCallingInstance2.save()

        ExternalScript externalScript_Annotation = new ExternalScript(
                scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/annotation.sh",
                author: "otptest",
                )
        assert externalScript_Annotation.save()

        ExternalScript externalScript_Calling = new ExternalScript(
                scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/calling.sh",
                author: "otptest",
                )
        assert externalScript_Calling.save()

        SnvJobResult snvJobResult_Calling = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Calling,
                processingState: SnvProcessingStates.FINISHED,
                chromosomeJoinExternalScript: testData.externalScript_Joining,
                fileSize: 1234l,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                )
        assert snvJobResult_Calling.save()

        snvJobResult_Annotation = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Annotation,
                processingState: SnvProcessingStates.FINISHED,
                inputResult: snvJobResult_Calling,
                )
        assert snvJobResult_Annotation.save()

        ExternalScript externalScript_DeepAnnotation = new ExternalScript(
                scriptIdentifier: SnvCallingStep.SNV_DEEPANNOTATION.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/deepAnnotation.sh",
                author: "otptest",
                )
        externalScript_DeepAnnotation.save()

        snvJobResult_DeepAnnotation = new SnvJobResult(
                step: SnvCallingStep.SNV_DEEPANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_DeepAnnotation,
                inputResult: snvJobResult_Annotation,
                )
        snvJobResult_DeepAnnotation.save()

        snvDeepAnnotationJob = applicationContext.getBean('snvDeepAnnotationJob',
                DomainFactory.createAndSaveProcessingStep(SnvDeepAnnotationJob.toString()), [])
        snvDeepAnnotationJob.log = log

        ParameterType typeRealm = new ParameterType(
                name: REALM,
                className: "${SnvDeepAnnotationJob.class}",
                jobDefinition: snvDeepAnnotationJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeRealm.save()

        ParameterType typeScript = new ParameterType(
                name: SCRIPT,
                className: "${SnvDeepAnnotationJob.class}",
                jobDefinition: snvDeepAnnotationJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeScript.save()
    }

    @After
    void tearDown() {
        testData = null
        snvJobResult_Annotation = null
        snvJobResult_DeepAnnotation = null
        snvCallingInstance1 = null
        snvCallingInstance2 = null
        processedMergedBamFile1 = null
        // Reset meta classes
        removeMetaClass(ExecutionService, executionService)
        removeMetaClass(CreateClusterScriptService, createClusterScriptService)
        removeMetaClass(LinkFileUtils, linkFileUtils)
        LsdfFilesService.metaClass = null
        WaitingFileUtils.metaClass = null
        // Clean-up
        TestCase.cleanTestDirectory()
    }

    @Test
    void testGetStep() {
        assertEquals(SnvCallingStep.SNV_DEEPANNOTATION, snvDeepAnnotationJob.getStep())
    }

    @Test
    void testGetPreviousStep() {
        assertEquals(SnvCallingStep.SNV_ANNOTATION, snvDeepAnnotationJob.getPreviousStep())
    }

    @Test
    void testMaybeSubmit_ConfiguredNotToRun_ResultsExist() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult_DeepAnnotation }
        executionService.metaClass.sendScript = { Realm realm, String text, String jobIdentifier, String qsubParameters ->
            throw new RuntimeException("This area should not be reached since the deep annotation job shall not run")
        }
        assertEquals(NextAction.SUCCEED, snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2))
    }

    @Test
    void testMaybeSubmit_ConfiguredNotToRun_NoResultsExist() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvDeepAnnotationJob.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        shouldFail(RuntimeException, { snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2) })
    }

    @Test
    void testMaybeSubmit() {
        testData.createProcessingOptions()
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        SnvCallingStep step = SnvCallingStep.SNV_DEEPANNOTATION
        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }

        snvDeepAnnotationJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult -> }
        snvDeepAnnotationJob.metaClass.writeConfigFile = { SnvCallingInstance instance ->
            return testData.createConfigFileWithContentInFileSystem(
                    snvCallingInstance2.configFilePath.absoluteDataManagementPath,
                    snvCallingInstance2.config.configuration)
        }

        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->

            File snvFile = new OtpPath(snvCallingInstance2.snvInstancePath, step.getResultFileName(snvCallingInstance2.individual)).absoluteDataManagementPath
            File md5sumFile = createMD5SUMFile(snvCallingInstance2, SnvCallingStep.SNV_DEEPANNOTATION)

            String scriptCommandPart = "# BEGIN ORIGINAL SCRIPT\n" +
                    "cp ${snvJobResult_Annotation.getResultFilePath().absoluteDataManagementPath} ${snvFile};"
                    "/tmp/scriptLocation/deepAnnotation.sh; " +
                    "md5sum ${snvFile} > ${md5sumFile}"

            String qsubParameterCommandPart = "-v CONFIG_FILE=" +
                    "${snvCallingInstance2.configFilePath.absoluteDataManagementPath}," +
                    "pid=${snvCallingInstance2.individual.pid}," +
                    "PID=${snvCallingInstance2.individual.pid}," +
                    "TOOL_ID=snvDeepAnnotation," +
                    "PIPENAME=SNV_DEEPANNOTATION," +
                    "FILENAME_VCF=${snvFile}," +
                    "FILENAME_VCF_SNVS=${snvFile}," +
                    "FILENAME_CHECKPOINT=${step.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath},"


            assert command.contains(scriptCommandPart)
            assert command.contains(qsubParameterCommandPart)

            createResultFile(snvCallingInstance2, SnvCallingStep.SNV_DEEPANNOTATION)

            return [PBS_ID]
        }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep snvCallingStep -> return snvJobResult_Annotation }

        createResultFile(snvCallingInstance2, SnvCallingStep.SNV_ANNOTATION)

        schedulerService.startingJobExecutionOnCurrentThread(snvDeepAnnotationJob)

        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2))

            assert !SnvCallingStep.SNV_DEEPANNOTATION.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath.exists()

            File configFile = snvCallingInstance2.configFilePath.absoluteDataManagementPath
            assert configFile.exists()
            AbstractSnvCallingJob.assertDataManagementConfigContentsOk(snvCallingInstance2)

            File annotationFile = new OtpPath(snvCallingInstance2.snvInstancePath, SnvCallingStep.SNV_ANNOTATION.getResultFileName(snvCallingInstance2.individual)).absoluteDataManagementPath
            File deepAnnotationFile = new OtpPath(snvCallingInstance2.snvInstancePath, step.getResultFileName(snvCallingInstance2.individual)).absoluteDataManagementPath
            assert annotationFile.text == deepAnnotationFile.text
        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(snvDeepAnnotationJob)
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testMaybeSubmit_InputFileNotReadable() {
        SnvCallingStep step = SnvCallingStep.SNV_DEEPANNOTATION
        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }

        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep snvCallingStep -> return snvJobResult_Annotation }

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
            throw new AssertionError("Not readable")
        }

        assert shouldFail(AssertionError, { snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2)} ).contains("Not readable")
    }

    @Test
    void testValidate() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)

        File checkpointFile = SnvCallingStep.SNV_DEEPANNOTATION.getCheckpointFilePath(snvCallingInstance1).absoluteDataManagementPath
        checkpointFile.createNewFile()
        createResultFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)
        createMD5SUMFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        snvDeepAnnotationJob.metaClass.deleteResultFileIfExists = { File resultFile, Realm realm ->
            resultFile.delete()
        }

        WaitingFileUtils.metaClass.static.waitUntilDoesNotExist = { File file -> return true }

        executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert command == "rm ${checkpointFile.path}"
            checkpointFile.delete()
        }

        assert checkpointFile.exists()
        snvDeepAnnotationJob.validate(snvCallingInstance1)
        assert snvJobResult_DeepAnnotation.processingState == SnvProcessingStates.FINISHED
        assert !checkpointFile.exists()

        assert configFile.exists()
        AbstractSnvCallingJob.assertDataManagementConfigContentsOk(snvCallingInstance1)
    }

    @Test
    void testValidate_CheckpointFileDoesNotExists() {
        createResultFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)
        testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        shouldFail(AssertionError, {snvDeepAnnotationJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_ConfigFileDoesNotExists() {
        createResultFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)

        shouldFail(FileNotFoundException, {snvDeepAnnotationJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_WrongConfigurationInConfigFile() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, "wrong configuration")
        shouldFail(AssertionError, {snvDeepAnnotationJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_InputFileNotReadable() {
        SnvCallingStep step = SnvCallingStep.SNV_DEEPANNOTATION
        testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)
        createResultFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
            if (file.path.contains("_annotation")) {
                throw new AssertionError("Not readable")
            }
        }

        File checkpointFile = new OtpPath(snvCallingInstance1.snvInstancePath, step.checkpointFileName).absoluteDataManagementPath
        checkpointFile.createNewFile()

        snvDeepAnnotationJob.metaClass.deleteResultFileIfExists = { File resultFile, Realm realm ->
            resultFile.delete()
        }

        WaitingFileUtils.metaClass.static.waitUntilDoesNotExist = { File file -> return true }

        shouldFail(RuntimeException, { snvDeepAnnotationJob.validate(snvCallingInstance1) })
    }

    @Test
    void testValidate_ResultFileNotReadable() {
        testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)

        createResultFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
            if (!file.path.contains("_annotation")) {
                throw new AssertionError("Not readable")
            }
        }

        assert shouldFail(AssertionError, { snvDeepAnnotationJob.validate(snvCallingInstance1) }).contains("Not readable")
    }
}
