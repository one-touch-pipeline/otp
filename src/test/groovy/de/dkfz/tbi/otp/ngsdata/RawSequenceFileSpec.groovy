/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.HelperUtils

class RawSequenceFileSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RawSequenceFile,
                FastqFile,
                SequenceCramFile,
                FileType,
                ReferenceGenome,
                Individual,
                Project,
                Run,
                FastqImportInstance,
                Sample,
                SampleType,
                SeqPlatform,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    private final static String SEQUENCE_DIRECTORY = '/sequence/'

    void "test validate, when mateNumber is whatever and file type is alignment"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.ALIGNMENT])

        expect:
        createFastqFile(fileType: fileType)
    }

    void "test validate, when mateNumber is whatever and file type is sequence (not fastq)"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: 'SomeOtherDirectory'])

        expect:
        createFastqFile(fileType: fileType)
    }

    @Unroll
    void "test validate, when fileType is file type is sequence (fastq) and mateNumber is #matNumber and sequenceType is #sequencingReadType and indexFile is :indexFile, then it should be valid"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack  : createSeqTrack([
                        seqType: createSeqType([
                                libraryLayout: sequencingReadType,
                        ]),
                ]),
                fileType  : fileType,
                mateNumber: matNumber,
                indexFile : indexFile,
        ], false)

        expect:
        rawSequenceFile.validate()

        where:
        sequencingReadType        | matNumber | indexFile
        SequencingReadType.SINGLE | 1         | false
        SequencingReadType.PAIRED | 1         | false
        SequencingReadType.PAIRED | 2         | false
        SequencingReadType.SINGLE | 1         | true
        SequencingReadType.PAIRED | 1         | true
        SequencingReadType.SINGLE | 2         | true
        SequencingReadType.PAIRED | 2         | true
        SequencingReadType.SINGLE | 3         | true
        SequencingReadType.PAIRED | 3         | true
        SequencingReadType.SINGLE | 9         | true
        SequencingReadType.PAIRED | 9         | true
    }

    @Unroll
    void "validate, when file does not require a mate number and mateNumber is null and sequenceType is #sequencingReadType, then it should be valid"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createSequenceCramFile([
                seqTrack  : createSeqTrack([
                        seqType: createSeqType([
                                libraryLayout: sequencingReadType,
                        ]),
                ]),
                fileType  : fileType,
                mateNumber: null,
        ], false)

        expect:
        rawSequenceFile.validate()

        where:
        sequencingReadType << [
                SequencingReadType.SINGLE,
                SequencingReadType.PAIRED,
        ]
    }

    @Unroll
    void "validate, when a CRAM file was converted from a single fastq file and mateNumber is #mateNumber and sequenceType is #sequencingReadType, then it should still be valid"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createSequenceCramFile([
                seqTrack  : createSeqTrack([
                        seqType: createSeqType([
                                libraryLayout: sequencingReadType,
                        ]),
                ]),
                fileType  : fileType,
                mateNumber: mateNumber,
        ], false)

        expect:
        rawSequenceFile.validate()

        where:
        sequencingReadType        | mateNumber
        SequencingReadType.SINGLE | 1
        SequencingReadType.PAIRED | 1
        SequencingReadType.PAIRED | 2
    }

    void "validate, when a CRAM file has a mateNumber bigger than the mate count, should fail"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createSequenceCramFile([
                seqTrack  : createSeqTrack([
                        seqType: createSeqType([
                                libraryLayout: SequencingReadType.SINGLE,
                        ]),
                ]),
                fileType  : fileType,
                mateNumber: 2,
        ], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "mateNumber", "validator.invalid", 2)
    }

    void "validate, when file does not require a mate number but is an index file and mateNumber is null, should fail"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createSequenceCramFile([fileType: fileType, mateNumber: null, indexFile: true], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "mateNumber", "validator.invalid", null)
    }

    @Unroll
    void "test validate, when mateNumber is null and indexFile is #indexFile, should fail"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createFastqFile([fileType: fileType, mateNumber: null, indexFile: indexFile], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "mateNumber", "validator.invalid", null)

        where:
        indexFile << [
                false,
                true,
        ]
    }

    void "test validate, when mateNumber is zero, should fail"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createFastqFile([fileType: fileType, mateNumber: 0], false)

        expect:
        TestCase.assertAtLeastExpectedValidateError(rawSequenceFile, "mateNumber", "validator.invalid", 0)
    }

    void "test validate, when mateNumber is too big, should fail"() {
        given:
        FileType fileType = createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        RawSequenceFile rawSequenceFile = createFastqFile([fileType: fileType, mateNumber: 3], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "mateNumber", "validator.invalid", 3)
    }

    void "validate, when a CRAM file has no source fastq md5sum, then it should be valid"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile([fastqMd5sum: null], false)

        expect:
        rawSequenceFile.validate()
    }

    void "validate, when a CRAM file was converted from one fastq file and keeps its source md5sum, then it should be valid"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile([fastqMd5sum: HelperUtils.randomMd5sum], false)

        expect:
        rawSequenceFile.validate()
    }

    void "validate, when a CRAM file has neither a cram md5sum nor a source fastq md5sum, should fail"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile([fastqMd5sum: null, cramMd5sum: null], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "cramMd5sum", "validator.invalid", null)
    }

    void "validate, when a CRAM file has no cram md5sum but a source fastq md5sum, then it should be valid"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile([fastqMd5sum: HelperUtils.randomMd5sum, cramMd5sum: null], false)

        expect:
        rawSequenceFile.validate()
    }

    void "validate, when a fastq file has no fastq md5sum, should fail"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile([fastqMd5sum: null], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "fastqMd5sum", "validator.invalid", null)
    }

    void "validate, when a CRAM file has an invalid fastq md5sum, should fail"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile([fastqMd5sum: "invalid"], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "fastqMd5sum", "matches.invalid", "invalid")
    }

    void "test validate, when sequenceLength is a number"() {
        expect:
        createFastqFile(sequenceLength: "123")
    }

    void "test validate, when sequenceLength is a range"() {
        expect:
        createFastqFile(sequenceLength: "123-321")
    }

    void "test validate, when sequenceLength is invalid, should fail"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile([sequenceLength: "!1§2%3&"], false)

        expect:
        TestCase.assertValidateError(rawSequenceFile, "sequenceLength", "invalid", "!1§2%3&")
    }

    void "getNBasePairs, fails when required properties are null"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile(nReads: nReads, sequenceLength: sequenceLength)

        when:
        rawSequenceFile.NBasePairs

        then:
        AssertionError e = thrown()
        e.message.contains(missingValue)

        where:
        nReads | sequenceLength || missingValue
        null   | "100"          || "nReads"
        100    | null           || "sequenceLength"
        null   | null           || "nReads"
    }

    @Unroll
    void "getReadName, when mateNumber is #mateNumber and indexFile is #indexFile and sequencingReadType is #sequencingReadType, then return #expected"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack  : createSeqTrack([
                        seqType: createSeqType([
                                libraryLayout: sequencingReadType,
                        ]),
                ]),
                mateNumber: mateNumber,
                indexFile : indexFile,
        ], false)

        expect:
        rawSequenceFile.readName == expected

        where:
        mateNumber | indexFile | sequencingReadType         || expected
        1          | false     | SequencingReadType.PAIRED  || 'R1'
        2          | false     | SequencingReadType.PAIRED  || 'R2'
        1          | true      | SequencingReadType.PAIRED  || 'I1'
    }

    @Unroll
    void "getReadName, when it is a legacy fastq file without mateNumber and sequencingReadType is #sequencingReadType, then do not name it after all mates"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack  : createSeqTrack([
                        seqType: createSeqType([
                                libraryLayout: sequencingReadType,
                        ]),
                ]),
                mateNumber: null,
        ], false)

        expect:
        rawSequenceFile.readName == 'Rnull'

        where:
        sequencingReadType << [
                SequencingReadType.SINGLE,
                SequencingReadType.PAIRED,
        ]
    }

    @Unroll
    void "getReadName, when it is a CRAM file with mateNumber #mateNumber and sequencingReadType #sequencingReadType, then return #expected"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile([
                seqTrack  : createSeqTrack([
                        seqType: createSeqType([
                                libraryLayout: sequencingReadType,
                        ]),
                ]),
                mateNumber: mateNumber,
        ], false)

        expect:
        rawSequenceFile.readName == expected

        where:
        mateNumber | sequencingReadType        || expected
        1          | SequencingReadType.SINGLE || 'R1'
        1          | SequencingReadType.PAIRED || 'R1'
        2          | SequencingReadType.PAIRED || 'R2'
        null       | SequencingReadType.SINGLE || 'R1'
        null       | SequencingReadType.PAIRED || 'R1_2'
    }

    void "getReadName, when mateNumber is null and no sequencing read type is available, then return the prefix only"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile([mateNumber: null], false)
        rawSequenceFile.seqTrack = null

        expect:
        rawSequenceFile.readName == 'R'
    }

    void "getNBasePairs, multiplies meanSequenceLength times nReads"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile(nReads: 100, sequenceLength: sequenceLength)
        long result

        when:
        result = rawSequenceFile.NBasePairs

        then:
        result == 10000

        where:
        sequenceLength | _
        "100"          | _
        "90-110"       | _
    }
}
