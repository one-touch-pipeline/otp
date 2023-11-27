/*
 * Copyright 2011-2023 The OTP authors
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
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class WorkflowVersionSelectorServiceSpec extends Specification implements ServiceUnitTest<WorkflowVersionSelectorService>, DataTest, DomainFactoryCore, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                SeqType,
                Workflow,
                WorkflowVersion,
                WorkflowVersionSelector,
                ReferenceGenomeSelector,
        ]
    }

    void "test createOrUpdate, if no selector exists"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        WorkflowVersion workflowVersion = createWorkflowVersion()

        when:
        service.createOrUpdate(project, seqType, workflowVersion)

        then:
        WorkflowVersionSelector selector = exactlyOneElement(WorkflowVersionSelector.findAllByProjectAndSeqType(project, seqType))
        selector.workflowVersion == workflowVersion
    }

    void "test createOrUpdate, if a matching selector exists"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()

        WorkflowVersionSelector existing = createWorkflowVersionSelector(project: project, seqType: seqType)

        WorkflowVersion workflowVersion = createWorkflowVersion(workflow: existing.workflowVersion.workflow)

        when:
        service.createOrUpdate(project, seqType, workflowVersion)

        then:
        List<WorkflowVersionSelector> selectors = WorkflowVersionSelector.findAllByProjectAndSeqType(project, seqType)
        selectors.size() == 2
        exactlyOneElement(selectors.findAll { it.deprecationDate != null }) == existing
        WorkflowVersionSelector selector = exactlyOneElement(selectors.findAll { it.deprecationDate == null })
        selector.previous == existing
        selector.workflowVersion == workflowVersion
    }

    @Unroll
    void "hasAlignmentConfigForProjectAndSeqType, when project is #projectName and seqType is #seqTypeName and alignmentWorkflow is #alignmentWorkflow, then return #expect"() {
        given:
        Project project = createProject(name: 'Project_A')
        SeqType seqType = createSeqTypePaired(name: 'SeqType_A')
        createProject(name: 'Project_B')
        createSeqTypePaired(name: 'SeqType_B')

        and:
        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector([
                project: CollectionUtils.exactlyOneElement(Project.findAllByName(projectName)),
                seqType: CollectionUtils.exactlyOneElement(SeqType.findAllByName(seqTypeName)),
        ])

        and:
        Map<String, OtpWorkflow> alignmentWorkflows = alignmentWorkflow ? [(workflowVersionSelector.workflowVersion.workflow.beanName): Mock(OtpWorkflow)] : [:]
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupAlignableOtpWorkflowBeans() >> alignmentWorkflows
        }

        expect:
        service.hasAlignmentConfigForProjectAndSeqType(project, seqType) == expect

        where:
        projectName | seqTypeName | alignmentWorkflow || expect
        'Project_A' | 'SeqType_A' | true              || true
        'Project_A' | 'SeqType_A' | false             || false
        'Project_A' | 'SeqType_B' | true              || false
        'Project_A' | 'SeqType_B' | false             || false
        'Project_B' | 'SeqType_A' | true              || false
        'Project_B' | 'SeqType_A' | false             || false
        'Project_B' | 'SeqType_B' | true              || false
        'Project_B' | 'SeqType_B' | false             || false
    }

    void "deprecateSelectorIfUnused, should deprecate workflow version selector, if not used by a reference genome selector"() {
        given:
        createReferenceGenomeSelector()
        WorkflowVersionSelector workflowVersionSelector = createWorkflowVersionSelector()

        when:
        service.deprecateSelectorIfUnused(workflowVersionSelector)

        then:
        WorkflowVersionSelector.count == 1
        workflowVersionSelector.deprecationDate != null
    }

    void "deprecateSelectorIfUnused, should not deprecate workflow version selector, if  used by a reference genome selector"() {
        given:
        WorkflowVersion workflowVersion = createWorkflowVersion()
        ReferenceGenomeSelector rgSelector = createReferenceGenomeSelector([workflow: workflowVersion.workflow])
        WorkflowVersionSelector wvSelector = createWorkflowVersionSelector([
                project        : rgSelector.project,
                seqType        : rgSelector.seqType,
                workflowVersion: workflowVersion,
        ])

        when:
        service.deprecateSelectorIfUnused(wvSelector)

        then:
        WorkflowVersionSelector.all == [wvSelector]
        wvSelector.deprecationDate == null
    }
}
