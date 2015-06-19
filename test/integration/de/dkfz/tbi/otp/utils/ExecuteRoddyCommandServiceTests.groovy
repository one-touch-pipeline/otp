package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 */
class ExecuteRoddyCommandServiceTests {

    ExecuteRoddyCommandService executeRoddyCommandService = new ExecuteRoddyCommandService()


    final String RODDY_PATH = "/path/to/roddy/"
    final File TMP_OUTPUT_DIR = new File(TestCase.TEST_DIRECTORY, "temporaryOutputDir")
    final String CONFIG_NAME = "WORKFLOW_VERSION"
    final String ANALYSIS_ID = "WHOLE_GENOME"
    final String COMMAND = "Hallo\nTest"
    Realm realm
    RoddyBamFile roddyBamFile

    @Before
    void setUp() {
        SeqType.buildLazy(name: SeqTypeNames.EXOME.seqTypeName, alias: SeqTypeNames.EXOME.seqTypeName, libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED)
        roddyBamFile = DomainFactory.createRoddyBamFile()

        realm = DomainFactory.createRealmDataManagementDKFZ([
                name: roddyBamFile.project.realmName,
                rootPath: '${TMP_OUTPUT_DIR}/root',
                processingRootPath: '${TMP_OUTPUT_DIR}/processing',
        ])
        assert realm.save(flush: true)

    }

    @After
    void tearDown() {
        realm = null
        roddyBamFile = null
        TestCase.removeMetaClass(ExecuteRoddyCommandService, executeRoddyCommandService)
        TestCase.removeMetaClass(ExecutionService, executeRoddyCommandService.executionService)

        assert TMP_OUTPUT_DIR.deleteDir()
    }

    @Test
    void testRoddyBaseCommand_RoddyPathIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(null, CONFIG_NAME, ANALYSIS_ID)
        }
    }

    @Test
    void testRoddyBaseCommand_ConfigNameIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(RODDY_PATH, null, ANALYSIS_ID)
        }
    }

    @Test
    void testRoddyBaseCommand_AnalysisIdIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.roddyBaseCommand(RODDY_PATH, CONFIG_NAME, null)
        }
    }

    @Test
    void testRoddyBaseCommand_AllFine() {
        String expectedCmd =  "cd ${RODDY_PATH} && sudo -u OtherUnixUser roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} "
        String actualCmd = executeRoddyCommandService.roddyBaseCommand(RODDY_PATH, CONFIG_NAME, ANALYSIS_ID)
        assert expectedCmd == actualCmd
    }

    @Test
    void testExecuteRoddyCommand_InputCommandIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.executeRoddyCommand(null)
        }
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
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.returnStdoutOfFinishedCommandExecution(null)
        }
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
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.checkIfRoddyWFExecutionWasSuccessful(null)
        }
    }

    @Test
    void testCheckIfRoddyWFExecutionWasSuccessful_AllFine() {
        Process process = [ 'bash', '-c', "echo '${COMMAND}'" ].execute()
        process.waitFor()
        executeRoddyCommandService.checkIfRoddyWFExecutionWasSuccessful(process)
    }


    @Test
    void testGetAnalysisIDinConfigFile_InputIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(null)
        }
    }

    @Test
    void testGetAnalysisIDinConfigFile_ObjectDoesNotHaveGetSeqTypeMethod_ShouldFail() {
        roddyBamFile.metaClass.getSeqType = { -> null }
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }
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

        TestCase.shouldFail(RuntimeException) {
            executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        }
    }


    @Test
    void testDefaultRoddyExecutionCommand_ObjectIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(null, CONFIG_NAME, ANALYSIS_ID, realm)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_NameInConfigFileIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, null, ANALYSIS_ID, realm)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_AnalysisIDinConfigFileIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, null, realm)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_RealmIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, null)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        DomainFactory.createRoddyProcessingOptions()
        ProcessingOption.findByName("roddyPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyPath")
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyVersionIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        DomainFactory.createRoddyProcessingOptions()
        ProcessingOption.findByName("roddyVersion").delete(flush: true)
        assert !ProcessingOption.findByName("roddyVersion")
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyBaseConfigsPathIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        DomainFactory.createRoddyProcessingOptions()
        ProcessingOption.findByName("roddyBaseConfigsPath").delete(flush: true)
        assert !ProcessingOption.findByName("roddyBaseConfigsPath")
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_ProcessingOptionRoddyApplicationIniIsNull_ShouldFail() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        DomainFactory.createRoddyProcessingOptions()
        ProcessingOption.findByName("roddyApplicationIni").delete(flush: true)
        assert !ProcessingOption.findByName("roddyApplicationIni")
        TestCase.shouldFail(AssertionError) {
            executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, CONFIG_NAME, ANALYSIS_ID, realm)
        }
    }

    @Test
    void testDefaultRoddyExecutionCommand_AllFine() {
        executeRoddyCommandService.metaClass.createTemporaryOutputDirectory = { Realm realm, File file -> }

        DomainFactory.createRoddyProcessingOptions()

        String expectedCmd = "cd ${RODDY_PATH} && " +
"sudo -u OtherUnixUser roddy.sh rerun ${CONFIG_NAME}.config@${ANALYSIS_ID} " +
"${roddyBamFile.individual.pid} " +
"--useconfig=/path/to/roddyBaseConfigs/applicationProperties.ini " +
"--useRoddyVersion=2.1.28 " +
"--usePluginVersion=${roddyBamFile.config.externalScriptVersion} " +
"--configurationDirectories=${new File(roddyBamFile.config.configFilePath).parent},/path/to/roddyBaseConfigs/ " +
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
