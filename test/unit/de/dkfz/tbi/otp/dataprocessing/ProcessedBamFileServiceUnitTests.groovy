package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Test


@TestFor(ProcessedBamFileService)
@Build([
    ProcessedBamFile,
    ReferenceGenome,
])
class ProcessedBamFileServiceUnitTests {



    public void setUp() throws Exception {
        //if something failed and the toString method is called, the criteria in isLatestPass makes Problems
        //Therefore this method is mocked.
        AlignmentPass.metaClass.isLatestPass= {true}
    }



    @Test
    void testLibraryPreparationKitCorrect() {
        Map input = createKitAndBamFile(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack)
        assertEquals(input.kit, service.libraryPreparationKit(input.bamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testLibraryPreparationKitNullInput() {
        service.libraryPreparationKit(null)
    }

    private Map createKitAndBamFile(String seqTypeName, Class seqTypeClass) {
        assert seqTypeClass == SeqTrack || seqTypeClass == ExomeSeqTrack
        SeqType seqType = new SeqType()
        seqType.name = seqTypeName
        LibraryPreparationKit kit = new LibraryPreparationKit()
        Run run = new Run(name: 'run')
        SeqTrack seqTrack
        if (seqTypeClass == SeqTrack) {
            seqTrack = new SeqTrack()
        }
        if (seqTypeClass == ExomeSeqTrack) {
            seqTrack = new ExomeSeqTrack(libraryPreparationKit: kit)
        }
        seqTrack.run = run
        seqTrack.seqType = seqType
        AlignmentPass pass = new AlignmentPass(seqTrack: seqTrack)
        ProcessedBamFile bamFile = new ProcessedBamFile(alignmentPass: pass)
        return [kit: kit, bamFile: bamFile]
    }

    private ProcessedBamFileService createProcessedBamFileService() {
        ProcessedBamFileService processedBamFileService = new ProcessedBamFileService()
        processedBamFileService.processedAlignmentFileService = [
            getDirectory: { AlignmentPass alignmentPass -> return TestConstants.BASE_TEST_DIRECTORY}
        ] as ProcessedAlignmentFileService
        return processedBamFileService
    }



    public void testCheckConsistencyForProcessingFilesDeletion() {
        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()
        processedBamFileService.dataProcessingFilesService = [
            checkConsistencyWithDatabaseForDeletion: { final def dbFile, final File fsFile ->
                File filePath = processedBamFileService.getFilePath(processedBamFile) as File
                assert processedBamFile == dbFile
                assert filePath == fsFile
                return true
            },
        ] as DataProcessingFilesService

        assert processedBamFileService.checkConsistencyForProcessingFilesDeletion(processedBamFile)
    }

    public void testCheckConsistencyForProcessingFilesDeletion_ProcessedBamFileIsNull() {
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedBamFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }



    public void testDeleteProcessingFiles() {
        final int FILE_LENGTH = 10
        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()
        processedBamFileService.dataProcessingFilesService = [
            deleteProcessingFiles: { final def dbFile, final File fsFile, final File... additionalFiles ->
                File filePath = processedBamFileService.getFilePath(processedBamFile) as File
                File[] expectedAdditionFiles = [
                    processedBamFileService.baiFilePath(processedBamFile) as File,
                    processedBamFileService.bwaSampeErrorLogFilePath(processedBamFile) as File,
                ]
                assert processedBamFile == dbFile
                assert filePath == fsFile
                assert expectedAdditionFiles == additionalFiles
                return FILE_LENGTH
            },
        ] as DataProcessingFilesService

        assert FILE_LENGTH == processedBamFileService.deleteProcessingFiles(processedBamFile)
    }

    public void testDeleteProcessingFiles_ProcessedBamFileIsNull() {
        ProcessedBamFileService processedBamFileService = createProcessedBamFileService()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedBamFileService.deleteProcessingFiles(null) //
        }
    }
}
