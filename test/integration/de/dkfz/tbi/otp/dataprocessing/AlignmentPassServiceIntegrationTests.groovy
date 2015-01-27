package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

class AlignmentPassServiceIntegrationTests extends TestData {

    AlignmentPassService alignmentPassService

    /* SeqTrackServiceTests.testSetReadyForAlignment() relies on the criteria in
     * AlignmentPassService.ALIGNABLE_SEQTRACK_HQL being deeply tested here.
     */
    @Test
    void testFindAlignableSeqTrack() {
        createObjects()
        dataFile.delete()
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED

        findAlignableSeqTrackTest(SeqTrack.DataProcessingState.IN_PROGRESS)
        findAlignableSeqTrackTest(SeqTrack.DataProcessingState.NOT_STARTED)

        seqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        DataFile dataFile = createDataFile(false)
        assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())

        dataFile.fileExists = false
        assertNotNull(dataFile.save(flush: true))
        assertNull(alignmentPassService.findAlignableSeqTrack())
        dataFile.fileExists = true
        assertNotNull(dataFile.save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())

        dataFile.fileSize = 0
        assertNotNull(dataFile.save(flush: true))
        assertNull(alignmentPassService.findAlignableSeqTrack())
        dataFile.fileSize = 1
        assertNotNull(dataFile.save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())

        fileType.type = FileType.Type.ALIGNMENT
        assertNotNull(fileType.save(flush: true))
        assertNull(alignmentPassService.findAlignableSeqTrack())
        fileType.type = FileType.Type.SEQUENCE
        assertNotNull(fileType.save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())

        RunSegment runSegment = new RunSegment()
        runSegment.run = run
        runSegment.metaDataStatus = RunSegment.Status.PROCESSING
        runSegment.filesStatus = RunSegment.FilesStatus.FILES_MISSING
        runSegment.initialFormat = RunSegment.DataFormat.TAR
        runSegment.currentFormat = RunSegment.DataFormat.TAR
        runSegment.dataPath = ''
        runSegment.mdPath = ''
        assertNotNull(runSegment.save(flush: true))
        assertNull(alignmentPassService.findAlignableSeqTrack())
        runSegment.metaDataStatus = RunSegment.Status.COMPLETE
        assertNotNull(runSegment.save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())

