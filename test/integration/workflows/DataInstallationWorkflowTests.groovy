package workflows

import static org.junit.Assert.*
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.joda.time.Duration
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.dataInstallation.DataInstallationStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Currently only a test for exome data exist
 */
class DataInstallationWorkflowTests extends AbstractWorkflowTest {

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // TODO  ( jira: OTP-566)  want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs plus some buffer..
    final Duration TIMEOUT = Duration.standardMinutes(30)
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
        String cmdBuildFileStructure = "mkdir -p -m 2750 ${path} ${loggingPath}/log/status/"
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath} && ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath}"

        assert '0\n' == executionService.executeCommand(realm, "${cmdCleanUp} && ${cmdBuildFileStructure} && ${cmdBuildSoftLinkToFileToBeProcessed}; echo \$?")

        fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        SampleType sampleType = new SampleType()
        sampleType.name = "someSampleType"
        assertNotNull(sampleType.save(flush: true))

        project = TestData.createProject()
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

        seqPlatform = SeqPlatform.build()

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

    DataFile createDataFile(SeqTrack seqTrack, Integer readNumber, String fastqFilename, String fastqFilepath) {
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
        dataFile.readNumber = readNumber
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
    @Test
    void testDataInstallation_FilesHaveToBeCopied() {
        SeqTrack seqTrack = createWholeGenomeSetup()

        setExecutionPlan()
        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT)

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    // TODO  (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    @Test
    void testDataInstallation_FilesHaveToBeLinked() {
        SeqTrack seqTrack = createWholeGenomeSetup()
        seqTrack.linkedExternally = true
        assert seqTrack.save(flush: true)

        setExecutionPlan()
        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT)

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    // TODO  (jira: OTP-640) this ignore is here because of workflows tests are not transactional and so we cannot run multiple tests with clean database yet (We need to discovered best way to do it)
    // so at this moment only one test could be run at moment, all the others have to be commented
    @Ignore
    @Test
    void testChipSeqInstallation() {
        SpringSecurityUtils.doWithAuth("admin") {
            run("scripts/workflows/DataInstallationWorkflow.groovy")
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

        createDataFiles(seqTrack)

        setExecutionPlan()
        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT)
    }

    @Ignore
    @Test
    void testDataInstallationWithFastTrack() {
        SpringSecurityUtils.doWithAuth("admin") {
            run("scripts/workflows/DataInstallationWorkflow.groovy")
        }
        project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert project.save(flush: true)

        SeqType seqType = createSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, "SeqTypeDir")

        SeqTrack seqTrack = createSeqTrack(seqType)
        createDataFiles(seqTrack)

        setExecutionPlan()
        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT)

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    /**
     * Returns a comand to clean up the rootPath and processingRootPath
     * @return Command to clean up used folders
     */
    String cleanUpTestFoldersCommand() {
        return "rm -rf ${rootPath} ${processingRootPath} ${loggingPath}"
    }

    private SeqTrack createSeqTrack(SeqType seqType) {
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
        return seqTrack
    }

    private void checkThatWorkflowWasSuccessful(SeqTrack seqTrack) {
        seqTrack.refresh()
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack.fastqcState)
    }

    private void createDataFiles(SeqTrack seqTrack) {
        createDataFile(seqTrack, 1, fastqR1Filename, fastqR1Filepath)
        createDataFile(seqTrack, 2, fastqR2Filename, fastqR2Filepath)
    }

    private void setExecutionPlan() {
        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()
        // hack to be able to star the workflow
        dataInstallationStartJob.setJobExecutionPlan(jobExecutionPlan)
    }

    private SeqTrack createWholeGenomeSetup() {
        SpringSecurityUtils.doWithAuth("admin") {
            run("scripts/workflows/DataInstallationWorkflow.groovy")
        }
        SeqType seqType = createSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, "SeqTypeDir")

        SeqTrack seqTrack = createSeqTrack(seqType)
        createDataFiles(seqTrack)
        return seqTrack
    }

}
