package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.ProcessingType
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class MergingCriteriaServiceTests {

    MergingCriteriaService mergingCriteriaService

    MergingCriteria criteria
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
        criteria = MergingCriteria.DEFAULT

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

        seqTrack = new SeqTrack(
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

        mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

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
        criteria = null
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
    void testBamFiles2MergeCorrectCriteria() {
        List<ProcessedBamFile> processedBamFilesExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFilesAct = mergingCriteriaService.bamFiles2Merge(processedBamFile, criteria)
        assertEquals(processedBamFilesExp, processedBamFilesAct)
        assertEquals(State.INPROGRESS, processedBamFilesAct[0].status)
    }

    @Test(expected = IllegalArgumentException)
    void testBamFiles2MergeCriteriaNull() {
        List<ProcessedBamFile> processedBamFilesAct = mergingCriteriaService.bamFiles2Merge(processedBamFile, null)
    }

    @Test(expected = MissingMethodException)
    void testBamFiles2MergeCriteriaFalse() {
        List<ProcessedBamFile> processedBamFilesAct = mergingCriteriaService.bamFiles2Merge(processedBamFile, "test")
    }

    @Test
    void testBamFiles2MergeTwoBamFiles() {
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
                        identifier: 2,
                        seqTrack: seqTrack2,
                        description: "test"
                        )
        assertNotNull(alignmentPass2.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass2,
                        type: BamType.SORTED,
                        status: State.DECLARED
                        )
        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))

        List<ProcessedBamFile> processedBamFilesExp = [processedBamFile, processedBamFile2]
        List<ProcessedBamFile> processedBamFilesAct = mergingCriteriaService.bamFiles2Merge(processedBamFile, criteria)
        assertEquals(processedBamFilesExp, processedBamFilesAct)
        assertEquals(State.INPROGRESS, processedBamFilesAct[0].status)
        assertEquals(State.INPROGRESS, processedBamFilesAct[1].status)
    }

    @Test
    void testValidateBamFiles() {
        assertTrue(mergingCriteriaService.validateBamFiles(mergingSet))
    }

    @Test
    void testValidateBamFilesManually() {
        mergingWorkPackage.processingType = ProcessingType.MANUAL
        assertTrue(mergingCriteriaService.validateBamFiles(mergingSet))
    }

    @Test
    void testValidateBamFilesTwoFiles() {
        AlignmentPass alignmentPass2 = new AlignmentPass(
                        identifier: 2,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass2.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass2,
                        type: BamType.SORTED,
                        status: State.DECLARED
                        )
        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        assertTrue(mergingCriteriaService.validateBamFiles(mergingSet))
    }

    @Test
    void testValidateBamFilesSeqTypeNotValid() {
        SeqType seqType2 = new SeqType(
                        name: "name2",
                        libraryLayout: "library2",
                        dirName: "dirName2"
                        )
        assertNotNull(seqType2.save([flush: true, failOnError: true]))

        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType2,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass2 = new AlignmentPass(
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

        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        assertFalse(mergingCriteriaService.validateBamFiles(mergingSet))
    }

    @Test
    void testValidateBamFilesSampleNotValid() {
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

        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        assertFalse(mergingCriteriaService.validateBamFiles(mergingSet))
    }

    @Test
    void testValidateBamFilesSeqPlatformNotValid() {
        SeqPlatform seqPlatform2 = new SeqPlatform(
                        name: "name",
                        model: "model"
                        )
        assertNotNull(seqPlatform2.save([flush: true, failOnError: true]))

        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform2,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass2 = new AlignmentPass(
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

        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        assertFalse(mergingCriteriaService.validateBamFiles(mergingSet))
    }
}
