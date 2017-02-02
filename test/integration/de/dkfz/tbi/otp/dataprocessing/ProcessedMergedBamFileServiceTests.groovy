package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.validation.ValidationException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

class ProcessedMergedBamFileServiceTests {

    ProcessedMergedBamFileService processedMergedBamFileService
    DataProcessingFilesService dataProcessingFilesService

    File baseDir

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File testDirectory

    String directory
    String baseFile
    String basePath
    String realmName

    @Before
    void setUp() {
        testDirectory = tmpDir.newFolder('otp-test')

        directory = testDirectory.absolutePath + "/processing/project-dir/results_per_pid/patient/merging//sample-type/seq-type/library/DEFAULT/0/pass0"
        baseFile = "sample-type_patient_seq-type_library_merged.mdup"
        basePath = directory + "/" + baseFile
        realmName = "Realm_${HelperUtils.uniqueString}"

        DomainFactory.createRealmDataProcessing(
                name: realmName,
                processingRootPath: testDirectory.absolutePath + '/processing',
        )
        DomainFactory.createRealmDataManagement(
                name: realmName,
                rootPath: testDirectory.absolutePath + '/root',
        )

        baseDir = new File(directory)
        File bam = new File(basePath + ".bam")
        File bai = new File(basePath + ".bai")
        File metrics = new File(basePath + "_metrics.txt")
        assertTrue(baseDir.exists() || baseDir.mkdirs())
        assertTrue(baseDir.setReadable(true))
        [bam, bai, metrics].each { File file ->
            if (file.exists()) {
                assertTrue(file.delete())
            }
            file << "test"
            file.deleteOnExit()
        }
    }

