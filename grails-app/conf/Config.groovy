import de.dkfz.tbi.otp.config.*
import grails.util.*

Properties otpProperties = ConfigService.parsePropertiesFile()

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: [
        'text/html',
        'application/xhtml+xml'
    ],
    xml: [
        'text/xml',
        'application/xml'
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
        'text/json'
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

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = otpProperties.getProperty(OtpProperty.CONFIG_SERVER_URL.key)
    }
    WORKFLOW_TEST {
        grails.serverURL = "http://localhost:8080"
    }
}


String jobLogDir = otpProperties.getProperty(OtpProperty.PATH_JOB_LOGS.key) ?: OtpProperty.PATH_JOB_LOGS.defaultValue

// log4j configuration
log4j = {
    appenders {
        def jobHtmlLayout = new de.dkfz.tbi.otp.utils.logging.JobHtmlLayout()
        def jobAppender = new de.dkfz.tbi.otp.utils.logging.JobAppender(logDirectory: new File(jobLogDir), layout : jobHtmlLayout)
        appender name: "jobs", jobAppender

        console name: 'stdout', threshold: Environment.getCurrent() == Environment.TEST ? org.apache.log4j.Level.OFF : org.apache.log4j.Level.DEBUG
    }

    error stdout: [
            'org.codehaus.groovy.grails.web.servlet',           //  controllers
            'org.codehaus.groovy.grails.web.pages',             //  GSP
            'org.codehaus.groovy.grails.web.sitemesh',          //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter',    // URL mapping
            'org.codehaus.groovy.grails.web.mapping',           // URL mapping
            'org.codehaus.groovy.grails.commons',               // core / classloading
            'org.codehaus.groovy.grails.plugins',               // plugins
            'org.codehaus.groovy.grails.orm.hibernate',         // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate',
    ], additivity: false

    info stdout: [
            'liquibase',                                        // migration plugin: liquibase library
            'grails.plugin.databasemigration',                  // migration plugin: grails
    ], additivity: false

    debug stdout: [
            'de.dkfz.tbi.otp',                                  // our own stuff
            'seedme',                                           // seed plugin
            'grails.app.controllers.de.dkfz.tbi.otp',           // controllers
            'grails.app.domain.de.dkfz.tbi.otp',
            'grails.app.services.de.dkfz.tbi.otp',
            'grails.app.taglib.de.dkfz.tbi.otp',
            'grails.app.conf.BootStrap',
            'de.dkfz.roddy.execution.jobs.cluster', //ClusterJobManager conversion errors
    ], jobs: [
            'de.dkfz.tbi.otp.job.jobs',
    ], additivity: false
}

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'de.dkfz.tbi.otp.security.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'de.dkfz.tbi.otp.security.UserRole'
grails.plugin.springsecurity.authority.className = 'de.dkfz.tbi.otp.security.Role'

// ldap
if (!Boolean.parseBoolean(otpProperties.getProperty(OtpProperty.LDAP_ENABLED.key))) {
    println("Using database only for authentication")
    grails.plugin.springsecurity.providerNames = [
            'daoAuthenticationProvider',
            'anonymousAuthenticationProvider',
    ]
} else {
    println("Using LDAP and database for authentication")
    grails.plugin.springsecurity.providerNames = [
            'ldapDaoAuthenticationProvider',
            'anonymousAuthenticationProvider',
    ]
    if (otpProperties.getProperty(OtpProperty.LDAP_MANAGER_DN.key)) {
        grails.plugin.springsecurity.ldap.context.managerDn     = otpProperties.getProperty(OtpProperty.LDAP_MANAGER_DN.key)
    }
    grails.plugin.springsecurity.ldap.context.managerPassword = otpProperties.getProperty(OtpProperty.LDAP_MANAGER_PASSWORD.key)
    if (otpProperties.getProperty(OtpProperty.LDAP_MANAGER_PASSWORD.key)) {
        grails.plugin.springsecurity.ldap.auth.useAuthPassword = true
    } else {
        grails.plugin.springsecurity.ldap.auth.useAuthPassword = false
    }
    grails.plugin.springsecurity.ldap.context.server            = otpProperties.getProperty(OtpProperty.LDAP_SERVER.key)
    grails.plugin.springsecurity.ldap.search.base               = otpProperties.getProperty(OtpProperty.LDAP_SEARCH_BASE.key)
    grails.plugin.springsecurity.ldap.authorities.searchSubtree = otpProperties.getProperty(OtpProperty.LDAP_SEARCH_SUBTREE.key)
    grails.plugin.springsecurity.ldap.search.filter             = otpProperties.getProperty(OtpProperty.LDAP_SEARCH_FILTER.key)

    // static options
    grails.plugin.springsecurity.ldap.authorities.ignorePartialResultException = true
    grails.plugin.springsecurity.ldap.authorities.retrieveGroupRoles = false
    grails.plugin.springsecurity.ldap.authorities.retrieveDatabaseRoles = true
    grails.plugin.springsecurity.ldap.mapper.userDetailsClass = 'inetOrgPerson'
}

