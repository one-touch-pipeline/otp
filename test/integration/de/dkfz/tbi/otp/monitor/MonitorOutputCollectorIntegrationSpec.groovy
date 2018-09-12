package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import spock.lang.*

class MonitorOutputCollectorIntegrationSpec extends Specification {


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
            process.save(flush: true)
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
}
