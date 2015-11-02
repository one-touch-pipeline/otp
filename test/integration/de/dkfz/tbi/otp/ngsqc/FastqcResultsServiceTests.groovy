package de.dkfz.tbi.otp.ngsqc

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

class FastqcResultsServiceTests {

    FastqcResultsService fastqcResultsService
    SeqTrack seqTrack

    String runName = "1234_A00123_0012_ABCDEFGHIJ"
    String seqCenterName = "TheSequencingCenter"
    String sampleID = "1234_AB_CD_E"
    String projectName = "TheProject"
    String softwareToolName = "CASAVA"
    String softwareToolVersion = "1.8.2"
    String pipeLineVersion = "${softwareToolName}-${softwareToolVersion}"
    String libraryLayout = SeqType.LIBRARYLAYOUT_PAIRED
    String instrumentPlatform = "Illumina"
    String instrumentModel = "HiSeq2000"

    @Before
    void setUp() {
        Project project = TestData.createProject(
                        name: "project",
                        dirName: "project-dir",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SeqType seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        SeqPlatform seqPlatform = SeqPlatform.build()

        SeqCenter seqCenter = new SeqCenter(
                        name: seqCenterName,
                        dirName: seqCenterName
                        )
        assertNotNull(seqCenter.save(flush: true))

        SoftwareTool softwareTool = new SoftwareTool()
        softwareTool.programName = softwareToolName
        softwareTool.programVersion = softwareToolVersion
        softwareTool.type = SoftwareTool.Type.BASECALLING
        assertNotNull(softwareTool.save(flush: true))

        SoftwareToolIdentifier softwareToolIdentifier = new SoftwareToolIdentifier()
        softwareToolIdentifier.name = pipeLineVersion
        softwareToolIdentifier.softwareTool = softwareTool
        assertNotNull(softwareToolIdentifier.save(flush: true))

        Run run = new Run()
        run.name = runName
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        assertNotNull(run.save([flush: true, failOnError: true]))

        seqTrack = new SeqTrack(
            laneId: 0,
            run: run,
            sample: sample,
            seqType: seqType,
            seqPlatform: seqPlatform,
            pipelineVersion: softwareTool)
        assertNotNull(seqTrack.save([flush: true]))

    }

    @Test
    void testIsFastqcAvailable() {
        DataFile dataFile = new DataFile(
            seqTrack: seqTrack)
        assertNotNull(dataFile.save([flush: true]))
        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile(
            contentUploaded: true,
            dataFile: dataFile)
        assertNotNull(fastqcProcessedFile.save([flush: true]))
        assertTrue(fastqcResultsService.isFastqcAvailable(seqTrack))
    }

    @Test
    void testFastqcFilesForSeqTrack() {
        DataFile dataFile = new DataFile(
            seqTrack: seqTrack)
        assertNotNull(dataFile.save([flush: true]))
        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile(
            contentUploaded: true,
            dataFile: dataFile)
        assertNotNull(fastqcProcessedFile.save([flush: true]))
        List<FastqcProcessedFile> fastqcProcessedFiles = fastqcResultsService.fastqcFilesForSeqTrack(seqTrack)
        assertEquals([fastqcProcessedFile], fastqcProcessedFiles)
    }

    @After
    public void tearDown() throws Exception {
        fastqcResultsService = null
        seqTrack == null
    }
}
