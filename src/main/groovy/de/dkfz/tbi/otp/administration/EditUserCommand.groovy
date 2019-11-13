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

import de.dkfz.tbi.otp.security.User

class EditUserCommand implements Validateable {
    private static final long serialVersionUID = 1L

    User user
    String email
    String realName

    static constraints = {
        email(nullable: false, blank: false, email: true, validator: { val, obj ->
            User userByMail = User.findByEmail(val)
            if (userByMail != null && userByMail != obj.user) {
                return 'default.not.unique.message'
            }
        })
        realName(blank: false)
    }

    void setEmail(String email) {
        this.email = email?.trim()?.replaceAll(" +", " ")
    }

    void setRealName(String realName) {
        this.realName = realName?.trim()?.replaceAll(" +", " ")
    }
}
