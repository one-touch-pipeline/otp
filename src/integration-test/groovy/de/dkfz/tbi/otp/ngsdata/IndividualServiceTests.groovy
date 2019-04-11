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
import org.junit.Test
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles

import static org.junit.Assert.*

@Rollback
@Integration
class IndividualServiceTests implements UserAndRoles {
    IndividualService individualService
    private static long ARBITRARY_TIMESTAMP = 1337

    void setupData() {
        createUserAndRoles()
    }

    @Test
    void testGetIndividualByName() {
        setupData()
        Individual individual = mockIndividual()
        // a user should not be able to get the Individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual("test")
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual("test1"))
        }
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            // operator user should find this individual
            assertSame(individual, individualService.getIndividual("test"))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual("test1"))
        }
        SpringSecurityUtils.doWithAuth(ADMIN) {
            // admin user should find this individual
            assertSame(individual, individualService.getIndividual("test"))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual("test1"))
            // grant read to testuser
            addUserWithReadAccessToProject(User.findByUsername(TESTUSER), individual.project)
        }
        // now the user should be allowed to read this individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertSame(individual, individualService.getIndividual("test"))
        }
        // but a different user should still not be allowed to get the Individual
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual("test")
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual("test1"))
        }
    }

    @Test
    void testGetIndividualById() {
        setupData()
        Individual individual = mockIndividual()
        // a user should not be able to get the Individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual(individual.id)
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual(individual.id + 1))
        }
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            // admin user should find this individual
            assertSame(individual, individualService.getIndividual(individual.id))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual(individual.id + 1))
        }
        SpringSecurityUtils.doWithAuth(ADMIN) {
            // admin user should find this individual
            assertSame(individual, individualService.getIndividual(individual.id))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual(individual.id + 1))
            // grant read to testuser
            addUserWithReadAccessToProject(User.findByUsername(TESTUSER), individual.project)
        }
        // now the user should be allowed to read this individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertSame(individual, individualService.getIndividual(individual.id))
        }
        // but a different user should still not be allowed to get the Individual
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual(individual.id)
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual(individual.id + 1))
        }
    }

    @Test
    void testCreateCommentString_WhenSeveralPropertiesEqual_ShouldJustAddDifferentOnes() {
        setupData()
        String operation = "operation"
        Map mapA = [a: 1, b: 2, c: 3]
        Map mapB = [a: 1, b: 3, c: 4]
        Date date = new Date()

        assert """== operation - ${date.format("yyyy-MM-dd HH:mm")} ==
Old:
b: 2
c: 3
New:
b: 3
c: 4
""" == individualService.createCommentString(operation, mapA, mapB, date, null)
    }

    @Test
    void testCreateCommentString_WhenCommentWithAdditionalInformation_ShouldReturnCorrectString() {
        setupData()
        String operation = "operation"
        Map mapA = [a: 1]
        Map mapB = [a: 2]
        Date date = new Date()
        String additionalInformation = "additional information"

        assert """== operation - ${date.format("yyyy-MM-dd HH:mm")} ==
${additionalInformation}
Old:
a: 1
New:
a: 2
""" == individualService.createCommentString(operation, mapA, mapB, date, additionalInformation)
    }

    @Test
    void testCreateComment_WhenCommentAlreadyExists_ShouldAddNewComment() {
        setupData()
        Individual indOld = DomainFactory.createIndividual(comment: DomainFactory.createComment(comment: "old comment"))
        Individual indNew = DomainFactory.createIndividual()
        String operation = "operation"
        Map mapOld = [individual: indOld]
        Map mapNew = [individual: indNew]

        DateTimeUtils.setCurrentMillisFixed(ARBITRARY_TIMESTAMP)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            individualService.createComment(operation, mapOld, mapNew)
        }

        assert """== operation - ${new DateTime().toDate().format("yyyy-MM-dd HH:mm")} ==
Old:
individual: ${indOld}
New:
individual: ${indNew}

${indOld.comment.comment}""" == indNew.comment.comment

        DateTimeUtils.setCurrentMillisSystem()
    }

    @Test
    void testCreateComment_WhenParametersNull_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            individualService.createComment(null, null, null)
        }
    }

    @Test
    void testCreateComment_WhenMapsHaveDifferentKeySets_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            individualService.createComment(null, [individual: DomainFactory.createIndividual(), a: 1], [individual: DomainFactory.createIndividual()])
        }
    }

    @Test
    void testCreateComment_WhenInputMapsContainNoIndividual_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            individualService.createComment("", [property: null], [property: null])
        }
    }

    private Individual mockIndividual(String pid = "test", Project project = null) {
        Individual ind = new Individual(pid: pid, mockPid: pid, mockFullName: pid, project: project ?: mockProject(), type: Individual.Type.REAL)
        assertNotNull(ind.save(flush: true))
        return ind
    }

    private Project mockProject(String name = "test") {
        return Project.findOrSaveWhere(
                name: name,
                dirName: name,
                realm: DomainFactory.createRealm(),
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
    }
}
