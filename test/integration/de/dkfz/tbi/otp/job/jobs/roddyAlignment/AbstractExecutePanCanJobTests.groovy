package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*
import org.junit.rules.*

import static de.dkfz.tbi.otp.utils.ProcessHelperService.*

class AbstractExecutePanCanJobTests {

    AbstractExecutePanCanJob abstractExecutePanCanJob

    LsdfFilesService lsdfFilesService

    Realm dataManagement
    RoddyBamFile roddyBamFile
    File configFile
    File roddyCommand
    File roddyBaseConfigsPath
    File roddyApplicationIni
    File chromosomeStatSizeFile
    File featureTogglesConfigPath
    File referenceGenomeFile

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        DomainFactory.createPanCanAlignableSeqTypes()

        roddyBamFile = DomainFactory.createRoddyBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractMergedBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        roddyBamFile.workPackage.metaClass.findMergeableSeqTracks = { -> SeqTrack.list() }

        Realm dataProcessing = DomainFactory.createRealmDataProcessing(tmpDir.root, [name: roddyBamFile.project.realmName, roddyUser: HelperUtils.uniqueString])
        dataManagement = DomainFactory.createRealmDataManagement(tmpDir.root, [name: roddyBamFile.project.realmName])
        abstractExecutePanCanJob = [
                prepareAndReturnWorkflowSpecificCValues  : { RoddyBamFile bamFile -> ["workflowSpecificCValues"] },
                prepareAndReturnWorkflowSpecificParameter: { RoddyBamFile bamFile -> "workflowSpecificParameter " },

        ] as AbstractExecutePanCanJob

        abstractExecutePanCanJob.referenceGenomeService = new ReferenceGenomeService()
        abstractExecutePanCanJob.referenceGenomeService.configService = new ConfigService()
        abstractExecutePanCanJob.lsdfFilesService = new LsdfFilesService()
        abstractExecutePanCanJob.executeRoddyCommandService = new ExecuteRoddyCommandService()
        abstractExecutePanCanJob.bedFileService = new BedFileService()
        abstractExecutePanCanJob.configService = new ConfigService()

        File processingRootPath = dataProcessing.processingRootPath as File

        DomainFactory.createRoddyProcessingOptions(tmpDir.newFolder())

        File roddyPath = ProcessingOption.findByName("roddyPath").value as File
        roddyCommand = new File(roddyPath, 'roddy.sh')
        roddyBaseConfigsPath = ProcessingOption.findByName("roddyBaseConfigsPath").value as File
        roddyBaseConfigsPath.mkdirs()
        roddyApplicationIni = ProcessingOption.findByName("roddyApplicationIni").value as File
        roddyApplicationIni.text = "Some Text"
        featureTogglesConfigPath = ProcessingOption.findByName(ExecuteRoddyCommandService.FEATURE_TOGGLES_CONFIG_PATH).value as File

        File referenceGenomeDir = new File("${processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()
        referenceGenomeFile = new File(referenceGenomeDir, "${roddyBamFile.referenceGenome.fileNamePrefix}.fa")
        CreateFileHelper.createFile(referenceGenomeFile)

        configFile = tmpDir.newFile()
        CreateFileHelper.createFile(configFile)
        RoddyWorkflowConfig config = roddyBamFile.config
        config.configFilePath = configFile.path
        assert config.save(failOnError: true)

        chromosomeStatSizeFile = abstractExecutePanCanJob.referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage, false)
        CreateFileHelper.createFile(chromosomeStatSizeFile)

        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.correctPermissions = { RoddyBamFile bamFile, Realm realm -> }
    }


    @After
    void tearDown() {
        TestCase.removeMetaClass(ExecuteRoddyCommandService, abstractExecutePanCanJob.executeRoddyCommandService)
        TestCase.removeMetaClass(ReferenceGenomeService, abstractExecutePanCanJob.referenceGenomeService)
    }


    private String viewByPidString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RoddyResultIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(null, dataManagement)
        }.contains("roddyResult must not be null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RealmIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, null)
        }.contains("realm must not be null")
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_ConfigFileIsNotInFileSystem_ShouldFail() {
        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }
        assert configFile.delete()

        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        }.contains(roddyBamFile.config.configFilePath)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_AllFine() {
        roddyBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert roddyBamFile.project.save(flush: true)

        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String expectedCmd = """\
cd /tmp \
&& sudo -u OtherUnixUser ${roddyCommand} rerun \
${roddyBamFile.pipeline.name}_${roddyBamFile.seqType.roddyName}_\
${roddyBamFile.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--usePluginVersion=${roddyBamFile.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
workflowSpecificParameter \
--cvalues="workflowSpecificCValues,\
PBS_AccountName:FASTTRACK"\
"""

        String actualCmd = abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, dataManagement)
        assert expectedCmd == actualCmd
    }


    @Test
    void testPrepareAndReturnCValues_RoddyResultIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnCValues(null)
        }.contains("roddyResult must not be null")
    }


    @Test
    void testPrepareAndReturnCValues_NoFastTrack_setUpCorrect() {
        String expectedCommand = """\
--cvalues="workflowSpecificCValues"\
"""
        String actualCommand = abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
        assert expectedCommand == actualCommand
    }


    @Test
    void testPrepareAndReturnCValues_FastTrack_setUpCorrect() {
        roddyBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert roddyBamFile.project.save(flush: true)

        String expectedCommand = """\
--cvalues=\
"workflowSpecificCValues,\
PBS_AccountName:FASTTRACK"\
"""
        assert expectedCommand == abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
    }


    @Test
    void testGetChromosomeIndexParameter_RoddyResultIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameter(null)
        }.contains("assert referenceGenome")
    }

    @Test
    void testGetChromosomeIndexParameter_NoChromosomeNamesExist_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameter(roddyBamFile.referenceGenome)
        }.contains("No chromosome names could be found for reference genome")
    }


    @Test
    void testGetChromosomeIndexParameter_AllFine() {
        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "M", "X", "Y"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)

        assert "CHROMOSOME_INDICES:( ${chromosomeNames.join(' ')} )" == abstractExecutePanCanJob.getChromosomeIndexParameter(roddyBamFile.referenceGenome)
    }
}
