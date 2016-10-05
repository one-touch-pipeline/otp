package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.plugin.springsecurity.SpringSecurityService
import grails.test.mixin.Mock
import grails.validation.ValidationException
import spock.lang.*

@Mock([
        Comment,
        IlseSubmission,
])
class IlseSubmissionServiceSpec extends Specification {


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
