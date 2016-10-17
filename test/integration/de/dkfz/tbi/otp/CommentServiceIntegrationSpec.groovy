package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.testing.UserAndRoles
import grails.plugin.springsecurity.SpringSecurityUtils
import spock.lang.Specification

class CommentServiceIntegrationSpec extends Specification implements UserAndRoles {

    CommentService commentService


    def setup() {
        createUserAndRoles()
    }

    void "test saveComment with Project"() {
        given:
        Commentable project = DomainFactory.createProject()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            comment = commentService.saveComment(project, "testMessage")
        }

        then:
        comment.comment == "testMessage"
        comment.author == "admin"
        project.comment == comment
    }

    void "test saveComment with Individual"() {
        given:
        Commentable individual = DomainFactory.createIndividual()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            comment = commentService.saveComment(individual, "testMessage")
        }

        then:
        comment.comment == "testMessage"
        comment.author == "admin"
        individual.comment == comment
    }

    void "test saveComment with Datafile"() {
        given:
        Commentable datafile = DomainFactory.createDataFile()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            comment = commentService.saveComment(datafile, "testMessage")
        }

        then:
        comment.comment == "testMessage"
        comment.author == "admin"
        datafile.comment == comment
    }

    void "test saveComment with Process"() {
        given:
        Commentable process = DomainFactory.createProcess()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            comment = commentService.saveComment(process, "testMessage")
        }

        then:
        comment.comment == "testMessage"
        comment.author == "admin"
        process.comment == comment
    }

    void "test createOrUpdateComment with Process"() {
        given:
        Commentable process = DomainFactory.createProcess()

        when:
        Comment comment = commentService.createOrUpdateComment(process, 'testMessage', 'some user')

        then:
        comment.comment == 'testMessage'
        comment.author == 'some user'
        process.comment == comment
    }
}
