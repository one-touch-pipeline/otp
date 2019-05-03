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
endpoints {
    enabled = false
    jmx {
        enabled = true
    }
}
endpoints.enabled = false
grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: [
        'text/html',
        'application/xhtml+xml',
],
                      xml: [
                              'text/xml',
                              'application/xml',
                      ],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: [
                              'application/json',
                              'text/json',
                      ],
                      form: 'application/x-www-form-urlencoded',
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
grails.plugin.springsecurity.userLookup.userDomainClassName = 'de.dkfz.tbi.otp.security.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'de.dkfz.tbi.otp.security.UserRole'
grails.plugin.springsecurity.authority.className = 'de.dkfz.tbi.otp.security.Role'


grails.plugin.springsecurity.controllerAnnotations.staticRules = [
        // restricted access to special pages
        [pattern: "/adminSeed/**"                                        , access: ["denyAll"]],
        [pattern: "/console/**"                                          , access: ["hasRole('ROLE_ADMIN') and @dicomAuditConsoleHandler.log()"]],
        [pattern: "/static/console*/**"                                  , access: ["hasRole('ROLE_ADMIN') and @dicomAuditConsoleHandler.log()"]],
        [pattern: "/plugins/**"                                          , access: ["denyAll"]],
        [pattern: "/projectOverview/mmmlIdentifierMapping/**"            , access: ["hasRole('ROLE_MMML_MAPPING')"]],
        [pattern: "/login/impersonate"                                   , access: ["hasRole('ROLE_SWITCH_USER')"]],

        // publicly available pages
        [pattern: "/assets/**"                                           , access: ["permitAll"]],
        [pattern: "/grails-errorhandler/**"                              , access: ["permitAll"]],
        [pattern: "/login/**"                                            , access: ["permitAll"]],
        [pattern: "/logout/**"                                           , access: ["permitAll"]],
        [pattern: "/document/download/**"                                , access: ["permitAll"]],
        [pattern: "/info/**"                                             , access: ["permitAll"]],
        [pattern: "/privacyPolicy/index"                                 , access: ["permitAll"]],
        [pattern: "/root/intro*"                                         , access: ["permitAll"]],
        [pattern: "/"                                                    , access: ["permitAll"]],
        [pattern: "/metadataImport/autoImport"                           , access: ["permitAll"]],

        // regular pages with access for logged-in users, protected by annotations in services
        [pattern: "/**"                                                  , access:  ["isFullyAuthenticated()"]],
]

// hierarchy of roles
grails.plugin.springsecurity.roleHierarchy = '''
    ROLE_ADMIN > ROLE_OPERATOR
    ROLE_OPERATOR > ROLE_MMML_MAPPING
    ROLE_ADMIN > ROLE_SWITCH_USER
'''

grails.plugin.springsecurity.useSwitchUserFilter = true
grails.plugin.springsecurity.successHandler.defaultTargetUrl = '/home/index'
/* TODO: OTP-2282 uncomment when switching to the new layout and remove line above
grails.plugin.springsecurity.auth.loginFormUrl = "/?login=required"
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = "/?login=failed"
grails.plugin.springsecurity.adh.errorPage = null
grails.plugin.springsecurity.apf.storeLastUsername = true
//*/

grails.plugin.springsecurity.printStatusMessages = false

// enable event listeners for logging login processes to the Dicom audit log
grails.plugin.springsecurity.useSecurityEventListener = true
// Injection of the Dicom logout handler
// The way used above (Adding listeners to Spring Security) would be preferred,
// but Spring doesn't offer an interface for logout, so we had to use a bean.
grails.plugin.springsecurity.logout.additionalHandlerNames = [
        'dicomAuditLogoutHandler',
]

grails.plugin.databasemigration.changelogLocation = 'migrations'
grails.plugin.databasemigration.changelogFileName = 'migration-wrapper.groovy'
grails.plugin.databasemigration.updateOnStart = true
grails.plugin.databasemigration.updateOnStartFileName = "migration-wrapper.groovy"
grails.plugin.databasemigration.excludeObjects = [
        'aggregate_sequences',
        'meta_data_key',
        'sequences',
        'seed_me_checksum',
]

// WARNING: This setting (same as this entire application.groovy) has no effect on unit tests. See:
// * OTP-1126
// * http://grails.1312388.n4.nabble.com/unit-testing-grails-gorm-failOnError-true-td4231435.html
// * http://grails.1312388.n4.nabble.com/Unit-testing-with-failOnError-true-td2718543.html
grails.gorm.failOnError=true

// Shared constraints
grails.gorm.default.constraints = {
    greaterThanZero validator: { val, obj -> val > 0 }
}
grails.gorm.default.mapping = {
    id generator:'sequence'
}

// Restore old data-binding behaviour (before 2.3)
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false


//disable mail sending for tests
environments {
    WORKFLOW_TEST {
        grails.mail.disabled=true
        grails.plugin.databasemigration.updateOnStart = false
    }
    test {
        grails.mail.disabled=true
        grails.plugin.databasemigration.updateOnStart = false
    }

    //seed
    grails.plugin.seed.skipPlugins=true
    grails.plugin.seed.autoSeed=false
}

grails.plugin.console.enabled = true
environments {
    production {
        grails.plugin.console.baseUrl="/${appName}/console"
    }
}
