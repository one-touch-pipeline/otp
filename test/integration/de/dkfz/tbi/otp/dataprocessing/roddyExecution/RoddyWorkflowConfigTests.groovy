package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ExternalScript
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 */
class RoddyWorkflowConfigTests {

    final String PLUGIN_VERSION = "1234" // arbitrary version
    final String ANOTHER_PLUGIN_VERSION = "5678"
    final Workflow.Name WRONG_WORKFLOW_NAME = Workflow.Name.DEFAULT_OTP

    File configDir
    File configFile
    File secondConfigFile

    @Before
    void setUp() {
        configDir = TestCase.createEmptyTestDirectory()
        configFile = new File(configDir, 'tempConfigFile.txt')
        CreateFileHelper.createFile(configFile)
        secondConfigFile = new File(configDir, 'tempConfigFile_v2.txt')
        CreateFileHelper.createFile(secondConfigFile)
    }


    @After
    void tearDown() {
        configFile.delete()
        secondConfigFile.delete()
        configDir.deleteDir()
        LsdfFilesService.metaClass = null
    }


    @Test
    void testValidateNewConfigFile_WorkflowIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.validateNewConfigFile(PLUGIN_VERSION, null, configFile.path)
        }
    }

    @Test
    void testValidateNewConfigFile_NoExternalScriptForThisVersion_ShouldFail() {
        Workflow workflow = Workflow.build()
        ExternalScript.build(scriptIdentifier: workflow.name.name(), scriptVersion: PLUGIN_VERSION)
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.validateNewConfigFile(ANOTHER_PLUGIN_VERSION, workflow, configFile.path)
        }
    }

    @Test
    void testValidateNewConfigFile_NoExternalScriptForThisWorkflowName_ShouldFail() {
        Workflow workflow = Workflow.build(name: Workflow.Name.RODDY, type: Workflow.Type.ALIGNMENT)
        ExternalScript.build(scriptIdentifier: WRONG_WORKFLOW_NAME, scriptVersion: PLUGIN_VERSION)
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.validateNewConfigFile(PLUGIN_VERSION, workflow, configFile.path)
        }
    }

    @Test
    void testValidateNewConfigFile_AllFine() {
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        int version = 0
        LsdfFilesService.metaClass.static.ensureFileIsReadableAndNotEmpty = { File file ->
            version ++
        }
        RoddyWorkflowConfig.validateNewConfigFile(PLUGIN_VERSION, workflow, configFile.path)
        assert version == 1
    }


    @Test
    void testImportProjectConfigFile_ProjectIsNull_ShouldFail() {
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.importProjectConfigFile(null, PLUGIN_VERSION, workflow, configFile.path)
        }
    }

    @Test
    void testImportProjectConfigFile_WorkflowIsNull_ShouldFail() {
        Project project = Project.build()
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.importProjectConfigFile(project, PLUGIN_VERSION, null, configFile.path)
        }
    }

    @Test
    void testImportProjectConfigFile_PluginVersionToUseIsNull_ShouldFail() {
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.importProjectConfigFile(project, null, workflow, configFile.path)
        }
    }

    @Test
    void testImportProjectConfigFile_ConfigFilePathIsNull_ShouldFail() {
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.importProjectConfigFile(project, PLUGIN_VERSION, workflow, null)
        }
    }

    @Test
    void testImportProjectConfigFile_NoPreviousRoddyWorkflowConfigExists() {
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        assert RoddyWorkflowConfig.list().size == 0
        RoddyWorkflowConfig.importProjectConfigFile(project, PLUGIN_VERSION, workflow, configFile.path)
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        assert roddyWorkflowConfig.project == project
        assert roddyWorkflowConfig.workflow == workflow
        assert roddyWorkflowConfig.configFilePath == configFile.path
        assert roddyWorkflowConfig.externalScriptVersion == PLUGIN_VERSION
        assert roddyWorkflowConfig.previousConfig == null
    }

    @Test
    void testImportProjectConfigFile_PreviousRoddyWorkflowConfigExists() {
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig1 = RoddyWorkflowConfig.build(project: project, workflow: workflow, externalScriptVersion: ANOTHER_PLUGIN_VERSION)
        assert RoddyWorkflowConfig.list().size == 1
        RoddyWorkflowConfig.importProjectConfigFile(project, PLUGIN_VERSION, workflow, configFile.path)
        assert RoddyWorkflowConfig.list().size == 2
        RoddyWorkflowConfig roddyWorkflowConfig2 = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByExternalScriptVersion(PLUGIN_VERSION))
        assert roddyWorkflowConfig2.previousConfig == roddyWorkflowConfig1
        assert roddyWorkflowConfig1.obsoleteDate
    }


    @Test
    void testGetLatest_ProjectIsNull_ShouldFail() {
        Workflow workflow = Workflow.build()
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.getLatest(null, workflow)
        }
    }

    @Test
    void testGetLatest_WorkflowIsNull_ShouldFail() {
        Project project = Project.build()
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.getLatest(project, null)
        }
    }

    @Test
    void testGetLatest_ThereIsNoConfigFileInTheDatabase() {
        Project project = Project.build()
        Workflow workflow = Workflow.build()
        assert !RoddyWorkflowConfig.getLatest(project, workflow)
    }


    @Test
    void testGetLatest_OneRoddyWorkflowConfigExists() {
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig = RoddyWorkflowConfig.build(project: project, workflow: workflow, externalScriptVersion: PLUGIN_VERSION)
        assert RoddyWorkflowConfig.getLatest(project, workflow) == roddyWorkflowConfig
    }


    @Test
    void testGetLatest_OneActiveAndOneObsoleteRoddyWorkflowConfigExists() {
        File newConfigFile = CreateFileHelper.createFile(new File(configDir, 'ConfigFile2.txt'))
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig1 = RoddyWorkflowConfig.build(project: project, workflow: workflow, externalScriptVersion: PLUGIN_VERSION, obsoleteDate: new Date())
        RoddyWorkflowConfig roddyWorkflowConfig2 = RoddyWorkflowConfig.build(project: project, workflow: workflow, externalScriptVersion: ANOTHER_PLUGIN_VERSION,
                previousConfig: roddyWorkflowConfig1, configFilePath: newConfigFile.path)
        assert RoddyWorkflowConfig.getLatest(project, workflow) == roddyWorkflowConfig2
    }


    void testCreateConfigPerProject_PreviousConfigExists() {
        Workflow workflow = Workflow.build(name: Workflow.Name.RODDY, type: Workflow.Type.ALIGNMENT)
        ExternalScript externalScript1 = ExternalScript.build()
        Project project = TestData.createProject()
        ConfigPerProject firstConfigPerProject = RoddyWorkflowConfig.build(
                project: project,
                externalScriptVersion: externalScript1.scriptVersion,
                configFilePath: configFile.path,
        )
        firstConfigPerProject.save()

        ExternalScript externalScript2 = ExternalScript.build([
                scriptIdentifier: "scriptIdentifier2",
                scriptVersion: "v2",
                filePath: "${externalScript1.filePath}_1"
        ])

        ConfigPerProject newConfigPerProject = new RoddyWorkflowConfig(
                project: project,
                workflow: workflow,
                previousConfig: firstConfigPerProject,
                externalScriptVersion: externalScript2.scriptVersion,
                configFilePath: secondConfigFile.path,
        )

        assert !firstConfigPerProject.obsoleteDate

        newConfigPerProject.createConfigPerProject()

        assert ConfigPerProject.findAllByProject(project).size() == 2
        assert firstConfigPerProject.obsoleteDate
    }


    void testCreateConfigPerProject_PreviousConfigDoesNotExist(){
        ExternalScript externalScript = ExternalScript.build()
        Project project = TestData.createProject()
        ConfigPerProject configPerProject = RoddyWorkflowConfig.build(
                project: project,
                externalScriptVersion: externalScript.scriptVersion,
                configFilePath: configFile,
        )
        configPerProject.createConfigPerProject()
        assert ConfigPerProject.findAllByProject(project).size() == 1
    }


    void testMakeObsolete() {
        ExternalScript externalScript = ExternalScript.build()
        ConfigPerProject configPerProject = RoddyWorkflowConfig.build(
                project: TestData.createProject(),
                externalScriptVersion: externalScript.scriptVersion,
                configFilePath: configFile,
        )
        assert !configPerProject.obsoleteDate
        configPerProject.makeObsolete()
        assert configPerProject.obsoleteDate
    }


    private Workflow createCorrectSetupAndReturnWorkflow() {
        Workflow workflow = Workflow.build(name: Workflow.Name.RODDY, type: Workflow.Type.ALIGNMENT)
        ExternalScript externalScript = ExternalScript.build(scriptIdentifier: workflow.name.name(), scriptVersion: PLUGIN_VERSION)
        ExternalScript.build(scriptIdentifier: workflow.name.name(), scriptVersion: ANOTHER_PLUGIN_VERSION, filePath: "${externalScript.filePath}_v1")
        return workflow
    }

}
