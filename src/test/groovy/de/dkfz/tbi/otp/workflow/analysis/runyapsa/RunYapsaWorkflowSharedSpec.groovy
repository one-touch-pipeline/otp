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
package de.dkfz.tbi.otp.workflow.analysis.runyapsa

import grails.testing.gorm.DataTest
import spock.lang.Specification
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class RunYapsaWorkflowSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    private WorkflowStep workflowStep
    private RunYapsaWorkflowShared runYapsaWorkflowSharedInstance
    private RoddySnvCallingInstance snvCallingInstance
    private IndelCallingInstance indelInstance
    private RunYapsaInstance runYapsaInstance

    private static final String SNV_INPUT = "SNV"
    private static final String INDEL_INPUT = "INDEL"
    private static final String RUNYAPSA_OUTPUT = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                SeqTrack,
        ]
    }

    private void createData() {
        runYapsaWorkflowSharedInstance = Spy(RunYapsaWorkflowSharedInstance)
        snvCallingInstance = new RoddySnvCallingInstance()
        indelInstance = new IndelCallingInstance()
        runYapsaInstance = new RunYapsaInstance()
        runYapsaWorkflowSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: RunYapsaWorkflow.WORKFLOW
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getSnvInstance, should call checkWorkflowName and getInputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        runYapsaWorkflowSharedInstance.getSnvInstance(workflowStep)

        then:
        1 * runYapsaWorkflowSharedInstance.checkWorkflowName(workflowStep, RunYapsaWorkflow.WORKFLOW)
        1 * runYapsaWorkflowSharedInstance.concreteArtefactService.getInputArtefact(workflowStep, SNV_INPUT) >> snvCallingInstance
    }

    void "getIndelInstance, should call checkWorkflowName and getInputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        runYapsaWorkflowSharedInstance.getIndelInstance(workflowStep)

        then:
        1 * runYapsaWorkflowSharedInstance.checkWorkflowName(workflowStep, RunYapsaWorkflow.WORKFLOW)
        1 * runYapsaWorkflowSharedInstance.concreteArtefactService.getInputArtefact(workflowStep, INDEL_INPUT) >> indelInstance
    }

    void "getRunYapsaInstance, should call checkWorkflowName and getOutputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        runYapsaWorkflowSharedInstance.getRunYapsaInstance(workflowStep)

        then:
        1 * runYapsaWorkflowSharedInstance.checkWorkflowName(workflowStep, RunYapsaWorkflow.WORKFLOW)
        1 * runYapsaWorkflowSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, RUNYAPSA_OUTPUT) >> runYapsaInstance
    }

    @SuppressWarnings('EmptyClass')
    class RunYapsaWorkflowSharedInstance implements RunYapsaWorkflowShared {
    }
}
