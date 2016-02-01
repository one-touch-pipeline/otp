import grails.util.Environment

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

if (!otpConfig.otp.server.url) {
    otpConfig.otp.server.url = "http://localhost:8080"
}

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.logging.jul.usebridge = true
        grails.serverURL = "${otpConfig.otp.server.url}/${appName}"

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
        grails.serverURL = "${otpConfig.otp.server.url}/${appName}"
    }
    test {
        grails.serverURL = "${otpConfig.otp.server.url}/${appName}"
    }
}


if (otpConfig.otp.logging.jobLogDir instanceof ConfigObject) {
    otp.logging.jobLogDir = "logs"
} else {
    otp.logging.jobLogDir = otpConfig.otp.logging.jobLogDir
}

File jobLogDir = new File(otp.logging.jobLogDir)

// log4j configuration
log4j = {
    appenders {
        if(Environment.currentEnvironment == Environment.PRODUCTION) {
            assert jobLogDir.isDirectory()
        }

        def jobHtmlLayout = new de.dkfz.tbi.otp.utils.logging.JobHtmlLayout()
        def jobAppender = new de.dkfz.tbi.otp.utils.logging.JobAppender(logDirectory: jobLogDir, layout : jobHtmlLayout)
        appender name: "jobs", jobAppender

        console name: "stdout", threshold: Environment.getCurrent() == Environment.TEST ? org.apache.log4j.Level.OFF : org.apache.log4j.Level.DEBUG
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

if (otpConfig.otp.mail.notification.fasttrack.to instanceof ConfigObject) {
    otp.mail.notification.fasttrack.to = ''
} else {
    otp.mail.notification.fasttrack.to = otpConfig.otp.mail.notification.fasttrack.to
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
// pbs ssh key file
if (otpConfig.otp.pbs.ssh.keyFile instanceof ConfigObject) {
    otp.pbs.ssh.keyFile = System.getProperty("user.home") + "/.ssh/id_rsa"
} else {
    otp.pbs.ssh.keyFile = otpConfig.otp.pbs.ssh.keyFile
}
// should ssh-agent be used to get the password for the ssh key (true or false)
// if false, only key files without password can be used
// if true, an ssh-agent must be running and the key must be added to it, even if the key file doesn't have a password
if (otpConfig.otp.pbs.ssh.useSshAgent instanceof ConfigObject) {
    otp.pbs.ssh.useSshAgent = true
} else {
    otp.pbs.ssh.useSshAgent = otpConfig.otp.pbs.ssh.useSshAgent as boolean
}
if (otp.pbs.ssh.keyFile == "") {
    println "\n##### No SSH key file provided               #####"
    println "##### Using insecure password authentication #####"
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
        group = otpConfig.otp.testing.group ?: ''    //A real group of the developer but not the primary one
    }
}

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'de.dkfz.tbi.otp.security.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'de.dkfz.tbi.otp.security.UserRole'
grails.plugin.springsecurity.authority.className = 'de.dkfz.tbi.otp.security.Role'

// ldap
if ((otpConfig.otp.security.ldap.enabled instanceof ConfigObject) || !Boolean.parseBoolean(otpConfig.otp.security.ldap.enabled)) {
    otp.security.ldap.enabled = false
    println("Excluding ldap")
    pluginsToExclude << "spring-security-ldap"
} else {
    println("using ldap")
    otp.security.ldap.enabled = true
    grails.plugin.springsecurity.ldap.context.managerDn         = otpConfig.otp.security.ldap.managerDn
    grails.plugin.springsecurity.ldap.context.managerPassword   = otpConfig.otp.security.ldap.managerPw
    grails.plugin.springsecurity.ldap.context.server            = otpConfig.otp.security.ldap.server
    grails.plugin.springsecurity.ldap.search.base               = otpConfig.otp.security.ldap.search.base
    grails.plugin.springsecurity.ldap.authorities.searchSubtree = otpConfig.otp.security.ldap.search.subTree
    grails.plugin.springsecurity.ldap.search.filter             = otpConfig.otp.security.ldap.search.filter

    // static options
    grails.plugin.springsecurity.ldap.authorities.ignorePartialResultException = true
    grails.plugin.springsecurity.ldap.authorities.retrieveGroupRoles = true
    grails.plugin.springsecurity.ldap.authorities.retrieveDatabaseRoles = true
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
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
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
    "/plugins/console*/**":  ['ROLE_ADMIN'],
    "/plugins/**":           ['denyAll'],
    "/snv/**":               ['ROLE_OPERATOR'],
    "/j_spring_security_switch_user": ['ROLE_SWITCH_USER', 'IS_AUTHENTICATED_FULLY'],
    "/**":                   ['ROLE_USER'],

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

grails.plugin.console.enabled = true
grails.plugin.console.baseUrl="/${appName}/console"
