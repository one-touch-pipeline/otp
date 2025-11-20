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
package de.dkfz.tbi.otp.workflow.alignment

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.cellRanger.CellRangerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

abstract class AlignmentWorkflowSharedSpec<T extends AbstractBamFile> extends Specification
    implements WorkflowSystemDomainFactory, DataTest {

    protected WorkflowStep workflowStep
    protected AlignmentWorkflowShared<T> alignmentWorkflowSharedInstance
    protected final List<String> alignmentWorkflowNames = [
            CellRangerWorkflow.WORKFLOW,
            PanCancerWorkflow.WORKFLOW,
            RnaAlignmentWorkflow.WORKFLOW,
            WgbsWorkflow.WORKFLOW,
    ]

    abstract AlignmentWorkflowShared<T> createSharedInstance()
    abstract T createBamFile()
    abstract String getWorkflowName()
    abstract String getInputFastqConstant()
    abstract String getOutputBamConstant()

    private void createData() {
        alignmentWorkflowSharedInstance = createSharedInstance()
        alignmentWorkflowSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
            workflow: createWorkflow([name: workflowName])
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getSeqTracks should call checkWorkflowName and getInputArtefacts with correct arguments"() {
        given:
        createData()

        when:
        alignmentWorkflowSharedInstance.getSeqTracks(workflowStep)

        then:
        1 * alignmentWorkflowSharedInstance.checkWorkflowName(workflowStep, alignmentWorkflowNames)

        then:
        1 * alignmentWorkflowSharedInstance.concreteArtefactService.getInputArtefacts(workflowStep, inputFastqConstant) >> _
    }

    void "getBamFile should call checkWorkflowName and getOutputArtefact with correct arguments"() {
        given:
        createData()
        T bamFile = createBamFile()

        when:
        alignmentWorkflowSharedInstance.getBamFile(workflowStep)

        then:
        1 * alignmentWorkflowSharedInstance.checkWorkflowName(workflowStep, alignmentWorkflowNames)

        then:
        1 * alignmentWorkflowSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, outputBamConstant) >> bamFile
    }
}
