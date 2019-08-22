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


import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.Comment

class IlseSubmissionSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Comment,
                IlseSubmission,
        ]
    }

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
