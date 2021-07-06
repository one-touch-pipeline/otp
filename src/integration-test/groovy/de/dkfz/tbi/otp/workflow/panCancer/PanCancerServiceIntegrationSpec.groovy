/*
 * Copyright 2011-2021 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.shared.NoArtefactOfRoleException
import de.dkfz.tbi.otp.workflow.shared.NoConcreteArtefactException
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Integration
@Rollback
class PanCancerServiceIntegrationSpec extends Specification implements RoddyPancanFactory, WorkflowSystemDomainFactory {

    PanCancerService panCancerService

    void "getInputArtefacts, when called for two FastQc steps, should return both in a list"() {
        given:
        final String inputRoleName = "FASTQC"
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        final WorkflowArtefact workflowArtefact1 = createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : "${inputRoleName}_0",
                workflowArtefact: workflowArtefact1,
        ])

        SeqTrack seqTrack1 = createSeqTrack([
                workflowArtefact: workflowArtefact1,
        ])

        final WorkflowArtefact workflowArtefact2 = createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : "${inputRoleName}_1",
                workflowArtefact: workflowArtefact2,
        ])

        SeqTrack seqTrack2 = createSeqTrack([
                workflowArtefact: workflowArtefact2,
        ])

        when:
        List<SeqTrack> inputArtefacts = panCancerService.getInputArtefacts(workflowStep, inputRoleName)

        then:
        inputArtefacts == [seqTrack1, seqTrack2]
    }

    void "getInputArtefacts, when called for two FastQ steps, should return both in a list"() {
        given:
        final String inputRoleName = "FASTQ"
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        final WorkflowArtefact workflowArtefact1 = createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : "${inputRoleName}_0",
                workflowArtefact: workflowArtefact1,
        ])

        FastqcProcessedFile fastqcProcessedFile1 = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: workflowArtefact1,
        ])

        final WorkflowArtefact workflowArtefact2 = createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : "${inputRoleName}_1",
                workflowArtefact: workflowArtefact2,
        ])

        FastqcProcessedFile fastqcProcessedFile2 = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: workflowArtefact2,
        ])

        when:
        List<FastqcProcessedFile> inputArtefacts = panCancerService.getInputArtefacts(workflowStep, inputRoleName)

        then:
        inputArtefacts == [fastqcProcessedFile1, fastqcProcessedFile2]
    }

    @Unroll
    void "getInputArtefacts, when called for a #inputRoleName step with unexpected role, then throw NoArtefactOfRoleException"() {
        given:
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                producedBy: workflowRun,
                outputRole: "role_${nextId}",
        ])
        DomainFactory.createFastqcProcessedFile([
                workflowArtefact: workflowArtefact,
        ])

        when:
        panCancerService.getInputArtefacts(workflowStep, inputRoleName)

        then:
        thrown(NoArtefactOfRoleException)

        where:
        inputRoleName << ["FASTQC", "FASTQ"]
    }

    @Unroll
    void "getInputArtefacts, when called for #inputRoleName step with expected role, but without concrete artefact, then throw NoConcreteArtefactException"() {
        given:
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun: workflowRun,
                role       : inputRoleName,
        ])

        when:
        panCancerService.getInputArtefacts(workflowStep, inputRoleName)

        then:
        thrown(NoConcreteArtefactException)

        where:
        inputRoleName << ["FASTQC", "FASTQ"]
    }

    void "getInputArtefact, when called for a #inputRoleName step, should return SeqTrack"() {
        given:
        final String inputRoleName = "FASTQ"
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        final WorkflowArtefact workflowArtefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : inputRoleName,
                workflowArtefact: workflowArtefact,
        ])

        SeqTrack seqTrack = createSeqTrack([
                workflowArtefact: workflowArtefact,
        ])

        when:
        SeqTrack inputArtefacts = panCancerService.getInputArtefact(workflowStep, inputRoleName)

        then:
        inputArtefacts == seqTrack
    }

    void "getInputArtefact, when called for a #inputRoleName step, should return FastqcProcessedFile"() {
        given:
        final String inputRoleName = "FASTQC"
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        final WorkflowArtefact workflowArtefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : inputRoleName,
                workflowArtefact: workflowArtefact,
        ])

        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: workflowArtefact,
        ])

        when:
        FastqcProcessedFile inputArtefacts = panCancerService.getInputArtefact(workflowStep, inputRoleName)

        then:
        inputArtefacts == fastqcProcessedFile
    }

    void "getInputArtefact, when called for a #inputRoleName step, should return RoddyBamFile"() {
        given:
        final String inputRoleName = "BAM"
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        final WorkflowArtefact workflowArtefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact([
                workflowRun     : workflowRun,
                role            : inputRoleName,
                workflowArtefact: workflowArtefact,
        ])

        RoddyBamFile roddyBamFile = createBamFile([
                workflowArtefact: workflowArtefact,
        ])

        when:
        RoddyBamFile inputArtefacts = panCancerService.getInputArtefact(workflowStep, inputRoleName)

        then:
        inputArtefacts == roddyBamFile
    }

    void "getOutputArtefact, when called with a RoddyBamFile in output, should return RoddyBamFile"() {
        given:
        final String outputRoleName = "BAM"
        final WorkflowRun workflowRun = createWorkflowRun([
                workflow: createWorkflow([
                        name: PanCancerService.WORKFLOW,
                ]),
        ])
        final WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: workflowRun,
        ])
        final WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                producedBy: workflowRun,
                outputRole: outputRoleName,
        ])
        RoddyBamFile roddyBamFile = createBamFile([
                workflowArtefact: workflowArtefact,
        ])

        when:
        RoddyBamFile outputWorkflowArtefacts = panCancerService.getOutputArtefact(workflowStep, outputRoleName)

        then:
        outputWorkflowArtefacts == roddyBamFile
    }
}
