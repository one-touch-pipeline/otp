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
package de.dkfz.tbi.otp.workflow.bamImport

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.domainFactory.workflowSystem.BamImportWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.AntibodyTarget
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class BamImportSharedSpec extends Specification implements BamImportWorkflowDomainFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                AntibodyTarget,
        ]
    }

    void "getBamFile, should call checkWorkflowName and getOutputArtefact with correct arguments and in order"() {
        given:
        final BamImportSharedInstance bamImportSharedInstance = Spy(BamImportSharedInstance)
        bamImportSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: BamImportWorkflow.WORKFLOW
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])
        ExternallyProcessedBamFile bamFile = createBamFile()

        when:
        bamImportSharedInstance.getBamFile(workflowStep)

        then:
        1 * bamImportSharedInstance.checkWorkflowName(workflowStep, BamImportWorkflow.WORKFLOW)

        then:
        1 * bamImportSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, BamImportWorkflow.OUTPUT_BAM) >> bamFile
    }

    @SuppressWarnings('EmptyClass')
    class BamImportSharedInstance implements BamImportShared { }
}
