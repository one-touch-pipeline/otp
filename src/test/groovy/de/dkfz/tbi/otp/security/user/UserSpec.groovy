/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.security.user

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.User

class UserSpec extends Specification implements DataTest, UserDomainFactory {

    private static final String EMAIL_INTERN = 'intern@de.de'
    private static final String EMAIL_EXTERN = 'extern@de.de'

    @Override
    Class[] getDomainClassesToMock() {
        return [
                User,
        ]
    }

    @Unroll
    void "validate email, when create new user with account '#username' and email='#email', then validation error count should be #errorCount"() {
        given:
        createUser([
                username: 'intern',
                email   : EMAIL_INTERN,
        ])
        createUser([
                username: null,
                email   : EMAIL_EXTERN,
        ])

        when:
        User newUser = new User([
                username: username,
                email   : email,
                password: "some value ${nextId}",
        ])

        newUser.validate()

        then:
        newUser.errors.errorCount == errorCount

        where:
        username    | email        || errorCount
        'newintern' | 'new@de.de'  || 0
        'newintern' | EMAIL_INTERN || 0
        'newintern' | EMAIL_EXTERN || 0
        null        | 'new@de.de'  || 0
        null        | EMAIL_INTERN || 0
        null        | EMAIL_EXTERN || 1
    }
}
