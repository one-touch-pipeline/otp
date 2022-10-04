/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class CommentServiceIntegrationSpec extends Specification implements UserAndRoles {

    CommentService commentService

    static final TEST_MESSAGE = "testMessage"

    void setupData() {
        createUserAndRoles()
    }

    void "test saveComment with Project"() {
        given:
        setupData()
        CommentableWithProject project = DomainFactory.createProject()
        Comment comment

        when:
        comment = doWithAuth(ADMIN) {
            commentService.saveComment(project, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        project.comment == comment
    }

    void "test saveComment with Individual"() {
        given:
        setupData()
        CommentableWithProject individual = DomainFactory.createIndividual()
        Comment comment

        when:
        comment = doWithAuth(ADMIN) {
            commentService.saveComment(individual, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        individual.comment == comment
    }

    void "test saveComment with Datafile"() {
        given:
        setupData()
        CommentableWithProject datafile = DomainFactory.createDataFile()
        Comment comment

        when:
        comment = doWithAuth(ADMIN) {
            commentService.saveComment(datafile, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        datafile.comment == comment
    }

    void "test saveComment with Process"() {
        given:
        setupData()
        CommentableWithProject process = DomainFactory.createProcess()
        Comment comment

        when:
        comment = doWithAuth(ADMIN) {
            commentService.saveComment(process, TEST_MESSAGE)
        }

        then:
        comment.comment == TEST_MESSAGE
        comment.author == ADMIN
        process.comment == comment
    }

    void "test createOrUpdateComment with Process"() {
        given:
        setupData()
        CommentableWithProject process = DomainFactory.createProcess()

        when:
        Comment comment = commentService.createOrUpdateComment(process, 'testMessage', USER)

        then:
        comment.comment == 'testMessage'
        comment.author == USER
        process.comment == comment
    }
}
