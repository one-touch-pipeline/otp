package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*


@TestFor(ProcessedMergedBamFile)
@Build([
    MergingPass,
])
class ProcessedMergedBamFileTests {

    MergingPass mergingPass = null
    MergingSet mergingSet = null
    MergingWorkPackage workPackage = null

    @Before
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

        Sample sample = new Sample(
                individual: individual,
                sampleType: sampleType)
        sample.save(flush: true)

        this.workPackage = new TestData().createMergingWorkPackage(
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

    @After
    void tearDown() {
        mergingPass = null
        mergingSet = null
        workPackage = null
    }

    @Test
    void testSave() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testSaveWithNumberOfLanes() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                numberOfMergedLanes: 3,
        ])
        Assert.assertTrue(bamFile.validate())
        bamFile.save(flush: true)
    }

    @Test
    void testConstraints() {
        // mergingPass must not be null
        ProcessedMergedBamFile bamFile = new ProcessedMergedBamFile(
                type: AbstractBamFile.BamType.SORTED)
        Assert.assertFalse(bamFile.validate())
    }

    @Test
    void testMergingWorkPackageConstraint_NoWorkpackage_ShouldFail() {
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                type: AbstractBamFile.BamType.MDUP,
                workPackage: null,
        ])
        TestCase.assertValidateError(processedMergedBamFile, "workPackage", "nullable", null)
    }

    @Test
    void testIsMostRecentBamFile() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        bamFile.save(flush: true)

        assertTrue(bamFile.isMostRecentBamFile())

        MergingPass secondMergingPass = new MergingPass(
                identifier: 2,
                mergingSet: mergingSet)
        secondMergingPass.save(flush: true)

        ProcessedMergedBamFile secondBamFile = DomainFactory.createProcessedMergedBamFile(secondMergingPass)
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

        ProcessedMergedBamFile firstBamFileOfSecondMergingSet = DomainFactory.createProcessedMergedBamFile(firstMergingPassOfSecondMergingSet)
        firstBamFileOfSecondMergingSet.save(flush: true)

        assertFalse(secondBamFile.isMostRecentBamFile())
        assertTrue(firstBamFileOfSecondMergingSet.isMostRecentBamFile())
    }

    void testGetBamFileName() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)

        assert "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup.bam" == bamFile.bamFileName
    }

    void testFileNameNoSuffix() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        assert "${bamFile.sampleType.name}_${bamFile.individual.pid}_${bamFile.seqType.name}_${bamFile.seqType.libraryLayout}_merged.mdup" == bamFile.fileNameNoSuffix()
    }
}
