package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class MergingCriteriaServiceTests {

    MergingCriteriaService mergingCriteriaService

    TestData testData = new TestData()
    ProcessedBamFile processedBamFile
    SeqTrack seqTrack
    MergingSet mergingSet
    MergingWorkPackage mergingWorkPackage
    Run run
    Sample sample
    SeqPlatform seqPlatform
    SoftwareTool softwareTool
    SeqType seqType
    Individual individual
    SampleType sampleType

    @Before
    void setUp() {
        Project project = DomainFactory.createProject(
                        name: "name",
                        dirName: "dirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        seqPlatform = DomainFactory.createSeqPlatform()

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
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

        seqType = DomainFactory.createSeqType()

        softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass(
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

        mergingWorkPackage = processedBamFile.mergingWorkPackage

        mergingSet = new MergingSet(
                        mergingWorkPackage: mergingWorkPackage)
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        processedBamFile = null
        seqTrack = null
        mergingSet = null
        mergingWorkPackage = null
        run = null
        sample = null
        seqPlatform = null
        softwareTool = null
        seqType = null
        individual = null
        sampleType = null
    }

    @Test
    void testProcessedBamFilesForMergingCorrectCriteria() {
        processedBamFile.status = State.INPROGRESS
        List<ProcessedBamFile> processedBamFilesExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFilesAct = mergingCriteriaService.processedBamFilesForMerging(processedBamFile.mergingWorkPackage)
        assertEquals(processedBamFilesExp, processedBamFilesAct)
        assertEquals(State.INPROGRESS, processedBamFilesAct[0].status)
    }

    @Test
    void testProcessedBamFilesForMergingTwoBamFiles() {
        processedBamFile.status = State.INPROGRESS

        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass2 = DomainFactory.createAlignmentPass(
                        identifier: 2,
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

        List<ProcessedBamFile> processedBamFilesExp = [processedBamFile, processedBamFile2]
        Set<ProcessedBamFile> processedBamFilesSeqExp = new HashSet<ProcessedBamFile>(processedBamFilesExp)
        List<ProcessedBamFile> processedBamFilesAct = mergingCriteriaService.processedBamFilesForMerging(processedBamFile.mergingWorkPackage)
        Set<ProcessedBamFile> processedBamFilesSetAct = new HashSet<ProcessedBamFile>(processedBamFilesAct)
        assertEquals(processedBamFilesSeqExp, processedBamFilesSetAct)
        assertEquals(State.INPROGRESS, processedBamFilesAct[0].status)
        assertEquals(State.INPROGRESS, processedBamFilesAct[1].status)
    }

    @Test
    void testProcessedMergedBamFileForMerging() {
        mergingSet.status = MergingSet.State.PROCESSED
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingPass mergingPass = new MergingPass(
                        mergingSet: mergingSet,
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile mergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        type: AbstractBamFile.BamType.MDUP,
                        status: State.PROCESSED,
                        ])

        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass2 = DomainFactory.createAlignmentPass(
                        identifier: 2,
                        seqTrack: seqTrack2,
                        )
        assertNotNull(alignmentPass2.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass2,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile mergedBamFileAct = mergingCriteriaService.processedMergedBamFileForMerging(mergingWorkPackage)
        assertEquals(mergedBamFile, mergedBamFileAct)
        assert mergedBamFile.status == State.INPROGRESS

        mergingSet.status = MergingSet.State.INPROGRESS
        assertNotNull(mergedBamFile.save([flush: true, failOnError: true]))

        assertNull(mergingCriteriaService.processedMergedBamFileForMerging(mergingWorkPackage))
    }
}
