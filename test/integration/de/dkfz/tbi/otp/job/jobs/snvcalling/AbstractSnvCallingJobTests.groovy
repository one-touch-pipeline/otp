package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateSNVFileHelper
import de.dkfz.tbi.otp.utils.ExternalScript
import de.dkfz.tbi.otp.utils.LinkFileUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static de.dkfz.tbi.otp.utils.CreateSNVFileHelper.createMD5SUMFile
import static de.dkfz.tbi.otp.utils.CreateSNVFileHelper.createResultFile
import static org.junit.Assert.assertEquals

/**
 */
class AbstractSnvCallingJobTests extends GroovyTestCase {

    @Autowired
    ApplicationContext applicationContext

    AbstractSnvCallingJob abstractSnvCallingJob
    File testDirectory
    SnvCallingInstanceTestData testData
    Realm realm_processing
    Project project
    SeqType seqType
    Individual individual
    SamplePair samplePair
    SnvCallingInstance snvCallingInstance
    ProcessedMergedBamFile processedMergedBamFile1
    ProcessedMergedBamFile processedMergedBamFile2
    ExternalScript externalScript_Annotation
    ExternalScript externalScript_Calling
    ExternalScript externalScript_Joining
    SnvJobResult snvCallingJobResult
    SnvConfig snvConfig


    public static final String SOME_INSTANCE_NAME = "2014-08-25_15h32"

    final String PBS_ID = "123456"

    SnvCallingStep step = SnvCallingStep.SNV_ANNOTATION

    final String CONFIGURATION ="""
RUN_CALLING=1
RUN_SNV_ANNOTATION=1
RUN_SNV_DEEPANNOTATION=1
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

    final String DIFFERENT_CONFIGURATION = """
RUN_CALLING=0
RUN_SNV_ANNOTATION=0
RUN_SNV_DEEPANNOTATION=0
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        abstractSnvCallingJob =  [
                getStep: { -> return step },
                getPreviousStep:{ -> return SnvCallingStep.CALLING }
        ] as AbstractSnvCallingJob

        abstractSnvCallingJob.configService = new ConfigService()
        abstractSnvCallingJob.executionService = new ExecutionService()
        abstractSnvCallingJob.lsdfFilesService = new LsdfFilesService()
        abstractSnvCallingJob.linkFileUtils = new LinkFileUtils()

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

        snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION,
                externalScriptVersion: "v1",
        )
        assert snvConfig.save(flush: true)

        snvCallingInstance = DomainFactory.createSnvCallingInstance(
                instanceName: SOME_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance.save(flush: true)

         externalScript_Joining = testData.externalScript_Joining

        snvCallingJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                externalScript: externalScript_Calling,
                processingState: SnvProcessingStates.FINISHED,
                chromosomeJoinExternalScript: externalScript_Joining,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                fileSize: 1234l,
        )
        assert snvCallingJobResult.save()


        externalScript_Annotation = new ExternalScript(
                scriptIdentifier: step.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/annotation.sh",
                author: "otptest",
        )
        assert externalScript_Annotation.save()
    }

    @After
    void tearDown() {
        abstractSnvCallingJob = null
        testData = null
        realm_processing = null
        project = null
        seqType = null
        individual = null
        samplePair = null
        snvCallingInstance = null
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
        externalScript_Annotation = null
        externalScript_Calling = null
        externalScript_Joining = null
        snvCallingJobResult = null
        snvConfig = null
        assert testDirectory.deleteDir()
    }


    @Test
    void testWriteConfigFile_InputIsNull() {
        shouldFail( IllegalArgumentException, { abstractSnvCallingJob.writeConfigFile(null) })
    }


    @Test
    void testWriteConfigFile_FileExistsAlready_InStagingDir() {

        File configFileInStagingDirectory = testData.createConfigFileWithContentInFileSystem(
                snvCallingInstance.configFilePath.absoluteStagingPath,
                CONFIGURATION)

        File configFileInProjectDirectory = snvCallingInstance.configFilePath.absoluteDataManagementPath

        abstractSnvCallingJob.configService.metaClass.getRealmDataProcessing = { Project project ->
            return realm_processing
        }

        abstractSnvCallingJob.executionService.metaClass.executeCommand = { Realm realm, String command ->

            String expectedCommandPart= "mkdir -p ${configFileInProjectDirectory.parent}; " +
                    "chmod 2750 ${configFileInProjectDirectory.parent}; " +
                    "cp ${configFileInStagingDirectory} ${configFileInProjectDirectory}; " +
                    "chmod 640 ${configFileInProjectDirectory}; " +
                    "rm ${configFileInStagingDirectory}"

            assert command.contains(expectedCommandPart)

            testData.createConfigFileWithContentInFileSystem(
                    snvCallingInstance.configFilePath.absoluteDataManagementPath,
                    CONFIGURATION)

            return PBS_ID
        }

        assertEquals(configFileInProjectDirectory, abstractSnvCallingJob.writeConfigFile(snvCallingInstance))
        AbstractSnvCallingJob.assertDataManagementConfigContentsOk(snvCallingInstance)
    }

    @Test
    void testWriteConfigFile_FileExistsAlreadyButWithDifferentContent_InStagingDir_ShouldFail() {

        testData.createConfigFileWithContentInFileSystem(
                snvCallingInstance.configFilePath.absoluteStagingPath,
                DIFFERENT_CONFIGURATION)

        abstractSnvCallingJob.configService.metaClass.getRealmDataProcessing = { Project project ->
            return realm_processing
        }

        shouldFail(AssertionError, {abstractSnvCallingJob.writeConfigFile(snvCallingInstance)})
    }


    @Test
    void testWriteConfigFile_FileExistsAlready_InProjectDir() {
        File configFile = testData.createConfigFileWithContentInFileSystem(
                snvCallingInstance.configFilePath.absoluteDataManagementPath,
                CONFIGURATION)

        abstractSnvCallingJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            throw new RuntimeException("This method should not be called.")
        }

        assertEquals(configFile, abstractSnvCallingJob.writeConfigFile(snvCallingInstance))
        AbstractSnvCallingJob.assertDataManagementConfigContentsOk(snvCallingInstance)
    }

    @Test
    void testWriteConfigFile_FileExistsAlreadyButWithDifferentContent_InProjectDir_ShouldFail() {
        testData.createConfigFileWithContentInFileSystem(
                snvCallingInstance.configFilePath.absoluteDataManagementPath,
                DIFFERENT_CONFIGURATION)

        abstractSnvCallingJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            throw new RuntimeException("This method should not be called.")
        }

        shouldFail(AssertionError, {abstractSnvCallingJob.writeConfigFile(snvCallingInstance)})
    }


    @Test
    void testWriteConfigFile() {

        File configFileInStagingDirectory = snvCallingInstance.configFilePath.absoluteStagingPath
        File configFileInProjectDirectory = snvCallingInstance.configFilePath.absoluteDataManagementPath

        abstractSnvCallingJob.lsdfFilesService.metaClass.createDirectory = { File dir, Project project ->
            dir.mkdirs()
        }
        abstractSnvCallingJob.executionService.metaClass.executeCommand = { Realm realm, String command ->

            String expectedCommandPart_first = "mkdir --parents --mode 2770 ${configFileInProjectDirectory.parent}"

            String expectedCommandPart_second= "mkdir -p ${configFileInProjectDirectory.parent}; " +
                    "chmod 2750 ${configFileInProjectDirectory.parent}; " +
                    "cp ${configFileInStagingDirectory} ${configFileInProjectDirectory}; " +
                    "chmod 640 ${configFileInProjectDirectory}; " +
                    "rm ${configFileInStagingDirectory}"

            assert (command.contains(expectedCommandPart_first) || command.contains(expectedCommandPart_second))

            testData.createConfigFileWithContentInFileSystem(
                    snvCallingInstance.configFilePath.absoluteDataManagementPath,
                    CONFIGURATION)

            return PBS_ID
        }

        File file = snvCallingInstance.configFilePath.absoluteDataManagementPath
        if (file.exists()) {
            file.delete()
        }
        assertEquals(file, abstractSnvCallingJob.writeConfigFile(snvCallingInstance))

        try {
            assert file.text == CONFIGURATION
        } finally {
            assert file.delete()
        }
    }


    @Test
    void testCreateAndSaveSnvJobResult() {
        assert 1 == SnvJobResult.count()
        abstractSnvCallingJob.createAndSaveSnvJobResult(snvCallingInstance, externalScript_Annotation, null, snvCallingJobResult)
        final List<SnvJobResult> snvJobResults = SnvJobResult.list(sort:"id", order:"desc")
        snvJobResults.size() == 2
        SnvJobResult snvJobResult = snvJobResults.first()
        assert snvJobResult.snvCallingInstance == snvCallingInstance
        assert snvJobResult.processingState == SnvProcessingStates.IN_PROGRESS
        assert snvJobResult.step == step
        assert snvJobResult.externalScript == externalScript_Annotation
        assert snvJobResult.inputResult == snvCallingJobResult
        assert snvJobResult.chromosomeJoinExternalScript == null
    }


    @Test
    void testCreateAndSaveSnvJobResult_ResultInProgress() {
        assert 1 == SnvJobResult.count()
        snvCallingJobResult.processingState = SnvProcessingStates.IN_PROGRESS
        assert snvCallingJobResult.save(flush: true)
        abstractSnvCallingJob.metaClass.getStep = { -> return SnvCallingStep.CALLING }
        abstractSnvCallingJob.createAndSaveSnvJobResult(snvCallingInstance, externalScript_Calling, externalScript_Joining, null)
        assert 1 == SnvJobResult.count()
        snvCallingJobResult.processingState == SnvProcessingStates.IN_PROGRESS
    }


    @Test
    void testChangeProcessingStateOfJobResult_StepAnnotation_NoAdditionalInformationShallBeAdded() {
        SnvJobResult snvAnnotationJobResult = createAndSaveAnnotationJobResult()

        createResultFile(snvCallingInstance, step)
        createMD5SUMFile(snvCallingInstance, step)
        List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstanceAndStep(snvCallingInstance, step)
        assert results.size == 1
        assert results.first() == snvAnnotationJobResult
        assert snvAnnotationJobResult.processingState == SnvProcessingStates.IN_PROGRESS
        snvAnnotationJobResult.md5sum = null
        assert snvAnnotationJobResult.save(flush: true)
        abstractSnvCallingJob.changeProcessingStateOfJobResult(snvCallingInstance, SnvProcessingStates.FINISHED)
        assert snvAnnotationJobResult.processingState == SnvProcessingStates.FINISHED
        assert snvAnnotationJobResult.md5sum == null
    }

    @Test
    void testChangeProcessingStateOfJobResult_StepCalling_AdditionalInformationShallBeAdded() {
        createResultFile(snvCallingInstance, SnvCallingStep.CALLING)
        createMD5SUMFile(snvCallingInstance, SnvCallingStep.CALLING)
        snvCallingJobResult.processingState = SnvProcessingStates.IN_PROGRESS
        snvCallingJobResult.md5sum = null
        assert snvCallingJobResult.save(flush: true)
        abstractSnvCallingJob.metaClass.getStep = { -> return SnvCallingStep.CALLING }
        abstractSnvCallingJob.changeProcessingStateOfJobResult(snvCallingInstance, SnvProcessingStates.FINISHED)
        assert snvCallingJobResult.processingState == SnvProcessingStates.FINISHED
        assert snvCallingJobResult.md5sum == CreateSNVFileHelper.MD5SUM
    }


    @Test
    void testAddFileInformationToJobResult() {
        SnvJobResult snvAnnotationJobResult = createAndSaveAnnotationJobResult()

        File file = createResultFile(snvCallingInstance, step)
        createMD5SUMFile(snvCallingInstance, step)
        SnvJobResult results = exactlyOneElement(SnvJobResult.findAllBySnvCallingInstanceAndStep(snvCallingInstance, step))

        abstractSnvCallingJob.addFileInformationToJobResult(results)
        assert results.fileSize == file.size()
        assert results.md5sum == CreateSNVFileHelper.MD5SUM
    }


    @Test
    void testCheckIfResultFilesExistsOrThrowException_NoResults() {
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        assert shouldFail(RuntimeException, {
            abstractSnvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance)
        }).contains(SnvCallingStep.SNV_ANNOTATION.name())
    }


    @Test
    void testCheckIfResultFilesExistsOrThrowException_WithResults() {
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvCallingJobResult }

        abstractSnvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance)
    }



    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_InputIsNull() {
        assert shouldFail(IllegalArgumentException,{
            abstractSnvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(null)
        }).contains("The input seqType must not be null in method getSnvPBSOptionsNameSeqTypeSpecific")
    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_AnnotationStep_ExomeSeqType() {
        assert "snvPipeline_SNV_ANNOTATION_WES" == abstractSnvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(DomainFactory.createExomeSeqType())
    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_AnnotationStep_WholeGenomeSeqType() {
        assert "snvPipeline_SNV_ANNOTATION_WGS" == abstractSnvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(DomainFactory.createWholeGenomeSeqType())
    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_CallingStep_ExomeSeqType() {
        SnvCallingJob snvCallingJob = applicationContext.getBean('snvCallingJob',
                DomainFactory.createAndSaveProcessingStep(SnvCallingJob.toString()), [])
        snvCallingJob.log = log
        assert "snvPipeline_CALLING_WES" == snvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(DomainFactory.createExomeSeqType())
    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_DeepAnnotationStep_ExomeSeqType() {
        SnvDeepAnnotationJob snvDeepAnnotationJob = applicationContext.getBean('snvDeepAnnotationJob',
                DomainFactory.createAndSaveProcessingStep(SnvDeepAnnotationJob.toString()), [])
        snvDeepAnnotationJob.log = log
        assert "snvPipeline_SNV_DEEPANNOTATION_WES" == snvDeepAnnotationJob.getSnvPBSOptionsNameSeqTypeSpecific(DomainFactory.createExomeSeqType())
    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_FilterStep_ExomeSeqType() {
        FilterVcfJob filterVcfJob = applicationContext.getBean('filterVcfJob',
                DomainFactory.createAndSaveProcessingStep(FilterVcfJob.toString()), [])
        filterVcfJob.log = log
        assert "snvPipeline_FILTER_VCF_WES" == filterVcfJob.getSnvPBSOptionsNameSeqTypeSpecific(DomainFactory.createExomeSeqType())
    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_OtherSeqType() {
        SeqType otherSeqType = new SeqType(
                name: "ChIP Seq",
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
                dirName: "chip_seq_sequencing")
        assert otherSeqType.save(flush: true)

        assert shouldFail(RuntimeException,{
            abstractSnvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(otherSeqType)
        }).contains("There are no PBS Options available for the SNV pipeline for seqtype")
    }


    @Test(expected = AssertionError)
    void testConfirmCheckPointFileExistsAndDeleteIt_CheckpointFileDoesNotExist_ShouldFail() {
        abstractSnvCallingJob.confirmCheckPointFileExistsAndDeleteIt(snvCallingInstance, step)
    }


    @Test(expected = AssertionError)
    void testConfirmCheckPointFileExistsAndDeleteIt_CheckpointFileWasNotDeleted_ShouldFail() {
        File checkpointFile = new OtpPath(snvCallingInstance.snvInstancePath, step.checkpointFileName).absoluteDataManagementPath
        checkpointFile.parentFile.mkdirs()
        checkpointFile.createNewFile()

        abstractSnvCallingJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert command == "rm ${checkpointFile.path}"

        }
        abstractSnvCallingJob.confirmCheckPointFileExistsAndDeleteIt(snvCallingInstance, step)
    }


    @Test
    void testConfirmCheckPointFileExistsAndDeleteIt() {
        File checkpointFile = new OtpPath(snvCallingInstance.snvInstancePath, step.checkpointFileName).absoluteDataManagementPath
        checkpointFile.parentFile.mkdirs()
        checkpointFile.createNewFile()

        abstractSnvCallingJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert command == "rm ${checkpointFile.path}"
            checkpointFile.delete()
        }
        abstractSnvCallingJob.confirmCheckPointFileExistsAndDeleteIt(snvCallingInstance, step)
    }


    SnvJobResult createAndSaveAnnotationJobResult() {
        SnvJobResult jobResult = new SnvJobResult(
                step: step,
                snvCallingInstance: snvCallingInstance,
                externalScript: externalScript_Annotation,
                processingState: SnvProcessingStates.IN_PROGRESS,
                chromosomeJoinExternalScript: null,
                inputResult: snvCallingJobResult,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                fileSize: 1234l,
        )
        assert jobResult.save()
        return jobResult
    }
}
