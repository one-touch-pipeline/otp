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
package de.dkfz.tbi.otp.workflow

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.springframework.beans.factory.annotation.Autowired
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

@Rollback
@Integration
class ConcreteArtefactServiceIntegrationSpec extends Specification implements RoddyPancanFactory, WorkflowSystemDomainFactory {

    @Autowired
    ConcreteArtefactService service

    static final String INPUT_ROLE = "INPUT_ROLE"
    static final String INPUT_ROLE1 = "INPUT_ROLE1"
    static final String INPUT_ROLE2 = "INPUT_ROLE2"
    static final String INPUT_ROLE3 = "INPUT_ROLE3"
    static final String INPUT_ROLE1_1 = "INPUT_ROLE1_1"
    static final String INPUT_ROLE1_2 = "INPUT_ROLE1_2"
    static final String INPUT_ROLE2_1 = "INPUT_ROLE2_1"
    static final String INPUT_ROLE2_2 = "INPUT_ROLE2_2"
    static final String INPUT_ROLE3_1 = "INPUT_ROLE3_1"
    static final String INPUT_ROLE3_2 = "INPUT_ROLE3_2"
    static final String OUTPUT_ROLE = "OUTPUT_ROLE"
    static final String OUTPUT_ROLE1 = "OUTPUT_ROLE1"
    static final String OUTPUT_ROLE2 = "OUTPUT_ROLE2"
    static final String OUTPUT_ROLE3 = "OUTPUT_ROLE3"
    static final String OUTPUT_ROLE1_1 = "OUTPUT_ROLE1_1"
    static final String OUTPUT_ROLE1_2 = "OUTPUT_ROLE1_2"
    static final String OUTPUT_ROLE2_1 = "OUTPUT_ROLE2_1"
    static final String OUTPUT_ROLE2_2 = "OUTPUT_ROLE2_2"
    static final String OUTPUT_ROLE3_1 = "OUTPUT_ROLE3_1"
    static final String OUTPUT_ROLE3_2 = "OUTPUT_ROLE3_2"

    @Unroll
    void "getInputArtefact, when called for right workflow and right step with required = #required, should return right artefact"() {
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE,
                workflowArtefact: artefact,
        )
        SeqTrack seqTrack = createSeqTrack([
                workflowArtefact: artefact,
        ])

        when:
        SeqTrack returned = service.getInputArtefact(step, INPUT_ROLE, required)

        then:
        returned == seqTrack

