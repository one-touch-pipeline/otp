package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.job.processing.ClusterJobLoggingService
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import org.apache.commons.logging.impl.NoOpLog

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.PbsService
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.LinkFileUtils
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
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

class SnvDeepAnnotationJobTests {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ExecutionService executionService

    @Autowired
    PbsService pbsService

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
    SnvJobResult snvJobResult_Annotation2
    SnvJobResult snvJobResult_DeepAnnotation1
    ProcessedMergedBamFile processedMergedBamFile1
    SnvCallingInstanceTestData testData
    ExternalScript externalScript_DeepAnnotation

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
        testDirectory = tmpDir.newFolder("otp-test")
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
            processingState: AnalysisProcessingStates.FINISHED,
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

        SnvJobResult snvJobResult_Calling1 = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_Calling,
                processingState: AnalysisProcessingStates.FINISHED,
                chromosomeJoinExternalScript: testData.externalScript_Joining,
                fileSize: 1234l,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
        )
        assert snvJobResult_Calling1.save()

        SnvJobResult snvJobResult_Annotation1 = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_Annotation,
                processingState: AnalysisProcessingStates.FINISHED,
                inputResult: snvJobResult_Calling1,
        )
        assert snvJobResult_Annotation1.save()

        SnvJobResult snvJobResult_Calling2 = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Calling,
                processingState: AnalysisProcessingStates.FINISHED,
                chromosomeJoinExternalScript: testData.externalScript_Joining,
                fileSize: 1234l,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                )
        assert snvJobResult_Calling2.save()

        snvJobResult_Annotation2 = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Annotation,
                processingState: AnalysisProcessingStates.FINISHED,
                inputResult: snvJobResult_Calling2,
                )
        assert snvJobResult_Annotation2.save()

        externalScript_DeepAnnotation = new ExternalScript(
                scriptIdentifier: SnvCallingStep.SNV_DEEPANNOTATION.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/deepAnnotation.sh",
                author: "otptest",
                )
        externalScript_DeepAnnotation.save()

        snvJobResult_DeepAnnotation1 = new SnvJobResult(
                step: SnvCallingStep.SNV_DEEPANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_DeepAnnotation,
                inputResult: snvJobResult_Annotation1,
                processingState: AnalysisProcessingStates.FINISHED,
                fileSize: 1234l,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                )
        snvJobResult_DeepAnnotation1.save()

        snvDeepAnnotationJob = applicationContext.getBean('snvDeepAnnotationJob',
                DomainFactory.createAndSaveProcessingStep(SnvDeepAnnotationJob.toString(), snvCallingInstance2), [])
        snvDeepAnnotationJob.log = new NoOpLog()

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
        // Reset meta classes
        removeMetaClass(ExecutionService, executionService)
        removeMetaClass(PbsService, pbsService)
        removeMetaClass(CreateClusterScriptService, createClusterScriptService)
        removeMetaClass(LinkFileUtils, linkFileUtils)
        removeMetaClass(ClusterJobLoggingService, pbsService.clusterJobLoggingService)

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

        linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> map, Realm realm ->
            assert map ==
                    [(new File(snvCallingInstance1.instancePath.absoluteDataManagementPath, SnvCallingStep.SNV_DEEPANNOTATION.getResultFileName(snvCallingInstance2.individual))):
                             new File(snvCallingInstance2.instancePath.absoluteDataManagementPath, SnvCallingStep.SNV_DEEPANNOTATION.getResultFileName(snvCallingInstance2.individual))]
        }
        pbsService.metaClass.executeJob = { Realm realm, String text, String qsubParameters ->
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

        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        assert (
        shouldFail(RuntimeException, { snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2) })
        ==
        "This SNV workflow instance is configured not to do the SNV SNV_DEEPANNOTATION and no non-withdrawn SNV SNV_DEEPANNOTATION was done before, so subsequent jobs will have no input."
        )
    }

    @Test
    void testMaybeSubmit() {
        TestCase.mockCreateDirectory(lsdfFilesService)
        SnvCallingStep step = SnvCallingStep.SNV_DEEPANNOTATION

        pbsService.clusterJobLoggingService.metaClass.createAndGetLogDirectory = { Realm realm, ProcessingStep processingStep ->
            return TestCase.uniqueNonExistentPath
        }
        snvDeepAnnotationJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult -> }
        snvDeepAnnotationJob.metaClass.writeConfigFile = { SnvCallingInstance instance ->
            return testData.createConfigFileWithContentInFileSystem(
                    snvCallingInstance2.configFilePath.absoluteDataManagementPath,
                    snvCallingInstance2.config.configuration)
        }

        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, File keyFile, boolean useSshAgent, String command ->

            File snvFile = new OtpPath(snvCallingInstance2.instancePath, step.getResultFileName(snvCallingInstance2.individual)).absoluteDataManagementPath
            File md5sumFile = createMD5SUMFile(snvCallingInstance2, SnvCallingStep.SNV_DEEPANNOTATION)

            String scriptCommandPart = "# BEGIN ORIGINAL SCRIPT\n" +
                    "cp ${snvJobResult_Annotation2.getResultFilePath().absoluteDataManagementPath} ${snvFile};"
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
                    "FILENAME_CHECKPOINT=${step.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath}"


            if (!command.startsWith("qrls")) {
                assert command.contains(scriptCommandPart)
                assert command.contains(qsubParameterCommandPart)
            }

            createResultFile(snvCallingInstance2, SnvCallingStep.SNV_DEEPANNOTATION)

            return new ProcessOutput("${PBS_ID}.pbs", "", 0)
        }

        createResultFile(snvCallingInstance2, SnvCallingStep.SNV_ANNOTATION)

        schedulerService.startingJobExecutionOnCurrentThread(snvDeepAnnotationJob)

        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2))

            assert !SnvCallingStep.SNV_DEEPANNOTATION.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath.exists()

            File configFile = snvCallingInstance2.configFilePath.absoluteDataManagementPath
            assert configFile.exists()
            AbstractSnvCallingJob.assertDataManagementConfigContentsOk(snvCallingInstance2)

            File annotationFile = new OtpPath(snvCallingInstance2.instancePath, SnvCallingStep.SNV_ANNOTATION.getResultFileName(snvCallingInstance2.individual)).absoluteDataManagementPath
            File deepAnnotationFile = new OtpPath(snvCallingInstance2.instancePath, step.getResultFileName(snvCallingInstance2.individual)).absoluteDataManagementPath
            assert annotationFile.text == deepAnnotationFile.text
        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(snvDeepAnnotationJob)
            TestCase.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testMaybeSubmit_InputFileNotReadable() {
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

        snvJobResult_DeepAnnotation1.processingState = AnalysisProcessingStates.IN_PROGRESS
        assert snvJobResult_DeepAnnotation1.save(flush: true)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        snvDeepAnnotationJob.metaClass.deleteResultFileIfExists = { File resultFile, Realm realm ->
            resultFile.delete()
        }

        WaitingFileUtils.metaClass.static.waitUntilDoesNotExist = { File file -> }

        executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert command == "rm ${checkpointFile.path}"
            checkpointFile.delete()
        }

        assert checkpointFile.exists()
        snvDeepAnnotationJob.validate(snvCallingInstance1)
        assert snvJobResult_DeepAnnotation1.processingState == AnalysisProcessingStates.FINISHED
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
        testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, "wrong configuration")
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

        File checkpointFile = new OtpPath(snvCallingInstance1.instancePath, step.checkpointFileName).absoluteDataManagementPath
        checkpointFile.createNewFile()

        snvDeepAnnotationJob.metaClass.deleteResultFileIfExists = { File resultFile, Realm realm ->
            resultFile.delete()
        }

        WaitingFileUtils.metaClass.static.waitUntilDoesNotExist = { File file -> }

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
