package de.dkfz.tbi.otp.job.restarting

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.logging.*
import org.springframework.context.*
import spock.lang.*

class RestartActionServiceSpec extends Specification {

    void "handleAction, when action is null, does nothing"() {
        given:
        RestartActionService service = new RestartActionService()
        Job job = GroovyMock(Job) {
            1 * getLog() >> Mock(Log) {
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
            1 * getLog() >> Mock(Log) {
                1 * debug(_) >> { GString message ->
                    assert message.contains('STOP')
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
            1 * getLog() >> Mock(Log) {
                1 * debug(_) >> { GString message ->
                    assert message.contains('CHECK_FURTHER')
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
            2 * getLog() >> Mock(Log) {
                2 * debug(_) >> { GString message ->
                    assert message.contains('RESTART_JOB')
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
            1 * getLog() >> Mock(Log) {
                1 * debug(_) >> { GString message ->
                    assert message.contains('RESTART_JOB')
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
                    1 * getBean(_) >> GroovyMock(RestartableStartJob) {
                        1 * restart()
                        _ * getJobExecutionPlanName() >> workflowName
                    }
                },
                commentService: Mock(CommentService) {
                    1 * createOrUpdateComment(_, _, _) >> { Commentable commentable, String message, String userName ->
                        assert message.contains(RestartActionService.WORKFLOW_RESTARTED)
                    }
                }
        )
        Job job = GroovyMock(Job) {
            2 * getLog() >> Mock(Log) {
                2 * debug(_) >> { GString message ->
                    assert message.contains('RESTART_WF')
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
                        _ * getJobExecutionPlanName() >> workflowName
                    }
                },
        )
        Job job = GroovyMock(Job) {
            1 * getLog() >> Mock(Log) {
                1 * debug(_) >> { GString message ->
                    assert message.contains('RESTART_WF')
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

    void "test logInCommentAndJobLog without old message"() {
        given:
        String messageValue = HelperUtils.uniqueString
        RestartActionService service = new RestartActionService(
                commentService: Mock(CommentService) {
                    1 * createOrUpdateComment(_, _, _) >> { Commentable commentable, String message, String userName ->
                        assert message.contains(messageValue)
                    }
                }
        )
        Job job = GroovyMock(AutoRestartableJob) {
            1 * getLog() >> Mock(Log) {
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
                    1 * createOrUpdateComment(_, _, _) >> { Commentable commentable, String message, String userName ->
                        assert message.contains(messageValue)
                        assert message.contains(oldMessageValue)
                    }
                }
        )
        Job job = GroovyMock(AutoRestartableJob) {
            1 * getLog() >> Mock(Log) {
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
}