        where:
        required << [true, false]
    }

    void "getInputArtefact, when called for workflow with step without expected role and artefact required, then throw NoArtefactOfRoleException"() {
        given:
        boolean required = true
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: "role_${nextId}",
                workflowArtefact: artefact,
        )
        createSeqTrack([
                workflowArtefact: artefact,
        ])

        when:
        service.getInputArtefact(step, INPUT_ROLE, required)

        then:
        thrown(NoArtefactOfRoleException)
    }

    void "getInputArtefact, when called for workflow with step without expected role and artefact not required, then return null"() {
        given:
        boolean required = false
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: "role_${nextId}",
                workflowArtefact: artefact,
        )
        createSeqTrack([
                workflowArtefact: artefact,
        ])

        when:
        SeqTrack receivedArtefact = service.getInputArtefact(step, INPUT_ROLE, required)

        then:
        receivedArtefact == null
    }

    void "getInputArtefacts, when called for workflow with step without expected role and artefact not required, then return empty list"() {
        given:
        boolean required = false
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: "role_${nextId}",
                workflowArtefact: artefact,
        )
        createSeqTrack([
                workflowArtefact: artefact,
        ])

        when:
        List<SeqTrack> receivedArtefacts = service.getInputArtefacts(step, INPUT_ROLE, required)

        then:
        receivedArtefacts == []
    }

    @Unroll
    void "getInputArtefact, when called for workflow with step without expected role with required = #required, but without concrete artefact, then throw NoConcreteArtefactException"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE,
        )

        when:
        service.getInputArtefact(step, INPUT_ROLE)

        then:
        thrown(NoConcreteArtefactException)

        where:
        required << [true, false]
    }

    @Unroll
    void "getOutputArtefact, when called for workflow with step without expected role with required = #required, then throw NoArtefactOfRoleException"() {
        given:
        ConcreteArtefactService service = new ConcreteArtefactService()

        WorkflowRun run = createWorkflowRun()
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
        service.getOutputArtefact(step, OUTPUT_ROLE)

        then:
        thrown(NoArtefactOfRoleException)

        where:
        required << [true, false]
    }

    void "getOutputArtefact, when called for workflow and a step with expected role, but without concrete artefact, then throw NoConcreteArtefactException"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        createWorkflowArtefact([
                producedBy: run,
                outputRole: OUTPUT_ROLE,
        ])

        when:
        service.getOutputArtefact(step, OUTPUT_ROLE)

        then:
        thrown(NoConcreteArtefactException)
    }

    @Unroll
    void "getInputArtefact, when called for Workflow and step with required = #required, should return the artefact"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact1 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE1,
                workflowArtefact: artefact1,
        )
        SeqTrack seqTrack = createSeqTrack([
                workflowArtefact: artefact1,
        ])
        WorkflowArtefact artefact2 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE2,
                workflowArtefact: artefact2,
        )
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: artefact2,
        ])
        WorkflowArtefact artefact3 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE3,
                workflowArtefact: artefact3,
        )
        RoddyBamFile roddyBamFile = createBamFile([
                workflowArtefact: artefact3,
        ])

        when:
        SeqTrack returned1 = service.getInputArtefact(step, INPUT_ROLE1, required)
        FastqcProcessedFile returned2 = service.getInputArtefact(step, INPUT_ROLE2, required)
        RoddyBamFile returned3 = service.getInputArtefact(step, INPUT_ROLE3, required)

        then:
        returned1 == seqTrack
        returned2 == fastqcProcessedFile
        returned3 == roddyBamFile

        where:
        required << [true, false]
    }

    @Unroll
    void "getInputArtefacts, when called for Workflow and steps with required = #required, should return list of artefacts"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact1_1 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE1_1,
                workflowArtefact: artefact1_1,
        )
        SeqTrack seqTrack1 = createSeqTrack([
                workflowArtefact: artefact1_1,
        ])
        WorkflowArtefact artefact1_2 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE1_2,
                workflowArtefact: artefact1_2,
        )
        SeqTrack seqTrack2 = createSeqTrack([
                workflowArtefact: artefact1_2,
        ])
        WorkflowArtefact artefact2_1 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE2_1,
                workflowArtefact: artefact2_1,
        )
        FastqcProcessedFile fastqcProcessedFile1 = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: artefact2_1,
        ])
        WorkflowArtefact artefact2_2 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE2_2,
                workflowArtefact: artefact2_2,
        )
        FastqcProcessedFile fastqcProcessedFile2 = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: artefact2_2,
        ])

        WorkflowArtefact artefact3_1 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE3_1,
                workflowArtefact: artefact3_1,
        )
        RoddyBamFile roddyBamFile1 = createBamFile([
                workflowArtefact: artefact3_1,
        ])
        WorkflowArtefact artefact3_2 = createWorkflowArtefact()
        createWorkflowRunInputArtefact(
                workflowRun: run,
                role: INPUT_ROLE3_2,
                workflowArtefact: artefact3_2,
        )
        RoddyBamFile roddyBamFile2 = createBamFile([
                workflowArtefact: artefact3_2,
        ])

        when:
        List<SeqTrack> returnedList1 = service.getInputArtefacts(step, INPUT_ROLE1, required)
        List<FastqcProcessedFile> returnedList2 = service.getInputArtefacts(step, INPUT_ROLE2, required)
        List<RoddyBamFile> returnedList3 = service.getInputArtefacts(step, INPUT_ROLE3, required)

        then:
        returnedList1 == [seqTrack1, seqTrack2]
        returnedList2 == [fastqcProcessedFile1, fastqcProcessedFile2]
        returnedList3 == [roddyBamFile1, roddyBamFile2]

        where:
        required << [true, false]
    }

    @Unroll
    void "getOutputArtefacts, when called for Workflow and steps with required = #required, should return list of artefacts"() {
        given:
        WorkflowRun run = createWorkflowRun()
        WorkflowStep step = createWorkflowStep([
                workflowRun: run,
        ])
        WorkflowArtefact artefact1_1 = createWorkflowArtefact([
                producedBy: run,
                outputRole: OUTPUT_ROLE1_1,
        ])
        SeqTrack seqTrack1 = createSeqTrack([
                workflowArtefact: artefact1_1,
        ])
        WorkflowArtefact artefact1_2 = createWorkflowArtefact([
                producedBy: run,
                outputRole: OUTPUT_ROLE1_2,
        ])

        SeqTrack seqTrack2 = createSeqTrack([
                workflowArtefact: artefact1_2,
        ])
        WorkflowArtefact artefact2_1 = createWorkflowArtefact([
                producedBy: run,
                outputRole: OUTPUT_ROLE2_1,
        ])

        FastqcProcessedFile fastqcProcessedFile1 = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: artefact2_1,
        ])
        WorkflowArtefact artefact2_2 = createWorkflowArtefact([
                producedBy: run,
                outputRole: OUTPUT_ROLE2_2,
        ])
        FastqcProcessedFile fastqcProcessedFile2 = DomainFactory.createFastqcProcessedFile([
                workflowArtefact: artefact2_2,
        ])

        WorkflowArtefact artefact3_1 = createWorkflowArtefact([
                producedBy: run,
                outputRole: OUTPUT_ROLE3_1,
        ])
        RoddyBamFile roddyBamFile1 = createBamFile([
                workflowArtefact: artefact3_1,
        ])
        WorkflowArtefact artefact3_2 = createWorkflowArtefact([
                producedBy: run,
                outputRole: OUTPUT_ROLE3_2,
        ])
        RoddyBamFile roddyBamFile2 = createBamFile([
                workflowArtefact: artefact3_2,
        ])

        when:
        List<SeqTrack> returnedList1 = service.getOutputArtefacts(step, OUTPUT_ROLE1, required)
        List<FastqcProcessedFile> returnedList2 = service.getOutputArtefacts(step, OUTPUT_ROLE2, required)
        List<RoddyBamFile> returnedList3 = service.getOutputArtefacts(step, OUTPUT_ROLE3, required)

        then:
        returnedList1 == [seqTrack1, seqTrack2]
        returnedList2 == [fastqcProcessedFile1, fastqcProcessedFile2]
        returnedList3 == [roddyBamFile1, roddyBamFile2]

        where:
        required << [true, false]
    }
}
