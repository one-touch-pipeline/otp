/*
 * Copyright 2011-2024 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

class FastqcUploadServiceSpec extends Specification implements DataTest, FastqcDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RawSequenceFile,
                FastqFile,
                FastqcProcessedFile,
                FileType,
                Individual,
                Project,
                FastqImportInstance,
                Sample,
                SampleType,
        ]
    }

    FastqcUploadService fastqcUploadService

    FastqcProcessedFile fastqcProcessedFile

    void setupData() {
        fastqcUploadService = Spy(FastqcUploadService)
        fastqcProcessedFile = createFastqcProcessedFile()
    }

    private void spyGetFastQCFileContent(String returnValue) {
        fastqcUploadService = Spy(FastqcUploadService) {
            getFastQCFileContent(_) >> { return returnValue }
        }
    }

    void "parseFastQCFile, all fine with tab at the end, returns parsed values"() {
        given:
        setupData()

        spyGetFastQCFileContent("""
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
>>END_MODULE""")

        when:
        Map<String, String> expected = fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)

        then:
        [nReads: "100", sequenceLength: "101"] == expected
    }

    void "parseFastQCFile, all fine with tab at the end and sequence length is range, returns parsed values"() {
        given:
        setupData()

        spyGetFastQCFileContent("""
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
>>END_MODULE""")

        when:
        Map<String, String> expected = fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)

        then:
        [nReads: "100", sequenceLength: "35-76"] == expected
    }

    void "parseFastQCFile, all fine without tab at the end, returns parsed values"() {
        given:
        setupData()

        spyGetFastQCFileContent("""
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
>>END_MODULE""")

        when:
        Map<String, String> expected = fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)

        then:
        [nReads: "100", sequenceLength: "101"] == expected
    }

    void "parseFastQCFile, all fine without tab at the end and sequence length is range, returns parsed values"() {
        given:
        setupData()

        spyGetFastQCFileContent("""
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
>>END_MODULE""")

        when:
        Map<String, String> expected = fastqcUploadService.parseFastQCFile(fastqcProcessedFile, FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED)

        then:
        [nReads: "100", sequenceLength: "35-76"] == expected
    }

    void "parseFastQCFile, fails when no FastQc file is given"() {
        given:
        setupData()

        when:
        fastqcUploadService.parseFastQCFile(null, ["a": "b"])

        then:
        AssertionError e = thrown()
        e.message.contains("No FastQC file defined")
    }

    void "parseFastQCFile, fails when FastQc file content is null"() {
        given:
        setupData()

        spyGetFastQCFileContent(null)

        when:
        fastqcUploadService.parseFastQCFile(fastqcProcessedFile, ["a": "b"])

        then:
        AssertionError e = thrown()
        e.message.contains("is empty")
    }

    void "parseFastQCFile, fails when there are not properties to be parsed"() {
        given:
        setupData()

        spyGetFastQCFileContent(null)

        when:
        fastqcUploadService.parseFastQCFile(fastqcProcessedFile, null)

        then:
        AssertionError e = thrown()
        e.message.contains("No properties defined to parse for in FastQC file")
    }

    void "parseFastQCFile, fails when FastQC file does not contain properties to be parsed"() {
        given:
        setupData()

        spyGetFastQCFileContent("""
##FastQC\t0.11.2\t
>>Basic Statistics\tpass\t
#Measure\tValue\t
Filename\tAS-100221-LR-14142_R1.fastq.gz\t
File type\tConventional base calls\t
Encoding\tSanger / Illumina 1.9\t
Total Sequences\t100\t
Sequences flagged as poor quality\t0\t
%GC\t48\t
>>END_MODULE""")

        when:
        fastqcUploadService.parseFastQCFile(fastqcProcessedFile, [sequenceLength: FastqcUploadService.PROPERTIES_REGEX_TO_BE_PARSED.get('sequenceLength')])

        then:
        RuntimeException e = thrown()
        e.message.contains("contains no information about")
    }

    void "uploadFastQCFileContentsToDataBase, fails when FastQcProcessedFile is null"() {
        given:
        setupData()

        when:
        fastqcUploadService.uploadFastQCFileContentsToDataBase(null)

        then:
        AssertionError e = thrown()
        e.message.contains("No FastQC file defined")
    }

    void "uploadFastQCFileContentsToDataBase, fails when RawSequenceFile has no SeqTrack"() {
        given:
        setupData()

        fastqcProcessedFile.sequenceFile.seqTrack = null
        fastqcProcessedFile.sequenceFile.save(flush: true)

        when:
        fastqcUploadService.uploadFastQCFileContentsToDataBase(fastqcProcessedFile)

        then:
        NullPointerException e = thrown()
        e.message.contains("Cannot invoke method getInputStreamFromZipFile() on null object")
    }

    void "uploadFastQCFileContentsToDataBase, fills RawSequenceFile for fine data"() {
        given:
        setupData()

        when:
        long parsedNReads = 100
        String parsedSequenceLength = "101"

        fastqcUploadService = Spy(FastqcUploadService) {
            parseFastQCFile(_, _) >> { [nReads : parsedNReads as String, sequenceLength : parsedSequenceLength] }
        }

        fastqcProcessedFile = createFastqcProcessedFile(sequenceFile: DomainFactory.createFastqFile(nReads: parsedNReads))

        fastqcUploadService.uploadFastQCFileContentsToDataBase(fastqcProcessedFile)

        then:
        parsedNReads == fastqcProcessedFile.sequenceFile.nReads
        parsedSequenceLength == fastqcProcessedFile.sequenceFile.sequenceLength
        fastqcProcessedFile.contentUploaded == true
    }
}
