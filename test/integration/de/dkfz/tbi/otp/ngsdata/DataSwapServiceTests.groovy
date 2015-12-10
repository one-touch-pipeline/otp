package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.fileSystemConsistency.*
import de.dkfz.tbi.otp.ngsqc.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static de.dkfz.tbi.otp.ngsdata.Realm.OperationType.DATA_MANAGEMENT
import static de.dkfz.tbi.otp.ngsdata.Realm.OperationType.DATA_PROCESSING

class DataSwapServiceTests extends GroovyScriptAwareTestCase {
    DataSwapService dataSwapService
    LsdfFilesService lsdfFilesService


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        createUserAndRoles()
    }



    @Test
    void test_moveSample() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY]
        ])
        String script = "TEST-MOVE_SAMPLE"
        Individual individual = Individual.build(project: bamFile.project)
        Realm realm = Realm.build(name: bamFile.project.realmName, operationType: DATA_MANAGEMENT, rootPath: temporaryFolder.newFolder("mgmt"))
        Realm.build(name: bamFile.project.realmName, operationType: DATA_PROCESSING, rootPath: temporaryFolder.newFolder("proc"))
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
        }

        String fileName = "FILE_NAME"
        String dataFileName1 = 'DataFileFileName_R1.gz'
        String dataFileName2 = 'DataFileFileName_R2.gz'

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(realm, bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, bamFile)
        List<File> roddyFilesToDelete = createRoddyFileListToDelete(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        File scriptFolder = temporaryFolder.newFolder("files")

        SpringSecurityUtils.doWithAuth("admin") {
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
                    scriptFolder.absolutePath,
            )
        }


        assert scriptFolder.listFiles().length != 0

        File alignmentScript = new File(scriptFolder, "restartAli_${script}.groovy")
        assert alignmentScript.exists()
        assert alignmentScript.text.contains("ctx.seqTrackService.decideAndPrepareForAlignment(SeqTrack.get(${bamFile.seqTracks.iterator().next().id}))")

        File copyScriptOtherUser = new File(scriptFolder, "${script}-OtherUnixUser.sh")
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        roddyFilesToDelete.each {
            assert copyScriptOtherUserContent.contains("#rm -rf ${it}")
        }

        File copyScript = new File(scriptFolder, "${script}.sh")
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' '${lsdfFilesService.getFileViewByPidPath(it)}'")
        }

        assert bamFile.individual == individual
    }


    @Test
    void test_moveIndividual() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY]
        ])
        Project newProject = Project.build(realmName: bamFile.project.realmName)
        String script = "TEST-MOVE-INDIVIDUAL"
        Realm realm = Realm.build(name: bamFile.project.realmName, operationType: DATA_MANAGEMENT, rootPath: temporaryFolder.newFolder("mgmt"))
        Realm.build(name: bamFile.project.realmName, operationType: DATA_PROCESSING, rootPath: temporaryFolder.newFolder("proc"))
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

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(realm, bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, bamFile)
        assert missedFile.delete()
        assert unexpectedFile.createNewFile()

        List<File> roddyFilesToDelete = createRoddyFileListToDelete(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        String fileName = "FILE_NAME"

        File scriptFolder = temporaryFolder.newFolder("files")

        StringBuilder outputStringBuilder = new StringBuilder()

        SpringSecurityUtils.doWithAuth("admin") {
            dataSwapService.moveIndividual(
                    bamFile.project.name,
                    newProject.name,
                    bamFile.individual.pid,
                    bamFile.individual.pid,
                    [(bamFile.sampleType.name): ""],
                    ['DataFileFileName_R1.gz': '', 'DataFileFileName_R2.gz': ''],
                    script,
                    outputStringBuilder,
                    false,
                    scriptFolder.absolutePath,
            )
        }
        String output = outputStringBuilder.toString()
        assert output.contains("${DataSwapService.MISSING_FILES_TEXT}\n    ${missedFile}")
        assert output.contains("${DataSwapService.EXCESS_FILES_TEXT}\n    ${unexpectedFile}")

        assert scriptFolder.listFiles().length != 0

        File alignmentScript = new File(scriptFolder, "restartAli_${script}.groovy")
        assert alignmentScript.exists()

        File copyScriptOtherUser = new File(scriptFolder, "${script}-OtherUnixUser.sh")
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        roddyFilesToDelete.each {
            assert copyScriptOtherUserContent.contains("#rm -rf ${it}")
        }

        File copyScript = new File(scriptFolder, "${script}.sh")
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileFinalPath(it)).getParent()}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}' '${lsdfFilesService.getFileFinalPath(it)}'")
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' '${lsdfFilesService.getFileViewByPidPath(it)}'")
        }

        assert bamFile.project == newProject
    }



    @Test
    void test_changeMetadataEntry() {
        Sample sample = Sample.build()
        SeqTrack seqTrack = SeqTrack.build(sample: sample)
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        MetaDataKey metaDataKey = MetaDataKey.build()
        String newValue = "NEW"
        MetaDataEntry metaDataEntry = MetaDataEntry.build(key: metaDataKey, dataFile: dataFile)

        dataSwapService.changeMetadataEntry(sample, metaDataKey.name, metaDataEntry.value, newValue)

        assert metaDataEntry.value == newValue
    }

    @Test
    void test_changeSeqType_withClassChange() {
        SeqType wgs = DomainFactory.createSeqType(name: SeqTypeNames.WHOLE_GENOME.seqTypeName)
        SeqType exome = DomainFactory.createSeqType(name: SeqTypeNames.EXOME.seqTypeName)
        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqType: wgs)
        assert seqTrack.class == SeqTrack
        long seqTrackId = seqTrack.id

        SeqTrack returnedSeqTrack = dataSwapService.changeSeqType(seqTrack, exome)

        assert returnedSeqTrack.id == seqTrackId
        assert returnedSeqTrack.seqType.id == exome.id
        assert returnedSeqTrack.class == ExomeSeqTrack
    }

    @Test
    void test_renameSampleIdentifiers() {

        Sample sample = Sample.build()
        SampleIdentifier sampleIdentifier = SampleIdentifier.build(sample: sample)
        String sampleIdentifierName = sampleIdentifier.name
        SeqTrack seqTrack = SeqTrack.build(sample: sample)
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        MetaDataKey metaDataKey = MetaDataKey.build(name: "SAMPLE_ID")
        MetaDataEntry metaDataEntry = MetaDataEntry.build(key: metaDataKey, dataFile: dataFile)

        dataSwapService.renameSampleIdentifiers(sample, new StringBuilder())

        assert sampleIdentifierName != sampleIdentifier.name
    }


    @Test
    void test_getSingleSampleForIndividualAndSampleType_singleSample() {
        Individual individual = Individual.build()
        SampleType sampleType = SampleType.build()
        Sample sample = Sample.build(individual: individual, sampleType: sampleType)

        assert sample == dataSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
    }

    @Test
    void test_getSingleSampleForIndividualAndSampleType_noSample() {
        Individual individual = Individual.build()
        SampleType sampleType = SampleType.build()

        shouldFail IllegalArgumentException, {
            dataSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
        }
    }


    @Test
    void test_getAndShowSeqTracksForSample() {
        Sample sample = Sample.build()
        SeqTrack seqTrack = SeqTrack.build(sample: sample)

        assert [seqTrack] == dataSwapService.getAndShowSeqTracksForSample(sample, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks_noDataFile_shouldFail() {
        SeqTrack seqTrack = SeqTrack.build()
        List<SeqTrack> seqTracks = [seqTrack]
        Map<String, String> dataFileMap = [:]

        shouldFail IllegalArgumentException, {
            dataSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
        }
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks() {
        SeqTrack seqTrack = SeqTrack.build()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [dataFile] == dataSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowAlignmentDataFilesForSeqTracks() {
        SeqTrack seqTrack = SeqTrack.build()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [] == dataSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())

        AlignmentLog alignmentLog = AlignmentLog.build(seqTrack: seqTrack)
        DataFile dataFile2 = DataFile.build(alignmentLog: alignmentLog)
        assert [dataFile2] == dataSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void test_collectFileNamesOfDataFiles() {
        DataFile dataFile = DataFile.build()

        assert [(dataFile): [directFileName: lsdfFilesService.getFileFinalPath(dataFile), vbpFileName: lsdfFilesService.getFileViewByPidPath(dataFile)]] ==
                dataSwapService.collectFileNamesOfDataFiles([dataFile])
    }

    @Test
    public void testDeleteFastQCInformationFromDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        FastqcProcessedFile fastqcProcessedFile = FastqcProcessedFile.build(dataFile: dataFile)
        FastqcBasicStatistics fastqcBasicStatistics = FastqcBasicStatistics.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcModuleStatus fastqcModuleStatus = FastqcModuleStatus.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcOverrepresentedSequences fastqcOverrepresentedSequences = FastqcOverrepresentedSequences.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerBaseSequenceAnalysis fastqcPerBaseSequenceAnalysis = FastqcPerBaseSequenceAnalysis.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceGCContent fastqcPerSequenceGCContent = FastqcPerSequenceGCContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceQualityScores fastqcPerSequenceQualityScores = FastqcPerSequenceQualityScores.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcKmerContent fastqcKmerContent = FastqcKmerContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceDuplicationLevels fastqcSequenceDuplicationLevels = FastqcSequenceDuplicationLevels.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.build(fastqcProcessedFile: fastqcProcessedFile)

        dataSwapService.deleteFastQCInformationFromDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
        assert !FastqcBasicStatistics.get(fastqcBasicStatistics.id)
        assert !FastqcModuleStatus.get(fastqcModuleStatus.id)
        assert !FastqcOverrepresentedSequences.get(fastqcOverrepresentedSequences.id)
        assert !FastqcPerBaseSequenceAnalysis.get(fastqcPerBaseSequenceAnalysis.id)
        assert !FastqcPerSequenceGCContent.get(fastqcPerSequenceGCContent.id)
        assert !FastqcPerSequenceQualityScores.get(fastqcPerSequenceQualityScores.id)
        assert !FastqcKmerContent.get(fastqcKmerContent.id)
        assert !FastqcSequenceDuplicationLevels.get(fastqcSequenceDuplicationLevels.id)
        assert !FastqcSequenceLengthDistribution.get(fastqcSequenceLengthDistribution.id)
    }

    @Test
    public void testDeleteMetaDataEntryForDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        MetaDataEntry metaDataEntry = MetaDataEntry.build(dataFile: dataFile)

        dataSwapService.deleteMetaDataEntryForDataFile(dataFile)

        assert !MetaDataEntry.get(metaDataEntry.id)
    }

    @Test
    public void testDeleteConsistencyStatusInformationForDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        ConsistencyStatus consistencyStatus = ConsistencyStatus.build(dataFile: dataFile)

        dataSwapService.deleteConsistencyStatusInformationForDataFile(dataFile)

        assert !ConsistencyStatus.get(consistencyStatus.id)
    }


    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedBamFile() throws Exception {
        AbstractBamFile abstractBamFile = ProcessedBamFile.build()

        QualityAssessmentPass qualityAssessmentPass = QualityAssessmentPass.build(processedBamFile: abstractBamFile)
        ChromosomeQualityAssessment chromosomeQualityAssessment = ChromosomeQualityAssessment.build(qualityAssessmentPass: qualityAssessmentPass)
        OverallQualityAssessment overallQualityAssessment = OverallQualityAssessment.build(qualityAssessmentPass: qualityAssessmentPass)

        dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessment.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessment.get(overallQualityAssessment.id)
    }

    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_ProcessedMergedBamFile() throws Exception {
        AbstractBamFile abstractBamFile = DomainFactory.createProcessedMergedBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = QualityAssessmentMergedPass.build(abstractMergedBamFile: abstractBamFile)
        ChromosomeQualityAssessmentMerged chromosomeQualityAssessment = ChromosomeQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        OverallQualityAssessmentMerged overallQualityAssessment = OverallQualityAssessmentMerged.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        PicardMarkDuplicatesMetrics picardMarkDuplicatesMetrics = PicardMarkDuplicatesMetrics.build(abstractBamFile: abstractBamFile)

        dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessmentMerged.get(chromosomeQualityAssessment.id)
        assert !OverallQualityAssessmentMerged.get(overallQualityAssessment.id)
        assert !PicardMarkDuplicatesMetrics.get(picardMarkDuplicatesMetrics.id)
    }

    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_RoddyBamFile() throws Exception {
        AbstractBamFile abstractBamFile = DomainFactory.createRoddyBamFile()

        QualityAssessmentMergedPass qualityAssessmentPass = QualityAssessmentMergedPass.build(abstractMergedBamFile: abstractBamFile)
        RoddyQualityAssessment roddyQualityAssessment = RoddyQualityAssessment.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        RoddyMergedBamQa roddyMergedBamQa = RoddyMergedBamQa.build(qualityAssessmentMergedPass: qualityAssessmentPass)
        RoddySingleLaneQa roddySingleLaneQa = RoddySingleLaneQa.build(seqTrack: abstractBamFile.seqTracks.iterator().next(), qualityAssessmentMergedPass: qualityAssessmentPass)

        dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)

        assert !QualityAssessmentMergedPass.get(qualityAssessmentPass.id)
        assert !ChromosomeQualityAssessmentMerged.get(roddyQualityAssessment.id)
        assert !RoddyMergedBamQa.get(roddyMergedBamQa.id)
        assert !RoddySingleLaneQa.get(roddySingleLaneQa.id)
    }

    @Test
    public void testDeleteQualityAssessmentInfoForAbstractBamFile_null() throws Exception {
        AbstractBamFile abstractBamFile = null

        final shouldFail = new GroovyTestCase().&shouldFail
        String message = shouldFail RuntimeException, {
            dataSwapService.deleteQualityAssessmentInfoForAbstractBamFile(abstractBamFile)
        }
        assert message == "The input AbstractBamFile is null"
    }

    @Test
    public void testDeleteStudiesOfOneProject() throws Exception {
        Project project = Project.build()
        Study study = Study.build(project: project)
        StudySample studySample = StudySample.build(study: study)

        dataSwapService.deleteStudiesOfOneProject(project)

        assert !Study.get(study.id)
        assert !StudySample.get(studySample.id)
    }

    @Test
    public void testDeleteMutationsAndResultDataFilesOfOneIndividual() throws Exception {
        Individual individual = Individual.build()
        ResultsDataFile resultsDataFile = ResultsDataFile.build()
        Mutation mutation = Mutation.build(individual: individual, resultsDataFile: resultsDataFile)

        dataSwapService.deleteMutationsAndResultDataFilesOfOneIndividual(individual)

        assert !ResultsDataFile.get(resultsDataFile.id)
        assert !Mutation.get(mutation.id)
    }


    @Test
    public void testDeleteMergingRelatedConnectionsOfBamFile() throws Exception {
        MergingWorkPackage mergingWorkPackage = MergingWorkPackage.build(workflow: Workflow.build(name: Workflow.Name.DEFAULT_OTP))
        MergingSet mergingSet = MergingSet.build(mergingWorkPackage: mergingWorkPackage)
        ProcessedBamFile processedBamFile = DomainFactory.createProcessedBamFile(mergingWorkPackage).save(flush: true)
        MergingPass mergingPass = MergingPass.build(mergingSet: mergingSet)
        MergingSetAssignment mergingSetAssignment = MergingSetAssignment.build(bamFile: processedBamFile, mergingSet: mergingSet)
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.build(workPackage: mergingWorkPackage, mergingPass: mergingPass)

        dataSwapService.deleteMergingRelatedConnectionsOfBamFile(processedBamFile)

        assert !MergingPass.get(mergingPass.id)
        assert !MergingSet.get(mergingSet.id)
        assert !MergingSetAssignment.get(mergingSetAssignment.id)
        assert !ProcessedMergedBamFile.get(bamFile.id)
    }

    @Test
    public void testDeleteDataFile() throws Exception {
        DataFile dataFile = DataFile.build()
        FastqcProcessedFile fastqcProcessedFile = FastqcProcessedFile.build(dataFile: dataFile)
        FastqcBasicStatistics fastqcBasicStatistics = FastqcBasicStatistics.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcModuleStatus fastqcModuleStatus = FastqcModuleStatus.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcOverrepresentedSequences fastqcOverrepresentedSequences = FastqcOverrepresentedSequences.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerBaseSequenceAnalysis fastqcPerBaseSequenceAnalysis = FastqcPerBaseSequenceAnalysis.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceGCContent fastqcPerSequenceGCContent = FastqcPerSequenceGCContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcPerSequenceQualityScores fastqcPerSequenceQualityScores = FastqcPerSequenceQualityScores.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcKmerContent fastqcKmerContent = FastqcKmerContent.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceDuplicationLevels fastqcSequenceDuplicationLevels = FastqcSequenceDuplicationLevels.build(fastqcProcessedFile: fastqcProcessedFile)
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.build(fastqcProcessedFile: fastqcProcessedFile)

        MetaDataEntry metaDataEntry = MetaDataEntry.build(dataFile: dataFile)

        ConsistencyStatus consistencyStatus = ConsistencyStatus.build(dataFile: dataFile)

        dataSwapService.deleteDataFile(dataFile)

        assert !FastqcProcessedFile.get(fastqcProcessedFile.id)
        assert !FastqcBasicStatistics.get(fastqcBasicStatistics.id)
        assert !FastqcModuleStatus.get(fastqcModuleStatus.id)
        assert !FastqcOverrepresentedSequences.get(fastqcOverrepresentedSequences.id)
        assert !FastqcPerBaseSequenceAnalysis.get(fastqcPerBaseSequenceAnalysis.id)
        assert !FastqcPerSequenceGCContent.get(fastqcPerSequenceGCContent.id)
        assert !FastqcPerSequenceQualityScores.get(fastqcPerSequenceQualityScores.id)
        assert !FastqcKmerContent.get(fastqcKmerContent.id)
        assert !FastqcSequenceDuplicationLevels.get(fastqcSequenceDuplicationLevels.id)
        assert !FastqcSequenceLengthDistribution.get(fastqcSequenceLengthDistribution.id)
        assert !MetaDataEntry.get(metaDataEntry.id)
        assert !ConsistencyStatus.get(consistencyStatus.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    public void testDeleteConnectionFromSeqTrackRepresentingABamFile() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        AlignmentLog alignmentLog = AlignmentLog.build(seqTrack: seqTrack)
        DataFile dataFile = DataFile.build(alignmentLog: alignmentLog)

        dataSwapService.deleteConnectionFromSeqTrackRepresentingABamFile(seqTrack)

        assert !AlignmentLog.get(alignmentLog.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    public void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_ProcessedBamFile() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        ProcessedSaiFile processedSaiFile = ProcessedSaiFile.build(dataFile: dataFile)

        TestData testData = new TestData()
        AlignmentPass alignmentPass = testData.createAlignmentPass(seqTrack: seqTrack)
        MergingWorkPackage workPackage = alignmentPass.workPackage
        MergingSet mergingSet = MergingSet.build(mergingWorkPackage: workPackage)
        MergingPass mergingPass = MergingPass.build(mergingSet: mergingSet)

        AbstractMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.build(mergingPass: mergingPass, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS, workPackage: workPackage)
        workPackage.bamFileInProjectFolder = processedMergedBamFile
        workPackage.save(flush: true, failOnError: true)
        alignmentPass.save(flush: true, failOnError: true)

        dataSwapService.deleteAllProcessingInformationAndResultOfOneSeqTrack(alignmentPass.seqTrack)

        assert !ProcessedSaiFile.get(processedSaiFile.id)
        assert !ProcessedBamFile.get(processedMergedBamFile.id)
        assert !AlignmentPass.get(alignmentPass.id)
    }


    @Test
    public void testDeleteAllProcessingInformationAndResultOfOneSeqTrack_RoddyBamFile() throws Exception {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.workPackage.bamFileInProjectFolder = roddyBamFile
        roddyBamFile.workPackage.save(flush: true)

        dataSwapService.deleteAllProcessingInformationAndResultOfOneSeqTrack(roddyBamFile.seqTracks.iterator().next())

        assert !RoddyBamFile.get(roddyBamFile.id)
        assert !MergingWorkPackage.get(roddyBamFile.workPackage.id)
    }


    @Test
    public void testDeleteSeqScanAndCorrespondingInformation() throws Exception {
        SeqScan seqScan = SeqScan.build()
        MergingLog mergingLog = MergingLog.build(seqScan: seqScan)
        MergedAlignmentDataFile mergedAlignmentDataFile = MergedAlignmentDataFile.build(mergingLog: mergingLog)

        dataSwapService.deleteSeqScanAndCorrespondingInformation(seqScan)

        assert !MergingLog.get(mergingLog.id)
        assert !MergedAlignmentDataFile.get(mergedAlignmentDataFile.id)
        assert !SeqScan.get(seqScan.id)
    }

    @Test
    public void testDeleteSeqTrack() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        MergingAssignment mergingAssignment = MergingAssignment.build(seqTrack: seqTrack)
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)

        dataSwapService.deleteSeqTrack(seqTrack)

        assert !SeqTrack.get(seqTrack.id)
        assert !MergingAssignment.get(mergingAssignment.id)
        assert !DataFile.get(dataFile.id)
    }

    @Test
    public void testDeleteRunAndRunSegmentsWithoutDataOfOtherProjects() throws Exception {
        Run run = Run.build()
        Project project = Project.build()
        RunByProject runByProject = RunByProject.build(run: run, project: project)
        RunSegment runSegment = RunSegment.build(run: run)

        dataSwapService.deleteRunAndRunSegmentsWithoutDataOfOtherProjects(run, project)

        assert !RunByProject.get(runByProject.id)
        assert !Run.get(run.id)
        assert !RunSegment.get(runSegment.id)
    }

    @Test
    public void testDeleteRun() throws Exception {
        StringBuilder outputStringBuilder = new StringBuilder()
        Run run = Run.build()
        RunByProject runByProject = RunByProject.build(run: run)
        RunSegment runSegment = RunSegment.build(run: run)
        DataFile dataFile = DataFile.build(run: run)
        MetaDataFile metaDataFile = MetaDataFile.build(runSegment: runSegment)

        dataSwapService.deleteRun(run, outputStringBuilder)

        assert !Run.get(run.id)
        assert !RunByProject.get(runByProject.id)
        assert !RunSegment.get(runSegment.id)
        assert !DataFile.get(dataFile.id)
        assert !MetaDataFile.get(metaDataFile.id)
    }

    @Test
    public void testDeleteRunByName() throws Exception {
        StringBuilder outputStringBuilder = new StringBuilder()
        Run run = Run.build()
        RunByProject runByProject = RunByProject.build(run: run)
        RunSegment runSegment = RunSegment.build(run: run)
        DataFile dataFile = DataFile.build(run: run)
        MetaDataFile metaDataFile = MetaDataFile.build(runSegment: runSegment)

        dataSwapService.deleteRunByName(run.name, outputStringBuilder)

        assert !Run.get(run.id)
        assert !RunByProject.get(runByProject.id)
        assert !RunSegment.get(runSegment.id)
        assert !DataFile.get(dataFile.id)
        assert !MetaDataFile.get(metaDataFile.id)
    }

    @Test
    public void testThrowExceptionInCaseOfExternalMergedBamFileIsAttached() throws Exception {
        SeqTrack seqTrack = SeqTrack.build()
        FastqSet fastqSet = FastqSet.build(seqTracks: [seqTrack])
        ExternallyProcessedMergedBamFile.build(fastqSet: fastqSet, type: AbstractBamFile.BamType.RMDUP).save(flush: true)

        final shouldFail = new GroovyTestCase().&shouldFail
        shouldFail AssertionError, {
            dataSwapService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
        }
    }


    private List<File> createRoddyFileListToDelete(RoddyBamFile roddyBamFile) {
        [
                roddyBamFile.workQADirectory,
                roddyBamFile.workExecutionStoreDirectory
        ]*.absolutePath
    }
}
