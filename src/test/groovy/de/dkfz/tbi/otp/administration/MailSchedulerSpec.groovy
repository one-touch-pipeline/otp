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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.security.UserAndRoles

class MailSchedulerSpec extends Specification implements DataTest, DomainFactoryCore, UserDomainFactory, UserAndRoles {

    MailScheduler mailScheduler

    void "scheduleMail, should schedule all waiting mails"() {
        given:
        mailScheduler = new MailScheduler()
        mailScheduler.mailHelperService = Mock(MailHelperService)
        List<Mail> mails = (1..3).collect {
            new Mail()
        }

        when:
        mailScheduler.scheduleMail()

        then:
        1 * mailScheduler.mailHelperService.fetchMailsInWaiting() >> mails
        3 * mailScheduler.mailHelperService.changeMailState(_ as Mail, Mail.State.SENDING)
        3 * mailScheduler.mailHelperService.sendMailByScheduler(_ as Mail)
        0 * mailScheduler.mailHelperService.changeMailState(_ as Mail, Mail.State.FAILED)
    }

    void "scheduleMail, when there is an exception, then change state to failed and send an error mail directly"() {
        given:
        mailScheduler = new MailScheduler()
        mailScheduler.mailHelperService = Mock(MailHelperService)
        Mail mail = new Mail()

        when:
        mailScheduler.scheduleMail()

        then:
        1 * mailScheduler.mailHelperService.fetchMailsInWaiting() >> [mail]
        1 * mailScheduler.mailHelperService.changeMailState(mail, Mail.State.SENDING)
        1 * mailScheduler.mailHelperService.sendMailByScheduler(mail) >> {
            throw new IllegalArgumentException()
        }
        1 * mailScheduler.mailHelperService.changeMailState(mail, Mail.State.FAILED)
        1 * mailScheduler.mailHelperService.sendMailByScheduler(_ as Mail) >> {
            assert mail.id == null
        }
    }
}
