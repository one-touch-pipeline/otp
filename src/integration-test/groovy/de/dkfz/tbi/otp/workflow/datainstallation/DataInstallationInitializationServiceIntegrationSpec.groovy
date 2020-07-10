/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.workflow.datainstallation

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.shared.*
import de.dkfz.tbi.otp.workflowExecution.*

@Rollback
@Integration
class DataInstallationInitializationServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    void "getSeqTrack, when called for a data installation step, then return the SeqTrack"() {
        given:
        DataInstallationInitializationService service = new DataInstallationInitializationService()

        WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: DataInstallationInitializationService.WORKFLOW,
                ]),
        ])
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact = createWorkflowArtefact([
                producedBy: run,
                outputRole: DataInstallationInitializationService.OUTPUT_ROLE,
        ])
        SeqTrack seqTrack = createSeqTrack([
                workflowArtefact: artefact,
        ])

        when:
        SeqTrack returned = service.getSeqTrack(step)

        then:
        returned == seqTrack
    }

    void "getSeqTrack, when called for a non data installation step, then throw WrongWorkflowException"() {
        given:
        DataInstallationInitializationService service = new DataInstallationInitializationService()

        WorkflowStep step = createWorkflowStep()

        when:
        service.getSeqTrack(step)

        then:
        thrown(WrongWorkflowException)
    }

    void "getSeqTrack, when called for a data installation step without expected role, then throw NoArtefactOfRoleException"() {
        given:
        DataInstallationInitializationService service = new DataInstallationInitializationService()

        WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: DataInstallationInitializationService.WORKFLOW,
                ]),
        ])
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact = createWorkflowArtefact([
                producedBy: run,
                outputRole: "role_${nextId}",
        ])
        createSeqTrack([
                workflowArtefact: artefact,
        ])

        when:
        service.getSeqTrack(step)

        then:
        thrown(NoArtefactOfRoleException)
    }

    void "getSeqTrack, when called for a data installation step with expected role, but without concrete artefact, then throw NoConcreteArtefactException"() {
        given:
        DataInstallationInitializationService service = new DataInstallationInitializationService()

        WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: DataInstallationInitializationService.WORKFLOW,
                ]),
        ])
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        createWorkflowArtefact([
                producedBy: run,
                outputRole: DataInstallationInitializationService.OUTPUT_ROLE,
        ])

        when:
        service.getSeqTrack(step)

        then:
        thrown(NoConcreteArtefactException)
    }

}
