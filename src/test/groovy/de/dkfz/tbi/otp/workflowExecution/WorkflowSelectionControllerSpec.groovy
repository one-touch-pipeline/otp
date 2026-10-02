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

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesCommonName
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.Project

class WorkflowSelectionControllerSpec extends Specification implements ControllerUnitTest<WorkflowSelectionController>, DataTest,
        DomainFactoryCore, WorkflowSystemDomainFactory, TaxonomyFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                ReferenceGenome,
                ReferenceGenomeSelector,
                SeqType,
                Species,
                SpeciesCommonName,
                SpeciesWithStrain,
                Workflow,
                WorkflowApiVersion,
                WorkflowDefaultGroup,
                WorkflowVersion,
                WorkflowVersionSelector,
                WorkflowVersionSelectorDefault,
        ]
    }

    void setupData() {
        controller.workflowDefaultGroupService = Mock(WorkflowDefaultGroupService)
        controller.projectSelectionService = Mock(ProjectSelectionService)
    }

    void "searchWorkflowDefaultGroups, when seqType is given, should search by seqType"() {
        given:
        setupData()
        SeqType seqType = createSeqType()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup(seqType: seqType)

        when:
        request.method = 'POST'
        params['seqType.id'] = seqType.id
        controller.searchWorkflowDefaultGroups()

        then:
        1 * controller.workflowDefaultGroupService.findAllBySeqType(seqType) >> [group]
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.size() == 1
        response.json[0].id == group.id
        response.json[0].text == "${group.name} (${group.seqType.displayNameWithLibraryLayout})".toString()
    }

    void "searchWorkflowDefaultGroups, when no seqType is given, should search by free-text query"() {
        given:
        setupData()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()

        when:
        request.method = 'POST'
        params['query'] = "wgs"
        controller.searchWorkflowDefaultGroups()

        then:
        1 * controller.workflowDefaultGroupService.search("wgs") >> [group]
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.size() == 1
        response.json[0].id == group.id
    }

    void "applyWorkflowDefaultGroup, should apply the group to the requested project and render each applied entry"() {
        given:
        setupData()
        Project project = createProject()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelector wvSelector = createWorkflowVersionSelector(project: project)
        AppliedDefaultEntry appliedEntry = new AppliedDefaultEntry(wvSelector, null)

        when:
        request.method = 'POST'
        params['group.id'] = group.id
        controller.applyWorkflowDefaultGroup()

        then:
        1 * controller.projectSelectionService.requestedProject >> project
        1 * controller.workflowDefaultGroupService.applyToProject(project, group) >> [appliedEntry]
        0 * controller.workflowDefaultGroupService._

        and:
        response.json.size() == 1
        response.json[0].workflow.id == wvSelector.workflowVersion.workflow.id
        response.json[0].version.id == wvSelector.workflowVersion.id
        response.json[0].workflowVersionSelector.id == wvSelector.id
        response.json[0].refGenSelectorId == null
    }

    void "applyWorkflowDefaultGroup, should include reference genome fields when the applied entry has one"() {
        given:
        setupData()
        Project project = createProject()
        WorkflowDefaultGroup group = createWorkflowDefaultGroup()
        WorkflowVersionSelector wvSelector = createWorkflowVersionSelector(project: project)
        ReferenceGenomeSelector rgSelector = createReferenceGenomeSelector(project: project)
        AppliedDefaultEntry appliedEntry = new AppliedDefaultEntry(wvSelector, rgSelector)

        when:
        request.method = 'POST'
        params['group.id'] = group.id
        controller.applyWorkflowDefaultGroup()

        then:
        1 * controller.projectSelectionService.requestedProject >> project
        1 * controller.workflowDefaultGroupService.applyToProject(project, group) >> [appliedEntry]

        and:
        response.json[0].refGenSelectorId == rgSelector.id
        response.json[0].refGenome.id == rgSelector.referenceGenome.id
        response.json[0].species.size() == rgSelector.species.size()
    }

    void "applyWorkflowDefaultGroup, when group is missing, should respond 406"() {
        given:
        setupData()

        when:
        request.method = 'POST'
        controller.applyWorkflowDefaultGroup()

        then:
        0 * controller.workflowDefaultGroupService._
        response.status == 406
    }
}
