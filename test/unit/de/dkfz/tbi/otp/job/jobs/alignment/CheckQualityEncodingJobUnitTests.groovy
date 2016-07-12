package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import org.junit.rules.*

import java.util.zip.*

@Mock([
    DataFile,
    FileType,
    Individual,
    Project,
    Realm,
    Run,
    SampleType,
    Sample,
    SeqCenter,
    SeqType,
    SoftwareTool,
    SeqTrack,
])
@Build([
    SeqPlatform,
])
@TestMixin(GrailsUnitTestMixin)
class CheckQualityEncodingJobUnitTests {

    CheckQualityEncodingJob checkQualityEncodingJob

    DataFile dataFile

    File file

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File testDirectory

    @Before
    public void setUp() throws Exception {
        checkQualityEncodingJob = new CheckQualityEncodingJob(
            lsdfFilesService: new LsdfFilesService(
                configService: new ConfigService()
            )
        )

        checkQualityEncodingJob.log = this.log

        Project project = TestData.createProject(
                        name: "projectName",
                        dirName: "dirName",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "pid",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sampleTypeName"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SeqType seqType = new SeqType(
                        name: "seqTypeName",
                        libraryLayout: "library",
                        dirName: "dirName"
                        )
        assertNotNull(seqType.save([flush: true]))

        SeqPlatform seqPlatform = SeqPlatform.build()

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        Run run = new Run(
                        name: "runName",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform
                        )
        assertNotNull(run.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "softwareToolName",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        SeqTrack seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))

        FileType fileType = new FileType(
                        type: FileType.Type.SEQUENCE,
                        vbpPath: "vbpPath"
                        )
        assertNotNull(fileType.save(flush: true))

        dataFile = DomainFactory.createDataFile(
                        fileName: "dataFile1.fastq",
                        pathName: "testPath",
                        run: run,
                        seqTrack: seqTrack,
                        used: true,
                        project: project,
                        fileType: fileType,
                        vbpFileName: "dataFile1.fastq"
                        )
        assertNotNull(dataFile.save(flush: true))

        testDirectory = tmpDir.newFolder("otp-test")

        Realm realm = DomainFactory.createRealmDataManagement(
                name: project.realmName,
                rootPath: testDirectory.absolutePath,
        )
        assertNotNull(realm.save(flush: true, failOnError: true))

        file = new File(checkQualityEncodingJob.lsdfFilesService.getFileViewByPidPath(dataFile))
        assertNotNull(file)
        if (!file.getParentFile().exists()) {
            assertTrue("could not create ${file.getParentFile()}", file.getParentFile().mkdirs())
        }
        if (file.exists()) {
            assertTrue(file.delete())
        }
    }

    @After
    public void tearDown() throws Exception {
        if (file.exists()) {
            assertTrue(file.delete())
        }
        checkQualityEncodingJob = null
        dataFile = null
        file = null
    }

    @Test
    void testOpenStream() {
        GZIPOutputStream stream = new GZIPOutputStream(new FileOutputStream(file))
        stream << "test"
        stream.close()

        LineNumberReader reader = checkQualityEncodingJob.openStream(dataFile)
        assertEquals("test", reader.readLine())
        assertEquals(null, reader.readLine())
        reader.close()
    }

    @Test
    void testOpenStream_NoFile() {
        shouldFail(FileNotReadableException) {
            checkQualityEncodingJob.openStream(dataFile)
        }
    }
}