    @After
    void tearDown() {
        assertTrue(baseDir.setReadable(true))
        baseDir.listFiles().each { File file ->
            assertTrue(file.setReadable(true))
            assertTrue(file.delete())
        }
        baseDir = null
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateBamMetricsFileFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamMetricsFileFileIsEmpty() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        File file = new File(processedMergedBamFileService.filePathForMetrics(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamMetricsFileFileNotReadable() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        File file = new File(processedMergedBamFileService.filePathForMetrics(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test
    void testUpdateBamMetricsFile() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertTrue(processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile))
    }


    @Test(expected = IllegalArgumentException)
    void testUpdateBamFileIndexFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileIndexFileIsEmpty() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        File file = new File(processedMergedBamFileService.filePathForBai(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileIndexFileNotReadable() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        File file = new File(processedMergedBamFileService.filePathForBai(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test
    void testUpdateBamFileIndex() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertTrue(processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateBamFileBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileFileNotReadable() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        File file = new File(processedMergedBamFileService.filePath(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileFileIsEmpty() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        File file = new File(processedMergedBamFileService.filePath(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test
    void testUpdateBamFile() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertTrue(processedMergedBamFileService.updateBamFile(processedMergedBamFile))
    }

    @Test
    void testCreateMergedBamFile() {
        MergingPass mergingPass = MergingPass.build()
        DomainFactory.assignNewProcessedBamFile(mergingPass.mergingSet)
        ProcessedMergedBamFile processedMergedBamFile = processedMergedBamFileService.createMergedBamFile(mergingPass)
        assertNotNull(processedMergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testCreateMergedBamFile_mergingPassIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = processedMergedBamFileService.createMergedBamFile(null)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathForBaiBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.filePathForBai(processedMergedBamFile)
    }

    @Test
    void testFilePathForBai() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        String pathExp = basePath + ".bai"
        String pathAct = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathForMetricsBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
    }

    @Test
    void testFilePathForMetrics() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        String pathExp = basePath + "_metrics.txt"
        String pathAct = processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFileNameForMetricsBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        String nameAct = processedMergedBamFileService.fileNameForMetrics(processedMergedBamFile)
    }

    @Test
    void testFileNameForMetrics() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        String nameExp = baseFile + "_metrics.txt"
        String nameAct = processedMergedBamFileService.fileNameForMetrics(processedMergedBamFile)
        assertEquals(nameExp, nameAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.filePath(processedMergedBamFile)
    }

    @Test
    void testFilePath() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        String pathExp = basePath +".bam"
        assertEquals(pathExp, processedMergedBamFileService.filePath(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testDirectoryByProcessedMergedBamFileBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.directory(processedMergedBamFile)
    }

    @Test
    void testDirectoryByProcessedMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        String pathExp = directory
        String pathAct = processedMergedBamFileService.directory(processedMergedBamFile)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testDirectoryByMergingPassMergingPassIsNull() {
        MergingPass mergingPass = null
        processedMergedBamFileService.directory(mergingPass)
    }

    @Test
    void testDirectoryByMergingPassWithoutProcessedMergedBamFile() {
        MergingPass mergingPass = createProcessedMergedBamFile().mergingPass
        String pathExp = directory
        String pathAct = processedMergedBamFileService.directory(mergingPass)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testDirectoryByMergingPass() {
        ProcessedMergedBamFile bamFile = createProcessedMergedBamFile()
        String pathExp = directory
        String pathAct = processedMergedBamFileService.directory(bamFile.mergingPass)
        assertEquals(pathExp, pathAct)
        bamFile.fileOperationStatus = FileOperationStatus.PROCESSED
        bamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        pathExp = testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/"
        pathAct = processedMergedBamFileService.directory(bamFile.mergingPass)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenQualityAssessmentStatusIsFalse() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.qualityAssessmentStatus = QaProcessingStatus.IN_PROGRESS
        mergedBamFile.mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenFileOperationStatusFalse() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.fileOperationStatus = FileOperationStatus.INPROGRESS
        mergedBamFile.mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test(expected = ValidationException)
    void testMergedBamFileWithFinishedQAWhenMd5sumNotNullStateIncorrect() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        mergedBamFile.mergingSet.status = State.PROCESSED
        mergedBamFile.fileSize = 10000
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenMd5sumNotNull() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        mergedBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
        mergedBamFile.mergingSet.status = State.PROCESSED
        mergedBamFile.fileSize = 10000
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenStatusNotProcessed() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenMergingNotFinished() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.mergingSet.status = State.PROCESSED
        mergedBamFile.status = AbstractBamFile.State.NEEDS_PROCESSING
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenMergedBamFileUsedForOngoingMerging() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.mergingSet.status = State.PROCESSED
        MergingSet mergingSet1 = new MergingSet(
                        identifier: MergingSet.nextIdentifier(mergedBamFile.mergingWorkPackage),
                        mergingWorkPackage: mergedBamFile.mergingWorkPackage,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet1.save([flush: true]))
        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet1,
                        bamFile: mergedBamFile
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true]))
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenAllCorrect() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.mergingSet.status = State.PROCESSED
        mergedBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        mergedBamFile.fileSize = 10000
        MergingSet mergingSet1 = new MergingSet(
                identifier: 1,
                mergingWorkPackage: mergedBamFile.mergingWorkPackage,
                status: State.PROCESSED
        )
        assertNotNull(mergingSet1.save([flush: true]))
        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                mergingSet: mergingSet1,
                bamFile: mergedBamFile
        )
        assertNotNull(mergingSetAssignment1.save([flush: true]))
        ProcessedMergedBamFile mergedBamFile1 = DomainFactory.createProcessedMergedBamFile(mergingSet1, [
                numberOfMergedLanes: 2,
                status: AbstractBamFile.State.PROCESSED,
                qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                fileOperationStatus: FileOperationStatus.NEEDS_PROCESSING,
        ])


        ProcessedMergedBamFile processedMergedBamFileExp = mergedBamFile1
        ProcessedMergedBamFile processedMergedBamFileAct = processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY)
        assertEquals(processedMergedBamFileExp, processedMergedBamFileAct)
    }

    @Test
    void testMergedBamFileWithFinishedQA_FastTrackFirst() {
        ProcessedMergedBamFile processedMergedBamFileNormalPriority = setupForSuccessfulMergedBamFileWithFinishedQa()

        ProcessedMergedBamFile processedMergedBamFileFastTrackPriority = setupForSuccessfulMergedBamFileWithFinishedQa()

        processedMergedBamFileFastTrackPriority.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert processedMergedBamFileFastTrackPriority.project.save(flush: true)

        ProcessedMergedBamFile processedMergedBamFileAct = processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL_PRIORITY)
        assertEquals(processedMergedBamFileFastTrackPriority, processedMergedBamFileAct)
    }


    @Test
    void testMergedBamFileWithFinishedQA_JobsReservedForFastTrack() {
        ProcessedMergedBamFile processedMergedBamFileFastTrackPriority = setupForSuccessfulMergedBamFileWithFinishedQa()
        processedMergedBamFileFastTrackPriority.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert processedMergedBamFileFastTrackPriority.project.save(flush: true)

        ProcessedMergedBamFile processedMergedBamFileAct = processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.FAST_TRACK_PRIORITY)
        assertEquals(processedMergedBamFileFastTrackPriority, processedMergedBamFileAct)
    }

    @Test
    void testProject() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        Project projectExp = mergedBamFile.project
        Project projectAct = processedMergedBamFileService.project(mergedBamFile)
        assertEquals(projectExp, projectAct)
    }

    @Test
    void testStoreMD5Digest() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        assertNull(mergedBamFile.md5sum)
        processedMergedBamFileService.storeMD5Digest(mergedBamFile, "68b329da9893e34099c7d8ad5cb9c940")
        String md5Exp = "68b329da9893e34099c7d8ad5cb9c940"
        String md5Act = mergedBamFile.md5sum
        assertEquals(md5Exp, md5Act)
    }

    @Test
    void testLocationsForFileCopying() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(mergedBamFile)
        DataProcessingFilesService.OutputDirectories dirType = DataProcessingFilesService.OutputDirectories.MERGING
        String sourceDirectoryExp = dataProcessingFilesService.getOutputDirectory(mergedBamFile.individual, dirType) + "/sample-type/seq-type/library/DEFAULT/0/pass0"
        assertEquals(sourceDirectoryExp, locations["sourceDirectory"])
        String destinationDirectoryExp =  testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/"
        assertEquals(destinationDirectoryExp, locations["destinationDirectory"])
        String bamFileExp = "sample-type_patient_seq-type_library_merged.mdup.bam"
        assertEquals(bamFileExp, locations["bamFile"])
        String baiFileExp = "sample-type_patient_seq-type_library_merged.mdup.bai"
        assertEquals(baiFileExp, locations["baiFile"])
        String md5BamFileExp = bamFileExp + ".md5sum"
        assertEquals(md5BamFileExp, locations["md5BamFile"])
        String md5BaiFileExp = baiFileExp + ".md5sum"
        assertEquals(md5BaiFileExp, locations["md5BaiFile"])
    }


    @Test(expected = IllegalArgumentException)
    void testDestinationTempDirectoryBamFileNull() {
        processedMergedBamFileService.destinationTempDirectory(null)
    }

    @Test
    void testDestinationTempDirectory() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        String destinationExp =  testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/.tmp"
        String destinationAct = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test(expected = IllegalArgumentException)
    void testQaResultTempDestinationDirectoryBamFileNull() {
        processedMergedBamFileService.qaResultTempDestinationDirectory(null)
    }

    @Test
    void testQaResultTempDestinationDirectory() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        String destinationExp =  testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/.tmp/QualityAssessment"
        String destinationAct = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test(expected = IllegalArgumentException)
    void testSampleFileIsNull() {
        processedMergedBamFileService.sample(null)
    }

    @Test
    void testSample() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        Sample sampleExp = mergedBamFile.sample
        Sample sampleAct = processedMergedBamFileService.sample(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testFastqFilesPerMergedBamFileInputNull() {
        processedMergedBamFileService.fastqFilesPerMergedBamFile(null)
    }

    @Test
    void testFastqFilesPerMergedBamFile() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        DataFile dataFile = DataFile.findBySeqTrack(processedMergedBamFile.containedSeqTracks.iterator().next())
        assertEquals([dataFile], processedMergedBamFileService.fastqFilesPerMergedBamFile(processedMergedBamFile))
    }

    @Test
    void testFastqFilesPerMergedBamFileNoFastqFiles() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        DataFile dataFile = DataFile.findBySeqTrack(processedMergedBamFile.containedSeqTracks.iterator().next())
        dataFile.fileType.type = FileType.Type.ALIGNMENT
        assert processedMergedBamFileService.fastqFilesPerMergedBamFile(processedMergedBamFile).isEmpty()
    }


    @Test(expected = IllegalArgumentException)
    void testGetInferredKitBamFileIsNull() {
        processedMergedBamFileService.getInferredKit(null)
    }


    @Test
    void testGetInferredKitBamFileIsNoExome() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        assertNull(processedMergedBamFileService.getInferredKit(processedMergedBamFile))
    }


