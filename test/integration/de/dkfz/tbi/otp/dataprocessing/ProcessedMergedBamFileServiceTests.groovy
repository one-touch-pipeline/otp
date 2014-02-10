package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.util.Environment
import grails.validation.ValidationException
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.MergingSet.State
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type

class ProcessedMergedBamFileServiceTests {

    ProcessedMergedBamFileService processedMergedBamFileService
    DataProcessingFilesService dataProcessingFilesService

    File baseDir
    Sample sample
    SeqType seqType
    MergingSet mergingSet
    Project project
    Individual individual
    SeqTrack seqTrack
    SampleType sampleType
    SoftwareTool softwareTool
    SeqPlatform seqPlatform
    Run run

    final static String directory = "/tmp/otp-unit-test/pmbfs/processing/project-dir/results_per_pid/patient/merging//sample-type/seq-type/library/DEFAULT/0/pass0"
    final static String baseFile = "sample-type_patient_seq-type_library_merged.mdup"
    final static String basePath = directory + "/" + baseFile

    @Before
    void setUp() {
        Map paths = [
            rootPath: '/tmp/otp-unit-test/pmfs/root',
            processingRootPath: '/tmp/otp-unit-test/pmbfs/processing',
            ]

        Realm realm = DomainFactory.createRealmDataProcessingDKFZ(paths).save([flush: true])
        Realm realm1 = DomainFactory.createRealmDataManagementDKFZ(paths).save([flush: true])

        project = new Project(
                        name: "project",
                        dirName: "project-dir",
                        realmName: 'DKFZ',
                        )
        assertNotNull(project.save([flush: true]))

        individual = new Individual(
                        pid: "patient",
                        mockPid: "mockPid",
                        mockFullName: "mockFullName",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true]))

        sampleType = new SampleType(
                        name: "sample-type"
                        )
        assertNotNull(sampleType.save([flush: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true]))

        seqType = new SeqType(
                        name: "seq-type",
                        libraryLayout: "library",
                        dirName: "seq-type-dir"
                        )
        assertNotNull(seqType.save([flush: true]))

        softwareTool = new SoftwareTool(
                        programName: "name",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true]))

        seqPlatform = new SeqPlatform(
                        name: "name",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true]))

        SeqCenter seqCenter = new SeqCenter(
                        name: "name",
                        dirName: "dirName"
                        )
        assertNotNull(seqCenter.save([flush: true]))

        run = new Run(
                        name: "name",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true]))

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
        sample = null
        seqType = null
        mergingSet = null
        project = null
        individual = null
        seqTrack = null
        sampleType = null
        softwareTool = null
        seqPlatform = null
        run = null
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateBamMetricsFileFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamMetricsFileFileIsEmpty() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForMetrics(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamMetricsFileFileNotReadable() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForMetrics(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile)
    }

    @Test
    void testUpdateBamMetricsFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertTrue(processedMergedBamFileService.updateBamMetricsFile(processedMergedBamFile))
    }


    @Test(expected = IllegalArgumentException)
    void testUpdateBamFileIndexFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileIndexFileIsEmpty() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForBai(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileIndexFileNotReadable() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePathForBai(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile)
    }

    @Test
    void testUpdateBamFileIndex() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertTrue(processedMergedBamFileService.updateBamFileIndex(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testUpdateBamFileBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileFileNotReadable() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePath(processedMergedBamFile))
        file.setReadable(false)
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test(expected = RuntimeException)
    void testUpdateBamFileFileIsEmpty() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        File file = new File(processedMergedBamFileService.filePath(processedMergedBamFile))
        file.text = ""
        processedMergedBamFileService.updateBamFile(processedMergedBamFile)
    }

    @Test
    void testUpdateBamFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertTrue(processedMergedBamFileService.updateBamFile(processedMergedBamFile))
    }

    @Test
    void testCreateMergedBamFile() {
        MergingPass mergingPass = createMergingPass()
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
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String pathExp = basePath + ".bai"
        String pathAct = processedMergedBamFileService.filePathForBai(processedMergedBamFile)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFileNameForBaiBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.fileNameForBai(processedMergedBamFile)
    }

    @Test
    void testFileNameForBai() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String nameExp = baseFile + ".bai"
        String nameAct = processedMergedBamFileService.fileNameForBai(processedMergedBamFile)
        assertEquals(nameExp, nameAct)
    }

    @Test(expected = IllegalArgumentException)
    void testFilePathForMetricsBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.filePathForMetrics(processedMergedBamFile)
    }

    @Test
    void testFilePathForMetrics() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
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
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
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
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        String pathExp = basePath +".bam"
        assertEquals(pathExp, processedMergedBamFileService.filePath(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testFileNameBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.fileName(processedMergedBamFile)
    }

    @Test
    void testFileName() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertEquals(baseFile + ".bam", processedMergedBamFileService.fileName(processedMergedBamFile))
    }

    @Test(expected = IllegalArgumentException)
    void testDirectoryByProcessedMergedBamFileBamFileIsNull() {
        ProcessedMergedBamFile processedMergedBamFile = null
        processedMergedBamFileService.directory(processedMergedBamFile)
    }

    @Test
    void testDirectoryByProcessedMergedBamFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
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
        MergingPass mergingPass = createMergingPass()
        String pathExp = directory
        String pathAct = processedMergedBamFileService.directory(mergingPass)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testDirectoryByMergingPass() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile bamFile = createProcessedMergedBamFile(mergingPass)
        String pathExp = directory
        String pathAct = processedMergedBamFileService.directory(mergingPass)
        assertEquals(pathExp, pathAct)
        bamFile.fileOperationStatus = FileOperationStatus.PROCESSED
        bamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        pathExp = "/tmp/otp-unit-test/pmfs/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/"
        pathAct = processedMergedBamFileService.directory(mergingPass)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenQualityAssessmentStatusFalse() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        mergedBamFile.qualityAssessmentStatus = QaProcessingStatus.IN_PROGRESS
        mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenFileOperationStatusFalse() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        mergedBamFile.fileOperationStatus = FileOperationStatus.INPROGRESS
        mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    @Test(expected = ValidationException)
    void testMergedBamFileWithFinishedQAWhenMd5sumNotNullStateIncorrect() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    void testMergedBamFileWithFinishedQAWhenMd5sumNotNull() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        mergedBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
        mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenStatusNotProcessed() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenMergingNotFinished() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        mergingSet.status = State.PROCESSED
        mergedBamFile.status = AbstractBamFile.State.NEEDS_PROCESSING
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenSingleLaneQaNotFinished() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        ProcessedBamFile processedBamFile = createProcessedBamFile()
        processedBamFile.qualityAssessmentStatus = QaProcessingStatus.IN_PROGRESS
        mergingSet.status = State.PROCESSED
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenMergedBamFileUsedForOngoingMerging() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        mergingSet.status = State.PROCESSED
        MergingWorkPackage mergingWorkPackage1 = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage1.save([flush: true]))
        MergingSet mergingSet1 = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage1,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet1.save([flush: true]))
        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet1,
                        bamFile: mergedBamFile
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true]))
        assertNull(processedMergedBamFileService.mergedBamFileWithFinishedQA())
    }

    @Test
    void testMergedBamFileWithFinishedQAWhenAllCorrect() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        mergingSet.status = State.PROCESSED
        mergedBamFile.fileOperationStatus = FileOperationStatus.PROCESSED
        mergedBamFile.md5sum = "68b329da9893e34099c7d8ad5cb9c940"
        ProcessedBamFile processedBamFile = createProcessedBamFile()
        MergingWorkPackage mergingWorkPackage1 = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage1.save([flush: true]))
        MergingSet mergingSet1 = new MergingSet(
                        identifier: 1,
                        mergingWorkPackage: mergingWorkPackage1,
                        status: State.PROCESSED
                        )
        assertNotNull(mergingSet1.save([flush: true]))
        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet1,
                        bamFile: mergedBamFile
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true]))
        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet1,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true]))
        MergingPass mergingPass1 = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet1
                        )
        assertNotNull(mergingPass1.save([flush: true]))
        ProcessedMergedBamFile mergedBamFile1 = createProcessedMergedBamFile(mergingPass1)
        ProcessedMergedBamFile processedMergedBamFileExp = mergedBamFile1
        ProcessedMergedBamFile processedMergedBamFileAct = processedMergedBamFileService.mergedBamFileWithFinishedQA()
        assertEquals(processedMergedBamFileExp, processedMergedBamFileAct)
    }

    @Test
    void testProject() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        Project projectExp = project
        Project projectAct = processedMergedBamFileService.project(mergedBamFile)
        assertEquals(projectExp, projectAct)
    }

    @Test
    void testStoreMD5Digest() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        assertNull(mergedBamFile.md5sum)
        processedMergedBamFileService.storeMD5Digest(mergedBamFile, "68b329da9893e34099c7d8ad5cb9c940")
        String md5Exp = "68b329da9893e34099c7d8ad5cb9c940"
        String md5Act = mergedBamFile.md5sum
        assertEquals(md5Exp, md5Act)
    }

    @Test
    void testSingleLaneQAResultsDirectories() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        ProcessedBamFile processedBamFile = createProcessedBamFile()
        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass(
                        identifier: 1,
                        processedBamFile:processedBamFile
                        )
        assertNotNull(qualityAssessmentPass.save([flush: true]))

        Map<String, String> locations = processedMergedBamFileService.singleLaneQAResultsDirectories(mergedBamFile)
        String singleLaneDirectoryNameExp = "name_laneId"
        String singleLaneDirectoryNameAct = locations.keySet().iterator().next()
        assertEquals(singleLaneDirectoryNameExp, singleLaneDirectoryNameAct)
        String singleLanePathExp = "/tmp/otp-unit-test/pmbfs/processing/project-dir/results_per_pid/patient/alignment//name_laneId/pass1/QualityAssessment/pass1"
        String singleLanePathAct = locations."${singleLaneDirectoryNameAct}"
        assertEquals(singleLanePathExp, singleLanePathAct)
    }

    @Test
    void testUpdateFileOperationStatus() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        FileOperationStatus statusExp = FileOperationStatus.NEEDS_PROCESSING
        FileOperationStatus statusAct = mergedBamFile.fileOperationStatus
        assertEquals(statusExp, statusAct)
        processedMergedBamFileService.updateFileOperationStatus(mergedBamFile, FileOperationStatus.INPROGRESS)
        statusExp = FileOperationStatus.INPROGRESS
        statusAct = mergedBamFile.fileOperationStatus
        assertEquals(statusExp, statusAct)
    }

    @Test
    void testLocationsForFileCopying() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        Map<String, String> locations = processedMergedBamFileService.locationsForFileCopying(mergedBamFile)
        DataProcessingFilesService.OutputDirectories dirType = DataProcessingFilesService.OutputDirectories.MERGING
        String sourceDirectoryExp = dataProcessingFilesService.getOutputDirectory(individual, dirType) + "/sample-type/seq-type/library/DEFAULT/0/pass0"
        assertEquals(sourceDirectoryExp, locations["sourceDirectory"])
        String destinationDirectoryExp = "/tmp/otp-unit-test/pmfs/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/"
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

    @Test
    void testDestinationDirectory() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        String destinationExp = "/tmp/otp-unit-test/pmfs/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/"
        String destinationAct = processedMergedBamFileService.destinationDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test(expected = IllegalArgumentException)
    void testDestinationTempDirectoryBamFileNull() {
        processedMergedBamFileService.destinationTempDirectory(null)
    }

    @Test
    void testDestinationTempDirectory() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        String destinationExp = "/tmp/otp-unit-test/pmfs/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/.tmp"
        String destinationAct = processedMergedBamFileService.destinationTempDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test(expected = IllegalArgumentException)
    void testQaResultTempDestinationDirectoryBamFileNull() {
        processedMergedBamFileService.qaResultTempDestinationDirectory(null)
    }

    @Test
    void testQaResultTempDestinationDirectory() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        String destinationExp = "/tmp/otp-unit-test/pmfs/root/project-dir/sequencing/seq-type-dir/view-by-pid/patient/sample-type/library/merged-alignment/.tmp/QualityAssessment"
        String destinationAct = processedMergedBamFileService.qaResultTempDestinationDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test(expected = IllegalArgumentException)
    void testSampleFileIsNull() {
        processedMergedBamFileService.sample(null)
    }

    @Test
    void testSample() {
        MergingPass mergingPass = createMergingPass()
        ProcessedMergedBamFile mergedBamFile = createProcessedMergedBamFile(mergingPass)
        Sample sampleExp = sample
        Sample sampleAct = processedMergedBamFileService.sample(mergedBamFile)
    }

    @Test(expected = IllegalArgumentException)
    void testFastqFilesPerMergedBamFileInputNull() {
        processedMergedBamFileService.fastqFilesPerMergedBamFile(null)
    }

    @Test
    void testFastqFilesPerMergedBamFile() {
        MergingPass mergingPass = createMergingPass()
        ProcessedBamFile processedBamFile = createProcessedBamFile()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        FileType fileType = new FileType(
                        type: de.dkfz.tbi.otp.ngsdata.FileType.Type.SEQUENCE
                        )
        assertNotNull(fileType.save([flush: true]))

        DataFile dataFile = new DataFile(
                        fileName: "dataFile",
                        seqTrack: seqTrack,
                        fileType: fileType
                        )
        assertNotNull(dataFile.save([flush: true]))
        assertEquals([dataFile], processedMergedBamFileService.fastqFilesPerMergedBamFile(processedMergedBamFile))
    }

    @Test
    void testFastqFilesPerMergedBamFileNoFastqFiles() {
        MergingPass mergingPass = createMergingPass()
        ProcessedBamFile processedBamFile = createProcessedBamFile()
        ProcessedMergedBamFile processedMergedBamFile = createProcessedMergedBamFile(mergingPass)
        FileType fileType = new FileType(
                        type: de.dkfz.tbi.otp.ngsdata.FileType.Type.ALIGNMENT
                        )
        assertNotNull(fileType.save([flush: true]))

        DataFile dataFile = new DataFile(
                        fileName: "dataFile",
                        seqTrack: seqTrack,
                        fileType: fileType
                        )
        assertNotNull(dataFile.save([flush: true]))
        assertTrue(processedMergedBamFileService.fastqFilesPerMergedBamFile(processedMergedBamFile).isEmpty())
    }

    private ProcessedMergedBamFile createProcessedMergedBamFile(MergingPass mergingPass) {
        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        fileOperationStatus: FileOperationStatus.NEEDS_PROCESSING,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))
        return processedMergedBamFile
    }

    private MergingPass createMergingPass() {
        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: State.NEEDS_PROCESSING
                        )
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))
        return mergingPass
    }

    private ProcessedBamFile createProcessedBamFile() {
        seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true]))

        AlignmentPass alignmentPass = new AlignmentPass(
                        identifier: 1,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.NEEDS_PROCESSING,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED
                        )
        assertNotNull(processedBamFile.save([flush: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true]))

        return processedBamFile
    }
}
