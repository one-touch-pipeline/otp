package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.TestCase
import org.junit.After
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstanceTestData
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Before
import org.junit.Test

class SnvCompletionJobTests extends GroovyTestCase {

    @Autowired
    ApplicationContext applicationContext

    File testDirectory
    Individual individual
    Project project
    Realm realm_processing
    SeqType seqType
    SnvCallingInstance snvCallingInstance
    SnvCompletionJob snvCompletionJob
    SnvCallingInstanceTestData testData

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
        testData.createObjects()
        realm_processing = testData.realm
        realm_processing.stagingRootPath = "${testDirectory}/staging"
        assert realm_processing.save()

        project = testData.project
        individual = testData.individual
        seqType = testData.seqType

        ProcessedMergedBamFile processedMergedBamFile1 = createProcessedMergedBamFile()
        assert processedMergedBamFile1.save()
        ProcessedMergedBamFile processedMergedBamFile2 = createProcessedMergedBamFile()
        assert processedMergedBamFile2.save()

        SnvConfig snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION)
        assert snvConfig.save()

        SampleTypePerProject.build(project: project, sampleType: processedMergedBamFile1.sampleType, category: SampleType.Category.DISEASE)

        SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual = new SampleTypeCombinationPerIndividual(
                individual: individual,
                sampleType1: processedMergedBamFile1.sampleType,
                sampleType2: processedMergedBamFile2.sampleType,
                seqType: seqType)
        assert sampleTypeCombinationPerIndividual.save()

        snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: SOME_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                sampleTypeCombination: sampleTypeCombinationPerIndividual)
        assert snvCallingInstance.save()

        snvCompletionJob = applicationContext.getBean('snvCompletionJob',
                DomainFactory.createAndSaveProcessingStep(SnvCompletionJob.toString()), [])
        snvCompletionJob.log = log
    }

    @After
    void tearDown() {
        testData = null
        individual = null
        project = null
        seqType = null
        snvCallingInstance = null
        realm_processing = null
        // Reset meta classes
        snvCompletionJob.metaClass = null
        // Clean-up file-system
        assert testDirectory.deleteDir()
    }

    @Test
    void test_execute_WhenRun_ShouldSetProcessingStateToFinished() {
        // Given:
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        // Mock deletion, so it does not get in the way of this test
        snvCompletionJob.metaClass.deleteStagingDirectory = { SnvCallingInstance instance -> }
        assert snvCallingInstance.processingState == SnvProcessingStates.IN_PROGRESS
        // When:
        snvCompletionJob.execute()
        // Then:
        assert snvCallingInstance.processingState == SnvProcessingStates.FINISHED
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsClean_ShouldDeleteDirectory() {
        // Given:
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        // When:
        snvCompletionJob.execute()
        // Then:
        assert !stagingPath.exists()
        assert !stagingPath.parentFile.exists()
        assert stagingPath.parentFile.parentFile.exists()
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsDirtyContainingFile_ShouldDeleteDirectory() {
        // Given:
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File fileNotSupposedToBeThere = new File(snvCallingInstance.snvInstancePath.absoluteStagingPath.parentFile, 'someFile.txt')
        fileNotSupposedToBeThere << 'dummy content'
        // When:
        snvCompletionJob.execute()
        // Then:
        assert !stagingPath.exists()
        assert !stagingPath.parentFile.exists()
        assert stagingPath.parentFile.parentFile.exists()
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsDirtyContainingDirectory_ShouldDeleteDirectory() {
        // Given:
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File dirNotSupposedToBeThere = new File(snvCallingInstance.snvInstancePath.absoluteStagingPath.parentFile, 'someDir')
        assert dirNotSupposedToBeThere.mkdirs()
        // When:
        snvCompletionJob.execute()
        // Then:
        assert !stagingPath.exists()
        assert !stagingPath.parentFile.exists()
        assert stagingPath.parentFile.parentFile.exists()
    }

    // Helper methods

    private createFakeResultFiles(File stagingPath) {
        assert stagingPath.mkdirs()

        File dummyFile1 = new File(stagingPath.canonicalPath, 'file1.txt')
        File dummyFile2 = new File(stagingPath.canonicalPath, 'file2.txt')

        dummyFile1 << 'some content'
        dummyFile2 << 'some other content'
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile() {
        return testData.createProcessedMergedBamFile(individual, seqType)
    }
}
