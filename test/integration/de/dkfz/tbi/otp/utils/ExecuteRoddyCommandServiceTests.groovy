package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 */
class ExecuteRoddyCommandServiceTests {

    ExecuteRoddyCommandService executeRoddyCommandService = new ExecuteRoddyCommandService()

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()


    File RODDY_PATH
    File TMP_OUTPUT_DIR
    final String CONFIG_NAME = "WORKFLOW_VERSION"
    final String ANALYSIS_ID = "WHOLE_GENOME"
    final String COMMAND = "Hallo\nTest"
    Realm realm
    RoddyBamFile roddyBamFile
    File roddyBaseConfigsPath
    File applicationIniPath

    @Before
    void setUp() {
        temporaryFolder.create()

        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        RODDY_PATH = new File(ProcessingOptionService.getValueOfProcessingOption("roddyPath"))
        TMP_OUTPUT_DIR = temporaryFolder.newFolder("temporaryOutputDir")

        SeqType.buildLazy(name: SeqTypeNames.EXOME.seqTypeName, alias: SeqTypeNames.EXOME.seqTypeName, libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = DomainFactory.createRealmDataManagementDKFZ([
                name: roddyBamFile.project.realmName,
                rootPath: '${TMP_OUTPUT_DIR}/root',
                processingRootPath: '${TMP_OUTPUT_DIR}/processing',
        ])
        assert realm.save(flush: true)

        roddyBaseConfigsPath = new File(ProcessingOptionService.getValueOfProcessingOption("roddyBaseConfigsPath"))
        roddyBaseConfigsPath.mkdirs()
        applicationIniPath = new File(ProcessingOptionService.getValueOfProcessingOption("roddyApplicationIni"))
        assert CreateFileHelper.createFile(applicationIniPath)
    }

    @After
    void tearDown() {
        RODDY_PATH = null
        TMP_OUTPUT_DIR = null
        realm = null
        roddyBamFile = null
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executeRoddyCommandService)
        TestCase.removeMetaClass(ExecutionService, executeRoddyCommandService.executionService)

        if (applicationIniPath.exists()) {
            assert applicationIniPath.delete()
        }

        if (roddyBaseConfigsPath.exists()) {
            assert roddyBaseConfigsPath.deleteDir()
        }
    }

