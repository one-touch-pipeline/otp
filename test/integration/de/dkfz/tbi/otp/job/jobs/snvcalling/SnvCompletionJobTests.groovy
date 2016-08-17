package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import org.apache.commons.logging.impl.NoOpLog

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class SnvCompletionJobTests {

    @Autowired
    ApplicationContext applicationContext
    @Autowired
    ConfigService configService
    @Autowired
    ExecutionService executionService
    @Autowired
    LsdfFilesService lsdfFilesService

    File testDirectory
    Individual individual
    Project project
    Realm realm_processing
    SeqType seqType
    SnvCallingInstance snvCallingInstance
    SnvCompletionJob snvCompletionJob
    SnvCallingInstanceTestData testData
    SnvConfig snvConfig
    ProcessedMergedBamFile processedMergedBamFile1
    ProcessedMergedBamFile processedMergedBamFile2
    SamplePair samplePair

    public static final String SOME_INSTANCE_NAME = "2014-08-25_15h32"

    public final String CONFIGURATION = """
RUN_CALLING=1
RUN_SNV_ANNOTATION=1
RUN_SNV_DEEPANNOTATION=1
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""


    @Before
    void setUp() {
        testDirectory = TestCase.createEmptyTestDirectory()

        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects(testDirectory)
        realm_processing = testData.realmProcessing

        processedMergedBamFile1 = testData.bamFileTumor
        processedMergedBamFile2 = testData.bamFileControl

        samplePair = testData.samplePair
        project = samplePair.project
        seqType = samplePair.seqType

        SnvCallingInstanceTestData.createOrFindExternalScript()
        snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION,
                externalScriptVersion: "v1",
                pipeline: DomainFactory.createOtpSnvPipelineLazy(),
        )
        assert snvConfig.save()

        snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: SOME_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance.save()

        snvCompletionJob = applicationContext.getBean('snvCompletionJob')
        snvCompletionJob.processingStep = DomainFactory.createAndSaveProcessingStep(SnvCompletionJob.toString(), snvCallingInstance)
        snvCompletionJob.log = new NoOpLog()
    }

    @After
    void tearDown() {
        project = null
        snvCallingInstance = null
        realm_processing = null
        snvConfig = null
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
        samplePair = null
        // Reset meta classes
        snvCompletionJob.metaClass = null
        snvCompletionJob.linkFileUtils.metaClass = null
        TestCase.removeMetaClass(ExecutionService, executionService)


        // Clean-up file-system
        TestCase.cleanTestDirectory()
    }

    @Test
    void test_execute_WhenRunAndInstanceIsNotInProgress_ShouldFail() {
        // Given:
        snvCallingInstance.processingState = AnalysisProcessingStates.FINISHED
        // Mock deletion, so it does not get in the way of this test
        snvCompletionJob.metaClass.deleteStagingDirectory = { SnvCallingInstance instance -> }
        // When:
        shouldFail { snvCompletionJob.execute() }
    }

    @Test
    void test_execute_WhenRun_ShouldSetProcessingStateToFinished() {
        // Given:
        // Mock deletion, so it does not get in the way of this test
        snvCompletionJob.metaClass.deleteStagingDirectory = { SnvCallingInstance instance -> }

        assert snvCallingInstance.processingState == AnalysisProcessingStates.IN_PROGRESS
        // When:
        snvCompletionJob.execute()
        // Then:
        assert snvCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsClean_ShouldDeleteDirectory() {
        // Given:
        TestCase.mockDeleteDirectory(lsdfFilesService)
        TestCase.mockCreateDirectory(lsdfFilesService)

        File stagingPath = snvCallingInstance.instancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        // When:
        snvCompletionJob.execute()
        // Then:
        try {
            assert !stagingPath.exists()
            assert !stagingPath.parentFile.exists()
            assert stagingPath.parentFile.parentFile.exists()
        } finally {
            TestCase.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsDirtyContainingFile_ShouldDeleteDirectory() {
        // Given:
        TestCase.mockDeleteDirectory(lsdfFilesService)

        File stagingPath = snvCallingInstance.instancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File fileNotSupposedToBeThere = new File(snvCallingInstance.instancePath.absoluteStagingPath.parentFile, 'someFile.txt')
        fileNotSupposedToBeThere << 'dummy content'
        // When:
        snvCompletionJob.execute()
        // Then:
        try {
            assert !stagingPath.exists()
            assert !stagingPath.parentFile.exists()
            assert stagingPath.parentFile.parentFile.exists()
        } finally {
            TestCase.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsDirtyContainingDirectory_ShouldDeleteDirectory() {
        // Given:
        TestCase.mockDeleteDirectory(lsdfFilesService)
        TestCase.mockCreateDirectory(lsdfFilesService)

        File stagingPath = snvCallingInstance.instancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File dirNotSupposedToBeThere = new File(snvCallingInstance.instancePath.absoluteStagingPath.parentFile, 'someDir')
        assert dirNotSupposedToBeThere.mkdirs()
        // When:
        snvCompletionJob.execute()
        // Then:
        try {
            assert !stagingPath.exists()
            assert !stagingPath.parentFile.exists()
            assert stagingPath.parentFile.parentFile.exists()
        } finally {
            TestCase.removeMockFileService(lsdfFilesService)
        }
    }


    // Helper methods

    private createFakeResultFiles(File stagingPath) {
        assert stagingPath.mkdirs()

        File dummyFile1 = new File(stagingPath.canonicalPath, 'file1.txt')
        File dummyFile2 = new File(stagingPath.canonicalPath, 'file2.txt')

        dummyFile1 << 'some content'
        dummyFile2 << 'some other content'
    }
}
