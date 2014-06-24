package workflows

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import static org.junit.Assert.*
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.dataInstallation.DataInstallationStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

/**
 * Currently only a test for exome data exist
 */
class DataInstallationWorkflowTests extends GroovyScriptAwareIntegrationTest {

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // TODO  ( jira: OTP-566)  want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs plus some buffer..
    final int SLEEPING_TIME_IN_MINUTES = 10
    final String PBS_WALLTIME = "00:05:00"

    LsdfFilesService lsdfFilesService
    ExecutionService executionService

    DataInstallationStartJob dataInstallationStartJob

    // TODO ( jira: OTP-566) This paths should be obtained from somewhere else..  maybe from .otpproperties, but I am hardcoding for now..
    String baseDir = "WORKFLOW_ROOT/"
    // TODO ( jira: OTP-566) change this to be dependent of the user
    String myBaseDir = "${baseDir}/DataInstallationWorkflow"
    String rootPath = "${myBaseDir}/root_path/"
    String processingRootPath = "${myBaseDir}/processing_root_path/"
    String testDataDir = "${baseDir}/files/"
    String ftpDir = "${rootPath}/ftp/"
    String loggingPath = "${myBaseDir}/logging_root_path"

    // files to be processed by the tests
    String fastqR1Filepath = "${testDataDir}/35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz"
    String fastqR2Filepath = "${testDataDir}/35-3B_NoIndex_L007_R2_complete_filtered.fastq.gz"

    String fastqR1Filename = "example_fileR1.fastq.gz"
    String fastqR2Filename = "example_fileR2.fastq.gz"
    String runName = "130312_D00133_0018_ADTWTJACXX"
    String runDate = "2013-03-12"
    Realm realm
    String realmProgramsRootPath = "/"
    String seqCenterName = "TheSequencingCenter"
    String sampleID = "1234_AB_CD_E"
    String projectName = "TheProject"

    String softwareToolName = "CASAVA"
    String softwareToolVersion = "1.8.2"
    String pipeLineVersion = "${softwareToolName}-${softwareToolVersion}"
    String libraryLayout = "PAIRED"
    String instrumentPlatform = "Illumina"
    String instrumentModel = "HiSeq2000"
    String libraryPreparationKit = "Agilent SureSelect V3"
    String laneNo = "1"
    long baseCount = 8781211000
    long readCount = 87812110
    int insertSize = 162

    Run run
    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SoftwareTool softwareTool
    SoftwareToolIdentifier softwareToolIdentifier
    RunSegment runSegment
    Project project
    FileType fileType

    private String md5sum(String filepath) {
        String cmdMd5sum = "md5sum ${filepath}"
        String output = executionService.executeCommand(realm, cmdMd5sum)
        String md5sum = output.split().first()
        return md5sum
    }