grails.plugin.springsecurity.controllerAnnotations.staticRules = [
        // restricted access to special pages
        "/adminSeed/**"                                        : ["denyAll"],
        "/console/**"                                          : ["hasRole('ROLE_ADMIN')"],
        "/plugins/console*/**"                                 : ["hasRole('ROLE_ADMIN')"],
        "/plugins/**"                                          : ["denyAll"],
        "/projectOverview/mmmlIdentifierMapping/**"            : ["hasRole('ROLE_MMML_MAPPING')"],
        "/j_spring_security_switch_user"                       : ["hasRole('ROLE_SWITCH_USER')"],

        // publicly available pages
        "/grails-errorhandler/**"                              : ["permitAll"],
        "/login/**"                                            : ["permitAll"],
        "/logout/**"                                           : ["permitAll"],
        "/document/download/**"                                : ["permitAll"],
        "/info/**"                                             : ["permitAll"],
        "/privacyPolicy/index"                                 : ["permitAll"],
        "/root/intro*"                                         : ["permitAll"],
        "/"                                                    : ["permitAll"],
        "/metadataImport/autoImport"                           : ["permitAll"],

        // regular pages with access for logged-in users, protected by annotations in services
        "/**"                                                  : ["hasRole('ROLE_USER')"],
]

// hierarchy of roles
grails.plugin.springsecurity.roleHierarchy = '''
    ROLE_ADMIN > ROLE_OPERATOR
    ROLE_OPERATOR > ROLE_USER
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


grails.plugin.databasemigration.changelogLocation = 'migrations'
grails.plugin.databasemigration.changelogFileName = 'changelog.groovy'
grails.plugin.databasemigration.updateOnStart = true
grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy', 'createSequenceViews.groovy']
grails.plugin.databasemigration.ignoredObjects = [
        'aggregate_sequences',
        'meta_data_key',
        'sequences',
]

// WARNING: This setting (same as this entire Config.groovy) has no effect on unit tests. See:
// * OTP-1126
// * http://grails.1312388.n4.nabble.com/unit-testing-grails-gorm-failOnError-true-td4231435.html
// * http://grails.1312388.n4.nabble.com/Unit-testing-with-failOnError-true-td2718543.html
grails.gorm.failOnError=true

// Shared constraints
grails.gorm.default.constraints = {
    greaterThanZero validator: { val, obj -> val > 0 }
}

// Documentation settings
grails.doc.title = 'The One Touch Pipeline (OTP)'
grails.doc.authors = 'The OTP Development Team'

// Restore old data-binding behaviour (before 2.3)
grails.databinding.convertEmptyStringsToNull = false
grails.databinding.trimStrings = false


//disable mail sending for tests
environments {
    WORKFLOW_TEST {
        grails.mail.disabled=true
    }
    test {
        grails.mail.disabled=true
    }

    //seed
    grails.plugin.seed.skipPlugins=true
    production {
        grails.plugin.seed.autoSeed=true
    }
    development {
        grails.plugin.seed.autoSeed=true
    }
}

grails.plugin.console.enabled = true
grails.plugin.console.baseUrl="/${appName}/console"
