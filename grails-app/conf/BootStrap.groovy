import grails.util.Environment
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.ngsdata.*
import org.codehaus.groovy.grails.plugins.springsecurity.SecurityFilterPosition
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

class BootStrap {
    def grailsApplication
    def schedulerService

    def init = { servletContext ->
        if (Environment.current != Environment.TEST) {
            List<JobExecutionPlan> plans = JobExecutionPlan.findAllByEnabledAndObsoleted(true, false)
            plans.each { JobExecutionPlan plan ->
                AbstractStartJobImpl startJob = grailsApplication.mainContext.getBean(plan.startJob.bean) as AbstractStartJobImpl
                startJob.setJobExecutionPlan(plan)
            }
        }
        // load the shutdown service
        grailsApplication.mainContext.getBean("shutdownService")
        // startup the scheduler
        schedulerService.startup()

        if (Environment.isDevelopmentMode()) {
            // adds the backdoor filter allowing a developer to login without password only in development mode
            SpringSecurityUtils.clientRegisterFilter('backdoorFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 10)
        }
    }
    def destroy = {
    }
}
