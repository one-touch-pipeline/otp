/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.restarting

import grails.testing.gorm.DataTest
import org.slf4j.Logger
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.HelperUtils

@SuppressWarnings("UnnecessaryGetter")
class RestartActionServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Process,
                ProcessingStep,
                ProcessingStepUpdate,
                JobExecutionPlan,
        ]
    }

    void "handleAction, when action is null, does nothing"() {
        given:
        RestartActionService service = new RestartActionService()
        Job job = GroovyMock(Job) {
            1 * getLog() >> Mock(Logger) {
                1 * debug(_) >> { String message ->
                    assert message.contains('Stopping') && message.contains('null')
                }
            }
        }

        expect:
        service.handleAction(null, job)
    }

    void "handleAction, when action is STOP, does nothing"() {
        given:
        RestartActionService service = new RestartActionService()
        Job job = GroovyMock(Job) {
            1 * getLog() >> Mock(Logger) {
                1 * debug(_, _) >> { _, JobErrorDefinition.Action action ->
                    assert action == JobErrorDefinition.Action.STOP
                }
            }
        }

        expect:
        service.handleAction(JobErrorDefinition.Action.STOP, job)
    }

    void "handleAction, when action is CHECK_FURTHER, throws exception"() {
        given:
        RestartActionService service = new RestartActionService()
        Job job = GroovyMock(Job) {
            1 * getLog() >> Mock(Logger) {
                1 * debug(_, _) >> { _, JobErrorDefinition.Action action ->
                    assert action == JobErrorDefinition.Action.CHECK_FURTHER
                }
            }
        }

        when:
        service.handleAction(JobErrorDefinition.Action.CHECK_FURTHER, job)

        then:
        UnsupportedOperationException e = thrown()
        e.message.contains('Action is CHECK_FURTHER. That should have been handled earlier.')
    }

    void "handleAction, when action is RESTART_JOB and job is restartable, calls schedulerService.restartProcessingStep"() {
        given:
        RestartActionService service = new RestartActionService(
                schedulerService: Mock(SchedulerService) {
                    1 * restartProcessingStep(_)
                    0 * _
                },
        )
        Job job = GroovyMock(AutoRestartableJob) {
            2 * getLog() >> Mock(Logger) {
                1 * debug(_, _) >> { _, JobErrorDefinition.Action action ->
                    assert action == JobErrorDefinition.Action.RESTART_JOB
                } >> { String message ->
                    assert message.contains('Job restarted.')
                }
            }
            1 * getProcessingStep() >> new ProcessingStep()
        }

        expect:
        service.handleAction(JobErrorDefinition.Action.RESTART_JOB, job)
    }

    void "handleAction, when action is RESTART_JOB and job is not restartable, throws exception"() {
        given:
        RestartActionService service = new RestartActionService()
        Job job = GroovyMock(Job) {
            1 * getLog() >> Mock(Logger) {
                1 * debug(_, _) >> { _, JobErrorDefinition.Action action ->
                    assert action == JobErrorDefinition.Action.RESTART_JOB
                }
            }
            1 * getProcessingStep() >> new ProcessingStep(jobClass: HelperUtils.uniqueString, process: new Process())
        }

        when:
        service.handleAction(JobErrorDefinition.Action.RESTART_JOB, job)

        then:
        RuntimeException e = thrown()
        e.message == RestartActionService.JOB_NOT_AUTO_RESTARTABLE
    }

    void "handleAction, when action is RESTART_WF and start job is restartable, calls restart method of start job"() {
        given:
        String workflowName = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                context: Mock(ApplicationContext) {
                    1 * getBean(_) >> Mock(RestartableStartJob) {
                        1 * restart(_) >> GroovyMock(Process) {
                            1 * save(_) >> new Process()
                        }
                    }
                },
                commentService: Mock(CommentService) {
                    1 * createOrUpdateComment(_, _, _) >> { CommentableWithProject commentable, String message, String userName ->
                        assert message.contains(RestartActionService.WORKFLOW_RESTARTED)
                    }
                }
        )
        Job job = GroovyMock(Job) {
            2 * getLog() >> Mock(Logger) {
                1 * debug(_, _) >> { _, JobErrorDefinition.Action action ->
                    assert action == JobErrorDefinition.Action.RESTART_WF
                } >> { String message ->
                    assert message == RestartActionService.WORKFLOW_RESTARTED
                }
            }
            2 * getProcessingStep() >> new ProcessingStep(
                    process: new Process(
                            jobExecutionPlan: new JobExecutionPlan(
                                    name: workflowName,
                                    startJob: new StartJobDefinition(
                                            bean: HelperUtils.uniqueString
                                    )
                            )
                    )
            )
        }

        expect:
        service.handleAction(JobErrorDefinition.Action.RESTART_WF, job)
    }

    void "handleAction, when action is RESTART_WF and start job is not restartable, throws exception"() {
        given:
        String workflowName = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                context: Mock(ApplicationContext) {
                    1 * getBean(_) >> GroovyMock(StartJob) {
                        0 * restart()
                    }
                },
        )
        Job job = GroovyMock(Job) {
            1 * getLog() >> Mock(Logger) {
                1 * debug(_, _) >> { _, JobErrorDefinition.Action action ->
                    assert action == JobErrorDefinition.Action.RESTART_WF
                }
            }
            1 * getProcessingStep() >> new ProcessingStep(
                    process: new Process(
                            jobExecutionPlan: new JobExecutionPlan(
                                    name: workflowName,
                                    startJob: new StartJobDefinition(
                                            bean: HelperUtils.uniqueString
                                    )
                            )
                    )
            )
        }

        when:
        service.handleAction(JobErrorDefinition.Action.RESTART_WF, job)

        then:
        RuntimeException e = thrown()
        e.message == RestartActionService.WORKFLOW_NOT_AUTO_RESTARTABLE
    }

    void "restartWorkflowWithProcess, calls restart method of start job"() {
        given:
        String workflowName = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                context: Mock(ApplicationContext) {
                    1 * getBean(_) >> Mock(RestartableStartJob) {
                        1 * restart(_) >> GroovyMock(Process) {
                            1 * save(_) >> new Process()
                        }
                    }
                },
                commentService: Mock(CommentService) {
                    1 * saveComment(_, _) >> { CommentableWithProject commentable, String message ->
                        assert message.contains(RestartActionService.WORKFLOW_RESTARTED)
                    }
                }
        )
        service.processService = new ProcessService()

        Process process = DomainFactory.createProcess(
                jobExecutionPlan: new JobExecutionPlan(
                        name: workflowName,
                        startJob: new StartJobDefinition(
                                bean: HelperUtils.uniqueString,
                        ),
                ),
        )
        ProcessingStep processingStep = DomainFactory.createProcessingStep(
                process: process,
                jobDefinition: new JobDefinition(plan: process.jobExecutionPlan),
        )
        ProcessingStepUpdate processingStepUpdate = DomainFactory.createProcessingStepUpdate(
                processingStep: processingStep,
                date: new Date(),
                state: ExecutionState.CREATED,
        )
        DomainFactory.createProcessingStepUpdate(
                processingStep: processingStep,
                date: new Date(),
                state: ExecutionState.FAILURE,
                previous: processingStepUpdate,
        )

        expect:
        service.restartWorkflowWithProcess(process)
    }

    void "test logInCommentAndJobLog without old message"() {
        given:
        String messageValue = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                commentService: Mock(CommentService) {
                    1 * createOrUpdateComment(_, _, _) >> { CommentableWithProject commentable, String message, String userName ->
                        assert message.contains(messageValue)
                    }
                }
        )
        Job job = GroovyMock(AutoRestartableJob) {
            1 * getLog() >> Mock(Logger) {
                1 * debug(_) >> { String message ->
                    assert message.contains(messageValue)
                }
            }
            1 * getProcessingStep() >> new ProcessingStep(process: new Process())
        }

        expect:
        service.logInCommentAndJobLog(job, messageValue)
    }

    void "test logInCommentAndJobLog with old message"() {
        given:
        String oldMessageValue = HelperUtils.uniqueString
        String messageValue = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                commentService: Mock(CommentService) {
                    1 * createOrUpdateComment(_, _, _) >> { CommentableWithProject commentable, String message, String userName ->
                        assert message.contains(messageValue)
                        assert message.contains(oldMessageValue)
                    }
                }
        )
        Job job = GroovyMock(AutoRestartableJob) {
            1 * getLog() >> Mock(Logger) {
                1 * debug(_) >> { String message ->
                    assert message.contains(messageValue)
                }
            }
            1 * getProcessingStep() >> new ProcessingStep(
                    process: new Process(
                            comment: new Comment(
                                    comment: oldMessageValue,
                            ),
                    ),
            )
        }

        expect:
        service.logInCommentAndJobLog(job, messageValue)
    }

    void "logInComment without old message"() {
        given:
        String messageValue = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                commentService: Mock(CommentService) {
                    1 * saveComment(_, _) >> { CommentableWithProject commentable, String message ->
                        assert message.contains(messageValue)
                    }
                }
        )
        Process process = DomainFactory.createProcess()

        expect:
        service.logInComment(process, messageValue)
    }

    void "logInComment with old message"() {
        given:
        String oldMessageValue = HelperUtils.uniqueString
        String messageValue = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                commentService: Mock(CommentService) {
                    1 * saveComment(_, _) >> { CommentableWithProject commentable, String message ->
                        assert message.contains(messageValue)
                        assert message.contains(oldMessageValue)
                    }
                }
        )
        Process process = DomainFactory.createProcess(
                comment: new Comment(
                        comment: oldMessageValue,
                ),
        )

        expect:
        service.logInComment(process, messageValue)
    }
}
