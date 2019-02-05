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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Test

import de.dkfz.tbi.TestCase

@Build([
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
])
@TestFor(DataFile)
class DataFileUnitTests {



    private final static String SEQUENCE_DIRECTORY = '/sequence/'



    @Test
    void testMateNumberConstraint_Alignment() {
        FileType fileType = FileType.build([type: FileType.Type.ALIGNMENT])
        DomainFactory.createDataFile(fileType: fileType)
    }

    @Test
    void testMateNumberConstraint_SequenceButNotFastq() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: 'SomeOtherDirectory'])
        DomainFactory.createDataFile(fileType: fileType)
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_OK_ReadIsOne() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DomainFactory.createDataFile(fileType: fileType, mateNumber: 1)
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_OK_ReadIsTwo() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DomainFactory.createDataFile(
                seqTrack: DomainFactory.createSeqTrack(
                        seqType: DomainFactory.createSeqType(libraryLayout: LibraryLayout.PAIRED)
                ),
                fileType: fileType,
                mateNumber: 2,
        )
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_NoMateNumber() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = DomainFactory.createDataFile([fileType: fileType, mateNumber: null], false)

        TestCase.assertValidateError(dataFile, "mateNumber", "validator.invalid", null)
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_MateNumberIsZero() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = DomainFactory.createDataFile([fileType: fileType, mateNumber: 0], false)

        TestCase.assertAtLeastExpectedValidateError(dataFile, "mateNumber", "validator.invalid", 0)
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_MateNumberIsToBig() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = DomainFactory.createDataFile([fileType: fileType, mateNumber: 3], false)

        TestCase.assertValidateError(dataFile, "mateNumber", "validator.invalid", 3)
    }

    @Test
    void testSequenceLengthConstraint_WhenSequenceLengthIsANumberAsString_ShouldPassValidation() {
        DomainFactory.createDataFile(sequenceLength: "123")
    }

    @Test
    void testSequenceLengthConstraint_WhenSequenceLengthIsARangeAsString_ShouldPassValidation() {
        DomainFactory.createDataFile(sequenceLength: "123-321")
    }

    @Test
    void testSequenceLengthConstraint_WhenSequenceLengthIsSomethingElse_ShouldPassValidation() {
        DataFile dataFile = DomainFactory.createDataFile([sequenceLength: "!1§2%3&"], false)

        TestCase.shouldFail(RuntimeException) {
            dataFile.validate()
        }
    }
}
