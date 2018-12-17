import grails.converters.JSON
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsApplication

import de.dkfz.odcf.audit.impl.DicomAuditLogger
import de.dkfz.odcf.audit.xml.layer.EventIdentification.EventOutcomeIndicator
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService

class BootStrap {
    ConfigService configService
    GrailsApplication grailsApplication
    PropertiesValidationService propertiesValidationService
    SchedulerService schedulerService

    def init = { servletContext ->
        // load the shutdown service
        grailsApplication.mainContext.getBean("shutdownService")

        propertiesValidationService.validateStartUpProperties()

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
        DicomAuditLogger.logActorStart(EventOutcomeIndicator.SUCCESS, ConfigService.getInstance().getDicomInstanceName())
    }

    def destroy = {
    }
}
