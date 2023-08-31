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
package de.dkfz.tbi.otp.workflow.alignment.rna

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class RnaAlignmentSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest, RoddyRnaFactory {

    private WorkflowStep workflowStep
    private RnaAlignmentShared rnaAlignmentSharedInstance
    private final List<String> alignmentWorkflowNames = [PanCancerWorkflow.WORKFLOW, WgbsWorkflow.WGBS_WORKFLOW, RnaAlignmentWorkflow.WORKFLOW]

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                WorkflowRun,
                WorkflowStep,
                RnaRoddyBamFile,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
        ]
    }

    private void createData() {
        rnaAlignmentSharedInstance = Spy(RnaAlignmentSharedInstance)
        rnaAlignmentSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: RnaAlignmentWorkflow.WORKFLOW
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getSeqTracks, should call checkWorkflowName and getInputArtefacts with correct arguments and in order"() {
        given:
        createData()

        when:
        rnaAlignmentSharedInstance.getSeqTracks(workflowStep)

        then:
        1 * rnaAlignmentSharedInstance.checkWorkflowName(workflowStep, alignmentWorkflowNames) >> _

        then:
        1 * rnaAlignmentSharedInstance.concreteArtefactService.getInputArtefacts(workflowStep, RnaAlignmentWorkflow.INPUT_FASTQ) >> _
    }

    void "getRoddyBamFile, should call checkWorkflowName and getInputArtefact with correct arguments and in order"() {
        given:
        createData()
        RnaRoddyBamFile bamFile = createBamFile()

        when:
        rnaAlignmentSharedInstance.getRoddyBamFile(workflowStep)

        then:
        1 * rnaAlignmentSharedInstance.checkWorkflowName(workflowStep, RnaAlignmentWorkflow.WORKFLOW) >> _

        then:
        1 * rnaAlignmentSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, RnaAlignmentWorkflow.OUTPUT_BAM) >> bamFile
    }

    @SuppressWarnings('EmptyClass')
    class RnaAlignmentSharedInstance implements RnaAlignmentShared { }
}
