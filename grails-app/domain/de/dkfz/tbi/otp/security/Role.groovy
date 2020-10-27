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
package de.dkfz.tbi.otp.security

import de.dkfz.tbi.otp.utils.Entity

/**
 * Auto generated class by spring security plugin.
 */
class Role implements Entity {

    static final String ROLE_ADMIN = 'ROLE_ADMIN'
    static final String ROLE_OPERATOR = 'ROLE_OPERATOR'
    static final String ROLE_SWITCH_USER = 'ROLE_SWITCH_USER'
    static final String ROLE_TEST_BIOINFORMATICAN = 'ROLE_TEST_BIOINFORMATICAN'
    static final String ROLE_TEST_SUBMITTER = 'ROLE_TEST_SUBMITTER'
    static final String ROLE_TEST_PI = 'ROLE_TEST_PI'

    static final List<String> REQUIRED_ROLES = [
            ROLE_ADMIN,
    ].asImmutable()

    static final List<String> IMPORTANT_ROLES = REQUIRED_ROLES + [
            ROLE_OPERATOR,
            ROLE_SWITCH_USER,
    ].asImmutable()

    static final List<String> ADMINISTRATIVE_ROLES = [
            ROLE_ADMIN,
            ROLE_OPERATOR,
    ].asImmutable()

    static final List<String> TEST_ROLES = [
            ROLE_TEST_BIOINFORMATICAN,
            ROLE_TEST_SUBMITTER,
            ROLE_TEST_PI,
    ].asImmutable()

    String authority

    static mapping = {
        cache true
    }

    static constraints = {
        authority blank: false, unique: true
    }
}
