package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.DataFile

@TestFor(FastqcDataFilesService)
@TestMixin(GrailsUnitTestMixin)
class FastqcDataFilesServiceUnitTests {

    FastqcDataFilesService fastqcDataFilesService

    @Before
    public void setUp() throws Exception {
        fastqcDataFilesService = new FastqcDataFilesService()
    }

    @After
    public void tearDown() throws Exception {
        fastqcDataFilesService = null
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
}
