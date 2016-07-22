package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.TestCase
import grails.validation.ValidationException
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class MergingSetServiceTests {

    MergingSetService mergingSetService

    TestData testData = new TestData()
    Sample sample
    SeqType seqType
    SeqPlatform seqPlatform
    SeqTrack seqTrack
    SeqTrack seqTrack2

    @Before
    void setUp() {
        Project project = DomainFactory.createProject(
                        name: "name_1",
                        dirName: "dirName",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "pid_1",
                        mockPid: "mockPid_1",
                        mockFullName: "mockFullName_1",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "name_1"
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

        seqPlatform = SeqPlatform.build()

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

        SoftwareTool softwareTool = new SoftwareTool(
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
        seqTrack2 = new SeqTrack(
                        laneId: "laneId2",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
        )
        assertNotNull(seqTrack2.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        sample = null
        seqType = null
        seqTrack = null
    }

    @Test
    void testCheckIfMergingSetNotExistsAlreadyExist() {
        MergingWorkPackage workPackage =createMergingWorkPackage()
        ProcessedBamFile bamFile1 = createBamFile()
        ProcessedBamFile bamFile2 = createBamFile()
        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: workPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        bamFile: bamFile1,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        bamFile: bamFile2,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true, failOnError: true]))

        List<ProcessedBamFile> filesToMerge = [bamFile1, bamFile2]
        assertFalse(mergingSetService.checkIfMergingSetNotExists(filesToMerge, workPackage))
    }

    @Test
    void testCheckIfMergingSetNotExistsExistNot() {
        MergingWorkPackage workPackage =createMergingWorkPackage()
        ProcessedBamFile bamFile1 = createBamFile()
        ProcessedBamFile bamFile2 = createBamFile()
        List<ProcessedBamFile> filesToMerge = [bamFile1, bamFile2]
        assertTrue(mergingSetService.checkIfMergingSetNotExists(filesToMerge, workPackage))
    }

    @Test
    void testCheckIfMergingSetNotExistsAlreadyExistWithMergedFile() {
        MergingWorkPackage workPackage =createMergingWorkPackage()

        ProcessedBamFile bamFile1 = createBamFile()

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: workPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        bamFile: bamFile1,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        MergingPass mergingPass  = new MergingPass(
                        identifier: 1,
                        mergingSet:mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        mergingPass: mergingPass,
                        type: BamType.MDUP,
                        status: State.PROCESSED,
                        ])
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))

        ProcessedBamFile bamFile2 = createBamFile()

        MergingSet mergingSet1 = new MergingSet(
                        identifier: 1,
                        mergingWorkPackage: workPackage
                        )
        assertNotNull(mergingSet1.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        bamFile: bamFile2,
                        mergingSet: mergingSet1
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        bamFile: processedMergedBamFile,
                        mergingSet: mergingSet1
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        List<ProcessedBamFile> filesToMerge = [processedMergedBamFile, bamFile2]
        assertFalse(mergingSetService.checkIfMergingSetNotExists(filesToMerge, workPackage))
    }

    @Test
    void testCheckIfMergingSetNotExistsExistNotWithMergedFile() {
        MergingWorkPackage workPackage =createMergingWorkPackage()

        ProcessedBamFile bamFile1 = createBamFile()

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: workPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        bamFile: bamFile1,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        MergingPass mergingPass  = new MergingPass(
                        identifier: 1,
                        mergingSet:mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        type: BamType.MDUP,
                        status: State.PROCESSED,
                        ])
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))

        ProcessedBamFile bamFile2 = createBamFile()

        List<ProcessedBamFile> filesToMerge = [processedMergedBamFile, bamFile2]
        assertTrue(mergingSetService.checkIfMergingSetNotExists(filesToMerge, workPackage))
    }


    @Test
    void testNextMergingSet() {
        MergingSet next = mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.NORMAL_PRIORITY)
        assertNull(next)
        MergingSet mergingSet = MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)
        next = mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.NORMAL_PRIORITY)
        assertEquals(mergingSet, next)
        MergingSet mergingSet2 = MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)
        MergingSet mergingSet3 = MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)
        next = mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.NORMAL_PRIORITY)
        assertNotNull(next)
        assertTrue(next.equals(mergingSet) || next.equals(mergingSet2) || next.equals(mergingSet3) )
        mergingSet.status = MergingSet.State.INPROGRESS
        mergingSet.save([flush: true, failOnError: true])
        mergingSet2.status = MergingSet.State.INPROGRESS
        mergingSet2.save([flush: true, failOnError: true])
        mergingSet3.status = MergingSet.State.INPROGRESS
        mergingSet3.save([flush: true, failOnError: true])
        next = mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.NORMAL_PRIORITY)
        assertNull(next)
        MergingSet mergingSet4 = MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)
        next = mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.NORMAL_PRIORITY)
        assertNotNull(next)
        assertTrue(next.equals(mergingSet4))
    }

    @Test
    void testNextMergingSet_FastTrackFirst() {
        MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)

        MergingSet mergingSetFastTrack = MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)
        mergingSetFastTrack.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert mergingSetFastTrack.project.save(flush: true)

        assert mergingSetFastTrack == mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.NORMAL_PRIORITY)
    }

    @Test
    void testNextMergingSet_SlotsReservedForFastTrack() {
        MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)

        assertNull(mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.FAST_TRACK_PRIORITY))

        MergingSet mergingSetFastTrack = MergingSet.build(status: MergingSet.State.NEEDS_PROCESSING)
        mergingSetFastTrack.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert mergingSetFastTrack.project.save(flush: true)

        assert mergingSetFastTrack == mergingSetService.mergingSetInStateNeedsProcessing(ProcessingPriority.FAST_TRACK_PRIORITY)
    }


    @Test(expected = AssertionError)
    void testNextIdentifierIdentifierNull() {
        MergingSet.nextIdentifier(null)
    }

    @Test
    void testNextIdentifier() {
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        assertEquals(0, MergingSet.nextIdentifier(mergingWorkPackage))
        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        assertEquals(1, MergingSet.nextIdentifier(mergingWorkPackage))
        MergingSet mergingSet2 = new MergingSet(
                        identifier: 1,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet2.save([flush: true, failOnError: true]))
        assertEquals(2, MergingSet.nextIdentifier(mergingWorkPackage))
    }

    @Test
    void testCreateMergingSetForBamFile() {
        assertEquals(0, MergingWorkPackage.count)
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        ProcessedBamFile bamFile = createBamFile()
        mergingSetService.createMergingSetForBamFile(bamFile)
        assertEquals(1, MergingWorkPackage.count)
        assertEquals(1, MergingSet.count)
        assertEquals(1, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetForBamFileMergingPackageNotExistsTwoBamFiles_sameSeqTrack() {
        assertEquals(0, MergingWorkPackage.count)
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        ProcessedBamFile bamFile = createBamFile()
        ProcessedBamFile bamFile2 = createBamFile()
        mergingSetService.createMergingSetForBamFile(bamFile)
        assertEquals(1, MergingWorkPackage.count)
        assertEquals(1, MergingSet.count)
        // Only the latest BAM file for one SeqTrack is merged.
        assert TestCase.containSame(MergingSetAssignment.list()*.bamFile, [bamFile2])
        assertEquals(1, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetForBamFileMergingPackageNotExistsTwoBamFiles_differentSeqTracks() {
        assertEquals(0, MergingWorkPackage.count)
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        ProcessedBamFile bamFile = createBamFile()
        ProcessedBamFile bamFile2 = createBamFile(seqTrack2)
        assert bamFile.mergingWorkPackage == bamFile2.mergingWorkPackage
        mergingSetService.createMergingSetForBamFile(bamFile)
        assertEquals(1, MergingWorkPackage.count)
        assertEquals(1, MergingSet.count)
        assert TestCase.containSame(MergingSetAssignment.list()*.bamFile, [bamFile, bamFile2])
        assertEquals(2, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetForBamFileMergingPackageAlreadyExists() {
        MergingSet mergingSet = createMergingSet()
        mergingSet.status = MergingSet.State.PROCESSED
        ProcessedBamFile bamFile = createBamFile()
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: bamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        MergingPass mergingPass = new MergingPass(
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        type: AbstractBamFile.BamType.MDUP,
                        status: State.PROCESSED,
                        ])
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))

        bamFile.status = State.PROCESSED
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        assertEquals(1, MergingWorkPackage.count)
        assertEquals(1, MergingSet.count)
        assertEquals(1, MergingSetAssignment.count)
        ProcessedBamFile bamFile2 = createBamFile(seqTrack2)
        mergingSetService.createMergingSetForBamFile(bamFile2)
        assertEquals(1, MergingWorkPackage.count)
        final Collection<MergingSet> mergingSets = MergingSet.list()
        assertEquals(2, mergingSets.size())
        final MergingSet mergingSetCreatedByServiceMethod = exactlyOneElement(mergingSets - mergingSet)
        assert TestCase.containSame(MergingSetAssignment.findAllByMergingSet(mergingSet)*.bamFile, [bamFile])
        assert TestCase.containSame(MergingSetAssignment.findAllByMergingSet(mergingSetCreatedByServiceMethod)*.bamFile,
                [processedMergedBamFile, bamFile2])
        assertEquals(3, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetForBamFileMergingPackageAlreadyExistsNotFinished() {
        MergingSet mergingSet = createMergingSet()
        ProcessedBamFile bamFile = createBamFile()
        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: bamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        MergingPass mergingPass = new MergingPass(
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass, [
                        type: AbstractBamFile.BamType.MDUP,
                        status: State.INPROGRESS,
                        ])
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))

        bamFile.status = State.PROCESSED
        assertNotNull(bamFile.save([flush: true, failOnError: true]))

        assertEquals(1, MergingWorkPackage.count)
        assertEquals(1, MergingSet.count)
        assertEquals(1, MergingSetAssignment.count)
        ProcessedBamFile bamFile2 = createBamFile()
        mergingSetService.createMergingSetForBamFile(bamFile2)
        assertEquals(1, MergingWorkPackage.count)
        assertEquals(2, MergingSet.count)
        assertEquals(2, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSet() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        ProcessedBamFile bamFile = createBamFile()
        bamFiles.add(bamFile)
        mergingSetService.createMergingSet(bamFiles)
        assertEquals(1, MergingSet.count)
        assertEquals(1, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetTwoBamFiles() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        ProcessedBamFile bamFile = createBamFile()
        bamFiles.add(bamFile)
        ProcessedBamFile bamFile2 = DomainFactory.createProcessedBamFile(bamFile.mergingWorkPackage)
        bamFiles.add(bamFile2)
        mergingSetService.createMergingSet(bamFiles)
        assertEquals(1, MergingSet.count)
        assertEquals(2, MergingSetAssignment.count)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateMergingSetNoBamFiles() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        mergingSetService.createMergingSet(bamFiles)
    }

    @Test
    void testCreateMergingSetAlreadyExists() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        ProcessedBamFile bamFile = createBamFile()
        bamFiles.add(bamFile)
        mergingSetService.createMergingSet(bamFiles)
        bamFile.status = State.NEEDS_PROCESSING
        assertEquals(State.NEEDS_PROCESSING, bamFile.status)
        mergingSetService.createMergingSet(bamFiles)
        assertEquals(State.PROCESSED, bamFile.status)
    }

    @Test(expected = ValidationException)
    void testAssertSaveFails() {
        MergingSet mergingSet = new MergingSet()
        mergingSet.status = MergingSet.State.PROCESSED
        mergingSetService.assertSave(mergingSet)
    }

    @Test
    void testAssertSave() {
        MergingSet mergingSet = createMergingSet()
        assertEquals(mergingSet, mergingSetService.assertSave(mergingSet))
    }

    private ProcessedBamFile createBamFile(final SeqTrack seqTrack = this.seqTrack) {
        AlignmentPass alignmentPass = testData.createAlignmentPass(
                        identifier: AlignmentPass.nextIdentifier(seqTrack),
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
        return processedBamFile
    }


    private MergingWorkPackage createMergingWorkPackage() {
        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage(
                        sample: sample,
                        seqType: seqType,
                        seqPlatformGroup: seqPlatform.seqPlatformGroup,
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))
        return mergingWorkPackage
    }

    private MergingSet createMergingSet() {
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: de.dkfz.tbi.otp.dataprocessing.MergingSet.State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))
        return mergingSet
    }
}
