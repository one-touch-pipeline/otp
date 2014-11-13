package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static org.junit.Assert.*
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.ParameterType
import de.dkfz.tbi.otp.job.processing.ParameterUsage
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExternalScript

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

    File testDirectory
    Realm realm_processing
    Project project
    SeqType seqType
    Individual individual
    SnvCallingInstance snvCallingInstance
    SnvCallingInstance snvCallingInstance2
    ExternalScript externalScript_Calling
    ExternalScript externalScript_Joining
    SnvJobResult snvJobResult
    SnvCallingJob snvCallingJob
    SnvCallingInstanceTestData testData

    final String CONFIGURATION ="""
RUN_CALLING=1
RUN_SNV_ANNOTATION=1
RUN_SNV_DEEPANNOTATION=1
RUN_FILTER_VCF=1
CHROMOSOME_INDICES=( {1..21} X Y)
"""

    final String PBS_ID = "123456"
    String UNIQUE_PATH = TestCase.getUniqueString()

    @Before
    void setUp() {

        testDirectory = new File("/tmp/otp-test/${UNIQUE_PATH}")
        assert testDirectory.mkdirs()  // This will fail if the directory already exists or if it could not be created.

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

        ProcessedMergedBamFile processedMergedBamFile1 = createProcessedMergedBamFile("1")
        assert processedMergedBamFile1.save()
        ProcessedMergedBamFile processedMergedBamFile2 = createProcessedMergedBamFile("2")
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

        snvCallingInstance = new SnvCallingInstance(
                instanceName: SOME_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                sampleTypeCombination: sampleTypeCombinationPerIndividual)
        assert snvCallingInstance.save()

        snvCallingInstance2 = new SnvCallingInstance(
                instanceName: OTHER_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                sampleTypeCombination: sampleTypeCombinationPerIndividual)
        assert snvCallingInstance2.save()

        externalScript_Calling = new ExternalScript(
                scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                filePath: "/tmp/scriptLocation/calling.sh",
                author: "otptest",
                )
        assert externalScript_Calling.save()

        externalScript_Joining = new ExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                filePath: "/tmp/scriptLocation/joining.sh",
                author: "otptest",
                )
        assert externalScript_Joining.save()

        snvJobResult = new SnvJobResult(
                step: SnvCallingStep.CALLING,
                snvCallingInstance: snvCallingInstance,
                externalScript: externalScript_Calling,
                chromosomeJoinExternalScript: externalScript_Joining,
                )
        assert snvJobResult.save()

        snvCallingJob = applicationContext.getBean('snvCallingJob',
            DomainFactory.createAndSaveProcessingStep(SnvCallingJob.toString()), [])
        snvCallingJob.log = log

        ParameterType typeRealm = new ParameterType(
                name: REALM,
                className: "${SnvCallingJob.class}",
                jobDefinition: snvCallingJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeRealm.save()

        ParameterType typeScript = new ParameterType(
                name: SCRIPT,
                className: "${SnvCallingJob.class}",
                jobDefinition: snvCallingJob.getProcessingStep().jobDefinition,
                parameterUsage: ParameterUsage.OUTPUT
                )
        assert typeScript.save()
    }

    @After
    void tearDown() {
        testData = null
        individual = null
        project = null
        seqType = null
        snvCallingInstance = null
        realm_processing = null
        snvCallingInstance2 = null
        externalScript_Calling = null
        externalScript_Joining = null
        snvJobResult = null
        removeMetaClass(CreateClusterScriptService, createClusterScriptService)
        removeMetaClass(ExecutionService, executionService)
        LsdfFilesService.metaClass = null
        assert testDirectory.deleteDir()
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
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
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
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        shouldFail(RuntimeException, { snvCallingJob.maybeSubmit(snvCallingInstance) })
    }

    @Test
    void testMaybeSubmitWithSnvCallingInput() {
        snvJobResult.delete()
        snvCallingJob.metaClass.getProcessParameterObject = { return snvCallingInstance }
        snvCallingJob.metaClass.createAndSaveSnvJobResult = { SnvCallingInstance instance, ExternalScript externalScript, SnvJobResult inputResult ->
            return true
        }
        executionService.metaClass.querySsh = { String host, int port, int timeout, String username, String password, String command, File script, String options ->
            return [PBS_ID]
        }
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
            snvCallingInstance.configFilePath.absoluteStagingPath,
            CONFIGURATION)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> return true }
        createClusterScriptService.metaClass.createTransferScript = { List<File> sourceLocations, List<File> targetLocations, List<File> linkLocations, boolean move ->

            // test that source files are correct
            File stagingBase = new File("${testDirectory}/staging/")
            File individualPathStaging = new File(stagingBase, "otp_test_project/sequencing/whole_genome_sequencing/view-by-pid/654321/")
            File sampleTypeCombinationPathStaging = new File(individualPathStaging, "snv_results/paired/sampletype1_sampletype2/")
            File instancePathStaging = new File(sampleTypeCombinationPathStaging, "2014-08-25_15h32/")

            assert sourceLocations.size() == 3
            assert sourceLocations.contains(new File(instancePathStaging, "snvs_654321_raw.vcf.gz"))
            assert sourceLocations.contains(new File(instancePathStaging, "snvs_654321_raw.vcf.gz.tbi"))
            assert sourceLocations.contains(new File(instancePathStaging, "config.txt"))

            // test that target files are correct
            File rootBase = new File("${testDirectory}/root/")
            File individualPathRoot = new File(rootBase, "otp_test_project/sequencing/whole_genome_sequencing/view-by-pid/654321/")
            File sampleTypeCombinationPathRoot = new File(individualPathRoot, "snv_results/paired/sampletype1_sampletype2/")
            File instancePathRoot = new File(sampleTypeCombinationPathRoot, "2014-08-25_15h32/")

            assert targetLocations.size() == 3
            assert targetLocations.contains(new File(instancePathRoot, "snvs_654321_raw.vcf.gz"))
            assert targetLocations.contains(new File(instancePathRoot, "snvs_654321_raw.vcf.gz.tbi"))
            assert targetLocations.contains(new File(instancePathRoot, "config.txt"))

            // test that linked files are correct
            assert linkLocations.size() == 3
            assert linkLocations.contains(new File(sampleTypeCombinationPathRoot, "snvs_654321_raw.vcf.gz"))
            assert linkLocations.contains(new File(sampleTypeCombinationPathRoot, "snvs_654321_raw.vcf.gz.tbi"))
            assert linkLocations.contains(new File(sampleTypeCombinationPathRoot, "config_calling_2014-08-25_15h32.txt"))

            return "#some script"
        }
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
        try {
            snvCallingJob.validate(snvCallingInstance)
        } finally {
            configFile.parentFile.deleteDir()
        }
    }

    @Test
    void testWriteConfigFile_InputIsNull() {
        shouldFail( IllegalArgumentException, { snvCallingJob.writeConfigFile(null) })
    }

    @Test
    void testWriteConfigFile_FileExistsAlready() {
        File configFile = testData.createConfigFileWithContentInFileSystem(
            snvCallingInstance.configFilePath.absoluteStagingPath,
            CONFIGURATION)

        assertEquals(configFile, snvCallingJob.writeConfigFile(snvCallingInstance))
        try {
            assert configFile.text == snvCallingInstance.config.configuration
        } finally {
            configFile.parentFile.deleteDir()
        }
    }

    @Test
    void testWriteConfigFile() {
        File file = snvCallingInstance.configFilePath.absoluteStagingPath
        if (file.exists()) {
            file.delete()
        }
        assertEquals(file, snvCallingJob.writeConfigFile(snvCallingInstance))
        try {
            assert file.text == CONFIGURATION
        } finally {
            assert file.delete()
        }
    }

    @Test
    void testCreateAndSaveSnvJobResult() {
        snvJobResult.delete()
        assert SnvJobResult.count() == 0
        snvCallingJob.createAndSaveSnvJobResult(snvCallingInstance, externalScript_Calling, externalScript_Joining)
        final SnvJobResult snvJobResult = exactlyOneElement(SnvJobResult.findAll())
        assert snvJobResult.snvCallingInstance == snvCallingInstance
        assert snvJobResult.processingState == SnvProcessingStates.IN_PROGRESS
        assert snvJobResult.step == SnvCallingStep.CALLING
        assert snvJobResult.externalScript == externalScript_Calling
        assert snvJobResult.chromosomeJoinExternalScript == externalScript_Joining
    }

    @Test
     void testChangeProcessingStateOfJobResult() {
         List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstanceAndStep(snvCallingInstance, SnvCallingStep.CALLING)
         assert results.size == 1
         assert results.first() == snvJobResult
         assert snvJobResult.processingState == SnvProcessingStates.IN_PROGRESS
         snvCallingJob.changeProcessingStateOfJobResult(snvCallingInstance, SnvProcessingStates.FINISHED)
         assert snvJobResult.processingState == SnvProcessingStates.FINISHED
     }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_NoResults_NoPbsOut() {
        /*
         * In this test the method 'addOutputParameter' shall not be called since pbsOutput == false.
         * To make sure that it can be recognized if the method would be called it is overwritten to throw an exception.
         */
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> throw new RuntimeException()}
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }
        assert shouldFail(RuntimeException, {
            snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance2, false)
        }).contains(SnvCallingStep.CALLING.name())
    }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_NoResults_WithPbsOut() {
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> }
        snvCallingInstance2.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return null }

        assert shouldFail(RuntimeException, {
            snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance2, false)
        }).contains(SnvCallingStep.CALLING.name())
    }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_WithResults_NoPbsOut() {
        final String errorMessage = "No Pbs output"
        /*
         * In this test the method 'addOutputParameter' shall not be called since pbsOutput == false.
         * To make sure that it can be recognized if the method would be called it is overwritten to throw an exception.
         */
        snvCallingJob.metaClass.addOutputParameter = { String name, String value -> throw new RuntimeException(errorMessage) }
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult }

        snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance, false)
    }

    @Test
    void testCheckIfResultFilesExistsOrThrowException_WithResults_WithPbsOut() {
        snvCallingInstance.metaClass.findLatestResultForSameBamFiles = { SnvCallingStep step -> return snvJobResult }

        snvCallingJob.checkIfResultFilesExistsOrThrowException(snvCallingInstance, true)
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(String identifier) {

        final String bamFileContent = 'I am a test BAM file. Nice to meet you. :)'

        final ProcessedMergedBamFile bamFile = testData.createProcessedMergedBamFile(individual, seqType, identifier)
        bamFile.fileSize = bamFileContent.length()
        assert bamFile.save(failOnError: true)

        final File file = new File(processedMergedBamFileService.destinationDirectory(bamFile), processedMergedBamFileService.fileName(bamFile))
        assert file.path.startsWith(testDirectory.path)
        file.parentFile.mkdirs()
        file.text = bamFileContent

        return bamFile
    }
}
