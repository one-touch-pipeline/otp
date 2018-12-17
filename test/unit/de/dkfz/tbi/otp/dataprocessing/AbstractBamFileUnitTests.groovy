package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Assert
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

@TestFor(MockAbstractBamFile)
@Build([FileType, DataFile, ProcessedBamFile])
class AbstractBamFileUnitTests {

    @Test
    void testSave() {
        AbstractBamFile bamFile = new MockAbstractBamFile()
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testSaveCoverageNotNull() {
        AbstractBamFile bamFile = new MockAbstractBamFile()
        bamFile.coverage = 30.0
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testWithdraw_ChangeStatusFromNeedsProcessingToDeclared() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                status: AbstractBamFile.State.NEEDS_PROCESSING,
        )
        assert bamFile.status == AbstractBamFile.State.NEEDS_PROCESSING

        LogThreadLocal.withThreadLog(System.out) {
            bamFile.withdraw()
        }
        assert bamFile.withdrawn
        assert bamFile.status == AbstractBamFile.State.DECLARED
    }
}


class MockAbstractBamFile extends AbstractBamFile {

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
