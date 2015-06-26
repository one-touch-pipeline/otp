package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired


class ExecutePanCanJobTests {

    @Autowired
    ExecutePanCanJob executePanCanJob

    Realm dataProcessing
    Realm dataManagement
    RoddyBamFile roddyBamFile
    SeqTrack seqTrack
    File referenceGenomeDir
    File referenceGenomeFile
    File configFile
    String roddyPath
    String roddyVersion
    String roddyBaseConfigsPath
    String roddyApplicationIni
    String chromosomeSizeFiles
    String processingRootPath
    File dataFile1File
    File dataFile2File

    @Before
    void setUp() {
        executePanCanJob.lsdfFilesService.metaClass.getFileFinalPath = { DataFile dataFile ->
            "${processingRootPath}/${dataFile.fileName}"
        }

        DomainFactory.createAlignableSeqTypes()

        roddyBamFile = DomainFactory.createRoddyBamFile([md5sum: null, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED])
        dataProcessing = Realm.build(name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_PROCESSING)
        dataManagement = Realm.build(name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_MANAGEMENT)

        processingRootPath = dataProcessing.processingRootPath

        DomainFactory.createRoddyProcessingOptions(new File(processingRootPath))

        roddyPath = ProcessingOption.findByName("roddyPath").value
        roddyVersion = ProcessingOption.findByName("roddyVersion").value
        roddyBaseConfigsPath = ProcessingOption.findByName("roddyBaseConfigsPath").value
        new File(roddyBaseConfigsPath).mkdirs()
        roddyApplicationIni = ProcessingOption.findByName("roddyApplicationIni").value
        CreateFileHelper.createFile(new File(roddyApplicationIni))

        seqTrack = roddyBamFile.seqTracks.iterator()[0]
        DataFile dataFile1 = DataFile.findBySeqTrack(seqTrack)
        dataFile1File = new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile1))
        createFileAndAddFileSize(dataFile1File, dataFile1)
        DataFile dataFile2 =  DomainFactory.buildSequenceDataFile([seqTrack: seqTrack, fileName: "DataFileFileName_R2.gz"])
        assert dataFile2.save(flush: true)
        dataFile2File = new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile2))
        createFileAndAddFileSize(dataFile2File, dataFile2)

        referenceGenomeDir = new File("${processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()
        referenceGenomeFile = new File(referenceGenomeDir, "${roddyBamFile.referenceGenome.fileNamePrefix}.fa")
        CreateFileHelper.createFile(referenceGenomeFile)
        configFile = new File(roddyBamFile.config.configFilePath)
        CreateFileHelper.createFile(configFile)

        chromosomeSizeFiles = executePanCanJob.referenceGenomeService.pathToChromosomeSizeFilesPerReference(roddyBamFile.project, roddyBamFile.referenceGenome)
        new File(chromosomeSizeFiles).mkdirs()

        roddyBamFile.workPackage.metaClass.findMergeableSeqTracks = { -> SeqTrack.list() }

    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecutePanCanJob, executePanCanJob)
        TestCase.removeMetaClass(ReferenceGenomeService, executePanCanJob.referenceGenomeService)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executePanCanJob.executeRoddyCommandService)
        TestCase.removeMetaClass(LsdfFilesService, executePanCanJob.lsdfFilesService)

        assert new File(dataManagement.rootPath).deleteDir()
        assert new File(dataManagement.processingRootPath).deleteDir()
        assert new File(roddyBaseConfigsPath).deleteDir()

        processingRootPath = null
        roddyPath = null
        roddyVersion = null
        roddyBaseConfigsPath = null
        roddyApplicationIni = null
        chromosomeSizeFiles = null
        dataProcessing = null
        dataManagement = null
        seqTrack = null
        roddyBamFile = null
        configFile = null
        dataFile1File = null
        dataFile2File = null
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RoddyBamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(null, dataManagement)
        }.contains("roddyResult must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RealmIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, null)
        }.contains("realm must not be null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_PathToReferenceGenomeIsNull_ShouldFail() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        DataFile dataFile = DataFile.findBySeqTrack(seqTrack)
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile)), dataFile)

        DataFile dataFile2 =  DomainFactory.buildSequenceDataFile([seqTrack: seqTrack, fileName: "DataFileFileName_R2.gz"])
        assert dataFile2.save(flush: true)
        dataFile2File = new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile2))
        createFileAndAddFileSize(dataFile2File, dataFile2)

        executePanCanJob.referenceGenomeService.metaClass.fastaFilePath = { Project project, ReferenceGenome referenceGenome ->
            return null
        }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains("Path to the reference genome file is null")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_PathToChromosomeSizeFilesIsNull_ShouldFail() {
        executePanCanJob.referenceGenomeService.metaClass.pathToChromosomeSizeFilesPerReference = { Project project, ReferenceGenome referenceGenome ->
            return null
        }

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains("Path to the chromosome size files is null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsButIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"

        assert roddyBamFile.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        SeqTrack seqTrack = roddyBamFile2.seqTracks.iterator()[0]
        DataFile dataFile = DataFile.findBySeqTrack(seqTrack)
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile)), dataFile)

        DataFile dataFile2 =  DomainFactory.buildSequenceDataFile([seqTrack: seqTrack, fileName: "DataFileFileName_R2.gz"])
        assert dataFile2.save(flush: true)
        dataFile2File = new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile2))
        createFileAndAddFileSize(dataFile2File, dataFile2)

        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile2, dataManagement)
        }.contains(roddyBamFile.finalBamFile.path)

    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_DataFilesAreNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        DataFile.findAllBySeqTrack(seqTrack).each {
            assert new File(executePanCanJob.lsdfFilesService.getFileFinalPath(it)).delete()
        }
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains("${processingRootPath}/DataFileFileName")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ConfigFileIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        assert configFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(roddyBamFile.config.configFilePath)
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ReferenceGenomeIsNotInFileSystem_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        assert referenceGenomeFile.delete()
        assert TestCase.shouldFail(RuntimeException) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(referenceGenomeFile.path)

    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ChromosomeSizeDirDoesNotExist_ShouldFail() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }
        File chromosomeSizeFile = new File(chromosomeSizeFiles)
        assert chromosomeSizeFile.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(chromosomeSizeFile.path)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_NoBaseBamFileExists() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        String seqTracks = SeqTrack.list().join(";")

        String expectedCmd =  """\
cd ${roddyPath} \
&& sudo -u OtherUnixUser roddy.sh rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.externalScriptVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${roddyBamFile.tmpRoddyDirectory} \
--cvalues=fastq_list:${dataFile1File};${dataFile2File},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile.path},\
CHROM_SIZES_FILE:${chromosomeSizeFiles},\
possibleControlSampleNamePrefixes:(${roddyBamFile.sampleType.dirName})\
"""

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlready() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "abcdefabcdefabcdefabcdefabcdefab"
        assert roddyBamFile.save(flush: true)

        CreateFileHelper.createFile(roddyBamFile.finalBamFile)
        roddyBamFile.fileSize = roddyBamFile.finalBamFile.length()
        assert roddyBamFile.save(flush: true)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)

        SeqTrack seqTrack1 = roddyBamFile2.seqTracks.iterator()[0]
        DataFile dataFile = DataFile.findBySeqTrack(seqTrack1)
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile)), dataFile)

        DataFile dataFile2 = DomainFactory.buildSequenceDataFile([seqTrack: seqTrack1, fileName: "DataFileFileName_R2.gz"]).save(flush: true)
        createFileAndAddFileSize(new File(executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile2)), dataFile2)

        String expectedCmd = """\
cd ${roddyPath} \
&& sudo -u OtherUnixUser roddy.sh rerun ${roddyBamFile2.workflow.name}_${roddyBamFile2.config.externalScriptVersion}.config@WGS \
${roddyBamFile2.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile2.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile2.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${roddyBamFile2.tmpRoddyDirectory} \
--cvalues=fastq_list:${executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile)};${executePanCanJob.lsdfFilesService.getFileFinalPath(dataFile2)},\
bam:${roddyBamFile.finalBamFile},\
REFERENCE_GENOME:${referenceGenomeFile},\
INDEX_PREFIX:${referenceGenomeFile},\
CHROM_SIZES_FILE:${chromosomeSizeFiles},\
possibleControlSampleNamePrefixes:(${roddyBamFile.sampleType.dirName})\
"""

        String actualCmd =  executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile2, dataManagement)

        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_WhenReadNumberOrderIsWrong_ShouldBeOk() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        SeqTrack.list().join(";")

        String expectedCmd =  """\
cd ${roddyPath} \
&& sudo -u OtherUnixUser roddy.sh rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.externalScriptVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${roddyBamFile.tmpRoddyDirectory} \
--cvalues=fastq_list:/tmp/otp-unit-test/processing/DataFileFileName_R1.gz;/tmp/otp-unit-test/processing/DataFileFileName_R2.gz,\
REFERENCE_GENOME:/tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/fileNamePrefix.fa,\
INDEX_PREFIX:/tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/fileNamePrefix.fa,\
CHROM_SIZES_FILE:/tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/stats/,\
possibleControlSampleNamePrefixes:(${roddyBamFile.sampleType.dirName})\
"""

        List<DataFile> dataFiles = []

        roddyBamFile.seqTracks.each { seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).each { DataFile dataFile ->
                dataFiles << dataFile
            }
        }

        dataFiles.first().fileName = "DataFileFileName_R2.gz"
        dataFiles.last().fileName = "DataFileFileName_R1.gz"

        assert roddyBamFile.save(flush: true)

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }


    @Test
    void testValidate_RoddyBamFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(null)
        }.contains("Input roddyResultObject must not be null")
    }

    @Test
    void testValidate_BamDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyBamFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyBamFile.path)
    }

    @Test
    void testValidate_BaiDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyBaiFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyBaiFile.path)
    }

    @Test
    void testValidate_Md5sumDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyMd5sumFile.delete()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyMd5sumFile.path)
    }

    @Test
    void testValidate_MergedQaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyQADirectory.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyQADirectory.path)
    }

    @Test
    void testValidate_SingleLaneQaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddySingleLaneQADirectories.values()*.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(RoddyBamFile.RUN_PREFIX)
    }

    @Test
    void testValidate_ExecutionStoreDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyExecutionStoreDirectory.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }.contains(roddyBamFile.tmpRoddyExecutionStoreDirectory.path)
    }

    @Test
    void testValidate_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        executePanCanJob.validate(roddyBamFile)
    }

    void createFileAndAddFileSize(File file, DataFile dataFile) {
        CreateFileHelper.createFile(file)
        dataFile.fileSize = file.length()
        assert dataFile.save(flush: true)
    }
}
