/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesCommonName
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

class WorkflowSystemConfigControllerSpec extends Specification implements ControllerUnitTest<WorkflowSystemConfigController>, DataTest,
        DomainFactoryCore, WorkflowSystemDomainFactory, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                ReferenceGenome,
                SeqType,
                Species,
                SpeciesCommonName,
                SpeciesWithStrain,
                Workflow,
                WorkflowApiVersion,
                WorkflowDefaultGroup,
                WorkflowVersion,
                WorkflowVersionSelectorDefault,
        ]
    }

    void setupData() {
        controller.workflowDefaultGroupService = Mock(WorkflowDefaultGroupService)
    }

    void "getWorkflowVersions, should include isDefault and displayName for each version"() {
        given:
        setupData()
        Workflow workflow = createWorkflow()
        WorkflowVersion defaultVersion = createWorkflowVersion(
                apiVersion: createWorkflowApiVersion(workflow: workflow), allowedReferenceGenomes: [] as Set, supportedSeqTypes: [] as Set
        )
        WorkflowVersion otherVersion = createWorkflowVersion(
                apiVersion: createWorkflowApiVersion(workflow: workflow), allowedReferenceGenomes: [] as Set, supportedSeqTypes: [] as Set
        )
        workflow.defaultVersion = defaultVersion
        workflow.save(flush: true)
        controller.workflowVersionService = new WorkflowVersionService()

        when:
        controller.getWorkflowVersions(workflow.id)

        then:
        Map defaultVersionJson = response.json.find { it.id == defaultVersion.id }
        defaultVersionJson.isDefault
        defaultVersionJson.displayName.contains("(default)")

        Map otherVersionJson = response.json.find { it.id == otherVersion.id }
        !otherVersionJson.isDefault
        !otherVersionJson.displayName.contains("(default)")
    }

    @SuppressWarnings('UnnecessaryGetter')
    void "getWorkflowDefaultGroups, should render the flattened rows from the service"() {
        given:
        setupData()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelectorDefault entry = createWorkflowVersionSelectorDefault(group: group)

        when:
        controller.getWorkflowDefaultGroups()

        then:
        1 * controller.workflowDefaultGroupService.list() >> [group]
        1 * controller.workflowDefaultGroupService.findAllEntries() >> [entry]
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.size() == 1
        response.json[0].groupId == group.id
        response.json[0].name == group.name
        response.json[0].workflow.id == entry.workflowVersion.workflow.id
        response.json[0].workflowVersion.id == entry.workflowVersion.id
    }

    void "saveWorkflowDefaultGroup, when group is absent, should create a new group with all given workflow versions"() {
        given:
        setupData()
        SeqType seqType = createSeqType()
        WorkflowVersion version1 = createWorkflowVersion()
        WorkflowVersion version2 = createWorkflowVersion()
        WorkflowDefaultGroup createdGroup = createWorkflowDefaultGroup(name: "someName", seqType: seqType)

        when:
        request.method = 'POST'
        params['name'] = "someName"
        params['seqType.id'] = seqType.id
        params['workflowVersion'] = [version1.id.toString(), version2.id.toString()]
        controller.saveWorkflowDefaultGroup()

        then:
        1 * controller.workflowDefaultGroupService.saveGroup(null, "someName", seqType, [version1, version2] as Set) >> createdGroup
        1 * controller.workflowDefaultGroupService.findEntries(createdGroup) >> []
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.name == "someName"
        response.json.seqType.id == seqType.id
    }

    void "saveWorkflowDefaultGroup, when group is given, should update the existing group"() {
        given:
        setupData()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        SeqType newSeqType = createSeqType()
        WorkflowVersion version = createWorkflowVersion()

        when:
        request.method = 'POST'
        params['group.id'] = group.id
        params['name'] = "newName"
        params['seqType.id'] = newSeqType.id
        params['workflowVersion'] = [version.id.toString()]
        controller.saveWorkflowDefaultGroup()

        then:
        1 * controller.workflowDefaultGroupService.saveGroup(group, "newName", newSeqType, [version] as Set) >> group
        1 * controller.workflowDefaultGroupService.findEntries(group) >> []
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.id == group.id
    }

    void "saveWorkflowDefaultGroup, when required fields are missing, should respond 406"() {
        given:
        setupData()

        when:
        request.method = 'POST'
        controller.saveWorkflowDefaultGroup()

        then:
        0 * controller.workflowDefaultGroupService._
        response.status == 406
    }

    void "saveWorkflowDefaultGroup, when a workflowVersion id is unknown, should respond 406"() {
        given:
        setupData()
        SeqType seqType = createSeqType()

        when:
        request.method = 'POST'
        params['name'] = "someName"
        params['seqType.id'] = seqType.id
        params['workflowVersion'] = ["-1"]
        controller.saveWorkflowDefaultGroup()

        then:
        0 * controller.workflowDefaultGroupService._
        response.status == 406
    }

    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "deleteWorkflowDefaultGroup, should call the service and render an empty response"() {
        given:
        setupData()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()

        when:
        request.method = 'POST'
        params['group.id'] = group.id
        controller.deleteWorkflowDefaultGroup()

        then:
        1 * controller.workflowDefaultGroupService.delete(group)
        0 * controller.workflowDefaultGroupService._
        response.status == 200
    }
}
