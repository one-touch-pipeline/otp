/*
 * Copyright 2011-2022 The OTP authors
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

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class WorkflowVersionSelectorServiceSpec extends Specification implements ServiceUnitTest<WorkflowVersionSelectorService>, DataTest, DomainFactoryCore, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                Realm,
                SeqType,
                Workflow,
                WorkflowVersion,
                WorkflowVersionSelector,
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
}
