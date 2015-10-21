package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 */
class RoddyWorkflowConfigTests {

    static final String TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART = 'plugin'
    static final String TEST_RODDY_PLUGIN_VERSION_VERSION_PART = '1.2.3'
    static final String TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2 = '1.2.4'
    static final String TEST_RODDY_PLUGIN_VERSION = "${TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART}:${TEST_RODDY_PLUGIN_VERSION_VERSION_PART}"
    static final String TEST_RODDY_PLUGIN_VERSION_2 = "${TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART}:${TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2}"
    static final String TEST_RODDY_CONFIG_FILE_NAME = "${Workflow.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_PLUGIN_VERSION_VERSION_PART}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_FILE_NAME_PLUGIN_VERSION_2 = "${Workflow.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_LABEL_IN_FILE = "${Workflow.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_PLUGIN_VERSION}_${DomainFactory.TEST_CONFIG_VERSION}"
    final String INVALID_CONFIG_VERSION = "invalid"

    File configDir
    File configFile
    File secondConfigFile

    @Before
    void setUp() {
        configDir = TestCase.createEmptyTestDirectory()
        configFile = new File(configDir, TEST_RODDY_CONFIG_FILE_NAME)
        CreateFileHelper.createRoddyWorkflowConfig(configFile, TEST_RODDY_CONFIG_LABEL_IN_FILE)
        secondConfigFile = new File(configDir, TEST_RODDY_CONFIG_FILE_NAME_PLUGIN_VERSION_2)
        CreateFileHelper.createRoddyWorkflowConfig(secondConfigFile, TEST_RODDY_CONFIG_LABEL_IN_FILE)
    }


    @After
    void tearDown() {
        configFile.delete()
        secondConfigFile.delete()
        TestCase.cleanTestDirectory()
    }



    @Test
    void testImportProjectConfigFile_ProjectIsNull_ShouldFail() {
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The project is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(null, TEST_RODDY_PLUGIN_VERSION, workflow, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_WorkflowIsNull_ShouldFail() {
        Project project = Project.build()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The workflow is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, TEST_RODDY_PLUGIN_VERSION, null, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_PluginVersionToUseIsNull_ShouldFail() {
        Project project = Project.build()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The pluginVersionToUse is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, null, workflow, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_ConfigFilePathIsNull_ShouldFail() {
        Project project = Project.build()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The configFilePath is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, TEST_RODDY_PLUGIN_VERSION, workflow, null, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_ConfigVersionIsBlank_ShouldFail() {
        Project project = Project.build()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The configVersion is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, TEST_RODDY_PLUGIN_VERSION, workflow, configFile.path, '')
        }
    }

    @Test
    void testImportProjectConfigFile_NoPreviousRoddyWorkflowConfigExists() {
        Project project = Project.build()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        assert RoddyWorkflowConfig.list().size == 0
        RoddyWorkflowConfig.importProjectConfigFile(project, TEST_RODDY_PLUGIN_VERSION, workflow, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        assert roddyWorkflowConfig.project == project
        assert roddyWorkflowConfig.workflow == workflow
        assert roddyWorkflowConfig.configFilePath == configFile.path
        assert roddyWorkflowConfig.pluginVersion == TEST_RODDY_PLUGIN_VERSION
        assert roddyWorkflowConfig.previousConfig == null
        assert roddyWorkflowConfig.configVersion == DomainFactory.TEST_CONFIG_VERSION
    }

    @Test
    void testImportProjectConfigFile_PreviousRoddyWorkflowConfigExists() {
        Project project = Project.build()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(project: project, workflow: workflow, pluginVersion: TEST_RODDY_PLUGIN_VERSION_2, configVersion: DomainFactory.TEST_CONFIG_VERSION)
        assert RoddyWorkflowConfig.list().size == 1
        RoddyWorkflowConfig.importProjectConfigFile(project, TEST_RODDY_PLUGIN_VERSION, workflow, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        assert RoddyWorkflowConfig.list().size == 2
        RoddyWorkflowConfig roddyWorkflowConfig2 = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByPluginVersion(TEST_RODDY_PLUGIN_VERSION))
        assert roddyWorkflowConfig2.previousConfig == roddyWorkflowConfig1
        assert roddyWorkflowConfig1.obsoleteDate
    }


    @Test
    void testGetLatest_ProjectIsNull_ShouldFail() {
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The project is not allowed to be null') {
            RoddyWorkflowConfig.getLatest(null, workflow)
        }
    }

    @Test
    void testGetLatest_WorkflowIsNull_ShouldFail() {
        Project project = DomainFactory.createProject()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The workflow is not allowed to be null') {
            RoddyWorkflowConfig.getLatest(project, null)
        }
    }

    @Test
    void testGetLatest_ThereIsNoConfigFileInTheDatabase() {
        Project project = DomainFactory.createProject()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        assert !RoddyWorkflowConfig.getLatest(project, workflow)
    }


    @Test
    void testGetLatest_OneRoddyWorkflowConfigExists() {
        Project project = DomainFactory.createProject()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                workflow: workflow,
        ])
        assert RoddyWorkflowConfig.getLatest(project, workflow) == roddyWorkflowConfig
    }


    @Test
    void testGetLatest_OneActiveAndOneObsoleteRoddyWorkflowConfigExists() {
        File newConfigFile = CreateFileHelper.createFile(new File(configDir, 'ConfigFile2.txt'))
        Project project = DomainFactory.createProject()
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                workflow: workflow,
                obsoleteDate: new Date(),
        )
        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                workflow: workflow,
                previousConfig: roddyWorkflowConfig1,
        )
        assert RoddyWorkflowConfig.getLatest(project, workflow) == roddyWorkflowConfig2
    }


    @Test
    void testCreateConfigPerProject_PreviousConfigExists() {
        Workflow workflow = DomainFactory.returnOrCreateAnyWorkflow()
        Project project = DomainFactory.createProject()
        ConfigPerProject firstConfigPerProject = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                workflow: workflow,
        )

        ConfigPerProject newConfigPerProject = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                workflow: workflow,
                previousConfig: firstConfigPerProject,
        ], false)

        assert !firstConfigPerProject.obsoleteDate

        newConfigPerProject.createConfigPerProject()

        assert ConfigPerProject.findAllByProject(project).size() == 2
        assert firstConfigPerProject.obsoleteDate
    }


    @Test
    void testCreateConfigPerProject_PreviousConfigDoesNotExist(){
        Project project = DomainFactory.createProject()
        ConfigPerProject configPerProject = DomainFactory.createRoddyWorkflowConfig(
                project: project,
        )
        configPerProject.createConfigPerProject()
        assert ConfigPerProject.findAllByProject(project).size() == 1
    }


    @Test
    void testMakeObsolete() {
        ConfigPerProject configPerProject = DomainFactory.createRoddyWorkflowConfig()
        assert !configPerProject.obsoleteDate
        configPerProject.makeObsolete()
        assert configPerProject.obsoleteDate
    }
}
