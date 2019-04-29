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

package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase

class DataFileSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                DataFile,
                FileType,
                Individual,
                Project,
                ProjectCategory,
                Run,
                RunSegment,
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
        FileType fileType = DomainFactory.createFileType([type: FileType.Type.ALIGNMENT])

        expect:
        DomainFactory.createDataFile(fileType: fileType)
    }

    void "test validate, when mateNumber is whatever and file type is sequence (not fastq)"() {
        given:
        FileType fileType = DomainFactory.createFileType([type: FileType.Type.SEQUENCE, vbpPath: 'SomeOtherDirectory'])

        expect:
        DomainFactory.createDataFile(fileType: fileType)
    }

    void "test validate, when mateNumber is 1 and file type is sequence (fastq)"() {
        given:
        FileType fileType = DomainFactory.createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])

        expect:
        DomainFactory.createDataFile(fileType: fileType, mateNumber: 1)
    }

    void "test validate, when mateNumber is 2 and file type is sequence (fastq)"() {
        given:
        FileType fileType = DomainFactory.createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])

        expect:
        DomainFactory.createDataFile(
                seqTrack: DomainFactory.createSeqTrack(
                        seqType: DomainFactory.createSeqType(libraryLayout: LibraryLayout.PAIRED)
                ),
                fileType: fileType,
                mateNumber: 2,
        )
    }

    void "test validate, when mateNumber is null, should fail"() {
        given:
        FileType fileType = DomainFactory.createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = DomainFactory.createDataFile([fileType: fileType, mateNumber: null], false)

        expect:
        TestCase.assertValidateError(dataFile, "mateNumber", "validator.invalid", null)
    }

    void "test validate, when mateNumber is zero, should fail"() {
        given:
        FileType fileType = DomainFactory.createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = DomainFactory.createDataFile([fileType: fileType, mateNumber: 0], false)

        expect:
        TestCase.assertAtLeastExpectedValidateError(dataFile, "mateNumber", "validator.invalid", 0)
    }

    void "test validate, when mateNumber is too big, should fail"() {
        given:
        FileType fileType = DomainFactory.createFileType([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = DomainFactory.createDataFile([fileType: fileType, mateNumber: 3], false)

        expect:
        TestCase.assertValidateError(dataFile, "mateNumber", "validator.invalid", 3)
    }

    void "test validate, when sequenceLength is a number"() {
        expect:
        DomainFactory.createDataFile(sequenceLength: "123")
    }

    void "test validate, when sequenceLength is a range"() {
        expect:
        DomainFactory.createDataFile(sequenceLength: "123-321")
    }

    void "test validate, when sequenceLength is invalid, should fail"() {
        given:
        DataFile dataFile = DomainFactory.createDataFile([sequenceLength: "!1§2%3&"], false)

        when:
        dataFile.validate()

        then:
        thrown(RuntimeException)
    }
}
