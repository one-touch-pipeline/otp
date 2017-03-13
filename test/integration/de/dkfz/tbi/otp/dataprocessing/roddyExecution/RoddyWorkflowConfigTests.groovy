package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProject
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper
import org.junit.After
import org.junit.Before
import org.junit.Test


class RoddyWorkflowConfigTests {

    static final String TEST_RODDY_SEQ_TYPE_RODDY_NAME = 'roddyName'
    static final String TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART = 'plugin'
    static final String TEST_RODDY_PLUGIN_VERSION_VERSION_PART = '1.2.3'
    static final String TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2 = '1.2.4'
    static final String TEST_RODDY_PLUGIN_VERSION = "${TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART}:${TEST_RODDY_PLUGIN_VERSION_VERSION_PART}"
    static final String TEST_RODDY_PLUGIN_VERSION_2 = "${TEST_RODDY_PLUGIN_VERSION_PLUGIN_PART}:${TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2}"
    static final String TEST_RODDY_CONFIG_FILE_NAME = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${TEST_RODDY_PLUGIN_VERSION_VERSION_PART}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_FILE_NAME_PLUGIN_VERSION_2 = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${TEST_RODDY_PLUGIN_VERSION_VERSION_PART_2}_${DomainFactory.TEST_CONFIG_VERSION}.xml"
    static final String TEST_RODDY_CONFIG_LABEL_IN_FILE = "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_${TEST_RODDY_SEQ_TYPE_RODDY_NAME}_${TEST_RODDY_PLUGIN_VERSION}_${DomainFactory.TEST_CONFIG_VERSION}"

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
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The project is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(null, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_SeqTypeIsNull_ShouldFail() {
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Project project = DomainFactory.createProject()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The seqType is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, null, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_PipelineIsNull_ShouldFail() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The pipeline is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, null, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_PluginVersionToUseIsNull_ShouldFail() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The pluginVersionToUse is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, seqType, null, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_ConfigFilePathIsNull_ShouldFail() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The configFilePath is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, null, DomainFactory.TEST_CONFIG_VERSION)
        }
    }

    @Test
    void testImportProjectConfigFile_ConfigVersionIsBlank_ShouldFail() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The configVersion is not allowed to be null') {
            RoddyWorkflowConfig.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, '')
        }
    }

    @Test
    void testImportProjectConfigFile_NoPreviousRoddyWorkflowConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        assert RoddyWorkflowConfig.list().size() == 0
        RoddyWorkflowConfig.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        assert roddyWorkflowConfig.project == project
        assert roddyWorkflowConfig.seqType == seqType
        assert roddyWorkflowConfig.pipeline == pipeline
        assert roddyWorkflowConfig.configFilePath == configFile.path
        assert roddyWorkflowConfig.pluginVersion == TEST_RODDY_PLUGIN_VERSION
        assert roddyWorkflowConfig.previousConfig == null
        assert roddyWorkflowConfig.configVersion == DomainFactory.TEST_CONFIG_VERSION
        assert roddyWorkflowConfig.individual == null
    }

    @Test
    void testImportProjectConfigFile_WithIndividual_NoPreviousRoddyWorkflowConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)

        assert RoddyWorkflowConfig.list().size() == 0
        configFile = new File(configDir, "${individual.pid}/${TEST_RODDY_CONFIG_FILE_NAME}")
        CreateFileHelper.createRoddyWorkflowConfig(configFile, TEST_RODDY_CONFIG_LABEL_IN_FILE)
        RoddyWorkflowConfig.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION, false, individual)
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.list())
        assert roddyWorkflowConfig.individual == individual
    }


    @Test
    void testImportProjectConfigFile_PreviousRoddyWorkflowConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(project: project, seqType: seqType, pipeline: pipeline, pluginVersion: TEST_RODDY_PLUGIN_VERSION_2, configVersion: DomainFactory.TEST_CONFIG_VERSION)
        assert RoddyWorkflowConfig.list().size() == 1
        RoddyWorkflowConfig.importProjectConfigFile(project, seqType, TEST_RODDY_PLUGIN_VERSION, pipeline, configFile.path, DomainFactory.TEST_CONFIG_VERSION)
        assert RoddyWorkflowConfig.list().size() == 2
        RoddyWorkflowConfig roddyWorkflowConfig2 = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByPluginVersion(TEST_RODDY_PLUGIN_VERSION))
        assert roddyWorkflowConfig2.previousConfig == roddyWorkflowConfig1
        assert roddyWorkflowConfig1.obsoleteDate
    }


    @Test
    void testGetLatestForProject_ProjectIsNull_ShouldFail() {
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The project is not allowed to be null') {
            RoddyWorkflowConfig.getLatestForProject(null, seqType, pipeline)
        }
    }

    @Test
    void testGetLatestForProject_SeqTypeIsNull_ShouldFail() {
        Project project = DomainFactory.createProject()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The seqType is not allowed to be null') {
            RoddyWorkflowConfig.getLatestForProject(project, null, pipeline)
        }
    }

    @Test
    void testGetLatestForProject_PipelineIsNull_ShouldFail() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The pipeline is not allowed to be null') {
            RoddyWorkflowConfig.getLatestForProject(project, seqType, null)
        }
    }

    @Test
    void testGetLatestForProject_ThereIsNoConfigFileInTheDatabase() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        assert !RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
    }


    @Test
    void testGetLatestForProject_OneRoddyWorkflowConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
        ])
        assert RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline) == roddyWorkflowConfig
    }


    @Test
    void testGetLatestForProject_OneActiveAndOneObsoleteRoddyWorkflowConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        RoddyWorkflowConfig roddyWorkflowConfig1 = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                obsoleteDate: new Date(),
        )
        RoddyWorkflowConfig roddyWorkflowConfig2 = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                previousConfig: roddyWorkflowConfig1,
        )
        assert RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline) == roddyWorkflowConfig2
    }


    @Test
    void testGetLatest_ConfigForIndividualAndDefaultConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatest_ConfigForTwoDifferentIndividualsExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                individual: DomainFactory.createIndividual(project: project),
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatest_TwoConfigForOneDifferentIndividualExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                individual: individual,
                obsoleteDate: new Date(),
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }


    @Test
    void testGetLatestForIndividual_ConfigForIndividualExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatestForIndividual_ConfigForIndividualAndDefaultConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatestForIndividual_OnlyDefaultConfigExists() {
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                seqType: seqType,
                pipeline: pipeline,
        ])

        assert RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfig
    }


    @Test
    void testCreateConfigPerProject_PreviousConfigExists() {
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Project project = DomainFactory.createProject()
        ConfigPerProject firstConfigPerProject = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                seqType: seqType,
                pipeline: pipeline,
        )

        ConfigPerProject newConfigPerProject = DomainFactory.createRoddyWorkflowConfig([
                project: project,
                pipeline: pipeline,
                seqType: seqType,
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
