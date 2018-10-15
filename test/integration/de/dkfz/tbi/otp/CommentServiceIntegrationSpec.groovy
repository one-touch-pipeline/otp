package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.testing.UserAndRoles
import grails.plugin.springsecurity.SpringSecurityUtils
import spock.lang.Specification

class CommentServiceIntegrationSpec extends Specification implements UserAndRoles {

    CommentService commentService

    static final TEST_MESSAGE = "testMessage"

    def setup() {
        createUserAndRoles()
    }

    void "test saveComment with Project"() {
        given:
        Commentable project = DomainFactory.createProject()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            comment = commentService.saveComment(project, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        project.comment == comment
    }

    void "test saveComment with Individual"() {
        given:
        Commentable individual = DomainFactory.createIndividual()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            comment = commentService.saveComment(individual, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        individual.comment == comment
    }

    void "test saveComment with Datafile"() {
        given:
        Commentable datafile = DomainFactory.createDataFile()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            comment = commentService.saveComment(datafile, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        datafile.comment == comment
    }

    void "test saveComment with Process"() {
        given:
        Commentable process = DomainFactory.createProcess()
        Comment comment

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            comment = commentService.saveComment(process, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        process.comment == comment
    }

    void "test createOrUpdateComment with Process"() {
        given:
        Commentable process = DomainFactory.createProcess()

        when:
        Comment comment = commentService.createOrUpdateComment(process, 'testMessage', USER)

        then:
        comment.comment == 'testMessage'
        comment.author == USER
        process.comment == comment
    }
}
