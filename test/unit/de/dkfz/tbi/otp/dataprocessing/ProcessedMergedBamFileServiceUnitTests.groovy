package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.junit.*

@TestFor(ProcessedMergedBamFileService)
@Build([
    MergingCriteria,
    MergingPass,
    MergingSet,
    ProcessedBamFile,
    Realm,
])
class ProcessedMergedBamFileServiceUnitTests {

    private final static String SOME_MD5SUM_VALUE = "12345678901234567890123456789012"

    private final static long SOME_FILE_LENGTH = 10  //Content should only be positive

    @Before
    void setup() {
        new TestConfigService()
    }

    @After
    void tearDown() {
        ConfigService.metaClass = null
        MergedAlignmentDataFileService.metaClass = null
        MergingPass.metaClass = null
        MergingSet.metaClass = null
    }

    @Test
    void testLibraryPreparationKitCorrect() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        assertEquals(input.kit, service.libraryPreparationKit(mergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testLibraryPreparationKitNullInput() {
        service.libraryPreparationKit(null)
    }

    @Test(expected = IllegalArgumentException)
    void testLibraryPreparationKitNotExomSeqType() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.WHOLE_GENOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.libraryPreparationKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testLibraryPreparationKitNoSingleLaneBamFiles() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { []}
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.libraryPreparationKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testLibraryPreparationKitDiffSeqTrackTypes() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        SeqTrack seqTrack = new SeqTrack()
        seqTrack.sample = input.sample
        seqTrack.laneId = "1"
        seqTrack.run = input.run
        seqTrack.seqType = input.seqType
        input.bamFiles[2].alignmentPass.seqTrack = seqTrack
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.libraryPreparationKit(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testLibraryPreparationKitDiffKits() {
        ProcessedMergedBamFile mergedBamFile = new ProcessedMergedBamFile()
        LibraryPreparationKit kit = new LibraryPreparationKit(name: 'kit2')
        Map input = createKitAndSingleLaneBamFiles(SeqTypeNames.EXOME.seqTypeName, ExomeSeqTrack, mergedBamFile)
        input.bamFiles[2].alignmentPass.seqTrack.libraryPreparationKit = kit
        def abstractBamFileService = [
            findAllByProcessedMergedBamFile: { input.bamFiles }
        ] as AbstractBamFileService
        service.abstractBamFileService = abstractBamFileService
        service.libraryPreparationKit(mergedBamFile)
    }

    private Map createKitAndSingleLaneBamFiles(String seqTypeName, Class seqTypeClass, ProcessedMergedBamFile processegMergedBamFile) {
        assert seqTypeClass == SeqTrack || seqTypeClass == ExomeSeqTrack
        List bamFiles = []
        SeqType seqType = DomainFactory.createSeqType(
                name: seqTypeName,
                dirName: "tmp",
        )
        Sample sample = createSampleAndDBConnections()
        mergingPassAndDBConnections(processegMergedBamFile, sample, seqType)
        LibraryPreparationKit kit = new LibraryPreparationKit(name: 'kit1')
        Run run = new Run(name: 'run')
        3.times {
            SeqTrack seqTrack
            if (seqTypeClass == SeqTrack) {
                seqTrack = new SeqTrack()
            }
            if (seqTypeClass == ExomeSeqTrack) {
                seqTrack = new ExomeSeqTrack(libraryPreparationKit: kit)
            }
            seqTrack.laneId = "1"
            seqTrack.run = run
            seqTrack.seqType = seqType
            seqTrack.sample = sample
            AlignmentPass pass = new AlignmentPass(seqTrack: seqTrack)
            ProcessedBamFile bamFile = new ProcessedBamFile(alignmentPass: pass)
            bamFiles << bamFile
        }
        ProcessedMergedBamFileService.metaClass.seqType = { ProcessedMergedBamFile bamFile -> seqType }
        return [kit:kit, bamFiles:bamFiles, run: run, sample: sample, seqType: seqType]
    }


    private Sample createSampleAndDBConnections() {
        return DomainFactory.createSample(
                individual: DomainFactory.createIndividual(
                                project: DomainFactory.createProject(),
                            ),
                sampleType: DomainFactory.createSampleType(),
        )
    }


    private void mergingPassAndDBConnections(ProcessedMergedBamFile processedMergedBamFile, Sample sample, SeqType seqType) {
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(
                sample: sample,
                seqType: seqType,
                libraryPreparationKit: DomainFactory.createLibraryPreparationKit(name: 'libraryPreparationKit'),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        )
        MergingSet mergingSet = DomainFactory.createMergingSet(
                mergingWorkPackage: mergingWorkPackage,
        )
        MergingPass mergingPass = DomainFactory.createMergingPass(
                mergingSet: mergingSet,
        )

        processedMergedBamFile.mergingPass = mergingPass
        processedMergedBamFile.fileExists = true
        processedMergedBamFile.dateFromFileSystem = new Date()
    }



    private def createDataForDeleteChecking(Boolean valueForDataBaseConsistence = null, Boolean valueForDestinationConsistence = null) {
        final String dataProcessingTempDir = TestCase.getUniqueNonExistentPath() as String
        final File projectRootDir = TestCase.getUniqueNonExistentPath()

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile([
            md5sum: SOME_MD5SUM_VALUE,
            fileOperationStatus: FileOperationStatus.PROCESSED,
            fileSize: 10000
        ])

        ProcessedMergedBamFileService processedMergedBamFileService = new ProcessedMergedBamFileService()

        ConfigService.metaClass.static.getProjectRootPath = { Project project -> return projectRootDir }

        MergedAlignmentDataFileService.metaClass.static.buildRelativePath = { SeqType type, Sample sample -> return "RelativeDirectory"}


        processedMergedBamFileService.checksumFileService = [:] as ChecksumFileService


        processedMergedBamFileService.dataProcessingFilesService = [
            getOutputDirectory: { Individual individual, DataProcessingFilesService.OutputDirectories dir ->
                return dataProcessingTempDir
            },

            checkConsistencyWithDatabaseForDeletion: { final def dbFile, final File fsFile ->
                if (valueForDataBaseConsistence == null) {
                    fail "checkConsistencyWithDatabaseForDeletion was called when it shouldn't be. Method under test should have failed earlier."
                } else {
                    File directory = processedMergedBamFileService.processingDirectory(processedMergedBamFile.mergingPass) as File
                    File fileName = new File(directory, processedMergedBamFile.getBamFileName())
                    assert processedMergedBamFile == dbFile
                    assert fileName == fsFile
                    return valueForDataBaseConsistence.value
                }
            },

            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                File expectedProcessingDirectory = processedMergedBamFileService.processingDirectory(processedMergedBamFile.mergingPass) as File
                File expectedFinalDestinationDirectory = AbstractMergedBamFileService.destinationDirectory(processedMergedBamFile) as File
                Collection<String> expectedAdditionalFiles = processedMergedBamFileService.additionalFileNames(processedMergedBamFile)
                expectedAdditionalFiles << processedMergedBamFile.getBamFileName()
                assert expectedProcessingDirectory == processingDirectory
                assert expectedFinalDestinationDirectory == finalDestinationDirectory
                assert expectedAdditionalFiles == fileNames
                if (valueForDestinationConsistence == null) {
                    fail "checkConsistencyWithFinalDestinationForDeletion was called when it shouldn't be. Method under test should have failed earlier."
                } else {
                    return valueForDestinationConsistence.value
                }
            },

            deleteProcessingFiles: { final def dbFile, final File fsFile, final File... additionalFiles ->
                File directory = processedMergedBamFileService.processingDirectory(processedMergedBamFile.mergingPass) as File
                File expectedFile = new File(directory, processedMergedBamFile.getBamFileName())
                File[] expectedAdditionalFiles = [
                    processedMergedBamFileService.additionalFileNames(processedMergedBamFile),
                    processedMergedBamFileService.additionalFileNamesProcessingDirOnly(processedMergedBamFile)
                ].flatten().collect {
                    new File(directory, it)
                } as File[]
                assert processedMergedBamFile == dbFile
                assert expectedFile == fsFile
                assert expectedAdditionalFiles == additionalFiles
                return SOME_FILE_LENGTH
            },
        ] as DataProcessingFilesService


        return [
            processedMergedBamFile,
            processedMergedBamFileService
        ]
    }


    @Test
    void testCheckConsistencyForProcessingFilesDeletion() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, true)

        assert processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_NoProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_DataBaseNotConsistent() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(false)

        assert !processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_NotLatestMergingPass() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true)
        MergingPass.metaClass.isLatestPass= {false}

