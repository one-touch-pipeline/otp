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
import org.springframework.security.access.AccessDeniedException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.util.TimeFormats

@Rollback
@Integration
class IndividualServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore {

    IndividualService individualService

    TestConfigService configService

    void setupData() {
        createUserAndRoles()
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "getIndividual by #identifier as #role"() {
        given:
        setupData()
        Individual individual = createIndividual()
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
        Individual individual = createIndividual()
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

        when:
        String result = individualService.createCommentString(operation, mapA, mapB, null)

        then:
        result == """== operation - ${TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(configService.zonedDateTime)} ==
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
        String additionalInformation = "additional information"

        when:
        String result = individualService.createCommentString(operation, mapA, mapB, additionalInformation)

        then:
        result == """== operation - ${TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(configService.zonedDateTime)} ==
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
            return createIndividual(comment: hasComment ? DomainFactory.createComment() : null)
        }

        String operation = "operation"
        Individual oldIndividual = createIndividualWithComment(oldHasComment)
        Individual newIndividual = createIndividualWithComment(newHasComment)

        configService.fixClockTo()

        String expected = """\
            |${newHasComment ? newIndividual.comment.comment : ""}
            |
            |== ${operation} - ${TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(configService.zonedDateTime)} ==
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
        Individual individual = createIndividual(comment: hasComment ? DomainFactory.createComment() : null)

        configService.fixClockTo()

        String expected = """\
            |${hasComment ? individual.comment.comment : ""}
            |
            |== ${operation} - ${TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedZonedDateTime(configService.zonedDateTime)} ==
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
        individualService.createComment(null, [individual: createIndividual(), a: 1], [individual: createIndividual()])

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