    @Test
    void testGetInferredKitBamFileIsExomeButHasTwoDifferentKits() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build(seqType: SeqType.build(name: SeqTypeNames.EXOME.seqTypeName), libraryPreparationKit: libraryPreparationKit)
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage)
        DomainFactory.assignNewProcessedBamFile(processedMergedBamFile)
        List<SeqTrack> seqtracks = processedMergedBamFile.mergingSet.containedSeqTracks as List
        seqtracks[0].libraryPreparationKit = libraryPreparationKit
        seqtracks[0].kitInfoReliability = InformationReliability.KNOWN
        seqtracks[1].libraryPreparationKit = LibraryPreparationKit.build()
        seqtracks[1].kitInfoReliability = InformationReliability.KNOWN
        TestCase.shouldFail(ProcessingException) {
            processedMergedBamFileService.getInferredKit(processedMergedBamFile)
        }
    }


    @Test
    void testGetInferredKitBamFileIsExomeButAllHaveKits() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build(seqType: SeqType.build(name: SeqTypeNames.EXOME.seqTypeName), libraryPreparationKit: libraryPreparationKit)
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage)
        DomainFactory.assignNewProcessedBamFile(processedMergedBamFile)
        List<SeqTrack> seqtracks = processedMergedBamFile.mergingSet.containedSeqTracks as List
        seqtracks[0].libraryPreparationKit = libraryPreparationKit
        seqtracks[1].libraryPreparationKit = libraryPreparationKit
        seqtracks[0].kitInfoReliability = InformationReliability.KNOWN
        seqtracks[1].kitInfoReliability = InformationReliability.KNOWN
        assertNull(processedMergedBamFileService.getInferredKit(processedMergedBamFile))
    }


    @Test
    void testGetInferredKitBamFileIsExomeAndOneKitInferred() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build(seqType: SeqType.build(name: SeqTypeNames.EXOME.seqTypeName), libraryPreparationKit: libraryPreparationKit)
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage)
        DomainFactory.assignNewProcessedBamFile(processedMergedBamFile)
        List<SeqTrack> seqtracks = processedMergedBamFile.mergingSet.containedSeqTracks as List
        seqtracks[0].libraryPreparationKit = libraryPreparationKit
        seqtracks[1].libraryPreparationKit = libraryPreparationKit
        seqtracks[0].kitInfoReliability = InformationReliability.KNOWN
        seqtracks[1].kitInfoReliability = InformationReliability.INFERRED
        assertEquals(libraryPreparationKit, processedMergedBamFileService.getInferredKit(processedMergedBamFile))
    }


    private ProcessedMergedBamFile createProcessedMergedBamFile() {
        ProcessedMergedBamFile mergedBamFile = DomainFactory.createProcessedMergedBamFile()
        mergedBamFile.project.realmName = realmName
        mergedBamFile.project.dirName = 'project-dir'
        mergedBamFile.individual.pid = 'patient'
        mergedBamFile.sampleType.name = 'sample-type'
        mergedBamFile.seqType.name = 'seq-type'
        mergedBamFile.seqType.dirName = 'seq-type-dir'
        mergedBamFile.seqType.libraryLayout = 'library'
        mergedBamFile.md5sum = null
        mergedBamFile.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
        mergedBamFile.fileSize = 10000
        return mergedBamFile
    }

    private ProcessedMergedBamFile setupForSuccessfulMergedBamFileWithFinishedQa() {
        return DomainFactory.createProcessedMergedBamFile(
                DomainFactory.createMergingSet(status: MergingSet.State.PROCESSED), [
                qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                fileOperationStatus: FileOperationStatus.NEEDS_PROCESSING,
                status: AbstractBamFile.State.DECLARED
        ])
    }
}
