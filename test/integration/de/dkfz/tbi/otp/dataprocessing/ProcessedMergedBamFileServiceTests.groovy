package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.validation.*
import org.junit.*
import org.junit.rules.*

import static org.junit.Assert.*

class ProcessedMergedBamFileServiceTests {

    ProcessedMergedBamFileService processedMergedBamFileService
    DataProcessingFilesService dataProcessingFilesService

    File baseDir

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    File testDirectory
    TestConfigService configService

    String directory
    String baseFile
    String basePath
    Realm realm

    @Before
    void setUp() {
        testDirectory = tmpDir.newFolder('otp-test')

        directory = testDirectory.absolutePath + "/processing/project-dir/results_per_pid/patient/merging//sample-type/seq-type/${LibraryLayout.PAIRED.name().toLowerCase()}/DEFAULT/0/pass0"
        baseFile = "sample-type_patient_seq-type_${LibraryLayout.PAIRED}_merged.mdup"
        basePath = directory + "/" + baseFile

        realm =DomainFactory.createRealm()

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : testDirectory.absolutePath + '/root',
                (OtpProperty.PATH_PROCESSING_ROOT): testDirectory.absolutePath + '/processing',
        ])

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
        configService.clean()
    }


    @Test(expected = AssertionError)
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

    @Test(expected = AssertionError)
    void testFileNameForMetricsBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.fileNameForMetrics(processedMergedBamFile)
    }

    @Test
    void testFileNameForMetrics() {
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile()
        String nameExp = baseFile + "_metrics.txt"
        String nameAct = processedMergedBamFileService.fileNameForMetrics(processedMergedBamFile)
        assertEquals(nameExp, nameAct)
    }

    @Test(expected = AssertionError)
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

    @Test(expected = AssertionError)
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

    @Test(expected = AssertionError)
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
        pathExp = testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/${LibraryLayout.PAIRED.name().toLowerCase()}/merged-alignment/"
        pathAct = processedMergedBamFileService.directory(bamFile.mergingPass)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenQualityAssessmentStatusIsFalse() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.qualityAssessmentStatus = QaProcessingStatus.IN_PROGRESS
        mergedBamFile.mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenFileOperationStatusFalse() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.fileOperationStatus = FileOperationStatus.INPROGRESS
        mergedBamFile.mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL))
    }

    @Test(expected = ValidationException)
    void testMergedBamFileWithFinishedQAWhenMd5sumNotNullStateIncorrect() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        mergedBamFile.mergingSet.status = State.PROCESSED
        mergedBamFile.fileSize = 10000
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenMd5sumNotNull() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        mergedBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
        mergedBamFile.mergingSet.status = State.PROCESSED
        mergedBamFile.fileSize = 10000
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenStatusNotProcessed() {
        createProcessedMergedBamFile()
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL))
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenMergingNotFinished() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.mergingSet.status = State.PROCESSED
        mergedBamFile.status = AbstractBamFile.State.NEEDS_PROCESSING
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL))
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
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL))
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
        ProcessedMergedBamFile processedMergedBamFileAct = processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL)
        assertEquals(processedMergedBamFileExp, processedMergedBamFileAct)
    }

    @Test
    void testMergedBamFileWithFinishedQA_FastTrackFirst() {
        setupForSuccessfulMergedBamFileWithFinishedQa()

        ProcessedMergedBamFile processedMergedBamFileFastTrackPriority = setupForSuccessfulMergedBamFileWithFinishedQa()

        processedMergedBamFileFastTrackPriority.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        assert processedMergedBamFileFastTrackPriority.project.save(flush: true)

        ProcessedMergedBamFile processedMergedBamFileAct = processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.NORMAL)
        assertEquals(processedMergedBamFileFastTrackPriority, processedMergedBamFileAct)
    }


    @Test
    void testMergedBamFileWithFinishedQA_JobsReservedForFastTrack() {
        ProcessedMergedBamFile processedMergedBamFileFastTrackPriority = setupForSuccessfulMergedBamFileWithFinishedQa()
        processedMergedBamFileFastTrackPriority.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        assert processedMergedBamFileFastTrackPriority.project.save(flush: true)

        ProcessedMergedBamFile processedMergedBamFileAct = processedMergedBamFileService.mergedBamFileWithFinishedQA(ProcessingPriority.FAST_TRACK)
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
        String sourceDirectoryExp = dataProcessingFilesService.getOutputDirectory(mergedBamFile.individual, dirType) + "/sample-type/seq-type/${LibraryLayout.PAIRED.name().toLowerCase()}/DEFAULT/0/pass0"
        assertEquals(sourceDirectoryExp, locations["sourceDirectory"])
        String destinationDirectoryExp =  testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/${LibraryLayout.PAIRED.name().toLowerCase()}/merged-alignment/"
        assertEquals(destinationDirectoryExp, locations["destinationDirectory"])
        String bamFileExp = "sample-type_patient_seq-type_${LibraryLayout.PAIRED.name()}_merged.mdup.bam"
        assertEquals(bamFileExp, locations["bamFile"])
        String baiFileExp = "sample-type_patient_seq-type_${LibraryLayout.PAIRED.name()}_merged.mdup.bai"
        assertEquals(baiFileExp, locations["baiFile"])
        String md5BamFileExp = bamFileExp + ".md5sum"
        assertEquals(md5BamFileExp, locations["md5BamFile"])
        String md5BaiFileExp = baiFileExp + ".md5sum"
        assertEquals(md5BaiFileExp, locations["md5BaiFile"])
    }


    @Test(expected = AssertionError)
    void testDestinationTempDirectoryBamFileNull() {
        processedMergedBamFileService.destinationTempDirectory(null)
    }

    @Test
    void testDestinationTempDirectory() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        String destinationExp =  testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/${LibraryLayout.PAIRED.name().toLowerCase()}/merged-alignment/.tmp"
        String destinationAct = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test(expected = AssertionError)
    void testQaResultTempDestinationDirectoryBamFileNull() {
        processedMergedBamFileService.qaResultTempDestinationDirectory(null)
    }

    @Test
    void testQaResultTempDestinationDirectory() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        String destinationExp =  testDirectory.absolutePath + "/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/${LibraryLayout.PAIRED.name().toLowerCase()}/merged-alignment/.tmp/QualityAssessment"
        String destinationAct = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test(expected = AssertionError)
    void testSampleFileIsNull() {
        processedMergedBamFileService.sample(null)
    }

    @Test
    void testSample() {
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile()
        mergedBamFile.sample
        processedMergedBamFileService.sample(mergedBamFile)
    }

    @Test(expected = AssertionError)
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


    @Test(expected = AssertionError)
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
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                seqType: DomainFactory.createExomeSeqType(),
                libraryPreparationKit: libraryPreparationKit,
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        ])
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
        LibraryPreparationKit libraryPreparationKit = DomainFactory.createLibraryPreparationKit()
        SeqType seqType = DomainFactory.createExomeSeqType()
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(
                seqType: seqType,
                libraryPreparationKit: libraryPreparationKit,
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        )
        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage)
        DomainFactory.assignNewProcessedBamFile(processedMergedBamFile)
        List<SeqTrack> seqTracks = processedMergedBamFile.mergingSet.containedSeqTracks as List
        seqTracks[0].libraryPreparationKit = libraryPreparationKit
        seqTracks[1].libraryPreparationKit = libraryPreparationKit
        seqTracks[0].kitInfoReliability = InformationReliability.KNOWN
        seqTracks[1].kitInfoReliability = InformationReliability.KNOWN
        assertNull(processedMergedBamFileService.getInferredKit(processedMergedBamFile))
    }


    @Test
    void testGetInferredKitBamFileIsExomeAndOneKitInferred() {
        LibraryPreparationKit libraryPreparationKit = LibraryPreparationKit.build()
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                seqType: DomainFactory.createSeqType(name: SeqTypeNames.EXOME.seqTypeName),
                libraryPreparationKit: libraryPreparationKit,
                pipeline: DomainFactory.createDefaultOtpPipeline(),
        ])
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
        mergedBamFile.project.realm = realm
        mergedBamFile.project.dirName = 'project-dir'
        mergedBamFile.individual.pid = 'patient'
        mergedBamFile.sampleType.name = 'sample-type'
        mergedBamFile.seqType.name = 'seq-type'
        mergedBamFile.seqType.dirName = 'seq-type-dir'
        mergedBamFile.seqType.libraryLayout = LibraryLayout.PAIRED
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
                status: AbstractBamFile.State.DECLARED,
        ])
    }
}
