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
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.acls.AclPermissionEvaluator
import org.springframework.web.servlet.i18n.FixedLocaleResolver

import de.dkfz.tbi.otp.ProjectLinkGenerator
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.NumberConverter

import static grails.plugin.springsecurity.SpringSecurityUtils.getSecurityConfig

beans = {
    Properties otpProperties = ConfigService.parsePropertiesFile()

    // include Spring Beans with @Component annotation
    xmlns context: "http://www.springframework.org/schema/context"
    context.'component-scan'('base-package': "de.dkfz.tbi.otp")

    if (Environment.current == Environment.TEST) {
        // use Class.forName because classes in test-helper are not found in production env
        fileSystemService(Class.forName("de.dkfz.tbi.otp.job.processing.TestFileSystemService"))
    }
    if (Environment.current == Environment.TEST || Environment.current.name == "WORKFLOW_TEST") {
        configService(Class.forName("de.dkfz.tbi.otp.TestConfigService")) {
            processingOptionService = ref('processingOptionService')
        }
    } else {
        // proper thread pool
        xmlns task: "http://www.springframework.org/schema/task"
        task.executor(id: "taskExecutor", "pool-size": 10)
        task.scheduler(id: "taskScheduler", "pool-size": 10)
        task.'annotation-driven'(executor: "taskExecutor", scheduler: "taskScheduler")
    }

    permissionEvaluator(OtpPermissionEvaluator) {}
    aclPermissionEvaluator(AclPermissionEvaluator, ref('aclService')) {
        objectIdentityRetrievalStrategy = ref('objectIdentityRetrievalStrategy')
        objectIdentityGenerator = ref('objectIdentityRetrievalStrategy')
        sidRetrievalStrategy = ref('sidRetrievalStrategy')
        permissionFactory = ref('aclPermissionFactory')
    }

    // overwrite default 'authenticationEntryPoint'
    authenticationEntryPoint(TargetUrlEntryPoint, securityConfig.auth.loginFormUrl) {
        ajaxLoginFormUrl = securityConfig.auth.ajaxLoginFormUrl
        forceHttps = securityConfig.auth.forceHttps
        useForward = securityConfig.auth.useForward
        portMapper = ref('portMapper')
        portResolver = ref('portResolver')
        redirectStrategy = ref('redirectStrategy')
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

    // don't use the default locale specific value converter
    [
            Short, Short.TYPE,
            Integer, Integer.TYPE,
            Long, Long.TYPE,
            Float, Float.TYPE,
            Double, Double.TYPE,
            BigInteger,
            BigDecimal,
    ].each { numberType ->
        "defaultGrails${numberType.simpleName}Converter"(NumberConverter) {
            targetType = numberType
        }
    }

    grailsLinkGenerator(ProjectLinkGenerator, grailsApplication.config.getProperty("grails.serverURL", String.class)) { bean ->
        bean.autowire = true
    }

    // only use English (prevents translations included in plugins being used)
    localeResolver(FixedLocaleResolver, Locale.ENGLISH)

    if (Boolean.parseBoolean(otpProperties.getProperty(OtpProperty.OIDC_ENABLED.key))) {
        identityProvider(Class.forName("de.dkfz.tbi.otp.security.user.identityProvider.KeycloakService"))
    } else {
        identityProvider(Class.forName("de.dkfz.tbi.otp.security.user.identityProvider.LdapService"))
    }
}
