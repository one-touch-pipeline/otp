package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
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
    File referenceGenomeDir
    File referenceGenomeFile
    String roddyPath
    String roddyVersion
    String roddyBaseConfigsPath
    String roddyApplicationIni

    @Before
    void setUp() {
        DomainFactory.createAlignableSeqTypes()
        DomainFactory.createRoddyProcessingOptions()

        roddyPath = ProcessingOption.findByName("roddyPath").value
        roddyVersion = ProcessingOption.findByName("roddyVersion").value
        roddyBaseConfigsPath = ProcessingOption.findByName("roddyBaseConfigsPath").value
        roddyApplicationIni = ProcessingOption.findByName("roddyApplicationIni").value

        roddyBamFile = DomainFactory.createRoddyBamFile([md5sum: null, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED])
        dataProcessing = Realm.build(name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_PROCESSING)
        dataManagement = Realm.build(name: roddyBamFile.project.realmName, operationType: Realm.OperationType.DATA_MANAGEMENT)

        referenceGenomeDir = new File("${dataProcessing.processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()
        referenceGenomeFile = new File(referenceGenomeDir, "${roddyBamFile.referenceGenome.fileNamePrefix}.fa")
        assert referenceGenomeFile.createNewFile()

        roddyBamFile.workPackage.metaClass.findMergeableSeqTracks = { -> SeqTrack.list() }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecutePanCanJob, executePanCanJob)
        TestCase.removeMetaClass(ReferenceGenomeService, executePanCanJob.referenceGenomeService)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executePanCanJob.executeRoddyCommandService)

        assert new File(dataManagement.rootPath).deleteDir()
        assert new File(dataManagement.processingRootPath).deleteDir()

        roddyPath = null
        roddyVersion = null
        roddyBaseConfigsPath = null
        roddyApplicationIni = null
        dataProcessing = null
        dataManagement = null
        roddyBamFile = null
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RoddyBamFileIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(null, dataManagement)
        }
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RealmIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, null)
        }
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_PathToReferenceGenomeIsNull_ShouldFail() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile()
        executePanCanJob.referenceGenomeService.metaClass.fastaFilePath = { Project project, ReferenceGenome referenceGenome ->
            return null
        }

        TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_PathToChromosomeSizeFilesIsNull_ShouldFail() {
        executePanCanJob.referenceGenomeService.metaClass.pathToChromosomeSizeFilesPerReference = { Project project, ReferenceGenome referenceGenome ->
            return null
        }

        TestCase.shouldFail(AssertionError) {
            executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_NoBaseBamFileExists() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        String seqTracks = SeqTrack.list().join(";")

        String expectedCmd =  """\
cd ${roddyPath} \
&& sudo -U OtherUnixUser roddy.sh rerun ${roddyBamFile.workflow.name}_${roddyBamFile.config.externalScriptVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${roddyBamFile.tmpRoddyDirectory} \
--cvalues=fastq_list:${seqTracks},\
REFERENCE_GENOME://tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/fileNamePrefix.fa,\
INDEX_PREFIX://tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/fileNamePrefix.fa,\
CHROM_SIZES_FILE://tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/stats/,\
possibleControlSampleNamePrefixes:(${roddyBamFile.sampleType.dirName})\
"""

        String actualCmd = executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_BaseBamFileExistsAlready() {
        executePanCanJob.executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = "a841c64c5825e986c4709ac7298e9366"
        assert roddyBamFile.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        String seqTracks = (SeqTrack.list() - roddyBamFile.seqTracks as List).join(";")

        String expectedCmd = """\
cd ${roddyPath} \
&& sudo -U OtherUnixUser roddy.sh rerun ${roddyBamFile2.workflow.name}_${roddyBamFile2.config.externalScriptVersion}.config@WGS \
${roddyBamFile2.individual.pid} \
--useconfig=${roddyApplicationIni} \
--useRoddyVersion=${roddyVersion} \
--usePluginVersion=${roddyBamFile2.config.externalScriptVersion} \
--configurationDirectories=${new File(roddyBamFile2.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${roddyBamFile2.tmpRoddyDirectory} \
--cvalues=fastq_list:${seqTracks},\
bam:${roddyBamFile.finalBamFile},\
REFERENCE_GENOME://tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/fileNamePrefix.fa,\
INDEX_PREFIX://tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/fileNamePrefix.fa,\
CHROM_SIZES_FILE://tmp/otp-unit-test/processing/reference_genomes/${roddyBamFile.referenceGenome.path}/stats/,\
possibleControlSampleNamePrefixes:(${roddyBamFile.sampleType.dirName})\
"""

        String actualCmd =  executePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile2, dataManagement)

        assert expectedCmd == actualCmd
    }



    @Test
    void testValidate_RoddyBamFileIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(null)
        }
    }

    @Test
    void testValidate_BamDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyBamFile.delete()
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }
    }

    @Test
    void testValidate_BaiDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyBaiFile.delete()
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }
    }

    @Test
    void testValidate_Md5sumDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyMd5sumFile.delete()
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }
    }

    @Test
    void testValidate_QaDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyQADirectory.deleteDir()
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }
    }

    @Test
    void testValidate_ExecutionStoreDirDoesNotExist_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        roddyBamFile.tmpRoddyExecutionStoreDirectory.deleteDir()
        TestCase.shouldFail(AssertionError) {
            executePanCanJob.validate(roddyBamFile)
        }
    }

    @Test
    void testValidate_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(dataManagement, roddyBamFile)
        executePanCanJob.validate(roddyBamFile)
    }
}
