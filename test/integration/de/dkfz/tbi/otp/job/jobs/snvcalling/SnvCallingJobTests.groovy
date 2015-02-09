package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT
import static org.junit.Assert.*
import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import de.dkfz.tbi.otp.dataprocessing.*
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
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

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
    String UNIQUE_PATH = HelperUtils.getUniqueString()

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

        ProcessingOption processingOptionWGS = new ProcessingOption(
                name:"PBS_snvPipeline_WGS",
                type: "DKFZ",
                value:'{"-l": {walltime: "20:00:00"}}',
                dateCreated: new Date(),
                comment:"according to the CO group (Ivo) 20h is enough for the snv WGS jobs",
        )
        assert processingOptionWGS.save()

        ProcessingOption processingOptionWES = new ProcessingOption(
                name:"PBS_snvPipeline_WES",
                type: "DKFZ",
                value:'{"-l": {walltime: "5:00:00"}}',
                dateCreated: new Date(),
                comment:"according to the CO group (Ivo) 5h is enough for the snv WES jobs",
        )
        assert processingOptionWES.save()

        project = testData.project
        individual = testData.individual
        seqType = testData.seqType

        processedMergedBamFile1 = createProcessedMergedBamFile("1")
        assert processedMergedBamFile1.save()
        processedMergedBamFile2 = createProcessedMergedBamFile("2")
        assert processedMergedBamFile2.save()

        SnvConfig snvConfig = new SnvConfig(
                project: project,
                seqType: seqType,
                configuration: CONFIGURATION)
        assert snvConfig.save()

        SampleTypePerProject.build(project: project, sampleType: processedMergedBamFile1.sampleType, category: SampleType.Category.DISEASE)

        SamplePair samplePair = new SamplePair(
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

        snvCallingInstance2 = DomainFactory.createSnvCallingInstance(
                instanceName: OTHER_INSTANCE_NAME,
                config: snvConfig,
                sampleType1BamFile: processedMergedBamFile1,
                sampleType2BamFile: processedMergedBamFile2,
                samplePair: samplePair)
        assert snvCallingInstance2.save()

        externalScript_Calling = new ExternalScript(
                scriptIdentifier: SnvCallingStep.CALLING.externalScriptIdentifier,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/calling.sh",
                author: "otptest",
                )
        assert externalScript_Calling.save()

        externalScript_Joining = new ExternalScript(
                scriptIdentifier: SnvCallingJob.CHROMOSOME_VCF_JOIN_SCRIPT_IDENTIFIER,
                scriptVersion: 'v1',
                filePath: "/tmp/scriptLocation/joining.sh",
                author: "otptest",
                )
        assert externalScript_Joining.save()

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
        processedMergedBamFile1 = null
        processedMergedBamFile2 = null
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
                File tumorBamFile = new File(processedMergedBamFileService.destinationDirectory(processedMergedBamFile1), processedMergedBamFileService.fileName(processedMergedBamFile1))
                File controlBamFile = new File(processedMergedBamFileService.destinationDirectory(processedMergedBamFile2), processedMergedBamFileService.fileName(processedMergedBamFile2))

                String scriptCommandPart = "/tmp/scriptLocation/calling.sh"

                String qsubParameterCommandPart = "-v CONFIG_FILE=" +
                        "${snvCallingInstance.configFilePath.absoluteStagingPath}," +
                        "pid=654321," +
                        "PID=654321," +
                        "TUMOR_BAMFILE_FULLPATH_BP=${tumorBamFile}," +
                        "CONTROL_BAMFILE_FULLPATH_BP=${controlBamFile}," +
                        "TOOL_ID=snvCalling," +
                        "PARM_CHR_INDEX=${chromosome}," +
                        "FILENAME_VCF_SNVS=${snvFile}"

                assert command.contains(scriptCommandPart)
                assert command.contains(qsubParameterCommandPart)

            } else {
                File snvFile = new OtpPath(snvCallingInstance.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(snvCallingInstance.individual, null)).absoluteStagingPath
                String scriptCommandPart = "/tmp/scriptLocation/joining.sh; " +
                        "md5sum ${snvFile} > ${snvFile}.md5sum"
                assert command.contains(scriptCommandPart)
            }

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
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
        }
    }

    @Test
    void testValidateWithSnvCallingInput() {
        File configFile = testData.createConfigFileWithContentInFileSystem(
            snvCallingInstance.configFilePath.absoluteStagingPath,
            CONFIGURATION)

        createResultFile(snvCallingInstance, SnvCallingStep.CALLING)
        createMD5SUMFile(snvCallingInstance, SnvCallingStep.CALLING)

        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file -> return true }
        createClusterScriptService.metaClass.createTransferScript = { List<File> sourceLocations, List<File> targetLocations, List<File> linkLocations, boolean move ->

            // test that source files are correct
            File stagingBase = new File("${testDirectory}/staging/")
            File individualPathStaging = new File(stagingBase, "otp_test_project/sequencing/whole_genome_sequencing/view-by-pid/654321/")
            File samplePairPathStaging = new File(individualPathStaging, "snv_results/paired/sampletype1_sampletype2/")
            File instancePathStaging = new File(samplePairPathStaging, "2014-08-25_15h32/")

            assert sourceLocations.size() == 3
            assert sourceLocations.contains(new File(instancePathStaging, "snvs_654321_raw.vcf.gz"))
            assert sourceLocations.contains(new File(instancePathStaging, "snvs_654321_raw.vcf.gz.tbi"))
            assert sourceLocations.contains(new File(instancePathStaging, "config.txt"))

            // test that target files are correct
            File rootBase = new File("${testDirectory}/root/")
            File individualPathRoot = new File(rootBase, "otp_test_project/sequencing/whole_genome_sequencing/view-by-pid/654321/")
            File samplePairPathRoot = new File(individualPathRoot, "snv_results/paired/sampletype1_sampletype2/")
            File instancePathRoot = new File(samplePairPathRoot, "2014-08-25_15h32/")

            assert targetLocations.size() == 3
            assert targetLocations.contains(new File(instancePathRoot, "snvs_654321_raw.vcf.gz"))
            assert targetLocations.contains(new File(instancePathRoot, "snvs_654321_raw.vcf.gz.tbi"))
            assert targetLocations.contains(new File(instancePathRoot, "config.txt"))

            // test that linked files are correct
            assert linkLocations.size() == 3
            assert linkLocations.contains(new File(samplePairPathRoot, "snvs_654321_raw.vcf.gz"))
            assert linkLocations.contains(new File(samplePairPathRoot, "snvs_654321_raw.vcf.gz.tbi"))
            assert linkLocations.contains(new File(samplePairPathRoot, "config_calling_2014-08-25_15h32.txt"))

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
        LsdfFilesServiceTests.mockCreateDirectory(lsdfFilesService)
        File file = snvCallingInstance.configFilePath.absoluteStagingPath
        if (file.exists()) {
            file.delete()
        }
        assertEquals(file, snvCallingJob.writeConfigFile(snvCallingInstance))
        try {
            assert file.text == CONFIGURATION
        } finally {
            assert file.delete()
            LsdfFilesServiceTests.removeMockFileService(lsdfFilesService)
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
         createResultFile(snvCallingInstance, SnvCallingStep.CALLING)
         createMD5SUMFile(snvCallingInstance, SnvCallingStep.CALLING)
         List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstanceAndStep(snvCallingInstance, SnvCallingStep.CALLING)
         assert results.size == 1
         assert results.first() == snvJobResult
         assert snvJobResult.processingState == SnvProcessingStates.IN_PROGRESS
         snvCallingJob.changeProcessingStateOfJobResult(snvCallingInstance, SnvProcessingStates.FINISHED)
         assert snvJobResult.processingState == SnvProcessingStates.FINISHED
     }

    @Test
    void testAddFileInformationToJobResult() {
        File file = createResultFile(snvCallingInstance, SnvCallingStep.CALLING)
        File md5sum = createMD5SUMFile(snvCallingInstance, SnvCallingStep.CALLING)
        SnvJobResult results = exactlyOneElement(SnvJobResult.findAllBySnvCallingInstanceAndStep(snvCallingInstance, SnvCallingStep.CALLING))

        snvCallingJob.addFileInformationToJobResult(results)
        assert results.fileSize == file.size()
        assert results.md5sum == CreateFileHelper.MD5SUM
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


    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_InputIsNull() {
        assert shouldFail(IllegalArgumentException,{
            snvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(null)
        }).contains("The input seqType must not be null in method getSnvPBSOptionsNameSeqTypeSpecific")
    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_ExomeSeqType() {
            assert "snvPipeline_WES" == snvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(testData.exomeSeqType)

    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_WholeGenomeSeqType() {
            assert "snvPipeline_WGS" == snvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(testData.seqType)

    }

    @Test
    void testGetSnvPBSOptionsNameSeqTypeSpecific_OtherSeqType() {
        SeqType otherSeqType = new SeqType(
        name: "ChIP Seq",
        libraryLayout: "PAIRED",
        dirName: "chip_seq_sequencing")
        assert otherSeqType.save(flush: true)

        assert shouldFail(RuntimeException,{
            snvCallingJob.getSnvPBSOptionsNameSeqTypeSpecific(otherSeqType)
        }).contains("There are no PBS Options available for the SNV pipeline for seqtype")
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
