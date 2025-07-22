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
package de.dkfz.tbi.otp.workflowExecution.decider.analysis

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.ArtefactType
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowService
import de.dkfz.tbi.otp.workflowExecution.decider.*

class AbstractWorkflowDeciderSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowArtefact,
        ]
    }

    void "decide, when deciderAction=SKIP, then no artefacts are created and a skip info is added"() {
        given:
        Decider decider = new PanCancerDecider()
        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                artefactType: ArtefactType.FASTQ,
        ])
        decider.workflowService = Mock(WorkflowService) {
            1 * getExactlyOneWorkflow(PanCancerWorkflow.WORKFLOW) >> { createWorkflow(name: PanCancerWorkflow.WORKFLOW) }
            0 * _
        }

        when:
        DeciderResult deciderResult = decider.decide([workflowArtefact], ['PanCancerDecider': DeciderCreateWorkflowActions.SKIP.toString()])

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.empty
        deciderResult.infos.any {
            it.contains('Skipping creating runs')
        }
    }
}
