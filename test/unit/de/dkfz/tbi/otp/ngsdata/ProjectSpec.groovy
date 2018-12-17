package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.utils.HelperUtils

@Mock([
        Project,
        ProjectCategory,
        Realm,
])
class ProjectSpec extends Specification {

    void "test getProjectDirectory all fine should return File"() {
        given:
        Project project = DomainFactory.createProject()

        when:
        File file = project.getProjectDirectory()

        then:
        file.isAbsolute()
        file.path.contains(project.dirName)
    }

    void "test getProjectDirectory project directory contains slashes should return File"() {
        given:
        Project project = DomainFactory.createProject(
                dirName: "${HelperUtils.uniqueString}/${HelperUtils.uniqueString}/${HelperUtils.uniqueString}"
        )

        when:
        File file = project.getProjectDirectory()

        then:
        file.isAbsolute()
        file.path.contains(project.dirName)
    }
}
