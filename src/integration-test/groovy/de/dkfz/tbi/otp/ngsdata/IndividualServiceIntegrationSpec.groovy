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
package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.springframework.security.access.AccessDeniedException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class IndividualServiceIntegrationSpec extends Specification implements UserAndRoles {

    IndividualService individualService
    private static final long ARBITRARY_TIMESTAMP = 1337

    void setupData() {
        createUserAndRoles()
    }

    @Unroll
    void "getIndividual by #identifier as #role"() {
        given:
        setupData()
        Individual individual = DomainFactory.createIndividual()
        Map<String, Object> searchProperty = [id: individual.id, name: individual.mockFullName]
        Individual returnedIndividual

        when: "requesting an available individual"
        SpringSecurityUtils.doWithAuth(role) {
            returnedIndividual = individualService.getIndividual(searchProperty[identifier] as String)
        }

        then: "permission granted, returns individual"
        returnedIndividual == individual

        when: "requesting an unavailable individual"
        SpringSecurityUtils.doWithAuth(role) {
            returnedIndividual = individualService.getIndividual("unavail_" + searchProperty[identifier] as String)
        }

        then: "permission granted, returns null"
        returnedIndividual == null

        where:
        role     | identifier
        OPERATOR | "name"
        OPERATOR | "id"
        ADMIN    | "name"
        ADMIN    | "id"
    }

    @Unroll
    void "getIndividual by #identifier with permission granted via project membership"() {
        given:
        setupData()
        Individual individual = DomainFactory.createIndividual()
        Map<String, Object> searchProperty = [id: individual.id, name: individual.mockFullName]

        when: "an unauthorized user requests an available individual"
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            individualService.getIndividual(searchProperty[identifier] as String)
        }

        then: "throw an AccessDeniedException"
        thrown(AccessDeniedException)

        when: "add permission to user and request again"
        SpringSecurityUtils.doWithAuth(ADMIN) {
            addUserWithReadAccessToProject(User.findByUsername(TESTUSER), individual.project)
        }

        Individual returnedIndividual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            returnedIndividual = individualService.getIndividual(searchProperty[identifier] as String)
        }

        then: "return the requested individual"
        returnedIndividual == individual

        when: "non-project user requests the same individual"
        SpringSecurityUtils.doWithAuth(USER) {
            individualService.getIndividual(searchProperty[identifier] as String)
        }

        then: "still throws an AccessDeniedException"
        thrown(AccessDeniedException)

        where:
        identifier | _
        "name"     | _
        "id"       | _
    }

    void "createCommentString, when several properties equal, should just add different ones"() {
        given:
        setupData()

        String operation = "operation"
        Map mapA = [a: 1, b: 2, c: 3]
        Map mapB = [a: 1, b: 3, c: 4]
        Date date = new Date()

        when:
        String result = individualService.createCommentString(operation, mapA, mapB, date, null)

        then:
        result == """== operation - ${date.format("yyyy-MM-dd HH:mm")} ==
Old:
b: 2
c: 3
New:
b: 3
c: 4
"""
    }

    void "createCommentString, when comment with additional information, should return correct string"() {
        given:
        setupData()

        String operation = "operation"
        Map mapA = [a: 1]
        Map mapB = [a: 2]
        Date date = new Date()
        String additionalInformation = "additional information"

        when:
        String result = individualService.createCommentString(operation, mapA, mapB, date, additionalInformation)

        then:
        result == """== operation - ${date.format("yyyy-MM-dd HH:mm")} ==
${additionalInformation}
Old:
a: 1
New:
a: 2
"""
    }

    @Unroll
    void "createComment, with different combinations of preexisting comments; old individual: #oldHasComment, new individual: #newHasComment"() {
        given:
        setupData()

        Closure<Individual> createIndividualWithComment = { boolean hasComment ->
            return DomainFactory.createIndividual(comment: hasComment ? DomainFactory.createComment() : null)
        }

        String operation = "operation"
        Individual oldIndividual = createIndividualWithComment(oldHasComment)
        Individual newIndividual = createIndividualWithComment(newHasComment)

        DateTimeUtils.currentMillisFixed = ARBITRARY_TIMESTAMP

        String expected = """\
            |${newHasComment ? newIndividual.comment.comment : ""}
            |
            |== ${operation} - ${new DateTime().toDate().format("yyyy-MM-dd HH:mm")} ==
            |Old:
            |individual: ${oldIndividual}
            |New:
            |individual: ${newIndividual}
            |
            |
            |${oldHasComment ? oldIndividual.comment.comment : ""}""".stripMargin().trim()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            individualService.createComment(operation, [individual: oldIndividual], [individual: newIndividual])
        }

        then:
        newIndividual.comment.comment == expected

        cleanup:
        DateTimeUtils.setCurrentMillisSystem()

        where:
        oldHasComment | newHasComment
        false         | false
        false         | true
        true          | false
        true          | true
    }

    @Unroll
    void "createComment, when target and source individual are the same, does not copy over source comment (with existing comment: #hasComment)"() {
        given:
        setupData()

        String operation = "operation"
        Individual individual = DomainFactory.createIndividual(comment: hasComment ? DomainFactory.createComment() : null)

        DateTimeUtils.currentMillisFixed = ARBITRARY_TIMESTAMP

        String expected = """\
            |${hasComment ? individual.comment.comment : ""}
            |
            |== ${operation} - ${new DateTime().toDate().format("yyyy-MM-dd HH:mm")} ==
            |Old:
            |diff: A
            |New:
            |diff: B""".stripMargin().trim()

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            individualService.createComment(operation, [individual: individual, diff: "A"], [individual: individual, diff: "B"])
        }

        then:
        individual.comment.comment == expected

        cleanup:
        DateTimeUtils.setCurrentMillisSystem()

        where:
        hasComment | _
        false      | _
        true       | _
    }

    void "createComment, should fail when parameters are null"() {
        given:
        setupData()

        when:
        individualService.createComment(null, null, null)

        then:
        thrown(AssertionError)
    }

    void "createComment, should fail, when maps have different key sets"() {
        given:
        setupData()

        when:
        individualService.createComment(null, [individual: DomainFactory.createIndividual(), a: 1], [individual: DomainFactory.createIndividual()])

        then:
        thrown(AssertionError)
    }

    void "createComment, should fail, when input maps contain no individual"() {
        given:
        setupData()

        when:
        individualService.createComment("", [property: null], [property: null])

        then:
        thrown(AssertionError)
    }
}
