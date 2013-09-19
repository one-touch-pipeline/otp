package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.validation.ValidationException
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class MergingCriteriaSpecificServiceTests {

    MergingCriteriaSpecificService mergingCriteriaSpecificService

    ProcessedBamFile processedBamFile
    SeqTrack seqTrack
    MergingSet mergingSet
    SoftwareTool softwareTool
    SeqType seqType
    Sample sample
    SeqPlatform seqPlatform
    Run run
    SampleType sampleType
    Individual individual

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
                        identifier: 0,
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

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
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
        processedBamFile = null
        seqTrack = null
        mergingSet = null
        softwareTool = null
        seqType = null
        sample = null
        seqPlatform = null
        run = null
        sampleType = null
        individual = null
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULT() {
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFile, processedBamFileAct[0])
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFiles() {
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
                        identifier: 0,
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

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile, processedBamFile2]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTThreeBamFilesEqualSeqTrack() {
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
                        identifier: 0,
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

        AlignmentPass alignmentPass3 = new AlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack2,
                        description: "test"
                        )
        assertNotNull(alignmentPass3.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile3 = new ProcessedBamFile(
                        alignmentPass: alignmentPass3,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile3.save([flush: true, failOnError: true]))

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile, processedBamFile3]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test(expected = ValidationException)
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesNotSorted() {
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
                        type: BamType.RMDUP,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesSampleNotEqual() {
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
                        identifier: 0,
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

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesSeqPlatformNotEqual() {
        SeqPlatform seqPlatform2 = new SeqPlatform(
                        name: "name1",
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
                        identifier: 0,
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

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesSeqTypeNotEqual() {
        SeqType seqType2 = new SeqType(
                        name: "name",
                        libraryLayout: "library",
                        dirName: "dirName"
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
                        identifier: 0,
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

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULT() {
        assertTrue(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTTwoBamFiles() {
        AlignmentPass alignmentPass2 = new AlignmentPass(
                        identifier: 2,
                        seqTrack: seqTrack,
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

        assertTrue(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTWrongPlatform() {
        SeqPlatform seqPlatform2 = new SeqPlatform(
                        name: "name2",
                        model: "model2"
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

        assertFalse(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTWrongSample() {
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

        assertFalse(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testValidateBamFilesForMergingCriteriaDEFAULTWrongSeqType() {
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

        assertFalse(mergingCriteriaSpecificService.validateBamFilesForMergingCriteriaDEFAULT(mergingSet))
    }

    @Test
    void testMethodsForMergingCriteria() {
        List<MergingCriteria> mergingCriterias = MergingCriteria.values()
        mergingCriterias.each { MergingCriteria mergingCriteria ->
            assertNotNull(mergingCriteriaSpecificService."bamFilesForMergingCriteria${mergingCriteria}"(processedBamFile))
            assertNotNull(mergingCriteriaSpecificService."validateBamFilesForMergingCriteria${mergingCriteria}"(mergingSet))
        }
    }

    @Test
    void testBamFilesForMergingCriteriaDEFAULTTwoBamFilesSeqTypeNotEqualEqualNames() {
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
                        identifier: 0,
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

        List<ProcessedBamFile> processedBamFileExp = [processedBamFile,processedBamFile2]
        List<ProcessedBamFile> processedBamFileAct = mergingCriteriaSpecificService.bamFilesForMergingCriteriaDEFAULT(processedBamFile)
        assertEquals(processedBamFileExp, processedBamFileAct)
    }
}
