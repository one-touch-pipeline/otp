package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.utils.HelperUtils

/**
 */
class RoddyWorkflowConfigTests {

    final String PLUGIN_VERSION = "1234" // arbitrary version
    final String ANOTHER_PLUGIN_VERSION = "5678"

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
        TestCase.cleanTestDirectory()
        LsdfFilesService.metaClass = null
    }


    @Test
    void testValidateNewConfigFile_WorkflowIsNull_ShouldFail() {
        TestCase.shouldFail(AssertionError) {
            RoddyWorkflowConfig.validateNewConfigFile(PLUGIN_VERSION, null, configFile.path)
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
        assert roddyWorkflowConfig.pluginVersion == PLUGIN_VERSION
        assert roddyWorkflowConfig.previousConfig == null
    }

    @Test
    void testImportProjectConfigFile_PreviousRoddyWorkflowConfigExists() {
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig1 = RoddyWorkflowConfig.build(project: project, workflow: workflow, pluginVersion: ANOTHER_PLUGIN_VERSION)
        assert RoddyWorkflowConfig.list().size == 1
        RoddyWorkflowConfig.importProjectConfigFile(project, PLUGIN_VERSION, workflow, configFile.path)
        assert RoddyWorkflowConfig.list().size == 2
        RoddyWorkflowConfig roddyWorkflowConfig2 = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByPluginVersion(PLUGIN_VERSION))
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
        RoddyWorkflowConfig roddyWorkflowConfig = RoddyWorkflowConfig.build(project: project, workflow: workflow, pluginVersion: PLUGIN_VERSION)
        assert RoddyWorkflowConfig.getLatest(project, workflow) == roddyWorkflowConfig
    }


    @Test
    void testGetLatest_OneActiveAndOneObsoleteRoddyWorkflowConfigExists() {
        File newConfigFile = CreateFileHelper.createFile(new File(configDir, 'ConfigFile2.txt'))
        Project project = Project.build()
        Workflow workflow = createCorrectSetupAndReturnWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig1 = RoddyWorkflowConfig.build(project: project, workflow: workflow, pluginVersion: PLUGIN_VERSION, obsoleteDate: new Date())
        RoddyWorkflowConfig roddyWorkflowConfig2 = RoddyWorkflowConfig.build(project: project, workflow: workflow, pluginVersion: ANOTHER_PLUGIN_VERSION,
                previousConfig: roddyWorkflowConfig1, configFilePath: newConfigFile.path)
        assert RoddyWorkflowConfig.getLatest(project, workflow) == roddyWorkflowConfig2
    }


    void testCreateConfigPerProject_PreviousConfigExists() {
        Workflow workflow = DomainFactory.createPanCanWorkflow()
        Project project = TestData.createProject()
        ConfigPerProject firstConfigPerProject = RoddyWorkflowConfig.build(
                project: project,
                pluginVersion: HelperUtils.uniqueString,
                configFilePath: configFile.path,
        )
        firstConfigPerProject.save()

        ConfigPerProject newConfigPerProject = new RoddyWorkflowConfig(
                project: project,
                workflow: workflow,
                previousConfig: firstConfigPerProject,
                pluginVersion: HelperUtils.uniqueString,
                configFilePath: secondConfigFile.path,
        )

        assert !firstConfigPerProject.obsoleteDate

        newConfigPerProject.createConfigPerProject()

        assert ConfigPerProject.findAllByProject(project).size() == 2
        assert firstConfigPerProject.obsoleteDate
    }


    void testCreateConfigPerProject_PreviousConfigDoesNotExist(){
        Project project = TestData.createProject()
        ConfigPerProject configPerProject = RoddyWorkflowConfig.build(
                project: project,
                pluginVersion: HelperUtils.uniqueString,
                configFilePath: configFile,
        )
        configPerProject.createConfigPerProject()
        assert ConfigPerProject.findAllByProject(project).size() == 1
    }


    void testMakeObsolete() {
        ConfigPerProject configPerProject = RoddyWorkflowConfig.build(
                project: TestData.createProject(),
                pluginVersion: HelperUtils.uniqueString,
                configFilePath: configFile,
        )
        assert !configPerProject.obsoleteDate
        configPerProject.makeObsolete()
        assert configPerProject.obsoleteDate
    }


    private Workflow createCorrectSetupAndReturnWorkflow() {
        Workflow workflow = DomainFactory.createPanCanWorkflow()
        return workflow
    }

}
