package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.utils.HelperUtils

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static org.junit.Assert.*
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript
import static de.dkfz.tbi.otp.utils.CreateFileHelper.*

class SnvDeepAnnotationJobTests extends GroovyTestCase {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    ExecutionService executionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @Autowired
    LsdfFilesService lsdfFilesService

    File testDirectory
    SnvDeepAnnotationJob snvDeepAnnotationJob
    SnvCallingInstance snvCallingInstance1
    SnvCallingInstance snvCallingInstance2
    SnvJobResult snvJobResult_Annotation
    SnvJobResult snvJobResult_DeepAnnotation
    ProcessedMergedBamFile processedMergedBamFile1
    SnvCallingInstanceTestData testData

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
    String UNIQUE_PATH = HelperUtils.getUniqueString()

    @Before
    void setUp() {

        testDirectory = new File("/tmp/otp-test/${UNIQUE_PATH}")
        assert testDirectory.mkdirs()  // This will fail if the directory already exists or if it could not be created.

        testData = new SnvCallingInstanceTestData()
        testData.createSnvObjects()

        Realm realm_processing = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: '${testDirectory}/processing',
            stagingRootPath   : "${testDirectory}/staging"
        ])
        assert realm_processing.save()

        Realm realm_management = DomainFactory.createRealmDataManagementDKFZ([
            rootPath          : "${testDirectory}/root",
            processingRootPath: "${testDirectory}/processing",
            stagingRootPath   : null
        ])
        assert realm_management.save()

        processedMergedBamFile1 = testData.bamFileTumor
        ProcessedMergedBamFile processedMergedBamFile2 = testData.bamFileControl

        SampleType sampleType1 = processedMergedBamFile1.sample.sampleType
        sampleType1.name = "sampleType1"
        assert sampleType1.save(flush: true)
        SampleType sampleType2 = processedMergedBamFile2.sample.sampleType
        sampleType2.name = "sampleType2"
        assert sampleType2.save(flush: true)

        SnvConfig snvConfig = testData.snvConfig
        snvConfig.configuration = CONFIGURATION
        assert snvConfig.save()

        snvCallingInstance1 = testData.createSnvCallingInstance([
            sampleType1BamFile: processedMergedBamFile1,
            sampleType2BamFile: processedMergedBamFile2,
            instanceName      : SOME_INSTANCE_NAME,
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
                filePath: "/tmp/scriptLocation/annotation.sh",
                author: "otptest",
                )
        assert externalScript_Annotation.save()

        ExternalScript externalScript_Calling = new ExternalScript(
                scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                filePath: "/tmp/scriptLocation/calling.sh",
                author: "otptest",
                )
        assert externalScript_Calling.save()

        SnvJobResult snvJobResult_Calling = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Calling,
                processingState: SnvProcessingStates.FINISHED,
                chromosomeJoinExternalScript: testData.externalScript_Joining,
                fileSize: 1234l,
                md5sum: "a841c64c5825e986c4709ac7298e9366",
                )
        assert snvJobResult_Calling.save()

        snvJobResult_Annotation = new SnvJobResult(
                step: SnvCallingStep.SNV_ANNOTATION,
                snvCallingInstance: snvCallingInstance2,
                externalScript: externalScript_Annotation,
                processingState: SnvProcessingStates.FINISHED,
                inputResult: snvJobResult_Calling,
                )
        assert snvJobResult_Annotation.save()

        ExternalScript externalScript_DeepAnnotation = new ExternalScript(
                scriptIdentifier: SnvCallingStep.SNV_DEEPANNOTATION.externalScriptIdentifier,
                filePath: "/tmp/scriptLocation/deepAnnotation.sh",
                author: "otptest",
                )
        externalScript_DeepAnnotation.save()

        snvJobResult_DeepAnnotation = new SnvJobResult(
                step: SnvCallingStep.SNV_DEEPANNOTATION,
                snvCallingInstance: snvCallingInstance1,
                externalScript: externalScript_DeepAnnotation,
                inputResult: snvJobResult_Annotation,
                )
        snvJobResult_DeepAnnotation.save()

        snvDeepAnnotationJob = applicationContext.getBean('snvDeepAnnotationJob',
                DomainFactory.createAndSaveProcessingStep(SnvDeepAnnotationJob.toString()), [])
        snvDeepAnnotationJob.log = log

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
        testData = null
        snvJobResult_Annotation = null
        snvJobResult_DeepAnnotation = null
        snvCallingInstance1 = null
        snvCallingInstance2 = null
        processedMergedBamFile1 = null
        // Reset meta classes
        removeMetaClass(ExecutionService, executionService)
        removeMetaClass(CreateClusterScriptService, createClusterScriptService)
        LsdfFilesService.metaClass = null
        // Clean-up
        assert testDirectory.deleteDir()
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

        snvDeepAnnotationJob.metaClass.addOutputParameter = { String name, String value -> }
        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult_DeepAnnotation }

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

        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvDeepAnnotationJob.metaClass.addOutputParameter = { String name, String value -> }
        snvDeepAnnotationJob.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        shouldFail(RuntimeException, { snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2) })
    }

    @Test
    void testMaybeSubmit() {
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        SnvCallingStep step = SnvCallingStep.SNV_DEEPANNOTATION
        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }
        snvDeepAnnotationJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult -> }
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->

            File snvFile = new OtpPath(snvCallingInstance2.snvInstancePath, step.getResultFileName(snvCallingInstance2.individual)).absoluteStagingPath
            File md5sumFile = createMD5SUMFile(snvCallingInstance2, SnvCallingStep.SNV_DEEPANNOTATION)

            String scriptCommandPart = "# BEGIN ORIGINAL SCRIPT\n" +
                    "/tmp/scriptLocation/deepAnnotation.sh; " +
                    "md5sum ${snvFile} > ${md5sumFile}"

            String qsubParameterCommandPart = "-v CONFIG_FILE=" +
                    "${snvCallingInstance2.configFilePath.absoluteStagingPath}," +
                    "pid=654321," +
                    "PID=654321," +
                    "TOOL_ID=snvDeepAnnotation," +
                    "PIPENAME=SNV_DEEPANNOTATION," +
                    "FILENAME_VCF=${snvFile}," +
                    "FILENAME_VCF_SNVS=${snvFile}," +
                    "FILENAME_CHECKPOINT=${step.getCheckpointFilePath(snvCallingInstance2).absoluteStagingPath},"


            assert command.contains(scriptCommandPart)
            assert command.contains(qsubParameterCommandPart)

            return [PBS_ID]
        }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep snvCallingStep -> return snvJobResult_Annotation }

        createResultFile(snvCallingInstance2, SnvCallingStep.SNV_ANNOTATION)

        schedulerService.startingJobExecutionOnCurrentThread(snvDeepAnnotationJob)

        try {
            assertEquals(NextAction.WAIT_FOR_CLUSTER_JOBS, snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2))

            assert !SnvCallingStep.SNV_DEEPANNOTATION.getCheckpointFilePath(snvCallingInstance2).absoluteStagingPath.exists()

            File configFile = snvCallingInstance2.configFilePath.absoluteStagingPath
            assert configFile.exists()
            assert configFile.text == snvCallingInstance2.config.configuration

            File annotationFile = new OtpPath(snvCallingInstance2.snvInstancePath, SnvCallingStep.SNV_ANNOTATION.getResultFileName(snvCallingInstance2.individual)).absoluteStagingPath
            File deepAnnotationFile = new OtpPath(snvCallingInstance2.snvInstancePath, step.getResultFileName(snvCallingInstance2.individual)).absoluteStagingPath
            assert annotationFile.text == deepAnnotationFile.text
        } finally {
            schedulerService.finishedJobExecutionOnCurrentThread(snvDeepAnnotationJob)
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testMaybeSubmit_InputFileNotReadable() {
        SnvCallingStep step = SnvCallingStep.SNV_DEEPANNOTATION
        snvDeepAnnotationJob.metaClass.getProcessParameterObject = { return snvCallingInstance2 }

        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep snvCallingStep -> return snvJobResult_Annotation }

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
            throw new AssertionError("Not readable")
        }

        assert shouldFail(AssertionError, { snvDeepAnnotationJob.maybeSubmit(snvCallingInstance2)} ).contains("Not readable")
    }

    @Test
    void testValidate() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteStagingPath, CONFIGURATION)

        File checkpointFile = SnvCallingStep.SNV_DEEPANNOTATION.getCheckpointFilePath(snvCallingInstance1).absoluteStagingPath
        checkpointFile.createNewFile()
        createResultFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)
        createMD5SUMFile(snvCallingInstance1, SnvCallingStep.SNV_DEEPANNOTATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }
        createClusterScriptService.metaClass.createTransferScript = { List<File> sourceLocations, List<File> targetLocations, List<File> linkLocations, boolean move ->

            // test that source files are correct
            File stagingBase = new File("/tmp/otp-test/${UNIQUE_PATH}/staging/")
            File individualPathStaging = new File(stagingBase, "dirName/sequencing/whole_genome_sequencing/view-by-pid/654321/")
            File samplePairPathStaging = new File(individualPathStaging, "snv_results/paired/sampletype1_sampletype2/")
            File instancePathStaging = new File(samplePairPathStaging, "2014-09-01_15h32/")

            assert sourceLocations.size() == 3
            assert sourceLocations.contains(new File(instancePathStaging, "snvs_654321.vcf.gz"))
            assert sourceLocations.contains(new File(instancePathStaging, "snvs_654321.vcf.gz.tbi"))
            assert sourceLocations.contains(new File(instancePathStaging, "config.txt"))

            // test that target files are correct
            File rootBase = new File("/tmp/otp-test/${UNIQUE_PATH}/root/")
            File individualPathRoot = new File(rootBase, "dirName/sequencing/whole_genome_sequencing/view-by-pid/654321/")
            File samplePairPathRoot = new File(individualPathRoot, "snv_results/paired/sampletype1_sampletype2/")
            File instancePathRoot = new File(samplePairPathRoot, "2014-09-01_15h32/")

            assert targetLocations.size() == 3
            assert targetLocations.contains(new File(instancePathRoot, "snvs_654321.vcf.gz"))
            assert targetLocations.contains(new File(instancePathRoot, "snvs_654321.vcf.gz.tbi"))
            assert targetLocations.contains(new File(instancePathRoot, "config.txt"))

            // test that linked files are correct
            assert linkLocations.size() == 3
            assert linkLocations.contains(new File(samplePairPathRoot, "snvs_654321.vcf.gz"))
            assert linkLocations.contains(new File(samplePairPathRoot, "snvs_654321.vcf.gz.tbi"))
            assert linkLocations.contains(new File(samplePairPathRoot, "config_snv_deepannotation_2014-09-01_15h32.txt"))

            "#some script"
        }
        assert checkpointFile.exists()
        snvDeepAnnotationJob.validate(snvCallingInstance1)
        assert snvJobResult_DeepAnnotation.processingState == SnvProcessingStates.FINISHED
        assert !checkpointFile.exists()

        assert configFile.exists()
        assert configFile.text == snvCallingInstance1.config.configuration
    }

    @Test
    void testValidate_CheckpointFileDoesNotExists() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteStagingPath, CONFIGURATION)
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> }

        shouldFail(AssertionError, {snvDeepAnnotationJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_ConfigFileDoesNotExists() {
        shouldFail(FileNotFoundException, {snvDeepAnnotationJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_WrongConfigurationInConfigFile() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteStagingPath, "wrong configuration")
        shouldFail(AssertionError, {snvDeepAnnotationJob.validate(snvCallingInstance1)})
    }

    @Test
    void testValidate_InputFileNotReadable() {
        SnvCallingStep step = SnvCallingStep.SNV_DEEPANNOTATION
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteStagingPath, CONFIGURATION)
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
            if (file.path.contains("_annotation")) {
                throw new AssertionError("Not readable")
            }
        }

        File checkpointFile = new OtpPath(snvCallingInstance1.snvInstancePath, step.checkpointFileName).absoluteStagingPath
        checkpointFile.createNewFile()

        shouldFail(RuntimeException, { snvDeepAnnotationJob.validate(snvCallingInstance1) })
    }

    @Test
    void testValidate_ResultFileNotReadable() {
        File configFile = testData.createConfigFileWithContentInFileSystem(snvCallingInstance1.configFilePath.absoluteStagingPath, CONFIGURATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
            if (!file.path.contains("_annotation")) {
                throw new AssertionError("Not readable")
            }
        }

        assert shouldFail(AssertionError, { snvDeepAnnotationJob.validate(snvCallingInstance1) }).contains("Not readable")
    }
}
