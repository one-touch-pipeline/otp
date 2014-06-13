package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SampleType
import org.joda.time.DateTime

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.Individual

import grails.buildtestdata.mixin.Build
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
    void testQualityControlIsNotNull() {
        AbstractBamFile bamFile = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        bamFile.qualityControl =  null
        Assert.assertFalse(bamFile.validate())
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
    void testGetLatestSequenceDataFileCreationDate() {

        final FileType sequenceFileType = FileType.build(type: FileType.Type.SEQUENCE)

        final SeqTrack seqTrack1 = SeqTrack.build()
        final DataFile dataFile11 = DataFile.build(
                seqTrack: seqTrack1,
                fileType: sequenceFileType,
        )

        final SeqTrack seqTrack2 = SeqTrack.build()
        final DataFile dataFile21 = DataFile.build(
                seqTrack: seqTrack2,
                fileType: sequenceFileType,
                dateCreated: new DateTime(dataFile11.dateCreated).plusHours(1).toDate(),
        )
        final DataFile dataFile22 = DataFile.build(
                seqTrack: seqTrack2,
                fileType: sequenceFileType,
                dateCreated: new DateTime(dataFile11.dateCreated).plusHours(2).toDate(),
        )
        final DataFile dataFile23 = DataFile.build(
                seqTrack: seqTrack2,
                fileType: FileType.build(type: FileType.Type.ALIGNMENT),
                dateCreated: new DateTime(dataFile11.dateCreated).plusHours(3).toDate(),
        )

        assert dataFile11.dateCreated < dataFile21.dateCreated
        assert dataFile21.dateCreated < dataFile22.dateCreated
        assert dataFile22.dateCreated < dataFile23.dateCreated

        final AbstractBamFile bamFile1 = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        bamFile1.metaClass.getContainedSeqTracks = { [seqTrack1].toSet() }
        final AbstractBamFile bamFile2 = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        bamFile2.metaClass.getContainedSeqTracks = { [seqTrack2].toSet() }
        final AbstractBamFile bamFile3 = new MockAbstractBamFile(type: AbstractBamFile.BamType.SORTED)
        bamFile3.metaClass.getContainedSeqTracks = { [seqTrack1, seqTrack2].toSet() }

        assert AbstractBamFile.getLatestSequenceDataFileCreationDate(bamFile2, bamFile1) == dataFile22.dateCreated
        assert AbstractBamFile.getLatestSequenceDataFileCreationDate(bamFile3) == dataFile22.dateCreated
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
