package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.utils.HelperUtils

class LsdfFilesServiceTests {

    LsdfFilesService lsdfFilesService

    String ftpDir = "/tmp"
    String fastqR1Filename = "example_fileR1.fastq.gz"
    String runName = "130312_D00133_0018_ADTWTJACXX"
    String runDate = "2013-03-12"
    String realmName = "DKFZ"
    String seqCenterName = "TheSequencingCenter"
    String sampleID = "SampleIdentifier"
    String sampleTypeName = "someSampleType"
    String projectName = "TheProject"

    String individualPid = "1234"
    final String VBP_PATH = "/sequence/"
    final String VIEW_BY_PID_PATH = "view-by-pid"

    String softwareToolName = "CASAVA"
    String softwareToolVersion = "1.8.2"
    String pipeLineVersion = "${softwareToolName}-${softwareToolVersion}"
    String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED
    String instrumentPlatform = "Illumina"
    String instrumentModel = "HiSeq2000"
    String libraryPreparationKit = "Agilent SureSelect V3"
    String laneNo = "1"
    long baseCount = 8781211000
    int insertSize = 162

    Run run
    Sample sample
    SeqPlatform seqPlatform
    SoftwareTool softwareTool
    RunSegment runSegment
    Project project
    FileType fileType


    @Before
    void setUp() {
        // Setup logic here

        fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        fileType.subType = "fastq"
        fileType.vbpPath = VBP_PATH
        fileType.signature = ".fastq"
        assertNotNull(fileType.save(flush: true))

        SampleType sampleType = new SampleType()
        sampleType.name = sampleTypeName
        assertNotNull(sampleType.save(flush: true))

        project = DomainFactory.createProject()
        project.name = projectName
        project.dirName = projectName
        project.realmName = realmName
        assertNotNull(project.save(flush: true))

        Individual individual = new Individual()
        individual.pid = individualPid
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

        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier()
        softwareToolIdentifier.name = pipeLineVersion
        softwareToolIdentifier.softwareTool = softwareTool
        assertNotNull(softwareToolIdentifier.save(flush: true))


        SeqPlatformModelLabel seqPlatformModelLabel = new SeqPlatformModelLabel()
        seqPlatformModelLabel.name = instrumentModel
        assertNotNull(seqPlatformModelLabel.save(flush: true))

        seqPlatform = new SeqPlatform()
        seqPlatform.name = instrumentPlatform
        seqPlatform.seqPlatformModelLabel= seqPlatformModelLabel
        assertNotNull(seqPlatform.save(flush: true))

        SeqCenter seqCenter = new SeqCenter()
        seqCenter.name = seqCenterName
        seqCenter.dirName = seqCenterName
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run()
        run.name = runName
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        assertNotNull(run.save([flush: true]))

        runSegment = new RunSegment()
        assertNotNull(runSegment.save([flush: true]))
    }

