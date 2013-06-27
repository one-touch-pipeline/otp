package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

import de.dkfz.tbi.otp.ngsdata.*

@TestFor(MergingWorkPackage)
@Mock([Sample, SampleType, Individual, Project])
class MergingWorkPackageTests {

    Sample sample = null

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

        this.sample = new Sample (
            individual: individual,
            sampleType: sampleType)
        this.sample.save(flush: true)
    }

    void tearDown() {
        this.sample = null
    }

    void testSave() {
        MergingWorkPackage workPackage = new MergingWorkPackage(
            sample: sample)
        Assert.assertTrue(workPackage.validate())
        workPackage.save(flush: true)
    }

    void testContraints() {
        // sample is not null
        MergingWorkPackage workPackage = new MergingWorkPackage()
        Assert.assertFalse(workPackage.validate())
        // processingType is not null
        workPackage.sample = sample
        Assert.assertTrue(workPackage.validate())
        workPackage.processingType = null
        Assert.assertFalse(workPackage.validate())
        // mergingCriteria can be null
        workPackage.processingType = MergingWorkPackage.ProcessingType.SYSTEM
        Assert.assertTrue(workPackage.validate())
        workPackage.mergingCriteria = null
        Assert.assertTrue(workPackage.validate())
    }
}
