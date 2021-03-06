
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
import org.springframework.web.servlet.i18n.FixedLocaleResolver

import de.dkfz.tbi.otp.ProjectLinkGenerator
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.NumberConverter

import static grails.plugin.springsecurity.SpringSecurityUtils.getSecurityConfig

beans = {
    // include Spring Beans with @Component annotation
    xmlns context:"http://www.springframework.org/schema/context"
    context.'component-scan'('base-package': "de.dkfz.tbi.otp")

    if (Environment.getCurrent() == Environment.TEST) {
        // use Class.forName because classes in test-helper are not found in production env
        fileSystemService(Class.forName("de.dkfz.tbi.otp.job.processing.TestFileSystemService"))
    }
    if (Environment.getCurrent() == Environment.TEST || Environment.getCurrent().getName() == "WORKFLOW_TEST") {
        configService(Class.forName("de.dkfz.tbi.otp.TestConfigService")) {
            processingOptionService = ref('processingOptionService')
        }
    }

    userDetailsService(UserCreatingUserDetailsService)

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

    grailsLinkGenerator(ProjectLinkGenerator, grailsApplication.config.grails.serverURL) { bean ->
        bean.autowire = true
    }

    // only use English (prevents translations included in plugins being used)
    localeResolver(FixedLocaleResolver, Locale.ENGLISH)
}
