package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*

import de.dkfz.tbi.otp.ngsdata.*

import static org.junit.Assert.assertTrue

@TestFor(MergingSetAssignment)
@Build([
        MergingCriteria,
        MergingSet,
        ProcessedBamFile,
])
class MergingSetAssignmentTests {

    MergingSet mergingSet = null
    ProcessedBamFile processedBamFile = null

    @Before
    void setUp() {
        Individual individual = DomainFactory.createIndividual()

        SampleType sampleType = DomainFactory.createSampleType( [
                name: "TUMOR",
        ])

        Sample sample = DomainFactory.createSample(
                individual: individual,
                sampleType: sampleType,
        )

        SeqType seqType = DomainFactory.createWholeGenomeSeqType(LibraryLayout.SINGLE)

        Run run = DomainFactory.createRun()

        SoftwareTool softwareTool = DomainFactory.createSoftwareTool([
                type: SoftwareTool.Type.ALIGNMENT,
        ])

        SeqTrack seqTrack = DomainFactory.createSeqTrack([
                run: run,
                sample: sample,
                seqType: seqType,
                pipelineVersion: softwareTool,
        ])

        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass(
            identifier: 2,
            seqTrack: seqTrack,
        )

        processedBamFile = new ProcessedBamFile()
        processedBamFile.type = AbstractBamFile.BamType.SORTED
        processedBamFile.fileExists = true
        processedBamFile.dateFromFileSystem = new Date()
        processedBamFile.alignmentPass = alignmentPass
        processedBamFile.save(flush: true)
        assertTrue(processedBamFile.validate())

        MergingWorkPackage workPackage = processedBamFile.mergingWorkPackage
        workPackage.save(flush: true)

        this.mergingSet = new MergingSet(
            mergingWorkPackage: workPackage)
        this.mergingSet.save(flush: true)
    }

    @After
    void tearDown() {
        mergingSet = null
        processedBamFile = null
    }

    @Test
    void testSave() {
        MergingSetAssignment mtm = new MergingSetAssignment(
            mergingSet: mergingSet,
            bamFile: processedBamFile)
        Assert.assertTrue(mtm.validate())
        mtm.save(flush: true)
    }

    @Test
    void testConstraints() {
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
