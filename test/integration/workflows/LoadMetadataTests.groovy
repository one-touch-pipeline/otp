package workflows

import static org.junit.Assert.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.metaData.MetaDataStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

/**
 * Currently only a test for exome data exist
 */
class LoadMetadataTests extends GroovyScriptAwareIntegrationTest {

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false
    // TODO want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs..
    int SLEEPING_TIME_IN_MINUTES = 3

    LsdfFilesService lsdfFilesService
    ExecutionService executionService

    MetaDataStartJob metaDataStartJob

    // TODO This paths should be obtained from somewhere else..  maybe from .otpproperties, but I am hardcoding for now..
    String baseDir = "WORKFLOW_ROOT/"
    // TODO change this to be dependent of the user
    String myBaseDir = "${baseDir}/MetaDataWorkflow"
    String rootPath = "${myBaseDir}/root_path/"
    String processingRootPath = "${myBaseDir}/processing_root_path/"
    String testDataDir = "${baseDir}/files/"
    String ftpDir = "${rootPath}/ftp/"

    // files to be processed by the tests
    String fastqR1Filepath = "${testDataDir}/35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz"
    String fastqR2Filepath = "${testDataDir}/35-3B_NoIndex_L007_R2_complete_filtered.fastq.gz"

    String barcode = "GATCGA"
    String fastqR1Filename = "example_${barcode}_fileR1.fastq.gz"
    String fastqR2Filename = "example_${barcode}_fileR2.fastq.gz"
    String runName = "130312_D00133_0018_ADTWTJACXX"
    String runDate = "2013-03-12"
    String metaDataFilepath = "${ftpDir}/${runName}/${runName}.fastq.tsv"

    Realm realm
    String realmName = "DKFZ"
    String realmBioquantUnixUser = "$USER"
    String realmDKFZUnixUser = "$USER"
    String realmProgramsRootPath = "/"
    String realmHost = "headnode"
    int realmPort = 22
    String realmWebHost = "https://otp.local/ngsdata/"
    String realmPbsOptions = '{"-l": {nodes: "1:lsdf", walltime: "48:00:00"}}'
    int realmTimeout = 0

    String seqCenterName = "TheSequencingCenter"
    String sampleID = "SampleIdentifier"
    String projectName = "TheProject"
    String seqTypeName = SeqTypeNames.EXOME.seqTypeName
    String softwareToolName = "CASAVA"
    String softwareToolVersion = "1.8.2"
    String pipeLineVersion = "${softwareToolName}-${softwareToolVersion}"
    String libraryLayout = "PAIRED"
    String instrumentPlatform = "Illumina"
    String instrumentModel = "HiSeq2000"
    String libraryPreparationKit = "Agilent SureSelect V3"

    String laneNo = "1"
    String baseCount = "8781211000"
    String readCount = "87812110"
    String cycleCount = "101"
    String insertSize = "162"

    private String md5sum(String filepath) {
        String cmdMd5sum = "md5sum ${filepath}"
        String output = executionService.executeCommand(realm, cmdMd5sum)
        String md5sum = output.split().first()
        return md5sum
    }

    private Map<String, String> metaDataDefault() {
        Map<String, String> metaDataDefault = [
            FASTQ_FILE: "",
            MD5: "",
            CENTER_NAME: seqCenterName,
            RUN_ID: runName,
            RUN_DATE: runDate,
            LANE_NO: "",
            BASE_COUNT: baseCount,
            READ_COUNT: readCount,
            CYCLE_COUNT: cycleCount,
            SAMPLE_ID: sampleID,
            SEQUENCING_TYPE: seqTypeName,
            INSTRUMENT_PLATFORM: instrumentPlatform,
            INSTRUMENT_MODEL: instrumentModel,
            PIPELINE_VERSION: pipeLineVersion,
            INSERT_SIZE: insertSize,
            LIBRARY_LAYOUT: libraryLayout,
            WITHDRAWN: "0",
            WITHDRAWN_DATE: "",
            COMMENT: "",
            BARCODE: "",
            LIB_PREP_KIT: ""]
        return metaDataDefault
    }

