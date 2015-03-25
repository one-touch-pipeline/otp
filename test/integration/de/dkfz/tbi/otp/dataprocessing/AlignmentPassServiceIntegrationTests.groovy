package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsdata.*

class AlignmentPassServiceIntegrationTests extends TestData {

    AlignmentPassService alignmentPassService

    @Test
    void testFindAlignmentPassForProcessing() {
        createObjects()
        dataFile.delete()
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED

        findAlignmentPassForProcessingTest(AlignmentState.IN_PROGRESS)
        findAlignmentPassForProcessingTest(AlignmentState.NOT_STARTED)

        AlignmentPass alignmentPass = createAndSaveAlignmentPass(
                seqTrack: seqTrack,
                alignmentState: AlignmentState.NOT_STARTED,
        )
        DataFile dataFile = createDataFile(false)
        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())

        dataFile.fileExists = false
        assertNotNull(dataFile.save(flush: true))
        assertNull(alignmentPassService.findAlignmentPassForProcessing())
        dataFile.fileExists = true
        assertNotNull(dataFile.save(flush: true))
        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())

        dataFile.fileSize = 0
        assertNotNull(dataFile.save(flush: true))
        assertNull(alignmentPassService.findAlignmentPassForProcessing())
        dataFile.fileSize = 1
        assertNotNull(dataFile.save(flush: true))
        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())

        fileType.type = FileType.Type.ALIGNMENT
        assertNotNull(fileType.save(flush: true))
        assertNull(alignmentPassService.findAlignmentPassForProcessing())
        fileType.type = FileType.Type.SEQUENCE
        assertNotNull(fileType.save(flush: true))
        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())

        RunSegment runSegment = new RunSegment()
        runSegment.run = run
        runSegment.metaDataStatus = RunSegment.Status.PROCESSING
        runSegment.filesStatus = RunSegment.FilesStatus.FILES_MISSING
        runSegment.initialFormat = RunSegment.DataFormat.TAR
        runSegment.currentFormat = RunSegment.DataFormat.TAR
        runSegment.dataPath = '/tmp/'
        runSegment.mdPath = '/tmp/'
        assertNotNull(runSegment.save(flush: true))
        assertNull(alignmentPassService.findAlignmentPassForProcessing())
        runSegment.metaDataStatus = RunSegment.Status.COMPLETE
        assertNotNull(runSegment.save(flush: true))
        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())

        referenceGenomeProjectSeqType.delete(flush: true)
        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack.alignmentState)

        alignmentPass.alignmentState = AlignmentState.NOT_STARTED
        assertNotNull(createReferenceGenomeProjectSeqType().save(flush: true))
        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())
    }

    @Test
    void testFindAlignmentPassForProcessing_fastqcMustBeFinished() {
        createObjects()
        createDataFile(false)
        AlignmentPass alignmentPass = createAndSaveAlignmentPass(
                seqTrack: seqTrack,
                alignmentState: AlignmentState.NOT_STARTED,
        )

        seqTrack.fastqcState = SeqTrack.DataProcessingState.IN_PROGRESS
        assertNull(alignmentPassService.findAlignmentPassForProcessing())

        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())
    }

    @Test
    void testFindAlignmentPassForProcessing_Exome_KitANDBedFileANDReferenceGenomeConnectionMissing() {
        createObjects()
        dataFile.delete()
        seqTrack.delete()
        ExomeSeqTrack exomeSeqTrack = createExomeSeqTrack(run)
        AlignmentPass alignmentPass = createAndSaveAlignmentPass(
                seqTrack: exomeSeqTrack,
                alignmentState: AlignmentState.NOT_STARTED,
        )
        exomeSeqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED

        assertNull(alignmentPassService.findAlignmentPassForProcessing())

        dataFile = createDataFile([seqTrack: exomeSeqTrack])
        dataFile.save(flush: true)
        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignmentPassForProcessing_Exome_KitANDBedFileMissing() {
        AlignmentPass alignmentPass = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()
        ExomeSeqTrack exomeSeqTrack = alignmentPass.seqTrack

        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)
        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignmentPassForProcessing_Exome_KitConnectionANDBedFileMissing() {
        AlignmentPass alignmentPass = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()
        ExomeSeqTrack exomeSeqTrack = alignmentPass.seqTrack

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit("libraryPreparationKit")
        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)

        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignmentPassForProcessing_Exome_BedFileMissing() {
        AlignmentPass alignmentPass = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()
        ExomeSeqTrack exomeSeqTrack = alignmentPass.seqTrack

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit("libraryPreparationKit")
        addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit)
        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)

        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignmentPassForProcessing_Exome_ReferenceGenomeAndBedFileMissing() {
        AlignmentPass alignmentPass = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()
        ExomeSeqTrack exomeSeqTrack = alignmentPass.seqTrack

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit("libraryPreparationKit")
        addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit)

        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignmentPassForProcessing_Exome_ReferenceGenomeAndKitMissing() {
        AlignmentPass alignmentPass = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()
        ExomeSeqTrack exomeSeqTrack = alignmentPass.seqTrack

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit("libraryPreparationKit")
        createBedFile(referenceGenome, libraryPreparationKit)

        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignmentPassForProcessing_Exome_ReferenceGenomeMissing() {
        AlignmentPass alignmentPass = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()
        ExomeSeqTrack exomeSeqTrack = alignmentPass.seqTrack

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit("libraryPreparationKit")
        createBedFile(referenceGenome, libraryPreparationKit)
        addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit)

        assert alignmentPassService.findAlignmentPassForProcessing() == alignmentPass
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignmentPassForProcessing_Exome_AllInformationAvailable() {
        AlignmentPass alignmentPass = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()
        ExomeSeqTrack exomeSeqTrack = alignmentPass.seqTrack

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit("libraryPreparationKit")
        createBedFile(referenceGenome, libraryPreparationKit)
        addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit)
        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)

        assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())
    }

    private void findAlignmentPassForProcessingTest(final AlignmentState state) {
        findAlignmentPassForProcessingTest(state, 0, 0)
        findAlignmentPassForProcessingTest(state, 1, 0)
        findAlignmentPassForProcessingTest(state, 0, 1)
        findAlignmentPassForProcessingTest(state, 0, 2)
        findAlignmentPassForProcessingTest(state, 1, 1)
        findAlignmentPassForProcessingTest(state, 2, 0)
        findAlignmentPassForProcessingTest(state, 0, 3)
        findAlignmentPassForProcessingTest(state, 1, 2)
        findAlignmentPassForProcessingTest(state, 2, 1)
        findAlignmentPassForProcessingTest(state, 3, 0)
    }

    private void findAlignmentPassForProcessingTest(
            final AlignmentState state,
            final int nonWithdrawnDataFiles,
            final int withdrawnDataFiles) {
        AlignmentPass alignmentPass = createAndSaveAlignmentPass(
                seqTrack: seqTrack,
                alignmentState: state,
        )
        Collection<DataFile> dataFiles = new ArrayList<DataFile>()
        for (int i = 0; i < nonWithdrawnDataFiles; i++) {
            dataFiles.add createDataFile(false)
        }
        for (int i = 0; i < withdrawnDataFiles; i++) {
            dataFiles.add createDataFile(true)
        }
        assertEquals(nonWithdrawnDataFiles + withdrawnDataFiles, DataFile.count)
        if (state == AlignmentState.NOT_STARTED &&
        nonWithdrawnDataFiles >= 1 && withdrawnDataFiles == 0) {
            assertEquals(alignmentPass, alignmentPassService.findAlignmentPassForProcessing())
        } else {
            assertNull(alignmentPassService.findAlignmentPassForProcessing())
        }
        dataFiles*.delete(flush: true)
    }

    DataFile createDataFile(boolean withdrawn) {
        DataFile dataFile = createDataFile()
        dataFile.fileWithdrawn = withdrawn
        assertNotNull(dataFile.save(flush: true))
        return dataFile
    }

    @Test
    void testIsLibraryPreparationKitOrBedFileMissing() {
        createObjects()
        assertFalse(alignmentPassService.isLibraryPreparationKitOrBedFileMissing(seqTrack))

        ExomeSeqTrack exomeSeqTrack = createExomeSeqTrack(run)
        assertTrue(alignmentPassService.isLibraryPreparationKitOrBedFileMissing(exomeSeqTrack))

        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)
        assertTrue(alignmentPassService.isLibraryPreparationKitOrBedFileMissing(exomeSeqTrack))

        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit("libraryPreparationKit")
        addKitToExomeSeqTrack(exomeSeqTrack, libraryPreparationKit)
        assertTrue(alignmentPassService.isLibraryPreparationKitOrBedFileMissing(exomeSeqTrack))

        createBedFile(referenceGenome, libraryPreparationKit)
        assertFalse(alignmentPassService.isLibraryPreparationKitOrBedFileMissing(exomeSeqTrack))
    }

    private AlignmentPass deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile() {
        createObjects()
        dataFile.delete()
        seqTrack.delete()
        ExomeSeqTrack exomeSeqTrack = createExomeSeqTrack(run)
        AlignmentPass alignmentPass = createAndSaveAlignmentPass(
                seqTrack: exomeSeqTrack,
                alignmentState: AlignmentState.NOT_STARTED,
        )
        exomeSeqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        dataFile = createDataFile([seqTrack: exomeSeqTrack])
        dataFile.save(flush: true)
        return alignmentPass
    }

    private ProcessedBamFile createProcessedBamFile(SeqTrack seqTrack, int identifier) {
        AlignmentPass alignmentPass = createAlignmentPass(
                identifier: identifier,
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
}
