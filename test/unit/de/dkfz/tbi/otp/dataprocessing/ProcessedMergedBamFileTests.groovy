package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ProcessedMergedBamFile)
@Mock([MergingPass, MergingSet, MergingWorkPackage,
    Sample, SampleType, Individual, Project, SeqType])
class ProcessedMergedBamFileTests {

    MergingPass mergingPass = null
    MergingSet mergingSet = null
    MergingWorkPackage workPackage = null

    void setUp() {
        Project project = new Project(
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

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType)
        sample.save(flush: true)

        this.workPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: new SeqType())
        this.workPackage.save(flush: true)

        this.mergingSet = new MergingSet(
                        identifier: 1,
                        mergingWorkPackage: workPackage)
        this.mergingSet.save(flush: true)

        this.mergingPass = new MergingPass(
                        identifier: 1,
                        mergingSet: mergingSet)
        this.mergingPass.save(flush: true)
    }

    void tearDown() {
        mergingPass = null
        mergingSet = null
        workPackage = null
    }

    void testSave() {
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                        type: AbstractBamFile.BamType.SORTED,
                        mergingPass: mergingPass)
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    void testConstraints() {
        // mergingPass must not be null
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                        type: AbstractBamFile.BamType.SORTED)
        Assert.assertFalse(bamFile.validate())
    }

    void testIsMostRecentBamFile() {
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                        type: AbstractBamFile.BamType.SORTED,
                        mergingPass: mergingPass)
        bamFile.save(flush: true)

        assertTrue(bamFile.isMostRecentBamFile())

        MergingPass secondMergingPass = new MergingPass(
                        identifier: 2,
                        mergingSet: mergingSet)
        secondMergingPass.save(flush: true)

        ProcessedMergedBamFile secondBamFile = new ProcessedMergedBamFile(
                        type: AbstractBamFile.BamType.SORTED,
                        mergingPass: secondMergingPass)
        secondBamFile.save(flush: true)

        assertFalse(bamFile.isMostRecentBamFile())
        assertTrue(secondBamFile.isMostRecentBamFile())

        MergingSet secondMergingSet = new MergingSet(
                        identifier: 2,
                        mergingWorkPackage: workPackage)
        secondMergingSet.save(flush: true)

        MergingPass firstMergingPassOfSecondMergingSet = new MergingPass(
                        identifier: 1,
                        mergingSet: secondMergingSet)
        firstMergingPassOfSecondMergingSet.save(flush: true)

        ProcessedMergedBamFile firstBamFileOfSecondMergingSet = new ProcessedMergedBamFile(
                        type: AbstractBamFile.BamType.SORTED,
                        mergingPass: firstMergingPassOfSecondMergingSet)
        firstBamFileOfSecondMergingSet.save(flush: true)

        assertFalse(secondBamFile.isMostRecentBamFile())
        assertTrue(firstBamFileOfSecondMergingSet.isMostRecentBamFile())

    }
}