    private Map<String, String> metaData(Map<String, String> metaDataToOverride) {
        // TODO maybe add some validation to not allow wrong keys to be passed..
        Map<String, String> metaData = metaDataDefault()
        metaDataToOverride.keySet().each { String key ->
            metaData[key] = metaDataToOverride.get(key)
        }
        return metaData
    }

    private String metaDataTableHeader() {
        String[] tableHeader = MetaDataColumn.values() as String[]
        return tableHeader.join("\t") + "\n"
    }

    private String metaDataTableEntry(Map<String, String> metaData) {
        return metaData.values().join("\t")
    }

    //    // TODO construir o body
    //    private String metaDataTableBody() {
    //
    //    }

    private String metaDataTextWellFormedForExon() {
        StringBuffer sb = new StringBuffer()
        sb << metaDataTableHeader()
        sb << metaDataTableEntry(metaData([FASTQ_FILE: fastqR1Filename, MD5: md5sum(fastqR1Filepath), LANE_NO: laneNo, LIB_PREP_KIT: libraryPreparationKit])) + "\n"
        sb << metaDataTableEntry(metaData([FASTQ_FILE: fastqR2Filename, MD5: md5sum(fastqR2Filepath), LANE_NO: laneNo, LIB_PREP_KIT: libraryPreparationKit])) + "\n"
        return sb.toString()
    }

    @Before
    void setUp() {
        // Setup logic here
        realm = new Realm(
                        name: realmName,
                        env: Environment.getCurrent().getName(),
                        operationType: Realm.OperationType.DATA_MANAGEMENT,
                        cluster: Realm.Cluster.DKFZ,
                        rootPath: rootPath,
                        processingRootPath: processingRootPath,
                        programsRootPath: realmProgramsRootPath,
                        webHost: realmWebHost,
                        host: realmHost,
                        port: realmPort,
                        unixUser: realmDKFZUnixUser,
                        timeout: realmTimeout,
                        pbsOptions: realmPbsOptions
                        )
        assertNotNull(realm.save(flush: true))

        realm = new Realm(
                        name: realmName,
                        env: Environment.getCurrent().getName(),
                        operationType: Realm.OperationType.DATA_PROCESSING,
                        cluster: Realm.Cluster.DKFZ,
                        rootPath: rootPath,
                        processingRootPath: processingRootPath,
                        programsRootPath: realmProgramsRootPath,
                        webHost: realmWebHost,
                        host: realmHost,
                        port: realmPort,
                        unixUser: realmDKFZUnixUser,
                        timeout: realmTimeout,
                        pbsOptions: realmPbsOptions
                        )
        assertNotNull(realm.save(flush: true))

        //String metaDataFile = metaDataText(runName)
        String metaDataFile = metaDataTextWellFormedForExon()

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath = "${path}/${fastqR1Filename}"
        String softLinkFastqR2Filepath = "${path}/${fastqR2Filename}"

        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildFileStructure = "mkdir -p ${path}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath}"
        String cmdCreateMetadataFile = "echo '${metaDataFile}' > ${metaDataFilepath}"
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildFileStructure}; ${cmdBuildSoftLinkToFileToBeProcessed}; ${cmdCreateMetadataFile}")

        FileType fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        SampleType sampleType = new SampleType()
        sampleType.name = "some sample type"
        assertNotNull(sampleType.save(flush: true))

        Project project = new Project()
        project.name = projectName
        project.dirName = projectName
        project.realmName = realmName
        assertNotNull(project.save(flush: true))

        Individual individual = new Individual()
        individual.pid = "1234"
        individual.mockPid = "abcd"
        individual.mockFullName = "mockFullName"
        individual.type = Individual.Type.REAL
        individual.project = project
        assertNotNull(individual.save(flush: true))

        Sample sample = new Sample()
        sample.individual = individual
        sample.sampleType = sampleType
        assertNotNull(sample.save(flush: true))

        SampleIdentifier sampleIdentifier = new SampleIdentifier()
        sampleIdentifier.name = sampleID
        sampleIdentifier.sample = sample
        assertNotNull(sampleIdentifier.save(flush: true))

