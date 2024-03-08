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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflowShared
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class AceseqWorkflowSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    private WorkflowStep workflowStep
    private AceseqWorkflowShared aceseqWorkflowSharedInstance
    private SophiaInstance sophiaInstance
    private AceseqInstance aceseqInstance
    private static final String SOPHIA_INPUT = "SOPHIA"
    private static final String ACESEQ_OUTPUT = "ACESEQ"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                SeqTrack,
        ]
    }

    private void createData() {
        aceseqWorkflowSharedInstance = Spy(AceseqWorkflowSharedInstance)
        sophiaInstance = new SophiaInstance()
        aceseqInstance = new AceseqInstance()
        aceseqWorkflowSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: AceseqWorkflow.WORKFLOW
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getSophiaInstance, should call checkWorkflowName and getInputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        aceseqWorkflowSharedInstance.getSophiaInstance(workflowStep)

        then:
        1 * aceseqWorkflowSharedInstance.checkWorkflowName(workflowStep, AceseqWorkflow.WORKFLOW)

        then:
        1 * aceseqWorkflowSharedInstance.concreteArtefactService.getInputArtefact(workflowStep, SOPHIA_INPUT) >> sophiaInstance
    }

    void "getAceseqInstance, should call checkWorkflowName and getOutputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        aceseqWorkflowSharedInstance.getAceseqInstance(workflowStep)

        then:
        1 * aceseqWorkflowSharedInstance.checkWorkflowName(workflowStep, AceseqWorkflow.WORKFLOW)

        then:
        1 * aceseqWorkflowSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, ACESEQ_OUTPUT) >> aceseqInstance
    }

    @SuppressWarnings('EmptyClass')
    class AceseqWorkflowSharedInstance implements AceseqWorkflowShared { }
}
