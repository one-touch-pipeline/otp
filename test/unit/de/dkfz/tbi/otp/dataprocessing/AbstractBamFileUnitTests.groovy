package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

import static org.junit.Assert.*
import grails.test.mixin.*
import org.junit.*

/**
 * The DOM has been created long time ago.
 * Here only possibility to create and save DOM is tested
 *
 *
 */
@TestFor(MockAbstractBamFile)
@Mock([MockAbstractBamFile])
class AbstractBamFileUnitTests {

    void testSave() {
        AbstractBamFile bamFile = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    void testQualityControlIsNotNull() {
        AbstractBamFile bamFile = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        bamFile.qualityControl =  null
        Assert.assertFalse(bamFile.validate())
    }

    void testhasMetricsFileTrueBamTypeSorted() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.SORTED,
                hasMetricsFile: true
                )
        Assert.assertFalse(bamFile.validate())
    }

    void testhasMetricsFileFalseBamTypeSorted() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.SORTED,
                hasMetricsFile: false
                )
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    void testhasMetricsFileTrueBamTypeRmdup() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.RMDUP,
                hasMetricsFile: true
                )
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    void testhasMetricsFileFalseBamTypeRmdup() {
        AbstractBamFile bamFile = new MockAbstractBamFile(
                type: AbstractBamFile.BamType.RMDUP,
                hasMetricsFile: false
                )
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    void testSaveCoverageNotNull() {
        AbstractBamFile bamFile = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        bamFile.coverage = 30.0
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }
}


class MockAbstractBamFile extends AbstractBamFile {
    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return null
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        return null
    }

    @Override
    SeqType getSeqType() {
        return null
    }
}
