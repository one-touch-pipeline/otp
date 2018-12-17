package de.dkfz.tbi.otp.job.restarting

import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.jobs.RestartableStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.HelperUtils

class RestartCheckerServiceSpec extends Specification {

    void "canJobBeRestarted, when job is of type AutoRestartable, returns true"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()

        expect:
        restartCheckerService.canJobBeRestarted(Mock(AutoRestartableJob))
    }

    void "canJobBeRestarted, when job is not of type AutoRestartable, returns false"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()

        expect:
        !restartCheckerService.canJobBeRestarted(Mock(Job))
    }


    void "canWorkflowBeRestarted, when start job bean is of type RestartableStartJob, returns true"() {
        given:
        String planName = HelperUtils.uniqueString
        RestartCheckerService restartCheckerService = new RestartCheckerService(
                context: Mock(ApplicationContext) {
                    getBean(_) >> Mock(RestartableStartJob)
                }
        )
        ProcessingStep step = new ProcessingStep(
                process: new Process(
                        jobExecutionPlan: new JobExecutionPlan(
                                name: planName,
                                startJob: new StartJobDefinition(
                                        bean: HelperUtils.uniqueString
                                )
                        )
                )
        )

        expect:
        restartCheckerService.canWorkflowBeRestarted(step)
    }

    void "canWorkflowBeRestarted, when start job bean is not of type RestartableStartJob, returns false"() {
        given:
        String planName = HelperUtils.uniqueString
        RestartCheckerService restartCheckerService = new RestartCheckerService(
                context: Mock(ApplicationContext) {
                    getBean(_) >> Mock(StartJob)
                }
        )
        ProcessingStep step = new ProcessingStep(
                process: new Process(
                        jobExecutionPlan: new JobExecutionPlan(
                                name: planName,
                                startJob: new StartJobDefinition(
                                        bean: HelperUtils.uniqueString
                                )
                        )
                )
        )

        expect:
        !restartCheckerService.canWorkflowBeRestarted(step)
    }


    void "isJobAlreadyRestarted, when job has already been restarted, returns true"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new RestartedProcessingStep()

        expect:
        restartCheckerService.isJobAlreadyRestarted(step)
    }

    void "isJobAlreadyRestarted, when job has not been restarted, returns false"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new ProcessingStep()

        expect:
        !restartCheckerService.isJobAlreadyRestarted(step)
    }

    void "isWorkflowAlreadyRestarted, when workflow has already been restarted, returns true"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new ProcessingStep(
                process: new Process(
                    restarted: new Process()
                )
        )

        expect:
        restartCheckerService.isWorkflowAlreadyRestarted(step)
    }

    void "isWorkflowAlreadyRestarted, when workflow has not been restarted, returns false"() {
        given:
        RestartCheckerService restartCheckerService = new RestartCheckerService()
        ProcessingStep step = new ProcessingStep(
                process: new Process()
        )

        expect:
        !restartCheckerService.isWorkflowAlreadyRestarted(step)
    }

}
