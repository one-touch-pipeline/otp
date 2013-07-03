package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class ProcessedBamFileServiceTests {

    ProcessedBamFileService processedBamFileService

    ProcessedBamFile processedBamFile
    SeqType seqType
    Sample sample
    SeqPlatform seqPlatform

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

        Run run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "pid",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: de.dkfz.tbi.otp.ngsdata.Individual.Type.REAL,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
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

        SoftwareTool softwareTool = new SoftwareTool(
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
                        pipelineVersion: softwareTool
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
