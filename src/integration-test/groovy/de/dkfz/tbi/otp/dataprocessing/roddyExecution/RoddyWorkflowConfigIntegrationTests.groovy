/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class RoddyWorkflowConfigIntegrationTests {

    static final String TEST_RODDY_SEQ_TYPE_RODDY_NAME = 'roddyName'

    RoddyWorkflowConfigService service

    void setupData() {
        service = new RoddyWorkflowConfigService()
        service.fileSystemService = new TestFileSystemService()
    }

    @Test
    void testGetLatestForProject_ProjectIsNull_ShouldFail() {
        setupData()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The project is not allowed to be null') {
            RoddyWorkflowConfig.getLatestForProject(null, seqType, pipeline)
        }
    }

    @Test
    void testGetLatestForProject_SeqTypeIsNull_ShouldFail() {
        setupData()
        Project project = DomainFactory.createProject()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The seqType is not allowed to be null') {
            RoddyWorkflowConfig.getLatestForProject(project, null, pipeline)
        }
    }

    @Test
    void testGetLatestForProject_PipelineIsNull_ShouldFail() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        TestCase.shouldFailWithMessageContaining(AssertionError, 'The pipeline is not allowed to be null') {
            RoddyWorkflowConfig.getLatestForProject(project, seqType, null)
        }
    }

    @Test
    void testGetLatestForProject_ThereIsNoConfigFileInTheDatabase() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        assert !RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
    }


    @Test
    void testGetLatestForProject_OneRoddyWorkflowConfigExists() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig([
                project : project,
                seqType : seqType,
                pipeline: pipeline,
        ])
        assert RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline) == roddyWorkflowConfig
    }


    @Test
    void testGetLatestForProject_OneActiveAndOneObsoleteRoddyWorkflowConfigExists() {
        setupData()
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
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project : project,
                seqType : seqType,
                pipeline: pipeline,
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project   : project,
                seqType   : seqType,
                pipeline  : pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatest_ConfigForTwoDifferentIndividualsExists() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project   : project,
                seqType   : seqType,
                pipeline  : pipeline,
                individual: DomainFactory.createIndividual(project: project),
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project   : project,
                seqType   : seqType,
                pipeline  : pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatest_TwoConfigForOneDifferentIndividualExists() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project     : project,
                seqType     : seqType,
                pipeline    : pipeline,
                individual  : individual,
                obsoleteDate: new Date(),
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project   : project,
                seqType   : seqType,
                pipeline  : pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }


    @Test
    void testGetLatestForIndividual_ConfigForIndividualExists() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project   : project,
                seqType   : seqType,
                pipeline  : pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatestForIndividual_ConfigForIndividualAndDefaultConfigExists() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        DomainFactory.createRoddyWorkflowConfig([
                project : project,
                seqType : seqType,
                pipeline: pipeline,
        ])

        RoddyWorkflowConfig roddyWorkflowConfigIndividual = DomainFactory.createRoddyWorkflowConfig([
                project   : project,
                seqType   : seqType,
                pipeline  : pipeline,
                individual: individual,
        ])

        assert RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    @Test
    void testGetLatestForIndividual_OnlyDefaultConfigExists() {
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        Individual individual = DomainFactory.createIndividual(project: project)
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig([
                project : project,
                seqType : seqType,
                pipeline: pipeline,
        ])

        assert RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfig
    }


    @Test
    void testCreateConfigPerProject_PreviousConfigExists() {
        setupData()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Project project = DomainFactory.createProject()
        ConfigPerProjectAndSeqType firstConfigPerProject = DomainFactory.createRoddyWorkflowConfig(
                project: project,
                seqType: seqType,
                pipeline: pipeline,
        )

        ConfigPerProjectAndSeqType newConfigPerProject = DomainFactory.createRoddyWorkflowConfig([
                project       : project,
                pipeline      : pipeline,
                seqType       : seqType,
                previousConfig: firstConfigPerProject,
        ], false)

        assert !firstConfigPerProject.obsoleteDate

        service.createConfigPerProjectAndSeqType(newConfigPerProject)

        assert ConfigPerProjectAndSeqType.findAllByProject(project).size() == 2
        assert firstConfigPerProject.obsoleteDate
    }


    @Test
    void testCreateConfigPerProject_PreviousConfigDoesNotExist() {
        setupData()
        Project project = DomainFactory.createProject()
        ConfigPerProjectAndSeqType configPerProject = DomainFactory.createRoddyWorkflowConfig(
                project: project,
        )
        service.createConfigPerProjectAndSeqType(configPerProject)
        assert ConfigPerProjectAndSeqType.findAllByProject(project).size() == 1
    }


    @Test
    void testMakeObsolete() {
        setupData()
        ConfigPerProjectAndSeqType configPerProject = DomainFactory.createRoddyWorkflowConfig()
        assert !configPerProject.obsoleteDate
        service.makeObsolete(configPerProject)
        assert configPerProject.obsoleteDate
    }
}
