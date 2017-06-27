package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.job.processing.ClusterJobLoggingService
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import org.apache.commons.logging.impl.NoOpLog

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
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
import static org.junit.Assert.assertEquals

class FilterVcfJobTests {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ExecutionService executionService

    @Autowired
    ClusterJobSchedulerService clusterJobSchedulerService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    SnvCallingInstanceTestData snvCallingInstanceTestData

    File testDirectory
    FilterVcfJob filterVcfJob
    SnvCallingInstance snvCallingInstance1
    SnvCallingInstance snvCallingInstance2
    SnvJobResult snvJobResultFilter1
    ProcessedMergedBamFile processedMergedBamFile1
    ProcessedMergedBamFile processedMergedBamFile2
    SampleType sampleType1
    SampleType sampleType2

    public static final String SOME_INSTANCE_NAME = "2014-08-25_15h32"
    public static final String OTHER_INSTANCE_NAME = "2014-09-01_15h32"

    final String CONFIGURATION ="""
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

        snvCallingInstanceTestData.createSnvObjects(testDirectory)

        processedMergedBamFile1 = snvCallingInstanceTestData.bamFileTumor
        processedMergedBamFile2 = snvCallingInstanceTestData.bamFileControl

        sampleType1 = processedMergedBamFile1.sample.sampleType
        sampleType2 = processedMergedBamFile2.sample.sampleType

        SnvConfig snvConfig = snvCallingInstanceTestData.createSnvConfig(CONFIGURATION)

        snvCallingInstance1 = snvCallingInstanceTestData.createSnvCallingInstance([
            sampleType1BamFile: processedMergedBamFile1,
            sampleType2BamFile: processedMergedBamFile2,
            instanceName: SOME_INSTANCE_NAME,
            processingState: AnalysisProcessingStates.FINISHED,
        ])
        assert snvCallingInstance1.save()

        snvCallingInstance2 = snvCallingInstanceTestData.createSnvCallingInstance([
            sampleType1BamFile: processedMergedBamFile1,
            sampleType2BamFile: processedMergedBamFile2,
            instanceName: OTHER_INSTANCE_NAME
        ])
        assert snvCallingInstance2.save()

        ExternalScript externalScript_Calling = new ExternalScript(
                scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/calling.sh",
                author: "otptest",
                )
        assert externalScript_Calling.save()

        ExternalScript externalScript_Annotation = new ExternalScript(
                scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/annotation.sh",
                author: "otptest",
                )
        assert externalScript_Annotation.save()

        ExternalScript externalScript_DeepAnnotation = new ExternalScript(
                scriptIdentifier: SnvCallingStep.SNV_DEEPANNOTATION.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/deepAnnotation.sh",
                author: "otptest",
                )
        assert externalScript_DeepAnnotation.save()

        ExternalScript externalScript_Filter = new ExternalScript(
                scriptIdentifier: SnvCallingStep.FILTER_VCF.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/filter.sh",
                author: "otptest",
                )
        assert externalScript_Filter.save()

        SnvJobResult snvJobResultCalling1 = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_Calling,
                processingState: AnalysisProcessingStates.FINISHED,
                chromosomeJoinExternalScript: snvCallingInstanceTestData.externalScript_Joining,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                fileSize: 235l,
                )
        assert snvJobResultCalling1.save()

        SnvJobResult snvJobResultAnnotation1 = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_Annotation,
                inputResult: snvJobResultCalling1,
                processingState: AnalysisProcessingStates.FINISHED
                )
        assert snvJobResultAnnotation1.save()

        SnvJobResult snvJobResultDeepAnnotation1 = new SnvJobResult(
                step: SnvCallingStep.SNV_DEEPANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_DeepAnnotation,
                inputResult: snvJobResultAnnotation1,
                processingState: AnalysisProcessingStates.FINISHED,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                fileSize: 235l,
                )
        assert snvJobResultDeepAnnotation1.save()

        snvJobResultFilter1 = new SnvJobResult(
                step: SnvCallingStep.FILTER_VCF,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_Filter,
                inputResult: snvJobResultDeepAnnotation1,
                processingState: AnalysisProcessingStates.FINISHED
                )
        assert snvJobResultFilter1.save()

        SnvJobResult snvJobResultFilter2 = new SnvJobResult(
                step: SnvCallingStep.FILTER_VCF,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Filter,
                inputResult: snvJobResultDeepAnnotation1
                )
        assert snvJobResultFilter2.save()

        filterVcfJob = applicationContext.getBean('filterVcfJob',
                DomainFactory.createAndSaveProcessingStep(FilterVcfJob.toString(), snvCallingInstance2), [])
        filterVcfJob.log = new NoOpLog()

        ParameterType typeRealm = new ParameterType(
                name: REALM,
                className: "${FilterVcfJob.class}",
                jobDefinition: filterVcfJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeRealm.save()

        ParameterType typeScript = new ParameterType(
                name: SCRIPT,
                className: "${FilterVcfJob.class}",
                jobDefinition: filterVcfJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeScript.save()
    }

    @After
    void tearDown() {
        snvCallingInstanceTestData = null
        snvCallingInstance1 = null
        snvCallingInstance2 = null
        snvJobResultFilter1 = null
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
        sampleType1 = null
        sampleType2 = null
        LsdfFilesService.metaClass = null
        WaitingFileUtils.metaClass = null
        assert testDirectory.deleteDir()
        removeMetaClass(ExecutionService, executionService)
        removeMetaClass(ClusterJobSchedulerService, clusterJobSchedulerService)
        removeMetaClass(LinkFileUtils, linkFileUtils)
        removeMetaClass(ClusterJobLoggingService, clusterJobSchedulerService.clusterJobLoggingService)
    }


    @Test
    void testGetStep() {
        assertEquals(SnvCallingStep.FILTER_VCF, filterVcfJob.getStep())
    }

    @Test
    void testGetPreviousStep() {
        assertEquals(SnvCallingStep.SNV_DEEPANNOTATION, filterVcfJob.getPreviousStep())
    }

    @Test
    void testMaybeSubmit_ConfiguredNotToRun_NoPreviousResultsAvailable() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=0
CHROMOSOME_INDICES=( {1..21} XY)
"""
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        shouldFail(RuntimeException, { filterVcfJob.maybeSubmit(snvCallingInstance2) })
    }

    @Test
    void testMaybeSubmit_InputFileExists() {
        TestCase.mockCreateDirectory(lsdfFilesService)
        filterVcfJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult ->
            return true
        }

        clusterJobSchedulerService.clusterJobLoggingService.metaClass.createAndGetLogDirectory = { Realm realm, ProcessingStep processingStep ->
            return TestCase.uniqueNonExistentPath
        }

        File pmbf1 = snvCallingInstanceTestData.createBamFile(processedMergedBamFile1)
        snvCallingInstanceTestData.createBamFile(processedMergedBamFile2)

        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, File keyFile, boolean useSshAgent, String command ->
            SnvJobResult inputResult = snvCallingInstance2.findLatestResultForSameBamFiles(SnvCallingStep.SNV_DEEPANNOTATION)
            File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath

                String scriptCommandPart = "# BEGIN ORIGINAL SCRIPT\n" +
                        "ln -sf ${inputResultFile} ${snvCallingInstance2.instancePath.absoluteDataManagementPath}/${inputResultFile.name}; "
                        "/tmp/scriptLocation/filter.sh; " +
                        "rm -f ${snvCallingInstance2.instancePath.absoluteDataManagementPath}" +
                        "# END ORIGINAL SCRIPT"

                String qsubParameterCommandPart = "-v CONFIG_FILE=" +
                        "${snvCallingInstance2.configFilePath.absoluteDataManagementPath}," +
                        "pid=${snvCallingInstance2.individual.pid}," +
                        "PID=${snvCallingInstance2.individual.pid}," +
                        "TOOL_ID=snvFilter," +
                        "SNVFILE_PREFIX=snvs_," +
                        "TUMOR_BAMFILE_FULLPATH_BP=${pmbf1.absolutePath}," +
                        "FILENAME_VCF=${new OtpPath(snvCallingInstance2.instancePath, SnvCallingStep.SNV_DEEPANNOTATION.getResultFileName(snvCallingInstance1.individual)).absoluteDataManagementPath}," +
                        "FILENAME_CHECKPOINT=${SnvCallingStep.FILTER_VCF.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath}"

            if (!command.startsWith("qrls")) {
                assert command.contains(scriptCommandPart)
                assert command.contains(qsubParameterCommandPart)
            }

            return new ProcessOutput("${PBS_ID}.pbs", "", 0)
        }

        filterVcfJob.metaClass.deleteResultFileIfExists = { File resultFile, Realm realm ->
            resultFile.delete()
        }

        filterVcfJob.metaClass.writeConfigFile = { SnvCallingInstance instance ->
            return snvCallingInstanceTestData.createConfigFileWithContentInFileSystem(
                    snvCallingInstance2.configFilePath.absoluteDataManagementPath,
                    snvCallingInstance2.config.configuration)
        }

        snvCallingInstanceTestData.createInputResultFile_Production(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)
        snvCallingInstanceTestData.createInputResultFile_Production(snvCallingInstance2, SnvCallingStep.CALLING)

        schedulerService.startingJobExecutionOnCurrentThread(filterVcfJob)
        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, filterVcfJob.maybeSubmit(snvCallingInstance2))

            File configFile = snvCallingInstance2.configFilePath.absoluteDataManagementPath
            assert configFile.exists()
            AbstractSnvCallingJob.assertDataManagementConfigContentsOk(snvCallingInstance2)

            assert !SnvCallingStep.FILTER_VCF.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath.exists()

        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(filterVcfJob)
            TestCase.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testMaybeSubmit_InputFileDoesNotExistInDB() {
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        shouldFail(AssertionError, { filterVcfJob.maybeSubmit(snvCallingInstance2) })
    }

    @Test
    void testMaybeSubmit_InputFileDoesNotExistInFilesystem() {
        shouldFail(AssertionError, { filterVcfJob.maybeSubmit(snvCallingInstance2) })
    }

    @Test
    void testValidate() {
        TestCase.mockCreateDirectory(lsdfFilesService)
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} XY)
"""
        File configFile = snvCallingInstanceTestData.createConfigFileWithContentInFileSystem(
                snvCallingInstance2.configFilePath.absoluteDataManagementPath,
                snvCallingInstance2.config.configuration)

        snvCallingInstanceTestData.createInputResultFile_Production(snvCallingInstance2, SnvCallingStep.SNV_DEEPANNOTATION)

        File checkpointFile = new OtpPath(snvCallingInstance2.instancePath, SnvCallingStep.FILTER_VCF.checkpointFileName).absoluteDataManagementPath
        checkpointFile.createNewFile()

        executionService.metaClass.executeCommand = { Realm realm, String command -> }

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        WaitingFileUtils.metaClass.static.waitUntilDoesNotExist = { File file -> }

        try {
            filterVcfJob.validate(snvCallingInstance2)
        } finally {
            configFile.parentFile.deleteDir()
            checkpointFile.delete()
            TestCase.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testValidate_FilterCheckpointFileDoesNotExists() {
        File configFile = snvCallingInstanceTestData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        shouldFail(AssertionError, {filterVcfJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_FilterConfigFileDoesNotExists() {
        shouldFail(FileNotFoundException, {filterVcfJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_WrongConfigurationInFilterConfigFile() {
        File configFile = snvCallingInstanceTestData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, "wrong configuration")
        shouldFail(AssertionError, {filterVcfJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_InputFileNotReadable() {
        SnvCallingStep step = SnvCallingStep.FILTER_VCF
        File configFile = snvCallingInstanceTestData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)

        WaitingFileUtils.metaClass.static.waitUntilDoesNotExist = { File file -> }

        File checkpointFile = new OtpPath(snvCallingInstance1.instancePath, step.checkpointFileName).absoluteDataManagementPath
        checkpointFile.createNewFile()

        shouldFail(RuntimeException, { filterVcfJob.validate(snvCallingInstance1) })
    }
}
