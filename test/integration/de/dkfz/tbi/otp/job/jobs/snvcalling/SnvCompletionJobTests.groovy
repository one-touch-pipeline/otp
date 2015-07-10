package de.dkfz.tbi.otp.job.jobs.snvcalling

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

class SnvCompletionJobTests extends GroovyTestCase {

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
    public static final String OTHER_INSTANCE_NAME = "2014-09-01_15h32"

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

        Realm realm_management = DomainFactory.createRealmDataManagementDKFZ([
                rootPath: "${testDirectory}/root",
                processingRootPath: "${testDirectory}/processing",
                stagingRootPath: null
        ])
        assert realm_management.save()

        project = testData.project
        individual = testData.individual
        seqType = testData.seqType

        processedMergedBamFile1 = createProcessedMergedBamFile()
        assert processedMergedBamFile1.save()
        processedMergedBamFile2 = createProcessedMergedBamFile()
        assert processedMergedBamFile2.save()

        SnvCallingInstanceTestData.createOrFindExternalScript()
        snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION,
                externalScriptVersion: "v1",
        )
        assert snvConfig.save()

        SampleTypePerProject.build(project: project, sampleType: processedMergedBamFile1.sampleType, category: SampleType.Category.DISEASE)

        samplePair = new SamplePair(
                individual: individual,
                sampleType1: processedMergedBamFile1.sampleType,
                sampleType2: processedMergedBamFile2.sampleType,
                seqType: seqType)
        assert samplePair.save()

        snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: SOME_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
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
        snvCallingInstance.processingState = SnvProcessingStates.FAILED
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        // Mock deletion, so it does not get in the way of this test
        snvCompletionJob.metaClass.deleteStagingDirectory = { SnvCallingInstance instance -> }
        snvCompletionJob.metaClass.linkResultFiles = { SnvCallingInstance instance -> }
        snvCompletionJob.metaClass.linkConfigFiles = { SnvCallingInstance instance -> }
        // When:
        shouldFail { snvCompletionJob.execute() }
    }

    @Test
    void test_execute_WhenRun_ShouldSetProcessingStateToFinished() {
        // Given:
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        // Mock deletion, so it does not get in the way of this test
        snvCompletionJob.metaClass.deleteStagingDirectory = { SnvCallingInstance instance -> }

        snvCompletionJob.metaClass.linkResultFiles = { SnvCallingInstance instance -> }
        snvCompletionJob.metaClass.linkConfigFiles = { SnvCallingInstance instance -> }
        assert snvCallingInstance.processingState == SnvProcessingStates.IN_PROGRESS
        // When:
        snvCompletionJob.execute()
        // Then:
        assert snvCallingInstance.processingState == SnvProcessingStates.FINISHED
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsClean_ShouldDeleteDirectory() {
        // Given:
        LsdfFilesServiceTests.mockDeleteDirectory(lsdfFilesService)
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        snvCompletionJob.metaClass.linkResultFiles = { SnvCallingInstance instance -> }
        snvCompletionJob.metaClass.linkConfigFiles = { SnvCallingInstance instance -> }

        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        // When:
        snvCompletionJob.execute()
        // Then:
        try {
            assert !stagingPath.exists()
            assert !stagingPath.parentFile.exists()
            assert stagingPath.parentFile.parentFile.exists()
        } finally {
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsDirtyContainingFile_ShouldDeleteDirectory() {
        // Given:
        LsdfFilesServiceTests.mockDeleteDirectory(lsdfFilesService)
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        snvCompletionJob.metaClass.linkResultFiles = { SnvCallingInstance instance -> }
        snvCompletionJob.metaClass.linkConfigFiles = { SnvCallingInstance instance -> }

        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File fileNotSupposedToBeThere = new File(snvCallingInstance.snvInstancePath.absoluteStagingPath.parentFile, 'someFile.txt')
        fileNotSupposedToBeThere << 'dummy content'
        // When:
        snvCompletionJob.execute()
        // Then:
        try {
            assert !stagingPath.exists()
            assert !stagingPath.parentFile.exists()
            assert stagingPath.parentFile.parentFile.exists()
        } finally {
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void test_execute_WhenRunAndDirectoryIsDirtyContainingDirectory_ShouldDeleteDirectory() {
        // Given:
        LsdfFilesServiceTests.mockDeleteDirectory(lsdfFilesService)
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        snvCompletionJob.metaClass.getProcessParameterObject = { snvCallingInstance }
        snvCompletionJob.metaClass.linkResultFiles = { SnvCallingInstance instance -> }
        snvCompletionJob.metaClass.linkConfigFiles = { SnvCallingInstance instance -> }

        File stagingPath = snvCallingInstance.snvInstancePath.absoluteStagingPath
        createFakeResultFiles(stagingPath)
        File dirNotSupposedToBeThere = new File(snvCallingInstance.snvInstancePath.absoluteStagingPath.parentFile, 'someDir')
        assert dirNotSupposedToBeThere.mkdirs()
        // When:
        snvCompletionJob.execute()
        // Then:
        try {
            assert !stagingPath.exists()
            assert !stagingPath.parentFile.exists()
            assert stagingPath.parentFile.parentFile.exists()
        } finally {
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }


    @Test
    void testLinkResultFiles_InstanceIsNull_shouldFail() {
        assert shouldFail(IllegalArgumentException, {
            snvCompletionJob.linkResultFiles(null)
        }).contains("The input instance must not be null")
    }

    @Test
    void testLinkResultFiles_NoFilesInDirectory_NothingHasToBeLinked() {
        File directory = snvCallingInstance.snvInstancePath.absoluteDataManagementPath
        directory.mkdirs()

        snvCompletionJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert sourceLinkMap.isEmpty()
        }

        snvCompletionJob.linkResultFiles(snvCallingInstance)
    }

    @Test
    void testLinkResultFiles_OneFileInDirectory_OneFileHasToBeLinked() {
        File directory = snvCallingInstance.snvInstancePath.absoluteDataManagementPath
        directory.mkdirs()
        File parentDirectory = directory.parentFile
        String fileName = SnvCallingStep.CALLING.getResultFileName
        File sourceFile = new File(directory, fileName)
        assert sourceFile.createNewFile()
        File linkFile = new File(parentDirectory, fileName)
        snvCompletionJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert sourceLinkMap.size() == 1
            assert sourceLinkMap.get(sourceFile) == linkFile
        }

        snvCompletionJob.linkResultFiles(snvCallingInstance)
    }

    @Test
    void testLinkResultFiles_OneFileOneConfigFileInDirectory_OneFileHaveToBeLinked() {
        File directory = snvCallingInstance.snvInstancePath.absoluteDataManagementPath
        directory.mkdirs()
        File parentDirectory = directory.parentFile

        String fileName = SnvCallingStep.CALLING.getResultFileName
        File sourceFile1 = new File(directory, fileName)
        sourceFile1.createNewFile()
        File linkFile1 = new File(parentDirectory, fileName)

        File sourceFile2 = new File(directory, "config.txt")
        sourceFile2.createNewFile()

        snvCompletionJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert sourceLinkMap.size() == 1
            assert sourceLinkMap.get(sourceFile1) == linkFile1
        }

        snvCompletionJob.linkResultFiles(snvCallingInstance)
    }

    @Test
    void testLinkResultFiles_OneConfigFileInDirectory_NothingHasToBeLinked() {
        File directory = snvCallingInstance.snvInstancePath.absoluteDataManagementPath
        directory.mkdirs()

        File sourceFile = new File(directory, "config.txt")
        sourceFile.createNewFile()

        snvCompletionJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert sourceLinkMap.isEmpty()
        }

        snvCompletionJob.linkResultFiles(snvCallingInstance)
    }

    @Test
    void testLinkResultFiles_OneFileNotTypeFileInDirectory_NoFileHasToBeLinked() {
        File directory = snvCallingInstance.snvInstancePath.absoluteDataManagementPath
        directory.mkdirs()
        File fileNotTypeFile = new File(directory, "/subDir")
        fileNotTypeFile.mkdirs()

        snvCompletionJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert sourceLinkMap.isEmpty()
        }

        snvCompletionJob.linkResultFiles(snvCallingInstance)
    }



    @Test
    void testLinkConfigFiles_InputInstanceIsNull_ShouldFail() {
        assert shouldFail(IllegalArgumentException, {
            snvCompletionJob.linkConfigFiles(null)
        }).contains("The input instance must not be null")
    }

    @Test
    void testLinkConfigFiles() {
        File directory = snvCallingInstance.snvInstancePath.absoluteDataManagementPath
        File parentDirectory = directory.parentFile
        directory.mkdirs()
        File sourceFile = new File(directory, "config.txt")
        sourceFile.createNewFile()

        File linkFileCalling = new File(parentDirectory, "config_${SnvCallingStep.CALLING.configFileNameSuffix}_${snvCallingInstance.instanceName}.txt")
        File linkFileAnnotation = new File(parentDirectory, "config_${SnvCallingStep.SNV_ANNOTATION.configFileNameSuffix}_${snvCallingInstance.instanceName}.txt")
        File linkFileDeepAnnotation = new File(parentDirectory, "config_${SnvCallingStep.SNV_DEEPANNOTATION.configFileNameSuffix}_${snvCallingInstance.instanceName}.txt")
        File linkFileFilter = new File(parentDirectory, "config_${SnvCallingStep.FILTER_VCF.configFileNameSuffix}_${snvCallingInstance.instanceName}.txt")

        snvCompletionJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert [linkFileCalling, linkFileAnnotation, linkFileDeepAnnotation, linkFileFilter].contains(sourceLinkMap.get(sourceFile))
        }
        snvCompletionJob.linkConfigFiles(snvCallingInstance)
    }


    @Test
    void testLinkConfigFiles_OnlyFilterJobShallRun_LinkOnlyFilterConfigFile() {
        snvConfig.obsoleteDate = new Date()

        final String CONFIGURATION_NEW ="""
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

        SnvConfig snvConfig2 = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION_NEW,
                externalScriptVersion: "v1",
                previousConfig: snvConfig
        )
        assert snvConfig2.save(flush: true)

        SnvCallingInstance snvCallingInstance2 = DomainFactory.createSnvCallingInstance(
                instanceName: OTHER_INSTANCE_NAME,
                config: snvConfig2,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance2.save(flush: true)

        File directory = snvCallingInstance2.snvInstancePath.absoluteDataManagementPath
        File parentDirectory = directory.parentFile
        directory.mkdirs()
        File sourceFile = new File(directory, "config.txt")
        sourceFile.createNewFile()

        File linkFileFilter = new File(parentDirectory, "config_${SnvCallingStep.FILTER_VCF.configFileNameSuffix}_${snvCallingInstance2.instanceName}.txt")

        snvCompletionJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert sourceLinkMap.size() == 1
            assert sourceLinkMap.get(sourceFile) == linkFileFilter
        }
        snvCompletionJob.linkConfigFiles(snvCallingInstance2)
    }



    @Test
    void testDeleteConfigFileLinkOfPreviousInstance_InputRealmNull_ShouldFail() {
        shouldFail(IllegalArgumentException, {
            snvCompletionJob.deleteConfigFileLinkOfPreviousInstance(null, snvCallingInstance)
        })

    }

    @Test
    void testDeleteConfigFileLinkOfPreviousInstance_InputInstanceNull_ShouldFail() {
        shouldFail(IllegalArgumentException, {
            snvCompletionJob.deleteConfigFileLinkOfPreviousInstance(realm_processing, null)
        })
    }

    @Test
    void testDeleteConfigFileLinkOfPreviousInstance_NoPreviousInstance_NothingToDelete() {
        executionService.metaClass.executeCommand = {Realm realm, String command ->
            throw  new RuntimeException("This method should not be reached since nothing has to be deleted")
        }

        snvCompletionJob.deleteConfigFileLinkOfPreviousInstance(realm_processing, snvCallingInstance)
    }

    @Test
    void testDeleteConfigFileLinkOfPreviousInstance_OnePreviousInstance_OneFileToDelete() {
        SnvCallingInstance snvCallingInstance2 = DomainFactory.createSnvCallingInstance(
                instanceName: OTHER_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance2.save()

        File fileToDelete = snvCallingInstance2.getStepConfigFileLinkedPath(SnvCallingStep.CALLING).absoluteDataManagementPath
        fileToDelete.parentFile.mkdirs()
        fileToDelete.createNewFile()
        executionService.metaClass.executeCommand = {Realm realm, String command ->
            assert command.contains("rm -f ${fileToDelete.path}")
            fileToDelete.delete()
        }

        snvCompletionJob.deleteConfigFileLinkOfPreviousInstance(realm_processing, snvCallingInstance2)
    }

    @Test
    void testDeleteConfigFileLinkOfPreviousInstance_TwoPreviousInstances_OneFileToDelete() {
        SnvCallingInstance snvCallingInstance2 = DomainFactory.createSnvCallingInstance(
                instanceName: OTHER_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance2.save()

        SnvCallingInstance snvCallingInstance3 = DomainFactory.createSnvCallingInstance(
                instanceName: "thirdInstanceName",
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance3.save()

        File fileToDelete = snvCallingInstance3.getStepConfigFileLinkedPath(SnvCallingStep.CALLING).absoluteDataManagementPath
        fileToDelete.parentFile.mkdirs()
        fileToDelete.createNewFile()

        executionService.metaClass.executeCommand = {Realm realm, String command ->
            assert command.contains("rm -f ${fileToDelete.path}")
            fileToDelete.delete()
        }

        snvCompletionJob.deleteConfigFileLinkOfPreviousInstance(realm_processing, snvCallingInstance3)
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
