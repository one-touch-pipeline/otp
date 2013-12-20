package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

class AbstractBamFileServiceTests {

    AbstractBamFileService abstractBamFileService

    SeqCenter seqCenter
    Run run
    SeqPlatform seqPlatform
    MergingSet mergingSet
    MergingPass mergingPass
    AlignmentPass alignmentPass
    ProcessedBamFile processedBamFile

    @Before
    void setUp() {
        Project project = new Project(
                        name: "project",
                        dirName: "project-dir",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        SeqType seqType = new SeqType(
                        name:"seq-type",
                        libraryLayout:"library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        seqCenter = new SeqCenter(
                        name: "seq-center",
                        dirName: "seq-center-dir"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "software-tool",
                        programVersion: "software-tool-version",
                        qualityCode: "software-tool-quality-code",
                        type: SoftwareTool.Type.BASECALLING
                        )
        assertNotNull(softwareTool.save([flush: true]))

        seqPlatform = new SeqPlatform(
                        name: "seq-platform",
                        model: "seq-platform-model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

        run = createRun("run")
        assertNotNull(run.save([flush: true]))

        SeqTrack seqTrack = new SeqTrack(
                        laneId: "0",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))


        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true]))

        mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        alignmentPass = new AlignmentPass(
                        identifier: 0,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true]))

        processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile.save([flush: true]))
    }

    public Run createRun(String name) {
        return new Run(
                name: name,
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
                storageRealm: Run.StorageRealm.DKFZ
                )
    }

    @After
    void tearDown() {
        mergingSet = null
        mergingPass = null
        alignmentPass = null
    }

    @Test(expected = IllegalArgumentException)
    void testFindByMergingSetIsNull() {
        abstractBamFileService.findByMergingSet(null)
    }

    @Test
    void testFindByMergingSet() {
        List<AbstractBamFile> abstractBamFilesExp
        List<AbstractBamFile> abstractBamFilesAct

        abstractBamFilesExp = []
        abstractBamFilesAct = abstractBamFileService.findByMergingSet(mergingSet)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedBamFile processedBamFile1 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        fileExists: true
                        )
        assertNotNull(processedBamFile1.save([flush: true]))

        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile1
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true]))

        abstractBamFilesExp = []
        abstractBamFilesExp.add(processedBamFile1)
        abstractBamFilesAct = abstractBamFileService.findByMergingSet(mergingSet)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        fileExists: true
                        )

        assertNotNull(processedBamFile2.save([flush: true]))
        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true]))

        abstractBamFilesExp.add(processedBamFile2)
        abstractBamFilesAct = abstractBamFileService.findByMergingSet(mergingSet)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        MergingSetAssignment mergingSetAssignment3 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedMergedBamFile
                        )
        assertNotNull(mergingSetAssignment3.save([flush: true]))

        abstractBamFilesExp.add(processedMergedBamFile)
        abstractBamFilesAct = abstractBamFileService.findByMergingSet(mergingSet)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFindByProcessedMergedBamFileIsNull() {
        abstractBamFileService.findByProcessedMergedBamFile(null)
    }

    @Test
    void testFindByProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        List<AbstractBamFile> abstractBamFilesExp
        List<AbstractBamFile> abstractBamFilesAct

        abstractBamFilesExp = []
        abstractBamFilesAct = abstractBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedBamFile processedBamFile1 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        fileExists: true
                        )
        assertNotNull(processedBamFile1.save([flush: true]))

        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile1
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true]))

        abstractBamFilesExp = []
        abstractBamFilesExp.add(processedBamFile1)
        abstractBamFilesAct = abstractBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        fileExists: true
                        )

        assertNotNull(processedBamFile2.save([flush: true]))
        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true]))

        abstractBamFilesExp.add(processedBamFile2)
        abstractBamFilesAct = abstractBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)

        ProcessedMergedBamFile processedMergedBamFile2 = createProcessedMergedBamFile()
        MergingSetAssignment mergingSetAssignment3 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedMergedBamFile2
                        )
        assertNotNull(mergingSetAssignment3.save([flush: true]))

        abstractBamFilesExp.add(processedMergedBamFile2)
        abstractBamFilesAct = abstractBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        assertEquals(abstractBamFilesExp, abstractBamFilesAct)
    }

    public ProcessedMergedBamFile createProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))
        return processedMergedBamFile
    }

    @Test
    void testAssignedToMergingSet() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertEquals(processedBamFile.status, AbstractBamFile.State.NEEDS_PROCESSING)
        abstractBamFileService.assignedToMergingSet(processedBamFile)
        assertEquals(processedBamFile.status, AbstractBamFile.State.PROCESSED)
    }

    @Test @Ignore
    void testFindAllByProcessedMergedBamFile() {

    }

}
