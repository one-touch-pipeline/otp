package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class ProcessedBamFileServiceTests {

    ProcessedBamFileService processedBamFileService

    ProcessedBamFile processedBamFile
    SeqType seqType
    Sample sample
    SeqPlatform seqPlatform
    Run run
    SoftwareTool softwareTool
    Individual individual
    SampleType sampleType

    @Before
    void setUp() {
        Project project = new Project(
                        name: "name",
                        dirName: "dirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        seqPlatform = new SeqPlatform(
                        name: "name",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true, failOnError: true]))

        individual = new Individual(
                        pid: "pid",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: de.dkfz.tbi.otp.ngsdata.Individual.Type.REAL,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        sampleType = new SampleType(
                        name: "name"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        seqType = new SeqType(
                        name: "name",
                        libraryLayout: "library",
                        dirName: "dirName"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        alignmentState: SeqTrack.DataProcessingState.FINISHED
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass = new AlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        processedBamFile = null
        seqType = null
        sample = null
        seqPlatform = null
        run = null
        softwareTool = null
        individual = null
        sampleType = null
    }

    @Test
    void testProcessedBamFileNeedsProcessingAllCorrect() {
        assertEquals(processedBamFile.id, processedBamFileService.processedBamFileNeedsProcessing().id)
    }

    @Test
    void testProcessedBamFileNeedsProcessingNoBamFileNeedsProcessing() {
        processedBamFile.status = State.DECLARED
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
        assertNull(processedBamFileService.processedBamFileNeedsProcessing())
    }

    @Test
    void testProcessedBamFileNeedsProcessing() {
        //no mergingSet exists
        assertEquals(processedBamFile, processedBamFileService.processedBamFileNeedsProcessing())

        MergingWorkPackage workpackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(workpackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                        mergingWorkPackage: workpackage,
                        status: MergingSet.State.DECLARED
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingSetAssignment assignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(assignment.save([flush: true, failOnError: true]))

        Sample sample2 = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample2.save([flush: true, failOnError: true]))

        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample2,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass2 = new AlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack2,
                        description: "test"
                        )
        assertNotNull(alignmentPass2.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass2,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))

        assertEquals(processedBamFile2, processedBamFileService.processedBamFileNeedsProcessing())

        //a mergingSet exists for processedBamFile and is not finished
        mergingSet.status = MergingSet.State.PROCESSED
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        //a mergingSet exists for processedBamFile and is finished
        assertEquals(processedBamFile, processedBamFileService.processedBamFileNeedsProcessing())
    }

    @Test
    void testProcessedBamFileNeedsProcessingAlignmentNotFinished() {
        processedBamFile.status = AbstractBamFile.State.PROCESSED

        MergingWorkPackage workpackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(workpackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                        mergingWorkPackage: workpackage,
                        status: MergingSet.State.PROCESSED
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingSetAssignment assignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(assignment.save([flush: true, failOnError: true]))

        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass2 = new AlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack2,
                        description: "test"
                        )
        assertNotNull(alignmentPass2.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass2,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))

        SeqTrack seqTrack3 = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        alignmentState: SeqTrack.DataProcessingState.NOT_STARTED
                        )
        assertNotNull(seqTrack3.save([flush: true, failOnError: true]))

        assertNull(processedBamFileService.processedBamFileNeedsProcessing())

        seqTrack3.alignmentState = SeqTrack.DataProcessingState.IN_PROGRESS
        assertNotNull(seqTrack3.save([flush: true, failOnError: true]))

        assertNull(processedBamFileService.processedBamFileNeedsProcessing())

        seqTrack3.alignmentState = SeqTrack.DataProcessingState.FINISHED
        assertNotNull(seqTrack3.save([flush: true, failOnError: true]))

        assertEquals(processedBamFile2, processedBamFileService.processedBamFileNeedsProcessing())
    }

    @Test
    void testBlockedForAssigningToMergingSet() {
        assertEquals(processedBamFile.status, State.NEEDS_PROCESSING)
        processedBamFileService.blockedForAssigningToMergingSet(processedBamFile)
        assertEquals(processedBamFile.status, State.INPROGRESS)
    }

    @Test
    void testAssignedToMergingSet() {
        assertEquals(processedBamFile.status, State.NEEDS_PROCESSING)
        processedBamFileService.assignedToMergingSet(processedBamFile)
        assertEquals(processedBamFile.status, State.PROCESSED)
    }

    @Test
    void testNotAssignedToMergingSet() {
        assertTrue(processedBamFileService.notAssignedToMergingSet(processedBamFile))
        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        seqType: seqType,
                        sample: sample,
                        seqPlatform: seqPlatform
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 1,
                        status: de.dkfz.tbi.otp.dataprocessing.MergingSet.State.DECLARED,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        assertFalse(processedBamFileService.notAssignedToMergingSet(processedBamFile))
    }
}
