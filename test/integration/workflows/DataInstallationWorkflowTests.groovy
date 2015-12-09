package workflows

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.joda.time.Duration
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@Ignore
@TestMixin(IntegrationTestMixin)
class DataInstallationWorkflowTests extends WorkflowTestCase {

    LsdfFilesService lsdfFilesService


    // files to be processed by the tests
    String fastqR1Filepath
    String fastqR2Filepath
    String bamFilepath

    String fastqR1Filename = "example_fileR1.fastq.gz"
    String fastqR2Filename = "example_fileR2.fastq.gz"
    String bamFilename = "example_bamFile.mdup.bam"
    String runName = "130312_D00133_0018_ADTWTJACXX"
    String runDate = "2013-03-12"
    String seqCenterName = "TheSequencingCenter"
    String sampleID = "1234_AB_CD_E"
    String projectName = "TheProject"

    String softwareToolName = "CASAVA"
    String softwareToolVersion = "1.8.2"
    String pipeLineVersion = "${softwareToolName}-${softwareToolVersion}"
    String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED
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
    FileType bamFileType

    private String md5sum(String filepath) {
        String cmdMd5sum = "md5sum ${filepath}"
        String output = executionService.executeCommand(realm, cmdMd5sum)
        String md5sum = output.split().first()
        return md5sum
    }

    @Before
    void setUp() {
        fastqR1Filepath = "${testDataDir}/35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz"
        fastqR2Filepath = "${testDataDir}/35-3B_NoIndex_L007_R2_complete_filtered.fastq.gz"

        String path = "${ftpDir}/${runName}"
        String softLinkFastqR1Filepath = "${path}/${fastqR1Filename}"
        String softLinkFastqR2Filepath = "${path}/${fastqR2Filename}"
        bamFilepath = "${path}/${bamFilename}"

        createDirectoriesString([path])
        String cmdBuildSoftLinkToFileToBeProcessed = "ln -s ${fastqR1Filepath} ${softLinkFastqR1Filepath} && ln -s ${fastqR2Filepath} ${softLinkFastqR2Filepath} && echo 'something' > ${bamFilepath}"
        assert '0\n' == executionService.executeCommand(realm, "${cmdBuildSoftLinkToFileToBeProcessed}; echo \$?")

        fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = "/sequence/"
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        bamFileType = new FileType(
                type: FileType.Type.ALIGNMENT,
                subType: "bam",
                vbpPath: "/bam/",
                signature: ".bam",
        )
        assert bamFileType.save(flush: true)

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

        seqPlatform = new SeqPlatform(name: 'seqPlatformName')
        assert seqPlatform.save(flush: true)

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

    DataFile createBamDataFile(SeqTrack seqTrack, String bamFilename, String bamFilepath) {
        AlignmentParams alignmentParams = new AlignmentParams(pipeline: softwareTool)
        assert alignmentParams.save(flush: true)

        AlignmentLog alignmentLog = new AlignmentLog(seqTrack: seqTrack, alignmentParams: alignmentParams)
        assert alignmentLog.save(flush: true)

        DataFile dataFile = new DataFile(
            fileName: bamFilename,
            vbpFileName: bamFilename,
            run: run,
            runSegment: runSegment,
            used: true,
            project: project,
            md5sum: md5sum(bamFilepath),
            seqTrack: null,
            fileType: bamFileType,
            pathName: "",
            fileExists: true,
            fileSize: 100,
            alignmentLog: alignmentLog,
        )
        assert dataFile.save(flush: true)
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

    @Test
    void testDataInstallation_FilesHaveToBeCopied() {
        SeqTrack seqTrack = createWholeGenomeSetup()

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    @Test
    void testDataInstallation_FilesHaveToBeLinked() {
        SeqTrack seqTrack = createWholeGenomeSetup()
        seqTrack.linkedExternally = true
        assert seqTrack.save(flush: true)

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    @Test
    void testChipSeqInstallation() {
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

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    @Test
    void testDataInstallationWithFastTrack() {
        project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert project.save(flush: true)

        SeqType seqType = createSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, "SeqTypeDir")

        SeqTrack seqTrack = createSeqTrack(seqType)
        createDataFiles(seqTrack)

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
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

        DataFile.list().collect {
            [
                lsdfFilesService.getFileFinalPath(it),
                lsdfFilesService.getFileViewByPidPath(it)
            ]
        }.flatten().each {
            WaitingFileUtils.waitUntilExists(new File(it))
        }
    }

    private void createDataFiles(SeqTrack seqTrack) {
        createDataFile(seqTrack, 1, fastqR1Filename, fastqR1Filepath)
        createDataFile(seqTrack, 2, fastqR2Filename, fastqR2Filepath)
        createBamDataFile(seqTrack, bamFilename, bamFilepath)
    }

    private SeqTrack createWholeGenomeSetup() {
        SeqType seqType = createSeqType(SeqTypeNames.WHOLE_GENOME.seqTypeName, "SeqTypeDir")

        SeqTrack seqTrack = createSeqTrack(seqType)
        createDataFiles(seqTrack)
        return seqTrack
    }

    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/DataInstallationWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(30)
    }
}
