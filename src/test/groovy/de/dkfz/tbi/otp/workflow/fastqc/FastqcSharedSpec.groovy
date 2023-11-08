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
package de.dkfz.tbi.otp.workflow.fastqc

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class FastqcSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest {

    private WorkflowStep workflowStep
    private FastqcShared fastqcSharedInstance

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                SeqTrack,
        ]
    }

    private void createData() {
        fastqcSharedInstance = Spy(FastqcSharedInstance)
        fastqcSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: BashFastQcWorkflow.WORKFLOW
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getSeqTrack, should call checkWorkflowName and getInputArtefact with correct arguments and in order"() {
        given:
        createData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        when:
        fastqcSharedInstance.getSeqTrack(workflowStep)

        then:
        1 * fastqcSharedInstance.checkWorkflowName(workflowStep, _)

        then:
        1 * fastqcSharedInstance.concreteArtefactService.getInputArtefact(workflowStep, BashFastQcWorkflow.INPUT_FASTQ) >> seqTrack
    }

    void "getFastqcProcessedFiles, should call checkWorkflowName and getOutputArtefacts with correct arguments and in order"() {
        given:
        createData()

        when:
        fastqcSharedInstance.getFastqcProcessedFiles(workflowStep)

        then:
        1 * fastqcSharedInstance.checkWorkflowName(workflowStep, _)

        then:
        1 * fastqcSharedInstance.concreteArtefactService.getOutputArtefacts(workflowStep, BashFastQcWorkflow.OUTPUT_FASTQC) >> _
    }

    @SuppressWarnings('EmptyClass')
    class FastqcSharedInstance implements FastqcShared { }
}
