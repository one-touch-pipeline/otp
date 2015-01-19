package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.filehandling.BwaLogFileParser
import de.dkfz.tbi.otp.ngsqc.FastqcBasicStatistics

import static org.junit.Assert.*

import org.junit.*

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class ProcessedBamFileServiceTests extends GroovyTestCase {

    ProcessedBamFileService processedBamFileService

    TestData testData

    ProcessedBamFile processedBamFile
    SeqType seqType
    Sample sample
    SeqPlatform seqPlatform
    Run run
    SoftwareTool softwareTool
    SeqTrack seqTrack
    Individual individual
    SampleType sampleType
    AlignmentPass alignmentPass
    RunSegment runSegment
    FastqcProcessedFile fastqcProcessedFile

    static final long READ_NUMBER = 12345

    @Before
    void setUp() {
        testData = new TestData()

        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()
        assertNotNull(realm.save([flush: true, failOnError: true]))

        Project project = new Project(
                        name: "name",
                        dirName: "dirName",
                        realmName: realm.name,
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

        runSegment = testData.createRunSegment(run)
        assertNotNull(runSegment.save([flush: true, failOnError: true]))

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
                        pipelineVersion: softwareTool,
                        alignmentState: SeqTrack.DataProcessingState.FINISHED
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        DataFile datafile = testData.createDataFile([
                fileName: "datafile1.fastq",
                fileType: testData.createFileType(FileType.Type.SEQUENCE),
                seqTrack: seqTrack,
                runSegment: runSegment,
                run: run,
        ])
        assertNotNull(datafile.save([flush: true, failOnError: true]))


        fastqcProcessedFile = testData.createFastqcProcessedFile([dataFile: datafile])
        assertNotNull(fastqcProcessedFile.save([flush: true, failOnError: true]))

        FastqcBasicStatistics fastqcBasicStatistics = testData.createFastqcBasicStatistics([
                totalSequences: READ_NUMBER,
                fastqcProcessedFile: fastqcProcessedFile
        ])
        assertNotNull(fastqcBasicStatistics.save([flush: true, failOnError: true]))

        alignmentPass = new AlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))

        ProcessedSaiFile processedSaiFile = testData.createProcessedSaiFile([
                alignmentPass: alignmentPass,
                dataFile: datafile,
        ])
        assertNotNull(processedSaiFile.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        processedBamFile = null
        seqType = null
        sample = null
        seqPlatform = null
        softwareTool = null
        run = null
        individual = null
        sampleType = null
        runSegment = null
        alignmentPass = null
    }

    private void prepareSetNeedsProcessing(
            AbstractBamFile.State bamFileStatus,
            SeqTrack.DataProcessingState alignmentState) {
        processedBamFile.status = bamFileStatus
        assertNotNull processedBamFile.save(flush: true)
        seqTrack.alignmentState = alignmentState
        assertNotNull seqTrack.save(flush: true)
    }

    @Test
    void testSetNeedsProcessing() {

        shouldFail(IllegalArgumentException.class) {
            processedBamFileService.setNeedsProcessing(null)
        }

        // check preconditions
        assertEquals(seqTrack, processedBamFile.alignmentPass.seqTrack)
        assertEquals(seqTrack, processedBamFile.seqTrack)

        prepareSetNeedsProcessing(AbstractBamFile.State.DECLARED, SeqTrack.DataProcessingState.FINISHED)
        processedBamFileService.setNeedsProcessing(processedBamFile)
        assertEquals(AbstractBamFile.State.NEEDS_PROCESSING, processedBamFile.status)

        prepareSetNeedsProcessing(AbstractBamFile.State.NEEDS_PROCESSING, SeqTrack.DataProcessingState.FINISHED)
        processedBamFileService.setNeedsProcessing(processedBamFile)
        assertEquals(AbstractBamFile.State.NEEDS_PROCESSING, processedBamFile.status)

        prepareSetNeedsProcessing(AbstractBamFile.State.INPROGRESS, SeqTrack.DataProcessingState.FINISHED)
        shouldFail AssertionError, { processedBamFileService.setNeedsProcessing(processedBamFile) }
        assertEquals(AbstractBamFile.State.INPROGRESS, processedBamFile.status)

        prepareSetNeedsProcessing(AbstractBamFile.State.DECLARED, SeqTrack.DataProcessingState.IN_PROGRESS)
        shouldFail AssertionError, { processedBamFileService.setNeedsProcessing(processedBamFile) }
        assertEquals(AbstractBamFile.State.DECLARED, processedBamFile.status)
    }

    @Test
    void testProcessedBamFileNeedsProcessingAllCorrect() {
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert processedBamFileService.isMergeable(processedBamFile)
        assertEquals(processedBamFile.id, processedBamFileService.processedBamFileNeedsProcessing().id)
    }

    @Test
    //if a processed merged bam file is marked as withdrawn, the state of qa should be ignored
    void testProcessedBamFileNeedsProcessingAllCorrectWithWithdrawnProcessedBamFile() {
        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId2",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        alignmentState: SeqTrack.DataProcessingState.FINISHED
                        )
        assertNotNull(seqTrack2.save([flush: true]))

        AlignmentPass alignmentPass = new AlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack2,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true]))

        ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        withdrawn: true,
                        qualityAssessmentStatus: QaProcessingStatus.IN_PROGRESS,
                        status: State.PROCESSED
                        )
        assertNotNull(processedBamFile2.save([flush: true]))
        List<SeqTrack> seqTracks = [processedBamFile.seqTrack, processedBamFile2.seqTrack]

        assert !processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isMergingInProgress(processedBamFile2.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert processedBamFileService.isMergeable(processedBamFile)
        assert !processedBamFileService.isMergeable(processedBamFile2)
        assertEquals(processedBamFile.id, processedBamFileService.processedBamFileNeedsProcessing().id)
    }


    @Test
    void testProcessedBamFileNeedsProcessingAllCorrectWithOldNotFinishedPass() {
        AlignmentPass oldAlignmentPassToIgnore = testData.createAlignmentPass(
                        identifier: 0,
                        seqTrack: seqTrack,
                        )
        assertNotNull(oldAlignmentPassToIgnore.save([flush: true]))

        ProcessedBamFile oldProcessedBamFileToIgnore = testData.createProcessedBamFile(
                        alignmentPass: oldAlignmentPassToIgnore,
                        )
        assertNotNull(oldProcessedBamFileToIgnore.save([flush: true]))

        List<SeqTrack> seqTracks = [processedBamFile.seqTrack]

        assert !processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert processedBamFileService.isMergeable(processedBamFile)
        assertEquals(processedBamFile.id, processedBamFileService.processedBamFileNeedsProcessing().id)
    }


    @Test
    //if a seq track contains withdrawn data, the state of alignment should be ignored
    void testProcessedBamFileNeedsProcessingAllCorrectWithWithdrawnSeqTrack() {
        SeqTrack seqTrack2 = new SeqTrack(
                        laneId: "laneId2",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        alignmentState: SeqTrack.DataProcessingState.IN_PROGRESS
                        )
        assertNotNull(seqTrack2.save([flush: true]))

        DataFile dataFile = testData.createDataFile(seqTrack2, null)
        dataFile.fileWithdrawn = true
        assertNotNull(dataFile.save([flush: true]))

        List<SeqTrack> seqTracks = [processedBamFile.seqTrack, seqTrack2]

        assert !processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert processedBamFileService.isMergeable(processedBamFile)
        assertEquals(processedBamFile.id, processedBamFileService.processedBamFileNeedsProcessing().id)
    }


    @Test
    void testProcessedBamFileNeedsProcessingNoBamFileNeedsProcessing() {
        processedBamFile.status = State.DECLARED
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergeable(processedBamFile)
        assertNull(processedBamFileService.processedBamFileNeedsProcessing())
    }

    @Test
    void testProcessedBamFileNeedsProcessingQaNotFinished() {
        processedBamFile.qualityAssessmentStatus = QaProcessingStatus.IN_PROGRESS
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert processedBamFileService.isMergeable(processedBamFile)
        assertEquals(processedBamFile.id, processedBamFileService.processedBamFileNeedsProcessing().id)
    }

    @Test
    void testRelatedBamFileIsNotProcessable() {
        final AlignmentPass alignmentPass2 = new AlignmentPass(
                identifier: 2,
                seqTrack: seqTrack,
                description: "test"
        )
        assertNotNull(alignmentPass2.save([flush: true, failOnError: true]))
        final ProcessedBamFile processedBamFile2 = new ProcessedBamFile(
            alignmentPass: alignmentPass2,
            type: BamType.SORTED,
            qualityAssessmentStatus: QaProcessingStatus.FINISHED,
            status: State.NEEDS_PROCESSING
        )
        assert processedBamFile2.save()
        assert processedBamFile2.id > processedBamFile.id

        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert processedBamFileService.isMergeable(processedBamFile)
        assertEquals(processedBamFile, processedBamFileService.processedBamFileNeedsProcessing())

        processedBamFile2.status = State.DECLARED
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergeable(processedBamFile)
        assertNull(processedBamFileService.processedBamFileNeedsProcessing())

        processedBamFile2.status = State.NEEDS_PROCESSING
        processedBamFile2.qualityAssessmentStatus = QaProcessingStatus.IN_PROGRESS
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert processedBamFileService.isMergeable(processedBamFile)
        assertEquals(processedBamFile.id, processedBamFileService.processedBamFileNeedsProcessing().id)
    }

    @Test
    void testProcessedBamFileNeedsProcessing() {
        //no mergingSet exists
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert processedBamFileService.isMergeable(processedBamFile)
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
                        alignmentState: SeqTrack.DataProcessingState.FINISHED,
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
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(processedBamFile2.save([flush: true, failOnError: true]))

        //a mergingSet exists for processedBamFile and is not finished
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergeable(processedBamFile)
        assert !processedBamFileService.isAnyAlignmentPending([seqTrack2])
        assert !processedBamFileService.isMergingInProgress(processedBamFile2.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([seqTrack2])
        assert processedBamFileService.isMergeable(processedBamFile2)
        assertEquals(processedBamFile2, processedBamFileService.processedBamFileNeedsProcessing())

        mergingSet.status = MergingSet.State.PROCESSED
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        //a mergingSet exists for processedBamFile and is finished
        assert !processedBamFileService.isAnyAlignmentPending([processedBamFile.seqTrack])
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([processedBamFile.seqTrack])
        assert processedBamFileService.isMergeable(processedBamFile)
        assert !processedBamFileService.isAnyAlignmentPending([seqTrack2])
        assert !processedBamFileService.isMergingInProgress(processedBamFile2.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable([seqTrack2])
        assert processedBamFileService.isMergeable(processedBamFile2)
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
                        alignmentState: SeqTrack.DataProcessingState.FINISHED,
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
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
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

        final Iterable<SeqTrack> seqTracks = [processedBamFile.seqTrack, seqTrack2, seqTrack3].asImmutable()

        assert processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert !processedBamFileService.isMergeable(processedBamFile)
        assert processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile2.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert !processedBamFileService.isMergeable(processedBamFile2)
        assertNull(processedBamFileService.processedBamFileNeedsProcessing())

        seqTrack3.alignmentState = SeqTrack.DataProcessingState.IN_PROGRESS
        assertNotNull(seqTrack3.save([flush: true, failOnError: true]))

        assert processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert !processedBamFileService.isMergeable(processedBamFile)
        assert processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile2.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert !processedBamFileService.isMergeable(processedBamFile2)
        assertNull(processedBamFileService.processedBamFileNeedsProcessing())

        seqTrack3.alignmentState = SeqTrack.DataProcessingState.FINISHED
        assertNotNull(seqTrack3.save([flush: true, failOnError: true]))

        assert !processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert processedBamFileService.isMergeable(processedBamFile)
        assert !processedBamFileService.isAnyAlignmentPending(seqTracks)
        assert !processedBamFileService.isMergingInProgress(processedBamFile2.seqTrack)
        assert !processedBamFileService.isAnyBamFileNotProcessable(seqTracks)
        assert processedBamFileService.isMergeable(processedBamFile2)
        assertEquals(processedBamFile2, processedBamFileService.processedBamFileNeedsProcessing())
    }

    @Test
    void testBlockedForAssigningToMergingSet() {
        assertEquals(processedBamFile.status, State.NEEDS_PROCESSING)
        processedBamFileService.blockedForAssigningToMergingSet(processedBamFile)
        assertEquals(processedBamFile.status, State.INPROGRESS)
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

    @Test
    void testIsMergeable() {
        shouldFail IllegalArgumentException.class,
                { processedBamFileService.isMergeable(null) }
        // Further tests of isMergeable are included in the test methods above.
    }

    @Test
    void testIsAnyAlignmentPending() {
        shouldFail IllegalArgumentException.class,
                { processedBamFileService.isAnyAlignmentPending(null) }
        final SeqTrack finished = new SeqTrack(
                        alignmentState: SeqTrack.DataProcessingState.FINISHED,
                        laneId: "lane finished",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        )
        assertNotNull(finished.save([flush: true]))

        final SeqTrack unknown = new SeqTrack(
                        alignmentState: SeqTrack.DataProcessingState.UNKNOWN,
                        laneId: "lane unknown",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        )
        assertNotNull(unknown.save([flush: true]))

        assert !processedBamFileService.isAnyAlignmentPending([])
        assert !processedBamFileService.isAnyAlignmentPending([finished])
        assert processedBamFileService.isAnyAlignmentPending([unknown])
        assert !processedBamFileService.isAnyAlignmentPending([finished, finished])
        assert processedBamFileService.isAnyAlignmentPending([finished, unknown])
        assert processedBamFileService.isAnyAlignmentPending([unknown, finished])
        assert processedBamFileService.isAnyAlignmentPending([unknown, unknown])
        // Further tests of isAnyAlignmentPending are included in the test methods above.
    }

    @Test
    void testIsMergingInProgress() {
        shouldFail IllegalArgumentException.class,
                { processedBamFileService.isMergingInProgress(null) }
        // Further tests of isMergingInProgress are included in the test methods above.
    }

    @Test
    void testIsAnyBamFileNotProcessable() {
        shouldFail IllegalArgumentException.class,
                { processedBamFileService.isAnyBamFileNotProcessable(null) }
        assert !processedBamFileService.isAnyBamFileNotProcessable([])
        // Further tests of isAnyBamFileNotProcessable are included in the test methods above.
    }

    void testGetAlignmentReadLength_ProcessedBamFileIsNull() {
        shouldFail(IllegalArgumentException, { processedBamFileService.getAlignmentReadLength(null) })
    }

    void testGetAlignmentReadLength_LibrarySingle() {
        BwaLogFileParser.metaClass.static.parseReadNumberFromLog = { File file -> READ_NUMBER }

        try {
            assert READ_NUMBER == processedBamFileService.getAlignmentReadLength(processedBamFile)
        } finally {
            BwaLogFileParser.metaClass.static.parseReadNumberFromLog = null
        }
    }

    void testGetAlignmentReadLength_LibraryPaired() {
        BwaLogFileParser.metaClass.static.parseReadNumberFromLog = { File file -> READ_NUMBER }

        DataFile datafile2 = testData.createDataFile([
                fileName: "datafile2.fastq",
                fileType: testData.createFileType(FileType.Type.SEQUENCE),
                seqTrack: seqTrack,
                runSegment: runSegment,
                run: run,
        ])
        assertNotNull(datafile2.save([flush: true, failOnError: true]))

        ProcessedSaiFile processedSaiFile2 = testData.createProcessedSaiFile([
                alignmentPass: alignmentPass,
                dataFile: datafile2,])
        assertNotNull(processedSaiFile2.save([flush: true, failOnError: true]))

        try {
            assert READ_NUMBER * 2 == processedBamFileService.getAlignmentReadLength(processedBamFile)
        } finally {
            BwaLogFileParser.metaClass.static.parseReadNumberFromLog = null
        }
    }

    void testGetFastQCReadLength_ProcessedBamFileIsNull() {
        shouldFail(IllegalArgumentException, {processedBamFileService.getFastQCReadLength(null)})
    }

    void testGetFastQCReadLength_LibrarySingle() {
        assert READ_NUMBER == processedBamFileService.getFastQCReadLength(processedBamFile)
    }

    void testGetFastQCReadLength_LibraryPaired() {

        DataFile datafile2 = testData.createDataFile([
                fileName: "datafile2.fastq",
                fileType: testData.createFileType(FileType.Type.SEQUENCE),
                seqTrack: seqTrack,
                runSegment: runSegment,
                run: run,
        ])
        assertNotNull(datafile2.save([flush: true, failOnError: true]))

        FastqcProcessedFile fastqcProcessedFile2 = testData.createFastqcProcessedFile([dataFile: datafile2])
        assertNotNull(fastqcProcessedFile2.save([flush: true, failOnError: true]))

        FastqcBasicStatistics fastqcBasicStatistics2 = testData.createFastqcBasicStatistics([totalSequences: READ_NUMBER, fastqcProcessedFile: fastqcProcessedFile2])
        assertNotNull(fastqcBasicStatistics2.save([flush: true, failOnError: true]))

        assert READ_NUMBER * 2 == processedBamFileService.getFastQCReadLength(processedBamFile)
    }

    void testGetFastQCReadLength_LibrarySingle_WrongCountofStatistic() {
        FastqcBasicStatistics fastqcBasicStatistics2 = testData.createFastqcBasicStatistics([totalSequences: READ_NUMBER, fastqcProcessedFile: fastqcProcessedFile])
        assertNotNull(fastqcBasicStatistics2.save([flush: true, failOnError: true]))

        String message = shouldFail (RuntimeException) {
            processedBamFileService.getFastQCReadLength(processedBamFile)
        }
        assert message.startsWith('Fail to fetch exactly one FastqcBasicStatistics for ')
    }

}
