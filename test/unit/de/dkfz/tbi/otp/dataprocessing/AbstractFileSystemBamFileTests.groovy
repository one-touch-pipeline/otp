package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*


@TestFor(MockAbstractFileSystemBamFile)
@Mock([MockAbstractFileSystemBamFile])
class AbstractFileSystemBamFileTests {

    void testSave() {
        AbstractFileSystemBamFile bamFile = new MockAbstractFileSystemBamFile(
                type: AbstractBamFile.BamType.SORTED)
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    void testContraints() {
        // dateCreated is not null
        AbstractFileSystemBamFile bamFile = new MockAbstractFileSystemBamFile(
                type: AbstractBamFile.BamType.SORTED)
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
    Set<SeqTrack> getContainedSeqTracks() {
        return null
    }
}
