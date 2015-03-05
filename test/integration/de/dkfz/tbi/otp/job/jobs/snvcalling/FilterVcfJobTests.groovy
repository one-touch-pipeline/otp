package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.LinkFileUtils
import de.dkfz.tbi.otp.utils.WaitingFileUtils

import static de.dkfz.tbi.TestCase.removeMetaClass
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static org.junit.Assert.*
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript

class FilterVcfJobTests extends GroovyTestCase {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ExecutionService executionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    LinkFileUtils linkFileUtils

    File testDirectory
    FilterVcfJob filterVcfJob
    SnvCallingInstance snvCallingInstance1
    SnvCallingInstance snvCallingInstance2
    SnvCallingInstanceTestData testData
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
    String UNIQUE_PATH = HelperUtils.getUniqueString()

    @Before
    void setUp() {
        testDirectory = new File("/tmp/otp-test/${UNIQUE_PATH}")
        assert testDirectory.mkdirs()

        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()

        Realm realm_processing = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: '${testDirectory}/processing',
            stagingRootPath : "${testDirectory}/staging"
        ])
        assert realm_processing.save()

        Realm realm_management = DomainFactory.createRealmDataManagementDKFZ([
            rootPath: "${testDirectory}/root",
            processingRootPath: "${testDirectory}/processing",
            stagingRootPath: null
        ])
        assert realm_management.save()

        processedMergedBamFile1 = testData.bamFileTumor
        processedMergedBamFile2 = testData.bamFileControl

        sampleType1 = processedMergedBamFile1.sample.sampleType
        sampleType1.name = "sampletype1"
        assert sampleType1.save(flush: true)
        sampleType2 = processedMergedBamFile2.sample.sampleType
        sampleType2.name = "sampletype2"
        assert sampleType2.save(flush: true)

        SnvConfig snvConfig = testData.snvConfig
        snvConfig.configuration = CONFIGURATION
        assert snvConfig.save()

        snvCallingInstance1 = testData.createSnvCallingInstance([
            sampleType1BamFile: processedMergedBamFile1,
            sampleType2BamFile: processedMergedBamFile2,
            instanceName: SOME_INSTANCE_NAME
        ])
        assert snvCallingInstance1.save()

        snvCallingInstance2 = testData.createSnvCallingInstance([
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
                processingState: SnvProcessingStates.FINISHED,
                chromosomeJoinExternalScript: testData.externalScript_Joining,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                fileSize: 235l,
                )
        assert snvJobResultCalling1.save()

        SnvJobResult snvJobResultAnnotation1 = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_Annotation,
                inputResult: snvJobResultCalling1,
                processingState: SnvProcessingStates.FINISHED
                )
        assert snvJobResultAnnotation1.save()

        SnvJobResult snvJobResultDeepAnnotation1 = new SnvJobResult(
                step: SnvCallingStep.SNV_DEEPANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_DeepAnnotation,
                inputResult: snvJobResultAnnotation1,
                processingState: SnvProcessingStates.FINISHED,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                fileSize: 235l,
                )
        assert snvJobResultDeepAnnotation1.save()

        snvJobResultFilter1 = new SnvJobResult(
                step: SnvCallingStep.FILTER_VCF,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_Filter,
                inputResult: snvJobResultDeepAnnotation1,
                processingState: SnvProcessingStates.FINISHED
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
                DomainFactory.createAndSaveProcessingStep(FilterVcfJob.toString()), [])
        filterVcfJob.log = log

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
        testData = null
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
        removeMetaClass(LinkFileUtils, linkFileUtils)
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
    void testMaybeSubmit_ConfiguredNotToRun_ResultsExist() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=0
