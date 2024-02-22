/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.hibernate.annotation.ManagedEntity
import org.apache.commons.validator.routines.EmailValidator

import de.dkfz.tbi.otp.utils.Entity

import java.time.ZonedDateTime

@ManagedEntity
class Mail implements Entity {

    enum State {
        WAITING,
        SENDING,
        SENT,
        FAILED,
    }

    String subject

    String body

    Set<String> to

    Set<String> cc

    Set<String> bcc

    ZonedDateTime sendDateTime

    State state = State.WAITING

    static Closure mapping = {
        body type: "text"
        state index: "mail_state_date_created_idx"
        dateCreated index: "mail_state_date_created_idx"
    }

    static Closure constraints = {
        subject blank: false, maxSize: 1000
        body blank: false
        sendDateTime nullable: true
        to validator: {
            checkMail(it)
        }
        cc validator: {
            checkMail(it)
        }
        bcc validator: {
            checkMail(it)
        }
    }

    static private boolean checkMail(Set<String> mailAddresses) {
        EmailValidator emailValidator = EmailValidator.instance
        return mailAddresses.every {
            emailValidator.isValid(it)
        }
    }
}
