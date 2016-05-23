package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        Project,
        Realm,
])
class ProjectSpec extends Specification {


    void "test getProjectDirectory all fine should return File"() {
        given:
        Project project = DomainFactory.createProjectWithRealms()

        when:
        File file = project.getProjectDirectory()

        then:
        file.isAbsolute()
        file.path.contains(project.dirName)
    }

    void "test getProjectDirectory project directory contains slashes should return File"() {
        given:
        Project project = DomainFactory.createProjectWithRealms(
                dirName: "${HelperUtils.uniqueString}/${HelperUtils.uniqueString}/${HelperUtils.uniqueString}"
        )

        when:
        File file = project.getProjectDirectory()

        then:
        file.isAbsolute()
        file.path.contains(project.dirName)
    }

    void "test getProjectDirectory realm not known should thrown Exception"() {
        given:
        Project project = DomainFactory.createProject()

        when:
        project.getProjectDirectory()

        then:
        RuntimeException e = thrown()
        e.message.contains("No ${Realm.OperationType.DATA_MANAGEMENT} realm found for project ${project}.")
    }

}
