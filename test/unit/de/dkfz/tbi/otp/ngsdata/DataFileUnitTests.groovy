package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.buildtestdata.mixin.Build
import org.junit.*

/**
 */

@Build([FileType])
@TestFor(DataFile)
class DataFileUnitTests {

    @Test
    void testReadConstraint() {
        FileType fileTypeReadsHaveToBeSet = FileType.build([type: FileType.Type.SEQUENCE, subType: "fastq"])
        FileType fileTypeReadsHaveNOTtoBeSet = FileType.build([type: FileType.Type.ALIGNMENT])

        DataFile dataFileReadsHaveNOTtoBeSet = new DataFile(fileType: fileTypeReadsHaveNOTtoBeSet)
        assertTrue dataFileReadsHaveNOTtoBeSet.validate()

        DataFile dataFileReadsHaveToBeSet = new DataFile(fileType: fileTypeReadsHaveToBeSet)
        assertFalse dataFileReadsHaveToBeSet.validate()

        dataFileReadsHaveToBeSet.readNumber = 8
        assertFalse dataFileReadsHaveToBeSet.validate()

        dataFileReadsHaveToBeSet.readNumber = 1
        assertTrue dataFileReadsHaveToBeSet.validate()

        dataFileReadsHaveToBeSet.readNumber = 2
        assertTrue dataFileReadsHaveToBeSet.validate()
    }
}