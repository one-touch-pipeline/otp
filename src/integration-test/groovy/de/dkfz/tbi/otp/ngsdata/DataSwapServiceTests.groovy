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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*

import java.nio.file.Path

@Rollback
@Integration
class DataSwapServiceTests implements UserAndRoles {
    DataSwapService dataSwapService
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
    void test_moveSample() {
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        String script = "TEST-MOVE_SAMPLE"
        Individual individual = DomainFactory.createIndividual(project: bamFile.project)

        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
        }

        String dataFileName1 = 'DataFileFileName_R1.gz'
        String dataFileName2 = 'DataFileFileName_R2.gz'

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        List<File> roddyFilesToDelete = createRoddyFileListToDelete(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = temporaryFolder.newFolder("files").toPath()

        SpringSecurityUtils.doWithAuth(ADMIN) {
            dataSwapService.moveSample(
                    bamFile.project.name,
                    bamFile.project.name,
                    bamFile.individual.pid,
                    individual.pid,
                    bamFile.sampleType.name,
                    bamFile.sampleType.name,
                    [(dataFileName1): '', (dataFileName2): ''],
                    script,
                    new StringBuilder(),
                    false,
                    scriptFolder,
                    false,
            )
        }

        assert scriptFolder.toFile().listFiles().length != 0

        File alignmentScript = scriptFolder.resolve("restartAli_${script}.groovy").toFile()
        assert alignmentScript.exists()
        assert alignmentScript.text.contains("ctx.seqTrackService.decideAndPrepareForAlignment(SeqTrack.get(${bamFile.seqTracks.iterator().next().id}))")

        File copyScriptOtherUser = scriptFolder.resolve("${script}-otherUser.sh").toFile()
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        roddyFilesToDelete.each {
            assert copyScriptOtherUserContent.contains("#rm -rf ${it}")
        }

        File copyScript = scriptFolder.resolve("${script}.sh").toFile()
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' \\\n      '${lsdfFilesService.getFileViewByPidPath(it)}'")
            assert it.getComment().comment == "Attention: Datafile swapped!"
        }
    }

    @Test
    void test_moveIndividual() {
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        Project newProject = DomainFactory.createProject(realm: bamFile.project.realm)
        String scriptName = "TEST-MOVE-INDIVIDUAL"
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        List<String> dataFilePaths = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
            dataFilePaths.add(lsdfFilesService.getFileFinalPath(it))
        }
        File missedFile = bamFile.finalMd5sumFile
        File unexpectedFile = new File(bamFile.baseDirectory, 'notExpectedFile.txt')

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        assert missedFile.delete()
        assert unexpectedFile.createNewFile()

        List<File> roddyFilesToDelete = createRoddyFileListToDelete(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = temporaryFolder.newFolder("files").toPath()

        StringBuilder outputLog = new StringBuilder()

        SpringSecurityUtils.doWithAuth(ADMIN) {
            dataSwapService.moveIndividual(
                    bamFile.project.name,
                    newProject.name,
                    bamFile.individual.pid,
                    bamFile.individual.pid,
                    [(bamFile.sampleType.name): ""],
                    ['DataFileFileName_R1.gz': '', 'DataFileFileName_R2.gz': ''],
                    scriptName,
                    outputLog,
                    false,
                    scriptFolder,
            )
        }
        String output = outputLog
        assert output.contains("${DataSwapService.MISSING_FILES_TEXT}\n    ${missedFile}")
        assert output.contains("${DataSwapService.EXCESS_FILES_TEXT}\n    ${unexpectedFile}")

        assert scriptFolder.toFile().listFiles().length != 0

        File alignmentScript = scriptFolder.resolve("restartAli_${scriptName}.groovy").toFile()
        assert alignmentScript.exists()

        File copyScriptOtherUser = scriptFolder.resolve("${scriptName}-otherUser.sh").toFile()
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        roddyFilesToDelete.each {
            assert copyScriptOtherUserContent.contains("#rm -rf ${it}")
        }

        File copyScript = scriptFolder.resolve("${scriptName}.sh").toFile()
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileFinalPath(it)).getParent()}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}' \\\n   '${lsdfFilesService.getFileFinalPath(it)}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}.md5sum' \\\n     '${lsdfFilesService.getFileFinalPath(it)}.md5sum'")
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' \\\n      '${lsdfFilesService.getFileViewByPidPath(it)}'")
            assert it.getComment().comment == "Attention: Datafile swapped!"
        }
    }

    @Test
    void test_changeMetadataEntry() {
        setupData()
        Sample sample = DomainFactory.createSample()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(sample: sample)
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        MetaDataKey metaDataKey = DomainFactory.createMetaDataKey()
        String newValue = "NEW"
        MetaDataEntry metaDataEntry = DomainFactory.createMetaDataEntry(key: metaDataKey, dataFile: dataFile)

        dataSwapService.changeMetadataEntry(sample, metaDataKey.name, metaDataEntry.value, newValue)

        assert metaDataEntry.value == newValue
    }

    @Test
    void test_renameSampleIdentifiers() {
        setupData()

        Sample sample = DomainFactory.createSample()
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier(sample: sample)
        String sampleIdentifierName = sampleIdentifier.name
        SeqTrack seqTrack = DomainFactory.createSeqTrack(sample: sample)
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        MetaDataKey metaDataKey = DomainFactory.createMetaDataKey(name: "SAMPLE_ID")
        DomainFactory.createMetaDataEntry(key: metaDataKey, dataFile: dataFile)

        dataSwapService.renameSampleIdentifiers(sample, new StringBuilder())

        assert sampleIdentifierName != sampleIdentifier.name
    }

    @Test
    void test_getSingleSampleForIndividualAndSampleType_singleSample() {
        setupData()
        Individual individual = DomainFactory.createIndividual()
        SampleType sampleType = DomainFactory.createSampleType()
        Sample sample = DomainFactory.createSample(individual: individual, sampleType: sampleType)

        assert sample == dataSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
    }

    @Test
    void test_getSingleSampleForIndividualAndSampleType_noSample() {
        setupData()
        Individual individual = DomainFactory.createIndividual()
        SampleType sampleType = DomainFactory.createSampleType()

        TestCase.shouldFail(IllegalArgumentException) {
            dataSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
        }
    }

    @Test
    void test_getAndShowSeqTracksForSample() {
        setupData()
        Sample sample = DomainFactory.createSample()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(sample: sample)

        assert [seqTrack] == dataSwapService.getAndShowSeqTracksForSample(sample, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks_noDataFile_shouldFail() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        List<SeqTrack> seqTracks = [seqTrack]
        Map<String, String> dataFileMap = [:]

        TestCase.shouldFail(IllegalArgumentException) {
            dataSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
        }
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [dataFile] == dataSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowAlignmentDataFilesForSeqTracks() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [] == dataSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())

        AlignmentLog alignmentLog = DomainFactory.createAlignmentLog(seqTrack: seqTrack)
        DataFile dataFile2 = DomainFactory.createDataFile(alignmentLog: alignmentLog)
        dataFileMap = [(dataFile2.fileName): ""]
        assert [dataFile2] == dataSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void test_collectFileNamesOfDataFiles() {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile(used: false)

        assert [(dataFile): [directFileName: lsdfFilesService.getFileFinalPath(dataFile), vbpFileName: lsdfFilesService.getFileViewByPidPath(dataFile)]] ==
                dataSwapService.collectFileNamesOfDataFiles([dataFile])
    }

    @Test
    void testDeleteFastQCInformationFromDataFile() throws Exception {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(dataFile: dataFile)

        dataSwapService.deleteFastQCInformationFromDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
    }

    @Test
    void testDeleteMetaDataEntryForDataFile() throws Exception {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        MetaDataEntry metaDataEntry = DomainFactory.createMetaDataEntry(dataFile: dataFile)

        dataSwapService.deleteMetaDataEntryForDataFile(dataFile)

        assert !MetaDataEntry.get(metaDataEntry.id)
    }

    @Test
    void testDeleteConsistencyStatusInformationForDataFile() throws Exception {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        ConsistencyStatus consistencyStatus = DomainFactory.createConsistencyStatus(dataFile: dataFile)

        dataSwapService.deleteConsistencyStatusInformationForDataFile(dataFile)

        assert !ConsistencyStatus.get(consistencyStatus.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedBamFile() throws Exception {
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.createProcessedBamFile()

        QualityAssessmentPass qualityAssessmentPass = DomainFactory.createQualityAssessmentPass(processedBamFile: abstractBamFile)
        ChromosomeQualityAssessment chromosomeQualityAssessment = DomainFactory.createChromosomeQualityAssessment(qualityAssessmentPass: qualityAssessmentPass, referenceLength: 0)
        OverallQualityAssessment overallQualityAssessment = DomainFactory.createOverallQualityAssessment(qualityAssessmentPass: qualityAssessmentPass, referenceLength: 0)

        dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessment.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessment.get(overallQualityAssessment.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedMergedBamFile() throws Exception {
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.createProcessedMergedBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = DomainFactory.createQualityAssessmentMergedPass(abstractMergedBamFile: abstractBamFile)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessment = DomainFactory.createChromosomeQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentPass, referenceLength: 0)
        OverallQualityAssessmentMerged overallQualityAssessment = DomainFactory.createOverallQualityAssessmentMerged(qualityAssessmentMergedPass: qualityAssessmentPass, referenceLength: 0)
        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = DomainFactory.createPicardMarkDuplicatesMetrics(abstractBamFile: abstractBamFile)

        dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessmentMerged.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessmentMerged.get(overallQualityAssessment.id)
        assert !PicardMarkDuplicatesMetrics.get(picardMarkDuplicatesMetrics.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_RoddyBamFile() throws Exception {
        setupData()
        AbstractBamFile abstractBamFile = DomainFactory.createRoddyBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = DomainFactory.createQualityAssessmentMergedPass(abstractMergedBamFile: abstractBamFile)
        RoddyLibraryQa roddyLibraryQa = DomainFactory.createRoddyLibraryQa(qualityAssessmentMergedPass: qualityAssessmentPass,
                genomeWithoutNCoverageQcBases: 0, referenceLength: 0)
        RoddyMergedBamQa roddyMergedBamQa = DomainFactory.createRoddyMergedBamQa(qualityAssessmentMergedPass: qualityAssessmentPass,
                genomeWithoutNCoverageQcBases: 0, referenceLength: 0)
        RoddySingleLaneQa roddySingleLaneQa = DomainFactory.createRoddySingleLaneQa(seqTrack: abstractBamFile.seqTracks.iterator().next(),
                qualityAssessmentMergedPass: qualityAssessmentPass, genomeWithoutNCoverageQcBases: 0, referenceLength: 0)

        dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !RoddyLibraryQa.get(roddyLibraryQa.id)
        assert !RoddyMergedBamQa.get(roddyMergedBamQa.id)
        assert !RoddySingleLaneQa.get(roddySingleLaneQa.id)
    }

    @Test
    void testDeleteQualityAssessmentInfoForAbstractBamFile_null() throws Exception {
        setupData()
        AbstractBamFile abstractBamFile = null

        final shouldFail = new GroovyTestCase().&shouldFail
        String message = shouldFail RuntimeException, {
            dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)
        }
        assert message == "The input AbstractBamFile is null"
    }

    @Test
    void testDeleteMergingRelatedConnectionsOfBamFile() throws Exception {
        setupData()
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                pipeline: DomainFactory.createDefaultOtpPipeline()
        ])
        MergingSet mergingSet = DomainFactory.createMergingSet(mergingWorkPackage: mergingWorkPackage)
        ProcessedBamFile processedBamFile = DomainFactory.createProcessedBamFile(mergingWorkPackage).save(flush: true)
        MergingPass mergingPass = DomainFactory.createMergingPass(mergingSet: mergingSet)
        MergingSetAssignment mergingSetAssignment = DomainFactory.createMergingSetAssignment(bamFile: processedBamFile, mergingSet: mergingSet)
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(workPackage: mergingWorkPackage, mergingPass: mergingPass)

        dataSwapService.deleteMergingRelatedConnectionsOfBamFile(processedBamFile)

        assert !MergingPass.get(mergingPass.id)
        assert !MergingSet.get(mergingSet.id)
        assert !MergingSetAssignment.get(mergingSetAssignment.id)
        assert !ProcessedMergedBamFile.get(bamFile.id)
    }

    @Test
    void testDeleteDataFile() throws Exception {
        setupData()
        DataFile dataFile = DomainFactory.createDataFile()
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile(dataFile: dataFile)

        DomainFactory.createMetaDataEntry(dataFile: dataFile)

        DomainFactory.createConsistencyStatus(dataFile: dataFile)

        dataSwapService.deleteDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
    }

    @Test
    void testDeleteConnectionFromSeqTrackRepresentingABamFile() throws Exception {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        AlignmentLog alignmentLog = DomainFactory.createAlignmentLog(seqTrack: seqTrack)
        DataFile dataFile = DomainFactory.createDataFile(alignmentLog: alignmentLog)

        dataSwapService.deleteConnectionFromSeqTrackRepresentingABamFile(seqTrack)

        assert !AlignmentLog.get(alignmentLog.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_ProcessedBamFile() throws Exception {
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

        dataSwapService.deleteAllProcessingInformationAndResultOfOneSeqTrack(alignmentPass.seqTrack)

        assert !ProcessedSaiFile.get(processedSaiFile.id)
        assert !ProcessedBamFile.get(processedMergedBamFile.id)
        assert !AlignmentPass.get(alignmentPass.id)
    }

    @Test
    void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_RoddyBamFile() throws Exception {
        setupData()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
        roddyBamFile.workPackage.save(flush: true)

        dataSwapService.deleteAllProcessingInformationAndResultOfOneSeqTrack(roddyBamFile.seqTracks.iterator().next())

        assert !RoddyBamFile.get(roddyBamFile.id)
        assert !MergingWorkPackage.get(roddyBamFile.workPackage.id)
    }

    @Test
    void testDeleteSeqScanAndCorrespondingInformation() throws Exception {
        setupData()
        SeqScan seqScan = DomainFactory.createSeqScan()
        MergingLog mergingLog = DomainFactory.createMergingLog(seqScan: seqScan)
        MergedAlignmentDataFile mergedAlignmentDataFile = DomainFactory.createMergedAlignmentDataFile(mergingLog: mergingLog)

        dataSwapService.deleteSeqScanAndCorrespondingInformation(seqScan)

        assert !MergingLog.get(mergingLog.id)
        assert !MergedAlignmentDataFile.get(mergedAlignmentDataFile.id)
        assert !SeqScan.get(seqScan.id)
    }

    @Test
    void testDeleteSeqTrack() throws Exception {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        MergingAssignment mergingAssignment = DomainFactory.createMergingAssignment(seqTrack: seqTrack)
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)

        dataSwapService.deleteSeqTrack(seqTrack)

        assert !SeqTrack.get(seqTrack.id)
        assert !MergingAssignment.get(mergingAssignment.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testDeleteSeqTrack_seqTrackIsOnlyLinked() throws Exception {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(linkedExternally: true)
        DomainFactory.createMergingAssignment(seqTrack: seqTrack)
        DomainFactory.createDataFile(seqTrack: seqTrack)

        TestCase.shouldFailWithMessageContaining(AssertionError, "seqTracks only linked") {
            dataSwapService.deleteSeqTrack(seqTrack)
        }
    }

    @Test
    void testDeleteRun() throws Exception {
        setupData()
        StringBuilder outputStringBuilder = new StringBuilder()
        Run run = DomainFactory.createRun()
        DataFile dataFile = DomainFactory.createDataFile(run: run)

        dataSwapService.deleteRun(run, outputStringBuilder)

        assert !Run.get(run.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testDeleteRunByName() throws Exception {
        setupData()
        StringBuilder outputStringBuilder = new StringBuilder()
        Run run = DomainFactory.createRun()
        DataFile dataFile = DomainFactory.createDataFile(run: run)

        dataSwapService.deleteRunByName(run.name, outputStringBuilder)

        assert !Run.get(run.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    void testThrowExceptionInCaseOfExternalMergedBamFileIsAttached() throws Exception {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createExternallyProcessedMergedBamFile(
                workPackage: DomainFactory.createExternalMergingWorkPackage(
                        sample: seqTrack.sample,
                        seqType: seqTrack.seqType,
                )
        ).save(flush: true)

        final shouldFail = new GroovyTestCase().&shouldFail
        shouldFail AssertionError, {
            dataSwapService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
        }
    }

    @Test
    void testThrowExceptionInCaseOfSeqTracksAreOnlyLinked() throws Exception {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(linkedExternally: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "seqTracks only linked") {
            dataSwapService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_EmptyProject() {
        setupData()
        Project project = DomainFactory.createProject()

        TestCase.shouldFail(AssertionError) {
            dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder)
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesMissing() {
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project

        TestCase.shouldFail(FileNotFoundException) {
            dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder)
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesAvailable() {
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()

        dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder)
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesLinked() {
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        markFilesAsLinked(SeqTrack.list())

        TestCase.shouldFail(FileNotFoundException) {
            dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder)
        }

    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesLinked_Verified() {
        setupData()
        Project project = deleteProcessingFilesOfProject_NoProcessedData_SetupWithFiles()
        markFilesAsLinked(SeqTrack.list())

        dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder, true)
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesWithdrawn() {
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project
        markFilesAsWithdrawn([seqTrack])

        TestCase.shouldFail(FileNotFoundException) {
            dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder, true)
        }
    }

    @Test
    void testDeleteProcessingFilesOfProject_NoProcessedData_FastqFilesWithdrawn_IgnoreWithdrawn() {
        setupData()
        SeqTrack seqTrack = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        Project project = seqTrack.project
        markFilesAsWithdrawn([seqTrack])

        dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder, true, true)
    }

    @Test
    void testDeleteProcessingFileSOfProject_NoProcessedData_FastqFilesAvailalbe_explicitSeqTrack() {
        setupData()
        SeqTrack st = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([st])

        assert [st] == dataSwapService.deleteProcessingFilesOfProject(st.project.name, outputFolder, true, true, [st])
    }

    @Test
    void testDeleteProcessingFileSOfProject_NoProcessedData_FastqFilesAvailalbe_explicitSeqTrackDifferentProject_ShouldFail() {
        setupData()
        SeqTrack st = deleteProcessingFilesOfProject_NoProcessedData_Setup()
        createFastqFiles([st])

        Project project = DomainFactory.createProject()

        TestCase.shouldFail(AssertionError) {
            assert [st] == dataSwapService.deleteProcessingFilesOfProject(project.name, outputFolder, true, true, [st])
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

        dataSwapService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

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

        dataSwapService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)

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

        dataSwapService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        assert outputFile.text.contains(finalBamFile.path)

        deleteProcessingFilesOfProject_RBF_Validation()
    }

    @Test
    void testDeleteProcessingFilesOfProject_RBF_notVerified() {
        setupData()
        RoddyBamFile bamFile = deleteProcessingFilesOfProject_RBF_Setup()

        File finalBamFile = new File(AbstractMergedBamFileService.destinationDirectory(bamFile))
        Path outputFile = outputFolder.resolve("Delete_${bamFile.project.name}.sh")

        dataSwapService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)

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

        dataSwapService.deleteProcessingFilesOfProject(snvCallingInstance.project.name, outputFolder, true)

        deleteProcessingFilesOfProject_RBF_SNV_Validation(snvCallingInstance)
    }

    @Test
    void testDeleteProcessingFilesOfProject_RBF_SNV_notVerified() {
        setupData()
        AbstractSnvCallingInstance snvCallingInstance = deleteProcessingFilesOfProject_RBF_SNV_Setup()

        dataSwapService.deleteProcessingFilesOfProject(snvCallingInstance.project.name, outputFolder)

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
            dataSwapService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder)
        })
    }

    @Test
    void testDeleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified() {
        setupData()
        ExternallyProcessedMergedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        dataSwapService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        deleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified_Validation(bamFile)
    }

    @Test
    void testDeleteProcessingFilesOfProject_ExternalBamFilesAttached_nonMergedSeqTrackExists_Verified() {
        setupData()
        ExternallyProcessedMergedBamFile bamFile = deleteProcessingFilesOfProject_ExternalBamFilesAttached_Setup()

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles([sample: bamFile.sample, seqType: bamFile.seqType])
        createFastqFiles([seqTrack])

        dataSwapService.deleteProcessingFilesOfProject(bamFile.project.name, outputFolder, true)

        deleteProcessingFilesOfProject_ExternalBamFilesAttached_Verified_Validation(bamFile)
    }

    private List<File> createRoddyFileListToDelete(RoddyBamFile roddyBamFile) {
        [
                roddyBamFile.workExecutionDirectories,
                roddyBamFile.workMergedQADirectory,
                roddyBamFile.workSingleLaneQADirectories.values(),
        ].flatten()*.absolutePath
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

        List<String> allFilesToDeleteCmd = dataSwapService.deleteIndividual(pid)
        String allFilesToDeleteCmdConcatenated = allFilesToDeleteCmd[0] + allFilesToDeleteCmd[1]

        assert !Individual.findByPid(pid)

        filesToDelete.flatten().each { File file ->
            assert allFilesToDeleteCmdConcatenated.contains(file.absolutePath)
        }
    }
}
