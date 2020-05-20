/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata


import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

@Rollback
@Integration
class DeletionServiceTests implements UserAndRoles {

    DeletionService deletionService
    LsdfFilesService lsdfFilesService
    DataProcessingFilesService dataProcessingFilesService
    TestConfigService configService

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    Path outputFolder

    void setupData() {
        createUserAndRoles()
        outputFolder = temporaryFolder.newFolder("outputFolder").toPath()
        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : outputFolder.toString(),
                (OtpProperty.PATH_PROCESSING_ROOT): outputFolder.toString(),
        ])
    }

    @After
    void tearDown() {
        configService.clean()
    }

    @Test
    void testDeleteFastQCInformationFromDataFile() {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(dataFile: dataFile)

        deletionService.deleteFastQCInformationFromDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
    }

    @Test
    void testDeleteMetaDataEntryForDataFile() {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        MetaDataEntry metaDataEntry = DomainFactory.createMetaDataEntry(dataFile: dataFile)

        deletionService.deleteMetaDataEntryForDataFile(dataFile)

        assert !MetaDataEntry.get(metaDataEntry.id)
    }

    @Test
    void testDeleteConsistencyStatusInformationForDataFile() {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        ConsistencyStatus consistencyStatus = DomainFactory.createConsistencyStatus(dataFile: dataFile)

        deletionService.deleteConsistencyStatusInformationForDataFile(dataFile)

        assert !ConsistencyStatus.get(consistencyStatus.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedBamFile() {
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.createProcessedBamFile()

        QualityAssessmentPass qualityAssessmentPass = DomainFactory.createQualityAssessmentPass(processedBamFile: abstractBamFile)
        ChromosomeQualityAssessment chromosomeQualityAssessment = DomainFactory.createChromosomeQualityAssessment(qualityAssessmentPass: qualityAssessmentPass, referenceLength: 0)
        OverallQualityAssessment overallQualityAssessment = DomainFactory.createOverallQualityAssessment(qualityAssessmentPass: qualityAssessmentPass, referenceLength: 0)

        deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessment.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessment.get(overallQualityAssessment.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedMergedBamFile() {
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.createProcessedMergedBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = DomainFactory.createQualityAssessmentMergedPass(abstractMergedBamFile: abstractBamFile)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessment = DomainFactory.createChromosomeQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentPass, referenceLength: 0)
        OverallQualityAssessmentMerged overallQualityAssessment = DomainFactory.createOverallQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentPass, referenceLength: 0)
        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = DomainFactory.createPicardMarkDuplicatesMetrics(abstractBamFile: abstractBamFile)

        deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessmentMerged.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessmentMerged.get(overallQualityAssessment.id)
        assert !PicardMarkDuplicatesMetrics.get(picardMarkDuplicatesMetrics.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_RoddyBamFile() {
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.createRoddyBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = DomainFactory.createQualityAssessmentMergedPass(abstractMergedBamFile: abstractBamFile)
        RoddyLibraryQa roddyLibraryQa = DomainFactory.createRoddyLibraryQa(qualityAssessmentMergedPass: qualityAssessmentPass,
                genomeWithoutNCoverageQcBases: 0, referenceLength: 0)
        RoddyMergedBamQa roddyMergedBamQa = DomainFactory.createRoddyMergedBamQa(qualityAssessmentMergedPass: qualityAssessmentPass,
                genomeWithoutNCoverageQcBases: 0, referenceLength: 0)
        RoddySingleLaneQa roddySingleLaneQa = DomainFactory.createRoddySingleLaneQa(seqTrack: abstractBamFile.seqTracks.iterator().next(),
                qualityAssessmentMergedPass: qualityAssessmentPass, genomeWithoutNCoverageQcBases: 0, referenceLength: 0)

        deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !RoddyLibraryQa.get(roddyLibraryQa.id)
        assert !RoddyMergedBamQa.get(roddyMergedBamQa.id)
        assert !RoddySingleLaneQa.get(roddySingleLaneQa.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_SingleCellBamFile() {
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.proxyCellRanger.createBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = DomainFactory.createQualityAssessmentMergedPass(abstractMergedBamFile: abstractBamFile)

        deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_null() {
        setupData()
        AbstractBamFile abstractBamFile = null

        final shouldFail = new GroovyTestCase().&shouldFail
        String message = shouldFail RuntimeException, {
            deletionService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)
        }
        assert message == "The input AbstractBamFile is null"
    }

    @Test
    void testDeleteMergingRelatedConnectionsOfBamFile() {
        setupData()
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                pipeline: DomainFactory.createDefaultOtpPipeline()
        ])
        MergingSet mergingSet = DomainFactory.createMergingSet(mergingWorkPackage: mergingWorkPackage)
        ProcessedBamFile processedBamFile = DomainFactory.createProcessedBamFile(mergingWorkPackage).save(flush: true)
        MergingPass mergingPass = DomainFactory.createMergingPass(mergingSet: mergingSet)
        MergingSetAssignment mergingSetAssignment = DomainFactory.createMergingSetAssignment(bamFile: processedBamFile, mergingSet: mergingSet)
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(workPackage: mergingWorkPackage, mergingPass: mergingPass)

        deletionService.deleteMergingRelatedConnectionsOfBamFile(processedBamFile)

        assert !MergingPass.get(mergingPass.id)
        assert !MergingSet.get(mergingSet.id)
        assert !MergingSetAssignment.get(mergingSetAssignment.id)
        assert !ProcessedMergedBamFile.get(bamFile.id)
    }

    @Test
    void testDeleteDataFile() {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(dataFile: dataFile)

        DomainFactory.createMetaDataEntry(dataFile: dataFile)

        DomainFactory.createConsistencyStatus(dataFile: dataFile)

        deletionService.deleteDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
    }

    @Test
    void testDeleteConnectionFromSeqTrackRepresentingABamFile() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        AlignmentLog alignmentLog = DomainFactory.createAlignmentLog(seqTrack: seqTrack)
        DataFile dataFile = DomainFactory.createDataFile(alignmentLog: alignmentLog)

        deletionService.deleteConnectionFromSeqTrackRepresentingABamFile(seqTrack)

        assert !AlignmentLog.get(alignmentLog.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_ProcessedBamFile() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        ProcessedSaiFile processedSaiFile = DomainFactory.createProcessedSaiFile(dataFile: dataFile)

        new TestData()
        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass(seqTrack: seqTrack)
        MergingWorkPackage workPackage = alignmentPass.workPackage
        MergingSet mergingSet = DomainFactory.createMergingSet(mergingWorkPackage: workPackage)
        MergingPass mergingPass = DomainFactory.createMergingPass(mergingSet: mergingSet)

        AbstractMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass: mergingPass, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS, workPackage: workPackage)
        workPackage.bamFileInProjectFolder = processedMergedBamFile
        workPackage.save(flush: true)
        alignmentPass.save(flush: true)

        deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(alignmentPass.seqTrack)

        assert !ProcessedSaiFile.get(processedSaiFile.id)
        assert !ProcessedBamFile.get(processedMergedBamFile.id)
        assert !AlignmentPass.get(alignmentPass.id)
    }

    @Test
    void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_RoddyBamFile() {
        setupData()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
        roddyBamFile.workPackage.save(flush: true)

        deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(roddyBamFile.seqTracks.iterator().next())

        assert !RoddyBamFile.get(roddyBamFile.id)
        assert !MergingWorkPackage.get(roddyBamFile.workPackage.id)
    }

    @Test
    void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_SingleCellBamFile() {
        setupData()
        SingleCellBamFile singleCellBamFile = DomainFactory.proxyCellRanger.createBamFile()
        singleCellBamFile.workPackage.bamFileInProjectFolder = singleCellBamFile
        singleCellBamFile.workPackage.save(flush: true)

        deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(singleCellBamFile.seqTracks.iterator().next())

        assert !RoddyBamFile.get(singleCellBamFile.id)
        assert !MergingWorkPackage.get(singleCellBamFile.workPackage.id)
    }

    @Test
    void testDeleteSeqScanAndCorrespondingInformation() {
        setupData()
        SeqScan seqScan = DomainFactory.createSeqScan()
        MergingLog mergingLog = DomainFactory.createMergingLog(seqScan: seqScan)
        MergedAlignmentDataFile mergedAlignmentDataFile = DomainFactory.createMergedAlignmentDataFile(mergingLog: mergingLog)

        deletionService.deleteSeqScanAndCorrespondingInformation(seqScan)

        assert !MergingLog.get(mergingLog.id)
        assert !MergedAlignmentDataFile.get(mergedAlignmentDataFile.id)
        assert !SeqScan.get(seqScan.id)
    }

    @Test
    void testDeleteSeqTrack() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        MergingAssignment mergingAssignment = DomainFactory.createMergingAssignment(seqTrack: seqTrack)
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)

        deletionService.deleteSeqTrack(seqTrack)

        assert !SeqTrack.get(seqTrack.id)
        assert !MergingAssignment.get(mergingAssignment.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testDeleteSeqTrack_seqTrackIsOnlyLinked() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(linkedExternally: true)
        DomainFactory.createMergingAssignment(seqTrack: seqTrack)
        DomainFactory.createDataFile(seqTrack: seqTrack)

        TestCase.shouldFailWithMessageContaining(AssertionError, "seqTracks only linked") {
            deletionService.deleteSeqTrack(seqTrack)
        }
    }

    @Test
    void testDeleteRun() {
        setupData()
        Run run = DomainFactory.createRun()
        DataFile dataFile = DomainFactory.createDataFile(run: run)

        deletionService.deleteRun(run)

        assert !Run.get(run.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testDeleteRunByName() {
        setupData()
        Run run = DomainFactory.createRun()
        DataFile dataFile = DomainFactory.createDataFile(run: run)

        deletionService.deleteRunByName(run.name)

        assert !Run.get(run.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testDeleteProcessingFilesOfProject_EmptyProject() {
        setupData()
        Project project = DomainFactory.createProject()

        TestCase.shouldFail(AssertionError) {
            deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesMissing() {
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project

        TestCase.shouldFail(FileNotFoundException) {
            deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesAvailable() {
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()

        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesLinked() {
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        markFilesAsLinked(SeqTrack.list())

        TestCase.shouldFail(FileNotFoundException) {
            deletionService.deleteProcessingFilesOfProject(project.name, outputFolder)
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesLinked_Verified() {
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        markFilesAsLinked(SeqTrack.list())

        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true)
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesWithdrawn() {
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project
        markFilesAsWithdrawn([seqTrack])

        TestCase.shouldFail(FileNotFoundException) {
            deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true)
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesWithdrawn_IgnoreWithdrawn() {
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project
        markFilesAsWithdrawn([seqTrack])

        deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true, true)
    }

    @Test
    void testDeleteProcessingFileSOfProject_NoProcessedData_FastqFilesAvailalbe_explicitSeqTrack() {
        setupData()
        SeqTrack st = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([st])

        assert [st] == deletionService.deleteProcessingFilesOfProject(st.project.name, outputFolder, true, true, [st])
    }

    @Test
    void testDeleteProcessingFileSOfProject_NoProcessedData_FastqFilesAvailalbe_explicitSeqTrackDifferentProject_ShouldFail() {
        setupData()
        SeqTrack st = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([st])

        Project project = DomainFactory.createProject()

        TestCase.shouldFail(AssertionError) {
            assert [st] == deletionService.deleteProcessingFilesOfProject(project.name, outputFolder, true, true, [st])
        }
    }

    private SeqTrack deleteProcessingFilesOfProject_NoProcessedData_Setup() {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles()

        return seqTrack
    }

    private Project deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles() {
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([seqTrack])

        return seqTrack.project
    }

    private void markFilesAsLinked(List<SeqTrack> seqTracks) {
        seqTracks.each {
            it.linkedExternally = true
            assert it.save(flush: true)
        }
    }

    private void markFilesAsWithdrawn(List<SeqTrack> seqTracks) {
        List<DataFile> dataFiles = DataFile.findAllBySeqTrackInList(seqTracks)
        dataFiles*.fileWithdrawn = true
        assert dataFiles*.save(flush: true)
    }

    private void createFastqFiles(List<SeqTrack> seqTracks) {
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        DataFile.findAllBySeqTrackInList(seqTracks).each {
            it.fastqImportInstance = fastqImportInstance
            assert it.save(flush: true)
            CreateFileHelper.createFile(new File(lsdfFilesService.getFileViewByPidPath(it)))
        }
    }

    private void createFastqFiles(AbstractMergedBamFile bamFile) {
        createFastqFiles(bamFile.getContainedSeqTracks() as List)
    }

    private void dataBaseSetupForMergedBamFiles(AbstractMergedBamFile bamFile, boolean addRealm = true) {
        AbstractMergingWorkPackage mergingWorkPackage = bamFile.mergingWorkPackage
        mergingWorkPackage.bamFileInProjectFolder = bamFile
        assert mergingWorkPackage.save(flush: true)
        Project project = bamFile.project
        if (addRealm) {
            project.realm = DomainFactory.createRealm()
        }
    }

    private ProcessedMergedBamFile deleteProcessingFilesOfProject_PMBF_Setup() {
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile([
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum: HelperUtils.randomMd5sum,
                fileSize: 1000,
        ])
        dataBaseSetupForMergedBamFiles(bamFile)
        createFastqFiles(bamFile)

        File processingBamFile = new File(dataProcessingFilesService.getOutputDirectory(bamFile.individual, DataProcessingFilesService.OutputDirectories.MERGING))
        File finalBamFile = new File(AbstractMergedBamFileService.destinationDirectory(bamFile))
        CreateFileHelper.createFile(new File(processingBamFile, "test.bam"))
        CreateFileHelper.createFile(new File(finalBamFile, "test.bam"))

        return bamFile
    }

    private void deleteProcessingFilesOfProject_PMBF_Validation() {
        assert AbstractBamFile.list().empty
        assert MergingWorkPackage.list().empty
        assert AlignmentPass.list().empty
        assert MergingPass.list().empty
    }

    @Test
    void testDeleteProcessingFilesOfProject_PMBF() {
        setupData()
        ProcessedMergedBamFile bamFile = deleteProcessingFilesOfProject_PMBF_Setup()

        File processingBamFile = new File(dataProcessingFilesService.getOutputDirectory(bamFile.individual, DataProcessingFilesService.OutputDirectories.MERGING))
        File finalBamFile = new File(AbstractMergedBamFileService.destinationDirectory(bamFile))
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        assert outputFile.text.contains(processingBamFile.path) && outputFile.text.contains(finalBamFile.path)

        deleteProcessingFilesOfProject_PMBF_Validation()
    }

    @Test
    void testDeleteProcessingFilesOfProject_PMBF_notVerified() {
        setupData()
        ProcessedMergedBamFile bamFile = deleteProcessingFilesOfProject_PMBF_Setup()

        File processingBamFile = new File(dataProcessingFilesService.getOutputDirectory(bamFile.individual, DataProcessingFilesService.OutputDirectories.MERGING))
        File finalBamFile = new File(AbstractMergedBamFileService.destinationDirectory(bamFile))
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)

        assert outputFile.text.contains(processingBamFile.path) && outputFile.text.contains(finalBamFile.path)

        deleteProcessingFilesOfProject_PMBF_Validation()
    }

    private RoddyBamFile deleteProcessingFilesOfProject_RBF_Setup() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()

        dataBaseSetupForMergedBamFiles(bamFile)
        createFastqFiles(bamFile)

        File finalBamFile = new File(AbstractMergedBamFileService.destinationDirectory(bamFile))
        CreateFileHelper.createFile(new File(finalBamFile, "test.bam"))

        return bamFile
    }

    private void deleteProcessingFilesOfProject_RBF_Validation() {
        assert AbstractBamFile.list().empty
        assert MergingWorkPackage.list().empty
    }

    @Test
    void testDeleteProcessingFilesOfProject_RBF() {
        setupData()
        RoddyBamFile bamFile = deleteProcessingFilesOfProject_RBF_Setup()

        File finalBamFile = new File(AbstractMergedBamFileService.destinationDirectory(bamFile))
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        assert outputFile.text.contains(finalBamFile.path)

        deleteProcessingFilesOfProject_RBF_Validation()
    }

    @Test
    void testDeleteProcessingFilesOfProject_RBF_notVerified() {
        setupData()
        RoddyBamFile bamFile = deleteProcessingFilesOfProject_RBF_Setup()

        File finalBamFile = new File(AbstractMergedBamFileService.destinationDirectory(bamFile))
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)

        assert outputFile.text.contains(finalBamFile.path)

        deleteProcessingFilesOfProject_RBF_Validation()
    }

    private AbstractSnvCallingInstance deleteProcessingFilesOfProject_RBF_SNV_Setup() {
        AbstractSnvCallingInstance snvCallingInstance = DomainFactory.createSnvInstanceWithRoddyBamFiles(processingState: AnalysisProcessingStates.FINISHED)

        AbstractMergedBamFile tumorBamFiles = snvCallingInstance.sampleType1BamFile
        dataBaseSetupForMergedBamFiles(tumorBamFiles)
        createFastqFiles(tumorBamFiles)

        AbstractMergedBamFile controlBamFiles = snvCallingInstance.sampleType2BamFile
        dataBaseSetupForMergedBamFiles(controlBamFiles, false)
        createFastqFiles(controlBamFiles)

        File snvFolder = snvCallingInstance.getInstancePath().absoluteDataManagementPath
        CreateFileHelper.createFile(new File(snvFolder, "test.vcf"))

        return snvCallingInstance
    }

    private void deleteProcessingFilesOfProject_RBF_SNV_Validation(AbstractSnvCallingInstance snvCallingInstance) {
        File snvFolder = snvCallingInstance.samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()

        Path outputFile = outputFolder.resolve("Delete_${snvCallingInstance.project.name}.sh")
        assert outputFile.text.contains(snvFolder.path) && outputFile.text.contains(snvFolder.parent)

        assert AbstractSnvCallingInstance.list().empty
        assert SamplePair.list().empty
    }

    @Test
    void testDeleteProcessingFilesOfProject_RBF_SNV() {
        setupData()
        AbstractSnvCallingInstance snvCallingInstance = deleteProcessingFilesOfProject_RBF_SNV_Setup()

        deletionService.deleteProcessingFilesOfProject(snvCallingInstance.project.name, outputFolder, true)

        deleteProcessingFilesOfProject_RBF_SNV_Validation(snvCallingInstance)
    }

    @Test
    void testDeleteProcessingFilesOfProject_RBF_SNV_notVerified() {
        setupData()
        AbstractSnvCallingInstance snvCallingInstance = deleteProcessingFilesOfProject_RBF_SNV_Setup()

        deletionService.deleteProcessingFilesOfProject(snvCallingInstance.project.name, outputFolder)

        deleteProcessingFilesOfProject_RBF_SNV_Validation(snvCallingInstance)
    }

    private ExternallyProcessedMergedBamFile deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup() {
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        SeqTrack seqTrack = SeqTrack.createCriteria().get {
            sample {
                individual {
                    eq('project', project)
                }
            }
        }

        ExternallyProcessedMergedBamFile bamFile = DomainFactory.createExternallyProcessedMergedBamFile(
                workPackage: DomainFactory.createExternalMergingWorkPackage(
                        sample: seqTrack.sample,
                        seqType: seqTrack.seqType,
                )
        )
        CreateFileHelper.createFile(bamFile.getNonOtpFolder())

        return bamFile
    }

    private void deleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified_Validation(ExternallyProcessedMergedBamFile bamFile) {
        File nonOtpFolder = bamFile.getNonOtpFolder()
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        assert !outputFile.text.contains(nonOtpFolder.path)
        assert ExternallyProcessedMergedBamFile.list().contains(bamFile)
    }

    @Test
    void testDeleteProcessingFilesOfProject_ExternalBamFilesAttached() {
        setupData()
        ExternallyProcessedMergedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        TestCase.shouldFailWithMessageContaining(AssertionError, "external merged bam files", {
            deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)
        })
    }

    @Test
    void testDeleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified() {
        setupData()
        ExternallyProcessedMergedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        deleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified_Validation(bamFile)
    }

    @Test
    void testDeleteProcessingFilesOfProject_ExternalBamFilesAttached_nonMergedSeqTrackExists_Verified() {
        setupData()
        ExternallyProcessedMergedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles([sample: bamFile.sample, seqType: bamFile.seqType])
        createFastqFiles([seqTrack])

        deletionService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        deleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified_Validation(bamFile)
    }

    @Test
    void testDeleteIndividual_SnvWasExecuted() {
        setupData()
        testDeleteIndividualMethod(DomainFactory.createSnvInstanceWithRoddyBamFiles())
    }

    @Test
    void testDeleteIndividual_IndelWasExecuted() {
        setupData()
        testDeleteIndividualMethod(DomainFactory.createIndelCallingInstanceWithRoddyBamFiles())
    }

    private void testDeleteIndividualMethod(BamFilePairAnalysis instance) {
        List<File> filesToDelete = []

        String pid = instance.individual.pid
        instance.sampleType1BamFile.containedSeqTracks.each { SeqTrack seqTrack ->
            filesToDelete << seqTrack.dataFiles.collect { new File(lsdfFilesService.getFileFinalPath(it)) }
        }
        filesToDelete << instance.instancePath.absoluteDataManagementPath
        filesToDelete << instance.individual.getViewByPidPath(instance.seqType).absoluteDataManagementPath

        List<String> allFilesToDeleteCmd = deletionService.deleteIndividual(pid)
        String allFilesToDeleteCmdConcatenated = allFilesToDeleteCmd[0] + allFilesToDeleteCmd[1]

        assert !Individual.findByPid(pid)

        filesToDelete.flatten().each { File file ->
            assert allFilesToDeleteCmdConcatenated.contains(file.absolutePath)
        }
    }
}
