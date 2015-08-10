package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.LinkFileUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.TestCase.removeMetaClass
import static de.dkfz.tbi.otp.utils.CreateSNVFileHelper.createMD5SUMFile
import static de.dkfz.tbi.otp.utils.CreateSNVFileHelper.createResultFile
import static org.junit.Assert.assertEquals

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

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    LinkFileUtils linkFileUtils

    File testDirectory
    Realm realm_processing
    Project project
    SeqType seqType
    Individual individual
    SamplePair samplePair
    SnvCallingInstance snvCallingInstance
    SnvCallingInstance snvCallingInstance2
    ExternalScript externalScript_Calling
    ExternalScript externalScript_Joining
    SnvJobResult snvJobResult
    SnvCallingJob snvCallingJob
    SnvCallingInstanceTestData testData
    ProcessedMergedBamFile processedMergedBamFile1
    ProcessedMergedBamFile processedMergedBamFile2

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
        tmpDir.create()
        testDirectory = tmpDir.newFolder("/otp-test")
        if(!testDirectory.exists()) {
            assert testDirectory.mkdirs()
        }

        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects(testDirectory)
        realm_processing = testData.realmProcessing

        samplePair = testData.samplePair
        project = samplePair.project
        individual = samplePair.individual
        seqType = samplePair.seqType

        processedMergedBamFile1 = testData.bamFileTumor
        processedMergedBamFile2 = testData.bamFileControl

        externalScript_Calling = new ExternalScript(
                scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/calling.sh",
                author: "otptest",
        )
        assert externalScript_Calling.save()

        SnvConfig snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION,
                externalScriptVersion: "v1",
        )
        assert snvConfig.save()

        snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: SOME_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance.save()

        snvCallingInstance2 = DomainFactory.createSnvCallingInstance(
                instanceName: OTHER_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance2.save()

        externalScript_Joining = testData.externalScript_Joining

        snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                externalScript: externalScript_Calling,
                chromosomeJoinExternalScript: externalScript_Joining,
                fileSize: 1234l,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                )
        assert snvJobResult.save()

        snvCallingJob = applicationContext.getBean('snvCallingJob',
            DomainFactory.createAndSaveProcessingStep(SnvCallingJob.toString()), [])
        snvCallingJob.log = log
    }

    @After
    void tearDown() {
        testData = null
        individual = null
        project = null
        seqType = null
        samplePair = null
        snvCallingInstance = null
        realm_processing = null
        snvCallingInstance2 = null
        externalScript_Calling = null
        externalScript_Joining = null
        snvJobResult = null
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
        removeMetaClass(CreateClusterScriptService, createClusterScriptService)
        removeMetaClass(ExecutionService, executionService)
        removeMetaClass(SnvCallingJob, snvCallingJob)
        removeMetaClass(LinkFileUtils, linkFileUtils)
        removeMetaClass(LsdfFilesService, lsdfFilesService)
        assert testDirectory.deleteDir()

        TestCase.cleanTestDirectory()
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
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        executionService.metaClass.sendScript = { Realm realm, String text, String jobIdentifier, String qsubParameters ->
            throw new RuntimeException("This area should not be reached since the calling job shall not run")
        }
        shouldFail(RuntimeException, { snvCallingJob.maybeSubmit(snvCallingInstance) })
    }

    @Test
    void testMaybeSubmitWithSnvCallingInput() {
        testData.createProcessingOptions()
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        snvJobResult.delete()
        snvCallingJob.metaClass.getProcessParameterObject = { return snvCallingInstance }
        snvCallingJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult ->
            return true
        }
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->
            if (command.contains('PARM_CHR_INDEX=')) {
                String chromosome = command.split('PARM_CHR_INDEX=')[1].split(',')[0]
                File snvFile = new OtpPath(snvCallingInstance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(snvCallingInstance.individual, chromosome)).absoluteStagingPath
                File tumorBamFile = new File(AbstractMergedBamFileService.destinationDirectory(processedMergedBamFile1), processedMergedBamFile1.getBamFileName())
                File controlBamFile = new File(AbstractMergedBamFileService.destinationDirectory(processedMergedBamFile2), processedMergedBamFile2.getBamFileName())

                String scriptCommandPart = "/tmp/scriptLocation/calling.sh"

                String qsubParameterCommandPart = "-v CONFIG_FILE=" +
                        "${snvCallingInstance.configFilePath.absoluteDataManagementPath}," +
                        "pid=${snvCallingInstance.individual.pid}," +
                        "PID=${snvCallingInstance.individual.pid}," +
                        "TUMOR_BAMFILE_FULLPATH_BP=${tumorBamFile}," +
                        "CONTROL_BAMFILE_FULLPATH_BP=${controlBamFile}," +
                        "TOOL_ID=snvCalling," +
                        "PARM_CHR_INDEX=${chromosome}," +
                        "FILENAME_VCF_SNVS=${snvFile}"

                assert command.contains(scriptCommandPart)
                assert command.contains(qsubParameterCommandPart)

            } else {
                File snvFile = new OtpPath(snvCallingInstance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(snvCallingInstance.individual, null)).absoluteDataManagementPath
                String scriptCommandPart = "/tmp/scriptLocation/joining.sh; " +
                        "md5sum ${snvFile} > ${snvFile}.md5sum"
                assert command.contains(scriptCommandPart)
            }

            return [PBS_ID]
        }

        snvCallingJob.metaClass.writeConfigFile = { SnvCallingInstance instance ->
            return testData.createConfigFileWithContentInFileSystem(
                    snvCallingInstance.configFilePath.absoluteDataManagementPath,
                    snvCallingInstance.config.configuration)
        }

        createProcessedMergedBamFileOnFileSystem(snvCallingInstance.sampleType1BamFile)
        createProcessedMergedBamFileOnFileSystem(snvCallingInstance.sampleType2BamFile)

        schedulerService.startingJobExecutionOnCurrentThread(snvCallingJob)
        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, snvCallingJob.maybeSubmit(snvCallingInstance))
            List<SnvJobResult> result = SnvJobResult.findAllBySnvCallingInstance(snvCallingInstance)
            assert result.size() == 1
            assert result.first().chromosomeJoinExternalScript == externalScript_Joining

        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(snvCallingJob)
        }
    }

    @Test
    void testValidateWithSnvCallingInput() {
        File configFile = testData.createConfigFileWithContentInFileSystem(
            snvCallingInstance.configFilePath.absoluteDataManagementPath,
            CONFIGURATION)

        createResultFile(snvCallingInstance, SnvCallingStep.CALLING)
        createMD5SUMFile(snvCallingInstance, SnvCallingStep.CALLING)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> return true }

        snvCallingJob.metaClass.changeProcessingStateOfJobResult = { SnvCallingInstance instance, SnvProcessingStates newState -> }

        createProcessedMergedBamFileOnFileSystem(snvCallingInstance.sampleType1BamFile)
        createProcessedMergedBamFileOnFileSystem(snvCallingInstance.sampleType2BamFile)

        snvCallingJob.validate(snvCallingInstance)
    }


    private void createProcessedMergedBamFileOnFileSystem(ProcessedMergedBamFile bamFile) {

        final String bamFileContent = 'I am a test BAM file. Nice to meet you. :)'

        bamFile.fileSize = bamFileContent.length()
        assert bamFile.save(failOnError: true)

        final File file = new File(AbstractMergedBamFileService.destinationDirectory(bamFile), bamFile.getBamFileName())
        assert file.path.startsWith(testDirectory.path)
        file.parentFile.mkdirs()
        file.text = bamFileContent
    }
}
