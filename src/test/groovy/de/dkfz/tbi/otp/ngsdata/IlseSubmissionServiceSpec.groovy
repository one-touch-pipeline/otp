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
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class IlseSubmissionServiceSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Comment,
                IlseSubmission,
                Individual,
                ProcessingPriority,
                Project,
                Realm,
                Sample,
                SampleType,
        ]
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

    @Unroll
    void "test getSortedBlacklistedIlseSubmissions single"() {
        given:
        createIlseSubmission(
                comment: DomainFactory.createComment(),
                warning: blacklisted
        )
        IlseSubmissionService service = new IlseSubmissionService(commentService: new CommentService())

        when:
        List<IlseSubmission> ilseSubmissions = service.sortedBlacklistedIlseSubmissions

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
        createIlseSubmission(
                ilseNumber: ilse1,
                comment: DomainFactory.createComment(),
                warning: true
        )
        createIlseSubmission(
                ilseNumber: ilse2,
                comment: DomainFactory.createComment(),
                warning: blacklisted
        )
        IlseSubmissionService service = new IlseSubmissionService()

        when:
        List<IlseSubmission> ilseSubmissions = service.sortedBlacklistedIlseSubmissions

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
        createIlseSubmission(
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

    void "test createNewIlseSubmissions valid"() {
        given:
        IlseSubmissionService service = createIlseSubmissionServiceHelper()

        expect:
        List<IlseSubmission> ilseSubmission = service.createNewIlseSubmissions([1234, 567, 89,], HelperUtils.uniqueString)
        ilseSubmission
        ilseSubmission.size() == 3
        ilseSubmission[0].id
        ilseSubmission[0].ilseNumber == 1234
        ilseSubmission[1].id
        ilseSubmission[1].ilseNumber == 567
        ilseSubmission[2].id
        ilseSubmission[2].ilseNumber == 89
    }

    @Unroll
    void "test createNewIlseSubmissions invalid (#ilse, #expectedErrorPart)"() {
        given:
        IlseSubmissionService service = createIlseSubmissionServiceHelper()

        when:
        service.createNewIlseSubmissions([ilse], comment)

        then:
        ValidationException e = thrown()
        e.message.contains(expectedErrorPart)

        where:
        ilse                               | comment                  || expectedErrorPart
        IlseSubmission.MIN_ILSE_VALUE - 1  | HelperUtils.uniqueString || 'ilse'
        IlseSubmission.MAX_ILSE_NUMBER + 1 | HelperUtils.uniqueString || 'ilse'
        1234                               | null                     || 'comment'
    }

    private SeqTrack createSeqTrackWithIlseNumber(Integer ilseNumber) {
        IlseSubmission ilseSubmission = ilseNumber ? (IlseSubmission.findByIlseNumber(ilseNumber) ?: createIlseSubmission(ilseNumber: ilseNumber)) : null
        return createSeqTrack(ilseSubmission: ilseSubmission)
    }

    @Unroll
    void "buildIlseIdentifier and buildIlseIdentifierFromSeqTracks, all given, some missing, all missing, empty"() {
        given:
        IlseSubmissionService service = new IlseSubmissionService()
        List<SeqTrack> seqTracks = ilses.collect { Integer ilseNumber ->
            createSeqTrackWithIlseNumber(ilseNumber)
        }

        expect:
        expected == service.buildIlseIdentifierFromSeqTracks(seqTracks)
        expected == service.buildIlseIdentifier(ilses)

        where:
        ilses           || expected
        [3, 1, 1, 2]    || "[S#1,2,3]"
        [6, 4, 5, null] || "[S#4,5,6]"
        [null, null]    || ""
        []              || ""
    }
}
