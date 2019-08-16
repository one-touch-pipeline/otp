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

package de.dkfz.tbi.otp.administration

import grails.validation.Validateable

import de.dkfz.tbi.otp.security.*

class CreateUserCommand implements Validateable {

    String username
    String email
    String realName
    List<Long> role
    List<Long> group

    static constraints = {
        username(validator: { value ->
            return User.findByUsername(value) == null
        })
        email(email: true)
        realName(blank: false, nullable: true)
        role(nullable: true, validator: { value ->
            boolean valid = true
            value.each { id ->
                if (!Role.get(id)) {
                    valid = false
                }
            }
            return valid
        })
        group(nullable: true, validator: { value ->
            boolean valid = true
            value.each { id ->
                if (!Group.get(id)) {
                    valid = false
                }
            }
            return valid
        })
    }
}
