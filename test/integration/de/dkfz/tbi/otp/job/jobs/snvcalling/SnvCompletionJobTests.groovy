package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.TestCase
import org.junit.After
import org.junit.Ignore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
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
    TestData testData

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

        testData = new TestData()
        testData.createObjects()
        realm_processing = testData.realm
        realm_processing.stagingRootPath = "${testDirectory}/staging"
        assert realm_processing.save()

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
    void test_execute_WhenRunAndDirectoryIsDirtyContainingFile_ShouldThrowException() {
        // Given:
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File fileNotSupposedToBeThere = new File(snvCallingInstance.snvInstancePath.absoluteStagingPath.parentFile, 'someFile.txt')
        fileNotSupposedToBeThere << 'dummy content'
        // When:
        def msg = shouldFail IOException, { snvCompletionJob.execute() }
        // Then:
        assert msg =~ /contains unknown files/
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsDirtyContainingDirectory_ShouldThrowException() {
        // Given:
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File dirNotSupposedToBeThere = new File(snvCallingInstance.snvInstancePath.absoluteStagingPath.parentFile, 'someDir')
        assert dirNotSupposedToBeThere.mkdirs()
        // When:
        def msg = shouldFail IOException, { snvCompletionJob.execute() }
        // Then:
        assert msg =~ /contains unknown files/
    }

    // Helper methods

    private createFakeResultFiles(File stagingPath) {
        assert stagingPath.mkdirs()

        File dummyFile1 = new File(stagingPath.canonicalPath, 'file1.txt')
        File dummyFile2 = new File(stagingPath.canonicalPath, 'file2.txt')

        dummyFile1 << 'some content'
        dummyFile2 << 'some other content'
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(String identifier) {
        SampleType sampleType = testData.createSampleType([name: "SampleType" + identifier])
        assert sampleType.save(flush: true)

        Sample sample = testData.createSample([individual: individual, sampleType: sampleType])
        assert sample.save(flush: true)

        SeqTrack seqTrack = testData.createSeqTrack([sample: sample, seqType: seqType])
        assert seqTrack.save(flush: true)

        AlignmentPass alignmentPass = testData.createAlignmentPass([seqTrack: seqTrack])
        assert alignmentPass.save(flush: true)

        ProcessedBamFile processedBamFile = testData.createProcessedBamFile([
                alignmentPass          : alignmentPass,
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
        ])
        assert processedBamFile.save(flush: true)

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage([sample: sample, seqType: seqType])
        assert mergingWorkPackage.save(flush: true)

        MergingSet mergingSet = testData.createMergingSet([mergingWorkPackage: mergingWorkPackage, status: MergingSet.State.PROCESSED])
        assert mergingSet.save(flush: true)

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                mergingSet: mergingSet,
                bamFile: processedBamFile,
        )
        assert mergingSetAssignment.save(flush: true)

        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        mergingPass.save(flush: true)

        final String ARBITRARY_SIZE = 1234

        final ProcessedMergedBamFile bamFile = testData.createProcessedMergedBamFile([
                mergingPass            : mergingPass,
                fileExists             : true,
                fileSize               : ARBITRARY_SIZE,
                md5sum                 : '0123456789ABCDEF0123456789ABCDEF',
                qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED,
                status                 : AbstractBamFile.State.PROCESSED,
                fileOperationStatus    : AbstractBamFile.FileOperationStatus.PROCESSED,
        ])

        return bamFile
    }
}
