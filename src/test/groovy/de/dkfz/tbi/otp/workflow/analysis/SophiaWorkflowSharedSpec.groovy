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

import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflowShared
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class SophiaWorkflowSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    private WorkflowStep workflowStep
    private SophiaWorkflowShared sophiaWorkflowSharedInstance
    private SophiaInstance sophiaInstance
    private static final String SOPHIA_OUTPUT = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                SeqTrack,
        ]
    }

    private void createData() {
        sophiaWorkflowSharedInstance = Spy(SophiaWorkflowSharedInstance)
        sophiaInstance = new SophiaInstance()
        sophiaWorkflowSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: SophiaWorkflow.WORKFLOW
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getSophiaInstance, should call checkWorkflowName and getoutputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        sophiaWorkflowSharedInstance.getSophiaInstance(workflowStep)

        then:
        1 * sophiaWorkflowSharedInstance.checkWorkflowName(workflowStep, SophiaWorkflow.WORKFLOW)

        then:
        1 * sophiaWorkflowSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, SOPHIA_OUTPUT) >> sophiaInstance
    }

    @SuppressWarnings('EmptyClass')
    class SophiaWorkflowSharedInstance implements SophiaWorkflowShared { }
}
