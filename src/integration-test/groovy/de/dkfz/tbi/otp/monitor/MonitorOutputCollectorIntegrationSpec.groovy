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
package de.dkfz.tbi.otp.monitor

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.web.mapping.LinkGenerator
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflowExecution.*

@Rollback
@Integration
class MonitorOutputCollectorIntegrationSpec extends Specification implements DomainFactoryCore, WorkflowSystemDomainFactory {

    void "addInfoAboutProcessErrors, when all fine, then return nothing"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        ProcessParameter parameter = DomainFactory.createProcessParameter([
                value    : seqTrack.id.longValue(),
                className: SeqTrack.class.getName(),
        ])
        String workflowName = parameter.process.jobExecutionPlan.name
        MonitorOutputCollector collector = new MonitorOutputCollector()
        String expected = ''

        when:
        collector.addInfoAboutProcessErrors(workflowName, [seqTrack], { it.id }, { it })
        String output = collector.getOutput()

        then:
        expected == output
    }

    @Unroll
    void "addInfoAboutProcessErrors, when error '#errorCase', then create some error output"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        JobExecutionPlan jobExecutionPlan = DomainFactory.createJobExecutionPlan()
        Process process
        procesCount.times {
            process = DomainFactory.createProcess([
                    jobExecutionPlan: jobExecutionPlan,
            ])
            ProcessingStep processingStep = DomainFactory.createProcessingStep([
                    process: process,
            ])
            if (!noProcessUpdate) {
                DomainFactory.createProcessingStepUpdate([
                        processingStep: processingStep,
                        state         : ExecutionState.CREATED,
                ])
                if (hasFailure) {
                    DomainFactory.createProcessingStepUpdate([
                            processingStep: processingStep,
                            state         : ExecutionState.FAILURE,
                    ])
                }
            }
            DomainFactory.createProcessParameter([
                    value    : seqTrack.id.longValue(),
                    className: SeqTrack.class.getName(),
                    process  : processingStep.process,
            ])
        }
        String workflowName = jobExecutionPlan.name
        if (hasComment) {
            process.comment = DomainFactory.createComment()
            process.save()
        }
        MonitorOutputCollector collector = new MonitorOutputCollector()

        when:
        collector.addInfoAboutProcessErrors(workflowName, [seqTrack], { it.id }, { it })
        String output = collector.getOutput().split('\n')*.trim().join('\n')

        then:
        !output.empty
        if (hasComment) {
            !output.contains('comment')
        }

        where:
        errorCase                            | procesCount | noProcessUpdate | hasFailure | hasComment
        'No process'                         | 0           | false           | false      | false
        'Multiple processes'                 | 2           | false           | false      | false
        'No update'                          | 1           | true            | false      | false
        'process has failure and no comment' | 1           | false           | true       | false
        'process has failure and comment'    | 1           | false           | true       | true
    }

    @Unroll
    void "addInfoAboutProcessErrors, when object is from new workflow system and #errorCase, then create error output"() {
        given:
        WorkflowRun workflowRun = createWorkflowRun([
                workflowSteps: [createWorkflowStep([
                        state        : WorkflowStep.State.FAILED,
                        workflowError: createWorkflowError(),
                ])],
        ])
        if (hasComment) {
            workflowRun.comment = DomainFactory.createComment([comment: "this is a commentary"])
        }
        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                producedBy: workflowRun,
                state     : WorkflowArtefact.State.FAILED,
        ])
        SeqTrack seqTrack1 = createSeqTrack([
                workflowArtefact: workflowArtefact,
        ])
        SeqTrack seqTrack2 = createSeqTrack([
                workflowArtefact: workflowArtefact,
        ])
        LinkGenerator linkGenerator = Mock(LinkGenerator)
        MonitorOutputCollector collector = Spy(MonitorOutputCollector)

        when:
        collector.addInfoAboutProcessErrors(workflowRun.workflow.name, [seqTrack1, seqTrack2], { it.id }, { it })
        String output = collector.getOutput().split('\n')*.trim().join('\n')

        then:
        2 * linkGenerator.link(_) >> { return "link" }
        2 * collector.getGrailsLinkGenerator() >> linkGenerator

        and:
        !output.empty
        output.contains("shortName")
        if (hasComment) {
            output.contains('this is a commentary')
        }

        where:
        errorCase                                      | hasComment
        'workflow artefact has failure and no comment' | false
        'workflow artefact has failure and comment'    | true
    }

    void "addInfoAboutProcessErrors, when object is from new workflow system and has no error, then create no output"() {
        given:
        WorkflowRun workflowRun
        if (hasComment) {
            workflowRun = createWorkflowRun([
                    comment: DomainFactory.createComment(),
            ])
        } else {
            workflowRun = createWorkflowRun()
        }
        WorkflowArtefact workflowArtefact = createWorkflowArtefact([
                producedBy: workflowRun,
                state     : state,
        ])
        SeqTrack seqTrack = createSeqTrack([
                workflowArtefact: workflowArtefact,
        ])
        LinkGenerator linkGenerator = Mock(LinkGenerator)
        MonitorOutputCollector collector = Spy(MonitorOutputCollector)

        when:
        collector.addInfoAboutProcessErrors(workflowRun.workflow.name, [seqTrack], { it.id }, { it })
        String output = collector.getOutput().split('\n')*.trim().join('\n')

        then:
        0 * linkGenerator.link(_) >> { return "link" }
        0 * collector.getGrailsLinkGenerator() >> linkGenerator
        output.empty

        where:
        hasComment | state
        false      | WorkflowArtefact.State.SUCCESS
        true       | WorkflowArtefact.State.OMITTED
    }
}
