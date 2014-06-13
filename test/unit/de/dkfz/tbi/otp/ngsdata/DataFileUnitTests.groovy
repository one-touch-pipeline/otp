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
    void testReadConstraint_Alignment() {
        FileType fileType = FileType.build([type: FileType.Type.ALIGNMENT])
        DataFile dataFile = new DataFile(fileType: fileType)

        assert dataFile.validate()
    }

    @Test
    void testReadConstraint_SequenceButNotFastq() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: 'SomeOtherDirectory'])
        DataFile dataFile = new DataFile(fileType: fileType)

        assert dataFile.validate()
    }

    @Test
    void testReadConstraint_SequenceFastq_OK_ReadIsOne() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, readNumber: 1)

        assert dataFile.validate()
    }

    @Test
    void testReadConstraint_SequenceFastq_OK_ReadIsTwo() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, readNumber: 2)

        assert dataFile.validate()
    }

    @Test
    void testReadConstraint_SequenceFastq_NoReadNumber() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, readNumber: null)

        TestCase.assertValidateError(dataFile, "readNumber", "validator.invalid", null)
    }

    @Test
    void testReadConstraint_SequenceFastq_ReadNumberIsZero() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, readNumber: 0)

        TestCase.assertValidateError(dataFile, "readNumber", "validator.invalid", 0)
    }

    @Test
    void testReadConstraint_SequenceFastq_ReadNumberIsToBig() {
        FileType fileType = FileType.build([type: FileType.Type.SEQUENCE, vbpPath: SEQUENCE_DIRECTORY])
        DataFile dataFile = new DataFile(fileType: fileType, readNumber: 3)

        TestCase.assertValidateError(dataFile, "readNumber", "validator.invalid", 3)
    }


}
