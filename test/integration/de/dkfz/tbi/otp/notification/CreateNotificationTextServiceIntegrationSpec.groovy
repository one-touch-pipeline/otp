package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import org.codehaus.groovy.grails.commons.*
import org.springframework.beans.factory.annotation.*

class CreateNotificationTextServiceIntegrationSpec extends IntegrationSpec {

    static final String CONTROLLER = 'controller'
    static final String ACTION = 'action'


    CreateNotificationTextService createNotificationTextService

    @Autowired
    GrailsApplication grailsApplication


    void "createOtpLinks, when input invalid, should throw assert"() {
        when:
        createNotificationTextService.createOtpLinks(projects, controller, action)

        then:
        AssertionError e = thrown()
        e.message.contains('assert ' + errorMessage)

        where:
        projects        | controller | action || errorMessage
        null            | CONTROLLER | ACTION || 'projects'
        []              | CONTROLLER | ACTION || 'projects'
        [new Project()] | null       | ACTION || CONTROLLER
        [new Project()] | CONTROLLER | null   || ACTION
    }


    void "createOtpLinks, when input valid, return sorted URLs of the projects"() {
        given:
        List<Project> projects = [
                DomainFactory.createProject(name: 'project3'),
                DomainFactory.createProject(name: 'project5'),
                DomainFactory.createProject(name: 'project2'),
        ]

        String expected = [
                "${grailsApplication.config.grails.serverURL}/${CONTROLLER}/${ACTION}?project=project2",
                "${grailsApplication.config.grails.serverURL}/${CONTROLLER}/${ACTION}?project=project3",
                "${grailsApplication.config.grails.serverURL}/${CONTROLLER}/${ACTION}?project=project5",
        ].join('\n')

        expect:
        expected == createNotificationTextService.createOtpLinks(projects, CONTROLLER, ACTION)
    }

    void "createOtpLinks, when input valid and custom project parameter name, return sorted URLs of the projects"() {
        given:
        List<Project> projects = [
                DomainFactory.createProject(name: 'project3'),
                DomainFactory.createProject(name: 'project5'),
                DomainFactory.createProject(name: 'project2'),
        ]

        String expected = [
                "${grailsApplication.config.grails.serverURL}/${CONTROLLER}/${ACTION}?projectName=project2",
                "${grailsApplication.config.grails.serverURL}/${CONTROLLER}/${ACTION}?projectName=project3",
                "${grailsApplication.config.grails.serverURL}/${CONTROLLER}/${ACTION}?projectName=project5",
        ].join('\n')

        expect:
        expected == createNotificationTextService.createOtpLinks(projects, CONTROLLER, ACTION, 'projectName')
    }

}
