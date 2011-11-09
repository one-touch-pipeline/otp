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
        executorService(de.dkfz.tbi.otp.testing.SynchronousTestingExecutorService)
        servletContext(de.dkfz.tbi.otp.testing.OTPServletContext)
    }
}
