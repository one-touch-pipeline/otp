#!/usr/bin/env groovy
import groovy.transform.Field
import groovy.transform.Immutable
import groovy.transform.ToString
@Grapes(@Grab('org.jsoup:jsoup:1.8.3'))
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

import java.util.jar.JarEntry
import java.util.jar.JarFile

@Immutable
@ToString
class BuildInformation {
    String shortRevision
    String branch
}

private extractBuildInformationFromWarFile(File war) {
    JarFile jarFile = new JarFile(war)
    JarEntry entry = jarFile.entries().find({
        it.name == 'WEB-INF/grails-app/views/templates/_version.gsp'
    }) as JarEntry

    assert entry: 'Unable to find version information in WAR file'

    BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))

    String gspText = reader.text.replaceAll(/\r?\n/, '')

    extractBuildInformationFromTextWithPattern(gspText, BUILD_INFO_PATTERN)
}

private BuildInformation extractBuildInformationFromURL(String url) {
    Document doc = Jsoup.connect(url).get()
    Element footer = doc.select('div.footer:containsOwn(Build:)')?.first()

    assert footer: 'No element containing the build information was found.'

    extractBuildInformationFromTextWithPattern(footer.ownText(), /Build:\s+$BUILD_INFO_PATTERN/)
}

private BuildInformation extractBuildInformationFromTextWithPattern(String text, String pattern) {
    def matches = (text =~ pattern)

    assert matches: 'Unable to parse build information'

    new BuildInformation(matches.group('hash'), matches.group('branch'))
}

private void continueIfArgumentsAreValidOrExitWithError() {
    if (args.length < 1) {
        println "Usage: ${this.getClass().simpleName} WARFILE"
        System.exit(1)
    }
}


@Field
static String BUILD_INFO_PATTERN = /(?<hash>\p{XDigit}{7})\s+\((?<branch>.+)\)/

Map HOSTS = [
        production: 'otp.local/otp/',
        beta: 'otp.local/otp/',
]

continueIfArgumentsAreValidOrExitWithError()

File warFile = new File(args[0])
String host = args.size() >= 2 ? args[1] : 'production'

assert warFile.exists() : "File ${warFile} does not exist"
assert host in HOSTS.keySet() : "URL of host ${host} is not known"

String url = HOSTS[host]

BuildInformation warVersion = extractBuildInformationFromWarFile(warFile)
BuildInformation webVersion = extractBuildInformationFromURL(url)

if (warVersion != webVersion) {
    System.err.println "ERROR: Versions do NOT match between WAR file ${warFile} and ${url}!"
    System.err.println "  WAR file: ${warVersion.shortRevision}, ${warVersion.branch}"
    System.err.println "  Deployed: ${webVersion.shortRevision}, ${webVersion.branch}"
    System.exit(13)
}

println "OK: Found version ${warVersion.shortRevision} from branch ${warVersion.branch} in WAR file ${warFile} and ${url}."
