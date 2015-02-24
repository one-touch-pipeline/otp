package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(MergingWorkPackage)
@Build([
    ReferenceGenome,
    Sample,
    SeqPlatformGroup,
    SeqType,
    SequencingKit,
    SeqPlatform,
    SeqTrack,
    MergingWorkPackage,
    ProcessedBamFile,
    AlignmentPass,
])
class MergingWorkPackageTests {

    Sample sample = null
    SeqType seqType = null

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

        this.sample = new Sample(
            individual: individual,
            sampleType: sampleType)
        this.sample.save(flush: true)

        this.seqType = new SeqType(
            name: "WHOLE_GENOME",
            libraryLayout: "SINGLE",
            dirName: "whole_genome_sequencing")
        seqType.save(flush: true)
    }

    void tearDown() {
        this.sample = null
        this.seqType = null
    }

    void testSave() {
        MergingWorkPackage workPackage = new TestData().createMergingWorkPackage(
            sample: sample,
            seqType: seqType)
        Assert.assertTrue(workPackage.validate())
        workPackage.save(flush: true)
    }

    // TODO OTP-1409: check the returned map content
    @Test
    void testGetMergingProperties() {
        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        def result = MergingWorkPackage.getMergingProperties(seqTrack)
        assert result.sequencingKit == null
    }

    @Test
    void testGetMergingProperties_whenNotHiSeq2xxx() {
        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "NOT HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        def result = MergingWorkPackage.getMergingProperties(seqTrack)
        assert result.sequencingKit == sequencingKit
    }

    @Test
    void testGetMergingProperties_whenNotV123() {
        SequencingKit sequencingKit = SequencingKit.build(name: "NEITHER V1 NOR V2 NOR V3")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        def result = MergingWorkPackage.getMergingProperties(seqTrack)
        assert result.sequencingKit == sequencingKit
    }


    @Test
    void testSatisfiesCriteriaSeqTrack_whenValidHiSeq() {
        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup, sequencingKit: null)
        assert workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenInvalid() {
        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "NOT HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup, sequencingKit: null)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }
    @Test
    void testSatisfiesCriteriaSeqTrack_whenInvalidHiSeq() {
        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup, sequencingKit: sequencingKit)
        assert !workPackage.satisfiesCriteria(seqTrack)
    }

    @Test
    void testSatisfiesCriteriaSeqTrack_whenValid() {
        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "NOT HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup, sequencingKit: sequencingKit)
        assert workPackage.satisfiesCriteria(seqTrack)
    }



    @Test
    void testSatisfiesCriteriaBamFile_whenValid() {
        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = MergingWorkPackage.build(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup, sequencingKit: null)
        AlignmentPass alignmentPass = AlignmentPass.build(seqTrack: seqTrack, workPackage: workPackage)

        ProcessedBamFile processedBamFile = ProcessedBamFile.build(alignmentPass: alignmentPass)
        assert workPackage.satisfiesCriteria(processedBamFile)
    }

    @Test
    void testSatisfiesCriteriaBamFile_whenInvalid() {
        MergingWorkPackage workPackage = MergingWorkPackage.build()

        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        assert !workPackage.satisfiesCriteria(processedBamFile)
    }
}
