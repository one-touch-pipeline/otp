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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class PanCancerSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    private WorkflowStep workflowStep
    private PanCancerShared panCancerSharedInstance

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
        ]
    }

    private void createData() {
        panCancerSharedInstance = Spy(PanCancerSharedInstance)
        panCancerSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerWorkflow.WORKFLOW
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getSeqTracks, should call checkWorkflowName and getInputArtefacts with correct arguments and in order"() {
        given:
        createData()

        when:
        panCancerSharedInstance.getSeqTracks(workflowStep)

        then:
        1 * panCancerSharedInstance.checkWorkflowName(workflowStep, [PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WORKFLOW]) >> _

        then:
        1 * panCancerSharedInstance.concreteArtefactService.getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQ) >> _
    }

    void "getFastqcProcessedFiles, should call checkWorkflowName and getInputArtefacts with correct arguments and in order"() {
        given:
        createData()

        when:
        panCancerSharedInstance.getFastqcProcessedFiles(workflowStep)

        then:
        1 * panCancerSharedInstance.checkWorkflowName(workflowStep, [PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WORKFLOW]) >> _

        then:
        1 * panCancerSharedInstance.concreteArtefactService.getInputArtefacts(workflowStep, PanCancerWorkflow.INPUT_FASTQC) >> _
    }

    void "getBaseRoddyBamFile, should call checkWorkflowName and getInputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        panCancerSharedInstance.getBaseRoddyBamFile(workflowStep)

        then:
        1 * panCancerSharedInstance.checkWorkflowName(workflowStep, [PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WORKFLOW]) >> _

        then:
        1 * panCancerSharedInstance.concreteArtefactService.getInputArtefact(workflowStep, PanCancerWorkflow.INPUT_BASE_BAM_FILE, false) >> _
    }

    void "getRoddyBamFile, should call checkWorkflowName and getInputArtefact with correct arguments and in order"() {
        given:
        createData()

        when:
        panCancerSharedInstance.getRoddyBamFile(workflowStep)

        then:
        1 * panCancerSharedInstance.checkWorkflowName(workflowStep, [PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WORKFLOW]) >> _

        then:
        1 * panCancerSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> _
    }

    @SuppressWarnings('EmptyClass')
    class PanCancerSharedInstance implements PanCancerShared { }
}