        referenceGenomeProjectSeqType.delete(flush: true)
        assert alignmentPassService.findAlignableSeqTrack() == seqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, seqTrack.alignmentState)

        seqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        assertNotNull(createReferenceGenomeProjectSeqType().save(flush: true))
        assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())
    }

    @Test
    void testFindAlignableSeqTrack_fastqcMustBeFinished() {
        createObjects()
        createDataFile(false)
        seqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED

        seqTrack.fastqcState = SeqTrack.DataProcessingState.IN_PROGRESS
        assertNull(alignmentPassService.findAlignableSeqTrack())

        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())
    }

    @Test
    void testFindAlignableExomeSeqTrackKitANDBedFileANDReferenceGenomeConnectionMissing() {
        createObjects()
        dataFile.delete()
        seqTrack.delete()
        ExomeSeqTrack exomeSeqTrack = createExomeSeqTrack(run)
        exomeSeqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        exomeSeqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED

        assertNull(alignmentPassService.findAlignableSeqTrack())

        dataFile = createDataFile([seqTrack: exomeSeqTrack])
        dataFile.save(flush: true)
        assert alignmentPassService.findAlignableSeqTrack() == exomeSeqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignableExomeSeqTrackKitANDBedFileMissing() {
        ExomeSeqTrack exomeSeqTrack = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()

        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)
        assert alignmentPassService.findAlignableSeqTrack() == exomeSeqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignableExomeSeqTrackKitConnectionANDBedFileMissing() {
        ExomeSeqTrack exomeSeqTrack = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()

        ExomeEnrichmentKit exomeEnrichmentKit = createEnrichmentKit("exomeEnrichmentKit")
        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)

        assert alignmentPassService.findAlignableSeqTrack() == exomeSeqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignableExomeSeqTrackBedFileMissing() {
        ExomeSeqTrack exomeSeqTrack = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()

        ExomeEnrichmentKit exomeEnrichmentKit = createEnrichmentKit("exomeEnrichmentKit")
        addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit)
        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)

        assert alignmentPassService.findAlignableSeqTrack() == exomeSeqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignableExomeSeqTrackReferenceGenomeAndBedFileMissing() {
        ExomeSeqTrack exomeSeqTrack = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()

        ExomeEnrichmentKit exomeEnrichmentKit = createEnrichmentKit("exomeEnrichmentKit")
        addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit)

        assert alignmentPassService.findAlignableSeqTrack() == exomeSeqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignableExomeSeqTrackReferenceGenomeAndKitMissing() {
        ExomeSeqTrack exomeSeqTrack = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()

        ExomeEnrichmentKit exomeEnrichmentKit = createEnrichmentKit("exomeEnrichmentKit")
        createBedFile(referenceGenome, exomeEnrichmentKit)

        assert alignmentPassService.findAlignableSeqTrack() == exomeSeqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignableExomeSeqTrackReferenceGenomeMissing() {
        ExomeSeqTrack exomeSeqTrack = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()

        ExomeEnrichmentKit exomeEnrichmentKit = createEnrichmentKit("exomeEnrichmentKit")
        createBedFile(referenceGenome, exomeEnrichmentKit)
        addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit)

        assert alignmentPassService.findAlignableSeqTrack() == exomeSeqTrack
        assertEquals(SeqTrack.DataProcessingState.NOT_STARTED, exomeSeqTrack.alignmentState)
    }


    @Test
    void testFindAlignableExomeSeqTrackAllInformationAvailable() {
        ExomeSeqTrack exomeSeqTrack = deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile()

        ExomeEnrichmentKit exomeEnrichmentKit = createEnrichmentKit("exomeEnrichmentKit")
        createBedFile(referenceGenome, exomeEnrichmentKit)
        addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit)
        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)

        assertEquals(exomeSeqTrack, alignmentPassService.findAlignableSeqTrack())
    }

    private void findAlignableSeqTrackTest(final SeqTrack.DataProcessingState state) {
        findAlignableSeqTrackTest(state, 0, 0)
        findAlignableSeqTrackTest(state, 1, 0)
        findAlignableSeqTrackTest(state, 0, 1)
        findAlignableSeqTrackTest(state, 0, 2)
        findAlignableSeqTrackTest(state, 1, 1)
        findAlignableSeqTrackTest(state, 2, 0)
        findAlignableSeqTrackTest(state, 0, 3)
        findAlignableSeqTrackTest(state, 1, 2)
        findAlignableSeqTrackTest(state, 2, 1)
        findAlignableSeqTrackTest(state, 3, 0)
    }

    private void findAlignableSeqTrackTest(
                    final SeqTrack.DataProcessingState state,
                    final int nonWithdrawnDataFiles,
                    final int withdrawnDataFiles) {
        seqTrack.alignmentState = state
        Collection<DataFile> dataFiles = new ArrayList<DataFile>()
        for (int i = 0; i < nonWithdrawnDataFiles; i++) {
            dataFiles.add createDataFile(false)
        }
        for (int i = 0; i < withdrawnDataFiles; i++) {
            dataFiles.add createDataFile(true)
        }
        assertEquals(nonWithdrawnDataFiles + withdrawnDataFiles, DataFile.count)
        if (state == SeqTrack.DataProcessingState.NOT_STARTED &&
        nonWithdrawnDataFiles >= 1 && withdrawnDataFiles == 0) {
            assertEquals(seqTrack, alignmentPassService.findAlignableSeqTrack())
        } else {
            assertNull(alignmentPassService.findAlignableSeqTrack())
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
    void testCreateAlignmentPass() {
        createObjects()
        assertNull(alignmentPassService.createAlignmentPass())
        seqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        DataFile dataFile1 = createDataFile(false)
        AlignmentPass pass1 = alignmentPassService.createAlignmentPass()
        assertNotNull(pass1)
        assertEquals(seqTrack, pass1.seqTrack)
        AlignmentPass pass2 = alignmentPassService.createAlignmentPass()
        assertNotNull(pass2)
        assertEquals(seqTrack, pass2.seqTrack)
        assertNotSame(pass1.identifier, pass2.identifier)
        dataFile1.fileWithdrawn = true
        assertNotNull(dataFile1.save(flush: true))
        assertNull(alignmentPassService.createAlignmentPass())
    }

    @Test
    void testMaximalIdentifier() {
        createObjects()

        ProcessedBamFile processedBamFile = createProcessedBamFile(seqTrack, 0)
        assertEquals(0, alignmentPassService.maximalIdentifier(processedBamFile))

        ProcessedBamFile processedBamFile1 = createProcessedBamFile(seqTrack, 1)
        assertEquals(1, alignmentPassService.maximalIdentifier(processedBamFile))
        assertEquals(1, alignmentPassService.maximalIdentifier(processedBamFile1))

        ProcessedBamFile processedBamFile2 = createProcessedBamFile(seqTrack, 2)
        assertEquals(2, alignmentPassService.maximalIdentifier(processedBamFile))
        assertEquals(2, alignmentPassService.maximalIdentifier(processedBamFile1))
        assertEquals(2, alignmentPassService.maximalIdentifier(processedBamFile2))
    }


    @Test
    void testIsExomeEnrichmentKitOrBedFileMissing() {
        createObjects()
        assertFalse(alignmentPassService.isExomeEnrichmentKitOrBedFileMissing(seqTrack))

        ExomeSeqTrack exomeSeqTrack = createExomeSeqTrack(run)
        assertTrue(alignmentPassService.isExomeEnrichmentKitOrBedFileMissing(exomeSeqTrack))

        createReferenceGenomeProjectSeqType([seqType: exomeSeqType]).save(flush: true)
        assertTrue(alignmentPassService.isExomeEnrichmentKitOrBedFileMissing(exomeSeqTrack))

        ExomeEnrichmentKit exomeEnrichmentKit = createEnrichmentKit("exomeEnrichmentKit")
        addKitToExomeSeqTrack(exomeSeqTrack, exomeEnrichmentKit)
        assertTrue(alignmentPassService.isExomeEnrichmentKitOrBedFileMissing(exomeSeqTrack))

        createBedFile(referenceGenome, exomeEnrichmentKit)
        assertFalse(alignmentPassService.isExomeEnrichmentKitOrBedFileMissing(exomeSeqTrack))
    }


    @Test
    void testUpdateSeqTrackDataProcessingState() {
        createObjects()

        assertEquals(SeqTrack.DataProcessingState.UNKNOWN, seqTrack.alignmentState)

        alignmentPassService.updateSeqTrackDataProcessingState(seqTrack, SeqTrack.DataProcessingState.IN_PROGRESS)
        assertEquals(SeqTrack.DataProcessingState.IN_PROGRESS, seqTrack.alignmentState)
    }


    private ExomeSeqTrack deleteNonExomeSeqTrackAndPrepareExomeSeqTrackAndDataFile() {
        createObjects()
        dataFile.delete()
        seqTrack.delete()
        ExomeSeqTrack exomeSeqTrack = createExomeSeqTrack(run)
        exomeSeqTrack.alignmentState = SeqTrack.DataProcessingState.NOT_STARTED
        exomeSeqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        dataFile = createDataFile([seqTrack: exomeSeqTrack])
        dataFile.save(flush: true)
        return exomeSeqTrack
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
