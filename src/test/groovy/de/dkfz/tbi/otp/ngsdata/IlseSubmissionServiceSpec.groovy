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

import grails.plugin.springsecurity.SpringSecurityService
import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.utils.HelperUtils

class IlseSubmissionServiceSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            Comment,
            IlseSubmission,
    ]}

    @Unroll
    void "test getSortedBlacklistedIlseSubmissions single"() {
        given:
        DomainFactory.createIlseSubmission(
                comment: DomainFactory.createComment(),
                warning: blacklisted
        )
        IlseSubmissionService service = new IlseSubmissionService(commentService: new CommentService())

        when:
        List<IlseSubmission> ilseSubmissions = service.getSortedBlacklistedIlseSubmissions()

        then:
        expectedResultSize == ilseSubmissions.size()

        where:
        blacklisted || expectedResultSize
        true        || 1
        false       || 0
    }


    @Unroll
    void "test getSortedBlacklistedIlseSubmissions multiple"() {
        given:
        DomainFactory.createIlseSubmission(
                ilseNumber: ilse1,
                comment: DomainFactory.createComment(),
                warning: true
        )
        DomainFactory.createIlseSubmission(
                ilseNumber: ilse2,
                comment: DomainFactory.createComment(),
                warning: blacklisted
        )
        IlseSubmissionService service = new IlseSubmissionService()

        when:
        List<IlseSubmission> ilseSubmissions = service.getSortedBlacklistedIlseSubmissions()

        then:
        expectedIlse == ilseSubmissions*.ilseNumber

        where:
        blacklisted | ilse1 | ilse2 || expectedIlse
        true        | 1234  | 2345  || [2345, 1234]
        false       | 1234  | 2345  || [1234]
        true        | 4321  | 23456 || [23456, 4321]
    }


    @Unroll
    void "test checkIfIlseNumberDoesNotExist"() {
        given:
        DomainFactory.createIlseSubmission(
                ilseNumber: 1234,
        )
        IlseSubmissionService service = new IlseSubmissionService()

        expect:
        notExists == service.checkIfIlseNumberDoesNotExist(ilse)

        where:
        ilse || notExists
        1234 || false
        2345 || true
    }


    void "test createNewIlseSubmission valid"() {
        given:
        IlseSubmissionService service = createIlseSubmissionServiceHelper()

        expect:
        IlseSubmission ilseSubmission = service.createNewIlseSubmission(1234, HelperUtils.uniqueString)
        ilseSubmission.id
    }


    @Unroll
    void "test createNewIlseSubmission invalid"() {
        given:
        IlseSubmissionService service = createIlseSubmissionServiceHelper()

        when:
        service.createNewIlseSubmission(ilse, comment)

        then:
        ValidationException e = thrown()
        e.message.contains(expectedErrorPart)

        where:
        ilse | comment                  || expectedErrorPart
        0    | HelperUtils.uniqueString || 'ilse'
        1234 | null                     || 'comment'
    }


    private IlseSubmissionService createIlseSubmissionServiceHelper() {
        return new IlseSubmissionService(
                commentService: new CommentService(
                        springSecurityService: Mock(SpringSecurityService) {
                            getPrincipal() >> GroovyMock(Object) {
                                getUsername() >> 'username'
                            }
                        }
                )
        )
    }
}
