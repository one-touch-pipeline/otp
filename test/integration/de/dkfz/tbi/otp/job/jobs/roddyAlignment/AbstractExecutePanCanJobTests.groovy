package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*
import org.junit.rules.*

class AbstractExecutePanCanJobTests {

    AbstractExecutePanCanJob abstractExecutePanCanJob

    LsdfFilesService lsdfFilesService

    Realm realm
    TestConfigService configService
    RoddyBamFile roddyBamFile
    File configFile
    File roddyCommand
    File roddyBaseConfigsPath
    File roddyApplicationIni
    File chromosomeStatSizeFile
    File featureTogglesConfigPath
    File referenceGenomeFile

    String workflowSpecificCValues = "workflowSpecificCValues"

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {
        DomainFactory.createRoddyAlignableSeqTypes()

        roddyBamFile = DomainFactory.createRoddyBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractMergedBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        roddyBamFile.workPackage.metaClass.seqTracks = SeqTrack.list()

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : tmpDir.root.path + "/root",
                (OtpProperty.PATH_PROCESSING_ROOT): tmpDir.root.path + "/processing",
        ]
        )
        realm = DomainFactory.createRealm()
        abstractExecutePanCanJob = [
                prepareAndReturnWorkflowSpecificCValues  : { RoddyBamFile bamFile -> ["${workflowSpecificCValues}"] },
                prepareAndReturnWorkflowSpecificParameter: { RoddyBamFile bamFile -> "workflowSpecificParameter" },
        ] as AbstractExecutePanCanJob

        abstractExecutePanCanJob.referenceGenomeService = new ReferenceGenomeService()
        abstractExecutePanCanJob.referenceGenomeService.configService = configService
        abstractExecutePanCanJob.referenceGenomeService.processingOptionService = new ProcessingOptionService()
        abstractExecutePanCanJob.lsdfFilesService = new LsdfFilesService()
        abstractExecutePanCanJob.executeRoddyCommandService = new ExecuteRoddyCommandService()
        abstractExecutePanCanJob.executeRoddyCommandService.processingOptionService = new ProcessingOptionService()
        abstractExecutePanCanJob.bedFileService = new BedFileService()
        abstractExecutePanCanJob.configService = configService
        abstractExecutePanCanJob.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()

        File processingRootPath = configService.getProcessingRootPath()

        DomainFactory.createRoddyProcessingOptions(tmpDir.newFolder())

        File roddyPath = ProcessingOption.findByName(OptionName.RODDY_PATH).value as File
        roddyCommand = new File(roddyPath, 'roddy.sh')
        roddyBaseConfigsPath = ProcessingOption.findByName(OptionName.RODDY_BASE_CONFIGS_PATH).value as File
        roddyBaseConfigsPath.mkdirs()
        roddyApplicationIni = ProcessingOption.findByName(OptionName.RODDY_APPLICATION_INI).value as File
        roddyApplicationIni.text = "Some Text"
        featureTogglesConfigPath = ProcessingOption.findByName(OptionName.RODDY_FEATURE_TOGGLES_CONFIG_PATH).value as File

        File referenceGenomeDir = new File("${processingRootPath}/reference_genomes/${roddyBamFile.referenceGenome.path}")
        assert referenceGenomeDir.mkdirs()
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDir.parent)
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
        configService.clean()
    }


    private String viewByPidString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.individual.getViewByPidPathBase(roddyBamFile.seqType).absoluteDataManagementPath.path
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_RoddyResultIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(null, realm)
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
            abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, realm)
        }.contains(roddyBamFile.config.configFilePath)
    }


    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_NormalPriority_AllFine() {
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper("${roddyBamFile.config.pluginVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_MinimalPriority_AllFine() {
        roddyBamFile.project.processingPriority = ProcessingPriority.MINIMUM.priority
        roddyBamFile.project.save()
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper("${roddyBamFile.config.pluginVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_FasttrackPriority_AllFine() {
        roddyBamFile.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        roddyBamFile.project.save()
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper("${roddyBamFile.config.pluginVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}-fasttrack")
    }

    @Test
    void testPrepareAndReturnWorkflowSpecificCommand_OverFasttrackPriority_AllFine() {
        roddyBamFile.project.processingPriority = (ProcessingPriority.FAST_TRACK.priority + 10) as short
        roddyBamFile.project.save()
        testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper("${roddyBamFile.config.pluginVersion}-${roddyBamFile.seqType.roddyName.toLowerCase()}-fasttrack")
    }

    void testPrepareAndReturnWorkflowSpecificCommand_AllFineHelper(String additionalImports) {
        DomainFactory.createProcessingOptionForInitRoddyModule()
        abstractExecutePanCanJob.executeRoddyCommandService.metaClass.createWorkOutputDirectory = { Realm realm, File file -> }

        String expectedCmd = """
${roddyCommand} rerun \
${roddyBamFile.pipeline.name}_${roddyBamFile.seqType.roddyName}_${roddyBamFile.seqType.libraryLayout}_\
${roddyBamFile.config.pluginVersion}_${roddyBamFile.config.configVersion}.config@WGS \
${roddyBamFile.individual.pid} \
--useconfig=${roddyApplicationIni} \
--usefeaturetoggleconfig=${featureTogglesConfigPath} \
--usePluginVersion=${roddyBamFile.config.pluginVersion} \
--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath},${roddyBaseConfigsPath}/resource/${roddyBamFile.project.realm.jobScheduler.toString().toLowerCase()} \
--useiodir=${viewByPidString()},${roddyBamFile.workDirectory} \
--additionalImports=${additionalImports} \
workflowSpecificParameter \
--cvalues="$workflowSpecificCValues"\
"""

        String actualCmd = abstractExecutePanCanJob.prepareAndReturnWorkflowSpecificCommand(roddyBamFile, realm)
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
--cvalues="$workflowSpecificCValues"\
"""
        assert expectedCommand == abstractExecutePanCanJob.prepareAndReturnCValues(roddyBamFile)
    }

    @Test
    void testGetChromosomeIndexParameterWithMitochondrium_RoddyResultIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithMitochondrium(null)
        }.contains("assert referenceGenome")
    }

    @Test
    void testGetChromosomeIndexParameterWithMitochondrium_NoChromosomeNamesExist_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithMitochondrium(roddyBamFile.referenceGenome)
        }.contains("No chromosome names could be found for reference genome")
    }


    @Test
    void testGetChromosomeIndexParameterWithMitochondrium_AllFine() {
        List<String> chromosomeNames = ["1", "4", "3", "X", "5", "M", "2", "Y", "21"]
        List<String> chromosomeNamesExpected = ["1", "2", "3", "4", "5", "21", "X", "Y", "M"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)

        assert "CHROMOSOME_INDICES:( ${chromosomeNamesExpected.join(' ')} )" == abstractExecutePanCanJob.getChromosomeIndexParameterWithMitochondrium(roddyBamFile.referenceGenome)
    }

    @Test
    void testGetChromosomeIndexParameterWithoutMitochondrium_RoddyResultIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithoutMitochondrium(null)
        }.contains("assert referenceGenome")
    }

    @Test
    void testGetChromosomeIndexParameterWithoutMitochondrium_NoChromosomeNamesExist_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            abstractExecutePanCanJob.getChromosomeIndexParameterWithoutMitochondrium(roddyBamFile.referenceGenome)
        }.contains("No chromosome names could be found for reference genome")
    }


    @Test
    void testGetChromosomeIndexParameterWithoutMitochondrium_AllFine() {
        List<String> chromosomeNames = ["1", "4", "3", "X", "5", "2", "Y", "21"]
        List<String> chromosomeNamesExpected = ["1", "2", "3", "4", "5", "21", "X", "Y"]
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, chromosomeNames)
        DomainFactory.createReferenceGenomeEntry(
                referenceGenome: roddyBamFile.referenceGenome,
                classification: ReferenceGenomeEntry.Classification.MITOCHONDRIAL,
                name: "M",
                alias: "M"
        )

        assert "CHROMOSOME_INDICES:( ${chromosomeNamesExpected.join(' ')} )" == abstractExecutePanCanJob.getChromosomeIndexParameterWithoutMitochondrium(roddyBamFile.referenceGenome)
    }
}