CHROMOSOME_INDICES=( {1..21} XY)
"""
        filterVcfJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResultFilter1 }
        executionService.metaClass.sendScript = { Realm realm, String text, String jobIdentifier, String qsubParameters ->
            throw new RuntimeException("This area should not be reached since the filter job shall not run")
        }
        assertEquals(NextAction.SUCCEED, filterVcfJob.maybeSubmit(snvCallingInstance2))
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
        filterVcfJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        shouldFail(RuntimeException, { filterVcfJob.maybeSubmit(snvCallingInstance2) })
    }

    @Test
    void testMaybeSubmit_InputFileExists() {
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        filterVcfJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        filterVcfJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult ->
            return true
        }

        File pmbf1 = testData.createBamFile(processedMergedBamFile1)
        testData.createBamFile(processedMergedBamFile2)

        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->
            SnvJobResult inputResult = snvCallingInstance2.findLatestResultForSameBamFiles(SnvCallingStep.SNV_DEEPANNOTATION)
            File inputResultFile = inputResult.resultFilePath.absoluteDataManagementPath

                String scriptCommandPart = "# BEGIN ORIGINAL SCRIPT\n" +
                        "ln -s ${inputResultFile} ${snvCallingInstance2.snvInstancePath.absoluteDataManagementPath}/${inputResultFile.name}; "
                        "/tmp/scriptLocation/filter.sh; " +
                        "rm -f ${snvCallingInstance2.snvInstancePath.absoluteDataManagementPath}" +
                        "# END ORIGINAL SCRIPT"

                String qsubParameterCommandPart = "-v CONFIG_FILE=" +
                        "${snvCallingInstance2.configFilePath.absoluteDataManagementPath}," +
                        "pid=654321," +
                        "PID=654321," +
                        "TOOL_ID=snvFilter," +
                        "SNVFILE_PREFIX=snvs_," +
                        "TUMOR_BAMFILE_FULLPATH_BP=${pmbf1.absolutePath}," +
                        "FILENAME_VCF=${new OtpPath(snvCallingInstance2.snvInstancePath, SnvCallingStep.SNV_DEEPANNOTATION.getResultFileName(snvCallingInstance1.individual)).absoluteDataManagementPath}," +
                        "FILENAME_CHECKPOINT=${SnvCallingStep.FILTER_VCF.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath}"

                assert command.contains(scriptCommandPart)
                assert command.contains(qsubParameterCommandPart)

            return [PBS_ID]
        }

        filterVcfJob.metaClass.deleteResultFileIfExists = { File resultFile, Realm realm ->
            resultFile.delete()
        }

        filterVcfJob.metaClass.writeConfigFile = { SnvCallingInstance instance ->
            return testData.createConfigFileWithContentInFileSystem(
                    snvCallingInstance2.configFilePath.absoluteDataManagementPath,
                    snvCallingInstance2.config.configuration)
        }

        filterVcfJob.metaClass.linkResultFiles = { SnvCallingInstance instance -> }

        testData.createInputResultFile_Production(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)
        testData.createInputResultFile_Production(snvCallingInstance2, SnvCallingStep.CALLING)

        schedulerService.startingJobExecutionOnCurrentThread(filterVcfJob)
        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, filterVcfJob.maybeSubmit(snvCallingInstance2))

            File configFile = snvCallingInstance2.configFilePath.absoluteDataManagementPath
            assert configFile.exists()
            AbstractSnvCallingJob.assertDataManagementConfigContentsOk(snvCallingInstance2)

            assert !SnvCallingStep.FILTER_VCF.getCheckpointFilePath(snvCallingInstance2).absoluteDataManagementPath.exists()

        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(filterVcfJob)
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
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
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} XY)
"""
        File configFile = testData.createConfigFileWithContentInFileSystem(
                snvCallingInstance2.configFilePath.absoluteDataManagementPath,
                snvCallingInstance2.config.configuration)

        testData.createInputResultFile_Production(snvCallingInstance2, SnvCallingStep.SNV_DEEPANNOTATION)

        File checkpointFile = new OtpPath(snvCallingInstance2.snvInstancePath, SnvCallingStep.FILTER_VCF.checkpointFileName).absoluteDataManagementPath
        checkpointFile.createNewFile()

        executionService.metaClass.executeCommand = { Realm realm, String command -> }

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        WaitingFileUtils.metaClass.static.confirmDoesNotExist = { File file -> return true }

        try {
            filterVcfJob.validate(snvCallingInstance2)
        } finally {
            configFile.parentFile.deleteDir()
            checkpointFile.delete()
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testValidate_FilterCheckpointFileDoesNotExists() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        shouldFail(AssertionError, {filterVcfJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_FilterConfigFileDoesNotExists() {
        shouldFail(FileNotFoundException, {filterVcfJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_WrongConfigurationInFilterConfigFile() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, "wrong configuration")
        shouldFail(AssertionError, {filterVcfJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_InputFileNotReadable() {
        SnvCallingStep step = SnvCallingStep.FILTER_VCF
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteDataManagementPath, CONFIGURATION)
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
                throw new AssertionError("Not readable")
        }

        WaitingFileUtils.metaClass.static.confirmDoesNotExist = { File file -> return true }

        File checkpointFile = new OtpPath(snvCallingInstance1.snvInstancePath, step.checkpointFileName).absoluteDataManagementPath
        checkpointFile.createNewFile()

        shouldFail(RuntimeException, { filterVcfJob.validate(snvCallingInstance1) })
    }
}
