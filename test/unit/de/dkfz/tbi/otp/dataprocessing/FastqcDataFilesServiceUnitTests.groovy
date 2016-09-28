package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import org.junit.*

import static org.junit.Assert.*

@Mock([DataFile, FastqcProcessedFile, FileType, Individual, Project, ProjectCategory, Realm, Run, RunSegment, Sample, SampleType, SeqCenter, SeqPlatform, SeqPlatformGroup, SeqTrack, SeqType, SoftwareTool])
@TestFor(FastqcDataFilesService)
class FastqcDataFilesServiceUnitTests {

    FastqcDataFilesService fastqcDataFilesService

    SeqTrack seqTrack
    DataFile dataFile
    Realm realm

    @Before
    public void setUp() throws Exception {
        fastqcDataFilesService = new FastqcDataFilesService()
        fastqcDataFilesService.lsdfFilesService = new LsdfFilesService()
        fastqcDataFilesService.lsdfFilesService.configService = new ConfigService()

        realm = DomainFactory.createRealmDataManagement()

        seqTrack = DomainFactory.createSeqTrack()
        seqTrack.project.realmName = realm.name
        assert seqTrack.project.save(flush: true)

        dataFile = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run])
    }

    @After
    public void tearDown() throws Exception {
        fastqcDataFilesService = null

        seqTrack = null
        dataFile = null
        realm = null
    }

    @Test
    void testFastqcFileName() {
        DataFile dataFile = new DataFile()
        Map<String, String> files = [
            //no extension
            "123": "123",

            //one extension
            "123.gz": "123",
            "123.bz2": "123",
            "123.txt": "123",
            "123.fastq": "123",
            "123.sam": "123",
            "123.bam": "123",
            "123.other": "123.other",

            //two extension, second is gz
            "123.gz.gz": "123.gz",
            "123.bz2.gz": "123",
            "123.txt.gz": "123",
            "123.fastq.gz": "123",
            "123.sam.gz": "123",
            "123.bam.gz": "123",

            //two extension, second is bz2
            "123.gz.bz2": "123.gz",
            "123.bz2.bz2": "123.bz2",
            "123.txt.bz2": "123",
            "123.fastq.bz2": "123",
            "123.sam.bz2": "123",
            "123.bam.bz2": "123",

            //two extension, second is other
            "123.gz.other": "123.gz.other",
            "123.bz2.other": "123.bz2.other",
            "123.txt.other": "123.txt.other",
            "123.fastq.other": "123.fastq.other",
            "123.sam.other": "123.sam.other",
            "123.bam.other": "123.bam.other",

            //two extension, first is other
            "123.other.gz": "123.other",
            "123.other.bz2": "123.other",
            "123.other.txt": "123.other",
            "123.other.fastq": "123.other",
            "123.other.sam": "123.other",
            "123.other.bam": "123.other",

            //dot in name, no extension
            "123.456": "123.456",

            //dot in name, one extension
            "123.456.gz": "123.456",
            "123.456.bz2": "123.456",
            "123.456.txt": "123.456",
            "123.456.fastq": "123.456",
            "123.456.sam": "123.456",
            "123.456.bam": "123.456",


            //dot in name, two extension, second is gz
            "123.456.gz.gz": "123.456.gz",
            "123.456.bz2.gz": "123.456",
            "123.456.txt.gz": "123.456",
            "123.456.fastq.gz": "123.456",
            "123.456.sam.gz": "123.456",
            "123.456.bam.gz": "123.456",

            //dot in name, two extension, second is bz2
            "123.456.gz.bz2": "123.456.gz",
            "123.456.bz2.bz2": "123.456.bz2",
            "123.456.txt.bz2": "123.456",
            "123.456.fastq.bz2": "123.456",
            "123.456.sam.bz2": "123.456",
            "123.456.bam.bz2": "123.456",

            //dot in name, two extension, second is other
            "123.456.gz.other": "123.456.gz.other",
            "123.456.bz2.other": "123.456.bz2.other",
            "123.456.txt.other": "123.456.txt.other",
            "123.456.fastq.other": "123.456.fastq.other",
            "123.456.sam.other": "123.456.sam.other",
            "123.456.bam.other": "123.456.bam.other",

            //dot in name, two extension, first is other
            "123.456.other.gz": "123.456.other",
            "123.456.other.bz2": "123.456.other",
            "123.456.other.txt": "123.456.other",
            "123.456.other.fastq": "123.456.other",
            "123.456.other.sam": "123.456.other",
            "123.456.other.bam": "123.456.other",
        ]

        files.each { key, value ->
            dataFile.fileName = key
            assertEquals(value + fastqcDataFilesService.FASTQC_FILE_SUFFIX + fastqcDataFilesService.FASTQC_ZIP_SUFFIX,
                    fastqcDataFilesService.fastqcFileName(dataFile))
        }
    }

    @Test
    void testFastqcOutputDirectory() {
        String fastqc = DataProcessingFilesService.OutputDirectories.FASTX_QC.toString().toLowerCase()

        String viewByPidPath = "${realm.rootPath}/${seqTrack.project.dirName}/sequencing/${seqTrack.seqType.dirName}/view-by-pid"
        String expectedPath = "${viewByPidPath}/${seqTrack.individual.pid}/${seqTrack.sampleType.dirName}/${seqTrack.seqType.libraryLayoutDirName}/run${seqTrack.run.name}/${fastqc}"
        String actualPath = fastqcDataFilesService.fastqcOutputDirectory(seqTrack)

        assert actualPath == expectedPath
    }


    @Test
    void testGetAndUpdateFastqcProcessedFile() {

        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(dataFile: dataFile)

        assert fastqcProcessedFile == fastqcDataFilesService.getAndUpdateFastqcProcessedFile(fastqcProcessedFile.dataFile)
    }


    @Test
    void testPathToFastQcResultFromSeqCenter() {
        String expected = "${dataFile.initialDirectory}/${fastqcDataFilesService.fastqcFileName(dataFile)}"
        String actual = fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile)
        assert actual == expected
    }
}
