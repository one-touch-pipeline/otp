import grails.util.Environment
import grails.plugin.springsecurity.SecurityFilterPosition
import grails.plugin.springsecurity.SpringSecurityUtils

class BootStrap {
    def grailsApplication
    def schedulerService

    def init = { servletContext ->
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
