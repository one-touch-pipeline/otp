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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty

Properties otpProperties = ConfigService.parsePropertiesFile()
Boolean consoleEnabled = Boolean.parseBoolean(otpProperties.getProperty(OtpProperty.GRAILS_CONSOLE.key))

spring {
    jmx {
        uniqueNames = true
    }
    devtools {
        restart {
            exclude = [
                    '**/*.gsp',
                    '**/*.less',
                    '**/*.css',
                    '**/*.js',
                    '**/*.json',
                    '**/*.sh',
            ]
        }
    }
}
management {
    endpoints {
        enabledByDefault = false
    }
}

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
        html         : [
                'text/html',
                'application/xhtml+xml',
        ],
        xml          : [
                'text/xml',
                'application/xml',
        ],
        text         : 'text/plain',
        js           : 'text/javascript',
        rss          : 'application/rss+xml',
        atom         : 'application/atom+xml',
        css          : 'text/css',
        csv          : 'text/csv',
        all          : '*/*',
        json         : [
                'application/json',
                'text/json',
        ],
        form         : 'application/x-www-form-urlencoded',
        multipartForm: 'multipart/form-data'
]

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}

grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

//max upload size
grails.controllers.upload.maxFileSize = 10*1024*1024
grails.controllers.upload.maxRequestSize = 10*1024*1024

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.authority.className = 'de.dkfz.tbi.otp.security.Role'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'de.dkfz.tbi.otp.security.UserRole'
grails.plugin.springsecurity.userLookup.userDomainClassName = 'de.dkfz.tbi.otp.security.User'
grails.plugin.springsecurity.userLookup.usernameIgnoreCase = true
grails.plugin.springsecurity.userLookup.accountExpiredPropertyName = null
grails.plugin.springsecurity.userLookup.accountLockedPropertyName = null
grails.plugin.springsecurity.userLookup.passwordExpiredPropertyName = null

ArrayList<LinkedHashMap<String, Serializable>> staticRules = [
        // restricted access to special pages
        [pattern: "/adminSeed/**"                                        , access: ["denyAll"]],
        [pattern: "/plugins/**"                                          , access: ["denyAll"]],
        [pattern: "/login/impersonate"                                   , access: ["hasRole('ROLE_SWITCH_USER')"]],

        // publicly available pages
        [pattern: "/assets/**"                                           , access: ["permitAll"]],
        [pattern: "/grails-errorhandler/**"                              , access: ["permitAll"]],
        [pattern: "/"                                                    , access: ["permitAll"]],

        // regular pages with access for logged-in users, protected by annotations in services
        // Hence, default behavior is to denyAll and explicitly allow access in the services
        [pattern: "/**"                                                  , access:  ["denyAll"]],
]

// avoid black screen if console is disabled
if(consoleEnabled) {
    staticRules = staticRules + [
            [pattern: "/console/**"                                          , access: ["hasRole('ROLE_ADMIN') and @dicomAuditConsoleHandler.log()"]],
            [pattern: "/static/console*/**"                                  , access: ["hasRole('ROLE_ADMIN') and @dicomAuditConsoleHandler.log()"]],
    ]
}

grails.plugin.springsecurity.controllerAnnotations.staticRules = staticRules

// hierarchy of roles
grails.plugin.springsecurity.roleHierarchy = '''
    ROLE_ADMIN > ROLE_OPERATOR
    ROLE_OPERATOR > ROLE_TEST_PI
    ROLE_OPERATOR > ROLE_TEST_BIOINFORMATICAN
    ROLE_OPERATOR > ROLE_TEST_SUBMITTER
    ROLE_ADMIN > ROLE_SWITCH_USER
'''

grails.plugin.springsecurity.adh.errorPage = "/errors/error403"
grails.plugin.springsecurity.useSwitchUserFilter = true

grails.plugin.springsecurity.successHandler.targetUrlParameter = "target"
grails.plugin.springsecurity.successHandler.defaultTargetUrl = '/home/index'
grails.plugin.springsecurity.apf.storeLastUsername = true
grails.plugin.springsecurity.logout.postOnly = false
grails.plugin.springsecurity.printStatusMessages = false

// enable event listeners for logging login processes to the Dicom audit log
grails.plugin.springsecurity.useSecurityEventListener = true
// Injection of the Dicom logout handler
// The way used above (Adding listeners to Spring Security) would be preferred,
// but Spring doesn't offer an interface for logout, so we had to use a bean.
grails.plugin.springsecurity.logout.additionalHandlerNames = [
        'dicomAuditLogoutHandler',
]

// Use a ThreadLocal as security context holder strategy
// By default, Grails uses an InheritableThreadLocal, but this does not work with thread pools (used by Grails promises)
grails.plugin.springsecurity.sch.strategyName = org.springframework.security.core.context.SecurityContextHolder.MODE_THREADLOCAL

//databasemigration configuration
grails.plugin.databasemigration.changelogLocation = 'migrations'
grails.plugin.databasemigration.changelogFileName = 'migration-wrapper.groovy'
grails.plugin.databasemigration.updateOnStart = true
grails.plugin.databasemigration.updateOnStartFileName = "migration-wrapper.groovy"
grails.plugin.databasemigration.excludeObjects = [
        'aggregate_sequences',
        'meta_data_key',
        'sequences',
]
environments {
    WORKFLOW_TEST {
        grails.plugin.databasemigration.updateOnStart = false
    }
    test {
        grails.plugin.databasemigration.updateOnStart = false
    }
}

// Restore old data-binding behaviour (before 2.3)
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.autoGrowCollectionLimit = 65536

grails.databinding.trimStrings = true

//configure mail sending: disable mail sending for tests
environments {
    WORKFLOW_TEST {
        grails.mail.disabled=true
    }
    test {
        grails.mail.disabled=true
    }
}

// disable auto config of the grails oauth2 plugin. the plugin is configured manually and used to handle the wes server to server communication.
grails.plugin.springsecurity.oauthProvider.active=false

//configure groovy web console
grails.plugin.console.enabled = consoleEnabled
environments {
    production {
        grails.plugin.console.baseUrl="/otp/console"
    }
    development {
        grails.plugin.console.baseUrl="/console"
    }
}
grails.plugin.console.fileStore.remote.defaultPath=System.getenv("CONSOLE_REMOTE_DEFAULTPATH")


System.setProperty("javax.net.ssl.trustStore", otpProperties.getProperty(OtpProperty.TRUSTSTORE_PATH.key) ?: OtpProperty.TRUSTSTORE_PATH.defaultValue)
System.setProperty("javax.net.ssl.trustStorePassword", otpProperties.getProperty(OtpProperty.TRUSTSTORE_PASSWORD.key) ?: OtpProperty.TRUSTSTORE_PASSWORD.defaultValue)
System.setProperty("javax.net.ssl.trustStoreType", otpProperties.getProperty(OtpProperty.TRUSTSTORE_TYPE.key) ?: OtpProperty.TRUSTSTORE_TYPE.defaultValue)
