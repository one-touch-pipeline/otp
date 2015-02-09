package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.TestCase.*
import static org.junit.Assert.*
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript

class SnvAnnotationJobTests extends GroovyTestCase {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ExecutionService executionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    LsdfFilesService lsdfFilesService

    File testDirectory
    SnvAnnotationJob snvAnnotationJob
    SnvCallingInstance snvCallingInstance
    SnvCallingInstance snvCallingInstance2
    SnvJobResult snvJobResult
    SnvJobResult snvJobResultInput
    ProcessedMergedBamFile processedMergedBamFile1
    SnvCallingInstanceTestData testData

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

    @Before
    void setUp() {
        testDirectory = TestCase.createEmptyTestDirectory()

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
        ProcessedMergedBamFile processedMergedBamFile2 = testData.bamFileControl

        SnvConfig snvConfig = testData.snvConfig
        snvConfig.configuration = CONFIGURATION
        assert snvConfig.save()

        snvCallingInstance = testData.createSnvCallingInstance([
            sampleType1BamFile: processedMergedBamFile1,
            sampleType2BamFile: processedMergedBamFile2,
            instanceName: SOME_INSTANCE_NAME
        ])
        assert snvCallingInstance.save()

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

        snvJobResultInput = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                externalScript: externalScript_Calling,
                processingState: SnvProcessingStates.FINISHED,
                chromosomeJoinExternalScript: testData.externalScript_Joining,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                fileSize: 1234l,
                )
        assert snvJobResultInput.save()


        ExternalScript externalScript_Annotation = new ExternalScript(
                scriptIdentifier: SnvCallingStep.SNV_ANNOTATION.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/annotation.sh",
                author: "otptest",
                )
        assert externalScript_Annotation.save()

        snvJobResult = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Annotation,
                inputResult: snvJobResultInput
                )
        assert snvJobResult.save()

        snvAnnotationJob = applicationContext.getBean('snvAnnotationJob',
                DomainFactory.createAndSaveProcessingStep(SnvAnnotationJob.toString()), [])
        snvAnnotationJob.log = log
    }

    @After
    void tearDown() {
        testData = null
        snvJobResult = null
        snvCallingInstance = null
        snvCallingInstance2 = null
        snvJobResultInput = null
        processedMergedBamFile1 = null
        removeMetaClass(ExecutionService, executionService)
        LsdfFilesService.metaClass = null
        assert testDirectory.deleteDir()
    }

    @Test
    void testGetStep() {
        assertEquals(SnvCallingStep.SNV_ANNOTATION, snvAnnotationJob.getStep())
    }


    @Test
    void testMaybeSubmit_ConfiguredNotToRun_ResultsExist() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} XY)
"""

        snvAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvAnnotationJob.metaClass.addOutputParameter = { String name, String value -> }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult }
        snvAnnotationJob.log = log
        assertEquals(NextAction.SUCCEED, snvAnnotationJob.maybeSubmit(snvCallingInstance2))
    }


    @Test
    void testMaybeSubmit_ConfiguredNotToRun_NoResultsExist() {
        snvCallingInstance2.config.configuration = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} XY)
"""

        snvAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvAnnotationJob.metaClass.addOutputParameter = { String name, String value -> }
        snvAnnotationJob.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        assert (
        shouldFail(RuntimeException, { snvAnnotationJob.maybeSubmit(snvCallingInstance2) })
        ==
        "This SNV workflow instance is configured not to do the SNV SNV_ANNOTATION and no non-withdrawn SNV SNV_ANNOTATION was done before, so subsequent jobs will have no input."
        )
    }

    @Test
    void testMaybeSubmit() {
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        snvAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvAnnotationJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult -> }
        snvAnnotationJob.metaClass.getExistingBamFilePath = {ProcessedMergedBamFile bamFile ->
            return new File(processedMergedBamFileService.destinationDirectory(processedMergedBamFile1), processedMergedBamFileService.fileName(processedMergedBamFile1))
        }
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->
            return [PBS_ID]
        }
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResultInput }
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> return true }

        schedulerService.startingJobExecutionOnCurrentThread(snvAnnotationJob)

        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, snvAnnotationJob.maybeSubmit(snvCallingInstance2))
        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(snvAnnotationJob)
        }
    }

    @Test
    void testValidate() {
        File configFile = testData.createConfigFileWithContentInFileSystem(
            snvCallingInstance2.configFilePath.absoluteStagingPath,
            CONFIGURATION)

        File checkpointFile = new OtpPath(snvCallingInstance2.snvInstancePath, SnvCallingStep.SNV_ANNOTATION.checkpointFileName).absoluteStagingPath
        checkpointFile.createNewFile()

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }
        snvAnnotationJob.metaClass.getExistingBamFilePath = {ProcessedMergedBamFile bamFile ->
            return new File(processedMergedBamFileService.destinationDirectory(processedMergedBamFile1), processedMergedBamFileService.fileName(processedMergedBamFile1))
        }
        try {
            assertNull(snvAnnotationJob.validate(snvCallingInstance2))
        } finally {
            configFile.parentFile.deleteDir()
            checkpointFile.delete()
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testValidate_FileNotReadable() {
        File configFile = testData.createConfigFileWithContentInFileSystem(
            snvCallingInstance2.configFilePath.absoluteStagingPath,
            CONFIGURATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> throw new AssertionError("Not readable") }
        snvAnnotationJob.metaClass.getExistingBamFilePath = {ProcessedMergedBamFile bamFile ->
            return new File(processedMergedBamFileService.destinationDirectory(processedMergedBamFile1), processedMergedBamFileService.fileName(processedMergedBamFile1))
        }
        try {
            assert shouldFail(AssertionError, { snvAnnotationJob.validate(snvCallingInstance2) }).contains("Not readable")
        } finally {
            configFile.parentFile.deleteDir()
        }
    }
}
