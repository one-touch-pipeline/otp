package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import org.junit.After
import org.junit.Before
import org.junit.Test

import static de.dkfz.tbi.TestCase.shouldFail
import static de.dkfz.tbi.TestCase.shouldFailWithMessageContaining

class FastqcUploadServiceTest {

    FastqcUploadService fastqcUploadService

    FastqcProcessedFile fastqcProcessedFile

    @Before
    void setUp() {
        fastqcProcessedFile = FastqcProcessedFile.build()
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(FastqcUploadService, fastqcUploadService)
    }

    @Test
    void test_parseFastQCFile_WhenAllFine_ShouldReturnParsedValues() {
        FastqcUploadService.metaClass.getFastQCFileContent = { FastqcProcessedFile a ->
            return """
##FastQC\t0.11.2\t
>>Basic Statistics\tpass\t
#Measure\tValue\t
Filename\tAS-100221-LR-14142_R1.fastq.gz\t
File type\tConventional base calls\t
Encoding\tSanger / Illumina 1.9\t
Total Sequences\t100\t
Sequences flagged as poor quality\t0\t
Sequence length\t101\t
%GC\t48\t
>>END_MODULE"""
        }

        assert [nReads: "100", sequenceLength: "101"] == fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)
    }

    @Test
    void test_parseFastQCFile_WhenNoFastQC_ShouldFail() {
        shouldFailWithMessageContaining(AssertionError, "No FastQC file defined") {
            fastqcUploadService.parseFastQCFile(null, ["a": "b"])
        }
    }

    @Test
    void test_parseFastQCFile_WhenFastQCFileContentIsNull_ShouldFail() {
        FastqcUploadService.metaClass.getFastQCFileContent = { FastqcProcessedFile a ->
            return null
        }

        shouldFailWithMessageContaining(AssertionError, "is empty") {
            fastqcUploadService.parseFastQCFile(fastqcProcessedFile, ["a": "b"])
        }
    }

    @Test
    void test_parseFastQCFile_WhenPropertiesToBeParsedIsNull_ShouldFail() {
        FastqcUploadService.metaClass.getFastQCFileContent = { FastqcProcessedFile a ->
            return null
        }

        shouldFailWithMessageContaining(AssertionError, "No properties defined to parse for in FastQC file") {
            fastqcUploadService.parseFastQCFile(fastqcProcessedFile, null)
        }
    }

    @Test
    void test_parseFastQCFile_WhenFastQCFileContentContainsNoInformationAboutProperty_ShouldFail() {
        FastqcUploadService.metaClass.getFastQCFileContent = { FastqcProcessedFile a ->
            return """
##FastQC\t0.11.2\t
>>Basic Statistics\tpass\t
#Measure\tValue\t
Filename\tAS-100221-LR-14142_R1.fastq.gz\t
File type\tConventional base calls\t
Encoding\tSanger / Illumina 1.9\t
Total Sequences\t100\t
Sequences flagged as poor quality\t0\t
%GC\t48\t
>>END_MODULE"""
        }

        shouldFailWithMessageContaining(RuntimeException, "contains no information about") {
            fastqcUploadService.parseFastQCFile(fastqcProcessedFile, [sequenceLength: FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED.get('sequenceLength')])
        }
    }

    @Test
    void test_uploadFastQCFileContentsToDataBase_WhenFastQCProcessedFileIsNull_ShouldFail() {
        shouldFailWithMessageContaining(AssertionError, "No FastQC file defined") {
            fastqcUploadService.uploadFastQCFileContentsToDataBase(null)
        }
    }

    @Test
    void test_uploadFastQCFileContentsToDataBase_WhenDataFileHasNoSeqTrack_ShouldFail() {
        fastqcProcessedFile.dataFile.seqTrack = null
        fastqcProcessedFile.dataFile.save(flush:true)

        shouldFailWithMessageContaining(RuntimeException, "Failed to load data from") {
            fastqcUploadService.uploadFastQCFileContentsToDataBase(fastqcProcessedFile)
        }
    }

    @Test
    void test_uploadFastQCFileContentsToDataBase_WhenAllFine_ShouldFillDataFile() {
        long parsedNReads = 100
        long parsedSequenceLength = 101

        FastqcUploadService.metaClass.parseFastQCFile = { FastqcProcessedFile a, Map b ->
            return [
                nReads: parsedNReads as String,
                sequenceLength: parsedSequenceLength as String,
            ]
        }

        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createDataFile([nReads: parsedNReads, seqTrack: seqTrack])
        fastqcProcessedFile.dataFile.seqTrack = seqTrack
        fastqcProcessedFile.dataFile.save(flush: true, failOnError: true)

        fastqcUploadService.uploadFastQCFileContentsToDataBase(fastqcProcessedFile)

        assert parsedNReads == fastqcProcessedFile.dataFile.nReads
        assert parsedSequenceLength == fastqcProcessedFile.dataFile.sequenceLength
    }
}