        assert processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_NotLatestSet() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true)
        MergingSet.metaClass.isLatestSet= {false}

        assert processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_md5SumIsNull() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true)
        processedMergedBamFile.md5sum = null
        processedMergedBamFile.fileOperationStatus = FileOperationStatus.DECLARED
        CheckedLogger checkedLogger = new CheckedLogger()
        checkedLogger.addError("ProcessedMergedBamFile ${processedMergedBamFile} does not have its md5sum set, although it belongs to the latest MergingPass of the latest MergingSet for its MergingWorkpackage.")
        try {
            LogThreadLocal.setThreadLog(checkedLogger)

            assert !processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)

            checkedLogger.assertAllMessagesConsumed()
        } finally {
            LogThreadLocal.removeThreadLog()
        }
    }

    @Test
    void testCheckConsistencyForProcessingFilesDeletion_FinalDestinationNotConsistent() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, false)

        assert !processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }


    @Test
    void testDeleteProcessingFiles() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, true)

        assert SOME_FILE_LENGTH == processedMergedBamFileService.deleteProcessingFiles(processedMergedBamFile)
    }

    @Test
    void testDeleteProcessingFiles_NoProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, true)

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedMergedBamFileService.deleteProcessingFiles(null) //
        }
    }

    @Test
    void testDeleteProcessingFiles_ConsistentCheckFail() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(false)

        assert 0 == processedMergedBamFileService.deleteProcessingFiles(processedMergedBamFile)
    }

}
