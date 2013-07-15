package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import org.junit.*

@TestFor(ProcessedMergedBamFile)
@Mock([MergingPass, MergingSet, MergingWorkPackage,
    Sample, SampleType, Individual, Project])
class ProcessedMergedBamFileTests {

    MergingPass mergingPass = null

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

        Sample sample = new Sample (
            individual: individual,
            sampleType: sampleType)
        sample.save(flush: true)

        MergingWorkPackage workPackage = new MergingWorkPackage(
            sample: sample)
        workPackage.save(flush: true)

        MergingSet mergingSet = new MergingSet(
            mergingWorkPackage: workPackage)
        mergingSet.save(flush: true)

        this.mergingPass = new MergingPass(
            identifier: 1,
            mergingSet: mergingSet)
        this.mergingPass.save(flush: true)
    }

    void tearDown() {
        mergingPass = null
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
}
