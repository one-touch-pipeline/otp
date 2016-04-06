package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*

import org.junit.*

import de.dkfz.tbi.TestCase

/**
 */

@Build([FileType])
@TestFor(DataFile)
class DataFileUnitTests {



    private final static String SEQUENCE_DIRECTORY = '/sequence/'



    @Test
    void testMateNumberConstraint_Alignment() {
        FileType fileType = FileType.build([type: FileType.Type.ALIGNMENT])
        DataFile dataFile = new DataFile(fileType: fileType)

        assert dataFile.validate()
    }

    @Test
    void testMateNumberConstraint_SequenceButNotFastq() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: 'SomeOtherDirectory'])
        DataFile dataFile = new DataFile(fileType: fileType)

        assert dataFile.validate()
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_OK_ReadIsOne() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, mateNumber: 1)

        assert dataFile.validate()
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_OK_ReadIsTwo() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, mateNumber: 2)

        assert dataFile.validate()
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_NoMateNumber() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, mateNumber: null)

        TestCase.assertValidateError(dataFile, "mateNumber", "validator.invalid", null)
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_MateNumberIsZero() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, mateNumber: 0)

        TestCase.assertAtLeastExpectedValidateError(dataFile, "mateNumber", "validator.invalid", 0)
    }

    @Test
    void testMateNumberConstraint_SequenceFastq_MateNumberIsToBig() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, mateNumber: 3)

        TestCase.assertValidateError(dataFile, "mateNumber", "validator.invalid", 3)
    }

    @Test
    void testSequenceLengthConstraint_WhenSequenceLengthIsANumberAsString_ShouldPassValidation() {
        DataFile dataFile = new DataFile(sequenceLength: "123")

        assert dataFile.validate()
    }

    @Test
    void testSequenceLengthConstraint_WhenSequenceLengthIsARangeAsString_ShouldPassValidation() {
        DataFile dataFile = new DataFile(sequenceLength: "123-321")

        assert dataFile.validate()
    }

    @Test
    void testSequenceLengthConstraint_WhenSequenceLengthIsSomethingElse_ShouldPassValidation() {
        DataFile dataFile = new DataFile(sequenceLength: "!1§2%3&")

        TestCase.shouldFail(RuntimeException) {
            dataFile.validate()
        }
    }
}
