import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import grails.plugin.springsecurity.*
import grails.util.*
import org.codehaus.groovy.grails.commons.*

class BootStrap {
    GrailsApplication grailsApplication
    SchedulerService schedulerService
    ConfigService configService

    def init = { servletContext ->
        // load the shutdown service
        grailsApplication.mainContext.getBean("shutdownService")

        if (configService.isJobSystemEnabled()) {
            // startup the scheduler
            log.info("JobSystem is enabled")
            schedulerService.startup()
        } else {
            log.info("JobSystem is disabled")
        }

        if (Environment.isDevelopmentMode()) {
            // adds the backdoor filter allowing a developer to login without password only in development mode
            SpringSecurityUtils.clientRegisterFilter('backdoorFilter', SecurityFilterPosition.SECURITY_CONTEXT_FILTER.order + 10)
        }

        if ([Environment.PRODUCTION, Environment.DEVELOPMENT].contains(Environment.getCurrent())) {
            UserService.createFirstAdminUserIfNoUserExists()
        }

        JSON.registerObjectMarshaller(Enum, { Enum e -> e.name() })
    }

    def destroy = {
    }
}
