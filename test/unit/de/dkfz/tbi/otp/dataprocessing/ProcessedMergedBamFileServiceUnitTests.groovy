package de.dkfz.tbi.otp.dataprocessing

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CheckedLogger
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal



@TestFor(ProcessedMergedBamFileService)
@Build([
    ProcessedMergedBamFile,
    Realm,
])
class ProcessedMergedBamFileServiceUnitTests {

    private final static String SOME_MD5SUM_VALUE = "12345678901234567890123456789012"

    private final static long SOME_FILE_LENGTH = 10  //Content should only be positive

    TestData testData = new TestData()

    @After
    void tearDown() {
        ConfigService.metaClass = null
        MergedAlignmentDataFileService.metaClass = null
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
        SeqType seqType = testData.createSeqType([name: seqTypeName, dirName: "/tmp"])
        assert seqType.save()
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
        Project project = testData.createProject()
        assert project.save()
        Individual individual = testData.createIndividual([project: project])
        assert individual.save()
        SampleType sampleType = testData.createSampleType()
        assert sampleType.save()
        Sample sample = testData.createSample([individual: individual, sampleType: sampleType])
        assert sample.save()
        return sample
    }


    private void mergingPassAndDBConnections(ProcessedMergedBamFile processedMergedBamFile, Sample sample, SeqType seqType) {
        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage([sample: sample, seqType: seqType])
        assert mergingWorkPackage.save()
        MergingSet mergingSet = testData.createMergingSet([mergingWorkPackage: mergingWorkPackage])
        assert mergingSet.save()
        MergingPass mergingPass = testData.createMergingPass([mergingSet: mergingSet])
        assert mergingPass.save()

        processedMergedBamFile.mergingPass = mergingPass
        processedMergedBamFile.fileExists = true
        processedMergedBamFile.dateFromFileSystem = new Date()
    }



    private def createDataForDeleteChecking(Boolean valueForDataBaseConsistence = null, Boolean valueForDestinationConsistence = null) {
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build([
            md5sum: SOME_MD5SUM_VALUE,
            fileOperationStatus: FileOperationStatus.PROCESSED,
            fileSize: 10000
        ])

        ProcessedMergedBamFileService processedMergedBamFileService = new ProcessedMergedBamFileService()

        ConfigService.metaClass.static.getProjectRootPath = { Project project -> return TestConstants.BASE_TEST_DIRECTORY}

        MergedAlignmentDataFileService.metaClass.static.buildRelativePath = { SeqType type, Sample sample -> return "RelativeDirectory"}


        processedMergedBamFileService.checksumFileService = [:] as ChecksumFileService


        processedMergedBamFileService.dataProcessingFilesService = [
            getOutputDirectory: { Individual individual, DataProcessingFilesService.OutputDirectories dir ->
                return TestConstants.BASE_TEST_DIRECTORY
            },

            checkConsistencyWithDatabaseForDeletion: { final def dbFile, final File fsFile ->
                if (valueForDataBaseConsistence == null) {
                    fail "checkConsistencyWithDatabaseForDeletion was called when it shouldn't be. Method under test should have failed earlier."
                } else {
                    File directory = processedMergedBamFileService.processingDirectory(processedMergedBamFile.mergingPass) as File
                    File fileName = new File(directory, processedMergedBamFileService.fileName(processedMergedBamFile))
                    assert processedMergedBamFile == dbFile
                    assert fileName == fsFile
                    return valueForDataBaseConsistence.value
                }
            },

            checkConsistencyWithFinalDestinationForDeletion: {final File processingDirectory, final File finalDestinationDirectory, final Collection<String> fileNames ->
                File expectedProcessingDirectory = processedMergedBamFileService.processingDirectory(processedMergedBamFile.mergingPass) as File
                File expectedFinalDestinationDirectory = AbstractMergedBamFileService.destinationDirectory(processedMergedBamFile) as File
                Collection<String> expectedAdditionalFiles = processedMergedBamFileService.additionalFileNames(processedMergedBamFile)
                expectedAdditionalFiles << processedMergedBamFileService.fileName(processedMergedBamFile)
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
                File expectedFile = new File(directory, processedMergedBamFileService.fileName(processedMergedBamFile))
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



    public void testCheckConsistencyForProcessingFilesDeletion() {
        Realm.build()
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, true)

        assert processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    public void testCheckConsistencyForProcessingFilesDeletion_NoProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking()

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(null) //
        }
    }

    public void testCheckConsistencyForProcessingFilesDeletion_DataBaseNotConsistent() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(false)

        assert !processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    public void testCheckConsistencyForProcessingFilesDeletion_NotLatestMergingPass() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true)
        MergingPass.metaClass.isLatestPass= {false}

        assert processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    public void testCheckConsistencyForProcessingFilesDeletion_NotLatestSet() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true)
        MergingSet.metaClass.isLatestSet= {false}

        assert processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }

    public void testCheckConsistencyForProcessingFilesDeletion_md5SumIsNull() {
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

    public void testCheckConsistencyForProcessingFilesDeletion_FinalDestinationNotConsistent() {
        Realm.build()
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, false)

        assert !processedMergedBamFileService.checkConsistencyForProcessingFilesDeletion(processedMergedBamFile)
    }



    public void testDeleteProcessingFiles() {
        Realm.build()
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, true)

        assert SOME_FILE_LENGTH == processedMergedBamFileService.deleteProcessingFiles(processedMergedBamFile)
    }


    public void testDeleteProcessingFiles_NoProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(true, true)

        assert TestConstants.ERROR_MESSAGE_SPRING_NOT_NULL == shouldFail (IllegalArgumentException) {
            processedMergedBamFileService.deleteProcessingFiles(null) //
        }
    }

    public void testDeleteProcessingFiles_ConsistentCheckFail() {
        ProcessedMergedBamFile processedMergedBamFile
        ProcessedMergedBamFileService processedMergedBamFileService
        (processedMergedBamFile, processedMergedBamFileService) = createDataForDeleteChecking(false)

        assert 0 == processedMergedBamFileService.deleteProcessingFiles(processedMergedBamFile)
    }

}
