package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*

class RoddyWorkflowConfigTests {

    static final String TEST_RODDY_SEQ_TYPE_RODDY_NAME = 'roddyName'

    RoddyWorkflowConfigService service

    @Before
    void setUp() {
        service = new RoddyWorkflowConfigService()
        service.fileSystemService = new TestFileSystemService()
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
