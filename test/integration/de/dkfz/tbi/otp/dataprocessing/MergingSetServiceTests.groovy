package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.validation.ValidationException
import org.junit.*

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class MergingSetServiceTests {

    MergingSetService mergingSetService

    Sample sample
    SeqType seqType
    SeqTrack seqTrack

    @Before
    void setUp() {
        Project project = new Project(
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

        SeqPlatform seqPlatform = new SeqPlatform(
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
    void testNextMergingSet() {
        MergingSet next = mergingSetService.mergingSetInStateNeedsProcessing()
        assertNull(next)

        MergingSet mergingSet = createMergingSet()
        next = mergingSetService.mergingSetInStateNeedsProcessing()
        assertEquals(mergingSet, next)

        MergingSet mergingSet2 = createMergingSet()
        MergingSet mergingSet3 = createMergingSet()
        next = mergingSetService.mergingSetInStateNeedsProcessing()
        assertNotNull(next)
        assertTrue(next.equals(mergingSet) || next.equals(mergingSet2) || next.equals(mergingSet3) )

        mergingSet.status = MergingSet.State.INPROGRESS
        mergingSet.save([flush: true, failOnError: true])
        mergingSet2.status = MergingSet.State.INPROGRESS
        mergingSet2.save([flush: true, failOnError: true])
        mergingSet3.status = MergingSet.State.INPROGRESS
        mergingSet3.save([flush: true, failOnError: true])
        next = mergingSetService.mergingSetInStateNeedsProcessing()
        assertNull(next)

        MergingSet mergingSet4 = createMergingSet()
        next = mergingSetService.mergingSetInStateNeedsProcessing()
        assertNotNull(next)
        assertTrue(next.equals(mergingSet4))
    }

    @Test(expected = IllegalArgumentException)
    void testNextIdentifierIdentifierNull() {
        mergingSetService.nextIdentifier(null)
    }

    @Test
    void testNextIdentifier() {
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()

        assertEquals(0, mergingSetService.nextIdentifier(mergingWorkPackage))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        assertEquals(1, mergingSetService.nextIdentifier(mergingWorkPackage))

        MergingSet mergingSet2 = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet2.save([flush: true, failOnError: true]))

        assertEquals(2, mergingSetService.nextIdentifier(mergingWorkPackage))
    }


    @Test
    void testCreateMergingSetForBamFileMergingPackageNotExists() {
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
    void testCreateMergingSetForBamFileMergingPackageNotExistsTwoBamFiles() {
        assertEquals(0, MergingWorkPackage.count)
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        ProcessedBamFile bamFile = createBamFile()
        ProcessedBamFile bamFile2 = createBamFile()
        mergingSetService.createMergingSetForBamFile(bamFile)
        assertEquals(1, MergingWorkPackage.count)
        assertEquals(1, MergingSet.count)
        assertEquals(2, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetForBamFileMergingPackageExists() {
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        assertEquals(1, MergingWorkPackage.count)
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        ProcessedBamFile bamFile = createBamFile()
        mergingSetService.createMergingSetForBamFile(bamFile)
        assertEquals(1, MergingWorkPackage.count)
        assertEquals(1, MergingSet.count)
        assertEquals(1, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSet() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        MergingWorkPackage workPackage = createMergingWorkPackage()
        ProcessedBamFile bamFile = createBamFile()
        bamFiles.add(bamFile)
        mergingSetService.createMergingSet(bamFiles, workPackage)
        assertEquals(1, MergingSet.count)
        assertEquals(1, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetTwoBamFiles() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        MergingWorkPackage workPackage = createMergingWorkPackage()
        ProcessedBamFile bamFile = createBamFile()
        bamFiles.add(bamFile)
        ProcessedBamFile bamFile2 = createBamFile()
        bamFiles.add(bamFile2)
        mergingSetService.createMergingSet(bamFiles, workPackage)
        assertEquals(1, MergingSet.count)
        assertEquals(2, MergingSetAssignment.count)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateMergingSetNoBamFiles() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        MergingWorkPackage workPackage = createMergingWorkPackage()
        mergingSetService.createMergingSet(bamFiles, workPackage)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateMergingSetWorkingPackageNull() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        ProcessedBamFile bamFile = createBamFile()
        bamFiles.add(bamFile)
        mergingSetService.createMergingSet(bamFiles, null)
        assertEquals(1, MergingSet.count)
        assertEquals(1, MergingSetAssignment.count)
    }

    @Test
    void testCreateMergingSetAlreadyExists() {
        assertEquals(0, MergingSet.count)
        assertEquals(0, MergingSetAssignment.count)
        List<ProcessedBamFile> bamFiles = []
        MergingWorkPackage workPackage = createMergingWorkPackage()
        ProcessedBamFile bamFile = createBamFile()
        bamFiles.add(bamFile)
        ProcessedBamFile bamFile2 = createBamFile()
        bamFiles.add(bamFile2)
        mergingSetService.createMergingSet(bamFiles, workPackage)
        bamFile.status = State.NEEDS_PROCESSING
        assertEquals(State.NEEDS_PROCESSING, bamFile.status)
        mergingSetService.createMergingSet(bamFiles, workPackage)
        assertEquals(State.PROCESSED, bamFile.status)
    }

    @Test(expected = ValidationException.class)
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


    private ProcessedBamFile createBamFile() {
        AlignmentPass alignmentPass = new AlignmentPass(
                        identifier: 1,
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
        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
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