    DataFile createDataFile(SeqTrack seqTrack, String fastqFilename) {
        DataFile dataFile = new DataFile()
        dataFile.fileName = fastqFilename
        dataFile.initialDirectory = TestCase.getUniqueNonExistentPath().path
        dataFile.runSegment = runSegment
        dataFile.used = true // is this file used in any seqTrack
        dataFile.project = project
        dataFile.run = run
        dataFile.md5sum = HelperUtils.randomMd5sum
        dataFile.seqTrack = seqTrack
        dataFile.fileType = fileType
        dataFile.vbpFileName = fastqFilename
        dataFile.pathName = ""  // TODO check what is going on here and why this is needed..
        dataFile.mateNumber = 1
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

    @Test
    void testGetFileViewByPidRelativeDirectorySeqTrackAboutAlignmentLog() {
        final String SEQ_TYPE = "OtherThanChipSeq"
        final String SEQ_TYPE_SEQUENCING_DIR = SEQ_TYPE
        SeqType seqType = createSeqType(SEQ_TYPE, SEQ_TYPE_SEQUENCING_DIR)

        SeqTrack seqTrack = createSeqTrack(seqType: seqType)
        DataFile dataFile = createDataFile(seqTrack, fastqR1Filename)

        AlignmentParams alignmentParams = new AlignmentParams(
                pipeline: softwareTool
                )
        assertNotNull(alignmentParams.save([flush: true]))

        AlignmentLog alignmentLog = new AlignmentLog(
                alignmentParams: alignmentParams,
                seqTrack: seqTrack
                )
        assertNotNull(alignmentLog.save([flush: true]))

        dataFile.alignmentLog = alignmentLog
        dataFile.seqTrack = null
        assertNotNull(dataFile.save([flush: true]))

        String correctPath = "${SEQ_TYPE_SEQUENCING_DIR}/${VIEW_BY_PID_PATH}/${individualPid}/${sampleTypeName.toLowerCase()}/${seqType.libraryLayout.toLowerCase()}/run${runName}/${VBP_PATH}/"
        String path = lsdfFilesService.getFileViewByPidRelativeDirectory(dataFile)
        assertEquals(new File(correctPath).path, new File(path).path)
    }

    @Test
    void testGetFileViewByPidRelativeDirectory() {
        final String SEQ_TYPE = "OtherThanChipSeq"
        final String SEQ_TYPE_SEQUENCING_DIR = SEQ_TYPE
        SeqType seqType = createSeqType(SEQ_TYPE, SEQ_TYPE_SEQUENCING_DIR)

        SeqTrack seqTrack = createSeqTrack(seqType: seqType)
        DataFile dataFile = createDataFile(seqTrack, fastqR1Filename)
        String correctPath = "${SEQ_TYPE_SEQUENCING_DIR}/${VIEW_BY_PID_PATH}/${individualPid}/${sampleTypeName.toLowerCase()}/${seqType.libraryLayout.toLowerCase()}/run${runName}/${VBP_PATH}/"
        String path = lsdfFilesService.getFileViewByPidRelativeDirectory(dataFile)
        assertEquals(new File(correctPath).path, new File(path).path)
    }

    @Test
    void testGetFileViewByPidRelativeDirectoryChipSeq() {
        final String SEQ_TYPE = SeqTypeNames.CHIP_SEQ.seqTypeName
        final String CHIP_SEQ_SEQUENCING_DIR = "chip_seq_sequencing"
        final String ANTIBODY_TARGET_NAME = "antibody1"
        SeqType seqType = createSeqType(SEQ_TYPE, CHIP_SEQ_SEQUENCING_DIR)
        // creating required Antibody target objects
        AntibodyTarget antibodyTarget = new AntibodyTarget()
        antibodyTarget.name = ANTIBODY_TARGET_NAME
        assertNotNull(antibodyTarget.save([flush: true]))

        ChipSeqSeqTrack seqTrack = new ChipSeqSeqTrack()
        seqTrack.antibodyTarget = antibodyTarget
        seqTrack.laneId = laneNo
        seqTrack.nBasePairs = baseCount
        seqTrack.insertSize = insertSize
        seqTrack.run = run
        seqTrack.sample = sample
        seqTrack.seqType = seqType
        seqTrack.seqPlatform = seqPlatform
        seqTrack.pipelineVersion = softwareTool
        assertNotNull(seqTrack.save([flush: true]))
        DataFile dataFile = createDataFile(seqTrack, fastqR1Filename)
        String correctPath = "${CHIP_SEQ_SEQUENCING_DIR}/${VIEW_BY_PID_PATH}/${individualPid}/${sampleTypeName.toLowerCase()}-${ANTIBODY_TARGET_NAME}/${seqType.libraryLayout.toLowerCase()}/run${runName}/${VBP_PATH}/"
        String path = lsdfFilesService.getFileViewByPidRelativeDirectory(dataFile)
        assertEquals(new File(correctPath).path, new File(path).path)
    }

    /**
     * This test check for compatibility of old chip seq data, which are loaded as normal {@link SeqTrack}
     */
    @Test
    void testGetFileViewByPidRelativeDirectoryChipSeqUsingSeqTrack() {
        final String SEQ_TYPE = SeqTypeNames.CHIP_SEQ.seqTypeName
        final String SEQ_TYPE_SEQUENCING_DIR = "chip_seq_sequencing"
        SeqType seqType = createSeqType(SEQ_TYPE, SEQ_TYPE_SEQUENCING_DIR)

        SeqTrack seqTrack = createSeqTrack(seqType: seqType)
        DataFile dataFile = createDataFile(seqTrack, fastqR1Filename)
        String correctPath = "${SEQ_TYPE_SEQUENCING_DIR}/${VIEW_BY_PID_PATH}/${individualPid}/${sampleTypeName.toLowerCase()}/${seqType.libraryLayout.toLowerCase()}/run${runName}/${VBP_PATH}/"
        String path = lsdfFilesService.getFileViewByPidRelativeDirectory(dataFile)
        assertEquals(new File(correctPath).path, new File(path).path)
    }

    @Test
    void testGetFileViewByPidDirectory() {
        Realm realm = DomainFactory.createRealmDataManagement([name: project.realmName])
        SeqType seqType = DomainFactory.createSeqType()
        SeqTrack seqTrack = createSeqTrack(seqType: seqType)
        createDataFile(seqTrack, fastqR1Filename)

        String viewByPidPath = "${realm.rootPath}/${seqTrack.project.dirName}/sequencing/${seqType.dirName}/view-by-pid"
        String expectedPath = "${viewByPidPath}/${seqTrack.individual.pid}/${seqTrack.sampleType.dirName}/${seqTrack.seqType.libraryLayoutDirName}/run${seqTrack.run.name}"
        String actualPath = lsdfFilesService.getFileViewByPidDirectory(seqTrack)

        assert expectedPath == actualPath
    }

    private SeqTrack createSeqTrack(Map properties = []) {
        return DomainFactory.createSeqTrack([
                laneId: laneNo,
                nBasePairs: baseCount,
                insertSize: insertSize,
                run: run,
                sample: sample,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool
                ] + properties
        )
    }
}
