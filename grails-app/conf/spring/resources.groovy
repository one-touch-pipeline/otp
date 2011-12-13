import grails.util.Environment

// Place your Spring DSL code here
beans = {
    // include Spring Beans with @Component annotation
    xmlns context:"http://www.springframework.org/schema/context"
    context.'component-scan'('base-package' :"de.dkfz.tbi.otp" )

    taskExecutor(org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) {
        corePoolSize = 5
        maxPoolSize = 10
    }
    xmlns task: "http://www.springframework.org/schema/task"
    task.'annotation-driven'() {
        executor = "taskExecutor"
    }

    if (Environment.getCurrent() == Environment.TEST) {
        trueExecutorService(grails.plugin.executor.PersistenceContextExecutorWrapper) { bean ->
            bean.destroyMethod = 'destroy'
            persistenceInterceptor = ref("persistenceInterceptor")
            executor = java.util.concurrent.Executors.newCachedThreadPool()
        }
        executorService(de.dkfz.tbi.otp.testing.SynchronousTestingExecutorService)
        servletContext(de.dkfz.tbi.otp.testing.OTPServletContext)
    }
    if (grailsApplication.config.otp.jabber.enabled) {
        jabberService(de.dkfz.tbi.otp.notification.JabberService) {
            grailsApplication = ref("grailsApplication")
            service = grailsApplication.config.otp.jabber.service
            username = grailsApplication.config.otp.jabber.username
            password = grailsApplication.config.otp.jabber.password
        }
    }
}