    @Before
    void setUp() {
        // Setup logic here
        super.createUserAndRoles()

        Map paths = [
            rootPath: rootPath,
            processingRootPath: processingRootPath,
            programsRootPath: realmProgramsRootPath,
            loggingRootPath: loggingPath,
        ]

        realm = DomainFactory.createRealmDataManagementDKFZ(paths).save(flush: true)
        realm = DomainFactory.createRealmDataProcessingDKFZ(paths).save(flush: true)

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath = "${path}/${fastqR1Filename}"
        String softLinkFastqR2Filepath = "${path}/${fastqR2Filename}"

        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildFileStructure = "mkdir -p ${path}"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath}; ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath}"

        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildFileStructure}; ${cmdBuildSoftLinkToFileToBeProcessed}")

        fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        SampleType sampleType = new SampleType()
        sampleType.name = "someSampleType"
        assertNotNull(sampleType.save(flush: true))

        project = new Project()
        project.name = projectName
        project.dirName = projectName
        project.realmName = realm.name
        assertNotNull(project.save(flush: true))

        Individual individual = new Individual()
        individual.pid = "1234"
        individual.mockPid = "abcd"
        individual.mockFullName = "mockFullName"
        individual.type = Individual.Type.REAL
        individual.project = project
        assertNotNull(individual.save(flush: true))

        sample = new Sample()
        sample.individual = individual
        sample.sampleType = sampleType
        assertNotNull(sample.save(flush: true))

        SampleIdentifier sampleIdentifier = new SampleIdentifier()
        sampleIdentifier.name = sampleID
        sampleIdentifier.sample = sample
        assertNotNull(sampleIdentifier.save(flush: true))

        softwareTool = new SoftwareTool()
        softwareTool.programName = softwareToolName
        softwareTool.programVersion = softwareToolVersion
        softwareTool.type = SoftwareTool.Type.BASECALLING
        assertNotNull(softwareTool.save(flush: true))

        softwareToolIdentifier = new SoftwareToolIdentifier()
        softwareToolIdentifier.name = pipeLineVersion
        softwareToolIdentifier.softwareTool = softwareTool
        assertNotNull(softwareToolIdentifier.save(flush: true))

        seqPlatform = new SeqPlatform()
        seqPlatform.name = instrumentPlatform
        seqPlatform.model = instrumentModel
        assertNotNull(seqPlatform.save(flush: true))

        SeqPlatformModelIdentifier seqPlatformModelIdentifier = new SeqPlatformModelIdentifier()
        seqPlatformModelIdentifier.name = instrumentModel
        seqPlatformModelIdentifier.seqPlatform = seqPlatform
        assertNotNull(seqPlatformModelIdentifier.save(flush: true))

        SeqCenter seqCenter = new SeqCenter()
        seqCenter.name = seqCenterName
        seqCenter.dirName = seqCenterName
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run()
        run.name = runName
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        assertNotNull(run.save([flush: true]))

        runSegment = new RunSegment()
        runSegment.metaDataStatus = RunSegment.Status.COMPLETE
        runSegment.initialFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.currentFormat = RunSegment.DataFormat.FILES_IN_DIRECTORY
        runSegment.dataPath = ftpDir
        runSegment.filesStatus = RunSegment.FilesStatus.NEEDS_INSTALLATION
        runSegment.mdPath = ftpDir
        runSegment.run = run
        runSegment.align = true
        assertNotNull(runSegment.save([flush: true]))
    }

    @After
    void tearDown() {
        executionService.executeCommand(realm, cleanUpTestFoldersCommand())
    }

    DataFile createDataFile(SeqTrack seqTrack, String fastqFilename, String fastqFilepath) {
        DataFile dataFile = new DataFile()
        dataFile.fileName = fastqFilename
        dataFile.runSegment = runSegment
        dataFile.used = true // is this file used in any seqTrack
        dataFile.project = project
        dataFile.run = run
        dataFile.md5sum = md5sum(fastqFilepath)
        dataFile.seqTrack = seqTrack
        dataFile.fileType = fileType
        dataFile.vbpFileName = fastqFilename
        dataFile.pathName = ""  // TODO check what is going on here and why this is needed..
        dataFile.fileExists = true
        dataFile.fileSize = 100
        assertNotNull(dataFile.save([flush: true]))
        return dataFile
    }

    SeqType createSeqType(String name, String dirName) {
        SeqType seqType = new SeqType()
        seqType.name = name
        seqType.libraryLayout = libraryLayout
        seqType.dirName = dirName
        assertNotNull(seqType.save([flush: true]))
        return seqType
    }

    // TODO  (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    void testDataInstallation() {
        SpringSecurityUtils.doWithAuth("admin") {
            run("scripts/DataInstallationWorkflow.groovy")
        }
        SeqType seqType = createSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, "SeqTypeDir")

        SeqTrack seqTrack = new SeqTrack()
        seqTrack.laneId = laneNo
        seqTrack.nBasePairs = baseCount
        seqTrack.nReads = readCount
        seqTrack.insertSize = insertSize
        seqTrack.run = run
        seqTrack.sample = sample
        seqTrack.seqType = seqType
        seqTrack.seqPlatform = seqPlatform
        seqTrack.pipelineVersion = softwareTool
        assertNotNull(seqTrack.save([flush: true]))

        createDataFile(seqTrack, fastqR1Filename, fastqR1Filepath)
        createDataFile(seqTrack, fastqR2Filename, fastqR2Filepath)

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()
        // hack to be able to star the workflow
        dataInstallationStartJob.setJobExecutionPlan(jobExecutionPlan)
        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)

        seqTrack.refresh()
        assertTrue(workflowFinishedSucessfully)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack.alignmentState)
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack.fastqcState)
    }

    // TODO  (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    void testChipSeqInstallation() {
        SpringSecurityUtils.doWithAuth("admin") {
            run("scripts/DataInstallationWorkflow.groovy")
        }
        SeqType seqType = createSeqType(SeqTypeNames.CHIP_SEQ.seqTypeName, "chip_seq_sequencing")

        // creating required Antibody target objects
        AntibodyTarget antibodyTarget = new AntibodyTarget()
        antibodyTarget.name = "antibody1"
        assertNotNull(antibodyTarget.save([flush: true]))

        ChipSeqSeqTrack seqTrack = new ChipSeqSeqTrack()
        seqTrack.antibodyTarget = antibodyTarget
        seqTrack.laneId = laneNo
        seqTrack.nBasePairs = baseCount
        seqTrack.nReads = readCount
        seqTrack.insertSize = insertSize
        seqTrack.run = run
        seqTrack.sample = sample
        seqTrack.seqType = seqType
        seqTrack.seqPlatform = seqPlatform
        seqTrack.pipelineVersion = softwareTool
        assertNotNull(seqTrack.save([flush: true]))

        createDataFile(seqTrack, fastqR1Filename, fastqR1Filepath)
        createDataFile(seqTrack, fastqR2Filename, fastqR2Filepath)

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()
        //hack to be able to star the workflow
        dataInstallationStartJob.setJobExecutionPlan(jobExecutionPlan)
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

    // TODO  ( jira: OTP-566) maybe we can make this a sub class and put this method in parent..
    /**
     * Checks if the process created by the test is already finished and retrieves corresponding value
     * @return true if the process is finished, false otherwise
     */
    boolean isProcessFinished() {
        //TODO ( jira: OTP-566) there should be not more than one .. can make assert to be sure
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
}
