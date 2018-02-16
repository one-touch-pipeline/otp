package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.junit.*

@TestFor(MockAbstractBamFile)
@Build([FileType, DataFile, ProcessedBamFile])
class AbstractBamFileUnitTests {

    @Test
    void testSave() {
        AbstractBamFile bamFile = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testhasMetricsFileTrueBamTypeSorted() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.SORTED,
                hasMetricsFile: true
                )
        Assert.assertFalse(bamFile.validate())
    }

    @Test
    void testhasMetricsFileFalseBamTypeSorted() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.SORTED,
                hasMetricsFile: false
                )
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testhasMetricsFileTrueBamTypeRmdup() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.RMDUP,
                hasMetricsFile: true
                )
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testhasMetricsFileFalseBamTypeRmdup() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.RMDUP,
                hasMetricsFile: false
                )
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testSaveCoverageNotNull() {
        AbstractBamFile bamFile = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        bamFile.coverage = 30.0
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testWithdraw_ChangeStatusFromNeedsProcessingToDeclared() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                status: AbstractBamFile.State.NEEDS_PROCESSING,
                type: AbstractBamFile.BamType.MDUP,
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
