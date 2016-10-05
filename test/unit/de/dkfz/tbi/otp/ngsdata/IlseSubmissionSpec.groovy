package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        Comment,
        IlseSubmission,
])
class IlseSubmissionSpec extends Specification {

    @Unroll
    void "test constraints all fine"() {
        expect:
        DomainFactory.createIlseSubmission(
                comment: DomainFactory.createComment(),
                warning: warning
        )

        where:
        warning << [true, false]
    }


    @Unroll
    void "test constraints ilseNumber invalid"() {
        given:
        IlseSubmission ilseSubmission = DomainFactory.createIlseSubmission()

        when:
        ilseSubmission.ilseNumber = value

        then:
        TestCase.assertValidateError(ilseSubmission, 'ilseNumber', constraint, value)

        where:
        value        || constraint
        1          || 'min.notmet'
        1111111111 || 'max.exceeded'
    }


    void "test unique constraints of ilseNumber invalid"() {
        given:
        IlseSubmission ilseSubmission = DomainFactory.createIlseSubmission()

        when:
        IlseSubmission ilseSubmission2 = DomainFactory.createIlseSubmission([ilseNumber: ilseSubmission.ilseNumber], false)

        then:
        TestCase.assertValidateError(ilseSubmission2, 'ilseNumber', 'unique', ilseSubmission.ilseNumber)
    }


    @Unroll
    void "test constraints comment invalid"() {
        given:
        IlseSubmission ilseSubmission = DomainFactory.createIlseSubmission(
                comment: comment,
        )

        when:
        ilseSubmission.warning = true

        then:
        TestCase.assertValidateError(ilseSubmission, 'comment', 'a comment need to be provided', comment)

        where:
        comment << [
                null,
                DomainFactory.createComment(comment: ''),
        ]
    }
}
