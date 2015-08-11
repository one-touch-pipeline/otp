Properties otpProperties = new Properties()
try {
    String propertiesFile = System.getenv("OTP_PROPERTIES")
    if (propertiesFile && new File(propertiesFile).canRead()) {
        otpProperties.load(new FileInputStream(propertiesFile))
    } else {
        otpProperties.load(new FileInputStream(System.getProperty("user.home") + System.getProperty("file.separator") + ".otp.properties"))
    }
} catch (Exception e) {
    otpProperties.setProperty("otp.security.ldap.enabled", "false")
    otpProperties.setProperty("otp.jabber.enabled", "false")
}
def otpConfig = new ConfigSlurper().parse(otpProperties)
List pluginsToExclude = []

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

// What URL patterns should be processed by the resources plugin
// (prevent disclosure of configuration and code (CVE-2014-0053))
grails.resources.adhoc.includes = [
    '/images/**',
    '/css/**',
    '/js/**',
    '/plugins/**',
    '/pages/**',
    '/xslt/**',
]

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
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
        grails.serverURL = "${otpConfig.otp.server.url}${appName}"

        // Backdoor config options for BackdoorFilter in development mode
        if (!(otpConfig.otp.security.useBackdoor instanceof ConfigObject)) {
            otp.security.useBackdoor = Boolean.parseBoolean(otpConfig.otp.security.useBackdoor)
            otp.security.backdoorUser = otpConfig.otp.security.backdoorUser
        }
    }
    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = otpConfig.otp.server.url
    }
    WORKFLOW_TEST {
        grails.serverURL = "http://localhost:8080/${appName}"
    }
    test {
        grails.serverURL = "http://localhost:8080/${appName}"
    }
}

// Folder for data of NGS data files
otp.ngsdata.bootstrap.dataPath = "$ROOT_PATH/ftp"
// Folder for meta data files of NGS meta data files
otp.ngsdata.bootstrap.mdPath = "${home}ngs-icgc/data-tracking-private/"


// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
    appenders {
        def jobHtmlLayout = new de.dkfz.tbi.otp.utils.logging.JobHtmlLayout()
        def jobAppender = new de.dkfz.tbi.otp.utils.logging.JobAppender(logDirectory: new File("logs"), layout : jobHtmlLayout)
        appender name: "jobs", jobAppender

        // file appender that writes out the textual, aggregated performance stats generated by the performanceStatsAppender
        def performanceStatsFileAppender = new org.apache.log4j.FileAppender(
                fileName: "logs/perfStats.log",
                layout: pattern(conversionPattern: '%m%n')  // alternatively use the StatisticsCsvLayout to generate CSV
                )
        appender name: 'performanceStatsFileAppender', performanceStatsFileAppender

        console name: "stdout", threshold: org.apache.log4j.Level.DEBUG
    }

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'

    debug stdout: ['de.dkfz.tbi.otp', // our own stuff
        'grails.app.controllers.de.dkfz.tbi.otp', // controllers
        'grails.app.domain.de.dkfz.tbi.otp',
        'grails.app.services.de.dkfz.tbi.otp',
        'grails.app.taglib.de.dkfz.tbi.otp'
    ],
    jobs: ['de.dkfz.tbi.otp.example',
        'de.dkfz.tbi.otp.job.jobs',
        'de.dkfz.tbi.otp.jobs']
}

// mail settings
if (otpConfig.otp.mail.sender instanceof ConfigObject) {
    otp.mail.sender = "otp@localhost"
} else {
    otp.mail.sender = otpConfig.otp.mail.sender
}

if (otpConfig.otp.mail.notification.to instanceof ConfigObject) {
    otp.mail.notification.to = ''
} else {
    otp.mail.notification.to = otpConfig.otp.mail.notification.to
}

// Folder for putting stacktrace files made by error log service
if (otpConfig.otp.errorLogging.stacktraces instanceof ConfigObject) {
    otp.errorLogging.stacktraces = "/tmp/otp/stacktraces/"
} else {
    otp.errorLogging.stacktraces = otpConfig.otp.errorLogging.stacktraces
}

if (otpConfig.otp.dataprocessing.outputbasedir instanceof ConfigObject) {
    otp.dataprocessing.outputbasedir = ""
} else {
    otp.dataprocessing.outputbasedir = otpConfig.otp.dataprocessing.outputbasedir
}

if (otpConfig.otp.dataprocessing.scriptdir instanceof ConfigObject) {
    otp.dataprocessing.scriptdir = ""
} else {
    otp.dataprocessing.scriptdir = otpConfig.otp.dataprocessing.scriptdir
}

if (otpConfig.otp.dataprocessing.alignment.referenceIndex instanceof ConfigObject) {
    otp.dataprocessing.alignment.referenceIndex = ""
} else {
    otp.dataprocessing.alignment.referenceIndex = otpConfig.otp.dataprocessing.alignment.referenceIndex
}

otp.environment.name = otpConfig.otp.environment.name

