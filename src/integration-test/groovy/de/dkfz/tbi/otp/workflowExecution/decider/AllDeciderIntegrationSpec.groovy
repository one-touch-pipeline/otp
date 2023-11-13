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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.FastqcWorkflowDomainFactory
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

@Rollback
@Integration
class AllDeciderIntegrationSpec extends Specification implements ServiceUnitTest<AllDecider>, FastqcWorkflowDomainFactory {

    void "test decide for Decider"() {
        given:
        AllDecider allDecider = new AllDecider()
        createBashFastqcWorkflowVersion()
        createWesFastqcWorkflowVersion()
        createWorkflow(name: PanCancerWorkflow.WORKFLOW)
        createWorkflow(name: WgbsWorkflow.WORKFLOW)
        createWorkflow(name: RnaAlignmentWorkflow.WORKFLOW)
        WorkflowStep workflowStep = createWorkflowStep()
        WorkflowArtefact wa1 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun,
                artefactType: ArtefactType.RUN_YAPSA)
        WorkflowArtefact wa2 = createWorkflowArtefact(state: WorkflowArtefact.State.SUCCESS, producedBy: workflowStep.workflowRun,
                artefactType: ArtefactType.RUN_YAPSA)
        Collection<WorkflowArtefact> allWorkflowArtefacts = [wa1, wa2]

        when:
        DeciderResult deciderResult = allDecider.decide(allWorkflowArtefacts, [:])

        then:
        deciderResult.newArtefacts.empty
        deciderResult.warnings.empty
    }
}
