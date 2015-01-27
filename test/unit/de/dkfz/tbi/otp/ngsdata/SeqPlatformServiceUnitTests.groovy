package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*

@TestMixin(GrailsUnitTestMixin)
@TestFor(SeqPlatformService)
@Mock([
    AlignmentPass,
    MergingPass,
    MergingSet,
    MergingSetAssignment,
    ProcessedBamFile,
    ProcessedMergedBamFile,
    ReferenceGenome,
    SeqPlatform,
    SeqTrack,
])
class SeqPlatformServiceUnitTests {

    SeqPlatformService seqPlatformService


    @Before
    public void setUp() throws Exception {
        seqPlatformService = new SeqPlatformService()
    }

    @After
    public void tearDown() throws Exception {
        seqPlatformService = null
    }

    @Test
    public void testPlatformForMergedBamFile() {
        SeqPlatform seqPlatform = new SeqPlatform(
                        name: "seqPlatform",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

        SeqTrack seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: new Run(),
                        sample: new Sample(),
                        seqType: new SeqType(),
                        seqPlatform: seqPlatform,
                        pipelineVersion: new SoftwareTool()
                        )
        assertNotNull(seqTrack.save([flush: true]))

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage()

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true]))

        AlignmentPass alignmentPass = new TestData().createAlignmentPass(
                        identifier: 0,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: AbstractBamFile.BamType.SORTED,
                        status: AbstractBamFile.State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile.save([flush: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        bamFile: processedBamFile,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingSetAssignment.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        type: AbstractBamFile.BamType.MDUP,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))

        //case: merging set of the given processedMergedBamFile is connected to a procesedBamFile
        assertEquals(seqPlatform, seqPlatformService.platformForMergedBamFile(processedMergedBamFile))

        mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true]))


        mergingSetAssignment = new MergingSetAssignment(
                        bamFile: processedMergedBamFile,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingSetAssignment.save([flush: true]))

        mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        type: AbstractBamFile.BamType.MDUP,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))

        //case: merging set of given processedMergedBamFile is connected to a processedMergedBamFile
        assertEquals(seqPlatform, seqPlatformService.platformForMergedBamFile(processedMergedBamFile))

        mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true]))

        mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        type: AbstractBamFile.BamType.MDUP,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))

        //case: no bam file for merging set
        shouldFail(RuntimeException) {
            assertEquals(seqPlatform, seqPlatformService.platformForMergedBamFile(processedMergedBamFile))
        }
        //case: parameter is null
        shouldFail(IllegalArgumentException) {
            seqPlatformService.platformForMergedBamFile(null)
        }
    }

}
