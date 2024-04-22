/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project

import java.time.LocalDate

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class WorkflowVersionSelectorServiceIntegrationSpec extends Specification implements DomainFactoryCore, WorkflowSystemDomainFactory {
    WorkflowVersionSelectorService workflowVersionSelectorService

    void "createOrUpdate, should create new selector, when no matching selector exists"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()
        WorkflowVersion workflowVersion = createWorkflowVersion()
        createWorkflowVersionSelector(project: project, seqType: seqType, deprecationDate: LocalDate.now())
        WorkflowVersionSelector existingWvs1 = createWorkflowVersionSelector(project: project, seqType: seqType)
        WorkflowVersionSelector existingWvs2 = createWorkflowVersionSelector(project: project)
        WorkflowVersionSelector existingWvs3 = createWorkflowVersionSelector(seqType: seqType)
        assert !existingWvs1.deprecationDate
        assert !existingWvs2.deprecationDate
        assert !existingWvs3.deprecationDate

        when:
        WorkflowVersionSelector createdWorkflowVersionSelector = workflowVersionSelectorService.createOrUpdate(project, seqType, workflowVersion)

        then:
        List<WorkflowVersionSelector> foundWvs = WorkflowVersionSelector.findAllByProjectAndSeqTypeAndWorkflowVersion(project, seqType, workflowVersion)
        exactlyOneElement(foundWvs) == createdWorkflowVersionSelector
        WorkflowVersionSelector.count == 5
        WorkflowVersionSelector.findAllByDeprecationDateIsNull().size() == 4
    }

    void "createOrUpdate should update selector, when a matching selector already exists"() {
        given:
        Project project = createProject()
        SeqType seqType = createSeqType()

        WorkflowVersionSelector existing = createWorkflowVersionSelector(project: project, seqType: seqType)

        WorkflowVersion workflowVersion = createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: existing.workflowVersion.workflow))

        when:
        workflowVersionSelectorService.createOrUpdate(project, seqType, workflowVersion)

        then:
        List<WorkflowVersionSelector> selectors = WorkflowVersionSelector.findAllByProjectAndSeqType(project, seqType)
        selectors.size() == 2
        exactlyOneElement(selectors.findAll { it.deprecationDate != null }) == existing
        WorkflowVersionSelector updatedSelector = exactlyOneElement(selectors.findAll { it.deprecationDate == null })
        updatedSelector.previous == existing
        updatedSelector.workflowVersion == workflowVersion
    }
}
