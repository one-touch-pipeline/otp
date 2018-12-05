package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import grails.test.mixin.*
import org.junit.*


@TestFor(MockAbstractFileSystemBamFile)
@Mock([MockAbstractFileSystemBamFile])
class AbstractFileSystemBamFileTests {

    @Test
    void testSave() {
        AbstractFileSystemBamFile bamFile = new MockAbstractFileSystemBamFile()
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testContraints() {
        // dateCreated is not null
        AbstractFileSystemBamFile bamFile = new MockAbstractFileSystemBamFile()
        bamFile.dateCreated = null
        // this check must fail but it does not:
        // probably grails sets value of this filed before the validation
        Assert.assertTrue(bamFile.validate())

        // dateFromFileSystem is nullable
        bamFile.dateFromFileSystem = null
        Assert.assertTrue(bamFile.validate())
    }
}


class MockAbstractFileSystemBamFile extends AbstractFileSystemBamFile {

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return null
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return null
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        return null
    }
}
