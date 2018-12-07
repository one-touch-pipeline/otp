import de.dkfz.tbi.otp.security.OtpPermissionEvaluator
import grails.util.Environment
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.acls.AclPermissionEvaluator
import de.dkfz.tbi.otp.security.CustomRequestDataValueProcessor
import de.dkfz.tbi.otp.security.DicomAuditLogoutHandler

beans = {
    // include Spring Beans with @Component annotation
    xmlns context:"http://www.springframework.org/schema/context"
    context.'component-scan'('base-package' :"de.dkfz.tbi.otp" )

    if (Environment.getCurrent() == Environment.TEST) {
        trueExecutorService(grails.plugin.executor.PersistenceContextExecutorWrapper) { bean ->
            bean.destroyMethod = 'destroy'
            persistenceInterceptor = ref("persistenceInterceptor")
            executor = java.util.concurrent.Executors.newCachedThreadPool()
        }
        executorService(de.dkfz.tbi.otp.testing.SynchronousTestingExecutorService)
        servletContext(de.dkfz.tbi.otp.testing.OTPServletContext)
        fileSystemService(de.dkfz.tbi.otp.job.processing.TestFileSystemService)
    } else {
        // proper thread pool
        xmlns task: "http://www.springframework.org/schema/task"
        task.executor(id: "taskExecutor", "pool-size": 10)
        task.scheduler(id: "taskScheduler", "pool-size": 10)
        if (Environment.getCurrent().getName() != "WORKFLOW_TEST") {
            task.'annotation-driven'(executor: "taskExecutor", scheduler: "taskScheduler")
        }
    }
    if (Environment.getCurrent() == Environment.TEST || Environment.getCurrent().getName() == "WORKFLOW_TEST") {
        configService(de.dkfz.tbi.otp.TestConfigService) {
            processingOptionService = ref('processingOptionService')
        }
    }

    permissionEvaluator(OtpPermissionEvaluator) {}
    aclPermissionEvaluator(AclPermissionEvaluator, ref('aclService')) {
        objectIdentityRetrievalStrategy = ref('objectIdentityRetrievalStrategy')
        objectIdentityGenerator = ref('objectIdentityRetrievalStrategy')
        sidRetrievalStrategy = ref('sidRetrievalStrategy')
        permissionFactory = ref('aclPermissionFactory')
    }

    // http://stackoverflow.com/questions/10013288/another-unnamed-cachemanager-already-exists-in-the-same-vm-ehcache-2-5
    aclCacheManager(EhCacheManagerFactoryBean) {
        shared = true
    }

    // workaround for @PreFilter annotation
    // https://jira.grails.org/browse/GPSPRINGSECURITYACL-37
    expressionHandler(DefaultMethodSecurityExpressionHandler) {
        parameterNameDiscoverer = ref('parameterNameDiscoverer')
        permissionEvaluator = ref('permissionEvaluator')
        roleHierarchy = ref('roleHierarchy')
        permissionCacheOptimizer = null
    }

    requestDataValueProcessor(CustomRequestDataValueProcessor)

    // Bean for handling logouts with the Dicom audit logger
    dicomAuditLogoutHandler(DicomAuditLogoutHandler)
}
