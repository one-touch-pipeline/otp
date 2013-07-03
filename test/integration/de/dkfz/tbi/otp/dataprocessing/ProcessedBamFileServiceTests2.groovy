package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

class ProcessedBamFileServiceTests2 {

    ProcessedBamFileService processedBamFileService

    MergingSet mergingSet

    MergingPass mergingPass

    AlignmentPass alignmentPass

    @Before
    void setUp() {
        Project project = new Project(
                        name: "project",
                        dirName: "project-dir",
                        realmName: "realmName"
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = new SeqType(
                        name:"seq-type",
                        libraryLayout:"library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        SeqCenter seqCenter = new SeqCenter(
                        name: "seq-center",
                        dirName: "seq-center-dir"
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "software-tool",
                        programVersion: "software-tool-version",
                        qualityCode: "software-tool-quality-code",
                        type: SoftwareTool.Type.BASECALLING
                        )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
                        name: "seq-platform",
                        model: "seq-platform-model"
                        )
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))

        Run run = new Run(
                        name: "run",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: Run.StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
                        laneId: "0",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))


        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        alignmentPass = new AlignmentPass(
                        identifier: 0,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        mergingSet = null
        mergingPass = null
        alignmentPass = null
    }

    @Test(expected = IllegalArgumentException)
    void testFindByMergingSetIsNull() {
        processedBamFileService.findByMergingSet(null)
    }

    @Test
    void testFindByMergingSet() {
        List<ProcessedBamFile> processedBamFilesExp
        List<ProcessedBamFile> processedBamFilesAct

        processedBamFilesExp = []
        processedBamFilesAct = processedBamFileService.findByMergingSet(mergingSet)
        assertEquals(processedBamFilesExp, processedBamFilesAct)

        ProcessedBamFile processedBamFile1 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.MDUP,
                        fileExists: true
                        )
        assertNotNull(processedBamFile1.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile1
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true, failOnError: true]))

        processedBamFilesExp = []
        processedBamFilesExp.add(processedBamFile1)
        processedBamFilesAct = processedBamFileService.findByMergingSet(mergingSet)
        assertEquals(processedBamFilesExp, processedBamFilesAct)

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.MDUP,
                        fileExists: true
                        )

        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))
        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        processedBamFilesExp.add(processedBamFile2)
        processedBamFilesAct = processedBamFileService.findByMergingSet(mergingSet)
        assertEquals(processedBamFilesExp, processedBamFilesAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFindByProcessedMergedBamFileIsNull() {
        processedBamFileService.findByProcessedMergedBamFile(null)
    }

    @Test
    void testFindByProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        List<ProcessedBamFile> processedBamFilesExp
        List<ProcessedBamFile> processedBamFilesAct

        processedBamFilesExp = []
        processedBamFilesAct = processedBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        assertEquals(processedBamFilesExp, processedBamFilesAct)

        ProcessedBamFile processedBamFile1 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.MDUP,
                        fileExists: true
                        )
        assertNotNull(processedBamFile1.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile1
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true, failOnError: true]))

        processedBamFilesExp = []
        processedBamFilesExp.add(processedBamFile1)
        processedBamFilesAct = processedBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        assertEquals(processedBamFilesExp, processedBamFilesAct)

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.MDUP,
                        fileExists: true
                        )

        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))
        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile2
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        processedBamFilesExp.add(processedBamFile2)
        processedBamFilesAct = processedBamFileService.findByProcessedMergedBamFile(processedMergedBamFile)
        assertEquals(processedBamFilesExp, processedBamFilesAct)
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP
                        )
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))
        return processedMergedBamFile
    }
}

