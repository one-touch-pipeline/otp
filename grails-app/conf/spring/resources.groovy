
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

import grails.util.Environment
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.acls.AclPermissionEvaluator

import de.dkfz.tbi.otp.security.*

beans = {
    // include Spring Beans with @Component annotation
    xmlns context:"http://www.springframework.org/schema/context"
    context.'component-scan'('base-package' :"de.dkfz.tbi.otp" )

    if (Environment.getCurrent() == Environment.TEST) {
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
