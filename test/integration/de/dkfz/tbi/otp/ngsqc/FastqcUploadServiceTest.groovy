/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.ngsqc

import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory

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
    void test_parseFastQCFile_WhenAllFine_WithTabAtTheEnd_ShouldReturnParsedValues() {
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
    void test_parseFastQCFile_SequenceLengthIsRange_WhenAllFine_WithTabAtTheEnd_ShouldReturnParsedValues() {
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
Sequence length\t35-76\t
%GC\t48\t
>>END_MODULE"""
        }

        assert [nReads: "100", sequenceLength: "35-76"] == fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)
    }

    @Test
    void test_parseFastQCFile_WhenAllFine_WithoutTabAtTheEnd_ShouldReturnParsedValues() {
        FastqcUploadService.metaClass.getFastQCFileContent = { FastqcProcessedFile a ->
            return """
##FastQC\t0.11.2
>>Basic Statistics\tpass
#Measure\tValue
Filename\tAS-100221-LR-14142_R1.fastq.gz
File type\tConventional base calls
Encoding\tSanger / Illumina 1.9
Total Sequences\t100
Sequences flagged as poor quality\t0
Sequence length\t101
%GC\t48
>>END_MODULE"""
        }

        assert [nReads: "100", sequenceLength: "101"] == fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)
    }

    @Test
    void test_parseFastQCFile_SequenceLengthIsRange_WhenAllFine_WithoutTabAtTheEnd_ShouldReturnParsedValues() {
        FastqcUploadService.metaClass.getFastQCFileContent = { FastqcProcessedFile a ->
            return """
##FastQC\t0.11.2
>>Basic Statistics\tpass
#Measure\tValue
Filename\tAS-100221-LR-14142_R1.fastq.gz
File type\tConventional base calls
Encoding\tSanger / Illumina 1.9
Total Sequences\t100
Sequences flagged as poor quality\t0
Sequence length\t35-76
%GC\t48
>>END_MODULE"""
        }

        assert [nReads: "100", sequenceLength: "35-76"] == fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)
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
        String parsedSequenceLength = "101"

        FastqcUploadService.metaClass.parseFastQCFile = { FastqcProcessedFile a, Map b ->
            return [
                nReads: parsedNReads as String,
                sequenceLength: parsedSequenceLength,
            ]
        }

        fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(
                dataFile: DomainFactory.createDataFile(nReads: parsedNReads))

        fastqcUploadService.uploadFastQCFileContentsToDataBase(fastqcProcessedFile)

        assert parsedNReads == fastqcProcessedFile.dataFile.nReads
        assert parsedSequenceLength == fastqcProcessedFile.dataFile.sequenceLength
    }
}
