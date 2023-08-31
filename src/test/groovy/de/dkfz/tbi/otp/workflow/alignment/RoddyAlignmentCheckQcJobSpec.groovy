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
package de.dkfz.tbi.otp.workflow.alignment

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightNotificationService
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

class RoddyAlignmentCheckQcJobSpec extends Specification implements WorkflowSystemDomainFactory, DataTest, RoddyPancanFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                RoddyBamFile,
                RoddyMergedBamQa,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
        ]
    }

    @Unroll
    void "execute, when result of threshold check is #checkSuccessful change workflow status to corresponding state, if false send mail"() {
        given:
        final String inputRoleName = "BAM"
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerWorkflow.WORKFLOW
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([workflowRun: run])
        final RoddyBamFile bamFile = createRoddyBamFile(RoddyBamFile)
        final RoddyMergedBamQa qa = createQa(bamFile)
        final RoddyAlignmentCheckQcJob job = new RoddyAlignmentCheckQcJob()

        job.qcTrafficLightService = Mock(QcTrafficLightService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.concreteArtefactService = Mock(ConcreteArtefactService)
        job.qcTrafficLightNotificationService = Mock(QcTrafficLightNotificationService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, inputRoleName) >> bamFile
        1 * job.qcTrafficLightService.setQcTrafficLightStatusBasedOnThresholdAndProjectSpecificHandling(bamFile, qa) >> {
            // only warning or not blocked are relevant for setting the workflow status
            if (checkSuccessful) {
                bamFile.qcTrafficLightStatus = AbstractBamFile.QcTrafficLightStatus.QC_PASSED
            } else {
                bamFile.qcTrafficLightStatus = AbstractBamFile.QcTrafficLightStatus.WARNING
            }
        }
        successCalls * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        sendwarningMail * job.qcTrafficLightNotificationService.informResultsAreWarned(bamFile)
        0 * _

        where:
        checkSuccessful || successCalls | sendwarningMail
        true            || 1            | 0
        false           || 1            | 1
    }
}
