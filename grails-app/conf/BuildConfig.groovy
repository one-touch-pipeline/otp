grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"

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
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()

        // uncomment these to enable remote dependency resolution from public Maven repositories
        //mavenCentral()
        //mavenLocal()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
        mavenRepo "http://download.eclipse.org/jgit/maven"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.16'
        //git
        compile 'org.eclipse.jgit:org.eclipse.jgit:1.0.0.201106090707-r'
        runtime "commons-jexl:commons-jexl:1.1"
        // XMPP
        compile "jivesoftware:smack:3.1.0"
    }

    plugins {
        compile ":hibernate:$grailsVersion"
        compile ":jquery:1.6.1.1"
        compile ":resources:1.0.2"
        compile ":executor:0.3"
        compile ":spring-security-core:1.2.4"
        compile ":spring-security-ldap:1.0.5"
        compile ":spring-security-acl:1.1"
        compile ":jquery-datatables:1.7.5"
        compile ":perf4j:0.1.1"
        compile ":mail:1.0-SNAPSHOT"

        build ":tomcat:$grailsVersion"
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
