grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.source.level = 1.6 // TODO: OTP-263: lets go to java-7...
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
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
        compile "gson:gson:2.2.2"
        compile "sam:sam:1.78"
        // bedUtils
        compile 'bedUtils:bedUtils:0.6'
        // general dependencies
        compile "joda-time:joda-time:2.3"
        compile "org.jadira.usertype:usertype.jodatime:2.0.1"
        // dependencies for the flowControl API
        compile "fcClient:fcClient:0.1"
    }

    plugins {
        // core plugins
        build ":tomcat:7.0.53"
        runtime ":hibernate4:4.3.8.1"

        // plugins for the compile step
        compile ":spring-security-core:2.0-RC5"
        compile ":spring-security-ldap:2.0-RC2"
        compile ":spring-security-acl:2.0-RC2"
        compile ":executor:0.3"
        compile ":console:1.3"
        compile ":mail:1.0.5"
        // used by jenkins
        compile ":codenarc:0.21"
        compile ":code-coverage:1.2.7"

        // plugins needed at runtime
        runtime ":database-migration:1.4.0"
        // jQuery
        runtime ":jquery:1.11.1"
        runtime ":jquery-ui:1.10.4"

        // resources
        runtime ":resources:1.2.14"
        runtime ":page-resources:0.2.5"
        compile ":lesscss-resources:1.3.3"
        compile ":build-test-data:2.4.0" //http://grails.org/plugin/build-test-data
    }
}

codenarc.reports = {
    CodeNarcXmlReport('xml') {
        outputFile = 'target/CodeNarc-Report.xml'
        title = "OTP CodeNarc Report"
    }
    CodeNarcHtmlReport('html') {
        outputFile = 'target/CodeNarc-Report.html'
        title = "OTP CodeNarc Report"
    }
}
codenarc.extraIncludeDirs=['ast']
codenarc.properties = {
    // Each property definition is of the form:  RULE.PROPERTY-NAME = PROPERTY-VALUE
    GrailsPublicControllerMethod.enabled = false
    GrailsDomainHasEquals.enabled = false
    GrailsDomainHasToString.enabled = false
}

grails.tomcat.jvmArgs = [
    "-Xmx1024m",
    "-XX:MaxPermSize=256m"
]
