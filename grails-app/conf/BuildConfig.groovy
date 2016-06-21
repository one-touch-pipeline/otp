import grails.util.Environment

grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)

grails.project.work.dir = 'target'

// As OTP is now productive the application's name should be clean from any version numbering
grails.project.war.file = "target/otp.war"

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    repositories {
        inherits true // Whether to inherit repository definitions from plugins
        mavenRepo "$ARTIFACTORY_URL"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes

        //git
        compile 'org.eclipse.jgit:org.eclipse.jgit:1.2.0.201112221803-r'
        runtime "commons-jexl:commons-jexl:1.1"
        // XMPP
        compile "jivesoftware:smack:3.1.0"
        // jdbc
        runtime "postgresql:postgresql:9.1-901.jdbc4"
        // ngstools
        compile "com.google.code.gson:gson:2.2.4"
        //compile "sam:sam:1.78"
        compile "com.github.broadinstitute:picard:1.134"
        // bedUtils
        compile 'bedUtils:bedUtils:0.7'

        // general dependencies
        compile "joda-time:joda-time:2.3"
        compile "org.jadira.usertype:usertype.jodatime:2.0.1"
        // dependencies for the flowControl API
        compile "fcClient:fcClient:0.1"

        // ssh and ssh-agent proxy library
        runtime "com.jcraft:jsch:0.1.53"
        runtime "com.jcraft:jsch.agentproxy.jsch:0.0.9"
        runtime "com.jcraft:jsch.agentproxy.connector-factory:0.0.9"

        // library for easy HTML editing
        compile "org.jsoup:jsoup:1.9.2"
    }

    plugins {
        // core plugins
        build ":tomcat:7.0.55.3"
        runtime ":hibernate4:4.3.10"
        compile ':cache:1.1.8'
        compile ':scaffolding:2.1.2'

        build(":release:3.1.1", ":rest-client-builder:2.1.1") {
            export = false
        }

        // plugins for the compile step
        compile "org.grails.plugins:spring-security-core:2.0.0"
        compile "org.grails.plugins:spring-security-ldap:2.0.1"
        compile "org.grails.plugins:spring-security-acl:2.0.1"
        compile ":executor:0.3"
        compile ":console:1.5.5"
        compile ":mail:1.0.7"
        // used by jenkins
        compile ":codenarc:0.24.1"
        test ":code-coverage:1.2.7"

        // plugins needed at runtime
        runtime ":database-migration:1.4.0"
        // jQuery
        runtime ":jquery:1.11.1"
        runtime ":jquery-ui:1.10.4"

        compile ":asset-pipeline:2.9.1"
        provided ":less-asset-pipeline:2.3.0"
        compile ":i18n-asset-pipeline:1.0.5"


        test ":build-test-data:2.4.0" //http://grails.org/plugin/build-test-data
        if ((Environment.getCurrent().getName() == "WORKFLOW_TEST")) {
            compile ":build-test-data:2.4.0"
        }
    }
}

codenarc {
    ruleSetFiles = ['file:grails-app/conf/CodeNarcRuleSet.groovy']
    extraIncludeDirs = ['ast']
    maxPriority1Violations = 0
    reports = {
        CodeNarcXmlReport('xml') {
            outputFile = 'target/CodeNarc-Report.xml'
            title = "OTP CodeNarc Report"
        }
        CodeNarcHtmlReport('html') {
            outputFile = 'target/CodeNarc-Report.html'
            title = "OTP CodeNarc Report"
        }
    }
}

coverage {
    exclusions = [
            // configuration
            '**/*RuleSet*',
            '**/TestDataConfig*',
            // test helper
            '**/de/dkfz/tbi/otp/testing/**',
            // tests
            'test/**',
    ]
    xml = true
}
