package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(MergingSet)
@Build([DataFile, FileType, MergingSet, MergingSetAssignment, ProcessedBamFile])
class MergingSetUnitTests {

    MergingWorkPackage workPackage = null

    void setUp() {
        Project project = TestData.createProject(
            name: "project",
            dirName: "dirName",
            realmName: "DKFZ")
        project.save(flush: true)

        Individual individual = new Individual(
            pid: "pid",
            mockPid: "mockPid",
            mockFullName: "mockFullName",
            type: Individual.Type.REAL,
            project: project)
        individual.save(flush: true)

        SampleType sampleType = new SampleType(
            name: "sample-type")
        sampleType.save(flush: true)

        Sample sample = new Sample (
            individual: individual,
            sampleType: sampleType)
        sample.save(flush: true)

        this.workPackage = new TestData().createMergingWorkPackage(
            sample: sample,
            seqType: new SeqType())
        this.workPackage.save(flush: true)
    }

    void tearDown() {
        this.workPackage = null
    }

    void testSave() {
        MergingSet mergingSet = new MergingSet(
            status: MergingSet.State.DECLARED,
            mergingWorkPackage: workPackage)
        Assert.assertTrue(mergingSet.validate())
        mergingSet.save(flush: true)
    }

    void testContraints() {
        // status in not null
        MergingSet mergingSet = new MergingSet(
            mergingWorkPackage: workPackage)
        mergingSet.status = null
        Assert.assertFalse(mergingSet.validate())
        // mergingWorkPackage is not null
        mergingSet.status = MergingSet.State.DECLARED
        Assert.assertTrue(mergingSet.validate())
        mergingSet.mergingWorkPackage = null
        Assert.assertFalse(mergingSet.validate())
    }

    void testIsLatestSet() {
        MergingSet mergingSet = new MergingSet(
            identifier: 1,
            status: MergingSet.State.DECLARED,
            mergingWorkPackage: workPackage)
        mergingSet.save(flush: true)
        assertTrue(mergingSet.isLatestSet())

        MergingSet mergingSet2 = new MergingSet(
            identifier: 2,
            status: MergingSet.State.DECLARED,
            mergingWorkPackage: workPackage)
        mergingSet2.save(flush: true)
        assertTrue(mergingSet2.isLatestSet())

        assertFalse(mergingSet.isLatestSet())
    }

    @Test
    void testGetContainedSeqTracks() {

        final SeqTrack seqTrack1 = SeqTrack.build()
        final SeqTrack seqTrack2 = SeqTrack.build()
        final SeqTrack seqTrack3 = SeqTrack.build()

        final Set<SeqTrack> seqTracks1 = [seqTrack1, seqTrack2].toSet()
        final Set<SeqTrack> seqTracks2 = [seqTrack3].toSet()
        final Set<SeqTrack> allSeqTracks = seqTracks1 + seqTracks2
        assert allSeqTracks.size() == 3

        final MergingSet mergingSet = MergingSet.build()
        assert mergingSet.containedSeqTracks == Collections.emptySet()

        final ProcessedBamFile bamFile1 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile1.metaClass.getContainedSeqTracks = { seqTracks1 }
        assert mergingSet.containedSeqTracks == seqTracks1

        final ProcessedBamFile bamFile2 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile2.metaClass.getContainedSeqTracks = { seqTracks2 }
        assert mergingSet.containedSeqTracks == allSeqTracks
    }

    @Test
    void testGetContainedSeqTracks_containedTwice() {

        final Set<SeqTrack> seqTracks = [SeqTrack.build()].toSet()

        final MergingSet mergingSet = MergingSet.build()

        final ProcessedBamFile bamFile1 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile1.metaClass.getContainedSeqTracks = { seqTracks }
        final ProcessedBamFile bamFile2 = DomainFactory.assignNewProcessedBamFile(mergingSet)
        bamFile2.metaClass.getContainedSeqTracks = { seqTracks }

        assert shouldFail(IllegalStateException, { mergingSet.containedSeqTracks }).contains('more than once')
    }
}
