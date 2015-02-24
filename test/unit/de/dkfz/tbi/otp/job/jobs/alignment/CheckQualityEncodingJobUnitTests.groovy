package de.dkfz.tbi.otp.job.jobs.alignment

import java.util.zip.GZIPOutputStream
import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

@Mock([
    LsdfFilesService,
    ConfigService,
    DataFile,
    FileType,
    Individual,
    Project,
    Realm,
    Run,
    SampleType,
    Sample,
    SeqCenter,
    SeqPlatform,
    SeqType,
    SoftwareTool,
    SeqTrack,
])
@TestMixin(GrailsUnitTestMixin)
class CheckQualityEncodingJobUnitTests {

    CheckQualityEncodingJob checkQualityEncodingJob

    DataFile dataFile

    File file

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

        SeqPlatform seqPlatform = new SeqPlatform(
                        name: "Illumina",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

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

        dataFile = new DataFile(
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

        Realm realm = DomainFactory.createRealmDataManagementDKFZ([rootPath: '/tmp/otp/otp-test/fakeRealm/root'])
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

    void testOpenStream() {
        GZIPOutputStream stream = new GZIPOutputStream(new FileOutputStream(file))
        stream << "test"
        stream.close()

        LineNumberReader reader = checkQualityEncodingJob.openStream(dataFile)
        assertEquals("test", reader.readLine())
        assertEquals(null, reader.readLine())
        reader.close()
    }

    void testOpenStream_NoFile() {
        shouldFail(FileNotReadableException) {
            checkQualityEncodingJob.openStream(dataFile)
        }
    }
}
