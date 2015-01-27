package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

@TestFor(MergingSetAssignment)
@Mock([
    AlignmentPass,
    Individual,
    MergingSet,
    MergingWorkPackage,
    ProcessedBamFile,
    Project,
    ReferenceGenome,
    Run,
    Sample,
    SampleType,
    SeqCenter,
    SeqPlatform,
    SeqTrack,
    SeqType,
    SoftwareTool,
])
class MergingSetAssignmentTests {

    MergingSet mergingSet = null
    ProcessedBamFile processedBamFile = null

    void setUp() {
        Project project = new Project()
        project.name = "SOME_PROJECT"
        project.dirName = "/some/relative/path"
        project.realmName = "def"
        project.save(flush: true)
        assertTrue(project.validate())

        Individual individual = new Individual()
        individual.pid = "SOME_PATIENT_ID"
        individual.mockPid = "PUBLIC_PID"
        individual.mockFullName = "PUBLIC_NAME"
        individual.type = Individual.Type.REAL
        individual.project = project
        individual.save(flush: true)
        assertTrue(individual.validate())

        SampleType sampleType = new SampleType()
        sampleType.name = "TUMOR"
        sampleType.save(flush: true)
        assertTrue(sampleType.validate())

        Sample sample = new Sample()
        sample.individual = individual
        sample.sampleType = sampleType
        sample.save(flush: true)
        assertTrue(sample.validate())

        SeqType seqType = new SeqType()
        seqType.name = "WHOLE_GENOME"
        seqType.libraryLayout = "SINGLE"
        seqType.dirName = "whole_genome_sequencing"
        seqType.save(flush: true)
        assertTrue(seqType.validate())

        SeqCenter seqCenter = new SeqCenter()
        seqCenter.name = "DKFZ"
        seqCenter.dirName = "core"
        seqCenter.save(flush: true)
        assertTrue(seqCenter.validate())

        SeqPlatform seqPlatform = new SeqPlatform()
        seqPlatform.name = "solid"
        seqPlatform.model = "4"
        seqPlatform.save(flush: true)
        assertTrue(seqPlatform.validate())

        Run run = new Run()
        run.name = "testname"
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        run.save(flush: true)
        assertTrue(run.validate())

        SoftwareTool softwareTool = new SoftwareTool()
        softwareTool.programName = "SOLID"
        softwareTool.programVersion = "0.4.8"
        softwareTool.qualityCode = null
        softwareTool.type = SoftwareTool.Type.ALIGNMENT
        softwareTool.save(flush: true)
        assertTrue(softwareTool.validate())

        SeqTrack seqTrack = new SeqTrack()
        seqTrack.laneId = "123"
        seqTrack.run = run
        seqTrack.sample = sample
        seqTrack.seqType = seqType
        seqTrack.seqPlatform = seqPlatform
        seqTrack.pipelineVersion = softwareTool
        seqTrack.save(flush: true)
        assertTrue(seqTrack.validate())

        AlignmentPass alignmentPass = new TestData().createAlignmentPass(
            identifier: 2,
            seqTrack: seqTrack,
        )
        alignmentPass.save(flush: true)
        assertTrue(alignmentPass.validate())

        processedBamFile = new ProcessedBamFile()
        processedBamFile.type = AbstractBamFile.BamType.SORTED
        processedBamFile.fileExists = true
        processedBamFile.dateFromFileSystem = new Date()
        processedBamFile.alignmentPass = alignmentPass
        processedBamFile.save(flush: true)
        assertTrue(processedBamFile.validate())

        MergingWorkPackage workPackage = new MergingWorkPackage(
            sample: sample)
        workPackage.save(flush: true)

        this.mergingSet = new MergingSet(
            mergingWorkPackage: workPackage)
        this.mergingSet.save(flush: true)

    }

    void tearDown() {
        mergingSet = null
        processedBamFile = null
    }

    void testSave() {
        MergingSetAssignment mtm = new MergingSetAssignment(
            mergingSet: mergingSet,
            bamFile: processedBamFile)
        Assert.assertTrue(mtm.validate())
        mtm.save(flush: true)
    }

    void testConstaints() {
        // mergingSet must not be null
        MergingSetAssignment mtm = new MergingSetAssignment(
            bamFile: processedBamFile)
        Assert.assertFalse(mtm.validate())
        // bamFile must not be null
        mtm.mergingSet = mergingSet
        Assert.assertTrue(mtm.validate())
        mtm.bamFile = null
        Assert.assertFalse(mtm.validate())
    }
}