// pbs password
if (otpConfig.otp.pbs.ssh.password instanceof ConfigObject) {
    otp.pbs.ssh.password = ""
} else {
    otp.pbs.ssh.password = otpConfig.otp.pbs.ssh.password
}
// pbs unixUser
if (otpConfig.otp.pbs.ssh.unixUser instanceof ConfigObject) {
    otp.pbs.ssh.unixUser = ""
} else {
    otp.pbs.ssh.unixUser = otpConfig.otp.pbs.ssh.unixUser
}
// pbs host
if (otpConfig.otp.pbs.ssh.host instanceof ConfigObject) {
    otp.pbs.ssh.host = ""
} else {
    otp.pbs.ssh.host = otpConfig.otp.pbs.ssh.host
}

otp {
    testing {
        // Default settings for work-flow integration tests
        workflows {
            account = otpProperties.getProperty("otp.testing.workflows.account")
            rootdir = (otpConfig.otp.testing.workflows.rootdir) ?: 'WORKFLOW_ROOT'
        }
    }
}

// Added by the Spring Security Core plugin:
grails.plugins.springsecurity.userLookup.userDomainClassName = 'de.dkfz.tbi.otp.security.User'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'de.dkfz.tbi.otp.security.UserRole'
grails.plugins.springsecurity.authority.className = 'de.dkfz.tbi.otp.security.Role'

// ldap
if ((otpConfig.otp.security.ldap.enabled instanceof ConfigObject) || !Boolean.parseBoolean(otpConfig.otp.security.ldap.enabled)) {
    otp.security.ldap.enabled = false
    println("Excluding ldap")
    pluginsToExclude << "spring-security-ldap"
} else {
    println("using ldap")
    otp.security.ldap.enabled = true
    grails.plugins.springsecurity.ldap.context.managerDn         = otpConfig.otp.security.ldap.managerDn
    grails.plugins.springsecurity.ldap.context.managerPassword   = otpConfig.otp.security.ldap.managerPw
    grails.plugins.springsecurity.ldap.context.server            = otpConfig.otp.security.ldap.server
    grails.plugins.springsecurity.ldap.search.base               = otpConfig.otp.security.ldap.search.base
    grails.plugins.springsecurity.ldap.authorities.searchSubtree = otpConfig.otp.security.ldap.search.subTree
    grails.plugins.springsecurity.ldap.search.filter             = otpConfig.otp.security.ldap.search.filter

    // static options
    grails.plugins.springsecurity.ldap.authorities.ignorePartialResultException = true
    grails.plugins.springsecurity.ldap.authorities.retrieveGroupRoles = true
    grails.plugins.springsecurity.ldap.authorities.retrieveDatabaseRoles = true
}

//configuration for jabber accounts
if ((otpConfig.otp.jabber.enabled instanceof ConfigObject) || !Boolean.parseBoolean(otpConfig.otp.jabber.enabled)) {
    println("jabber disabled")
    otp.jabber.enabled = false
} else {
    println("jabber enabled")
    otp.jabber.enabled = true
    otp.jabber.username = otpConfig.otp.jabber.username
    otp.jabber.password = otpConfig.otp.jabber.password
    otp.jabber.host = otpConfig.otp.jabber.host
    otp.jabber.port = otpConfig.otp.jabber.port
    otp.jabber.service = otpConfig.otp.jabber.service
}

// protect everything for role user
grails.plugins.springsecurity.controllerAnnotations.staticRules = [
    "/projectOverview/mmmlIdentifierMapping/**": ['ROLE_MMML_MAPPING'],
    "/grails-errorhandler/**": ['IS_AUTHENTICATED_ANONYMOUSLY'],
    "/seqTrackDataProvider/**": ['IS_AUTHENTICATED_ANONYMOUSLY'],
    "/fastqFilePathDataProvider/**": ['IS_AUTHENTICATED_ANONYMOUSLY'],
    "/login/**":             ['IS_AUTHENTICATED_ANONYMOUSLY'],
    "/logout/**":            ['IS_AUTHENTICATED_ANONYMOUSLY'],
    "/info/**":              ['permitAll'],
    "/":                     ['permitAll'],
    "/css/**":               ["permitAll"],
    "/images/**":            ["permitAll"],
    "/js/**":                ["permitAll"],
    "/igvSessionFiles/**":   ["permitAll"],
    "/console/**":           ['ROLE_ADMIN'],
    "/snv/**":               ['ROLE_OPERATOR'],
    "/j_spring_security_switch_user": ['ROLE_SWITCH_USER', 'IS_AUTHENTICATED_FULLY'],
    "/**":                   ['ROLE_USER'],

]

// hierarchy of roles
grails.plugins.springsecurity.roleHierarchy = '''
    ROLE_ADMIN > ROLE_OPERATOR
    ROLE_OPERATOR > ROLE_USER
    ROLE_OPERATOR > ROLE_MMML_MAPPING
    ROLE_ADMIN > ROLE_SWITCH_USER
'''

grails.plugins.springsecurity.useSwitchUserFilter = true
grails.plugins.springsecurity.successHandler.defaultTargetUrl = '/home/index'

// exclude unused plugins
if (pluginsToExclude) {
    grails.plugin.exclude = pluginsToExclude
}

grails.plugin.databasemigration.changelogFileName = 'changelog.xml'

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
}

