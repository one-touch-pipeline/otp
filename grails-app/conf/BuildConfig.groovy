grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.source.level = 1.6 // TODO: OTP-263: lets go to java-7...
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
// As OTP is now productive the application's name should be clean from any version numbering
grails.project.war.file = "target/otp.war"

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
        grailsRepo "$ARTIFACTORY_URL/svn.codehaus.org_grails-plugins"
        //grailsPlugins()
        //grailsHome()
        //grailsCentral()
        //mavenCentral()

        // uncomment these to enable remote dependency resolution from public Maven repositories
        //mavenCentral()
        //mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.16'
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
        compile "org.jadira.usertype:usertype.jodatime:1.9"
        // dependencies for the flowControl API
        compile "fcClient:fcClient:0.1"
    }

    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":jquery:1.8.3"
        runtime ":resources:1.2.RC2" //TODO: update
        runtime ":page-resources:0.2.3"
        compile ":executor:0.3"
        compile ":spring-security-core:1.2.7.3"
        compile ":spring-security-ldap:1.0.6"
        compile ":spring-security-acl:1.1.1"
        compile ":console:1.2"
        compile ":mail:1.0.1"
        compile ":codenarc:0.18.1"
        compile ":lesscss-resources:1.3.3"
        compile ":build-test-data:2.1.2" //http://grails.org/plugin/build-test-data
        runtime ":database-migration:1.3.2"
        build ":tomcat:$grailsVersion"
        // Enable Spock test. This plug-in is integrated into Grails in later versions and can be safely removed
        // after upgrading.
        test ":spock:0.7"
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