        SoftwareTool softwareTool = new SoftwareTool()
        softwareTool.programName = softwareToolName
        softwareTool.programVersion = softwareToolVersion
        softwareTool.type = SoftwareTool.Type.BASECALLING
        assertNotNull(softwareTool.save(flush: true))

        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier()
        softwareToolIdentifier.name = pipeLineVersion
        softwareToolIdentifier.softwareTool = softwareTool
        assertNotNull(softwareToolIdentifier.save(flush: true))

        SeqType seqType = new SeqType()
        seqType.name = seqTypeName
        seqType.libraryLayout = libraryLayout
        seqType.dirName = seqTypeName
        assertNotNull(seqType.save(flush: true))

        SeqPlatform seqPlatform = new SeqPlatform()
        seqPlatform.name = instrumentPlatform
        seqPlatform.model = instrumentModel
        assertNotNull(seqPlatform.save(flush: true))

        SeqPlatformModelIdentifier seqPlatformModelIdentifier = new SeqPlatformModelIdentifier()
        seqPlatformModelIdentifier.name = instrumentModel
        seqPlatformModelIdentifier.seqPlatform = seqPlatform
        assertNotNull(seqPlatformModelIdentifier.save(flush: true))

        SeqCenter seqCenter = new SeqCenter(
                        name: seqCenterName,
                        dirName: seqCenterName
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        Run run = new Run()
        run.name = runName
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        assertNotNull(run.save([flush: true, failOnError: true]))

        RunSegment runSegment = new RunSegment()
        runSegment.initialFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.currentFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.dataPath = ftpDir
        runSegment.filesStatus = RunSegment.FilesStatus.NEEDS_INSTALLATION
        runSegment.mdPath = ftpDir
        runSegment.run = run
        assertNotNull(runSegment.save([flush: true, failOnError: true]))

        println "path : " + (new File(metaDataFilepath)).getAbsolutePath()
        assertTrue(new File(metaDataFilepath).exists())
    }

    @After
    void tearDown() {
        executionService.executeCommand(realm, cleanUpTestFoldersCommand())
    }

    @Ignore
    void testExomeMetadata() {
        run("scripts/ExomeEnrichmentKit/LoadExomeEnrichmentKits.groovy")
        run("scripts/MetaDataWorkflow.groovy")

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()

        // TODO hack to be able to star the workflow
        metaDataStartJob.setJobExecutionPlan(jobExecutionPlan)

        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        assertTrue(workflowFinishedSucessfully)
    }

    /**
     * Returns a comand to clean up the rootPath and processingRootPath
     * @return Command to clean up used folders
     */
    String cleanUpTestFoldersCommand() {
        return "rm -rf ${rootPath}/* ${processingRootPath}/*"
    }

    /**
     * Pauses the test until the workflow is finished or the timeout is reached
     * @return true if the process is finished, false otherwise
     */
    boolean waitUntilWorkflowIsOverOrTimeout(int timeout) {
        println "Started to wait (until workflow is over or timeout)"
        int timeCount = 0
        boolean finished = false
        while (!finished && (timeCount < timeout)) {
            println "waiting ... "
            timeCount++
            sleep(60000)
            finished = isProcessFinished()
        }
        return finished
    }

    // TODO maybe we can make this a sub class and put this method in parent..
    /**
     * Checks if the process created by the test is already finished and retrieves corresponding value
     * @return true if the process is finished, false otherwise
     */
    boolean isProcessFinished() {
        //TODO there should be not more than one .. can make assert to be sure
        List<Process> processes = Process.list()
        boolean finished = false
        if (processes.size() > 0) {
            Process process = processes.first()
            // Required otherwise will never detect the change..
            process.refresh()
            finished = process?.finished
        }
        return finished
    }

    /**
     * Helper to see logs at console ( besides seeing at the reports in the end)
     * @msg Message to be shown
     */
    void println(String msg) {
        log.debug(msg)
        System.out.println(msg)
    }
}