    @Test
    void testRoddyBaseCommand_RoddyPathIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(null, CONFIG_NAME, ANALYSIS_ID)
        }.contains("roddyPath is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_ConfigNameIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(RODDY_PATH.path, null, ANALYSIS_ID)
        }.contains("configName is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AnalysisIdIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(RODDY_PATH.path, CONFIG_NAME, null)
        }.contains("analysisId is not allowed to be null")
    }

    @Test
    void testRoddyBaseCommand_AllFine() {
        String expectedCmd =  "cd ${RODDY_PATH.path} && sudo -u OtherUnixUser roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} "
        String actualCmd = executeRoddyCommandService.roddyBaseCommand(RODDY_PATH.path, CONFIG_NAME, ANALYSIS_ID)
        assert expectedCmd == actualCmd
    }

    @Test
    void testExecuteRoddyCommand_InputCommandIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.executeRoddyCommand(null)
        }.contains("The input cmd must not be null")
    }

    @Test
    void testExecuteRoddyCommand_AllFine() {
        StringBuffer stdout = new StringBuffer()
        StringBuffer stderr = new StringBuffer()

        Process process = executeRoddyCommandService.executeRoddyCommand("echo '${COMMAND}'")

        process.waitForProcessOutput(stdout, stderr)
        assert stdout.toString().trim() == COMMAND
    }


    @Test
    void testReturnStdoutOfFinishedCommandExecution_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.returnStdoutOfFinishedCommandExecution(null)
        }.contains("The input process must not be null")
    }

    @Test
    void testReturnStdoutOfFinishedCommandExecution_AllFine() {
        Process process = [ 'bash', '-c', "echo '${COMMAND}'" ].execute()
        String actual = executeRoddyCommandService.returnStdoutOfFinishedCommandExecution(process)
        String expected = COMMAND
        assert actual == expected
    }


    @Test
    void testCheckIfRoddyWFExecutionWasSuccessful_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.checkIfRoddyWFExecutionWasSuccessful(null)
        }.contains("The input process must not be null")
    }

    @Test
    void testCheckIfRoddyWFExecutionWasSuccessful_AllFine() {
        Process process = [ 'bash', '-c', "echo '${COMMAND}'" ].execute()
        process.waitFor()
        executeRoddyCommandService.checkIfRoddyWFExecutionWasSuccessful(process)
    }


    @Test
    void testGetAnalysisIDinConfigFile_InputIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(null)
        }.contains("The input roddyResult must not be null")
    }

    @Test
    void testGetAnalysisIDinConfigFile_ObjectDoesNotHaveGetSeqTypeMethod_ShouldFail() {
        roddyBamFile.metaClass.getSeqType = { -> null }
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }.contains("There is not seqType available")
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeWGS() {
        roddyBamFile.mergingWorkPackage.seqType = SeqType.wholeGenomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.wholeGenomePairedSeqType.alias == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_SeqTypeEXOME() {
        roddyBamFile.mergingWorkPackage.seqType = SeqType.exomePairedSeqType
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert SeqType.exomePairedSeqType.alias == executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
    }

    @Test
    void testGetAnalysisIDinConfigFile_DifferentSeqType_ShouldFail() {
        roddyBamFile.mergingWorkPackage.seqType = SeqType.build(name: "differentSeqType")
        assert roddyBamFile.mergingWorkPackage.save(flush: true)

        assert TestCase.shouldFail(RuntimeException) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }.contains("The seqType ${roddyBamFile.seqType} can not be processed at the moment")
    }


    @Test
    void testDefaultRoddyExecutionCommand_ObjectIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(null, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("The input roddyResult is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_NameInConfigFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, null, ANALYSIS_ID, realm)
        }.contains("The input nameInConfigFile is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_AnalysisIDinConfigFileIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, null, realm)
        }.contains("The input analysisIDinConfigFile is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_RealmIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, null)
        }.contains("The input realm is not allowed to be null")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyPath")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyVersionIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyVersion").delete(flush: true)
        assert !ProcessingOption.findByName("roddyVersion")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyBaseConfigsPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyBaseConfigsPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyBaseConfigsPath")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        ProcessingOption.findByName("roddyApplicationIni").delete(flush: true)
        assert !ProcessingOption.findByName("roddyApplicationIni")
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains("Collection contains 0 elements")
    }


    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniDoesNotExistInFilesystem_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }
        assert applicationIniPath.delete()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains(applicationIniPath.path)
    }

    @Test
    void testDefaultRoddyExecutionCommand_BaseConfigFolderDoesNotExistInFilesystem_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }
        assert roddyBaseConfigsPath.deleteDir()
        assert TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }.contains(roddyBaseConfigsPath.path)
    }


    @Test
    void testDefaultRoddyExecutionCommand_AllFine() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        String expectedCmd = "cd ${RODDY_PATH}/ && " +
"sudo -u OtherUnixUser roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
"${roddyBamFile.individual.pid} " +
"--useconfig=${applicationIniPath} " +
"--useRoddyVersion=2.1.28 " +
"--usePluginVersion=${roddyBamFile.config.externalScriptVersion} " +
"--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},${roddyBaseConfigsPath}/ " +
"--useiodir=${realm.rootPath}/${roddyBamFile.project.dirName}/sequencing/${roddyBamFile.seqType.dirName}/view-by-pid/" +
                "${roddyBamFile.individual.pid}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/" +
                "merged-alignment/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id} "

        String actualCmd = executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)

        assert expectedCmd == actualCmd
    }


    @Test
    void testCreateTemporaryOutputDirectory_RealmIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createTemporaryOutputDirectory(null, TMP_OUTPUT_DIR)
        }
    }

    @Test
    void testCreateTemporaryOutputDirectory_FileIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createTemporaryOutputDirectory(realm, null)
        }
    }

   @Test
    void testCreateTemporaryOutputDirectory_DirectoryCreationFailed_ShouldFail() {
       executeRoddyCommandService.executionService.metaClass.executeCommand = { Realm realm, String command -> }
       TMP_OUTPUT_DIR.absoluteFile.delete()
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.createTemporaryOutputDirectory(realm, TMP_OUTPUT_DIR)
        }
    }

    @Test
    void testCreateTemporaryOutputDirectory_AllFine() {
        executeRoddyCommandService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            [ 'bash', '-c', command ].execute().waitFor()
        }
        executeRoddyCommandService.createTemporaryOutputDirectory(realm, TMP_OUTPUT_DIR)
    }

}
