/*
 * Copyright 2011-2025 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@Rollback
@Integration
class RoddyWorkflowConfigIntegrationSpec extends Specification {

    static final String TEST_RODDY_SEQ_TYPE_RODDY_NAME = 'roddyName'

    RoddyWorkflowConfigService service

    void setupData() {
        service = new RoddyWorkflowConfigService()
        service.fileSystemService = new TestFileSystemService()
    }

    void "test getLatestForProject with null project should fail"() {
        given:
        setupData()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        RoddyWorkflowConfig.getLatestForProject(null, seqType, pipeline)

        then:
        AssertionError e = thrown()
        e.message.contains('The project is not allowed to be null')
    }

    void "test getLatestForProject with null seqType should fail"() {
        given:
        setupData()
        Project project = DomainFactory.createProject()
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        when:
        RoddyWorkflowConfig.getLatestForProject(project, null, pipeline)

        then:
        AssertionError e = thrown()
        e.message.contains('The seqType is not allowed to be null')
    }

    void "test getLatestForProject with null pipeline should fail"() {
        given:
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)

        when:
        RoddyWorkflowConfig.getLatestForProject(project, seqType, null)

        then:
        AssertionError e = thrown()
        e.message.contains('The pipeline is not allowed to be null')
    }

    void "test getLatestForProject when no config file exists in database"() {
        given:
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()

        expect:
        !RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
    }

    void "test getLatestForProject when one RoddyWorkflowConfig exists"() {
        given:
        setupData()
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType(roddyName: TEST_RODDY_SEQ_TYPE_RODDY_NAME)
        Pipeline pipeline = DomainFactory.returnOrCreateAnyPipeline()
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig([
                project : project,
                seqType : seqType,
                pipeline: pipeline,
        ])

        expect:
        RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline) == roddyWorkflowConfig
    }

    void "test getLatestForProject when one active and one obsolete RoddyWorkflowConfig exists"() {
        given:
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

        expect:
        RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline) == roddyWorkflowConfig2
    }

    void "test getLatest when config for individual and default config exists"() {
        given:
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

        expect:
        RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    void "test getLatest when config for two different individuals exists"() {
        given:
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

        expect:
        RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    void "test getLatest when two configs for one different individual exists"() {
        given:
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

        expect:
        RoddyWorkflowConfig.getLatest(project, individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    void "test getLatestForIndividual when config for individual exists"() {
        given:
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

        expect:
        RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    void "test getLatestForIndividual when config for individual and default config exists"() {
        given:
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

        expect:
        RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfigIndividual
    }

    void "test getLatestForIndividual when only default config exists"() {
        given:
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

        expect:
        RoddyWorkflowConfig.getLatestForIndividual(individual, seqType, pipeline) == roddyWorkflowConfig
    }

    void "test createConfigPerProject when previous config exists"() {
        given:
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

        expect:
        !firstConfigPerProject.obsoleteDate

        when:
        service.createConfigPerProjectAndSeqType(newConfigPerProject)

        then:
        ConfigPerProjectAndSeqType.findAllByProject(project).size() == 2
        firstConfigPerProject.obsoleteDate
    }

    void "test createConfigPerProject when previous config does not exist"() {
        given:
        setupData()
        Project project = DomainFactory.createProject()
        ConfigPerProjectAndSeqType configPerProject = DomainFactory.createRoddyWorkflowConfig(
                project: project,
        )

        when:
        service.createConfigPerProjectAndSeqType(configPerProject)

        then:
        ConfigPerProjectAndSeqType.findAllByProject(project).size() == 1
    }

    void "test makeObsolete"() {
        given:
        setupData()
        ConfigPerProjectAndSeqType configPerProject = DomainFactory.createRoddyWorkflowConfig()

        expect:
        !configPerProject.obsoleteDate

        when:
        service.makeObsolete(configPerProject)

        then:
        configPerProject.obsoleteDate
    }
}
